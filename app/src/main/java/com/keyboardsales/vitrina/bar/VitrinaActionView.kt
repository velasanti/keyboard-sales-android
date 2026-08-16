package com.keyboardsales.vitrina.bar

import android.content.Context
import android.view.Gravity
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import helium314.keyboard.latin.R

/**
 * Panel de accion de Vitrina capa: la confirmacion ADR-016 (mensaje a la vista
 * + Confirmar/Cancelar) y el Deshacer posterior. Reemplaza la fila de chips.
 */
class VitrinaActionView(context: Context) : LinearLayout(context) {

    private val preview = TextView(context)

    init {
        orientation = HORIZONTAL
        gravity = Gravity.CENTER_VERTICAL
        setPadding(
            context.resources.getDimensionPixelSize(R.dimen.kb_bar_pad_h),
            0,
            context.resources.getDimensionPixelSize(R.dimen.kb_bar_pad_h),
            0,
        )
        preview.apply {
            isFocusable = false
            setSingleLine(true)
            maxLines = 1
            ellipsize = android.text.TextUtils.TruncateAt.END
            gravity = Gravity.CENTER_VERTICAL
            setTextSize(
                android.util.TypedValue.COMPLEX_UNIT_PX,
                context.resources.getDimension(R.dimen.type_supporting_label_size),
            )
            setTextColor(ContextCompat.getColor(context, R.color.content_secondary))
        }
    }

    /** Confirmacion ADR-016: el mensaje concreto a la vista antes de enviar. */
    fun showConfirm(message: String, onConfirm: () -> Unit, onCancel: () -> Unit) {
        removeAllViews()
        preview.text = message
        preview.contentDescription = message
        addView(preview, LayoutParams(0, LayoutParams.MATCH_PARENT, 1f))
        val context = context
        val cancel = chip(context, R.string.vitrina_cancel, accent = false)
        cancel.setOnClickListener { onCancel() }
        val confirm = chip(context, R.string.vitrina_confirm, accent = true)
        confirm.setOnClickListener { onConfirm() }
        addView(cancel)
        addView(confirm)
    }

    /** Deshacer: el envio ya ocurrio, se ofrece revertirlo. */
    fun showUndo(onUndo: () -> Unit) {
        removeAllViews()
        preview.text = context.getString(R.string.vitrina_sent_label)
        preview.contentDescription = context.getString(R.string.vitrina_sent_label)
        addView(preview, LayoutParams(0, LayoutParams.MATCH_PARENT, 1f))
        val undo = chip(context, R.string.vitrina_undo, accent = true)
        undo.setOnClickListener { onUndo() }
        addView(undo)
    }

    private fun chip(context: Context, textRes: Int, accent: Boolean) =
        VitrinaChipView(
            context,
            context.getString(textRes),
            context.getString(textRes),
            accent,
        )
}