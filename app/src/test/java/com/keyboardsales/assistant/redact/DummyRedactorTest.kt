package com.keyboardsales.assistant.redact

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DummyRedactorTest {

    @Test
    fun `redact returns a non blank message`() {
        assertFalse(DummyRedactor.redact("cotizame la mesa").isBlank())
    }

    @Test
    fun `redact mentions cotizacion for a quote request`() {
        assertTrue(DummyRedactor.redact("cotizame 3 unidades").contains("cotización"))
    }

    @Test
    fun `redact is a sale message addressed to the client`() {
        val message = DummyRedactor.redact("cotizame la granada")
        assertTrue(message.startsWith("Buenas"))
        assertTrue(message.length > 20)
    }
}
