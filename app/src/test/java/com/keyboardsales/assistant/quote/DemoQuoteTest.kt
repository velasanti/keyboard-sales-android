package com.keyboardsales.assistant.quote

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Fija el contenido del preview ADR-016 que se muestra antes de generar el PDF.
 * La tarjeta de confirmación dice "Cotización en PDF · N ítems · Total $X", y
 * este test clava N y $X contra el placeholder para que un cambio en DemoQuote
 * no pase desapercibido en el preview.
 */
class DemoQuoteTest {

    private val quote = DemoQuote.build()

    @Test
    fun `preview muestra 7 items`() {
        assertEquals(7, quote.itemCount())
    }

    @Test
    fun `preview total es 5_675_000 COP`() {
        assertEquals(5_675_000L, quote.total())
        assertEquals("COP", quote.moneda)
    }

    @Test
    fun `cliente y tenant del escenario canonico`() {
        assertEquals("Juan Ramírez", quote.cliente)
        assertEquals("esculturas-dg", quote.tenant)
    }

    @Test
    fun `tres lineas con cantidades y precios del dummy`() {
        assertEquals(3, quote.items.size)
        assertEquals("Mesa Granada Nogal 180", quote.items[0].nombre)
        assertEquals(2, quote.items[0].cantidad)
        assertEquals(1_850_000L, quote.items[0].precioUnitario)
        assertEquals(4, quote.items[1].cantidad)
        assertEquals(420_000L, quote.items[1].precioUnitario)
        assertEquals(1, quote.items[2].cantidad)
        assertEquals(295_000L, quote.items[2].precioUnitario)
    }
}
