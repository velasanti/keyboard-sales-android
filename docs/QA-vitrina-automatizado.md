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

## Corrida QA Vitrina V1 — 2026-08-16 22:17:56

- Dispositivo: **SM-A515F** (Galaxy A51) · density 420 · pantalla 1080x2400 · trigger `#mesa` · chat WhatsApp Business

> **Medicion del IME por pixeles, especifica de este dispositivo.** En este A51 (One UI) `uiautomator dump` no incluye la ventana del IME, asi que badge y panel se leen por posicion y color de pixel (`scripts/qa-vitrina.py`, seccion CALIBRACION): badge = amber/700 `#B45309`, panel = `surface/panel` `#F7F8FA`. **No es una solucion universal**: si cambia el layout, los colores de tokens, la densidad o se prueba en otro equipo, hay que recalibrar esas constantes.

> **t_badge**: mide desde el trigger hasta la deteccion del badge en pixeles; como el badge es un dummy estatico (no gateado por matching real), se espera ~0ms — es un chequeo de regresion (el IME no se cayo al escribir el trigger, ver commit `3dc69b23`), no la latencia real del motor de sugerencias.

| Ronda | t_badge (ms) | t_panel (ms) | panel top (dp) | panel bottom (dp) | alto panel (dp) | ultimo msg bottom (dp) | solape | margen (dp) | contenido |
|---|---|---|---|---|---|---|---|---|---|---|
| 1 | ERROR | | | | | | | | | `ronda 1: no se abrio el panel (tap en (1028, 1537))` |
| 2 | 2006 | 970 | 835.4 | 861.3 | 25.9 | 571.4 | NO | 264.0 | no |

> **Alto del panel**: medido en px por el cluster de filas `surface/panel`, convertido a dp. La referencia es el token `kb/panel/height` = 192dp (**derivado**: `kb/row/height` × `kb/row/count` + 2 × `kb/pad/v` = 46×4+8; `design/tokens.json:187,218` — valor derivado, no spec de 04.10 que este en el repo).

---

## Corrida QA Vitrina V1 — 2026-08-16 22:27:49

- Dispositivo: **SM-A515F** (Galaxy A51) · density 420 · pantalla 1080x2400 · trigger `#mesa` · chat WhatsApp Business

> **Medicion del IME por pixeles, especifica de este dispositivo.** En este A51 (One UI) `uiautomator dump` no incluye la ventana del IME, asi que badge y panel se leen por posicion y color de pixel (`scripts/qa-vitrina.py`, seccion CALIBRACION): badge = amber/700 `#B45309`, panel = `surface/panel` `#F7F8FA`. **No es una solucion universal**: si cambia el layout, los colores de tokens, la densidad o se prueba en otro equipo, hay que recalibrar esas constantes.

> **t_badge**: mide desde el trigger hasta la deteccion del badge en pixeles; como el badge es un dummy estatico (no gateado por matching real), se espera ~0ms — es un chequeo de regresion (el IME no se cayo al escribir el trigger, ver commit `3dc69b23`), no la latencia real del motor de sugerencias.

| Ronda | t_badge (ms) | t_panel (ms) | panel top (dp) | panel bottom (dp) | alto panel (dp) | ultimo msg bottom (dp) | solape | margen (dp) | contenido |
|---|---|---|---|---|---|---|---|---|---|---|
| 1 | ERROR | | | | | | | | | `no se encontro el ancla del ancla (¿se cayo el IME o cambio el layout?)` |

> **Alto del panel**: medido en px por el cluster de filas `surface/panel`, convertido a dp. La referencia es el token `kb/panel/height` = 192dp (**derivado**: `kb/row/height` × `kb/row/count` + 2 × `kb/pad/v` = 46×4+8; `design/tokens.json:187,218` — valor derivado, no spec de 04.10 que este en el repo).

---

## Corrida QA Vitrina V1 — 2026-08-16 22:29:01

- Dispositivo: **SM-A515F** (Galaxy A51) · density 420 · pantalla 1080x2400 · trigger `#mesa` · chat WhatsApp Business

> **Medicion del IME por pixeles, especifica de este dispositivo.** En este A51 (One UI) `uiautomator dump` no incluye la ventana del IME, asi que badge y panel se leen por posicion y color de pixel (`scripts/qa-vitrina.py`, seccion CALIBRACION): badge = amber/700 `#B45309`, panel = `surface/panel` `#F7F8FA`. **No es una solucion universal**: si cambia el layout, los colores de tokens, la densidad o se prueba en otro equipo, hay que recalibrar esas constantes.

