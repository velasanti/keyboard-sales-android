# CLAUDE.md — Keyboard Sales AI · Android

> Claude Code lee este archivo automáticamente al abrir el proyecto. **Es el documento más importante del repo.**
>
> Vive en la raíz de `keyboard-sales-android`. La documentación de producto vive en el vault de Obsidian (Drive compartido *Keyboard-Obsidian-Vault*); este archivo **no la duplica, la referencia**.

---

## ¿Qué es Keyboard Sales AI?

Un **teclado nativo (IME) + app compañera** para equipos de ventas en Latinoamérica. Lleva el conocimiento de la empresa al vendedor **dentro del chat**, sin que tenga que salir de la conversación con el cliente.

Funciona en **cualquier app de mensajería**, no solo WhatsApp — es un IME, así que eso es por definición.

**El producto es conocimiento, no velocidad.** Ese orden se decidió el 2026-08-04 y es lo primero que hay que entender:

| Botón del teclado | Qué es | Rol |
|---|---|---|
| **☰ Vitrina** | Catálogo y respuestas rápidas. Estático y estructurado | Acá vive la velocidad (2-3 toques) |
| **✨** | Chat conversacional con el conocimiento de la empresa | **El corazón del producto** |

Nunca escribas en copy, commits, comentarios o nombres "el teclado más rápido para vender". Está superado.

**El botón ☰ se llama Vitrina** (decisión Santi, 2026-08-10). Antes se lo nombró "botón G" y "Menú"; los dos nombres están superados y no se usan más, ni en código ni en documentos. En código el namespace es `vitrina/`. **No uses `catalog/` para el botón**: `catalog` es el concepto de datos —los ítems que viven en el backend y en SQLDelight— y Vitrina es la superficie del teclado que los muestra. Mezclarlos produce el tipo de colisión de nombres que después no se puede deshacer.

Vitrina tiene **dos superficies**, y las dos existen (decisión Santi, 2026-08-10, que cierra la tensión "capa vs. modo" de 04.4 §4 contra 04.8):

| Superficie | Qué es | A qué situación sirve |
|---|---|---|
| **Vitrina capa** | Las coincidencias de producto aparecen como chips en la barra **mientras el vendedor escribe** | La **intención conocida**: ya sabe qué producto quiere y lo está tipeando |
| **Vitrina modo** | El ancla ☰ reemplaza el QWERTY con el catálogo completo | La **exploración**: no sabe, o el catálogo es grande |

Spec consolidado y autoritativo: `04.10 Vitrina — Especificación consolidada` en el vault. **Esa nota gana** sobre 04.4 §4 y sobre la propuesta abierta de 04.8 en todo lo que se contradigan.

**Contexto completo del producto:** vault de Obsidian → `CLAUDE.md`, `Home.md`, `01.1 Product Vision`, `03.1 MVP Definition`.

---

## Lo que este producto NUNCA hace

Esta sección va antes del stack a propósito. Violar cualquiera de estas líneas no es un bug: es romper la promesa sobre la que se vende el producto, y el cliente Android es **público y auditable** (GPL-3.0, ADR-012), así que cualquiera puede verificarlo.

1. **No usa `AccessibilityService`.** Nunca. No es una decisión de versión, es ADR-009. Si una feature parece necesitarlo, la feature está mal diseñada.
2. **No lee la pantalla ni los mensajes entrantes del cliente.** No hay captura de pantalla, no hay OCR, no hay lectura del árbol de vistas de la app anfitriona.
3. **No guarda el contenido de las conversaciones**, ni resúmenes, ni interpretaciones de lo que dijo el cliente (ADR-011).
4. **Sí guarda** el contacto (teléfono, y nombre si el vendedor lo asigna) y las acciones que el vendedor ejecuta sobre ese contacto (ADR-011). **Sí somos custodios de la base de contactos.**
5. **El cliente nunca habla directo con el proveedor de inferencia.** Toda llamada a IA pasa por nuestro backend. Nunca metas una API key de Groq ni de ningún modelo en el cliente.
6. **El contexto del panel ✨ sale del log de acciones del vendedor, jamás de la conversación.** Si estás escribiendo código que lee el `EditText` de la app anfitriona para "entender el contexto", pará y preguntá.

Y en copy, **nunca** escribas: "tus datos son tus datos", "IA on-device", "on-premise", "VPC propia", "residencia de datos en LatAm", "no guardamos la base de contactos". Todo eso es falso para esta arquitectura.

