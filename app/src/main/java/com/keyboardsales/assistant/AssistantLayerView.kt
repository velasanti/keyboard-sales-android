package com.keyboardsales.assistant

import android.content.Context
import android.graphics.drawable.GradientDrawable
import android.view.Gravity
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import helium314.keyboard.latin.R

/**
 * La capa ✨: reemplaza la franja de sugerencias cuando el asistente esta activo.
 * 04.4 §3 — es una capa, no un modo: el QWERTY nunca se toca.
 *
 * Paso 1 (shell): solo la estructura visual — historial de la IA arriba y
 * cuadro de texto propio del vendedor justo encima del QWERTY. La captura de
 * teclado (Paso 2) y el intent dummy (Paso 3) se enganchan despues.
 */
class AssistantLayerView(context: Context) : LinearLayout(context) {

    private val historyPlaceholder = TextView(context)
    private val inputBox = TextView(context)

    private val input = StringBuilder()

    var onClose: (() -> Unit)? = null
    var onSend: (() -> Unit)? = null

    init {
        orientation = VERTICAL
        setBackgroundColor(ContextCompat.getColor(context, R.color.surface_panel))
        addView(header(context))
        historyPlaceholder.run {
            setText(R.string.assistant_history_placeholder)
            gravity = Gravity.TOP
            setSingleLine(true)
            maxLines = 1
            ellipsize = android.text.TextUtils.TruncateAt.END
            setTextColor(ContextCompat.getColor(context, R.color.content_secondary))
            setTextSize(
                android.util.TypedValue.COMPLEX_UNIT_PX,
                context.resources.getDimension(R.dimen.type_supporting_label_size),
            )
            setPadding(
                context.resources.getDimensionPixelSize(R.dimen.kb_bar_pad_h),
                context.resources.getDimensionPixelSize(R.dimen.spacing_2),
                context.resources.getDimensionPixelSize(R.dimen.kb_bar_pad_h),
                context.resources.getDimensionPixelSize(R.dimen.spacing_2),
            )
        }
        addView(historyPlaceholder, LayoutParams(LayoutParams.MATCH_PARENT, 0, 1f))

        inputBox.run {
            setText(R.string.assistant_input_placeholder)
            gravity = Gravity.CENTER_VERTICAL
            setSingleLine(true)
            maxLines = 1
            ellipsize = android.text.TextUtils.TruncateAt.END
            setTextColor(ContextCompat.getColor(context, R.color.content_secondary))
            setTextSize(
                android.util.TypedValue.COMPLEX_UNIT_PX,
                context.resources.getDimension(R.dimen.type_body_small_size),
            )
            setPadding(
                context.resources.getDimensionPixelSize(R.dimen.kb_bar_pad_h),
                0,
                context.resources.getDimensionPixelSize(R.dimen.kb_bar_pad_h),
                0,
            )
            background = inputBackground(context)
        }
        val inputHeight = context.resources.getDimensionPixelSize(R.dimen.size_touch_min)
        addView(inputBox, LayoutParams(LayoutParams.MATCH_PARENT, inputHeight))
    }

    private fun header(context: Context): LinearLayout {
        val title = TextView(context).apply {
            setText(R.string.assistant_title)
            setSingleLine(true)
            maxLines = 1
            gravity = Gravity.CENTER_VERTICAL
            setTextColor(ContextCompat.getColor(context, R.color.content_primary))
            setTextSize(
                android.util.TypedValue.COMPLEX_UNIT_PX,
                context.resources.getDimension(R.dimen.type_body_regular_size),
            )
        }
        val close = AssistantAnchorView(context).apply {
            text = "✕"
            setTextColor(ContextCompat.getColor(context, R.color.content_primary))
            contentDescription = context.getString(R.string.assistant_close)
            setOnClickListener { onClose?.invoke() }
        }
        return LinearLayout(context).apply {
            orientation = HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(
                context.resources.getDimensionPixelSize(R.dimen.kb_bar_pad_h),
                0,
                context.resources.getDimensionPixelSize(R.dimen.kb_bar_pad_h),
                0,
            )
            addView(title, LayoutParams(0, LayoutParams.WRAP_CONTENT, 1f))
            addView(close)
            setBackgroundColor(ContextCompat.getColor(context, R.color.surface_panel))
        }
    }

    // ------------------------------------------------------------------
    // Captura de entrada del QWERTY (Paso 2) — el texto vive en [input],
    // nunca en el campo de la app anfitriona.
    // ------------------------------------------------------------------

    fun inputCharacter(codePoint: Int) {
        if (codePoint < 0) return
        val ch = codePoint.toChar()
        if (!ch.isDefined() || ch == '\n') return
        input.append(ch)
        renderInput()
    }

    fun inputBackspace() {
        if (input.isEmpty()) return
        input.deleteCharAt(input.length - 1)
        renderInput()
    }

    fun send() {
        if (input.isNotEmpty()) onSend?.invoke()
        input.clear()
        renderInput()
    }

    fun clearInput() {
        input.clear()
        renderInput()
    }

    private fun renderInput() {
        val context = context
        if (input.isEmpty()) {
            inputBox.setText(R.string.assistant_input_placeholder)
            inputBox.setTextColor(ContextCompat.getColor(context, R.color.content_secondary))
        } else {
            inputBox.text = input.toString()
            inputBox.setTextColor(ContextCompat.getColor(context, R.color.content_primary))
        }
    }

    private fun inputBackground(context: Context): GradientDrawable {
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
}