> **t_badge**: mide desde el trigger hasta la deteccion del badge en pixeles; como el badge es un dummy estatico (no gateado por matching real), se espera ~0ms — es un chequeo de regresion (el IME no se cayo al escribir el trigger, ver commit `3dc69b23`), no la latencia real del motor de sugerencias.

| Ronda | t_badge (ms) | t_panel (ms) | panel top (dp) | panel bottom (dp) | alto panel (dp) | ultimo msg bottom (dp) | solape | margen (dp) | contenido |
|---|---|---|---|---|---|---|---|---|---|---|
| 1 | 1136 | 1015 | 814.9 | 841.1 | 26.3 | 453.3 | NO | 361.5 | no |

> **Alto del panel**: medido en px por el cluster de filas `surface/panel`, convertido a dp. La referencia es el token `kb/panel/height` = 192dp (**derivado**: `kb/row/height` × `kb/row/count` + 2 × `kb/pad/v` = 46×4+8; `design/tokens.json:187,218` — valor derivado, no spec de 04.10 que este en el repo).

---

## Corrida QA Vitrina V1 — 2026-08-16 22:29:51

- Dispositivo: **SM-A515F** (Galaxy A51) · density 420 · pantalla 1080x2400 · trigger `#mesa` · chat WhatsApp Business

> **Medicion del IME por pixeles, especifica de este dispositivo.** En este A51 (One UI) `uiautomator dump` no incluye la ventana del IME, asi que badge y panel se leen por posicion y color de pixel (`scripts/qa-vitrina.py`, seccion CALIBRACION): badge = amber/700 `#B45309`, panel = `surface/panel` `#F7F8FA`. **No es una solucion universal**: si cambia el layout, los colores de tokens, la densidad o se prueba en otro equipo, hay que recalibrar esas constantes.

> **t_badge**: mide desde el trigger hasta la deteccion del badge en pixeles; como el badge es un dummy estatico (no gateado por matching real), se espera ~0ms — es un chequeo de regresion (el IME no se cayo al escribir el trigger, ver commit `3dc69b23`), no la latencia real del motor de sugerencias.

| Ronda | t_badge (ms) | t_panel (ms) | panel top (dp) | panel bottom (dp) | alto panel (dp) | ultimo msg bottom (dp) | solape | margen (dp) | contenido |
|---|---|---|---|---|---|---|---|---|---|---|
| 1 | 1196 | 978 | 814.9 | 841.1 | 26.3 | 453.3 | NO | 361.5 | no |

> **Alto del panel**: medido en px por el cluster de filas `surface/panel`, convertido a dp. La referencia es el token `kb/panel/height` = 192dp (**derivado**: `kb/row/height` × `kb/row/count` + 2 × `kb/pad/v` = 46×4+8; `design/tokens.json:187,218` — valor derivado, no spec de 04.10 que este en el repo).

---

## Corrida QA Vitrina V1 — 2026-08-16 22:46:48

- Dispositivo: **SM-A515F** (Galaxy A51) · density 420 · pantalla 1080x2400 · trigger `#mesa` · chat WhatsApp Business

> **Medicion del IME por pixeles, especifica de este dispositivo.** En este A51 (One UI) `uiautomator dump` no incluye la ventana del IME, asi que badge y panel se leen por posicion y color de pixel (`scripts/qa-vitrina.py`, seccion CALIBRACION): badge = amber/700 `#B45309`, panel = `surface/panel` `#F7F8FA`. **No es una solucion universal**: si cambia el layout, los colores de tokens, la densidad o se prueba en otro equipo, hay que recalibrar esas constantes.

> **t_badge**: mide desde el trigger hasta la deteccion del badge en pixeles; como el badge es un dummy estatico (no gateado por matching real), se espera ~0ms — es un chequeo de regresion (el IME no se cayo al escribir el trigger, ver commit `3dc69b23`), no la latencia real del motor de sugerencias.

| Ronda | t_badge (ms) | t_panel (ms) | panel top (dp) | panel bottom (dp) | alto panel (dp) | ultimo msg bottom (dp) | solape | margen (dp) | contenido |
|---|---|---|---|---|---|---|---|---|---|---|
| 1 | 1154 | 1015 | 814.9 | 841.1 | 26.3 | 453.3 | NO | 361.5 | no |
| 2 | 1097 | 1030 | 814.9 | 841.1 | 26.3 | 453.3 | NO | 361.5 | no |
| 3 | 1073 | 1075 | 814.9 | 841.1 | 26.3 | 453.3 | NO | 361.5 | no |

