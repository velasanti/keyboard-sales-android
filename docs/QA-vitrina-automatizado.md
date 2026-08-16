## Corrida QA Vitrina V1 — 2026-08-15 23:42:16

- Dispositivo: **SM-A515F** (Galaxy A51) · density 420 · pantalla 1080x2400 · trigger `#mesa` · chat WhatsApp Business

> **Medicion del IME por pixeles, especifica de este dispositivo.** En este A51 (One UI) `uiautomator dump` no incluye la ventana del IME, asi que badge y panel se leen por posicion y color de pixel (`scripts/qa-vitrina.py`, seccion CALIBRACION): badge = amber/700 `#B45309`, panel = `surface/panel` `#F7F8FA`. **No es una solucion universal**: si cambia el layout, los colores de tokens, la densidad o se prueba en otro equipo, hay que recalibrar esas constantes.

> **t_badge**: mide desde el trigger hasta la deteccion del badge en pixeles; como el badge es un dummy estatico (no gateado por matching real), se espera ~0ms — es un chequeo de regresion (el IME no se cayo al escribir el trigger, ver commit `3dc69b23`), no la latencia real del motor de sugerencias.

| Ronda | t_badge (ms) | t_panel (ms) | panel top (dp) | panel bottom (dp) | alto panel (dp) | ultimo msg bottom (dp) | solape | margen (dp) | contenido |
|---|---|---|---|---|---|---|---|---|---|
| 1 | 1989 | 980 | 674.3 | 865.9 | 191.6 | 571.4 | NO | 102.9 | SI |
| 2 | 2089 | 990 | 674.3 | 865.9 | 191.6 | 571.4 | NO | 102.9 | SI |
| 3 | 2018 | 1003 | 674.3 | 865.9 | 191.6 | 571.4 | NO | 102.9 | SI |

> **Alto del panel**: medido en px por el cluster de filas `surface/panel`, convertido a dp. La referencia es el token `kb/panel/height` = 192dp (**derivado**: `kb/row/height` × `kb/row/count` + 2 × `kb/pad/v` = 46×4+8; `design/tokens.json:187,218` — valor derivado, no spec de 04.10 que este en el repo).

---
## Corrida QA Vitrina V1 — 2026-08-16 01:16:23

- Dispositivo: **SM-A515F** (Galaxy A51) · density 420 · pantalla 1080x2400 · trigger `#mesa` · chat WhatsApp Business

> **Medicion del IME por pixeles, especifica de este dispositivo.** En este A51 (One UI) `uiautomator dump` no incluye la ventana del IME, asi que badge y panel se leen por posicion y color de pixel (`scripts/qa-vitrina.py`, seccion CALIBRACION): badge = amber/700 `#B45309`, panel = `surface/panel` `#F7F8FA`. **No es una solucion universal**: si cambia el layout, los colores de tokens, la densidad o se prueba en otro equipo, hay que recalibrar esas constantes.

> **t_badge**: mide desde el trigger hasta la deteccion del badge en pixeles; como el badge es un dummy estatico (no gateado por matching real), se espera ~0ms — es un chequeo de regresion (el IME no se cayo al escribir el trigger, ver commit `3dc69b23`), no la latencia real del motor de sugerencias.

| Ronda | t_badge (ms) | t_panel (ms) | panel top (dp) | panel bottom (dp) | alto panel (dp) | ultimo msg bottom (dp) | solape | margen (dp) | contenido |
|---|---|---|---|---|---|---|---|---|---|---|
| 1 | 2113 | 977 | 674.3 | 865.9 | 191.6 | 571.4 | NO | 102.9 | SI |
| 2 | 2046 | 968 | 674.3 | 865.9 | 191.6 | 571.4 | NO | 102.9 | SI |
| 3 | 2077 | 999 | 674.3 | 865.9 | 191.6 | 571.4 | NO | 102.9 | SI |

> **Alto del panel**: medido en px por el cluster de filas `surface/panel`, convertido a dp. La referencia es el token `kb/panel/height` = 192dp (**derivado**: `kb/row/height` × `kb/row/count` + 2 × `kb/pad/v` = 46×4+8; `design/tokens.json:187,218` — valor derivado, no spec de 04.10 que este en el repo).

---
