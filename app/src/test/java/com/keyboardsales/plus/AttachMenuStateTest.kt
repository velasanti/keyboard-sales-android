package com.keyboardsales.plus

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Semantica de fila FIJA (decision David 2026-08-22): sin auto-colapso, sin
 * timeout, sin rendimiento al tipeo. La fila existe mientras el campo acepte
 * media; el menu vive dentro de su alto fijo.
 */
class AttachMenuStateTest {

    private val oculta = AttachMenuState()

    // --------------------------------------------------------------
    // onFieldFocused
    // --------------------------------------------------------------

    @Test
    fun `arranca oculta`() {
        assertFalse(oculta.isVisible)
        assertEquals(AttachPanel.HIDDEN, oculta.panel)
    }

    @Test
    fun `campo con media abre en ROOT`() {
        val estado = oculta.onFieldFocused(acceptsMedia = true)
        assertTrue(estado.isVisible)
        assertEquals(AttachPanel.ROOT, estado.panel)
    }

    @Test
    fun `campo sin media sigue oculta`() {
        val estado = oculta.onFieldFocused(acceptsMedia = false)
        assertEquals(AttachPanel.HIDDEN, estado.panel)
    }

    @Test
    fun `foco nuevo reinicia un menu abierto a ROOT`() {
        val abierto = oculta.onFieldFocused(true).onPlusTapped().onMoreTapped()
        assertEquals(AttachPanel.MAS, abierto.panel)

        val reiniciado = abierto.onFieldFocused(acceptsMedia = true)
        assertEquals(AttachPanel.ROOT, reiniciado.panel)
    }

    // --------------------------------------------------------------
    // navegacion del menu
    // --------------------------------------------------------------

    @Test
    fun `plus abre el menu principal`() {
        val estado = oculta.onFieldFocused(true).onPlusTapped()
        assertEquals(AttachPanel.PRINCIPAL, estado.panel)
    }

    @Test
    fun `mas despliega Documento-Ubicacion-Contacto`() {
        val estado = oculta.onFieldFocused(true).onPlusTapped().onMoreTapped()
        assertEquals(AttachPanel.MAS, estado.panel)
    }

    @Test
    fun `volver desde MAS regresa a PRINCIPAL`() {
        val estado = oculta
            .onFieldFocused(true)
            .onPlusTapped()
            .onMoreTapped()
            .onBackTapped()
        assertEquals(AttachPanel.PRINCIPAL, estado.panel)
    }

    // --------------------------------------------------------------
    // cierre del menu: vuelve a ROOT, la fila permanece (fila fija)
    // --------------------------------------------------------------

    @Test
    fun `cerrar menu desde PRINCIPAL vuelve al reposo`() {
        val estado = oculta.onFieldFocused(true).onPlusTapped().closeMenu()
        assertEquals(AttachPanel.ROOT, estado.panel)
        assertTrue(estado.isVisible)
    }

    @Test
    fun `cerrar menu desde MAS vuelve al reposo`() {
        val estado = oculta.onFieldFocused(true).onPlusTapped().onMoreTapped().closeMenu()
        assertEquals(AttachPanel.ROOT, estado.panel)
        assertTrue(estado.isVisible)
    }

    @Test
    fun `closeMenu sobre ROOT u HIDDEN no hace nada`() {
        assertEquals(
            AttachMenuState(AttachPanel.ROOT),
            AttachMenuState(AttachPanel.ROOT).closeMenu(),
        )
        assertEquals(oculta, oculta.closeMenu())
    }

    // --------------------------------------------------------------
    // transiciones ilegales: no-ops
    // --------------------------------------------------------------

    @Test
    fun `plus fuera de ROOT no hace nada`() {
        assertEquals(
            AttachMenuState(AttachPanel.PRINCIPAL),
            AttachMenuState(AttachPanel.PRINCIPAL).onPlusTapped(),
        )
        assertEquals(
            AttachMenuState(AttachPanel.MAS),
            AttachMenuState(AttachPanel.MAS).onPlusTapped(),
        )
    }

    @Test
    fun `mas fuera de PRINCIPAL no hace nada`() {
        assertEquals(
            AttachMenuState(AttachPanel.ROOT),
            AttachMenuState(AttachPanel.ROOT).onMoreTapped(),
        )
    }

    @Test
    fun `volver fuera de MAS no hace nada`() {
        assertEquals(
            AttachMenuState(AttachPanel.PRINCIPAL),
            AttachMenuState(AttachPanel.PRINCIPAL).onBackTapped(),
        )
    }

    // --------------------------------------------------------------
    // picker cancelado
    // --------------------------------------------------------------

    @Test
    fun `picker cancelado desde PRINCIPAL vuelve al menu`() {
        val estado = oculta.onFieldFocused(true).onPlusTapped().onPickCancelled()
        assertEquals(AttachPanel.PRINCIPAL, estado.panel)
    }

    @Test
    fun `picker cancelado desde MAS tambien cae en principal`() {
        val estado = oculta.onFieldFocused(true).onPlusTapped().onMoreTapped().onPickCancelled()
        assertEquals(AttachPanel.PRINCIPAL, estado.panel)
    }

    @Test
    fun `picker cancelado sobre oculta no abre nada`() {
        assertEquals(oculta, oculta.onPickCancelled())
    }
}
