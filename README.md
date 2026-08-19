# keyboard-sales-android

Cliente Android de **Keyboard Sales AI**: un teclado nativo (IME) mas una app
companiera, en **un solo APK**, para equipos de ventas en LatAm.

El producto trae el conocimiento de la empresa al vendedor **dentro del chat**,
en cualquier app de mensajeria. El teclado tiene dos botones:

| Boton | Que es |
|---|---|
| **G** | Catalogo y respuestas rapidas. Estatico y estructurado. Aca vive la velocidad: 2-3 toques. |
| **Chat** | Chat conversacional con conocimiento de la empresa. Es el corazon del producto. |

Este repositorio contiene **solo el cliente Android**. El backend (Go +
PostgreSQL), la plataforma de administracion y el web publico de catalogo viven
en repositorios separados y cerrados, y se consumen por HTTP.

---

## Licencia

**GPL-3.0-or-later.** Todo el contenido de este repositorio, teclado y app
companiera.

Es una consecuencia, no una eleccion de marketing: el teclado es un fork de
[HeliBoard](https://github.com/HeliBorg/HeliBoard), que es GPL-3.0, y decidimos
distribuir **un solo APK** porque la activacion del teclado ya es el punto mas
fragil del onboarding (meta: activacion >= 70%). Dos instaladores para no
liberar la app companiera nos costaria mas de lo que nos protege. Ver ADR-012.

Frontera: **liberamos el teclado, vendemos la plataforma.**

- `LICENSE` — aviso de copyright y como aplicarlo. **Tiene un TODO pendiente:
  bajar el texto oficial de la GPL-3.0 antes del primer push publico.**
- `NOTICE` — avisos de copyright de AOSP, OpenBoard, HeliBoard y nuestro. No se
  borran ni se reescriben.

> **Advertencia irreversible:** una vez publicado un commit bajo GPL-3.0, ese
> codigo no se puede "cerrar" retroactivamente. Se puede dejar de publicar
> versiones nuevas, pero lo liberado sigue libre y forkeable. Nada que no deba
> ser libre entra aca. En particular: claves, prompts propietarios del servidor,
> logica de precios y cualquier secreto de negocio van en el backend.

---

## Estructura

```
keyboard-sales-android/
├── keyboard/                 # Modulo IME. Fork de HeliBoard. Kotlin + XML Views.
│   └── src/main/res/values/tokens_colors.xml      (GENERADO)
├── app/                      # App companiera. Kotlin + Jetpack Compose.
│   └── src/main/java/com/keyboardsales/app/ui/theme/Tokens.kt   (GENERADO)
├── core/                     # PROPUESTA: datos compartidos (SQLDelight, API client).
├── design/                   # tokens.json + generadores + verificador de contraste.
├── docs/UPSTREAM.md          # Procedimiento de rebase contra HeliBoard.
└── .github/workflows/ci.yml
```

Los nombres de modulo `keyboard` y `app` no son arbitrarios: son los que
`design/gen-tokens.py` usa como ruta de salida. Cambiarlos rompe el generador y
el paso de CI que lo verifica.

`core/` es **PROPUESTA**: el proyecto decidio "sin KMP en el MVP", pero no
decidio si el cliente Android tiene un modulo de datos compartido entre el IME y
la app. La alternativa es que la app dependa del modulo `keyboard`. Definir antes
de la primera linea de codigo de sincronizacion.

---

## Tokens de diseño

`design/tokens.json` es la **unica fuente de verdad** de color, dimension,
tipografia y motion. Los archivos de tokens se **generan**; no se editan a mano.

```bash
# Regenerar los archivos de tokens de Android
python3 design/gen-tokens.py --out /tmp/gen-tokens && \
  cp /tmp/gen-tokens/android/keyboard/src/main/res/values/tokens_colors.xml       keyboard/src/main/res/values/tokens_colors.xml && \
  cp /tmp/gen-tokens/android/keyboard/src/main/res/values-night/tokens_colors.xml keyboard/src/main/res/values-night/tokens_colors.xml && \
  cp /tmp/gen-tokens/android/keyboard/src/main/res/values/tokens_dimens.xml       keyboard/src/main/res/values/tokens_dimens.xml && \
  cp /tmp/gen-tokens/android/app/src/main/java/com/keyboardsales/app/ui/theme/Tokens.kt \
     app/src/main/java/com/keyboardsales/app/ui/theme/Tokens.kt

# Verificar contraste (falla el CI si un par no cumple)
python3 design/check-contrast.py
```

`gen-tokens.py` escribe los cinco archivos de las dos plataformas de una sola
vez, con prefijos `android/` e `ios/`, porque nacio pensando en un arbol unico.
En multi-repo eso obliga a generar en un directorio temporal y copiar lo que
corresponde a esta plataforma, que es lo que hace el script de arriba y lo que
hace el CI.

> **PROPUESTA:** agregar a `gen-tokens.py` una bandera `--platform android|ios`
> que recorte los objetivos y permita usar `--check` directamente sobre el repo.
> Hoy `--check` falla en este repo porque busca tambien el archivo de iOS, que no
> existe aca. Es un arreglo de diez lineas y limpia el CI.

`design/` esta **vendorizado en este repo** y es la copia canonica (**PROPUESTA**:
los tokens no son secreto, este repo es publico, y asi el CI no necesita
credenciales para leerlos; el repo de iOS los consume clonando este). Si se
decide un repo de diseño aparte, actualizar tambien el CI de iOS.

`design/gen-tokens.py` menciona un `check-literals.py` que verifica que no haya
colores literales en el codigo. **Ese script todavia no existe.** Sin el, la
regla 4 del pipeline no se hace cumplir.

---

## Como compilar

Requisitos:

- JDK **17** &nbsp;`# AJUSTAR si el AGP definitivo exige otra`
- Android SDK con `compileSdk` **35** y `minSdk` **26** &nbsp;`# AJUSTAR`
- Python 3.11+ (solo para los scripts de diseño)
- No hace falta Android Studio: el build es Gradle puro.

```bash
git clone git@github.com:velasanti/keyboard-sales-android.git
cd keyboard-sales-android

# Debug, sin firmar. No necesita secrets ni backend.
./gradlew :app:assembleDebug

# Instalar en un dispositivo conectado
./gradlew :app:installDebug

# Lint y formato
./gradlew :app:lintDebug
# (No hay tarea de ktlint en este repo: el CI cae a un warning por eso, ver .github/workflows/ci.yml)

# Tests unitarios
./gradlew testDebugUnitTest
```

Despues de instalar, el teclado hay que **activarlo a mano** en Ajustes ->
Sistema -> Teclados. La app companiera guia ese flujo; es el punto donde se gana
o se pierde la activacion.

### Build de release

La firma se configura por variables de entorno, nunca por archivo commiteado.
Ver la lista de secrets en `SETUP-GITHUB.md`.

```bash
KEYSTORE_PATH=... KEYSTORE_PASSWORD=... KEY_ALIAS=... KEY_PASSWORD=... \
  ./gradlew :app:assembleRelease
```

---

## Meta de peso

| Objetivo | Limite |
|---|---|
| APK completo (teclado + app companiera) | **< 25 MB** |
| Solo el teclado | **12-15 MB** |

El CI mide el APK completo y falla si pasa de 25 MB. **PROPUESTA:** el limite de
"solo teclado" no es medible con un APK unico; para vigilarlo hace falta o una
variante de build `keyboardOnly`, o medir el tamaño de los artefactos del modulo
`keyboard` (dex + recursos) y usar eso como proxy. Definir cual antes de que el
APK crezca, porque despues el numero solo sube.

Los diccionarios de español LatAm pesan y viven en `keyboard-sales-dictionaries`,
compilados con el `dicttool` de AOSP. Cuantos idiomas se empaquetan en el APK
contra cuantos se bajan a demanda es la palanca de peso mas grande que tenemos.

---

## Relacion con upstream (HeliBoard)

Este repo es un **fork mantenido**, no un punto de partida que se olvida:

- `upstream` es un remoto **permanente**, no un clon de una sola vez.
- La regla es **minimizar el diff con upstream**.
- Todo lo nuestro entra como **modulo aditivo** (barra de catalogo,
  sincronizacion, log de eventos), **no** como edicion a la tuberia de
  sugerencias.
- Todo cambio que toque el motor de sugerencias necesita **justificacion
  explicita en el PR** (hay una casilla obligatoria en la plantilla).
- Los avisos de copyright de AOSP, OpenBoard y HeliBoard se preservan.

Procedimiento completo, cadencia de rebase y que hacer ante un conflicto en el
motor de sugerencias: **`docs/UPSTREAM.md`**.

No se usa el boton "Fork" de GitHub. El motivo esta en `SETUP-GITHUB.md`
seccion 5.

---

## Ramas y commits

- `main` — protegida. Solo por PR. Siempre compilable.
- `develop` — integracion.
- `feature/*` — trabajo en curso.
- `upstream-sync/*` — ramas de rebase contra HeliBoard.

Conventional Commits obligatorio:

```
feat(catalog): barra de sugerencias con resultados de catalogo
fix(sync): reintento con backoff al perder red
chore(upstream): rebase sobre HeliBoard 3.2
```

Ambitos de uso frecuente: `keyboard`, `catalog`, `chat`, `sync`, `events`,
`app`, `design`, `upstream`, `ci`.

---

## Privacidad, y por que se puede auditar aca

Este cliente es publico a proposito: es lo que hace verificable el claim del
producto. Lo que el codigo tiene que poder demostrar a quien lo lea:

- **No usamos `AccessibilityService` ni lectura de pantalla.** No podemos leer
  los mensajes entrantes del cliente final, y nunca vamos a poder (ADR-009).
- No guardamos el contenido de las conversaciones, ni resumenes ni
  interpretaciones de lo que dijo el cliente (ADR-011).
- **Si** guardamos el contacto (telefono, y nombre si el vendedor lo asigna) y
  las acciones que el vendedor ejecuta desde el teclado sobre ese contacto
  (ADR-011). Somos custodios de la base de contactos.
- La inferencia va **siempre via backend**, nunca directo desde el cliente. No
  hay claves de proveedores de IA en este APK. Un APK publico con una clave
  embebida es una clave regalada.

El claim es sobre **alcance**, no sobre ubicacion: *no leemos ni guardamos lo que
dice tu cliente, solo lo que hace tu vendedor.* Nunca se escribe "IA
on-device", "on-premise", "VPC propia" ni "residencia de datos en LatAm": es
falso para esta arquitectura.

Si un PR agrega un permiso al manifest, la revision explica **por que**. El
manifest de un IME es lo primero que mira quien audita.
