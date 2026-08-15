package com.keyboardsales.vitrina.panel

import android.content.Context
import android.graphics.drawable.GradientDrawable
import android.view.Gravity
import android.widget.TextView
import androidx.core.content.ContextCompat
import helium314.keyboard.latin.R

/**
 * El ancla ☰ que alterna Vitrina modo. Flota en la esquina superior derecha del
 * area del teclado (ubicacion pendiente de 04.10; a verificar en A51).
 */
class VitrinaAnchorView(context: Context) : TextView(context) {

    init {
        text = "☰"
        gravity = Gravity.CENTER
        isClickable = true
        isFocusable = true
        setSingleLine(true)
        maxLines = 1
        setTextColor(ContextCompat.getColor(context, R.color.content_primary))
        setTextSize(
            android.util.TypedValue.COMPLEX_UNIT_PX,
            context.resources.getDimension(R.dimen.type_body_large_size),
        )
        background = anchorBackground(context)
        elevation = context.resources.getInteger(R.integer.z_bar).toFloat()
        layoutParams = android.view.ViewGroup.MarginLayoutParams(
            android.view.ViewGroup.MarginLayoutParams.WRAP_CONTENT,
            context.resources.getDimensionPixelSize(R.dimen.kb_anchor_size),
        )
    }

    private fun anchorBackground(context: Context): GradientDrawable {
        val resources = context.resources
        val radius = resources.getDimension(R.dimen.radius_pill)
        val stroke = resources.getDimension(R.dimen.border_width_hairline).toInt()
        return GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE
            cornerRadius = radius
            setColor(ContextCompat.getColor(context, R.color.surface_raised))
            setStroke(stroke, ContextCompat.getColor(context, R.color.border_subtle))
        }
    }
}