package com.keyboardsales.vitrina.bar

/**
 * Rama de altura de la barra cuando Vitrina capa esta activa.
 *
 * Regla de design/tokens.json (kb/bar/height-expanded, nota "unmeasured"):
 * la barra SOLO suma fila si el alto util es >= kb/bar/expanded-min-height
 * (640dp). A 640dp el total queda en 45.0%, clavado en el limite del presupuesto
 * vertical de CLAUDE.md. Debajo de eso los chips reemplazan la fila de
 * sugerencias sin crecer un pixel.
 *
 * Ambos valores entran en pixeles y la decision es pura.
 */
enum class BarLayout {
    /** Alto util >= umbral: la barra crece a kb/bar/height-expanded. */
    EXPANDED_ROW,

    /** Alto util < umbral: los chips reemplazan la fila, sin crecer. */
    OVERLAY_SUGGESTIONS,
}

object BarMode {

    fun decide(usableHeightPx: Int, expandedThresholdPx: Int): BarLayout =
        if (usableHeightPx >= expandedThresholdPx) {
            BarLayout.EXPANDED_ROW
        } else {
            BarLayout.OVERLAY_SUGGESTIONS
        }
}