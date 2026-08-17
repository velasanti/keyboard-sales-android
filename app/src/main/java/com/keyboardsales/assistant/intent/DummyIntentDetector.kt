package com.keyboardsales.assistant.intent

/**
 * Detector dummy de intencion por palabras clave (Paso 3).
 *
 * Sin Groq ni backend: en esta fase "la IA" son reglas simples sobre el texto
 * del vendedor. Puro y deterministico, corre FUERA del hilo de UI (mismo
 * criterio que la busqueda de Vitrina): el panel nunca se traba esperandolo.
 *
 * Prioridad de decision (la IA decide, el vendedor no elige modo):
 *   1. ACTION  — pdf / deriva / orden       (acciones de 04.9 §2c, ADR-016).
 *                Van primero porque sus verbos son los mas especificos e
 *                inequivocos: "armame el pdf de la cotizacion" es una accion,
 *                no un pedido de redaccion.
 *   2. REDACT  — cotizacion / cotiza        (funcion primaria de 04.9 §2a)
 *   3. CONSULT — cuota / financiamiento     (04.9 §2b)
 *   4. NONE    — ningun disparador
 *
 * Las tildes se normalizan para que "cotizame" y "cotización" matcheen igual.
 */
object DummyIntentDetector {

    private val NORMALIZED_RULES = listOf(
        Rule(AssistantIntentType.ACTION, setOf("pdf", "deriva", "derivalo", "orden")),
        Rule(AssistantIntentType.REDACT, setOf("cotiza", "cotizacion", "cotización")),
        Rule(AssistantIntentType.CONSULT, setOf("cuota", "cuotas", "financia", "financiamiento")),
    )

    fun detect(text: String): AssistantIntent {
        val normalized = normalize(text)
        if (normalized.isBlank()) return AssistantIntent(AssistantIntentType.NONE, null)
        for (rule in NORMALIZED_RULES) {
            val hit = rule.keywords.firstOrNull { normalized.contains(it) }
            if (hit != null) return AssistantIntent(rule.type, hit)
        }
        return AssistantIntent(AssistantIntentType.NONE, null)
    }

    /** Minusculas y sin tildes. "Cotización" y "cotizame" caen a la misma base. */
    fun normalize(text: String): String {
        val lower = text.lowercase()
        return buildString {
            for (ch in lower) {
                when (ch) {
                    'á' -> append('a'); 'é' -> append('e'); 'í' -> append('i')
                    'ó' -> append('o'); 'ú' -> append('u'); 'ü' -> append('u')
                    else -> append(ch)
                }
            }
        }
    }

    private data class Rule(val type: AssistantIntentType, val keywords: Set<String>)
}
