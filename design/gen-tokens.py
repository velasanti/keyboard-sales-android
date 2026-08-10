#!/usr/bin/env python3
"""
gen-tokens.py — genera los archivos de tokens de Android e iOS desde tokens.json.

tokens.json es la UNICA fuente de verdad. Los archivos que este script escribe
NO se editan a mano: llevan cabecera de generado y el CI compara el output contra
lo commiteado.

Reglas que hace cumplir (04.2 Foundations seccion 12):
  1. Los archivos generados no se editan a mano.
  2. Un semantico sin sus dos modos rompe el generador.
  3. Un primitivo sin uso en ningun semantico rompe el generador.
  4. Ningun color literal en codigo (lo verifica check-literals.py, no este script).

Uso:
    python3 gen-tokens.py --out ..            # escribe en los repos
    python3 gen-tokens.py --check --out ..    # falla si algo difiere de lo commiteado
"""

import argparse
import json
import sys
from pathlib import Path

HEADER_SLASH = "// GENERATED FROM design/tokens.json — DO NOT EDIT\n// Regenerar: python3 design/gen-tokens.py --out .\n"
HEADER_XML = ("<!-- GENERATED FROM design/tokens.json — DO NOT EDIT -->\n"
              "<!-- Regenerar: python3 design/gen-tokens.py --out . -->\n")

MODES = ("light", "dark")


# ---------- utilidades ----------

def snake(name):
    """accent/on-subtle -> accent_on_subtle"""
    return name.replace("/", "_").replace("-", "_").replace(".", "_")


def camel(name):
    """accent/on-subtle -> accentOnSubtle"""
    parts = name.replace("/", "_").replace("-", "_").split("_")
    return parts[0] + "".join(p.capitalize() for p in parts[1:])


def resolve_hex(primitives, alias):
    """Devuelve #AARRGGBB para Android / (hex, alpha) para Swift."""
    value = primitives[alias]
    if isinstance(value, dict):
        base = value["base"].lstrip("#")
        alpha = int(round(value["alpha"] * 255))
        return f"#{alpha:02X}{base.upper()}", value["base"], value["alpha"]
    h = value.lstrip("#").upper()
    return f"#FF{h}", value, 1.0


def validate(tokens):
    errors = []
    prims, sems = tokens["primitives"], tokens["semantic"]

    # Regla 2
    for name, entry in sems.items():
        for mode in MODES:
            if mode not in entry:
                errors.append(f"[regla 2] '{name}' no define el modo '{mode}'")
            elif entry[mode] not in prims:
                errors.append(f"[alias roto] '{name}'.{mode} apunta a '{entry[mode]}', que no es un primitivo")

    # Regla 3
    used = {entry[m] for entry in sems.values() for m in MODES if m in entry}
    used |= {e["color"] for e in tokens.get("elevation", {}).values()
             if isinstance(e, dict) and "color" in e}
    for orphan in sorted(set(prims) - used):
        errors.append(f"[regla 3] primitivo huerfano, sin uso en ningun semantico: '{orphan}'")

    return errors


# ---------- generadores ----------

def gen_android_colors(tokens, mode):
    prims = tokens["primitives"]
    lines = [HEADER_XML, "<resources>\n"]
    for name, entry in tokens["semantic"].items():
        argb, _, _ = resolve_hex(prims, entry[mode])
        lines.append(f'    <color name="{snake(name)}">{argb}</color>\n')
    lines.append("</resources>\n")
    return "".join(lines)


def gen_android_dimens(tokens):
    lines = [HEADER_XML, "<resources>\n"]
    for name, value in tokens["dimension"].items():
        if name.endswith("row/count"):
            lines.append(f'    <integer name="{snake(name)}">{int(value)}</integer>\n')
        elif name.startswith("z/"):
            lines.append(f'    <integer name="{snake(name)}">{int(value)}</integer>\n')
        else:
            lines.append(f'    <dimen name="{snake(name)}">{value}dp</dimen>\n')
    lines.append("\n    <!-- Tipografia: tamanos en sp, respetan la escala del sistema -->\n")
    for name, t in tokens["typography"].items():
        if name.startswith("$"):
            continue
        lines.append(f'    <dimen name="{snake(name)}_size">{t["size"]}sp</dimen>\n')
        if t.get("lineHeight"):
            lines.append(f'    <dimen name="{snake(name)}_line_height">{t["lineHeight"]}sp</dimen>\n')
    lines.append("\n    <!-- Motion en ms -->\n")
    for name, value in tokens["motion"].items():
        if isinstance(value, (int, float)):
            lines.append(f'    <integer name="{snake(name)}">{int(value)}</integer>\n')
    lines.append("</resources>\n")
    return "".join(lines)


