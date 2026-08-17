package com.keyboardsales.assistant.action

import org.junit.Assert.assertEquals
import org.junit.Test

class DummyActionResolverTest {

    @Test
    fun `pdf is action PDF`() {
        assertEquals(ActionType.PDF, DummyActionResolver.resolve("armame el pdf de la cotizacion").type)
    }

    @Test
    fun `derivalo is action DERIVE`() {
        assertEquals(ActionType.DERIVE, DummyActionResolver.resolve("derivalo a pedro").type)
    }

    @Test
    fun `orden is action ORDER`() {
        assertEquals(ActionType.ORDER, DummyActionResolver.resolve("registra la orden de venta").type)
    }

    @Test
    fun `no action keyword is GENERIC`() {
        assertEquals(ActionType.GENERIC, DummyActionResolver.resolve("hola buenos dias").type)
    }
}
