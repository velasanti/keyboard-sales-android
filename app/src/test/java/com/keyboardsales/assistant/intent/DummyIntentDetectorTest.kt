package com.keyboardsales.assistant.intent

import org.junit.Assert.assertEquals
import org.junit.Test

class DummyIntentDetectorTest {

    @Test
    fun `blank text is none`() {
        assertEquals(AssistantIntentType.NONE, DummyIntentDetector.detect("   ").type)
        assertEquals(AssistantIntentType.NONE, DummyIntentDetector.detect("").type)
    }

    @Test
    fun `cotizame is redact`() {
        val intent = DummyIntentDetector.detect("cotizame 3 unidades de la granada")
        assertEquals(AssistantIntentType.REDACT, intent.type)
        assertEquals("cotiza", intent.matchedKeyword)
    }

    @Test
    fun `cotizacion with accent is redact`() {
        assertEquals(AssistantIntentType.REDACT, DummyIntentDetector.detect("necesito una cotización del modelo").type)
    }

    @Test
    fun `cuotas is consult`() {
        assertEquals(AssistantIntentType.CONSULT, DummyIntentDetector.detect("a cuantas cuotas lo puedo vender").type)
    }

    @Test
    fun `financiamiento is consult`() {
        assertEquals(AssistantIntentType.CONSULT, DummyIntentDetector.detect("que financiamiento tiene la mesa").type)
    }

    @Test
    fun `pdf is action`() {
        assertEquals(AssistantIntentType.ACTION, DummyIntentDetector.detect("armame el pdf de la cotizacion").type)
    }

    @Test
    fun `derivalo is action`() {
        assertEquals(AssistantIntentType.ACTION, DummyIntentDetector.detect("derivalo a pedro tema financiamiento").type)
    }

    @Test
    fun `no keyword is none`() {
        assertEquals(AssistantIntentType.NONE, DummyIntentDetector.detect("hola buenos dias").type)
    }

    @Test
    fun `redact wins over consult when both present`() {
        // La prioridad decide: cotiza (REDACT) manda sobre cuotas (CONSULT).
        val intent = DummyIntentDetector.detect("cotizame la mesa a cuotas")
        assertEquals(AssistantIntentType.REDACT, intent.type)
    }

    @Test
    fun `case insensitive`() {
        assertEquals(AssistantIntentType.CONSULT, DummyIntentDetector.detect("CUOTAS?").type)
    }
}
