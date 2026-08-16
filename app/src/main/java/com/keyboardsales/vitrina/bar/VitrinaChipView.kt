package com.keyboardsales.vitrina.bar

import android.content.Context
import android.graphics.drawable.GradientDrawable
import android.view.Gravity
import android.widget.TextView
import androidx.core.content.ContextCompat
import helium314.keyboard.latin.R

/**
 * Un chip de Vitrina: producto (surface) o respuesta rapida (accent).
 *
 * Solo referencias a tokens: alto kb/chip/height, radio radius_pill, borde
 * border/width/hairline, texto type/supporting/label, colores semanticos.
 */
class VitrinaChipView(
    context: Context,
    label: String,
    description: String,
    accent: Boolean,
) : TextView(context) {

    constructor(context: Context) : this(context, "", "", false)

    constructor(context: Context, attrs: android.util.AttributeSet?) : this(context, "", "", false)

    init {
        val resources = context.resources
        text = label
        contentDescription = description
        isFocusable = true
        gravity = Gravity.CENTER
        setSingleLine(true)
        maxLines = 1
        ellipsize = android.text.TextUtils.TruncateAt.END
        setTextSize(
            android.util.TypedValue.COMPLEX_UNIT_PX,
            resources.getDimension(R.dimen.type_supporting_label_size),
        )
        setTextColor(
            ContextCompat.getColor(
                context,
                if (accent) R.color.accent_on_subtle else R.color.content_primary,
            ),
        )
        setPadding(
            resources.getDimensionPixelSize(R.dimen.kb_bar_pad_h),
            0,
            resources.getDimensionPixelSize(R.dimen.kb_bar_pad_h),
            0,
        )
        background = chipBackground(context, accent)
        layoutParams = android.view.ViewGroup.MarginLayoutParams(
            android.view.ViewGroup.MarginLayoutParams.WRAP_CONTENT,
            resources.getDimensionPixelSize(R.dimen.kb_chip_height),
        ).apply {
            marginEnd = resources.getDimensionPixelSize(R.dimen.kb_bar_gap)
        }
    }

    private fun chipBackground(context: Context, accent: Boolean): GradientDrawable {
        val resources = context.resources
        val radius = resources.getDimension(R.dimen.radius_pill)
        val strokeWidth = resources.getDimension(R.dimen.border_width_hairline).toInt()
        val fillColor = ContextCompat.getColor(
            context,
            if (accent) R.color.accent_subtle else R.color.surface_raised,
        )
        val strokeColor = ContextCompat.getColor(
            context,
            if (accent) R.color.accent_subtle else R.color.border_subtle,
        )
        return GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE
            cornerRadius = radius
            setColor(fillColor)
            setStroke(strokeWidth, strokeColor)
        }
    }
}