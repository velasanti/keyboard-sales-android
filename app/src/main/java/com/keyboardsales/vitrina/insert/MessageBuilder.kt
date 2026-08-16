package com.keyboardsales.vitrina.insert

import com.keyboardsales.vitrina.data.CatalogItem
import com.keyboardsales.vitrina.data.QuickReply

/**
 * Construccion del mensaje que llega al chat (06.5 Inserción).
 *
 * Reglas:
 *  - Producto: texto plano con UNA sola URL (ADR-017). No se inserta ninguna
 *    tarjeta; la app receptora renderiza desde los Open Graph tags.
 *  - Respuesta rapida: el texto tal cual, sin URL.
 */
object MessageBuilder {

    fun productMessage(item: CatalogItem): String = buildString {
        append(item.nombre)
        append(" — ")
        append(PriceFormatter.format(item.precio, item.moneda))
        append('\n')
        append(item.url)
    }

    fun quickReplyMessage(reply: QuickReply): String = reply.texto
}