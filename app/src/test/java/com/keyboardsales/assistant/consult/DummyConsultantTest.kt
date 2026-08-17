package com.keyboardsales.assistant.consult

import org.junit.Assert.assertEquals
import org.junit.Test

class DummyConsultantTest {

    private val cuotas = "12 cuotas"
    private val fallback = "genérico"

    @Test
    fun `cuotas returns cuotas message`() {
        assertEquals(cuotas, DummyConsultant.answer("a cuantas cuotas lo vendo", cuotas, fallback))
    }

    @Test
    fun `financiamiento returns cuotas message`() {
        assertEquals(cuotas, DummyConsultant.answer("que financiamiento tiene la mesa", cuotas, fallback))
    }

    @Test
    fun `no consult keyword returns fallback`() {
        assertEquals(fallback, DummyConsultant.answer("hola buenos dias", cuotas, fallback))
    }

    @Test
    fun `cuotas without accent is detected`() {
        assertEquals(cuotas, DummyConsultant.answer("a cuantas cuotas", cuotas, fallback))
    }
}
