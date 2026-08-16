#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
QA automatizado de Vitrina V1 sobre dispositivo fisico (Galaxy A51).

Mide con datos (pixeles + adb + uiautomator), sin verificacion visual:
- t_panel: cuan rápido abre el panel al tocar el badge del ancla.
- Panel: alto real (dp) y su borde superior, contra el token kb/panel/height.
- Solape: si el borde superior del panel tapa el ultimo mensaje del chat.
- Regresion del crash: escribir el trigger no debe tumbar el IME (ADR de la
  capa: showBar asignaba FrameLayout.LayoutParams a un hijo del LinearLayout
  raiz; ver commit 3dc69b23).

CÓMO SE LEE LA PANTALLA — DOS MECANISMOS, DOS ALCANCES:

1. uiautomator dump -> SOLO WhatsApp (campo de entrada, ultimo mensaje). En este
   dispositivo (Samsung One UI) el dump NO incluye la ventana del IME, asi que
   todo lo que vive dentro del teclado se lee por pixeles.

2. PIXELES -> el IME (badge del ancla y panel). Especifico de este
   dispositivo/resolucion: no es una solucion universal. SI CAMBIA el layout,
   los colores de tokens, la densidad o se prueba en otro equipo, recalibrar
   las constantes de la seccion CALIBRACION.

   - Badge del ancla: texto color feedback/warning-on-subtle = amber/700
     #B45309 (180,83,9), pill sobre el strip, esquina superior del teclado.
   - Panel: fondo surface/panel = neutral/50 #F7F8FA (247,248,250). El panel
     reemplaza el QWERTY y se detecta como el cluster de filas panel-colored
     anclado al borde inferior del area del teclado.

PRECONDICIONES (por ahora manuales):
1. Telefono por USB con depuracion activada: `adb devices` debe listarlo.
2. WhatsApp Business (com.whatsapp.w4b) abierto en el chat de prueba, con la
   conversacion visible. El script hace self-check buscando el campo de entrada
   (com.whatsapp.w4b:id/entry) y aborta si no lo encuentra.
3. El IME Keyboard Sales esta activo y default. El teclado puede estar oculto:
   enfocar el campo dispara onInputView, donde se renderiza el badge.
4. El borrador del campo se limpia automaticamente antes de cada ronda.
5. El catalogo cargado es el dummy (el badge solo existe mientras esDummy).

Uso:
    python3 scripts/qa-vitrina.py [--rondas 3] [--trigger '#mesa']
