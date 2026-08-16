package com.keyboardsales.vitrina.bar

import android.content.Context
import android.view.Gravity
import android.widget.FrameLayout
import android.widget.HorizontalScrollView
import android.widget.LinearLayout
import com.keyboardsales.vitrina.data.CatalogItem
import com.keyboardsales.vitrina.data.QuickReply
import helium314.keyboard.latin.R

/**
 * La superficie de Vitrina capa: fila de chips de coincidencias (productos o
 * respuestas rapidas) o panel de accion (confirm/undo). Es un hermano aditivo
 * de la fila de sugerencias de upstream dentro de strip_container.
 */
class VitrinaBarView(context: Context) : FrameLayout(context) {

    private val chipsRow = HorizontalScrollView(context).apply {
        isHorizontalScrollBarEnabled = false
    }
    private val chips = LinearLayout(context).apply {
        orientation = LinearLayout.HORIZONTAL
        gravity = Gravity.CENTER_VERTICAL
    }
    private val actionView = VitrinaActionView(context)

    init {
        val padX = context.resources.getDimensionPixelSize(R.dimen.kb_bar_pad_h)
        chipsRow.setPadding(padX, 0, padX, 0)
        chipsRow.addView(
            chips,
            FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.WRAP_CONTENT,
                FrameLayout.LayoutParams.MATCH_PARENT,
                Gravity.CENTER_VERTICAL,
            ),
        )
        addView(chipsRow, FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT))
        addView(actionView, FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT))
        showChips()
    }

    var onProductClick: ((CatalogItem) -> Unit)? = null
    var onQuickReplyClick: ((QuickReply) -> Unit)? = null
    var onConfirm: (() -> Unit)? = null
    var onCancel: (() -> Unit)? = null
    var onUndo: (() -> Unit)? = null

    fun showProducts(products: List<CatalogItem>) {
        chips.removeAllViews()
        for (product in products) {
            val chip = VitrinaChipView(
                chips.context,
                product.nombre,
                chips.context.getString(R.string.vitrina_chip_product_cd, product.nombre),
                accent = false,
            )
            chip.setOnClickListener { onProductClick?.invoke(product) }
            chips.addView(chip)
        }
        showChips()
    }

    fun showQuickReplies(replies: List<QuickReply>) {
        chips.removeAllViews()
        for (reply in replies) {
            val chip = VitrinaChipView(
                chips.context,
                "/${reply.atajo}",
                chips.context.getString(R.string.vitrina_chip_reply_cd, reply.atajo),
                accent = true,
            )
            chip.setOnClickListener { onQuickReplyClick?.invoke(reply) }
            chips.addView(chip)
        }
        showChips()
    }

    fun showConfirm(message: String) {
        actionView.showConfirm(message, onConfirm = { onConfirm?.invoke() }, onCancel = { onCancel?.invoke() })
        showAction()
    }

    fun showUndo() {
        actionView.showUndo(onUndo = { onUndo?.invoke() })
        showAction()
    }

    fun clearChips() {
        chips.removeAllViews()
    }

    fun showChips() {
        chipsRow.visibility = VISIBLE
        actionView.visibility = GONE
    }

    fun showAction() {
        chipsRow.visibility = GONE
        actionView.visibility = VISIBLE
    }
}