> **Alto del panel**: medido en px por el cluster de filas `surface/panel`, convertido a dp. La referencia es el token `kb/panel/height` = 192dp (**derivado**: `kb/row/height` × `kb/row/count` + 2 × `kb/pad/v` = 46×4+8; `design/tokens.json:187,218` — valor derivado, no spec de 04.10 que este en el repo).

---

## Corrida QA Vitrina V1 — 2026-08-16 22:48:08

- Dispositivo: **SM-A515F** (Galaxy A51) · density 420 · pantalla 1080x2400 · trigger `#mesa` · chat WhatsApp Business

> **Medicion del IME por pixeles, especifica de este dispositivo.** En este A51 (One UI) `uiautomator dump` no incluye la ventana del IME, asi que badge y panel se leen por posicion y color de pixel (`scripts/qa-vitrina.py`, seccion CALIBRACION): badge = amber/700 `#B45309`, panel = `surface/panel` `#F7F8FA`. **No es una solucion universal**: si cambia el layout, los colores de tokens, la densidad o se prueba en otro equipo, hay que recalibrar esas constantes.

> **t_badge**: mide desde el trigger hasta la deteccion del badge en pixeles; como el badge es un dummy estatico (no gateado por matching real), se espera ~0ms — es un chequeo de regresion (el IME no se cayo al escribir el trigger, ver commit `3dc69b23`), no la latencia real del motor de sugerencias.

| Ronda | t_badge (ms) | t_panel (ms) | panel top (dp) | panel bottom (dp) | alto panel (dp) | ultimo msg bottom (dp) | solape | margen (dp) | contenido |
|---|---|---|---|---|---|---|---|---|---|---|
| 1 | 1202 | 1064 | 814.9 | 841.1 | 26.3 | 453.3 | NO | 361.5 | no |
| 2 | 1115 | 1021 | 814.9 | 841.1 | 26.3 | 453.3 | NO | 361.5 | no |
| 3 | 1080 | 1057 | 814.9 | 841.1 | 26.3 | 453.3 | NO | 361.5 | no |

> **Alto del panel**: medido en px por el cluster de filas `surface/panel`, convertido a dp. La referencia es el token `kb/panel/height` = 192dp (**derivado**: `kb/row/height` × `kb/row/count` + 2 × `kb/pad/v` = 46×4+8; `design/tokens.json:187,218` — valor derivado, no spec de 04.10 que este en el repo).

---

## Corrida QA Vitrina V1 — 2026-08-16 23:03:10

- Dispositivo: **SM-A515F** (Galaxy A51) · density 420 · pantalla 1080x2400 · trigger `#mesa` · chat WhatsApp Business

> **Medicion del IME por pixeles, especifica de este dispositivo.** En este A51 (One UI) `uiautomator dump` no incluye la ventana del IME, asi que badge y panel se leen por posicion y color de pixel (`scripts/qa-vitrina.py`, seccion CALIBRACION): badge = amber/700 `#B45309`, panel = `surface/panel` `#F7F8FA`. **No es una solucion universal**: si cambia el layout, los colores de tokens, la densidad o se prueba en otro equipo, hay que recalibrar esas constantes.

> **t_badge**: mide desde el trigger hasta la deteccion del badge en pixeles; como el badge es un dummy estatico (no gateado por matching real), se espera ~0ms — es un chequeo de regresion (el IME no se cayo al escribir el trigger, ver commit `3dc69b23`), no la latencia real del motor de sugerencias.

| Ronda | t_badge (ms) | t_panel (ms) | panel top (dp) | panel bottom (dp) | alto panel (dp) | ultimo msg bottom (dp) | solape | margen (dp) | contenido |
|---|---|---|---|---|---|---|---|---|---|---|
| 1 | 2082 | 1145 | 835.4 | 861.3 | 25.9 | 571.4 | NO | 264.0 | no |

> **Alto del panel**: medido en px por el cluster de filas `surface/panel`, convertido a dp. La referencia es el token `kb/panel/height` = 192dp (**derivado**: `kb/row/height` × `kb/row/count` + 2 × `kb/pad/v` = 46×4+8; `design/tokens.json:187,218` — valor derivado, no spec de 04.10 que este en el repo).

---

## Corrida QA Vitrina V1 — 2026-08-16 23:03:46

- Dispositivo: **SM-A515F** (Galaxy A51) · density 420 · pantalla 1080x2400 · trigger `#mesa` · chat WhatsApp Business

