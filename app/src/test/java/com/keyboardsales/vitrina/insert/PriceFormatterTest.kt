package com.keyboardsales.vitrina.insert

import org.junit.Assert.assertEquals
import org.junit.Test

class PriceFormatterTest {

    @Test
    fun `miles con punto y sufijo de moneda`() {
        assertEquals("$1.850.000 COP", PriceFormatter.format(1_850_000L, "COP"))
        assertEquals("$295.000 COP", PriceFormatter.format(295_000L, "COP"))
        assertEquals("$1.000 COP", PriceFormatter.format(1_000L, "COP"))
    }

    @Test
    fun `sin separadores para miles pequeños`() {
        assertEquals("$0 COP", PriceFormatter.format(0L, "COP"))
        assertEquals("$42 USD", PriceFormatter.format(42L, "USD"))
    }

    @Test
    fun `miles grandes agrupados de a tres`() {
        assertEquals("$123.456.789 COP", PriceFormatter.format(123_456_789L, "COP"))
        assertEquals("$10.000.000 COP", PriceFormatter.format(10_000_000L, "COP"))
    }
}