El único claim correcto, literal: **"No leemos ni guardamos lo que dice tu cliente — solo lo que hace tu vendedor."** Es un claim sobre **alcance**, no sobre ubicación.

---

## Stack

| Capa | Tecnología | Versión / nota |
|---|---|---|
| Teclado (IME) | **Fork de HeliBoard** — Kotlin + Java + XML Views | GPL-3.0. ADR-012. **No se construye desde cero.** Ver §Reglas del fork |
| App compañera | Kotlin + **Jetpack Compose** | Mismo APK y mismo módulo que el teclado. **Ya habilitado en upstream** — ver §HeliBoard es un solo módulo |
| DB local | **SQLDelight** | Type-safe, y pesa poco — importa para la meta de 25 MB |
| Diccionarios | AOSP `dicttool` + wordlists propias de español LatAm | Repo aparte: `keyboard-sales-dictionaries` |
| Módulos Gradle | **Uno solo: `:app`** (de upstream), más `:tools:make-emoji-keys` | 
| Red | *por decidir* | Lo que se elija tiene que justificarse contra el peso del APK |
| Backend | Go + Chi + PostgreSQL + sqlc | Repo aparte, privado. El cliente solo consume su API |
| IA | Modelo open-weight sobre Groq, **siempre vía backend** | Nunca directo desde el cliente |
| Auth | JWT + refresh token. Invitaciones, no self-service | La empresa la creamos nosotros |
| Multiplatform | **Sin KMP en el MVP** | Nativo por separado, a propósito |
| CI/CD | GitHub Actions | Ver `.github/workflows/ci.yml` |

**Metas de peso, y son restricciones de diseño, no aspiraciones:** APK completo **< 25 MB**; solo teclado **≤ 12-15 MB**. Cada dependencia que agregues se justifica contra esto en el PR.

**Gama baja es el caso de diseño, no la excepción.** El dispositivo de referencia es 360×640dp. Ver §Presupuesto vertical.

---

## Comandos clave

```bash
# Desarrollo
./gradlew assembleDebug                    # APK de debug
./gradlew installDebug                     # instalar en dispositivo conectado
./gradlew :app:assembleDebug               # el módulo único (es el teclado)

# Calidad — todo esto corre en CI, corrélo antes de commitear
./gradlew ktlintCheck
./gradlew testDebugUnitTest
python3 design/check-contrast.py                              # 72 pares de color en 2 modos
python3 design/gen-tokens.py --out . --check --platform android  # ¿tokens sincronizados?
python3 design/check-literals.py . --dimensions               # ¿algún hex a mano?

# Tokens de diseño — cuando cambia design/tokens.json
python3 design/gen-tokens.py --out . --platform android

# Peso del APK
./gradlew assembleRelease && ls -lh app/build/outputs/apk/release/

# Upstream (HeliBoard) — ver docs/UPSTREAM.md antes de correr esto
git fetch upstream
```

---

## Design system — cómo se consume

**No hay un solo color ni una sola dimensión escrita a mano en este repo.**

```
design/tokens.json          ← ÚNICA fuente de verdad. 48 primitivos → 53 semánticos × 2 modos
   │
   ├─ design/gen-tokens.py       → genera los archivos de abajo
   ├─ design/check-contrast.py   → falla el CI si un par de color no cumple WCAG
   └─ design/check-literals.py   → falla el CI si hay un hex escrito a mano
   │
   ▼  GENERADOS — no editar, llevan cabecera de aviso
app/src/main/res/values/tokens_colors.xml
app/src/main/res/values-night/tokens_colors.xml
app/src/main/res/values/tokens_dimens.xml
app/src/main/java/com/keyboardsales/ui/theme/Tokens.kt
```

**Cómo se usa:**

```kotlin
// XML Views (el teclado)
android:background="@color/key_letter_bg"
android:layout_height="@dimen/kb_row_height"  <!-- desde tokens_dimens.xml -->

// Compose (la app compañera)
Surface(color = AppTheme.colors.surfaceRaised) {
    Text(text = name, color = AppTheme.colors.contentPrimary)
}
Spacer(Modifier.height(Dim.spacing4))
```

**Especificación de los componentes:** vault → `04.2 Foundations` (tokens, escalas, motion, accesibilidad), `04.3a Components — IME` (36 fichas), `04.3b Components — App` (50 fichas). Cada ficha trae anatomía, variantes, estados, tokens por nombre, medidas, comportamiento, accesibilidad y don'ts. **Leé la ficha antes de escribir el componente.**