> **Medicion del IME por pixeles, especifica de este dispositivo.** En este A51 (One UI) `uiautomator dump` no incluye la ventana del IME, asi que badge y panel se leen por posicion y color de pixel (`scripts/qa-vitrina.py`, seccion CALIBRACION): badge = amber/700 `#B45309`, panel = `surface/panel` `#F7F8FA`. **No es una solucion universal**: si cambia el layout, los colores de tokens, la densidad o se prueba en otro equipo, hay que recalibrar esas constantes.

> **t_badge**: mide desde el trigger hasta la deteccion del badge en pixeles; como el badge es un dummy estatico (no gateado por matching real), se espera ~0ms — es un chequeo de regresion (el IME no se cayo al escribir el trigger, ver commit `3dc69b23`), no la latencia real del motor de sugerencias.

| Ronda | t_badge (ms) | t_panel (ms) | panel top (dp) | panel bottom (dp) | alto panel (dp) | ultimo msg bottom (dp) | solape | margen (dp) | contenido |
|---|---|---|---|---|---|---|---|---|---|---|
| 1 | 2155 | 1164 | 835.4 | 861.3 | 25.9 | 571.4 | NO | 264.0 | no |

> **Alto del panel**: medido en px por el cluster de filas `surface/panel`, convertido a dp. La referencia es el token `kb/panel/height` = 192dp (**derivado**: `kb/row/height` × `kb/row/count` + 2 × `kb/pad/v` = 46×4+8; `design/tokens.json:187,218` — valor derivado, no spec de 04.10 que este en el repo).

---

## Corrida QA Vitrina V1 — 2026-08-16 23:07:56

- Dispositivo: **SM-A515F** (Galaxy A51) · density 420 · pantalla 1080x2400 · trigger `#mesa` · chat WhatsApp Business

> **Medicion del IME por pixeles, especifica de este dispositivo.** En este A51 (One UI) `uiautomator dump` no incluye la ventana del IME, asi que badge y panel se leen por posicion y color de pixel (`scripts/qa-vitrina.py`, seccion CALIBRACION): badge = amber/700 `#B45309`, panel = `surface/panel` `#F7F8FA`. **No es una solucion universal**: si cambia el layout, los colores de tokens, la densidad o se prueba en otro equipo, hay que recalibrar esas constantes.

> **t_badge**: mide desde el trigger hasta la deteccion del badge en pixeles; como el badge es un dummy estatico (no gateado por matching real), se espera ~0ms — es un chequeo de regresion (el IME no se cayo al escribir el trigger, ver commit `3dc69b23`), no la latencia real del motor de sugerencias.

| Ronda | t_badge (ms) | t_panel (ms) | panel top (dp) | panel bottom (dp) | alto panel (dp) | ultimo msg bottom (dp) | solape | margen (dp) | contenido |
|---|---|---|---|---|---|---|---|---|---|---|
| 1 | 2076 | 1183 | 814.9 | 841.1 | 26.3 | 509.3 | NO | 305.5 | no |
| 2 | 2015 | 1141 | 814.9 | 841.1 | 26.3 | 509.3 | NO | 305.5 | no |
| 3 | 2086 | 1139 | 814.9 | 841.1 | 26.3 | 509.3 | NO | 305.5 | no |

> **Alto del panel**: medido en px por el cluster de filas `surface/panel`, convertido a dp. La referencia es el token `kb/panel/height` = 192dp (**derivado**: `kb/row/height` × `kb/row/count` + 2 × `kb/pad/v` = 46×4+8; `design/tokens.json:187,218` — valor derivado, no spec de 04.10 que este en el repo).

---

## Corrida QA Vitrina V1 — 2026-08-16 23:09:38

- Dispositivo: **SM-A515F** (Galaxy A51) · density 420 · pantalla 1080x2400 · trigger `#mesa` · chat WhatsApp Business

> **Medicion del IME por pixeles, especifica de este dispositivo.** En este A51 (One UI) `uiautomator dump` no incluye la ventana del IME, asi que badge y panel se leen por posicion y color de pixel (`scripts/qa-vitrina.py`, seccion CALIBRACION): badge = amber/700 `#B45309`, panel = `surface/panel` `#F7F8FA`. **No es una solucion universal**: si cambia el layout, los colores de tokens, la densidad o se prueba en otro equipo, hay que recalibrar esas constantes.

> **t_badge**: mide desde el trigger hasta la deteccion del badge en pixeles; como el badge es un dummy estatico (no gateado por matching real), se espera ~0ms — es un chequeo de regresion (el IME no se cayo al escribir el trigger, ver commit `3dc69b23`), no la latencia real del motor de sugerencias.

