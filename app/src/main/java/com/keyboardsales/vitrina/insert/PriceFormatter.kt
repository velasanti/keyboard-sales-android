package com.keyboardsales.vitrina.insert

/**
 * Precio a texto plano para el mensaje. Miles con punto (convencion es-CO,
 * como el propio dummy: "$1.000.000"), sin decimales, sufijo de moneda.
 */
object PriceFormatter {

    /** Agrupa miles con punto y antepone "$", sin sufijo de moneda. */
    fun formatNumber(amount: Long): String {
        val digits = amount.toString()
        val grouped = StringBuilder(digits.length + digits.length / 3)
        for (i in digits.indices) {
            if (i > 0 && (digits.length - i) % 3 == 0) grouped.append('.')
            grouped.append(digits[i])
        }
        return "$$grouped"
    }

    fun format(amount: Long, moneda: String): String = "${formatNumber(amount)} $moneda"
}