Dos cosas del sistema que conviene saber de memoria porque se violan fácil:

- **Light y dark son pares, no jerarquía.** Un IME hereda el tema del sistema. Todo componente se verifica en los dos.
- **Máximo dos superficies de acento saturado a la vez en el teclado.** Cuentan `accent/default` y `accent/pressed` como relleno; no cuentan los tintes (`accent/subtle`), los enlaces ni el anillo de foco.

---

## Presupuesto vertical — la restricción que define el teclado

```
kb/bar/height           48dp
kb/pad/v × 2             8dp
kb/row/height × 4      184dp
                       ─────
total en reposo        240dp
```

| Dispositivo | Alto útil | Límite 45% | Reposo (240dp) | Barra expandida (288dp) |
|---|---|---|---|---|
| Android Go, 720×1280 hdpi | 640dp | 288dp | 37.5% ✅ | **45.0%** ⚠️ clavado en el límite |
| Típico LatAm, 720×1600 xhdpi | 800dp | 360dp | 30.0% ✅ | 36.0% ✅ |
| Peor caso, 480×854 hdpi | 569dp | 256dp | 42.2% ✅ | 50.6% ❌ |

**No hay margen.** Cualquier cosa que sume 1dp rompe el caso de 640dp, que es el dispositivo más común del segmento. Antes de agregar padding, una fila o alto a la barra: recalculá esta tabla.

**El criterio de aprobación no es que el teclado quepa: es que el último mensaje del cliente siga visible por encima del teclado.** Un teclado que tapa la conversación es inservible aunque quepa.

`kb/bar/height-expanded` y `kb/height/max-fraction` están **SIN MEDIR** en dispositivo real. Medirlos es el objetivo del primer sprint. En emulador no vale: miente sobre `getWindowVisibleDisplayFrame`.

---

## Estructura de carpetas

```
keyboard-sales-android/
├── CLAUDE.md                    ← este archivo
├── LICENSE                      ← GPL-3.0, EL DE UPSTREAM. No se toca nunca
├── NOTICE                       ← copyright de AOSP, OpenBoard, HeliBoard y nuestro
├── .literals-baseline           ← congela los literales heredados de upstream
├── settings.gradle              ← de upstream: include ':app' + ':tools:make-emoji-keys'
├── design/                      ← fuente de verdad de tokens + los 3 verificadores
│   ├── tokens.json
│   ├── catalog-dummy.json       ← catálogo de prueba para Vitrina
│   ├── gen-tokens.py
│   ├── check-contrast.py
│   └── check-literals.py
├── app/                         ← EL MÓDULO ÚNICO. Es el IME. De upstream
│   └── src/main/
│       ├── java/helium314/…/    ← código de upstream. MINIMIZAR EL DIFF
│       ├── java/com/keyboardsales/   ← TODO lo nuestro va acá
│       │   ├── ui/theme/Tokens.kt   ← GENERADO (Compose)
│       │   └── vitrina/         ← Vitrina: capa (barra) y modo (panel)
│       │       ├── bar/         ← chips de catálogo y respuestas rápidas
│       │       ├── panel/       ← catálogo a pantalla completa
│       │       ├── search/      ← matching, normalización, ranking
│       │       ├── data/        ← SQLDelight + carga del dummy
│       │       └── insert/      ← construcción del mensaje y Deshacer
│       └── res/values/          ← tokens_colors.xml y tokens_dimens.xml (GENERADOS)
│           └── ../values-night/tokens_colors.xml  (GENERADO)
├── tools/make-emoji-keys/       ← de upstream
└── docs/UPSTREAM.md             ← rebase + las 3 excepciones al diff aditivo
```

## HeliBoard es un solo módulo, y ya trae Compose

Verificado contra el fork el 2026-08-10. Dos hechos que cambian cómo se escribe código acá:

**1. `settings.gradle` dice `include ':app'` y `include ':tools:make-emoji-keys'`. No existe un módulo `keyboard/`: el IME *es* `:app`.** La estructura de dos módulos que describía `06.3 Repositorios` quedó invalidada por ADR-012, y apuntar a `keyboard/` crea un módulo fantasma que Gradle no compila. Todo lo nuestro vive bajo `app/src/main/java/com/keyboardsales/`.

**2. Upstream ya tiene Jetpack Compose habilitado**, así que la app compañera en Compose **no cuesta ninguna edición a upstream**:

| Qué | Versión en upstream |
|---|---|
| `kotlin("plugin.compose")` | 2.3.20 |
| `buildFeatures { compose = true }` | sí |
| `androidx.compose:compose-bom` | 2025.11.01 |
| `androidx.compose.material3:material3` | del BOM |
| `androidx.navigation:navigation-compose` | 2.9.8 |
| `colorpicker-compose` | 1.1.3 (upstream lo usa para colores de usuario) |

Consecuencias prácticas:

- **`Tokens.kt` se genera siempre.** No hace falta ningún flag ni habilitar nada.
- **material3 y navigation-compose están disponibles** para la app compañera. Se heredan; no se agregan como dependencia nueva ni se justifican en el PR.
- **El peso de Compose ya está en la línea base del APK de HeliBoard**, así que no es un costo que introduzcamos nosotros. La meta de < 25 MB se mide contra el APK de upstream como punto de partida, no contra cero.
- **Sigue en pie la regla de que el IME no usa Compose.** Que esté disponible en el módulo no cambia el motivo: el `InputMethodService` es lo primero que el sistema mata y el costo de arranque en frío no lo tolera. El teclado es XML Views; Compose es solo para las pantallas de la app compañera. Que las dos cosas convivan en un módulo no las mezcla — las clases de Compose no se cargan si el IME no las toca.
- Ojo con un comentario de upstream en `app/build.gradle.kts` línea 139: un BOM más nuevo que `2025.11.01` arrastra `material-android:1.10.0`, que **exige minSdk 23**. Si alguna vez se sube el BOM, hay que verificar el minSdk contra la meta de gama baja.

**Regla de ubicación:** todo lo nuestro vive bajo `app/src/main/java/com/keyboardsales/`, en nuestro propio paquete. **Nada nuestro va dentro del paquete de upstream**, y ningún archivo de upstream se edita salvo las tres excepciones documentadas en `docs/UPSTREAM.md`.

---

## Reglas del fork de HeliBoard

Éstas no son estilo, son la razón de haber forkeado. Si el fork diverge de upstream, perdimos la única ventaja de no haber escrito el IME desde cero.

1. **`upstream` es un remoto permanente.** Nunca se borra. Ver `docs/UPSTREAM.md`.
2. **Minimizar el diff con upstream.** Todo lo nuestro va como módulo **aditivo** en `sales/`, no como edición de archivos de upstream.
3. **No se edita la tubería de sugerencias.** Todo cambio que toque el motor de sugerencias necesita **justificación explícita en el PR** — hay una casilla en la plantilla de PR y es obligatoria.
4. **Se preservan los avisos de copyright y licencia de AOSP, OpenBoard y HeliBoard.** Borrar un encabezado de licencia es una violación de la GPL-3.0, no un descuido de formato.
5. **Toda dependencia nueva se audita contra GPL-3.0** antes de agregarse. Analytics propietario, crash reporting propietario y servicios de Google no compatibles están **prohibidos**. Ver el aviso de abajo.

> ⚠️ **La GPL se activa con el primer APK que salga de tu máquina, no con el lanzamiento.** Entregarle un APK a Esculturas DG para la prueba técnica ya es distribuir una obra derivada de HeliBoard, y obliga a ofrecer el fuente. La auditoría de dependencias y el texto real de la GPL-3.0 en `LICENSE` vencen **antes del primer APK compartido**, no antes del primer push público.

---

## Reglas absolutas

