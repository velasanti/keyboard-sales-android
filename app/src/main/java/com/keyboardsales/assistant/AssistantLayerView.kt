package com.keyboardsales.assistant

import android.content.Context
import android.graphics.drawable.GradientDrawable
import android.os.Handler
import android.os.Looper
import android.text.InputType
import android.view.Gravity
import android.widget.EditText
import android.widget.LinearLayout
import androidx.core.content.ContextCompat
import helium314.keyboard.latin.R

/**
 * La capa ✨ como FRANJA ANGOSTA (decisión 2026-08-19): una sola fila con la
 * caja de texto del vendedor (que muestra mensajes de ejemplo rotando cuando
 * está vacía). No hay header, historial ni botón de cerrar: cerrar es tocar de
 * nuevo ✦, igual que en el panel de ☰ (Vitrina). El campo de predicción de
 * texto normal queda visible debajo/arriba, no se tapa.
 *
 * La caja es un [EditText] real: el vendedor puede tocar en medio del texto
 * para posicionar el cursor y corregir, y la caja crece en alto (auto-expand)
 * hasta [MAX_INPUT_LINES] líneas antes de hacer scroll interno. La escritura
 * sigue llegando por [onAssistantEvent] desde el QWERTY (no por el teclado del
 * sistema), pero se inserta en la posición del cursor, no siempre al final.
 *
 * La tarjeta de confirmación (ADR-016) y el Deshacer reemplazan esa fila en el
 * mismo lugar (showConfirm / showUndo). Las respuestas de consulta y el
 * resultado de una acción se muestran como hint transitorio (addHistory), que
 * se reemplaza apenas el vendedor escribe.
 */
class AssistantLayerView(context: Context) : LinearLayout(context) {

    private val inputBox = EditText(context)
    private val inputRow = LinearLayout(context)
    private val confirmCard = LinearLayout(context)
    private val confirmText = android.widget.TextView(context)
    private val undoCard = LinearLayout(context)

