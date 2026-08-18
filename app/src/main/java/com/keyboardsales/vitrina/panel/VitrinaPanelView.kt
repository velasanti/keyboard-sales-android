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
import com.keyboardsales.vitrina.data.DocumentDummy
import com.keyboardsales.vitrina.data.DocumentItem
import com.keyboardsales.vitrina.data.MessageVariant
import com.keyboardsales.vitrina.data.QuickReply
import com.keyboardsales.vitrina.insert.PriceFormatter
import com.keyboardsales.vitrina.search.CatalogMatcher
import com.keyboardsales.vitrina.switch.VitrinaSwitch
import helium314.keyboard.latin.R

/**
 * La superficie Vitrina modo: el catalogo completo (productos y respuestas
 * rapidas) en lugar del QWERTY. Se inyecta como hermano de keyboard_view dentro
 * de keyboard_view_wrapper, con el mismo patron que emoji_palettes_view.
 *
 * Cabecera con VitrinaSwitch (Producto | Booking | Respuestas rapidas):
 * el segmento activo decide que se renderiza en `list`.
 *
 * Segmento Producto, ademas, trae:
 *  - la lupa (buscador) que filtra el catalogo en vivo reusando
 *    [CatalogMatcher] y [com.keyboardsales.vitrina.search.TextNormalizer];
 *  - la entrada "Documentos" (nivel 2 dentro del panel): reemplaza la lista por
 *    [DocumentSelectionCard] con afordancia explicita de volver.
 */
class VitrinaPanelView(context: Context) : LinearLayout(context) {

