package com.keyboardsales.vitrina.panel

import android.content.Context
import android.graphics.drawable.GradientDrawable
import android.view.Gravity
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import androidx.core.content.ContextCompat
import com.keyboardsales.vitrina.bar.VitrinaChipView
import com.keyboardsales.vitrina.data.CatalogGrouping
import com.keyboardsales.vitrina.data.CatalogItem
import com.keyboardsales.vitrina.data.MessageVariant
import com.keyboardsales.vitrina.data.QuickReply
import com.keyboardsales.vitrina.insert.PriceFormatter
import helium314.keyboard.latin.R

/**
 * La superficie Vitrina modo: el catalogo completo (productos y respuestas
 * rapidas) en lugar del QWERTY. Se inyecta como hermano de keyboard_view dentro
 * de keyboard_view_wrapper, con el mismo patron que emoji_palettes_view.
 */
class VitrinaPanelView(context: Context) : LinearLayout(context) {

    private val list = LinearLayout(context).apply { orientation = VERTICAL }

    private var products: List<CatalogItem> = emptyList()
    private var replies: List<QuickReply> = emptyList()
    private var variantsByItemId: Map<String, List<MessageVariant>> = emptyMap()
    private var expandedItemId: String? = null

    var onProductClick: ((CatalogItem) -> Unit)? = null
    var onVariantClick: ((CatalogItem, MessageVariant) -> Unit)? = null
    var onQuickReplyClick: ((QuickReply) -> Unit)? = null
    var onClose: (() -> Unit)? = null

