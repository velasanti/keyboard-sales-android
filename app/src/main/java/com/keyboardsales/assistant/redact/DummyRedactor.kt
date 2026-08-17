package com.keyboardsales.assistant.redact

/**
 * Redaccion dummy de mensaje para enviar al cliente (04.9 §2a).
 *
 * STUB deliberado: sin catalogo ni playbook aun, devuelve un mensaje de venta
 * generico. El redactado real (catalogo + conocimiento de empresa + tono) entra
 * con el LLM real (Fase 2b). Lo que este paso valida es el FLUJO: redactar ->
 * confirmacion ADR-016 -> insertar por InputConnection -> deshacer.
 */
object DummyRedactor {

    fun redact(input: String): String =
        "Buenas, por tu consulta te armo la cotización con las condiciones que pediste. " +
            "Quedo a tu disposición para ajustar lo que necesites."
}