1. **Kotlin, no Java**, en todo lo nuevo. El Java que hay es de upstream y no se toca.
2. **Nada de colores ni dimensiones literales.** Todo sale de los tokens generados. `check-literals.py` falla el build. Nunca escribas `Color(0xFF...)`, `#RRGGBB` ni `16.dp` a mano.
3. **El teclado usa XML Views. La app usa Compose.** No se mezcla. **Nunca metas Compose en el IME**: el costo de arranque en frío y de memoria del `InputMethodService` no lo tolera, y el `InputMethodService` es lo primero que el sistema mata.
4. **Nada de blur en el IME.** `RenderEffect.createBlurEffect` exige API 31, no tiene fallback razonable en gama baja y cuesta un paso de composición por frame. Prohibido.
5. **Las teclas no llevan sombra.** 30+ superficies con sombra redibujadas en cada pulsación es el gasto de render más fácil de eliminar.
6. **La pulsación de tecla no se anima.** Cambio de color inmediato. Cualquier duración > 0 se percibe como lag, y es el gesto que el vendedor hace mil veces por día.
7. **Nada de red en el hilo principal del IME**, y nada de red bloqueante en el arranque del `InputMethodService`. El teclado tiene que abrir con los productos ya sincronizados (offline first).
8. **Área de toque mínima: 48dp** para todo lo que no es una tecla (`size/touch/min`). Las teclas usan `kb/touch/min` = 44dp y son la **única** excepción. El tamaño visual y el área de toque son cosas distintas: un chip mide 32dp y se toca en 48dp vía padding transparente o `TouchDelegate`.
9. **Toda tecla y todo control tiene `contentDescription`.** Las modificadoras dicen su función, no su símbolo: `#` es "sigilo de producto", no "numeral".
10. **Un archivo, una responsabilidad.** Si un archivo pasa de ~300 líneas, se parte.
11. **Strings de UI solo desde `strings.xml`**, nunca hardcodeados. Código en inglés, UI en español.
12. **Nada de secrets en el código.** Ni API keys, ni URLs de producción, ni tokens. Van por `local.properties` (ignorado) o secrets de CI.
13. **Toda acción con efecto real pide confirmación explícita** con los datos concretos a la vista (ADR-016). Es la única excepción deliberada al principio de 2-3 toques y no se optimiza.
14. **Una sola URL por mensaje** (ADR-017). La tarjeta de producto **no se inserta**: se inserta texto plano con una URL y la app receptora la renderiza desde los Open Graph tags. No existe forma de insertar una tarjeta compuesta en ningún sistema operativo. Si estás escribiendo código que intenta insertar una imagen desde el IME, pará.

---

## Testing

Vyn no tenía nada de esto y pagó el precio: un indicador de UI estuvo tres días documentado como funcionando sin haber renderizado nunca. Acá el mínimo es:

- **Unit tests obligatorios** en: resolución de coincidencias de catálogo, construcción del mensaje a insertar (texto + URL única), y el log de eventos (ADR-011). Son las tres cosas donde un bug es invisible y caro.
- **Sin tests de UI en el MVP.** No vale la pena todavía; la verificación de UI es manual y en dispositivo.
- **La verificación de UI es en dispositivo físico, nunca en emulador.** El emulador miente sobre alto de ventana, latencia de tecla y comportamiento del IME.
- **"Verificado" significa mirar el elemento específico que cambiaste**, no que la pantalla abra. Si tocaste un componente compartido, verificalo en al menos dos lugares que lo usan.
- Los tres verificadores de `design/` corren en cada PR y son bloqueantes.

---

## Commits y ramas

- **Conventional Commits, en español**, con scope: `feat(bar): chip de candidato de producto en la barra`, `fix(sync): reintento con backoff al sincronizar catálogo`, `chore(tokens): regenerar tras cambio de accent`.
- **Un commit por unidad que funciona.** No acumular.
- Ramas: `main` (protegida) · `develop` · `feature/<nombre>`.
- Los commits que tocan el motor de sugerencias llevan `engine:` en el scope y su justificación en el cuerpo.

---

## Precedencia entre documentos

El sistema de Vyn se desincronizó porque cuatro notas prescribían una librería que el `CLAUDE.md` había descartado. Para que no vuelva a pasar:

1. **Valores de diseño (color, dimensión, motion):** gana `design/tokens.json`. Si `04.2 Foundations` dice otro número, la nota está vieja.
2. **Comportamiento de un componente:** ganan las fichas `04.3a` / `04.3b` del vault.
3. **Decisiones de producto y arquitectura:** gana el `CLAUDE.md` del vault (no éste) y `06.1 ADRs`.
4. **Reglas de código, estructura y stack:** gana **este** archivo.
5. Si encontrás una contradicción, **no la resuelvas en silencio**: anotala y preguntá. Las contradicciones del vault son información, no ruido.

---

## Errores conocidos — no repetir

Sección viva. **Cuando algo se rompe dos veces, se anota acá**, no en el vault: la memoria permanente tiene que estar donde Claude Code la lee automáticamente.

*(vacía — el proyecto no tiene código todavía. La primera entrada probablemente sea sobre el ciclo de vida del `InputMethodService`.)*

---

## Pendientes del proyecto que afectan lo que escribas

No los resuelvas por tu cuenta; si tu tarea choca con uno, decilo.

