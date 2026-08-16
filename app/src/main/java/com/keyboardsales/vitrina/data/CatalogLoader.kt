package com.keyboardsales.vitrina.data

import android.content.Context
import com.keyboardsales.vitrina.search.TextNormalizer
import helium314.keyboard.latin.utils.Log
import kotlinx.serialization.json.Json

/**
 * Carga el catalogo DUMMY a SQLDelight al primer arranque. Se corre fuera del
 * hilo de UI (regla: nada bloqueante en el arranque del InputMethodService).
 *
 * El JSON vive en assets/catalog-dummy.json, espejo de design/catalog-dummy.json.
 * Mientras el catalogo sea el dummy, catalog_meta.es_dummy = 1 y el teclado
 * muestra DummyCatalogBadge (la advertencia del propio $meta lo exige).
 */
object CatalogLoader {

    private val json = Json { ignoreUnknownKeys = true }

    fun seedIfEmpty(context: Context, repository: CatalogRepository) {
        if (repository.countItems() > 0) {
            return
        }
        val raw = context.assets.open(ASSET).bufferedReader().use { it.readText() }
        val file = json.decodeFromString<CatalogDummyFile>(raw)
        repository.replaceAll(file)
        Log.d(
            TAG,
            "seed: ${file.items.size} items, ${file.respuestasRapidas.size} respuestas, tenant=${file.tenant}",
        )
    }

    fun searchableText(vararg parts: String): String = TextNormalizer.normalize(parts.joinToString(" "))

    private const val ASSET = "catalog-dummy.json"
    private const val TAG = "Vitrina"
}