    init {
        orientation = VERTICAL
        setBackgroundColor(ContextCompat.getColor(context, R.color.surface_panel))
        addView(header(context))
        val scroll = ScrollView(context).apply {
            isVerticalScrollBarEnabled = false
            addView(list, LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT))
        }
        addView(scroll, LayoutParams(LayoutParams.MATCH_PARENT, 0, 1f))
    }

    fun populate(products: List<CatalogItem>, replies: List<QuickReply>, variants: List<MessageVariant>) {
        this.products = products
        this.replies = replies
        this.variantsByItemId = variants.groupBy { it.itemId }
        expandedItemId = null
        render()
    }

    private fun render() {
        val context = context
        list.removeAllViews()
        for ((categoria, grouped) in CatalogGrouping.byCategory(products)) {
            list.addView(sectionLabel(context, categoria))
            for (product in grouped) {
                list.addView(productRow(context, product))
                if (expandedItemId == product.id) {
                    for (variant in variantsByItemId[product.id].orEmpty()) {
                        list.addView(variantRow(context, product, variant))
                    }
                }
            }
        }
        list.addView(sectionLabel(context, context.getString(R.string.vitrina_section_quick_replies)))
        for (reply in replies) {
            list.addView(quickReplyRow(context, reply))
        }
    }

    private fun header(context: Context): LinearLayout {
        val title = TextView(context).apply {
            setText(R.string.vitrina_title)
            setSingleLine(true)
            maxLines = 1
            gravity = Gravity.CENTER_VERTICAL
            setTextColor(ContextCompat.getColor(context, R.color.content_primary))
            setTextSize(
                android.util.TypedValue.COMPLEX_UNIT_PX,
                context.resources.getDimension(R.dimen.type_body_regular_size),
            )
        }
        val close = VitrinaChipView(
            context,
            context.getString(R.string.vitrina_close),
            context.getString(R.string.vitrina_close),
            accent = false,
        ).apply { setOnClickListener { onClose?.invoke() } }
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

    private fun sectionLabel(context: Context, text: String): TextView =
        TextView(context).apply {
            setText(text)
            setSingleLine(true)
            maxLines = 1
            setTextColor(ContextCompat.getColor(context, R.color.content_secondary))
            setTextSize(
                android.util.TypedValue.COMPLEX_UNIT_PX,
                context.resources.getDimension(R.dimen.type_supporting_label_size),
            )
            setPadding(
                context.resources.getDimensionPixelSize(R.dimen.kb_bar_pad_h),
                context.resources.getDimensionPixelSize(R.dimen.spacing_1),
                context.resources.getDimensionPixelSize(R.dimen.kb_bar_pad_h),
                context.resources.getDimensionPixelSize(R.dimen.spacing_1),
            )
        }

    private fun productRow(context: Context, product: CatalogItem): LinearLayout {
        val name = TextView(context).apply {
            text = product.nombre
            setSingleLine(true)
            maxLines = 1
            ellipsize = android.text.TextUtils.TruncateAt.END
            setTextColor(ContextCompat.getColor(context, R.color.content_primary))
            setTextSize(
                android.util.TypedValue.COMPLEX_UNIT_PX,
                context.resources.getDimension(R.dimen.type_body_small_size),
            )
        }
        val price = TextView(context).apply {
            text = PriceFormatter.format(product.precio, product.moneda)
            setSingleLine(true)
            maxLines = 1
            setTextColor(ContextCompat.getColor(context, R.color.content_secondary))
            setTextSize(
                android.util.TypedValue.COMPLEX_UNIT_PX,
                context.resources.getDimension(R.dimen.type_supporting_label_size),
            )
        }
        return row(
            context,
            name,
            price,
            context.getString(R.string.vitrina_chip_product_cd, product.nombre),
        ).apply {
            setOnClickListener {
                if (variantsByItemId[product.id].isNullOrEmpty()) {
                    onProductClick?.invoke(product)
                } else {
                    expandedItemId = if (expandedItemId == product.id) null else product.id
                    render()
                }
            }
        }
    }

    private fun variantRow(context: Context, product: CatalogItem, variant: MessageVariant): LinearLayout {
        val tipo = TextView(context).apply {
            text = variant.tipo
            setSingleLine(true)
            maxLines = 1
            setTextColor(ContextCompat.getColor(context, R.color.accent_on_subtle))
            setTextSize(
                android.util.TypedValue.COMPLEX_UNIT_PX,
                context.resources.getDimension(R.dimen.type_supporting_label_size),
            )
        }
        val texto = TextView(context).apply {
            text = variant.texto
            setSingleLine(true)
            maxLines = 1
            ellipsize = android.text.TextUtils.TruncateAt.END
            setTextColor(ContextCompat.getColor(context, R.color.content_secondary))
            setTextSize(
                android.util.TypedValue.COMPLEX_UNIT_PX,
                context.resources.getDimension(R.dimen.type_body_small_size),
            )
        }
        return row(
            context,
            tipo,
            texto,
            context.getString(R.string.vitrina_chip_product_cd, product.nombre),
        ).apply {
            setOnClickListener { onVariantClick?.invoke(product, variant) }
        }
    }

    private fun quickReplyRow(context: Context, reply: QuickReply): LinearLayout {
        val atajo = TextView(context).apply {
            text = "/${reply.atajo}"
            setSingleLine(true)
            maxLines = 1
            setTextColor(ContextCompat.getColor(context, R.color.accent_on_subtle))
            setTextSize(
                android.util.TypedValue.COMPLEX_UNIT_PX,
                context.resources.getDimension(R.dimen.type_supporting_label_size),
            )
        }
        val texto = TextView(context).apply {
            text = reply.texto
            setSingleLine(true)
            maxLines = 1
            ellipsize = android.text.TextUtils.TruncateAt.END
            setTextColor(ContextCompat.getColor(context, R.color.content_secondary))
            setTextSize(
                android.util.TypedValue.COMPLEX_UNIT_PX,
                context.resources.getDimension(R.dimen.type_body_small_size),
            )
        }
        return row(
            context,
            atajo,
            texto,
            context.getString(R.string.vitrina_chip_reply_cd, reply.atajo),
        ).apply {
            setOnClickListener { onQuickReplyClick?.invoke(reply) }
        }
    }

    private fun row(context: Context, left: TextView, right: TextView, description: String): LinearLayout {
        val row = LinearLayout(context).apply {
            orientation = HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            contentDescription = description
            isClickable = true
            isFocusable = true
            setPadding(
                context.resources.getDimensionPixelSize(R.dimen.kb_bar_pad_h),
                context.resources.getDimensionPixelSize(R.dimen.spacing_1),
                context.resources.getDimensionPixelSize(R.dimen.kb_bar_pad_h),
                context.resources.getDimensionPixelSize(R.dimen.spacing_1),
            )
            background = rowBackground(context)
            addView(left, LayoutParams(0, LayoutParams.WRAP_CONTENT, 1f))
            right.setPadding(
                context.resources.getDimensionPixelSize(R.dimen.spacing_2),
                0,
                0,
                0,
            )
            addView(right)
        }
        row.layoutParams = LayoutParams(
            LayoutParams.MATCH_PARENT,
            context.resources.getDimensionPixelSize(R.dimen.kb_chip_height),
        )
        return row
    }

    private fun rowBackground(context: Context): GradientDrawable {
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