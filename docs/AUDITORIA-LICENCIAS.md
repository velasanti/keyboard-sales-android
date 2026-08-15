# Auditoría de licencias — dependencias

> Auditoría completa de las dependencias declaradas del repo (las nuestras y las
> heredadas de HeliBoard), para verificar compatibilidad con GPL-3.0 antes de
> distribuir el APK. Fecha: 2026-08-15.
>
> Regla que esto custodia (CLAUDE.md, Reglas del fork): *toda dependencia nueva
> se audita contra GPL-3.0 antes de agregarse*. Este documento es el inventario
> actual. En un PR que agregue una dependencia, actualizá esta tabla.

## Resumen

- **Total de dependencias declaradas:** 22.
- **Compatibles (MIT / Apache-2.0 / GPL-3.0 / GPL-2.0-or-later):** 20.
- **Con matiz (ver notas):** 2 — `desugar_jdk_libs` (GPL-2.0 con Classpath
  Exception) y `junit` (EPL-1.0, test-only, no entra al APK).
- **No compatibles:** 0.
- **"No pude determinar":** 0 — todas se verificaron contra fuente primaria
  (POM del repositorio de la librería o LICENSE de su repo).

## Tabla

| Dependencia | Versión | Declaración | Licencia | Clasificación | Fuente |
|---|---|---|---|---|---|
| androidx.core:core-ktx | 1.17.0 | app · runtime | Apache-2.0 | ✅ Compatible | POM Google Maven |
| androidx.recyclerview:recyclerview | 1.4.0 | app · runtime | Apache-2.0 | ✅ Compatible | POM Google Maven |
| androidx.autofill:autofill | 1.3.0 | app · runtime | Apache-2.0 | ✅ Compatible | POM Google Maven |
| androidx.viewpager2:viewpager2 | 1.1.0 | app · runtime | Apache-2.0 | ✅ Compatible | POM Google Maven |
| org.jetbrains.kotlinx:kotlinx-serialization-json | 1.11.0 | app · runtime | Apache-2.0 | ✅ Compatible | POM Maven Central |
| androidx.compose:compose-bom | 2025.11.01 | app · platform | Apache-2.0 | ✅ Compatible | POM Google Maven |
| androidx.compose.material3:material3 | del BOM | app · runtime | Apache-2.0 | ✅ Compatible | BOM / proyecto |
| androidx.compose.ui:ui-tooling-preview | del BOM | app · runtime | Apache-2.0 | ✅ Compatible | BOM / proyecto |
| androidx.compose.ui:ui-tooling | del BOM | app · debug | Apache-2.0 | ✅ Compatible | BOM / proyecto |
| androidx.navigation:navigation-compose | 2.9.8 | app · runtime | Apache-2.0 | ✅ Compatible | POM Google Maven |
| sh.calvin.reorderable:reorderable | 3.1.0 | app · runtime | Apache-2.0 | ✅ Compatible | LICENSE del repo |
| com.github.skydoves:colorpicker-compose | 1.1.3 | app · runtime | Apache-2.0 | ✅ Compatible | LICENSE del repo |
| com.android.tools:desugar_jdk_libs | 2.1.5 | app · coreLibraryDesugaring | GPL-2.0 con Classpath Exception | ⚠️ Compatible con matiz | POM Google Maven + LICENSE del repo |
| org.jetbrains.kotlin:kotlin-stdlib | 2.3.20 | tools/make-emoji-keys | Apache-2.0 | ✅ Compatible | POM Maven Central |
| kotlin("test") | Kotlin 2.3.20 | app · test | Apache-2.0 | ✅ Compatible | POM Maven Central |
| junit:junit | 4.13.2 | app · test | EPL-1.0 | ⚠️ No GPL-compatible, pero test-only | POM Maven Central |
| org.mockito:mockito-core | 5.23.0 | app · test | MIT | ✅ Compatible | POM Maven Central |
| org.robolectric:robolectric | 4.16.1 | app · test | MIT | ✅ Compatible | POM Maven Central |
| androidx.test:runner | 1.7.0 | app · test | Apache-2.0 | ✅ Compatible | POM Google Maven |
| androidx.test:core | 1.7.0 | app · test | Apache-2.0 | ✅ Compatible | POM Google Maven |
| com.android.tools.build:gradle (AGP) | 8.13.2 | buildscript · classpath | Apache-2.0 | ✅ Compatible (build-time) | repo AGP |
| org.jetbrains.kotlin:kotlin-gradle-plugin | 2.3.20 | buildscript · classpath | Apache-2.0 | ✅ Compatible (build-time) | repo Kotlin |

## Notas sobre las dos con matiz

### 1. `com.android.tools:desugar_jdk_libs:2.1.5` — GPL-2.0 con Classpath Exception

El POM de Google Maven la declara literal como *"GNU General Public License,
version 2, with the Classpath Exception"*. No es "GPL-2.0-or-later" puro, pero la
Classpath Exception es el mecanismo estándar (el mismo de GNU Classpath y
OpenJDK) que permite explícitamente incrustar o linkear la librería en cualquier
programa. Su código desugarizado se fusiona en el dex del APK, que es
exactamente el uso para el que Google la publica bajo ese licenciamiento.

**Conclusión:** compatible para el uso que le damos. Si se quiere cerrar del
todo, la letra de la excepción está en
`https://github.com/google/desugar_jdk_libs/blob/master/LICENSE`.

### 2. `junit:junit:4.13.2` — EPL-1.0

EPL-1.0 no es GPL-compatible en sentido estricto, pero esta dependencia es
**test-only**: no entra al APK distribuido. No afecta la distribución del
producto. Solo habría que re-auditar si algún día se distribuye un artefacto que
incluya los tests.

## Otras cosas que entran al APK y cómo quedan

- **Código JNI** (`app/src/main/jni/`) — cabeceras AOSP: Apache-2.0. Es código
  del propio fork, no una dependencia.
- **Assets** (diccionarios, emoji, layouts) — del fork de HeliBoard: GPL-3.0.
- **Sin librerías vendorizadas** — no hay `.aar`/`.jar` en `app/libs` ni
  `jniLibs` de terceros.
- **Gradle wrapper 8.14** — Apache-2.0 (herramienta de build, no se distribuye).
- **Plugins de Kotlin** (`plugin.serialization`, `plugin.compose`) — son parte
  del compilador Kotlin 2.3.20 (Apache-2.0), build-time.

## Cómo se verificó cada fila

- **POM de Google Maven** — bloque `<licenses>` del POM de la versión exacta:
  `https://dl.google.com/dl/android/maven2/<grupo>/<artefacto>/<version>/*.pom`.
- **POM de Maven Central** — idem en
  `https://repo1.maven.org/maven2/<grupo>/<artefacto>/<version>/*.pom`.
- **LICENSE del repo** — se usó cuando el POM no trae bloque `<licenses>`
  (caso de las librerías publicadas por JitPack: `sh.calvin.reorderable` y
  `com.github.skydoves:colorpicker-compose`, ambas Apache-2.0 según el LICENSE de
  su repositorio en GitHub).