| Ronda | t_badge (ms) | t_panel (ms) | panel top (dp) | panel bottom (dp) | alto panel (dp) | ultimo msg bottom (dp) | solape | margen (dp) | contenido |
|---|---|---|---|---|---|---|---|---|---|---|
| 1 | 2062 | 1191 | 612.2 | 906.3 | 294.1 | 509.3 | NO | 102.9 | SI |
| 2 | 2104 | 1158 | 612.2 | 906.3 | 294.1 | 509.3 | NO | 102.9 | SI |
| 3 | 2061 | 1129 | 612.2 | 906.3 | 294.1 | 509.3 | NO | 102.9 | SI |

> **Alto del panel**: medido en px por el cluster de filas `surface/panel`, convertido a dp. La referencia es el token `kb/panel/height` = 192dp (**derivado**: `kb/row/height` × `kb/row/count` + 2 × `kb/pad/v` = 46×4+8; `design/tokens.json:187,218` — valor derivado, no spec de 04.10 que este en el repo).

---

## Corrida QA Vitrina V1 — 2026-08-16 23:11:40

- Dispositivo: **SM-A515F** (Galaxy A51) · density 420 · pantalla 1080x2400 · trigger `#mesa` · chat WhatsApp Business

> **Medicion del IME por pixeles, especifica de este dispositivo.** En este A51 (One UI) `uiautomator dump` no incluye la ventana del IME, asi que badge y panel se leen por posicion y color de pixel (`scripts/qa-vitrina.py`, seccion CALIBRACION): badge = amber/700 `#B45309`, panel = `surface/panel` `#F7F8FA`. **No es una solucion universal**: si cambia el layout, los colores de tokens, la densidad o se prueba en otro equipo, hay que recalibrar esas constantes.

> **t_badge**: mide desde el trigger hasta la deteccion del badge en pixeles; como el badge es un dummy estatico (no gateado por matching real), se espera ~0ms — es un chequeo de regresion (el IME no se cayo al escribir el trigger, ver commit `3dc69b23`), no la latencia real del motor de sugerencias.

| Ronda | t_badge (ms) | t_panel (ms) | panel top (dp) | panel bottom (dp) | alto panel (dp) | ultimo msg bottom (dp) | solape | margen (dp) | contenido |
|---|---|---|---|---|---|---|---|---|---|---|
| 1 | 2050 | 1181 | 674.3 | 906.3 | 232.0 | 571.4 | NO | 102.9 | SI |

> **Alto del panel**: medido en px por el cluster de filas `surface/panel`, convertido a dp. La referencia es el token `kb/panel/height` = 192dp (**derivado**: `kb/row/height` × `kb/row/count` + 2 × `kb/pad/v` = 46×4+8; `design/tokens.json:187,218` — valor derivado, no spec de 04.10 que este en el repo).

---

## Corrida QA Vitrina V1 — 2026-08-16 23:13:40

- Dispositivo: **SM-A515F** (Galaxy A51) · density 420 · pantalla 1080x2400 · trigger `#mesa` · chat WhatsApp Business

> **Medicion del IME por pixeles, especifica de este dispositivo.** En este A51 (One UI) `uiautomator dump` no incluye la ventana del IME, asi que badge y panel se leen por posicion y color de pixel (`scripts/qa-vitrina.py`, seccion CALIBRACION): badge = amber/700 `#B45309`, panel = `surface/panel` `#F7F8FA`. **No es una solucion universal**: si cambia el layout, los colores de tokens, la densidad o se prueba en otro equipo, hay que recalibrar esas constantes.

> **t_badge**: mide desde el trigger hasta la deteccion del badge en pixeles; como el badge es un dummy estatico (no gateado por matching real), se espera ~0ms — es un chequeo de regresion (el IME no se cayo al escribir el trigger, ver commit `3dc69b23`), no la latencia real del motor de sugerencias.

| Ronda | t_badge (ms) | t_panel (ms) | panel top (dp) | panel bottom (dp) | alto panel (dp) | ultimo msg bottom (dp) | solape | margen (dp) | contenido |
|---|---|---|---|---|---|---|---|---|---|---|
| 1 | 2060 | 1154 | 863.2 | 906.3 | 43.0 | 571.4 | NO | 291.8 | no |
| 2 | 2022 | 1163 | 863.2 | 906.3 | 43.0 | 571.4 | NO | 291.8 | no |
| 3 | 2042 | 1158 | 863.2 | 906.3 | 43.0 | 571.4 | NO | 291.8 | no |

