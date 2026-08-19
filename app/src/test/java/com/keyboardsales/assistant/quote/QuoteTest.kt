package com.keyboardsales.assistant.quote

import org.junit.Assert.assertEquals
import org.junit.Test

class QuoteTest {

    private val items = listOf(
        QuoteItem("a", "Mesa", 2, 1_000L, "COP"),
        QuoteItem("b", "Silla", 4, 250L, "COP"),
    )

    @Test
    fun `subtotal es cantidad por precio unitario`() {
        assertEquals(2_000L, items[0].subtotal())
        assertEquals(1_000L, items[1].subtotal())
    }

    @Test
    fun `total suma los subtotales`() {
        val quote = Quote("t", "c", "COP", "19/08/2026", items)
        assertEquals(3_000L, quote.total())
    }

    @Test
    fun `item count suma las cantidades`() {
        val quote = Quote("t", "c", "COP", "19/08/2026", items)
        assertEquals(6, quote.itemCount())
    }

    @Test
    fun `cotizacion sin items totaliza cero`() {
        val quote = Quote("t", null, "COP", "19/08/2026", emptyList())
        assertEquals(0L, quote.total())
        assertEquals(0, quote.itemCount())
    }
}
