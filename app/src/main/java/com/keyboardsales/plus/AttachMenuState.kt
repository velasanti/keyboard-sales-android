package com.keyboardsales.plus

/**
 * Estados de la Franja 1 (adjuntos). Colapsada es el estado canonico:
 * GONE, cero costo sobre el presupuesto vertical (medicion del spike:
 * fija no entra en gama baja, 53.5% del caso critico).
 *
 *   HIDDEN    (colapsada, GONE)
 *   ROOT      [＋ Adjuntar] .......................... [✕]
 *   PRINCIPAL [Cámara] [Fotos] [Más ›] .............. [✕]
 *   MAS       [‹] [Documento] [Ubicación] [Contacto]. [✕]
 */
enum class AttachPanel { HIDDEN, ROOT, PRINCIPAL, MAS }

/**
 * Maquina de estados pura, sin Android: las transiciones ilegales son
 * no-ops y devuelven el mismo estado. El host la observa y renderiza.
 */
data class AttachMenuState(val panel: AttachPanel = AttachPanel.HIDDEN) {

    val isVisible: Boolean get() = panel != AttachPanel.HIDDEN

    /**
     * Foco en un campo nuevo. La franja solo se expande sola si el campo
     * acepta media; si ya estaba abierta se reinicia a ROOT (campo nuevo,
     * menu desde cero).
     */
    fun onFieldFocused(acceptsMedia: Boolean): AttachMenuState = copy(
        panel = if (acceptsMedia) AttachPanel.ROOT else AttachPanel.HIDDEN,
    )

    /** ＋: abre el menu principal (Cámara/Fotos/Más). */
    fun onPlusTapped(): AttachMenuState =
        if (panel == AttachPanel.ROOT) copy(panel = AttachPanel.PRINCIPAL) else this

    /** Más ›: despliega Documento/Ubicación/Contacto. */
    fun onMoreTapped(): AttachMenuState =
        if (panel == AttachPanel.PRINCIPAL) copy(panel = AttachPanel.MAS) else this

    /** ‹ Volver: de MAS a PRINCIPAL. */
    fun onBackTapped(): AttachMenuState =
        if (panel == AttachPanel.MAS) copy(panel = AttachPanel.PRINCIPAL) else this

    /**
     * El vendedor cancelo el picker (o fallo): vuelve directo al menu
     * principal, que era donde estaba cuando eligio. Nunca reabre ROOT.
     */
    fun onPickCancelled(): AttachMenuState =
        if (panel == AttachPanel.HIDDEN) this else copy(panel = AttachPanel.PRINCIPAL)

    /** ✕ o cualquier señal de colapso (tecla, timeout, foco perdido). */
    fun collapse(): AttachMenuState =
        if (panel == AttachPanel.HIDDEN) this else copy(panel = AttachPanel.HIDDEN)
}
