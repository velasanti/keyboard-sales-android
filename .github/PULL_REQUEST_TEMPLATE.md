# Que hace este PR

<!-- Una o dos frases. Que cambia para el vendedor o para el admin. -->

Issue relacionado: #

## Tipo de cambio

- [ ] `feat` — funcionalidad nueva
- [ ] `fix` — correccion
- [ ] `refactor` — sin cambio de comportamiento
- [ ] `chore(upstream)` — rebase o sincronizacion con HeliBoard
- [ ] `docs` / `ci` / `test`

---

## OBLIGATORIO — Motor de sugerencias

> Regla del fork: los cambios propios van como **modulo aditivo**, no como
> ediciones a la tuberia de sugerencias. Cada linea que tocamos ahi la vamos a
> pagar en cada rebase contra upstream, para siempre.

- [ ] **Este PR NO toca el motor de sugerencias** ni la tuberia de correccion,
      prediccion, diccionarios o `SuggestionStripView` y alrededores.
- [ ] **Este PR SI toca el motor de sugerencias.** Justificacion obligatoria abajo.

<!--
Si marcaste "SI", nada de esto es opcional. Un PR con la casilla marcada y esta
seccion vacia se cierra sin revisar.

**Que se toca exactamente** (archivos y funciones):

**Por que no se puede hacer de forma aditiva** (que se intento y por que no
alcanzo):

**Costo en el rebase** (con que parte de upstream va a chocar, y cuanto trabajo
es resolverlo cada vez):

**Alternativa descartada**:
-->

## Diff con upstream

- [ ] El diff con `upstream/main` no crecio, o crecio lo minimo indispensable.
- [ ] Los archivos heredados de AOSP, OpenBoard o HeliBoard que edite conservan
      su encabezado de copyright original (no lo reemplace por el nuestro).
- [ ] Si el cambio es de interes general y no especifico de nuestro producto,
      considere mandarlo **upstream** primero. Resultado: <!-- enviado / no aplica / por que no -->

## Licencia (repo publico, GPL-3.0)

- [ ] No agrego dependencias con licencia incompatible con GPL-3.0 (SDKs
      propietarios de analytics, crash reporting, servicios con clausulas de
      redistribucion restringida).
- [ ] Dependencias nuevas: <!-- nombre, version, licencia. "ninguna" si no hay -->
- [ ] Los archivos nuevos propios llevan el encabezado
      `SPDX-License-Identifier: GPL-3.0-or-later`.
- [ ] No hay secrets, claves, tokens ni URLs internas en el diff. **Este repo es
      publico: lo que se commitea queda en el historial y en los forks aunque se
      borre despues.**

## Privacidad (ADR-009 / ADR-010 / ADR-011)

- [ ] No agrego `AccessibilityService`, lectura de pantalla ni nada que permita
      leer los mensajes entrantes del cliente final. **Nunca.**
- [ ] No persisto contenido de conversaciones, ni resumenes ni interpretaciones
      de lo que dijo el cliente.
- [ ] Si toco datos de contacto o el log de eventos, respeta las categorias de
      ADR-011 y la retencion definida (crudos 90 dias, agregados el plazo fiscal).
- [ ] Permisos nuevos en el manifest: <!-- ninguno / cual y por que -->
- [ ] Las llamadas de IA van **via backend**, nunca directo al proveedor desde el
      cliente. Ninguna clave de proveedor queda en el APK.

## Tokens de diseño

- [ ] No edite a mano ningun archivo generado (`tokens_colors.xml`,
      `tokens_dimens.xml`, `Tokens.kt`).
- [ ] Si cambie tokens, edite `design/tokens.json`, regenere y commitee el
      resultado; el CI de contraste pasa.
- [ ] No hay colores literales (`Color(0x...)`, `#RRGGBB`) en el codigo nuevo.

## Peso

- [ ] Verifique el peso del APK en el CI. Antes: ____ MB. Despues: ____ MB.
- [ ] Si sume recursos, imagenes o diccionarios, esta justificado contra la meta
      (APK < 25 MB; solo teclado 12-15 MB).

## Prueba

- [ ] Probado en dispositivo fisico. Android version(es): <!-- ... -->
- [ ] Probado en **al menos dos apps de mensajeria distintas** (el teclado
      funciona en cualquier app, no solo WhatsApp — ADR-015).
- [ ] Probado en modo claro y oscuro.
- [ ] Probado con el teclado como IME por defecto y con el flujo de activacion
      desde cero, si el cambio lo toca.
- [ ] Tests unitarios nuevos o actualizados, o motivo por el que no aplica.

Capturas o video (si toca UI):

<!-- ... -->