> **Alto del panel**: medido en px por el cluster de filas `surface/panel`, convertido a dp. La referencia es el token `kb/panel/height` = 192dp (**derivado**: `kb/row/height` × `kb/row/count` + 2 × `kb/pad/v` = 46×4+8; `design/tokens.json:187,218` — valor derivado, no spec de 04.10 que este en el repo).

---

## Corrida QA Vitrina V1 — 2026-08-16 23:34:19

- Dispositivo: **SM-A515F** (Galaxy A51) · density 420 · pantalla 1080x2400 · trigger `#mesa` · chat WhatsApp Business

> **Medicion del IME por pixeles, especifica de este dispositivo.** En este A51 (One UI) `uiautomator dump` no incluye la ventana del IME, asi que badge y panel se leen por posicion y color de pixel (`scripts/qa-vitrina.py`, seccion CALIBRACION): badge = amber/700 `#B45309`, panel = `surface/panel` `#F7F8FA`. **No es una solucion universal**: si cambia el layout, los colores de tokens, la densidad o se prueba en otro equipo, hay que recalibrar esas constantes.

> **t_badge**: mide desde el trigger hasta la deteccion del badge en pixeles; como el badge es un dummy estatico (no gateado por matching real), se espera ~0ms — es un chequeo de regresion (el IME no se cayo al escribir el trigger, ver commit `3dc69b23`), no la latencia real del motor de sugerencias.

| Ronda | t_badge (ms) | t_panel (ms) | panel top (dp) | panel bottom (dp) | alto panel (dp) | ultimo msg bottom (dp) | solape | margen (dp) | contenido |
|---|---|---|---|---|---|---|---|---|---|---|
| 1 | 2074 | 1151 | 863.2 | 865.9 | 2.7 | 571.4 | NO | 291.8 | no |
| 2 | 2059 | 1159 | 863.2 | 865.9 | 2.7 | 571.4 | NO | 291.8 | no |
| 3 | 2109 | 1168 | 863.2 | 865.9 | 2.7 | 571.4 | NO | 291.8 | no |

> **Alto del panel**: medido en px por el cluster de filas `surface/panel`, convertido a dp. La referencia es el token `kb/panel/height` = 192dp (**derivado**: `kb/row/height` × `kb/row/count` + 2 × `kb/pad/v` = 46×4+8; `design/tokens.json:187,218` — valor derivado, no spec de 04.10 que este en el repo).

---

## Corrida QA Vitrina V1 — 2026-08-16 23:37:14

- Dispositivo: **SM-A515F** (Galaxy A51) · density 420 · pantalla 1080x2400 · trigger `#mesa` · chat WhatsApp Business

> **Medicion del IME por pixeles, especifica de este dispositivo.** En este A51 (One UI) `uiautomator dump` no incluye la ventana del IME, asi que badge y panel se leen por posicion y color de pixel (`scripts/qa-vitrina.py`, seccion CALIBRACION): badge = amber/700 `#B45309`, panel = `surface/panel` `#F7F8FA`. **No es una solucion universal**: si cambia el layout, los colores de tokens, la densidad o se prueba en otro equipo, hay que recalibrar esas constantes.

> **t_badge**: mide desde el trigger hasta la deteccion del badge en pixeles; como el badge es un dummy estatico (no gateado por matching real), se espera ~0ms — es un chequeo de regresion (el IME no se cayo al escribir el trigger, ver commit `3dc69b23`), no la latencia real del motor de sugerencias.

| Ronda | t_badge (ms) | t_panel (ms) | panel top (dp) | panel bottom (dp) | alto panel (dp) | ultimo msg bottom (dp) | solape | margen (dp) | contenido |
|---|---|---|---|---|---|---|---|---|---|---|
| 1 | 2031 | 1154 | 863.2 | 906.3 | 43.0 | 571.4 | NO | 291.8 | no |
| 2 | 2015 | 1168 | 863.2 | 906.3 | 43.0 | 571.4 | NO | 291.8 | no |
| 3 | 2003 | 1214 | 863.2 | 906.3 | 43.0 | 571.4 | NO | 291.8 | no |

> **Alto del panel**: medido en px por el cluster de filas `surface/panel`, convertido a dp. La referencia es el token `kb/panel/height` = 192dp (**derivado**: `kb/row/height` × `kb/row/count` + 2 × `kb/pad/v` = 46×4+8; `design/tokens.json:187,218` — valor derivado, no spec de 04.10 que este en el repo).

---

