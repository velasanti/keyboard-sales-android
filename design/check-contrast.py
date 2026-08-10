#!/usr/bin/env python3
"""
check-contrast.py — verifica el contraste de todos los pares declarados en tokens.json,
en los dos modos, y falla con codigo != 0 si alguno no cumple.

Esto NO es una recomendacion en una nota: es un paso de CI.
Ver "04.2 Foundations" seccion 10.1 y seccion 12.

Uso:
    python3 check-contrast.py [ruta/a/tokens.json]
    python3 check-contrast.py --verbose      # imprime todos los pares, no solo los fallos
"""

import json
import sys
from pathlib import Path

MODES = ("light", "dark")


# ---------- color ----------

def hex_to_rgb(h):
    h = h.lstrip("#")
    if len(h) == 3:
        h = "".join(c * 2 for c in h)
    return tuple(int(h[i:i + 2], 16) for i in (0, 2, 4))


def composite(fg_rgb, alpha, bg_rgb):
    """Alpha compositing en sRGB de 8 bits, que es lo que hace el compositor del sistema."""
    return tuple(round(f * alpha + b * (1 - alpha)) for f, b in zip(fg_rgb, bg_rgb))


def relative_luminance(rgb):
    def channel(c):
        c = c / 255.0
        return c / 12.92 if c <= 0.03928 else ((c + 0.055) / 1.055) ** 2.4
    r, g, b = (channel(c) for c in rgb)
    return 0.2126 * r + 0.7152 * g + 0.0722 * b


def contrast_ratio(rgb_a, rgb_b):
    la, lb = relative_luminance(rgb_a), relative_luminance(rgb_b)
    lighter, darker = max(la, lb), min(la, lb)
    return (lighter + 0.05) / (darker + 0.05)


# ---------- resolucion de tokens ----------

class Resolver:
    """Resuelve un token semantico a un RGB concreto en un modo dado.

    Los primitivos alpha se compositan sobre el color de fondo que corresponda,
    que es lo que hace el sistema al pintar. Sin esto, un borde de ink-a/12
    daria un contraste falso.
    """

    def __init__(self, tokens):
        self.primitives = tokens["primitives"]
        self.semantic = tokens["semantic"]
        self.errors = []

    def primitive_rgb(self, name, mode, backdrop_rgb):
        if name not in self.primitives:
            raise KeyError(f"primitivo inexistente: {name}")
        value = self.primitives[name]
        if isinstance(value, dict):  # alpha
            return composite(hex_to_rgb(value["base"]), value["alpha"], backdrop_rgb)
        return hex_to_rgb(value)

    def semantic_rgb(self, token, mode, backdrop_rgb=None):
        if token not in self.semantic:
            raise KeyError(f"semantico inexistente: {token}")
        entry = self.semantic[token]
        for m in MODES:
            if m not in entry:
                raise KeyError(f"'{token}' no define el modo '{m}' — regla 2 del pipeline")
        if backdrop_rgb is None:
            # Un token alpha sin fondo conocido se compone sobre el fondo de app del modo.
            backdrop_rgb = self.semantic_rgb(
                "surface/app" if token != "surface/app" else "surface/keyboard", mode,
                (255, 255, 255) if mode == "light" else (0, 0, 0)
            )
        return self.primitive_rgb(entry[mode], mode, backdrop_rgb)


# ---------- verificacion ----------

def main():
    args = [a for a in sys.argv[1:] if not a.startswith("--")]
    verbose = "--verbose" in sys.argv or "-v" in sys.argv
    path = Path(args[0]) if args else Path(__file__).parent / "tokens.json"

    tokens = json.loads(path.read_text(encoding="utf-8"))
    r = Resolver(tokens)
    pairs = tokens["a11y"]["contrastPairs"]
    exempt = set(tokens["a11y"].get("exempt", []))

    failures = []
    checked = 0

    print(f"check-contrast · {path.name} · {len(pairs)} pares × {len(MODES)} modos\n")

    for pair in pairs:
        fg_name, bg_name, minimum = pair["fg"], pair["bg"], pair["min"]
        if fg_name in exempt or bg_name in exempt:
            continue
        for mode in MODES:
            checked += 1
            try:
                bg_rgb = r.semantic_rgb(bg_name, mode)
                fg_rgb = r.semantic_rgb(fg_name, mode, backdrop_rgb=bg_rgb)
            except KeyError as e:
                failures.append((mode, fg_name, bg_name, None, minimum, str(e)))
                continue
            ratio = contrast_ratio(fg_rgb, bg_rgb)
            ok = ratio >= minimum
            if not ok:
                failures.append((mode, fg_name, bg_name, ratio, minimum, pair.get("why", "")))
            if verbose or not ok:
                mark = "ok  " if ok else "FALLA"
                print(f"  [{mode:5}] {mark} {ratio:5.2f}:1 (min {minimum})  "
                      f"{fg_name} sobre {bg_name}")

    # Regla 3 del pipeline: un primitivo sin uso rompe el build.
    used = set()
    for entry in tokens["semantic"].values():
        for mode in MODES:
            if mode in entry:
                used.add(entry[mode])
    for elev in tokens.get("elevation", {}).values():
        if isinstance(elev, dict) and "color" in elev:
            used.add(elev["color"])
    orphans = sorted(set(tokens["primitives"]) - used)

    print()
    if orphans:
        print(f"AVISO · {len(orphans)} primitivos sin uso en ningun semantico:")
        for o in orphans:
            print(f"    {o}")
        print("  Regla 3 del pipeline (04.2 §12): la paleta no crece sin uso.")
        print("  Es como Vyn llego a 136 tokens para tener que limpiarlos a 99.\n")

    if failures:
        print(f"RESULTADO: {len(failures)} de {checked} verificaciones FALLAN.\n")
        for mode, fg, bg, ratio, minimum, why in failures:
            got = f"{ratio:.2f}:1" if ratio else "no resuelto"
            print(f"  [{mode}] {fg} sobre {bg} → {got}, exige {minimum}:1")
            if why:
                print(f"          {why}")
        return 1

    print(f"RESULTADO: {checked} verificaciones OK en {len(MODES)} modos.")
    return 2 if orphans else 0


if __name__ == "__main__":
    sys.exit(main())
