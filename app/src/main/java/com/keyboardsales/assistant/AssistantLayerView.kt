package com.keyboardsales.assistant

import android.content.Context
import android.graphics.drawable.GradientDrawable
import android.os.Handler
import android.os.Looper
import android.view.Gravity
import android.view.View
import android.widget.LinearLayout
import android.widget.ScrollView
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

    private val historyList = LinearLayout(context).apply { orientation = VERTICAL }
    private val historyScroll = ScrollView(context)
    private val historyPlaceholder = TextView(context)
    private val inputBox = TextView(context)
    private val confirmCard = LinearLayout(context)
    private val confirmText = TextView(context)
    private val undoCard = LinearLayout(context)

    private val input = StringBuilder()

    private val exampleMessageRes = intArrayOf(
        R.string.assistant_example_1,
        R.string.assistant_example_2,
        R.string.assistant_example_3,
        R.string.assistant_example_4,
        R.string.assistant_example_5,
        R.string.assistant_example_6,
        R.string.assistant_example_7,
    )
    private var exampleIndex = 0
    private val rotationHandler = Handler(Looper.getMainLooper())
    private val rotationRunnable = object : Runnable {
        override fun run() {
            if (input.isEmpty()) {
                exampleIndex = (exampleIndex + 1) % exampleMessageRes.size
                renderInput()
            }
            rotationHandler.postDelayed(this, EXAMPLE_ROTATION_MS)
        }
    }

    var onClose: (() -> Unit)? = null
    var onSend: (() -> Unit)? = null
    var onConfirm: (() -> Unit)? = null
    var onCancelConfirm: (() -> Unit)? = null
    var onUndo: (() -> Unit)? = null

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
        historyList.addView(historyPlaceholder)
        historyScroll.run {
            isVerticalScrollBarEnabled = false
            addView(historyList, LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT))
        }
        addView(historyScroll, LayoutParams(LayoutParams.MATCH_PARENT, 0, 1f))

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
        val inputHeight = context.resources.getDimensionPixelSize(R.dimen.size_control_height_sm)
        addView(inputBox, LayoutParams(LayoutParams.MATCH_PARENT, inputHeight))

        confirmText.run {
            setSingleLine(true)
            maxLines = 1
            ellipsize = android.text.TextUtils.TruncateAt.END
            gravity = Gravity.CENTER_VERTICAL
            setTextColor(ContextCompat.getColor(context, R.color.content_primary))
            setTextSize(
                android.util.TypedValue.COMPLEX_UNIT_PX,
                context.resources.getDimension(R.dimen.type_body_small_size),
            )
        }
        val cancel = AssistantAnchorView(context).apply {
            text = context.getString(R.string.vitrina_cancel)
            setTextSize(
                android.util.TypedValue.COMPLEX_UNIT_PX,
                context.resources.getDimension(R.dimen.type_supporting_label_size),
            )
            contentDescription = context.getString(R.string.vitrina_cancel)
        }
        val confirm = AssistantAnchorView(context).apply {
            text = context.getString(R.string.vitrina_confirm)
            setTextSize(
                android.util.TypedValue.COMPLEX_UNIT_PX,
                context.resources.getDimension(R.dimen.type_supporting_label_size),
            )
            contentDescription = context.getString(R.string.vitrina_confirm)
            setOnClickListener { onConfirm?.invoke() }
        }
        cancel.setOnClickListener { onCancelConfirm?.invoke() ?: showInput() }
        confirmCard.run {
            orientation = HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(
                context.resources.getDimensionPixelSize(R.dimen.kb_bar_pad_h),
                context.resources.getDimensionPixelSize(R.dimen.spacing_1),
                context.resources.getDimensionPixelSize(R.dimen.kb_bar_pad_h),
                context.resources.getDimensionPixelSize(R.dimen.spacing_1),
            )
            addView(confirmText, LayoutParams(0, LayoutParams.WRAP_CONTENT, 1f))
            addView(cancel)
            addView(confirm)
            visibility = GONE
        }
        addView(confirmCard, LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT))

        val undo = AssistantAnchorView(context).apply {
            text = context.getString(R.string.vitrina_undo)
            setTextSize(
                android.util.TypedValue.COMPLEX_UNIT_PX,
                context.resources.getDimension(R.dimen.type_supporting_label_size),
            )
            contentDescription = context.getString(R.string.vitrina_undo)
            setOnClickListener { onUndo?.invoke() }
        }
        undoCard.run {
            orientation = HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(
                context.resources.getDimensionPixelSize(R.dimen.kb_bar_pad_h),
                context.resources.getDimensionPixelSize(R.dimen.spacing_1),
                context.resources.getDimensionPixelSize(R.dimen.kb_bar_pad_h),
                context.resources.getDimensionPixelSize(R.dimen.spacing_1),
            )
            addView(undo, LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT))
            visibility = GONE
        }
        addView(undoCard, LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT))
        showInput()
        rotationHandler.postDelayed(rotationRunnable, EXAMPLE_ROTATION_MS)
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
            addView(
                close,
                LayoutParams(
                    LayoutParams.WRAP_CONTENT,
                    context.resources.getDimensionPixelSize(R.dimen.size_control_height_sm),
                ),
            )
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

    fun currentInput(): String = input.toString()

    /** Confirmacion ADR-016: el mensaje redactado a la vista antes de insertar. */
    fun showConfirm(message: String) {
        confirmText.text = message
        confirmText.contentDescription = message
        inputBox.visibility = GONE
        confirmCard.visibility = VISIBLE
        undoCard.visibility = GONE
    }

    /** Deshacer: la insercion ya ocurrio, se ofrece revertirla. */
    fun showUndo() {
        inputBox.visibility = GONE
        confirmCard.visibility = GONE
        undoCard.visibility = VISIBLE
    }

    /** Vuelve al cuadro de texto (despues de cancelar o deshacer). */
    fun showInput() {
        confirmCard.visibility = GONE
        undoCard.visibility = GONE
        inputBox.visibility = VISIBLE
    }

    /**
     * Agrega una fila al historial de la IA (respuesta / resultado). La primera
     * fila reemplaza el placeholder de "historial vacio".
     */
    fun addHistory(text: String) {
        val context = context
        if (historyList.indexOfChild(historyPlaceholder) >= 0) {
            historyList.removeView(historyPlaceholder)
        }
        val row = TextView(context).apply {
            this.text = text
            setSingleLine(true)
            maxLines = 1
            ellipsize = android.text.TextUtils.TruncateAt.END
            setTextColor(ContextCompat.getColor(context, R.color.content_primary))
            setTextSize(
                android.util.TypedValue.COMPLEX_UNIT_PX,
                context.resources.getDimension(R.dimen.type_body_small_size),
            )
            setPadding(
                context.resources.getDimensionPixelSize(R.dimen.kb_bar_pad_h),
                context.resources.getDimensionPixelSize(R.dimen.spacing_1),
                context.resources.getDimensionPixelSize(R.dimen.kb_bar_pad_h),
                context.resources.getDimensionPixelSize(R.dimen.spacing_1),
            )
        }
        historyList.addView(row, LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT))
        historyScroll.post { historyScroll.fullScroll(View.FOCUS_DOWN) }
    }

    private fun renderInput() {
        val context = context
        if (input.isEmpty()) {
            inputBox.setText(exampleMessageRes[exampleIndex])
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

    private companion object {
        const val EXAMPLE_ROTATION_MS = 3_500L
    }
}
