package com.keyboardsales.assistant.quote

/**
 * Modelo de cotización (PDF). Puro y deterministico: los cálculos de subtotal,
 * total y cantidad de ítems viven acá para poder testearlos sin Android.
 *
 * Por ahora lo llena [DemoQuote] con el escenario canónico del dummy. Cuando
 * existan contactos (ADR-011) y el LLM real (Fase 2b), esta misma estructura es
 * la que esos datos van a poblar: el pipeline (modelo -> PDF -> share) ya no
 * cambia.
 */
data class QuoteItem(
    val id: String,
    val nombre: String,
    val cantidad: Int,
    val precioUnitario: Long,
    val moneda: String,
) {
    fun subtotal(): Long = cantidad.toLong() * precioUnitario
}

data class Quote(
    val tenant: String,
    val cliente: String?,
    val moneda: String,
    val fecha: String,
    val items: List<QuoteItem>,
) {
    /** Total en la moneda del catálogo (suma de subtotales). */
    fun total(): Long = items.sumOf { it.subtotal() }

    /** Cantidad total de unidades cotizadas (suma de cantidades). */
    fun itemCount(): Int = items.sumOf { it.cantidad }
}