## Corrida QA Vitrina V1 — 2026-08-16 23:39:21

- Dispositivo: **SM-A515F** (Galaxy A51) · density 420 · pantalla 1080x2400 · trigger `#mesa` · chat WhatsApp Business

> **Medicion del IME por pixeles, especifica de este dispositivo.** En este A51 (One UI) `uiautomator dump` no incluye la ventana del IME, asi que badge y panel se leen por posicion y color de pixel (`scripts/qa-vitrina.py`, seccion CALIBRACION): badge = amber/700 `#B45309`, panel = `surface/panel` `#F7F8FA`. **No es una solucion universal**: si cambia el layout, los colores de tokens, la densidad o se prueba en otro equipo, hay que recalibrar esas constantes.

> **t_badge**: mide desde el trigger hasta la deteccion del badge en pixeles; como el badge es un dummy estatico (no gateado por matching real), se espera ~0ms — es un chequeo de regresion (el IME no se cayo al escribir el trigger, ver commit `3dc69b23`), no la latencia real del motor de sugerencias.

| Ronda | t_badge (ms) | t_panel (ms) | panel top (dp) | panel bottom (dp) | alto panel (dp) | ultimo msg bottom (dp) | solape | margen (dp) | contenido |
|---|---|---|---|---|---|---|---|---|---|---|
| 1 | 2050 | 1166 | 863.2 | 906.3 | 43.0 | 571.4 | NO | 291.8 | no |
| 2 | 2049 | 1170 | 863.2 | 906.3 | 43.0 | 571.4 | NO | 291.8 | no |
| 3 | 2034 | 1173 | 863.2 | 906.3 | 43.0 | 571.4 | NO | 291.8 | no |

> **Alto del panel**: medido en px por el cluster de filas `surface/panel`, convertido a dp. La referencia es el token `kb/panel/height` = 192dp (**derivado**: `kb/row/height` × `kb/row/count` + 2 × `kb/pad/v` = 46×4+8; `design/tokens.json:187,218` — valor derivado, no spec de 04.10 que este en el repo).

---

## Corrida QA Vitrina V1 — 2026-08-17 00:18:15

- Dispositivo: **SM-A515F** (Galaxy A51) · density 420 · pantalla 1080x2400 · trigger `#mesa` · chat WhatsApp Business

> **Medicion del IME por pixeles, especifica de este dispositivo.** En este A51 (One UI) `uiautomator dump` no incluye la ventana del IME, asi que badge y panel se leen por posicion y color de pixel (`scripts/qa-vitrina.py`, seccion CALIBRACION): badge = amber/700 `#B45309`, panel = `surface/panel` `#F7F8FA`. **No es una solucion universal**: si cambia el layout, los colores de tokens, la densidad o se prueba en otro equipo, hay que recalibrar esas constantes.

> **t_badge**: mide desde el trigger hasta la deteccion del badge en pixeles; como el badge es un dummy estatico (no gateado por matching real), se espera ~0ms — es un chequeo de regresion (el IME no se cayo al escribir el trigger, ver commit `3dc69b23`), no la latencia real del motor de sugerencias.

| Ronda | t_badge (ms) | t_panel (ms) | panel top (dp) | panel bottom (dp) | alto panel (dp) | ultimo msg bottom (dp) | solape | margen (dp) | contenido |
|---|---|---|---|---|---|---|---|---|---|---|
| 1 | ERROR | | | | | | | | | `No se encontro el campo de entrada de WhatsApp Business. Precondicion: tener WhatsApp Business (com.whatsapp.w4b) abierto en el chat de prueba con la conversacion visible, dispositivo por USB y depuracion activada.` |
| 2 | ERROR | | | | | | | | | `No se encontro el campo de entrada de WhatsApp Business. Precondicion: tener WhatsApp Business (com.whatsapp.w4b) abierto en el chat de prueba con la conversacion visible, dispositivo por USB y depuracion activada.` |
| 3 | ERROR | | | | | | | | | `No se encontro el campo de entrada de WhatsApp Business. Precondicion: tener WhatsApp Business (com.whatsapp.w4b) abierto en el chat de prueba con la conversacion visible, dispositivo por USB y depuracion activada.` |

> **Alto del panel**: medido en px por el cluster de filas `surface/panel`, convertido a dp. La referencia es el token `kb/panel/height` = 192dp (**derivado**: `kb/row/height` × `kb/row/count` + 2 × `kb/pad/v` = 46×4+8; `design/tokens.json:187,218` — valor derivado, no spec de 04.10 que este en el repo).

---

