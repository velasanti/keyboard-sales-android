package com.keyboardsales.vitrina.panel

import android.content.Context
import android.graphics.drawable.GradientDrawable
import android.view.Gravity
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import com.keyboardsales.vitrina.data.DocumentItem
import helium314.keyboard.latin.R
import java.text.SimpleDateFormat
import java.util.Locale

/**
 * Tarjeta de un documento dentro de "Documentos" (nivel 2 de Vitrina modo).
 *
 * Mismo patron visual que las tarjetas de producto (fondo surface_raised,
 * borde hairline, radio radius_sm) pero sin precio: nombre + badge de tipo
 * (PDF) + metadato "actualizado hace X".
 */
class DocumentSelectionCard(
    context: Context,
    document: DocumentItem,
    onClick: () -> Unit,
) : LinearLayout(context) {

    init {
        val resources = context.resources
        orientation = HORIZONTAL
        gravity = Gravity.CENTER_VERTICAL
        isClickable = true
        isFocusable = true
        contentDescription = context.getString(R.string.vitrina_document_cd, document.nombre)
        setPadding(
            resources.getDimensionPixelSize(R.dimen.kb_bar_pad_h),
            resources.getDimensionPixelSize(R.dimen.spacing_1),
            resources.getDimensionPixelSize(R.dimen.kb_bar_pad_h),
            resources.getDimensionPixelSize(R.dimen.spacing_1),
        )
        background = cardBackground(context)

        val badge = TextView(context).apply {
            text = document.tipo
            gravity = Gravity.CENTER
            setSingleLine(true)
            maxLines = 1
            setTextColor(ContextCompat.getColor(context, R.color.accent_on_subtle))
            setTextSize(
                android.util.TypedValue.COMPLEX_UNIT_PX,
                resources.getDimension(R.dimen.type_supporting_label_size),
            )
            setPadding(
                resources.getDimensionPixelSize(R.dimen.spacing_2),
                0,
                resources.getDimensionPixelSize(R.dimen.spacing_2),
                0,
            )
            background = badgeBackground(context)
        }
        val name = TextView(context).apply {
            text = document.nombre
            setSingleLine(true)
            maxLines = 1
            ellipsize = android.text.TextUtils.TruncateAt.END
            setTextColor(ContextCompat.getColor(context, R.color.content_primary))
            setTextSize(
                android.util.TypedValue.COMPLEX_UNIT_PX,
                resources.getDimension(R.dimen.type_body_small_size),
            )
        }
        val meta = TextView(context).apply {
            text = updatedAgo(context, document.actualizadoEn)
            setSingleLine(true)
            maxLines = 1
            setTextColor(ContextCompat.getColor(context, R.color.content_secondary))
            setTextSize(
                android.util.TypedValue.COMPLEX_UNIT_PX,
                resources.getDimension(R.dimen.type_supporting_label_size),
            )
        }

        addView(badge, LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT).apply {
            marginEnd = resources.getDimensionPixelSize(R.dimen.spacing_2)
        })
        addView(name, LayoutParams(0, LayoutParams.WRAP_CONTENT, 1f))
        meta.setPadding(
            resources.getDimensionPixelSize(R.dimen.spacing_2),
            0,
            0,
            0,
        )
        addView(meta)

        setOnClickListener { onClick() }
    }

    private fun updatedAgo(context: Context, isoDate: String): String {
        val then = runCatching {
            SimpleDateFormat("yyyy-MM-dd", Locale.US).parse(isoDate)?.time ?: 0L
        }.getOrDefault(0L)
        val days = ((System.currentTimeMillis() - then) / MILLIS_PER_DAY).coerceAtLeast(0L)
        return when (days) {
            0L -> context.getString(R.string.vitrina_document_updated_today)
            1L -> context.getString(R.string.vitrina_document_updated_yesterday)
            else -> context.getString(R.string.vitrina_document_updated_days, days)
        }
    }

    private fun cardBackground(context: Context): GradientDrawable {
        val resources = context.resources
        val radius = resources.getDimension(R.dimen.radius_sm)
        val stroke = resources.getDimension(R.dimen.border_width_hairline).toInt()
        return GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE
            cornerRadius = radius
            setColor(ContextCompat.getColor(context, R.color.surface_raised))
            setStroke(stroke, ContextCompat.getColor(context, R.color.border_subtle))
        }
    }

    private fun badgeBackground(context: Context): GradientDrawable {
        val radius = context.resources.getDimension(R.dimen.radius_pill)
        return GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE
            cornerRadius = radius
            setColor(ContextCompat.getColor(context, R.color.accent_subtle))
        }
    }

    private companion object {
        const val MILLIS_PER_DAY = 86_400_000L
    }
}