def gen_compose(tokens):
    prims = tokens["primitives"]
    out = [HEADER_SLASH, "\npackage com.keyboardsales.ui.theme\n\n",
           "import androidx.compose.runtime.Composable\n",
           "import androidx.compose.runtime.Immutable\n",
           "import androidx.compose.runtime.ReadOnlyComposable\n",
           "import androidx.compose.runtime.compositionLocalOf\n",
           "import androidx.compose.ui.graphics.Color\n",
           "import androidx.compose.ui.unit.dp\n",
           "import androidx.compose.ui.unit.sp\n\n"]

    out.append("/** Escalas de dimension. No dependen del modo. */\nobject Dim {\n")
    for name, value in tokens["dimension"].items():
        if name.endswith("row/count") or name.startswith("z/"):
            out.append(f"    val {camel(name)} = {int(value)}\n")
        else:
            out.append(f"    val {camel(name)} = {value}.dp\n")
    out.append("}\n\n")

    out.append("/** Duraciones de motion, en milisegundos. */\nobject Motion {\n")
    for name, value in tokens["motion"].items():
        if isinstance(value, (int, float)):
            out.append(f"    const val {camel(name)} = {int(value)}\n")
    out.append("}\n\n")

    out.append("/** Estilos tipograficos. Familia del sistema: sin fuente propia (04.2 §5.1). */\nobject Type {\n")
    for name, t in tokens["typography"].items():
        if name.startswith("$"):
            continue
        lh = f"{t['lineHeight']}.sp" if t.get("lineHeight") else "androidx.compose.ui.unit.TextUnit.Unspecified"
        out.append(f"    // {name}\n")
        out.append(f"    val {camel(name)}Size = {t['size']}.sp\n")
        out.append(f"    val {camel(name)}Weight = {t['weight']}\n")
        out.append(f"    val {camel(name)}LineHeight = {lh}\n")
    out.append("}\n\n")

    out.append("@Immutable\ndata class AppColors(\n")
    for name in tokens["semantic"]:
        out.append(f"    val {camel(name)}: Color,\n")
    out.append(")\n\n")

    for mode in MODES:
        out.append(f"val {mode.capitalize()}Colors = AppColors(\n")
        for name, entry in tokens["semantic"].items():
            argb, _, _ = resolve_hex(prims, entry[mode])
            out.append(f"    {camel(name)} = Color(0x{argb.lstrip('#')}),\n")
        out.append(")\n\n")

    out.append("""val LocalAppColors = compositionLocalOf { LightColors }

/** Punto de acceso unico al color. Nunca se escribe un Color(0x...) en un componente. */
object AppTheme {
    val colors: AppColors
        @Composable @ReadOnlyComposable get() = LocalAppColors.current
}
""")
    return "".join(out)


def gen_swift(tokens):
    prims = tokens["primitives"]
    out = [HEADER_SLASH, "\nimport SwiftUI\n\n",
           "// Familia tipografica del sistema: sin fuente propia (04.2 §5.1).\n\n"]

    out.append("public enum Dim {\n")
    for name, value in tokens["dimension"].items():
        if name.endswith("row/count") or name.startswith("z/"):
            out.append(f"    public static let {camel(name)}: Int = {int(value)}\n")
        else:
            out.append(f"    public static let {camel(name)}: CGFloat = {value}\n")
    out.append("}\n\n")

    out.append("public enum Motion {\n")
    for name, value in tokens["motion"].items():
        if isinstance(value, (int, float)):
            out.append(f"    public static let {camel(name)}: TimeInterval = {value / 1000.0}\n")
    out.append("}\n\n")

    out.append("public enum Typo {\n")
    for name, t in tokens["typography"].items():
        if name.startswith("$"):
            continue
        weight = {400: ".regular", 500: ".medium", 600: ".semibold", 700: ".bold"}[t["weight"]]
        out.append(f"    /// {name}\n")
        out.append(f"    public static let {camel(name)} = Font.system(size: {t['size']}, weight: {weight})\n")
    out.append("}\n\n")

    out.append("public enum Tokens {\n")
    for name, entry in tokens["semantic"].items():
        light_argb, _, la = resolve_hex(prims, entry["light"])
        dark_argb, _, da = resolve_hex(prims, entry["dark"])
        out.append(f"    /// {name} · light {entry['light']} · dark {entry['dark']}\n")
        out.append(f"    public static let {camel(name)} = Color(\n")
        out.append(f"        light: 0x{light_argb.lstrip('#')[2:]}, lightAlpha: {la},\n")
        out.append(f"        dark: 0x{dark_argb.lstrip('#')[2:]}, darkAlpha: {da}\n    )\n")
    out.append("}\n\n")

    out.append("""extension Color {
    /// Un solo Color que resuelve por modo del sistema. Light y dark son pares (04.2 §3.1).
    init(light: UInt32, lightAlpha: Double, dark: UInt32, darkAlpha: Double) {
        self.init(UIColor { traits in
            let isDark = traits.userInterfaceStyle == .dark
            let hex = isDark ? dark : light
            let alpha = isDark ? darkAlpha : lightAlpha
            return UIColor(
                red:   CGFloat((hex >> 16) & 0xFF) / 255.0,
                green: CGFloat((hex >> 8) & 0xFF) / 255.0,
                blue:  CGFloat(hex & 0xFF) / 255.0,
                alpha: CGFloat(alpha)
            )
        })
    }
}
""")
    return "".join(out)


