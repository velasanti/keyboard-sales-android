#!/usr/bin/env python3
"""
check-literals.py — hace cumplir la regla 4 del pipeline de tokens:
NINGUN color literal ni dimension literal en codigo, fuera de los archivos generados.

Ver "04.2 Foundations" seccion 12, regla 4. Hasta que existio este script la regla
estaba escrita y no se verificaba en ningun lado, que es la peor de las dos opciones:
da la sensacion de estar cubierto sin estarlo.

Uso:
    python3 check-literals.py .                    # todo el repo
    python3 check-literals.py . --dimensions       # ademas, dimensiones sospechosas
    python3 check-literals.py . --baseline .literals-baseline
"""

import argparse
import re
import sys
from pathlib import Path

# Archivos generados por gen-tokens.py: son los UNICOS que pueden tener literales.
GENERATED = (
    "tokens_colors.xml",
    "tokens_dimens.xml",
    "Tokens.kt",
    "Tokens.swift",
)

SKIP_DIRS = {
    ".git", "build", ".gradle", ".idea", "node_modules", "Pods",
    ".build", "DerivedData", "design", "vendor", ".venv",
}

# Extensiones donde un color literal es un problema.
CODE_EXT = {".kt", ".java", ".swift", ".xml"}

# #RGB, #RRGGBB, #AARRGGBB
HEX_COLOR = re.compile(r"#(?:[0-9A-Fa-f]{3,4}|[0-9A-Fa-f]{6}|[0-9A-Fa-f]{8})\b")
# Color(0xFF...), UIColor(red:...), Color.rgb(...)
PROG_COLOR = re.compile(
    r"Color\s*\(\s*0x[0-9A-Fa-f]{6,8}"
    r"|UIColor\s*\(\s*(?:red|white)\s*:"
    r"|Color\s*\.\s*rgb\s*\("
    r"|android\.graphics\.Color\.(?:rgb|argb|parseColor)"
)
# 12.dp / 16dp / CGFloat literal en un modificador de layout
DIM_LITERAL = re.compile(
    r"\b\d{1,3}\s*\.\s*dp\b"
    r"|\"\d{1,3}dp\""
    r"|(?:padding|frame|height|width|cornerRadius|spacing)\s*\(\s*\d{1,3}(?:\.\d+)?\s*\)"
)

# Excepciones legitimas: 0 y 1 no son decisiones de diseño; los avisos de licencia
# y los comentarios de referencia a un token si pueden nombrar un valor.
ALLOWED_DIM = {"0", "1"}
COMMENT_PREFIXES = ("//", "*", "/*", "<!--", "#")


def is_generated(path: Path) -> bool:
    return path.name in GENERATED


def is_comment(line: str) -> bool:
    return line.strip().startswith(COMMENT_PREFIXES)


def scan(root: Path, check_dims: bool, baseline: set):
    findings = []
    for path in root.rglob("*"):
        if not path.is_file() or path.suffix not in CODE_EXT:
            continue
        if any(part in SKIP_DIRS for part in path.parts):
            continue
        if is_generated(path):
            continue
        rel = path.relative_to(root)
        try:
            lines = path.read_text(encoding="utf-8").splitlines()
        except UnicodeDecodeError:
            continue
        for n, line in enumerate(lines, 1):
            if is_comment(line):
                continue
            key = f"{rel}:{n}"
            if key in baseline:
                continue
            for m in HEX_COLOR.finditer(line):
                findings.append((rel, n, "color hex", m.group(0), line.strip()))
            for m in PROG_COLOR.finditer(line):
                findings.append((rel, n, "color programatico", m.group(0), line.strip()))
            if check_dims:
                for m in DIM_LITERAL.finditer(line):
                    num = re.search(r"\d+", m.group(0))
                    if num and num.group(0) in ALLOWED_DIM:
                        continue
                    findings.append((rel, n, "dimension literal", m.group(0), line.strip()))
    return findings


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("root", nargs="?", default=".")
    ap.add_argument("--dimensions", action="store_true",
                    help="ademas de color, marca dimensiones literales (mas ruidoso)")
    ap.add_argument("--baseline", default=None,
                    help="archivo con 'ruta:linea' por renglon, para deuda aceptada")
    args = ap.parse_args()

    root = Path(args.root).resolve()
    baseline = set()
    if args.baseline and Path(args.baseline).exists():
        baseline = {
            l.strip() for l in Path(args.baseline).read_text(encoding="utf-8").splitlines()
            if l.strip() and not l.startswith("#")
        }

    findings = scan(root, args.dimensions, baseline)

    if not findings:
        scope = "color y dimension" if args.dimensions else "color"
        print(f"check-literals: OK. Ningun literal de {scope} fuera de los archivos generados.")
        return 0

    print(f"check-literals: {len(findings)} literales fuera de los archivos generados.\n")
    print("Los valores de diseño salen de tokens.json via gen-tokens.py.")
    print("Ver 04.2 Foundations §12, regla 4.\n")
    for rel, n, kind, match, line in findings:
        print(f"  {rel}:{n}  [{kind}]  {match}")
        print(f"      {line[:100]}")
    if baseline:
        print(f"\n({len(baseline)} entradas ignoradas por el baseline.)")
    return 1


if __name__ == "__main__":
    sys.exit(main())
