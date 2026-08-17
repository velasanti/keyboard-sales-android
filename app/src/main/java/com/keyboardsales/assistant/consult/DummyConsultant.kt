package com.keyboardsales.assistant.consult

import com.keyboardsales.assistant.intent.DummyIntentDetector

/**
 * Respuesta dummy de consulta de conocimiento (04.9 §2b).
 *
 * Simula un cálculo con número fijo (cuotas/financiamiento) y devuelve una
 * respuesta que SE MUESTRA en el historial de ✨, nunca se inserta al chat.
 * STUB: sin catálogo ni backend real; el conocimiento de empresa entra con el
 * LLM real (Fase 2b).
 *
 * Puro y deterministico; los mensajes se resuelven en el llamador (recursos),
 * no acá, para mantenerlo testeable sin Android.
 */
object DummyConsultant {

    fun answer(input: String, cuotasMessage: String, fallbackMessage: String): String {
        val normalized = DummyIntentDetector.normalize(input)
        return if (normalized.contains("cuota") || normalized.contains("financia")) {
            cuotasMessage
        } else {
            fallbackMessage
        }
    }
}
