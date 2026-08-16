package com.keyboardsales.vitrina.insert

import com.keyboardsales.vitrina.data.CatalogItem
import com.keyboardsales.vitrina.data.MessageVariant
import com.keyboardsales.vitrina.data.QuickReply

/**
 * Construccion del mensaje que llega al chat (06.5 Inserción).
 *
 * Reglas:
 *  - Producto: texto plano con UNA sola URL (ADR-017). No se inserta ninguna
 *    tarjeta; la app receptora renderiza desde los Open Graph tags.
 *  - Variante de producto: el texto de la variante tal cual + la URL del
 *    producto en su propia linea (ADR-017). El texto ya viene resuelto.
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

    fun variantMessage(item: CatalogItem, variant: MessageVariant): String = buildString {
        append(variant.texto)
        append('\n')
        append(item.url)
    }

    fun quickReplyMessage(reply: QuickReply): String = reply.texto
}