# ---------- main ----------

def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("--out", default="..", help="raiz donde viven los repos")
    ap.add_argument("--tokens", default=None)
    ap.add_argument("--check", action="store_true",
                    help="no escribe: falla si el output difiere de lo commiteado")
    ap.add_argument("--platform", choices=["android", "ios", "all"], default="all",
                    help="limita a los archivos de una plataforma Y hace que las rutas "
                         "sean relativas a la raiz del repo de esa plataforma, quitando "
                         "el prefijo 'android/' o 'ios/'. Es lo que hay que usar dentro "
                         "de keyboard-sales-android o keyboard-sales-ios. Sin esto, "
                         "--check reporta deriva falsa por los archivos de la otra.")
    args = ap.parse_args()

    tpath = Path(args.tokens) if args.tokens else Path(__file__).parent / "tokens.json"
    tokens = json.loads(tpath.read_text(encoding="utf-8"))

    errors = validate(tokens)
    if errors:
        print("gen-tokens: tokens.json NO es valido.\n")
        for e in errors:
            print(f"  {e}")
        print("\nVer 04.2 Foundations §12 para las reglas del pipeline.")
        return 1

    root = Path(args.out)

    # Rutas reales del repo Android, verificadas contra el fork el 2026-08-10.
    # HeliBoard es un proyecto de UN SOLO modulo (settings.gradle: include ':app'),
    # asi que no existe un modulo 'keyboard/': el IME ES ':app'. La estructura de
    # dos modulos que estaba aca antes venia de 06.3 Repositorios y la invalido
    # ADR-012; apuntar a keyboard/ creaba un modulo fantasma que Gradle no compila.
    targets = {
        "android/app/src/main/res/values/tokens_colors.xml":       gen_android_colors(tokens, "light"),
        "android/app/src/main/res/values-night/tokens_colors.xml": gen_android_colors(tokens, "dark"),
        "android/app/src/main/res/values/tokens_dimens.xml":       gen_android_dimens(tokens),
        "ios/Shared/Design/Tokens.swift":                          gen_swift(tokens),
    }

    # Tokens.kt importa androidx.compose.*, asi que solo compila si el modulo tiene
    # Compose habilitado. VERIFICADO el 2026-08-10 contra app/build.gradle.kts del
    # fork: upstream ya lo trae — kotlin("plugin.compose") 2.3.20, compose = true,
    # compose-bom 2025.11.01, material3 y navigation-compose. Asi que se emite
    # siempre y no hace falta editar ningun archivo de upstream para habilitarlo.
    targets["android/app/src/main/java/com/keyboardsales/ui/theme/Tokens.kt"] = gen_compose(tokens)

    # En multi-repo cada repo solo tiene su plataforma, y su raiz ES la raiz del
    # repo de esa plataforma. Asi que --platform hace dos cosas:
    #   1. filtra los objetivos de la otra plataforma (sin esto, --check dentro de
    #      keyboard-sales-android reporta deriva por ios/Shared/Design/Tokens.swift,
    #      que en ese repo no existe ni tiene por que existir);
    #   2. QUITA el prefijo 'android/' o 'ios/' de las rutas, porque dentro del repo
    #      de Android el destino es keyboard/... y no android/keyboard/...
    # Sin el punto 2, "--out ." crea una carpeta android/ espuria en la raiz del repo.
    if args.platform != "all":
        prefijo = args.platform + "/"
        targets = {k[len(prefijo):]: v for k, v in targets.items() if k.startswith(prefijo)}
        if not targets:
            print(f"gen-tokens: no hay objetivos para la plataforma '{args.platform}'.")
            return 1

    drift = []
    for rel, content in targets.items():
        path = root / rel
        if args.check:
            if not path.exists():
                drift.append(f"{rel} (no existe)")
            elif path.read_text(encoding="utf-8") != content:
                drift.append(f"{rel} (difiere)")
        else:
            path.parent.mkdir(parents=True, exist_ok=True)
            path.write_text(content, encoding="utf-8")
            print(f"  escrito  {rel}  ({len(content.splitlines())} lineas)")

    if args.check:
        if drift:
            print("gen-tokens --check: los archivos generados estan desincronizados.\n")
            for d in drift:
                print(f"  {d}")
            print("\nCorrer: python3 design/gen-tokens.py --out . y commitear.")
            return 1
        print(f"gen-tokens --check: {len(targets)} archivos sincronizados con tokens.json.")
        return 0

    sem, prim = len(tokens["semantic"]), len(tokens["primitives"])
    print(f"\ngen-tokens: OK. {prim} primitivos → {sem} semanticos × 2 modos → "
          f"{len(targets)} archivos.")
    return 0


if __name__ == "__main__":
    sys.exit(main())
