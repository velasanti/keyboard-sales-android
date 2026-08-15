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
 * La fila de chips de Vitrina capa. Un HorizontalScrollView aditivo dentro de
 * strip_container, hermano de la fila de sugerencias de upstream (que se oculta
 * mientras Vitrina esta activa).
 */
class VitrinaBarView(context: Context) : HorizontalScrollView(context) {

    private val chips = LinearLayout(context).apply {
        orientation = LinearLayout.HORIZONTAL
        gravity = Gravity.CENTER_VERTICAL
    }

    init {
        isHorizontalScrollBarEnabled = false
        setPadding(
            context.resources.getDimensionPixelSize(R.dimen.kb_bar_pad_h),
            0,
            context.resources.getDimensionPixelSize(R.dimen.kb_bar_pad_h),
            0,
        )
        addView(
            chips,
            FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.WRAP_CONTENT,
                FrameLayout.LayoutParams.MATCH_PARENT,
                Gravity.CENTER_VERTICAL,
            ),
        )
    }

    fun showProducts(products: List<CatalogItem>) {
        renderChips(products.map { it.nombre to cd(it) }, accent = false)
    }

    fun showQuickReplies(replies: List<QuickReply>) {
        renderChips(replies.map { "/${it.atajo}" to cd(it) }, accent = true)
    }

    fun clear() {
        chips.removeAllViews()
    }

    private fun renderChips(items: List<Pair<String, String>>, accent: Boolean) {
        chips.removeAllViews()
        val context = chips.context
        for ((label, description) in items) {
            chips.addView(VitrinaChipView(context, label, description, accent))
        }
    }

    private fun cd(product: CatalogItem): String =
        context.getString(R.string.vitrina_chip_product_cd, product.nombre)

    private fun cd(reply: QuickReply): String =
        context.getString(R.string.vitrina_chip_reply_cd, reply.atajo)
}