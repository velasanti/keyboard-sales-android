package com.keyboardsales.vitrina.data

/**
 * Documento del tenant mostrado en Vitrina modo, nivel "Documentos".
 *
 * Por ahora hay UN solo documento dummy y NO se persiste en SQLDelight: el
 * catálogo (productos/respuestas/variantes) sí vive en la base, pero los
 * documentos todavia no tienen archivo real ni backend (04.10, sin número de
 * nota). Cuando exista el contrato de la API se mueve al mismo pipeline que
 * [CatalogItem].
 */
data class DocumentItem(
    val id: String,
    val nombre: String,
    val tipo: String,
    val url: String,
    /** Fecha en formato `yyyy-MM-dd`. El card la convierte a "actualizado hace X". */
    val actualizadoEn: String,
)

object DocumentDummy {

    fun all(): List<DocumentItem> = listOf(
        DocumentItem(
            id = "doc-catalogo-general",
            nombre = "Catálogo general en PDF",
            tipo = "PDF",
            url = "https://catalogo.esculturasdg.invalid/d/catalogo-general.pdf",
            actualizadoEn = "2026-08-14",
        ),
    )
}
