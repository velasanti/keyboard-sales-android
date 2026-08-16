package com.keyboardsales.vitrina.search

/**
 * Normaliza texto para el matching de Vitrina. El vendedor escribe sin tildes
 * y en minuscula: aca se normalizan LOS DOS lados (lo que se indexa en
 * `busqueda` y lo que tipea el vendedor) con la misma funcion.
 *
 * Quita diacriticos por descomposicion NFD (tildes, dieresis) y conserva la ñ,
 * que es una letra del espanol y no tiene forma combinable. Espacios colapsados.
 */
object TextNormalizer {

    fun normalize(text: String): String {
        val decomposed = java.text.Normalizer.normalize(text, java.text.Normalizer.Form.NFD)
        val sb = StringBuilder(decomposed.length)
        for (c in decomposed) {
            if (Character.getType(c) != Character.NON_SPACING_MARK.toInt()) {
                sb.append(c)
            }
        }
        return collapseSpaces(sb.toString().lowercase())
    }

    private fun collapseSpaces(text: String): String {
        val sb = StringBuilder(text.length)
        var inSpace = false
        for (c in text) {
            if (c == ' ') {
                if (!inSpace) {
                    sb.append(' ')
                    inSpace = true
                }
            } else {
                sb.append(c)
                inSpace = false
            }
        }
        return sb.toString().trim()
    }
}