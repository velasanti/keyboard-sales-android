# Relacion con upstream: HeliBoard

Este repositorio es un **fork mantenido** de
[HeliBoard](https://github.com/HeliBorg/HeliBoard) (GPL-3.0), que a su vez
desciende de OpenBoard y de AOSP LatinIME.

"Mantenido" quiere decir que `upstream` es un remoto **permanente** y que
sincronizamos con cadencia. No es un clon de una sola vez que despues divergimos
para siempre. La razon es concreta: HeliBoard arregla bugs del motor de entrada,
soporte de idiomas y compatibilidad con versiones nuevas de Android. Si nos
quedamos atras, eso pasa a ser trabajo nuestro, y no es el trabajo en el que
tenemos ventaja. Nuestra ventaja es el conocimiento de la empresa dentro del
chat, no reimplementar un IME.

---

## 1. Las cuatro reglas

**Regla 1 — minimizar el diff con upstream.**
El diff con `upstream/main` es una metrica de mantenimiento. Cada archivo
heredado que tocamos lo vamos a pagar en cada rebase, para siempre. Ante dos
soluciones que funcionan, gana la que toca menos archivos de upstream, aunque sea
menos elegante.

**Regla 2 — lo nuestro va como modulo aditivo.**
La barra de catalogo, el boton G, el chat conversacional, la sincronizacion con
el backend y el log de eventos viven en codigo **nuevo**, en paquetes propios,
enganchados con la menor superficie de contacto posible. No se implementan
editando la tuberia de sugerencias de HeliBoard.

**Regla 3 — todo cambio al motor de sugerencias necesita justificacion explicita
en el PR.**
Hay una casilla obligatoria en la plantilla de PR. Un PR que la marca sin
justificar se cierra sin revisar. Ver seccion 6.

**Regla 4 — se preservan los avisos de copyright.**
AOSP, OpenBoard y HeliBoard. Estan en `NOTICE` y en los encabezados de los
archivos heredados. No se borran, no se reescriben, no se reemplazan por el
nuestro. Si el cambio en un archivo heredado es sustancial, se **agrega** una
linea de copyright nuestra debajo de la existente. El CI verifica que `NOTICE`
conserve los tres avisos.

---

## 2. Configuracion inicial de los remotos

Se hace **una sola vez**, al crear el repo. No se usa el boton "Fork" de GitHub
(el motivo esta en `SETUP-GITHUB.md` seccion 5).

```bash
# 1. Clonar HeliBoard, con historial completo. El historial importa: es lo que
#    hace posible el rebase y es lo que la GPL espera que se pueda auditar.
git clone https://github.com/HeliBorg/HeliBoard.git keyboard-sales-android
cd keyboard-sales-android

# 2. El "origin" heredado del clon es HeliBoard. Se renombra a upstream y se
#    pone en solo-lectura, para que nadie pushee ahi por accidente.
git remote rename origin upstream
git remote set-url --push upstream DISABLED

# 3. origin pasa a ser nuestro repo
git remote add origin git@github.com:velasanti/keyboard-sales-android.git

# 4. Marcar el punto de partida. Esta etiqueta es la referencia de todos los
#    diffs de mantenimiento de aca en adelante.
git tag -a upstream-base -m "Punto de fork: HeliBoard <version>"   # AJUSTAR version

# 5. Verificar antes de pushear
git remote -v
#   origin    git@github.com:velasanti/keyboard-sales-android.git (fetch)
#   origin    git@github.com:velasanti/keyboard-sales-android.git (push)
#   upstream  https://github.com/HeliBorg/HeliBoard.git (fetch)
#   upstream  DISABLED (push)

git push -u origin main
git push origin --tags
git branch develop && git push -u origin develop
```

**Antes de ese ultimo `push`, dos cosas que no se pueden deshacer despues:**

1. `LICENSE` tiene que tener el texto oficial de la GPL-3.0 (hoy es un
   marcador con un TODO).
2. La auditoria de licencias de la app companiera tiene que estar hecha. Ver
   `SETUP-GITHUB.md` seccion 9.

---

## 3. Cadencia de sincronizacion

| Cuando | Que |
|---|---|
| **Cada release estable de HeliBoard** | Sincronizacion completa. Es el disparador principal. |
| **Como maximo cada 6 semanas** | Aunque no haya release. Un fork que pasa un trimestre sin sincronizar deja de ser sincronizable en la practica. |
| **Fuera de cadencia** | Parche de seguridad en upstream, o soporte para una version nueva de Android. Se sincroniza cuando aparece, sin esperar. |
| **Nunca** | En la semana anterior a una demo o al arranque del piloto. Un rebase es un evento de riesgo; no se mete al lado de un compromiso externo. |

**PROPUESTA de proceso:** un issue recurrente etiquetado `upstream-sync` con la
fecha objetivo, y un dueño fijo. Un mantenimiento que es "de todos" no lo hace
nadie. Con un solo desarrollador, es una tarea agendada de medio dia.

Adicional: `git fetch upstream` y una mirada al changelog **cada semana**, sin
integrar. Enterarse temprano de un cambio grande en upstream vale mas que
integrarlo rapido.

---

## 4. Rebase o merge

**Merge, no rebase, para integrar upstream.** El nombre "rebase" quedo en el
vocabulario del proyecto y sirve para nombrar la operacion, pero la mecanica
recomendada es merge:

- Un rebase de nuestra rama sobre upstream **reescribe nuestro historial**. En un
  repo publico con contribuciones externas y con `main` protegida, eso obliga a
  force-push sobre una rama compartida. Se paga cada vez.
- Un merge deja el historial de las dos partes intacto y el conflicto se resuelve
  **una vez**, no una vez por commit nuestro.
- La GPL-3.0 no exige ninguna de las dos, pero un historial que se reescribe hace
  mas dificil auditar la procedencia del codigo, que es justamente lo que nos
  compramos publicando.

**PROPUESTA** (es una decision tecnica que el vault no tomo): merge con commit de
merge explicito, en una rama `upstream-sync/*`, integrada a `develop` por PR.

---

## 5. Procedimiento de sincronizacion

```bash
# 0. Arbol limpio. Si hay trabajo sin commitear, se para aca.
git status --porcelain     # tiene que salir vacio

# 1. Traer upstream y ver que viene
git fetch upstream --tags
git log --oneline --no-merges HEAD..upstream/main | wc -l    # cuantos commits
git log --oneline --no-merges HEAD..upstream/main             # cuales

# 2. Medir el riesgo ANTES de tocar nada: que archivos que tocamos nosotros
#    tambien cambio upstream. Esta es la lista de conflictos probables.
comm -12 \
  <(git diff --name-only upstream-base..HEAD          | sort -u) \
  <(git diff --name-only HEAD...upstream/main         | sort -u)

# 3. Rama de sincronizacion
git switch develop && git pull
git switch -c upstream-sync/$(date +%Y-%m)

# 4. Integrar
git merge upstream/main
#    Si hay conflictos: resolver de a un archivo, ver seccion 6 para el criterio.
#    git status muestra la lista; git checkout --ours / --theirs solo cuando el
#    archivo es claramente de un lado.

# 5. Verificar. En este orden, porque cada paso es mas caro que el anterior.
python3 design/check-contrast.py
./gradlew ktlintCheck
./gradlew :app:assembleDebug
./gradlew testDebugUnitTest

# 6. Cuanto crecio nuestro diff con upstream. Si crecio y no agregamos features,
#    algo se resolvio mal en un conflicto.
git diff --stat upstream/main..HEAD

# 7. Probar EN DISPOSITIVO. Un IME que compila puede igual no escribir.
#    Checklist de humo en la seccion 7.
./gradlew :app:installDebug

# 8. PR a develop
git push -u origin HEAD
```

Commit del merge, con Conventional Commits:

```
chore(upstream): merge HeliBoard 3.2 (47 commits)

Conflictos resueltos: 3
  - keyboard/src/main/java/.../KeyboardSwitcher.kt   (aditivo, nuestro hook re-aplicado)
  - keyboard/src/main/res/layout/main_keyboard_frame.xml (upstream, nuestra barra re-insertada)
  - build.gradle.kts                                 (upstream, AGP 8.7)
Motor de sugerencias: no tocado.
Diff con upstream: 41 archivos (antes 41).
```

---

## 6. Conflicto en el motor de sugerencias

Es el caso peligroso y tiene procedimiento propio.

**Que cuenta como "motor de sugerencias":** la tuberia de correccion y
prediccion, el acceso a diccionarios binarios, el codigo nativo de correccion, y
la vista de la barra de sugerencias. Concretamente, y sujeto a los nombres reales
del arbol de HeliBoard:

```
keyboard/src/main/java/.../latin/inputlogic/
keyboard/src/main/java/.../latin/Suggest*.kt
keyboard/src/main/java/.../latin/suggestions/
keyboard/src/main/java/.../latin/Dictionary*.kt
keyboard/src/main/jni/                       (correccion nativa)
```

`# AJUSTAR: fijar las rutas exactas al hacer el clon y ponerlas en CODEOWNERS.`

**PROPUESTA:** un `.github/CODEOWNERS` que marque esas rutas, para que GitHub
pida revision automaticamente en cualquier PR que las toque. Es la forma de
hacer cumplir la regla 3 sin depender de que alguien se acuerde.

### Criterio de resolucion, en orden

1. **Gana upstream.** El default no es negociable: se toma la version de
   upstream y se re-aplica nuestro hook aditivo **por encima**, sin modificar la
   logica de upstream. En la mayoria de los conflictos alcanza, porque nuestro
   codigo deberia estar solo enganchado, no entretejido.

2. **Si no se puede re-aplicar el hook**, no se parchea el motor: se busca un
   punto de engance distinto y mas afuera. La pregunta correcta no es "como
   arreglo este conflicto" sino "por que mi codigo esta tan adentro". Un
   conflicto repetido en el motor es la señal de que la regla 2 se rompio en
   algun PR anterior.

3. **Si tampoco hay punto de engance afuera**, se documenta como deuda: un
   archivo `docs/UPSTREAM-DIVERGENCIAS.md` con una entrada por divergencia
   (archivo, que cambia, por que, y que haria falta para eliminarla). Una
   divergencia que nadie escribio la redescubre el que hace el rebase seis meses
   despues, a las once de la noche.

4. **Nunca** se resuelve un conflicto del motor con "lo dejo como estaba
   nuestro" sin leer que cambio upstream. Si upstream cambio ahi, probablemente
   arreglo un bug, y descartarlo es reintroducirlo.

5. Si la resolucion cuesta mas de **medio dia**, se para y se decide en equipo:
   sincronizar parcial (tomar upstream salvo esos archivos y agendar el resto) es
   mejor que un merge apurado en la logica de correccion de texto, que es
   exactamente el codigo cuyos bugs el usuario nota primero y reporta peor.

### Lo que hay que probar despues de un conflicto en el motor

No hay tests de integracion de un IME que valgan tanto como cinco minutos de
tipear. Ver seccion 7.

---

## 7. Checklist de humo despues de cada sincronizacion

En dispositivo fisico, con el teclado como IME por defecto:

- [ ] Escribir un parrafo en español con acentos y con `ñ`.
- [ ] Autocorreccion: escribir mal una palabra a proposito y ver la sugerencia.
- [ ] Barra de sugerencias: aparece, se puede tocar, inserta.
- [ ] Cambio de idioma (español / ingles) y layout.
- [ ] Emoji y borrado (incluido borrado de emoji compuesto).
- [ ] Portapapeles: copiar y pegar.
- [ ] Rotacion de pantalla con el teclado abierto.
- [ ] Modo claro y modo oscuro.
- [ ] **Boton G**: catalogo, busqueda, insercion de producto. 2-3 toques.
- [ ] **Boton de chat**: abre, responde, inserta.
- [ ] Sigilo de productos con `#`, con la tecla visible en la capa principal.
- [ ] En **al menos dos apps de mensajeria distintas** (ADR-015).
- [ ] Sin red: el teclado escribe igual. La composicion de mensaje con producto
      si exige red, a proposito (tension 2 / ADR-017), pero tipear nunca.
- [ ] Peso del APK: no crecio mas de lo esperado.

---

## 8. Contribuir de vuelta a upstream

Si un arreglo es de interes general y no especifico de nuestro producto, va
**primero a HeliBoard**. Tres razones, todas practicas:

1. Reduce nuestro diff: lo que esta en upstream deja de ser mantenimiento
   nuestro.
2. Un fork que solo consume tiende a que upstream ignore sus necesidades.
3. Es la obligacion de la GPL en espiritu, y ya cumplimos la letra publicando.

La plantilla de PR tiene una casilla para esto. **No** se manda upstream nada de
catalogo, sincronizacion, log de eventos ni chat conversacional: no le sirve a un
teclado de proposito general y expone nuestro roadmap.

---

## Excepciones a la regla de diff aditivo

La regla madre del fork es que **todo lo nuestro es aditivo**: archivos nuevos, nunca ediciones a archivos de upstream. Hay exactamente tres excepciones, y estan aca para que aparezcan como decisiones y no como sorpresas en el proximo rebase.

| Archivo de upstream | Que hacemos | Al rebasear |
|---|---|---|
| `README.md` | Lo reemplazamos entero. Es la puerta de entrada de *nuestro* repo y tiene que decir que es un fork con otro proposito, con nuestras metas de peso y nuestro pipeline de tokens | **Siempre tomar la nuestra.** `git checkout --ours README.md` |
| `.github/PULL_REQUEST_TEMPLATE.md` | Lo reemplazamos entero. El nuestro tiene la casilla obligatoria de "este PR toca el motor de sugerencias", que es el control que mantiene el fork mantenible | **Siempre tomar la nuestra.** `git checkout --ours .github/PULL_REQUEST_TEMPLATE.md` |
| `.gitignore` | Lo **extendemos**, no lo reemplazamos: el de upstream ignora cosas de Gradle y del NDK que el nuestro no cubre. Nuestro bloque va al final, detras del separador `# ── Keyboard Sales AI ──` | **Fusionar**: tomar el de upstream y re-agregar nuestro bloque |

> **Ojo con las mayusculas de `PULL_REQUEST_TEMPLATE.md`.** Upstream lo tiene en mayusculas. Si se crea la variante en minusculas, en macOS (case-insensitive) los dos son el mismo archivo y parece funcionar, pero en el runner de Linux del CI existen **los dos** y GitHub elige cualquiera. Usar siempre la grafia exacta de upstream.

Cualquier cuarta excepcion necesita justificacion explicita en el PR. Si la lista crece, el fork esta divergiendo y se pierde la unica razon de haber forkeado.

## Que regenerar despues de cada rebase

1. **El baseline de literales.** Esta indexado por `archivo:linea`; si upstream corre las lineas aparecen falsos positivos.
   ```bash
   python3 design/check-literals.py . --dimensions \
     | awk '/^  [^ ]+:[0-9]+/ {print $1}' > .literals-baseline
   ```
2. **Verificar que `settings.gradle` sigue teniendo un solo modulo de aplicacion.** Si upstream parte `:app` en varios modulos, las rutas de `design/gen-tokens.py` dejan de ser validas.

---

## Lo que de upstream NO gateamos, y por que

Nuestro CI custodia **nuestro** codigo. Un check que falla por codigo de HeliBoard no protege nada: entrena al equipo a ignorar el rojo. Estos tres casos estan acotados a proposito.

### 1. Los tests de HeliBoard no se corren

`./gradlew testDebugUnitTest` a secas ejecuta la suite de upstream, y **9 de sus 14 tests fallan en el runner**:

```
SubtypeTest > classMethod FAILED
    java.lang.UnsupportedOperationException at DefaultSdkProvider.java:170
XLinkTest · InputLogicTest · StringUtilsTest · SuggestTest — igual
14 tests completed, 9 failed
```

`DefaultSdkProvider` es de **Robolectric** y esa excepcion es la de Robolectric no resolviendo los jars del SDK de Android en el entorno de CI. **Es una condicion preexistente de upstream, no la introdujimos nosotros:** el propio `build-test-auto.yml` de HeliBoard falla identico en este fork.

Nuestro paso corre `--tests "com.keyboardsales.*"` y avisa mientras no existan tests propios. **Cuando escribamos los primeros tests** —normalizacion de busqueda, ranking, construccion del mensaje: los tres puntos donde un bug es invisible y caro— este paso pasa a ser bloqueante de verdad sin tocar nada mas.

Pendiente, de prioridad baja: averiguar si los tests de upstream se pueden hacer pasar (probablemente sea version de Robolectric contra JDK 21, o prefetch de los jars). No bloquea nada nuestro.

### 2. `:app:lintDebug` es informativo hasta que exista un baseline

Lint analiza **todo** el modulo `:app`, que es casi todo codigo de upstream. La solucion es la misma que ya funciono con los literales: congelar la deuda heredada en un baseline.

```bash
./gradlew :app:updateLintBaseline    # genera app/lint-baseline.xml
```

Generarlo con el repo todavia 100% upstream, commitearlo, y **sacar el `continue-on-error` del paso** en `.github/workflows/ci.yml`. Desde ahi lint solo marca hallazgos nuevos, que son los nuestros. Igual que `.literals-baseline`, **hay que regenerarlo despues de cada rebase**.

### 3. El workflow `build-test-auto.yml` de upstream se conserva

No lo borramos: seria una cuarta excepcion a la regla de diff aditivo y agrega friccion en cada rebase. Va a aparecer en rojo en los PRs por el motivo del punto 1. **No incluirlo en los checks requeridos de la proteccion de rama.**

## Que regenerar despues de cada rebase — lista completa

1. **`.literals-baseline`** — indexado por `archivo:linea`; si upstream corre las lineas aparecen falsos positivos.
   ```bash
   python3 design/check-literals.py . --dimensions \
     | awk '/^  [^ ]+:[0-9]+/ {print $1}' > .literals-baseline
   ```
2. **`app/lint-baseline.xml`**, cuando exista (punto 2 de arriba).
3. **Los cuatro archivos de tokens**, si upstream movio algo de `app/src/main/res/`.
   ```bash
   python3 design/gen-tokens.py --out . --platform android
   ```
4. **Verificar que `settings.gradle` sigue teniendo un solo modulo de aplicacion.** Si upstream parte `:app`, las rutas de `gen-tokens.py` dejan de ser validas.
5. **Verificar que `app/build.gradle.kts` sigue habilitando Compose.** Si upstream lo saca, `Tokens.kt` deja de compilar.
