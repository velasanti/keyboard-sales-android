package com.keyboardsales.vitrina

import org.junit.Assert.assertEquals
import org.junit.Test

class PanelToggleTest {

    @Test
    fun `tap con el panel cerrado abre el modo`() {
        assertEquals(PanelAction.SHOW, PanelToggle.next(panelVisible = false))
    }

    @Test
    fun `tap con el panel abierto lo cierra`() {
        assertEquals(PanelAction.HIDE, PanelToggle.next(panelVisible = true))
    }
}