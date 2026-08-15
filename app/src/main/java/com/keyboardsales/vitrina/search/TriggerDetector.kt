package com.keyboardsales.vitrina.search

/**
 * Que esta tipeando el vendedor, visto desde el texto que hay antes del cursor.
 *
 * La regla es deliberadamente conservadora: el disparador solo existe si la
 * ULTIMA palabra del texto empieza con '#' (producto) o '/' (respuesta rapida).
 * Asi un "#" o "/" a mitad de palabra ("correo@algo#tag", "sitio.com/ruta") no
 * dispara nada, y un texto sin disparador devuelve [VitrinaTrigger.None].
 *
 * Supuesto abierto (04.8/04.10 no leidos): el termino es una sola palabra.
 * Un espacio despues del termino cierra el disparador. El matching multi-palabra
 * depende de la spec consolidada.
 */
object TriggerDetector {

    fun detect(textBeforeCursor: String): VitrinaTrigger {
        val trimmed = textBeforeCursor.trimEnd()
        if (trimmed.isEmpty()) return VitrinaTrigger.None
        val wordStart = trimmed.indexOfLast { it.isWhitespace() } + 1
        val word = trimmed.substring(wordStart)
        if (word.isEmpty()) return VitrinaTrigger.None
        return when (word[0]) {
            '#' -> VitrinaTrigger.Product(word.substring(1))
            '/' -> VitrinaTrigger.QuickReply(word.substring(1))
            else -> VitrinaTrigger.None
        }
    }
}

sealed interface VitrinaTrigger {
    data class Product(val query: String) : VitrinaTrigger
    data class QuickReply(val query: String) : VitrinaTrigger
    data object None : VitrinaTrigger
}