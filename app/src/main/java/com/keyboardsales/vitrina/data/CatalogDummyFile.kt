package com.keyboardsales.vitrina.data

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Estructura del archivo de catalogo que llega en assets. Por ahora solo el
 * dummy de design/catalog-dummy.json; cuando exista backend, este DTO cambia
 * por el contrato de la API y el loader por la sincronizacion (fuera de V1).
 */
@Serializable
data class CatalogDummyFile(
    @SerialName("\$meta") val meta: CatalogDummyMeta,
    val version: Int,
    @SerialName("generado_en") val generadoEn: String,
    val tenant: String,
    @SerialName("moneda_default") val monedaDefault: String,
    val items: List<CatalogDummyItem>,
    @SerialName("respuestas_rapidas") val respuestasRapidas: List<CatalogDummyQuickReply>,
)

@Serializable
data class CatalogDummyMeta(
    val proposito: String,
    val spec: String,
    val escenario: String,
    val advertencia: String,
    @SerialName("elegido_para_romper") val elegidoParaRomper: List<String>,
)

@Serializable
data class CatalogDummyItem(
    val id: String,
    val nombre: String,
    val categoria: String,
    val precio: Long,
    val moneda: String,
    val alias: List<String> = emptyList(),
    val url: String,
    val imagen: String? = null,
    @SerialName("actualizado_en") val actualizadoEn: String,
)

@Serializable
data class CatalogDummyQuickReply(
    val id: String,
    val atajo: String,
    val texto: String,
)