    private val list = LinearLayout(context).apply { orientation = VERTICAL }
    private val scroll = ScrollView(context).apply {
        isVerticalScrollBarEnabled = false
        addView(list, LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT))
    }

    private var products: List<CatalogItem> = emptyList()
    private var replies: List<QuickReply> = emptyList()
    private var variantsByItemId: Map<String, List<MessageVariant>> = emptyMap()
    private var expandedItemId: String? = null
    private val documents: List<DocumentItem> = DocumentDummy.all()

    private enum class Segment { PRODUCTO, BOOKING, RESPUESTAS }
    private var currentSegment = Segment.PRODUCTO
    private var showingDocuments = false
    private var searchFocused = false
    private val query = StringBuilder()

    /** Selector de 3 segmentos en la cabecera. Expuesto para que VitrinaHost lo
     * encuentre (panel.segmentSwitch), aunque el cambio de contenido lo maneja
     * este mismo archivo internamente. */
    val segmentSwitch = VitrinaSwitch(context)

    private val searchField = TextView(context)
    private val searchClear = TextView(context)
    private val documentsButton = TextView(context)
    private val searchRow = buildSearchRow(context)
    private val backRow = buildBackRow(context)

    var onProductClick: ((CatalogItem) -> Unit)? = null
    var onVariantClick: ((CatalogItem, MessageVariant) -> Unit)? = null
    var onQuickReplyClick: ((QuickReply) -> Unit)? = null
    var onDocumentClick: ((DocumentItem) -> Unit)? = null
    var onClose: (() -> Unit)? = null
    var onSearchFocus: (() -> Unit)? = null
    var onSearchBlur: (() -> Unit)? = null

    init {
        orientation = VERTICAL
        setBackgroundColor(ContextCompat.getColor(context, R.color.surface_panel))
        val touchHeight = context.resources.getDimensionPixelSize(R.dimen.size_touch_min)
        addView(header(context))
        addView(segmentSwitch, LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT))
        addView(searchRow, LayoutParams(LayoutParams.MATCH_PARENT, touchHeight))
        addView(backRow, LayoutParams(LayoutParams.MATCH_PARENT, touchHeight))
        addView(scroll, LayoutParams(LayoutParams.MATCH_PARENT, 0, 1f))

        segmentSwitch.setOnProductSelected {
            currentSegment = Segment.PRODUCTO
            showingDocuments = false
            applyVisibility()
        }
        segmentSwitch.setOnBookingSelected {
            currentSegment = Segment.BOOKING
            showingDocuments = false
            applyVisibility()
        }
        segmentSwitch.setOnQuickRepliesSelected {
            currentSegment = Segment.RESPUESTAS
            showingDocuments = false
            applyVisibility()
        }
        applyVisibility()
    }

    fun populate(products: List<CatalogItem>, replies: List<QuickReply>, variants: List<MessageVariant>) {
        this.products = products
        this.replies = replies
        this.variantsByItemId = variants.groupBy { it.itemId }
        expandedItemId = null
        showingDocuments = false
        query.clear()
        searchFocused = false
        applyVisibility()
    }

    // ------------------------------------------------------------------
    // Busqueda (lupa) — captura de teclas, filtrado en vivo
    // ------------------------------------------------------------------

    fun inputCharacter(codePoint: Int) {
        if (codePoint < 0) return
        val ch = codePoint.toChar()
        if (!ch.isDefined() || ch == '\n') return
        // La captura reusa el mecanismo de ✨ (que conserva el case del
        // auto-capitalize del IME), pero la lupa es un buscador: normaliza a
        // minuscula solo aca, sin tocar el mecanismo compartido.
        query.append(ch.lowercaseChar())
        applyVisibility()
    }

    fun inputBackspace() {
        if (query.isEmpty()) return
        query.deleteCharAt(query.length - 1)
        applyVisibility()
    }

    private fun focusSearch() {
        if (searchFocused) return
        searchFocused = true
        onSearchFocus?.invoke()
        applyVisibility()
    }

    private fun blurSearch() {
        if (!searchFocused) return
        searchFocused = false
        onSearchBlur?.invoke()
        applyVisibility()
    }

    private fun toggleSearch() {
        if (searchFocused) blurSearch() else focusSearch()
    }

    private fun clearSearch() {
        if (query.isEmpty()) {
            blurSearch()
            return
        }
        query.clear()
        applyVisibility()
    }

    private fun openDocuments() {
        if (searchFocused) blurSearch()
        showingDocuments = true
        applyVisibility()
    }

    // ------------------------------------------------------------------
    // Render
    // ------------------------------------------------------------------

    private fun applyVisibility() {
        val inSearch = searchFocused
        val inDocuments = showingDocuments
        val productSegment = currentSegment == Segment.PRODUCTO

        segmentSwitch.visibility = if (inSearch || inDocuments) GONE else VISIBLE
        searchRow.visibility = if (productSegment && !inDocuments) VISIBLE else GONE
        backRow.visibility = if (inDocuments) VISIBLE else GONE
        scroll.visibility = if (inSearch) GONE else VISIBLE
        searchClear.visibility = if (query.isNotEmpty()) VISIBLE else GONE
        renderSearchField()
        render()
    }

    private fun renderSearchField() {
        val context = context
        if (query.isEmpty()) {
            searchField.setText(R.string.vitrina_search_hint)
            searchField.setTextColor(ContextCompat.getColor(context, R.color.content_secondary))
        } else {
            searchField.text = query.toString()
            searchField.setTextColor(ContextCompat.getColor(context, R.color.content_primary))
        }
    }

    private fun render() {
        val context = context
        list.removeAllViews()
        when {
            showingDocuments -> renderDocuments(context)
            else -> when (currentSegment) {
                Segment.PRODUCTO -> renderProducts(context)
                Segment.BOOKING -> renderBookingPlaceholder(context)
                Segment.RESPUESTAS -> renderQuickReplies(context)
            }
        }
    }

    private fun renderProducts(context: Context) {
        val visible = if (query.isBlank()) {
            products
        } else {
            CatalogMatcher.matchItems(products, query.toString())
        }
        if (visible.isEmpty()) {
            list.addView(emptyState(context))
            return
        }
        for ((categoria, grouped) in CatalogGrouping.byCategory(visible)) {
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
    }

    private fun renderDocuments(context: Context) {
        for (document in documents) {
            list.addView(
                DocumentSelectionCard(context, document) { onDocumentClick?.invoke(document) },
                LayoutParams(LayoutParams.MATCH_PARENT, context.resources.getDimensionPixelSize(R.dimen.kb_chip_height)),
            )
        }
    }

    private fun renderQuickReplies(context: Context) {
        for (reply in replies) {
            list.addView(quickReplyRow(context, reply))
        }
    }

    /** Booking no tiene UI real todavia (se construye en un paso posterior) —
     * placeholder simple para que el segmento no quede vacio sin explicacion. */
    private fun renderBookingPlaceholder(context: Context) {
        val placeholder = TextView(context).apply {
            text = "Proximamente"
            setSingleLine(true)
            maxLines = 1
            setTextColor(ContextCompat.getColor(context, R.color.content_secondary))
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
        list.addView(placeholder)
    }

    private fun emptyState(context: Context): TextView = TextView(context).apply {
        setText(R.string.vitrina_no_results)
        setSingleLine(true)
        maxLines = 1
        setTextColor(ContextCompat.getColor(context, R.color.content_secondary))
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

    // ------------------------------------------------------------------
    // Cabecera y filas de Producto
    // ------------------------------------------------------------------

    private fun header(context: Context): LinearLayout {
        val title = TextView(context).apply {
            setText(R.string.vitrina_title)
            setSingleLine(true)
            maxLines = 1
            gravity = Gravity.CENTER_VERTICAL
            isClickable = true
            isFocusable = true
            setTextColor(ContextCompat.getColor(context, R.color.content_primary))
            setTextSize(
                android.util.TypedValue.COMPLEX_UNIT_PX,
                context.resources.getDimension(R.dimen.type_body_regular_size),
            )
            // "tocar fuera" de la lupa: el titulo devuelve el listado.
            setOnClickListener { if (searchFocused) blurSearch() }
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

    private fun buildSearchRow(context: Context): LinearLayout {
        searchField.apply {
            gravity = Gravity.CENTER_VERTICAL
            setSingleLine(true)
            maxLines = 1
            ellipsize = android.text.TextUtils.TruncateAt.END
            isClickable = true
            isFocusable = true
            contentDescription = context.getString(R.string.vitrina_search_hint)
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
            background = fieldBackground(context)
            setOnClickListener { toggleSearch() }
        }
        searchClear.apply {
            text = "✕"
            gravity = Gravity.CENTER
            isClickable = true
            isFocusable = true
            contentDescription = context.getString(R.string.vitrina_search_clear_cd)
            setTextColor(ContextCompat.getColor(context, R.color.content_secondary))
            setTextSize(
                android.util.TypedValue.COMPLEX_UNIT_PX,
                context.resources.getDimension(R.dimen.type_body_small_size),
            )
            setOnClickListener { clearSearch() }
        }
        documentsButton.apply {
            text = context.getString(R.string.vitrina_documents)
            gravity = Gravity.CENTER
            setSingleLine(true)
            maxLines = 1
            isClickable = true
            isFocusable = true
            contentDescription = context.getString(R.string.vitrina_documents)
            setTextColor(ContextCompat.getColor(context, R.color.content_primary))
            setTextSize(
                android.util.TypedValue.COMPLEX_UNIT_PX,
                context.resources.getDimension(R.dimen.type_supporting_label_size),
            )
            setPadding(
                context.resources.getDimensionPixelSize(R.dimen.kb_bar_pad_h),
                0,
                context.resources.getDimensionPixelSize(R.dimen.kb_bar_pad_h),
                0,
            )
            background = chipBackground(context)
            setOnClickListener { openDocuments() }
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
            val gap = context.resources.getDimensionPixelSize(R.dimen.kb_bar_gap)
            addView(searchField, LayoutParams(0, LayoutParams.MATCH_PARENT, 1f))
            addView(searchClear, LayoutParams(
                context.resources.getDimensionPixelSize(R.dimen.size_touch_min),
                LayoutParams.MATCH_PARENT,
            ).apply { marginStart = gap })
            addView(documentsButton, LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.MATCH_PARENT).apply { marginStart = gap })
        }
    }

    private fun buildBackRow(context: Context): LinearLayout {
        val back = TextView(context).apply {
            text = "←"
            gravity = Gravity.CENTER
            isClickable = true
            isFocusable = true
            contentDescription = context.getString(R.string.vitrina_documents_back)
            setTextColor(ContextCompat.getColor(context, R.color.content_primary))
            setTextSize(
                android.util.TypedValue.COMPLEX_UNIT_PX,
                context.resources.getDimension(R.dimen.type_body_large_size),
            )
            val touch = context.resources.getDimensionPixelSize(R.dimen.size_touch_min)
            layoutParams = LayoutParams(touch, LayoutParams.MATCH_PARENT)
            setOnClickListener {
                showingDocuments = false
                applyVisibility()
            }
        }
        val title = TextView(context).apply {
            setText(R.string.vitrina_documents)
            gravity = Gravity.CENTER_VERTICAL
            setSingleLine(true)
            maxLines = 1
            setTextColor(ContextCompat.getColor(context, R.color.content_primary))
            setTextSize(
                android.util.TypedValue.COMPLEX_UNIT_PX,
                context.resources.getDimension(R.dimen.type_body_regular_size),
            )
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
            addView(back)
            addView(title, LayoutParams(0, LayoutParams.WRAP_CONTENT, 1f))
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

    private fun fieldBackground(context: Context): GradientDrawable {
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

    private fun chipBackground(context: Context): GradientDrawable {
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
