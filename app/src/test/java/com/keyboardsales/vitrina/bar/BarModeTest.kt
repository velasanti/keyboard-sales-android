package com.keyboardsales.vitrina.bar

import org.junit.Assert.assertEquals
import org.junit.Test

class BarModeTest {

    @Test
    fun `en el umbral exacto la barra suma fila`() {
        assertEquals(BarLayout.EXPANDED_ROW, BarMode.decide(usableHeightPx = 640, expandedThresholdPx = 640))
    }

    @Test
    fun `un pixel debajo del umbral reemplaza la fila`() {
        assertEquals(BarLayout.OVERLAY_SUGGESTIONS, BarMode.decide(usableHeightPx = 639, expandedThresholdPx = 640))
    }

    @Test
    fun `dispositivo tipico latam expande`() {
        assertEquals(BarLayout.EXPANDED_ROW, BarMode.decide(usableHeightPx = 800, expandedThresholdPx = 640))
    }

    @Test
    fun `gama baja reemplaza la fila sin crecer`() {
        assertEquals(BarLayout.OVERLAY_SUGGESTIONS, BarMode.decide(usableHeightPx = 480, expandedThresholdPx = 640))
    }

    @Test
    fun `umbral en cero siempre expande`() {
        assertEquals(BarLayout.EXPANDED_ROW, BarMode.decide(usableHeightPx = 0, expandedThresholdPx = 0))
    }
}