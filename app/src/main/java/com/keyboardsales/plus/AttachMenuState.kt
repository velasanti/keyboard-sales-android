package com.keyboardsales.plus

/**
 * Estados de la Fila 1 (adjuntos), fila FIJA (decisión David 2026-08-22,
 * Log de Decisiones: "＋ / 📷 / 🎤 se separan de AnchorBar"). Sin estados
 * expandibles ni auto-colapso: cuando el campo acepta media, la fila existe
 * siempre — patrón Grammarly (04.8 §2.1).
 *
 *   HIDDEN    campo sin mimes de media (o teclado cerrado): fila GONE
 *   ROOT      [＋ Adjuntar] ......................... [📷] [🎤]
 *   PRINCIPAL [Cámara] [Fotos] [Más ›] .............. [✕]
 *   MAS       [‹] [Documento] [Ubicación] [Contacto]. [✕]
 *
 * El menú (PRINCIPAL/MAS) vive DENTRO del alto fijo de la fila: reemplaza
 * su contenido, nunca suma píxeles verticales.
 */
enum class AttachPanel { HIDDEN, ROOT, PRINCIPAL, MAS }

/**
 * Maquina de estados pura, sin Android: las transiciones ilegales son
 * no-ops y devuelven el mismo estado. El host la observa y renderiza.
 */
data class AttachMenuState(val panel: AttachPanel = AttachPanel.HIDDEN) {

    val isVisible: Boolean get() = panel != AttachPanel.HIDDEN

    /**
     * Foco en un campo nuevo: la fila aparece si el campo acepta media y se
     * reinicia al estado de reposo (ROOT); desaparece si no lo acepta.
     */
    fun onFieldFocused(acceptsMedia: Boolean): AttachMenuState = copy(
        panel = if (acceptsMedia) AttachPanel.ROOT else AttachPanel.HIDDEN,
    )

    /** ＋: abre el menú principal (Cámara/Fotos/Más). */
    fun onPlusTapped(): AttachMenuState =
        if (panel == AttachPanel.ROOT) copy(panel = AttachPanel.PRINCIPAL) else this

    /** Más ›: despliega Documento/Ubicación/Contacto. */
    fun onMoreTapped(): AttachMenuState =
        if (panel == AttachPanel.PRINCIPAL) copy(panel = AttachPanel.MAS) else this

    /** ‹ Volver: de MAS a PRINCIPAL. */
    fun onBackTapped(): AttachMenuState =
        if (panel == AttachPanel.MAS) copy(panel = AttachPanel.PRINCIPAL) else this

    /**
     * ✕ o cierre del menú: vuelve al estado de reposo (ROOT). La fila NO se
     * oculta — es fija mientras el campo acepte media.
     */
    fun closeMenu(): AttachMenuState =
        if (panel == AttachPanel.PRINCIPAL || panel == AttachPanel.MAS) {
            copy(panel = AttachPanel.ROOT)
        } else {
            this
        }

    /** El campo dejo de aceptar media o el teclado cerro: la fila desaparece. */
    fun hide(): AttachMenuState =
        if (panel == AttachPanel.HIDDEN) this else copy(panel = AttachPanel.HIDDEN)

    /**
     * El picker fue cancelado (o falló): vuelve directo al menú principal,
     * que era donde estaba cuando eligió. Nunca abre desde HIDDEN.
     */
    fun onPickCancelled(): AttachMenuState =
        if (panel == AttachPanel.HIDDEN) this else copy(panel = AttachPanel.PRINCIPAL)
}