## Corrida QA Vitrina V1 — 2026-08-17 00:21:04

- Dispositivo: **SM-A515F** (Galaxy A51) · density 420 · pantalla 1080x2400 · trigger `#mesa` · chat WhatsApp Business

> **Medicion del IME por pixeles, especifica de este dispositivo.** En este A51 (One UI) `uiautomator dump` no incluye la ventana del IME, asi que badge y panel se leen por posicion y color de pixel (`scripts/qa-vitrina.py`, seccion CALIBRACION): badge = amber/700 `#B45309`, panel = `surface/panel` `#F7F8FA`. **No es una solucion universal**: si cambia el layout, los colores de tokens, la densidad o se prueba en otro equipo, hay que recalibrar esas constantes.

> **t_badge**: mide desde el trigger hasta la deteccion del badge en pixeles; como el badge es un dummy estatico (no gateado por matching real), se espera ~0ms — es un chequeo de regresion (el IME no se cayo al escribir el trigger, ver commit `3dc69b23`), no la latencia real del motor de sugerencias.

| Ronda | t_badge (ms) | t_panel (ms) | panel top (dp) | panel bottom (dp) | alto panel (dp) | ultimo msg bottom (dp) | solape | margen (dp) | contenido |
|---|---|---|---|---|---|---|---|---|---|---|
| 1 | 2053 | 1174 | 863.2 | 894.9 | 31.6 | 571.4 | NO | 291.8 | no |
| 2 | 2105 | 1126 | 863.2 | 894.9 | 31.6 | 571.4 | NO | 291.8 | no |
| 3 | 2029 | 1221 | 863.2 | 894.9 | 31.6 | 571.4 | NO | 291.8 | no |

> **Alto del panel**: medido en px por el cluster de filas `surface/panel`, convertido a dp. La referencia es el token `kb/panel/height` = 192dp (**derivado**: `kb/row/height` × `kb/row/count` + 2 × `kb/pad/v` = 46×4+8; `design/tokens.json:187,218` — valor derivado, no spec de 04.10 que este en el repo).

---

## Corrida QA Vitrina V1 — 2026-08-17 00:53:36

- Dispositivo: **SM-A515F** (Galaxy A51) · density 420 · pantalla 1080x2400 · trigger `#mesa` · chat WhatsApp Business

> **Medicion del IME por pixeles, especifica de este dispositivo.** En este A51 (One UI) `uiautomator dump` no incluye la ventana del IME, asi que badge y panel se leen por posicion y color de pixel (`scripts/qa-vitrina.py`, seccion CALIBRACION): badge = amber/700 `#B45309`, panel = `surface/panel` `#F7F8FA`. **No es una solucion universal**: si cambia el layout, los colores de tokens, la densidad o se prueba en otro equipo, hay que recalibrar esas constantes.

> **t_badge**: mide desde el trigger hasta la deteccion del badge en pixeles; como el badge es un dummy estatico (no gateado por matching real), se espera ~0ms — es un chequeo de regresion (el IME no se cayo al escribir el trigger, ver commit `3dc69b23`), no la latencia real del motor de sugerencias.

| Ronda | t_badge (ms) | t_panel (ms) | panel top (dp) | panel bottom (dp) | alto panel (dp) | ultimo msg bottom (dp) | solape | margen (dp) | contenido |
|---|---|---|---|---|---|---|---|---|---|---|
| 1 | ERROR | | | | | | | | | `No se encontro el campo de entrada de WhatsApp Business. Precondicion: tener WhatsApp Business (com.whatsapp.w4b) abierto en el chat de prueba con la conversacion visible, dispositivo por USB y depuracion activada.` |
| 2 | ERROR | | | | | | | | | `No se encontro el campo de entrada de WhatsApp Business. Precondicion: tener WhatsApp Business (com.whatsapp.w4b) abierto en el chat de prueba con la conversacion visible, dispositivo por USB y depuracion activada.` |
| 3 | ERROR | | | | | | | | | `No se encontro el campo de entrada de WhatsApp Business. Precondicion: tener WhatsApp Business (com.whatsapp.w4b) abierto en el chat de prueba con la conversacion visible, dispositivo por USB y depuracion activada.` |

> **Alto del panel**: medido en px por el cluster de filas `surface/panel`, convertido a dp. La referencia es el token `kb/panel/height` = 192dp (**derivado**: `kb/row/height` × `kb/row/count` + 2 × `kb/pad/v` = 46×4+8; `design/tokens.json:187,218` — valor derivado, no spec de 04.10 que este en el repo).

---
