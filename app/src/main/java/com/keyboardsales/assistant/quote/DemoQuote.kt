package com.keyboardsales.assistant.quote

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Cotización PLACEHOLDER mientras no existen contactos (ADR-011) ni LLM real
 * (Fase 2b). Usa el escenario canónico del dummy de Vitrina: María (Esculturas
 * DG) le cotiza a Juan Ramírez, con precios reales de `catalog-dummy.json`.
 *
 * Nada de esto pretende ser datos del cliente; es el contenido que demuestra el
 * pipeline real de generación + share. El día que existan las fuentes reales,
 * este objeto se reemplaza, no el pipeline.
 */
object DemoQuote {

    fun build(): Quote = Quote(
        tenant = "esculturas-dg",
        cliente = "Juan Ramírez",
        moneda = "COP",
        fecha = SimpleDateFormat("dd/MM/yyyy", Locale.US).format(Date()),
        items = listOf(
            QuoteItem("edg-mesa-granada-nogal-180", "Mesa Granada Nogal 180", 2, 1_850_000L, "COP"),
            QuoteItem("edg-silla-tunja-cuero", "Silla Tunja Cuero", 4, 420_000L, "COP"),
            QuoteItem("edg-repisa-monserrate", "Repisa Monserrate", 1, 295_000L, "COP"),
        ),
    )
}