"""

import argparse
import json
import re
import subprocess
import sys
import time
import xml.etree.ElementTree as ET
from datetime import datetime

from PIL import Image

# ---------------------------------------------------------------------------
# CALIBRACION — especifica del Galaxy A51 (1080x2400, density 420).
# Si cambia layout, tokens, densidad o dispositivo: revisar estas constantes.
# ---------------------------------------------------------------------------
SCREEN_PX = (1080, 2400)
DENSITY = 420.0  # wm density; se re-lee del dispositivo en runtime
# Badge: amber/700 #B45309 = (180,83,9). Tolerancia amplia, excluye azules.
BADGE_RGB = (180, 83, 9)
BADGE_TOL = (45, 45, 70)  # diff permitido por canal (b es limite alto: <70)
BADGE_SCAN_Y0 = 1100      # solo la mitad inferior (area del teclado)
# Panel: surface/panel neutral/50 #F7F8FA = (247,248,250).
PANEL_RGB = (247, 248, 250)
PANEL_TOL = 6             # diff por canal
PANEL_SCAN_Y0 = 1400      # desde aqui hacia abajo (fuera de la conversacion)
PANEL_BOTTOM_ANCHOR = 2380  # el cluster de filas debe llegar cerca del fondo
PANEL_GAP_TOL = 60        # px de tolerancia entre filas de contenido interno

ENTRY_RES = "com.whatsapp.w4b:id/entry"
MSG_TEXT_RES = "com.whatsapp.w4b:id/message_text"
PANEL_TITLE = "Catálogo"

KEYCODE_MOVE_END = "123"
KEYCODE_DEL = "67"
REPORT_PATH = "docs/QA-vitrina-automatizado.md"

PRECOND = (
    "No se encontro el campo de entrada de WhatsApp Business. Precondicion: "
    "tener WhatsApp Business (com.whatsapp.w4b) abierto en el chat de prueba "
    "con la conversacion visible, dispositivo por USB y depuracion activada."
)


class QaError(Exception):
    pass


def adb(*args):
    proc = subprocess.run(["adb"] + list(args), capture_output=True, text=True, timeout=40)
    if proc.returncode != 0:
        raise QaError(f"adb {' '.join(args)} fallo: {proc.stderr.strip()}")
    return proc.stdout


def screenshot():
    p = subprocess.run(["adb", "exec-out", "screencap", "-p"], capture_output=True, timeout=40)
    if p.returncode != 0:
        raise QaError("screencap fallo")
    img = Image.open(__import__("io").BytesIO(p.stdout)).convert("RGB")
    if img.size != SCREEN_PX:
        raise QaError(f"pantalla {img.size} != {SCREEN_PX}: recalibrar CALIBRACION")
    return img


# --------------------------- lectura del IME por pixeles --------------------


def badge_center(img):
    """Centro del pill del ancla. Devuelve (x,y) o None. Especifico de color."""
    w, h = img.size
    pts = []
    r0, g0, b0 = BADGE_RGB
    tr, tg, tb = BADGE_TOL
    for y in range(BADGE_SCAN_Y0, h, 2):
        for x in range(0, w, 2):
            r, g, b = img.getpixel((x, y))
            if abs(r - r0) < tr and abs(g - g0) < tg and b < tb:
                pts.append((x, y))
    if not pts:
        return None
    xs = [p[0] for p in pts]
    ys = [p[1] for p in pts]
    return (min(xs) + max(xs)) // 2, (min(ys) + max(ys)) // 2


def _panel_rows(img):
    w, h = img.size
    r0, g0, b0 = PANEL_RGB
    rows = []
    for y in range(PANEL_SCAN_Y0, PANEL_BOTTOM_ANCHOR):
        row = [img.getpixel((x, y)) for x in range(0, w, 8)]
        n = sum(1 for r, g, b in row if abs(r - r0) < PANEL_TOL and abs(g - g0) < PANEL_TOL and abs(b - b0) < PANEL_TOL)
        if n > len(row) * 0.08:
            rows.append(y)
    return rows


def panel_bounds(img):
    """(top, bottom, height_px) del panel, o None. Anclado al fondo del area del
    teclado: tolera las filas de contenido con otro fondo dentro del panel."""
    rows = _panel_rows(img)
    if not rows or rows[-1] < PANEL_BOTTOM_ANCHOR - 250:
        return None
    bottom = rows[-1]
    top = bottom
    prev = bottom
    for y in reversed(rows[:-1]):
        if prev - y <= PANEL_GAP_TOL:
            top = y
            prev = y
        else:
            break
    return top, bottom, bottom - top


def panel_has_content(img, bounds):
    """Verificacion barata de que el panel renderizo filas (no esta vacio):
    el area del panel debe tener varias filas con texto/contorno."""
    top, bottom, _ = bounds
    w = img.size[0]
    rows_with_text = 0
    for y in range(top, bottom, 8):
        row = [img.getpixel((x, y)) for x in range(0, w, 8)]
        edges = sum(
            1
            for a, b in zip(row, row[1:])
            if abs(a[0] - b[0]) + abs(a[1] - b[1]) + abs(a[2] - b[2]) > 60
        )
        if edges > 25:
            rows_with_text += 1
    return rows_with_text >= 3


def ime_shown():
    return "mInputShown=true" in adb("shell", "dumpsys", "input_method")


# --------------------------- lectura de WhatsApp por uiautomator -------------


def dump_ui():
    for _ in range(3):
        out = adb("exec-out", "uiautomator", "dump", "/dev/tty")
        start = out.find("<?xml")
        end = out.find("</hierarchy>")
        if start != -1 and end != -1:
            xml_part = out[start:end + len("</hierarchy>")]
            try:
                return ET.fromstring(xml_part)
            except ET.ParseError:
                time.sleep(0.2)
    raise QaError("no se pudo obtener un dump valido de uiautomator")


def nodes(root, **attrs):
    found = []
    for n in root.iter("node"):
        if all(n.get(k.replace("_", "-")) == v for k, v in attrs.items()):
            found.append(n)
    return found


def bounds(node):
    b = node.get("bounds") or ""
    m = re.match(r"\[(\d+),(\d+)\]\[(\d+),(\d+)\]", b)
    if not m:
        return None
    l, t, r, b_ = map(int, m.groups())
    return l, t, r, b_


def center(b):
    return (b[0] + b[2]) // 2, (b[1] + b[3]) // 2


def tap(x, y):
    adb("shell", "input", "tap", str(x), str(y))


def keyevents(*codes):
    adb("shell", "input", "keyevent", *codes)


def send_text(s):
    # comillas simples para el shell remoto: "#mesa" iniciaria un comentario.
    adb("shell", "input", "text", "'" + s + "'")


def wait_for_pixel(probe, timeout_s=12.0, poll_s=0.25):
    deadline = time.time() + timeout_s
    while time.time() < deadline:
        found = probe()
        if found:
            return found
        time.sleep(poll_s)
    return None


def dp_of(px, density):
    return px * 160.0 / density


def clear_entry():
    root = dump_ui()
    entry = nodes(root, resource_id=ENTRY_RES)
    if not entry:
        raise QaError(PRECOND)
    b = bounds(entry[0])
    # tocar el borde derecho del campo deja el cursor al final del texto (si se
    # toca el centro y hay un borrador largo, el cursor queda a mitad).
    tap(b[2] - 15, (b[1] + b[3]) // 2)
    time.sleep(0.6)
    for _ in range(15):
        keyevents(KEYCODE_MOVE_END)
        keyevents(*([KEYCODE_DEL] * 10))
        time.sleep(0.15)
        entry = nodes(dump_ui(), resource_id=ENTRY_RES)
        txt = entry[0].get("text") if entry else ""
        # un campo vacio expone el hint ("Mensaje") como text en el dump.
        if not txt or txt == "Mensaje":
            return
    entry = nodes(dump_ui(), resource_id=ENTRY_RES)
    txt = entry[0].get("text") if entry else "?"
    if txt and txt != "Mensaje":
        print(f"  aviso: el campo no quedo vacio (quedo {txt!r}); sigo igualmente")


def last_message_bottom():
    root = dump_ui()
    msgs = nodes(root, resource_id=MSG_TEXT_RES)
    bottoms = [bounds(n)[3] for n in msgs if bounds(n)]
    return max(bottoms) if bottoms else None


def one_round(round_no, density, trigger):
    print(f"  [ronda {round_no}] limpiando campo y enfocando...")
    clear_entry()

    print(f"  [ronda {round_no}] escribiendo trigger {trigger!r}...")
    send_text(trigger)
    t0 = time.time()
    # regresion del crash: escribir el trigger dispara showBar; el badge debe
    # seguir visible y el IME vivo. t_badge es ~0: el badge es un dummy estatico
    # (no gateado por matching), no representa latencia real del motor.
    img = screenshot()
    badge = wait_for_pixel(lambda: badge_center(screenshot()))
    t_badge_ms = int((time.time() - t0) * 1000)
    if badge is None:
        raise QaError("no se encontro el badge del ancla (¿se cayo el IME o cambio el layout?)")
    if not ime_shown():
        raise QaError("el IME no esta mostrado tras escribir el trigger (¿crash?)")

    bb = badge
    print(f"  [ronda {round_no}] badge en {bb}, tap, t_badge={t_badge_ms}ms")
    tap(*bb)

    t1 = time.time()
    pb = wait_for_pixel(lambda: panel_bounds(screenshot()))
    t_panel_ms = int((time.time() - t1) * 1000)
    if pb is None:
        raise QaError(f"ronda {round_no}: no se abrio el panel (tap en {bb})")

    panel_top_px, panel_bottom_px, _ = pb
    img = screenshot()
    content_ok = panel_has_content(img, pb)
    if not content_ok:
        print(f"  [ronda {round_no}] aviso: el panel no muestra filas de contenido")

    last_msg_bottom_px = last_message_bottom()

    panel_top_dp = dp_of(panel_top_px, density)
    panel_bottom_dp = dp_of(panel_bottom_px, density)
    panel_height_dp = dp_of(pb[2], density)
    last_msg_dp = dp_of(last_msg_bottom_px, density) if last_msg_bottom_px is not None else None

    if last_msg_bottom_px is not None:
        overlap = last_msg_bottom_px > panel_top_px
        margin_dp = dp_of(panel_top_px - last_msg_bottom_px, density)
    else:
        overlap, margin_dp = None, None

    print(
        f"  [ronda {round_no}] panel en {t_panel_ms}ms; top={panel_top_dp:.0f}dp "
        f"bottom={panel_bottom_dp:.0f}dp alto={panel_height_dp:.0f}dp; "
        f"ultimo_msg_bottom={last_msg_dp:.0f}dp overlap={overlap}"
    )

    print(f"  [ronda {round_no}] cerrando panel...")
    img = screenshot()
    badge2 = badge_center(img)
    if badge2:
        tap(*badge2)
    closed = wait_for_pixel(lambda: panel_bounds(screenshot()) is None, timeout_s=8)
    if not closed:
        print(f"  [ronda {round_no}] aviso: el panel no se cerro a tiempo")

    return {
        "ronda": round_no,
        "t_badge_ms": t_badge_ms,
        "t_panel_ms": t_panel_ms,
        "panel_top_dp": round(panel_top_dp, 1),
        "panel_bottom_dp": round(panel_bottom_dp, 1),
        "panel_height_dp": round(panel_height_dp, 1),
        "last_msg_bottom_dp": round(last_msg_dp, 1) if last_msg_dp is not None else None,
        "overlap": overlap,
        "margin_dp": round(margin_dp, 1) if margin_dp is not None else None,
        "panel_content": content_ok,
    }


def device_info():
    model = adb("shell", "getprop", "ro.product.model").strip()
    density = DENSITY
    m = re.search(r"(\d+)", adb("shell", "wm", "density"))
    if m:
        density = float(m.group(1))
    size = "?"
    m2 = re.search(r"\d+x\d+", adb("shell", "wm", "size"))
    if m2:
        size = m2.group(0)
    return model, density, size


def write_report(model, density, size, trigger, rounds):
    now = datetime.now().strftime("%Y-%m-%d %H:%M:%S")
    lines = []
    lines.append("")
    lines.append(f"## Corrida QA Vitrina V1 — {now}")
    lines.append("")
    lines.append(
        f"- Dispositivo: **{model}** (Galaxy A51) · density {density:.0f} · "
        f"pantalla {size} · trigger `{trigger}` · chat WhatsApp Business"
    )
    lines.append("")
    lines.append(
        "> **Medicion del IME por pixeles, especifica de este dispositivo.** En "
        "este A51 (One UI) `uiautomator dump` no incluye la ventana del IME, asi "
        "que badge y panel se leen por posicion y color de pixel "
        "(`scripts/qa-vitrina.py`, seccion CALIBRACION): badge = amber/700 "
        "`#B45309`, panel = `surface/panel` `#F7F8FA`. **No es una solucion "
        "universal**: si cambia el layout, los colores de tokens, la densidad o "
        "se prueba en otro equipo, hay que recalibrar esas constantes."
    )
    lines.append("")
    lines.append(
        "> **t_badge**: mide desde el trigger hasta la deteccion del badge en "
        "pixeles; como el badge es un dummy estatico (no gateado por matching "
        "real), se espera ~0ms — es un chequeo de regresion (el IME no se cayo "
        "al escribir el trigger, ver commit `3dc69b23`), no la latencia real "
        "del motor de sugerencias."
    )
    lines.append("")
    lines.append("| Ronda | t_badge (ms) | t_panel (ms) | panel top (dp) | panel bottom (dp) | alto panel (dp) | ultimo msg bottom (dp) | solape | margen (dp) | contenido |")
    lines.append("|---|---|---|---|---|---|---|---|---|---|---|")
    for r in rounds:
        err = r.get("error")
        if err:
            lines.append(f"| {r['ronda']} | ERROR | | | | | | | | | `{err}` |")
            continue
        om = r.get("overlap")
        solape = "SI" if om else ("NO" if om is False else "n/a")
        margen = r.get("margin_dp") if r.get("margin_dp") is not None else "n/a"
        lb = r.get("last_msg_bottom_dp") if r.get("last_msg_bottom_dp") is not None else "n/a"
        cont = "SI" if r.get("panel_content") else "no"
        lines.append(
            f"| {r['ronda']} | {r['t_badge_ms']} | {r['t_panel_ms']} | "
            f"{r['panel_top_dp']} | {r['panel_bottom_dp']} | {r['panel_height_dp']} | "
            f"{lb} | {solape} | {margen} | {cont} |"
        )
    lines.append("")
    lines.append(
        "> **Alto del panel**: medido en px por el cluster de filas `surface/panel`, "
        "convertido a dp. La referencia es el token `kb/panel/height` = 192dp "
        "(**derivado**: `kb/row/height` × `kb/row/count` + 2 × `kb/pad/v` = "
        "46×4+8; `design/tokens.json:187,218` — valor derivado, no spec de "
        "04.10 que este en el repo)."
    )
    lines.append("")
    lines.append("---")
    lines.append("")
    with open(REPORT_PATH, "a", encoding="utf-8") as f:
        f.write("\n".join(lines))
    print(f"reporte: {REPORT_PATH}")


def main():
    parser = argparse.ArgumentParser(description="QA automatizado de Vitrina V1 en dispositivo real")
    parser.add_argument("--rondas", type=int, default=3)
    parser.add_argument("--trigger", default="#mesa")
    args = parser.parse_args()

    model, density, size = device_info()
    print(f"dispositivo: {model} · density {density:.0f} · pantalla {size}")
    print(f"calibracion: badge {BADGE_RGB} tol {BADGE_TOL}, panel {PANEL_RGB} tol {PANEL_TOL}")

    rounds = []
    for r in range(1, args.rondas + 1):
        print(f"--- ronda {r}/{args.rondas} ---")
        try:
            data = one_round(r, density, args.trigger)
        except QaError as e:
            print(f"ERROR: {e}")
            data = {"ronda": r, "error": str(e)}
        rounds.append(data)
        print(json.dumps(data, ensure_ascii=False))

    write_report(model, density, size, args.trigger, rounds)
    if any("error" in r for r in rounds):
        sys.exit(1)


if __name__ == "__main__":
    main()