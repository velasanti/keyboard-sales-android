package com.keyboardsales.vitrina.search

import com.keyboardsales.vitrina.data.CatalogItem
import com.keyboardsales.vitrina.data.QuickReply

/**
 * Matching de productos y respuestas rapidas sobre el texto ya normalizado.
 *
 * Precondiciones:
 *  - items: [CatalogItem.busqueda] ya contiene nombre + categoria + alias
 *    normalizados (lo arma CatalogRepository al sembrar).
 *  - La consulta entra en crudo y se normaliza aca; los dos lados se comparan
 *    sin tildes y en minusculas.
 *
 * Reglas de puntaje (productos):
 *  - prefijo del nombre: 100
 *  - prefijo de la busqueda completa: 60
 *  - prefijo de un token (cualquier palabra, incluida la categoria o alias): 40
 *  - prefijo de un token del nombre: 30
 *  - substring en la busqueda completa: 20
 *  - score 0 = sin coincidencia, no entra.
 *
 * Desempate dentro de cada nivel (04.10 §3.4): recencia×frecuencia del
 * vendedor → popularidad en el tenant → alfabetico. En el prototipo con datos
 * dummy no hay historial (ADR-011 fuera de V1) ni popularidad en el tenant,
 * asi que el desempate cae DIRECTO a alfabetico sobre [CatalogItem.busqueda]
 * (que empieza por el nombre normalizado) y, para el mismo nombre, por id.
 */
object CatalogMatcher {

    fun matchItems(items: List<CatalogItem>, query: String): List<CatalogItem> {
        val q = TextNormalizer.normalize(query)
        if (q.isBlank()) return emptyList()
        return items
            .map { it to score(it, q) }
            .filter { it.second > 0 }
            .sortedWith(compareByDescending<Pair<CatalogItem, Int>> { it.second }
                .thenBy { it.first.busqueda }
                .thenBy { it.first.id })
            .map { it.first }
    }

    fun matchQuickReplies(replies: List<QuickReply>, query: String): List<QuickReply> {
        val q = TextNormalizer.normalize(query)
        if (q.isBlank()) return emptyList()
        return replies
            .filter { reply ->
                val atajo = TextNormalizer.normalize(reply.atajo)
                atajo.startsWith(q) || atajo.contains(q)
            }
            .sortedWith(compareByDescending<QuickReply> { TextNormalizer.normalize(it.atajo).startsWith(q) }
                .thenBy { TextNormalizer.normalize(it.atajo) }
                .thenBy { it.id })
    }

    private fun score(item: CatalogItem, q: String): Int {
        val nombre = TextNormalizer.normalize(item.nombre)
        val searchable = item.busqueda
        var score = 0

        if (nombre.startsWith(q)) score += 100
        if (searchable.startsWith(q)) score += 60
        if (searchable.split(' ').any { it.startsWith(q) }) score += 40
        if (nombre.split(' ').any { it.startsWith(q) }) score += 30
        if (searchable.contains(q)) score += 20

        return score
    }
}