- **Dónde queda el QWERTY mientras se le escribe al panel ✨.** `04.4 Keyboard UX` dice que el panel reemplaza el teclado; `04.9` dice que hay un cuadro de texto donde el vendedor escribe. Las dos cosas no pueden ser ciertas. **Bloquea el componente central del producto.**
- **El flujo de marcado ganado/perdido no existe**, ni como boceto. Bloquea el pipeline y el benchmark de rendimiento, que es el único foso de datos del proyecto.
- **La UX del selector de dos ramas al derivar** (rama cliente vs. rama interna) no está diseñada. Es un error de una sola pulsación con consecuencia comercial.
- **Si se avisa o no al vendedor cuando el mensaje va a salir sin tarjeta** (sin red). Las dos respuestas tienen costo.
- ~~Si el catálogo vive en la barra o sigue siendo un modo~~ — **RESUELTO 2026-08-10:** las dos superficies existen, con regla de reparto. Ver `04.10 Vitrina — Especificación consolidada` §2
- **No hay escritorio deslizante** (swipe typing) en código abierto. Gboard lo tiene y el vendedor lo espera. Brecha de paridad sin solución conocida.
- **Landscape y split-screen sin diseñar.** En landscape el teclado en reposo no cabe en el 45%.

---

## Specs de producto — leer antes de implementar

| Área | Nota del vault |
|---|---|
| Contexto y decisiones del proyecto | `CLAUDE.md` (el del vault) · `06.1 ADRs` |
| Tokens, escalas, motion, accesibilidad | `04.2 Foundations` |
| Componentes del teclado | `04.3a Components — IME` |
| Componentes de la app compañera | `04.3b Components — App` |
| Estructura y modos del teclado | `04.4 Keyboard UX` |
| **Vitrina (spec autoritativo)** | **`04.10 Vitrina — Especificación consolidada`** |
| Barra de sugerencias, los 9 estados | `04.8 Barra de Sugerencias — Diseño de Interacción` |
| Panel ✨ y acciones ejecutables | `04.9 Botón ✨ — Chat Conversacional` |
| Qué llega realmente al chat | `06.5 Inserción — Hallazgos de Research` |
| Arquitectura del IME | `05.2 Teclado Nativo` |
| App de control | `05.3 App de Control` |
| API del backend | `05.4 Backend` |
| Privacidad y permisos | `05.7 Seguridad & Permisos` |
| Flujos de usuario | `03.3 User Flows` · `03.7 CRM Básico` |
| Tareas por fase | `06.4 Tareas` |
| Mapa de pantallas del vendedor | `Mapa de pantallas — Flujos de uso del teclado` |

---

## Variables de entorno

En `local.properties` (ignorado por git), nunca en el código:

```
KS_API_BASE_URL=
KS_API_BASE_URL_DEBUG=
```

No hay más. **No existe ninguna clave de proveedor de IA en el cliente**, por diseño (§Lo que este producto NUNCA hace, punto 5).

---

## Criterio de verificación antes de reportar "Done" (obligatorio)

No declares una tarea terminada, ni digas "listo"/"completado"/"commiteado" hasta haber verificado cada uno de estos puntos y mostrado el output real (no un resumen narrado):

1. **Build**: `./gradlew assembleDebug` — mostrar que terminó sin error.
2. **Tests**: `./gradlew test` (o el subset relevante) — mostrar el conteo real de tests pasados/fallados, no solo "tests verdes".
3. **Estado de git real** (no lo que "creés" que pasó):
   - `git status` — árbol limpio o cambios esperados, explícitamente.
   - `git log --oneline -3` — el commit que decís que existe, existe.
   - `git log origin/<rama>..HEAD --oneline` — si hay commits sin pushear, decilo explícitamente y pusheá antes de reportar terminado.
4. Si el paso implica un fix de UI/interacción: no alcanza con "debería funcionar ahora" — hay que describir qué comportamiento específico verificaste (o pedir que el humano lo confirme en dispositivo antes de cerrar el ticket).
5. **Supuestos explícitos**: lo que no verificaste de verdad (lo asumiste, lo inferiste, "debería andar") no se presenta como hecho. Márcalo **SUPUESTO — NO VERIFICADO AÚN**. La confusión del badge "D" (2026-08-18) enseñó que "debería verse" no es verificación: distinguir lo medido de lo supuesto es parte del reporte.
6. **Identidad de build en capturas**: toda captura/grabación que sirva de evidencia tiene que dejar claro qué build la produjo (hash corto en la esquina superior derecha del teclado en debug, o `Log.i("BuildInfo", "commit=… rama=…")` en logcat) y si es en vivo (recién tomada) o vieja. Una captura sin identidad de build no es evidencia.

Si cualquiera de estos falla, no digas "Done" — seguí iterando o reportá explícitamente el bloqueo. "Terminé" sin este chequeo no es una respuesta válida.
