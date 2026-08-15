package com.keyboardsales.vitrina.bar

import android.content.Context
import android.graphics.drawable.GradientDrawable
import android.util.TypedValue
import android.view.Gravity
import android.widget.TextView
import androidx.core.content.ContextCompat
import helium314.keyboard.latin.R

/**
 * Aviso permanente mientras el catalogo cargado sea el DUMMY (la advertencia del
 * propio $meta de catalog-dummy.json lo exige: un dummy instalado como IME real
 * puede meter un precio inventado en un chat de trabajo).
 *
 * Es un pill solapado en la esquina superior del strip, sin consumir altura del
 * presupuesto vertical. La anatomia exacta del aviso se ajusta a la ficha de
 * 04.3a cuando exista; los tokens ya son los definitivos.
 */
class DummyCatalogBadgeView(context: Context) : TextView(context) {

    init {
        text = context.getString(R.string.vitrina_badge_dummy)
        contentDescription = text
        val displayContext = context
        setTextSize(
            TypedValue.COMPLEX_UNIT_PX,
            displayContext.resources.getDimension(R.dimen.type_supporting_label_size),
        )
        setTextColor(ContextCompat.getColor(displayContext, R.color.feedback_warning_on_subtle))
        val radiusPx = displayContext.resources.getDimensionPixelSize(R.dimen.radius_pill)
        val bg = GradientDrawable().apply {
            cornerRadius = radiusPx.toFloat()
            setColor(ContextCompat.getColor(displayContext, R.color.feedback_warning_subtle))
        }
        background = bg
        gravity = Gravity.CENTER_VERTICAL
        setPadding(
            displayContext.resources.getDimensionPixelSize(R.dimen.spacing_2),
            0,
            displayContext.resources.getDimensionPixelSize(R.dimen.spacing_2),
            0,
        )
    }
}