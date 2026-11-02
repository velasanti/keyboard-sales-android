package com.keyboardsales.assistant.intent

/**
 * Lo que la IA (dummy) decide que corresponde para el texto del vendedor.
 *
 * El vendedor no elige un modo: la IA decide cual de las tres funciones de 04.9
 * §2 se aplica. En esta fase la decision es por palabras clave ([DummyIntentDetector]),
 * sin Groq ni backend real.
 */
enum class AssistantIntentType {
    /** Redactar un mensaje para enviar al cliente (04.9 §2a). */
    REDACT,

    /** Consulta de conocimiento / calculo que no produce nada para enviar (04.9 §2b). */
    CONSULT,

    /** Accion ejecutable que pasa por confirmacion explicita ADR-016 (04.9 §2c). */
    ACTION,

    /** No se reconoce ninguna funcion. */
    NONE,
}

/**
 * Resultado de la clasificacion dummy.
 *
 * [matchedKeyword] es el termino que disparo la decision; se expone para debug y
 * para que el historial de ✨ pueda decir con que se detono, no para el cliente.
 */
data class AssistantIntent(
    val type: AssistantIntentType,
    val matchedKeyword: String?,
)
