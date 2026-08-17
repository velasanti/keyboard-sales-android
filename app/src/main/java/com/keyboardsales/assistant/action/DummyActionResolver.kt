package com.keyboardsales.assistant.action

import com.keyboardsales.assistant.intent.DummyIntentDetector

/**
 * Resuelve a qué acción ejecutable corresponde el pedido del vendedor (04.9 §2c),
 * ADR-016: toda acción con efecto real pasa por confirmación explícita.
 *
 * En esta fase no hay efecto real que ejecutar (no hay PDF, no hay backend);
 * lo que se construye es la UI de confirmación — la pieza que después nunca se
 * salta. Puro y deterministico; las etiquetas se resuelven en el llamador.
 */
enum class ActionType {
    PDF, DERIVE, ORDER, GENERIC,
}

data class ActionProposal(
    val type: ActionType,
    val detail: String?,
)

object DummyActionResolver {

    fun resolve(input: String): ActionProposal {
        val normalized = DummyIntentDetector.normalize(input)
        return when {
            normalized.contains("pdf") -> ActionProposal(ActionType.PDF, null)
            normalized.contains("deriva") -> ActionProposal(ActionType.DERIVE, null)
            normalized.contains("orden") -> ActionProposal(ActionType.ORDER, null)
            else -> ActionProposal(ActionType.GENERIC, null)
        }
    }
}