    private var resultText: String? = null

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
            if (inputBox.text.isEmpty() && resultText == null) {
                exampleIndex = (exampleIndex + 1) % exampleMessageRes.size
                renderInput()
            }
            rotationHandler.postDelayed(this, EXAMPLE_ROTATION_MS)
        }
    }

    var onSend: (() -> Unit)? = null
    var onConfirm: (() -> Unit)? = null
    var onCancelConfirm: (() -> Unit)? = null
    var onUndo: (() -> Unit)? = null

    /**
     * Se dispara cuando la caja cambia de alto (auto-expand) con el alto DESEADO
     * de la capa en px, calculado desde el lineCount del EditText (independiente
     * del alto del contenedor, sin dependencia circular).
     */
    var onInputResized: ((Int) -> Unit)? = null

    init {
        orientation = VERTICAL
        setBackgroundColor(ContextCompat.getColor(context, R.color.surface_panel))

        inputBox.run {
            setSingleLine(false)
            minLines = 1
            maxLines = MAX_INPUT_LINES
            minHeight = context.resources.getDimensionPixelSize(R.dimen.size_control_height_sm)
            inputType = InputType.TYPE_CLASS_TEXT or
                InputType.TYPE_TEXT_FLAG_MULTI_LINE or
                InputType.TYPE_TEXT_FLAG_CAP_SENTENCES
            gravity = Gravity.CENTER_VERTICAL or Gravity.START
            isFocusable = true
            isFocusableInTouchMode = true
            isCursorVisible = true
            setTextColor(ContextCompat.getColor(context, R.color.content_primary))
            setHintTextColor(ContextCompat.getColor(context, R.color.content_secondary))
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
            background = inputBackground(context)
            // Auto-expand: calcula el alto deseado de la capa desde el lineCount
            // del propio EditText (no desde el alto medido del contenedor). Rompe
            // la dependencia circular de "medir la propia altura para decidirla".
            addOnLayoutChangeListener { _, _, _, _, _, _, _, _, _ ->
                val lineHeight = lineHeight
                if (lineHeight <= 0) return@addOnLayoutChangeListener
                val textHeight = lineCount.coerceAtMost(MAX_INPUT_LINES) * lineHeight +
                    paddingTop + paddingBottom
                val layerHeight = textHeight + inputRow.paddingTop + inputRow.paddingBottom
                onInputResized?.invoke(layerHeight)
            }
        }

        inputRow.run {
            orientation = HORIZONTAL
            gravity = Gravity.TOP
            setPadding(
                context.resources.getDimensionPixelSize(R.dimen.kb_bar_pad_h),
                context.resources.getDimensionPixelSize(R.dimen.spacing_1),
                context.resources.getDimensionPixelSize(R.dimen.kb_bar_pad_h),
                context.resources.getDimensionPixelSize(R.dimen.spacing_1),
            )
            addView(inputBox, LayoutParams(0, LayoutParams.WRAP_CONTENT, 1f))
        }
        addView(inputRow, LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT))

        confirmText.run {
            setSingleLine(false)
            maxLines = MAX_CONFIRM_LINES
            ellipsize = android.text.TextUtils.TruncateAt.END
            gravity = Gravity.TOP or Gravity.START
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
            orientation = VERTICAL
            setPadding(
                context.resources.getDimensionPixelSize(R.dimen.kb_bar_pad_h),
                context.resources.getDimensionPixelSize(R.dimen.spacing_1),
                context.resources.getDimensionPixelSize(R.dimen.kb_bar_pad_h),
                context.resources.getDimensionPixelSize(R.dimen.spacing_1),
            )
            addView(confirmText, LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT))
            val buttons = LinearLayout(context).apply {
                orientation = HORIZONTAL
                gravity = Gravity.END
                addView(
                    cancel,
                    LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT).apply {
                        marginEnd = context.resources.getDimensionPixelSize(R.dimen.kb_bar_gap)
                    },
                )
                addView(confirm)
            }
            addView(buttons, LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT))
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
        renderInput()
        rotationHandler.postDelayed(rotationRunnable, EXAMPLE_ROTATION_MS)
    }

    // ------------------------------------------------------------------
    // Captura de entrada del QWERTY (Paso 2) — el texto vive en el EditText,
    // nunca en el campo de la app anfitriona. Se inserta en el cursor.
    // ------------------------------------------------------------------

    fun inputCharacter(codePoint: Int) {
        if (codePoint < 0) return
        val ch = codePoint.toChar()
        if (!ch.isDefined() || ch == '\n') return
        resultText = null
        val start = inputBox.selectionStart
        val end = inputBox.selectionEnd
        inputBox.text.replace(start, end, ch.toString())
        inputBox.setSelection(start + 1)
        renderInput()
    }

    fun inputBackspace() {
        resultText = null
        val start = inputBox.selectionStart
        val end = inputBox.selectionEnd
        if (start != end) {
            inputBox.text.delete(start, end)
        } else if (start > 0) {
            inputBox.text.delete(start - 1, start)
        }
        renderInput()
    }

    fun send() {
        if (inputBox.text.isNotEmpty()) onSend?.invoke()
        inputBox.text.clear()
        resultText = null
        renderInput()
    }

    fun clearInput() {
        inputBox.text.clear()
        resultText = null
        renderInput()
    }

    fun currentInput(): String = inputBox.text.toString()

    /** Confirmacion ADR-016: el mensaje redactado a la vista antes de insertar. */
    fun showConfirm(message: String) {
        confirmText.text = message
        confirmText.contentDescription = message
        inputRow.visibility = GONE
        confirmCard.visibility = VISIBLE
        undoCard.visibility = GONE
    }

    /** Deshacer: la insercion ya ocurrio, se ofrece revertirla. */
    fun showUndo() {
        inputRow.visibility = GONE
        confirmCard.visibility = GONE
        undoCard.visibility = VISIBLE
    }

    /** Vuelve al cuadro de texto (despues de cancelar o deshacer). */
    fun showInput() {
        confirmCard.visibility = GONE
        undoCard.visibility = GONE
        inputRow.visibility = VISIBLE
    }

    /**
     * Muestra una respuesta (consulta, resultado de acción) como hint transitorio
     * en la caja. Se reemplaza en cuanto el vendedor escribe.
     */
    fun addHistory(text: String) {
        resultText = text
        renderInput()
    }

    private fun renderInput() {
        inputBox.hint = resultText ?: context.getString(exampleMessageRes[exampleIndex])
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
        const val MAX_INPUT_LINES = 4
        const val MAX_CONFIRM_LINES = 4
    }
}
