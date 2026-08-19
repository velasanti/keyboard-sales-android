# Gotchas — errores conocidos y por que pasan

Este archivo es para que un agente de codigo lo lea antes de tocar algo
que ya rompio a alguien. No es documentacion de producto (eso vive en
Obsidian) — es memoria tecnica barata de cargar.

Regla: cuando algo falla por una causa no obvia, se anota aca antes de
cerrar la tarea. No se documenta despues.

## Indice

1. El modulo keyboard/ no existe
2. PULL_REQUEST_TEMPLATE.md en mayusculas
3. ignoreAssetsPatterns es asignacion, no suma
4. .literals-baseline indexado por archivo:linea
5. Push inicial de 288 MB falla por HTTP/2
6. Medir peso de APK con unzip -v, no tamano crudo
7. Antes de afirmar algo del artefacto, leer la config completa
8. uiautomator dump no ve el IME en One UI / A51
9. adb shell input text y caracteres especiales
10. BOM de Compose mayor a 2025.11.01 exige minSdk 23
11. og:image con ruta relativa falla en silencio
12. gen-tokens.py sin --platform android escribe mal
13. Colores a mano en el XML generado se pierden al regenerar

---

## 1. El modulo keyboard/ no existe

HeliBoard es un solo modulo de aplicacion: :app. El IME *es* :app.
Apuntar herramientas o scripts a un modulo keyboard/ crea un modulo
fantasma que Gradle no compila. Paso de verdad al generar los tokens.

## 2. PULL_REQUEST_TEMPLATE.md en mayusculas

Upstream lo tiene en mayusculas. La variante en minusculas parece
funcionar en macOS (case-insensitive) pero en el runner Linux del CI
existen las dos versiones y GitHub elige cualquiera. Escribir siempre
PULL_REQUEST_TEMPLATE.md, nunca pull_request_template.md.

## 3. ignoreAssetsPatterns es asignacion, no suma

En app/build.gradle.kts, upstream usa esta lista para excluir
main_ro.dict del paquete. Nosotros la extendemos para no empacar
idiomas que no vendemos. Es asignacion, no suma: en cada rebase de
upstream el merge choca ahi y hay que fusionar las dos listas a mano.
Es la palanca de mayor peso del tamano del APK.

## 4. .literals-baseline indexado por archivo:linea

Congela los cerca de 992 literales heredados de upstream. Si upstream
corre lineas por un rebase, aparecen falsos positivos. Regenerar
despues de cada rebase.

## 5. Push inicial de 288 MB falla por HTTP/2

RPC failed, HTTP 400, al pushear un repo grande por primera vez.
Arreglo: correr git config http.version HTTP/1.1
HTTP/2 tiene problemas conocidos con packs grandes.

## 6. Medir peso de APK con unzip -v, no tamano crudo

El tamano del archivo .apk en disco no es el tamano real instalado.
Medir con unzip -v, que da el tamano comprimido, para que el numero
sea comparable contra la meta de 25 MB.

## 7. Antes de afirmar algo del artefacto, leer la config completa

Regla nacida de un error real: se concluyo algo sobre debug o release
sin leer build.gradle.kts completo primero. Aplica a cualquier
afirmacion sobre el build, no solo al peso del APK.

## 8. uiautomator dump no ve el IME en One UI / A51

Algunos skins de OEM (confirmado: One UI en un Galaxy A51, tras
reinstalar con manejo de firma) dejan de exponer la ventana propia del
IME en uiautomator dump, aunque siguen exponiendo bien las vistas del
host, por ejemplo la lista de mensajes de WhatsApp. No perder tiempo
intentando arreglar el quirk del OEM. Fallback: screenshot mas
deteccion por color de pixel en una region conocida, calibrada al
dispositivo real. Detalle completo en el skill android-ime-physical-qa.

## 9. adb shell input text y caracteres especiales

El shell remoto interpreta caracteres como el numeral. Comillar con
cuidado el texto que se envia por adb shell input text.

## 10. BOM de Compose mayor a 2025.11.01 exige minSdk 23

Un BOM de Compose mas nuevo que 2025.11.01 arrastra
material-android:1.10.0, que exige minSdk 23. Verificar contra la meta
de gama baja antes de subir la version del BOM.

## 11. og:image con ruta relativa falla en silencio

La pagina se ve perfecta en el navegador, que si resuelve rutas
relativas, pero el crawler de WhatsApp no ejecuta JavaScript ni
resuelve igual, y el preview simplemente no sale, sin error en ningun
lado. Usar siempre URL absoluta HTTPS en og:image. Es el bug mas facil
de introducir y el mas dificil de notar en esta parte del sistema.

## 12. gen-tokens.py sin --platform android escribe mal

El comando que corre en CI es gen-tokens.py --check --platform android
--out punto. Correrlo sin --platform genera para todas las plataformas
y escribe en carpetas nuevas android/ e ios/ en la raiz del repo, en
vez de en las rutas reales como app/src/main/res. Pasa desapercibido
porque el comando funciona con exit 0, solo escribe en el lugar que no
es. Siempre usar --platform android --out punto al regenerar en este
repo.

## 13. Colores a mano en el XML generado se pierden al regenerar

design/tokens.json es la unica fuente de verdad. Si alguien agrega un
color directo en tokens_colors.xml, que es generado, sin pasar por
tokens.json, el proximo gen-tokens.py lo borra sin avisar, porque el
generador sobreescribe el archivo entero desde la fuente.

Caso real, 2026-08-19: vitrina_switch_selected_bg,
vitrina_switch_unselected_bg, vitrina_switch_selected_fg y
vitrina_switch_unselected_fg existian solo en el values de modo claro,
referenciados desde VitrinaSwitch.kt, pero no en values-night ni en
tokens.json. El check Tokens y contraste del CI viene fallando por
esto desde el 2026-08-18 sin que nadie lo notara, porque enforce_admins
estaba desactivado y los pushes a main entraban igual con el CI en
rojo.

Pendiente de resolver: agregar estos 4 colores a tokens.json con sus
dos modos light y dark, incluyendo un primitivo nuevo para
unselected_bg, valor F0F2F5, que no coincide con ningun primitivo
existente. Los otros tres coinciden exacto con primitivos ya
definidos: neutral/50, neutral/900, neutral/600. El valor de dark mode
para los cuatro esta sin decidir, es una decision de diseno, no
tecnica.

Regla derivada: cualquier color que necesite el codigo va primero a
design/tokens.json, nunca directo al XML generado.
