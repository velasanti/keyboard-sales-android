package com.keyboardsales.vitrina.data

import android.content.Context
import app.cash.sqldelight.driver.android.AndroidSqliteDriver
import com.keyboardsales.vitrina.search.TextNormalizer

/** Item del catalogo listo para consumir por las superficies. */
data class CatalogItem(
    val id: String,
    val nombre: String,
    val categoria: String,
    val precio: Long,
    val moneda: String,
    val url: String,
    val imagen: String?,
    val actualizadoEn: String,
    val busqueda: String,
)

/** Respuesta rapida lista para consumir por las superficies. */
data class QuickReply(
    val id: String,
    val atajo: String,
    val texto: String,
)

/** Variante de mensaje de un producto (04.10 flujo B, pantalla B5). */
data class MessageVariant(
    val id: String,
    val itemId: String,
    val tipo: String,
    val texto: String,
)

/**
 * Unico acceso a la base de Vitrina (SQLDelight). Es propiedad del proceso del
 * IME: lo crea SalesIME y lo comparten las superficies via VitrinaHost.
 */
class CatalogRepository(context: Context) {

    private val driver = AndroidSqliteDriver(
        schema = CatalogDatabase.Schema,
        context = context,
        name = DB_NAME,
    )
    private val queries = CatalogDatabase(driver).catalogQueries

    fun countItems(): Long = queries.countItems().executeAsOne()

    fun countQuickReplies(): Long = queries.countQuickReplies().executeAsOne()

    fun allItems(): List<CatalogItem> = queries.selectAllItems().executeAsList().map { it.toModel() }

    fun allQuickReplies(): List<QuickReply> = queries.selectAllQuickReplies().executeAsList().map { it.toModel() }

    fun allMessageVariants(): List<MessageVariant> = queries.selectAllMessageVariants().executeAsList().map { it.toModel() }

    fun isDummy(): Boolean = queries.selectMeta().executeAsOne().es_dummy != 0L

    /** Reemplaza el contenido completo (seed del primer arranque). */
    fun replaceAll(file: CatalogDummyFile) {
        queries.deleteAll()
        queries.insertMeta(
            version = file.version.toLong(),
            tenant = file.tenant,
            monedaDefault = file.monedaDefault,
            esDummy = 1L,
        )
        for (item in file.items) {
            val searchable = TextNormalizer.normalize(
                listOf(item.nombre, item.categoria, *item.alias.toTypedArray()).joinToString(" "),
            )
            queries.insertItem(
                id = item.id,
                nombre = item.nombre,
                categoria = item.categoria,
                precio = item.precio,
                moneda = item.moneda,
                url = item.url,
                imagen = item.imagen,
                actualizadoEn = item.actualizadoEn,
                busqueda = searchable,
            )
            for ((indice, variant) in item.variantes.withIndex()) {
                queries.insertMessageVariant(
                    id = "${item.id}::$indice",
                    itemId = item.id,
                    tipo = variant.tipo,
                    texto = variant.texto,
                    orden = indice.toLong(),
                )
            }
        }
        for (reply in file.respuestasRapidas) {
            queries.insertQuickReply(
                id = reply.id,
                atajo = reply.atajo,
                texto = reply.texto,
                busqueda = TextNormalizer.normalize(reply.atajo),
            )
        }
    }

    companion object {
        private const val DB_NAME = "vitrina.db"
    }
}

private fun Catalog_item.toModel() = CatalogItem(
    id = id,
    nombre = nombre,
    categoria = categoria,
    precio = precio,
    moneda = moneda,
    url = url,
    imagen = imagen,
    actualizadoEn = actualizado_en,
    busqueda = busqueda,
)

private fun Quick_reply.toModel() = QuickReply(
    id = id,
    atajo = atajo,
    texto = texto,
)

private fun Message_variant.toModel() = MessageVariant(
    id = id,
    itemId = item_id,
    tipo = tipo,
    texto = texto,
)