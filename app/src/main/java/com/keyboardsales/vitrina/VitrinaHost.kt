package com.keyboardsales.vitrina

import android.graphics.Rect
import android.os.Handler
import android.os.Looper
import android.view.Gravity
import android.view.View
import android.view.inputmethod.EditorInfo
import android.widget.FrameLayout
import android.widget.LinearLayout
import com.keyboardsales.ime.SalesIME
import com.keyboardsales.vitrina.bar.BarLayout
import com.keyboardsales.vitrina.bar.BarMode
import com.keyboardsales.vitrina.bar.DummyCatalogBadgeView
import com.keyboardsales.vitrina.bar.VitrinaBarView
import com.keyboardsales.vitrina.data.CatalogItem
import com.keyboardsales.vitrina.data.CatalogRepository
import com.keyboardsales.vitrina.data.MessageVariant
import com.keyboardsales.vitrina.data.QuickReply
import com.keyboardsales.vitrina.insert.InsertController
import com.keyboardsales.vitrina.insert.MessageBuilder
import com.keyboardsales.vitrina.panel.VitrinaAnchorView
import com.keyboardsales.vitrina.panel.VitrinaPanelView
import com.keyboardsales.vitrina.search.CatalogMatcher
import com.keyboardsales.vitrina.search.TriggerDetector
import com.keyboardsales.vitrina.search.VitrinaTrigger
import helium314.keyboard.latin.R
import helium314.keyboard.latin.utils.Log
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

/**
 * Punto unico de contacto entre [SalesIME] y las superficies de Vitrina.
 *
 * Escritura aditiva sobre la jerarquia de upstream: contenedores encontrados
 * por id y superficies propias agregadas en runtime. Ningun archivo de upstream
 * se modifica salvo AndroidManifest.xml.
 *
 * Maquina de estados de la capa:
 *   IDLE -> SEARCHING (trigger # o /) -> CONFIRM (ADR-016) -> insert -> UNDO
 *   UNDO -> (Deshacer | timeout | nuevo trigger)
 */
class VitrinaHost(private val ime: SalesIME) {

    private var stripContainer: FrameLayout? = null
    private var keyboardViewWrapper: android.view.ViewGroup? = null

    private var catalogIsDummy = false
    private var badgeAdded = false

    private var repository: CatalogRepository? = null
    private var barView: VitrinaBarView? = null
    private var suggestionStripView: View? = null
    private var keyboardView: View? = null
    private var anchorView: VitrinaAnchorView? = null
    private var panelView: VitrinaPanelView? = null
    private var panelVisible = false
    private var barActive = false
    private var pendingMessage: String? = null
    private var pendingDeleteLength = 0
    private var insertedLength = 0
    private var undoShowing = false
    private var undoRunnable: Runnable? = null

    private val searchExecutor: ExecutorService = Executors.newSingleThreadExecutor()
    private val mainHandler = Handler(Looper.getMainLooper())
    private val windowRect = Rect()
    private val maxReadAheadChars = 256

    fun onInputView(view: View) {
        stripContainer = view.findViewById<FrameLayout>(R.id.strip_container)
        keyboardViewWrapper = view.findViewById<android.view.ViewGroup>(R.id.keyboard_view_wrapper)
        keyboardView = view.findViewById(R.id.keyboard_view)
        Log.d(TAG, "onInputView: strip=${stripContainer != null}, wrapper=${keyboardViewWrapper != null}")
        if (stripContainer != null) {
            suggestionStripView = stripContainer?.findViewById(R.id.suggestion_strip_view)
            val bar = VitrinaBarView(view.context)
            wireBarCallbacks(bar)
            barView = bar
            stripContainer?.addView(
                bar,
                FrameLayout.LayoutParams(
                    FrameLayout.LayoutParams.MATCH_PARENT,
                    FrameLayout.LayoutParams.MATCH_PARENT,
                ),
            )
        }
        injectPanel(view)
        renderBadge()
    }

    /**
     * Vitrina modo: el ancla y el panel como hijos de keyboard_view_wrapper,
     * con el patron de emoji_palettes_view (hermanos del QWERTY que se
     * alternan por visibilidad).
     */
    private fun injectPanel(view: View) {
        val wrapper = keyboardViewWrapper ?: return
        val panel = VitrinaPanelView(view.context)
        panel.visibility = View.GONE
        panel.layoutParams = FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.MATCH_PARENT,
            view.resources.getDimensionPixelSize(R.dimen.kb_panel_height),
            Gravity.TOP,
        )
        wirePanelCallbacks(panel)
        panelView = panel
        wrapper.addView(panel)

        val anchor = VitrinaAnchorView(view.context)
        anchor.contentDescription = view.context.getString(R.string.vitrina_anchor_open)
        // Ancla en fila superior, lado izquierdo, junto a ✨ (04.4 §3, 04.10 §2)
        // Se superpone al QWERTY cuando Vitrina modo está activo.
        // Visible solo cuando el modo está activo (visibility GONE cuando no).
        val lp = FrameLayout.LayoutParams(
            view.context.resources.getDimensionPixelSize(R.dimen.kb_anchor_size),
            view.context.resources.getDimensionPixelSize(R.dimen.kb_anchor_size),
            Gravity.TOP or Gravity.START,
        ).apply {
            marginStart = view.context.resources.getDimensionPixelSize(R.dimen.kb_bar_pad_h)
            topMargin = view.context.resources.getDimensionPixelSize(R.dimen.kb_bar_pad_h)
        }
        anchor.setOnClickListener { togglePanel() }
        anchorView = anchor
        wrapper.addView(anchor, lp)
    }

    fun onStartInputView(editorInfo: EditorInfo?, restarting: Boolean) {
        Log.d(TAG, "onStartInputView(restarting=$restarting)")
        resetToIdle()
    }

    fun onFinishInputView(finishingInput: Boolean) {
        Log.d(TAG, "onFinishInputView(finishingInput=$finishingInput)")
        resetToIdle()
    }

    fun onRepositoryReady(repository: CatalogRepository) {
        this.repository = repository
        Log.d(TAG, "onRepositoryReady")
    }

    fun onCatalogLoaded(esDummy: Boolean) {
        catalogIsDummy = esDummy
        Log.d(TAG, "onCatalogLoaded(esDummy=$esDummy)")
        renderBadge()
    }

    fun onUpdateSelection(
        oldSelStart: Int,
        oldSelEnd: Int,
        newSelStart: Int,
        newSelEnd: Int,
        composingSpanStart: Int,
        composingSpanEnd: Int,
    ) {
        if (panelVisible) return
        val textBeforeCursor = ime.getCurrentInputConnection()
            ?.getTextBeforeCursor(maxReadAheadChars, 0)
            ?.toString()
            ?: return
        when (val trigger = TriggerDetector.detect(textBeforeCursor)) {
            is VitrinaTrigger.None -> {
                // Si estamos en UNDO, el texto cambio al insertar y no hay trigger:
                // se mantiene el Deshacer hasta el timeout.
                if (!undoShowing) hideBar()
            }
            is VitrinaTrigger.Product -> startSearch(textBeforeCursor, trigger.query, product = true)
            is VitrinaTrigger.QuickReply -> startSearch(textBeforeCursor, trigger.query, product = false)
        }
    }

    // ------------------------------------------------------------------
    // Busqueda (off del hilo de UI)
    // ------------------------------------------------------------------

    private fun startSearch(textBeforeCursor: String, query: String, product: Boolean) {
        cancelPendingUndo()
        val repository = this.repository ?: return
        searchExecutor.execute {
            val matches: List<Any> = if (product) {
                CatalogMatcher.matchItems(repository.allItems(), query)
            } else {
                CatalogMatcher.matchQuickReplies(repository.allQuickReplies(), query)
            }
            mainHandler.post {
                if (matches.isEmpty()) {
                    barView?.clearChips()
                    return@post
                }
                showBar()
                if (product) {
                    @Suppress("UNCHECKED_CAST")
                    barView?.showProducts(matches as List<CatalogItem>)
                } else {
                    @Suppress("UNCHECKED_CAST")
                    barView?.showQuickReplies(matches as List<QuickReply>)
                }
            }
        }
    }

    // ------------------------------------------------------------------
    // Confirmacion (ADR-016) e insercion
    // ------------------------------------------------------------------

    private fun wireBarCallbacks(bar: VitrinaBarView) {
        bar.onProductClick = product@{ item ->
            val text = readTextBeforeCursor() ?: return@product
            val trigger = TriggerDetector.detect(text)
            if (trigger !is VitrinaTrigger.Product) return@product
            val message = MessageBuilder.productMessage(item)
            if (message.isEmpty()) return@product
            pendingMessage = message
            pendingDeleteLength = TriggerDetector.deleteLength(text, trigger)
            barView?.showConfirm(message)
        }
        bar.onQuickReplyClick = reply@{ reply ->
            val text = readTextBeforeCursor() ?: return@reply
            val trigger = TriggerDetector.detect(text)
            if (trigger !is VitrinaTrigger.QuickReply) return@reply
            val message = MessageBuilder.quickReplyMessage(reply)
            if (message.isEmpty()) return@reply
            pendingMessage = message
            pendingDeleteLength = TriggerDetector.deleteLength(text, trigger)
            barView?.showConfirm(message)
        }
        bar.onConfirm = { performInsert() }
        bar.onCancel = { barView?.showChips() }
        bar.onUndo = { performUndo() }
    }

    private fun readTextBeforeCursor(): String? =
        ime.getCurrentInputConnection()?.getTextBeforeCursor(maxReadAheadChars, 0)?.toString()

    // ------------------------------------------------------------------
    // Vitrina modo: panel y ancla
    // ------------------------------------------------------------------

    private fun wirePanelCallbacks(panel: VitrinaPanelView) {
        panel.onProductClick = product@{ item ->
            val message = MessageBuilder.productMessage(item)
            if (message.isEmpty()) return@product
            pendingMessage = message
            pendingDeleteLength = 0
            barView?.showConfirm(message)
            showBar()
        }
        panel.onQuickReplyClick = reply@{ reply ->
            val message = MessageBuilder.quickReplyMessage(reply)
            if (message.isEmpty()) return@reply
            pendingMessage = message
            pendingDeleteLength = 0
            barView?.showConfirm(message)
            showBar()
        }
        panel.onVariantClick = variant@{ item, variant ->
            val message = MessageBuilder.variantMessage(item, variant)
            if (message.isEmpty()) return@variant
            pendingMessage = message
            pendingDeleteLength = 0
            barView?.showConfirm(message)
            showBar()
        }
        panel.onClose = { hidePanel() }

        // El cambio de segmento (Producto/Booking/Respuestas rapidas) lo maneja
        // VitrinaPanelView internamente sobre su propio segmentSwitch -- no se
        // cablea de nuevo aca para no pisar esos listeners.
    }

    private fun showPanel() {
        val panel = panelView ?: return
        val keyboard = keyboardView ?: return
        val repository = this.repository ?: return
        hideBar()
        panel.visibility = View.VISIBLE
        keyboard.visibility = View.GONE
        panelVisible = true
        anchorView?.contentDescription = ime.getString(R.string.vitrina_anchor_close)
        searchExecutor.execute {
            val products = repository.allItems()
            val replies = repository.allQuickReplies()
            val variants = repository.allMessageVariants()
            mainHandler.post { panel.populate(products, replies, variants) }
        }
    }

    private fun hidePanel() {
        if (!panelVisible) return
        panelVisible = false
        panelView?.visibility = View.GONE
        keyboardView?.visibility = View.VISIBLE
        anchorView?.contentDescription = ime.getString(R.string.vitrina_anchor_open)
        hideBar()
    }

    /** El chip de la barra y el ancla comparten la entrada al modo (04.10). */
    private fun togglePanel() {
        when (PanelToggle.next(panelVisible)) {
            PanelAction.SHOW -> showPanel()
            PanelAction.HIDE -> hidePanel()
        }
    }

    private fun performInsert() {
        val message = pendingMessage ?: return
        val insertResult = InsertController.insert(ime, pendingDeleteLength, message)
        pendingMessage = null
        if (insertResult < 0) return
        insertedLength = insertResult
        undoShowing = true
        barView?.showUndo()
        scheduleUndoTimeout()
    }

    private fun performUndo() {
        cancelPendingUndo()
        InsertController.undo(ime, insertedLength)
        undoShowing = false
        insertedLength = 0
        hideBar()
    }

    private fun cancelPendingUndo() {
        undoRunnable?.let { mainHandler.removeCallbacks(it) }
        undoRunnable = null
        undoShowing = false
    }

    private fun scheduleUndoTimeout() {
        val runnable = Runnable { if (undoShowing) hideBar() }
        undoRunnable = runnable
        mainHandler.postDelayed(runnable, UNDO_TIMEOUT_MS)
    }

    // ------------------------------------------------------------------
    // Barra: altura, alternancia de strips
    // ------------------------------------------------------------------

    /**
     * Supuesto (04.8 sin leer): EXPANDED_ROW suma fila (sugerencias arriba,
     * superficie de Vitrina abajo); OVERLAY reemplaza la fila sin crecer.
     */
    private fun showBar() {
        val container = stripContainer ?: return
        val bar = barView ?: return
        val suggestion = suggestionStripView ?: return
        barActive = true

        when (currentBarLayout(container)) {
            BarLayout.EXPANDED_ROW -> {
                container.layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    container.resources.getDimensionPixelSize(R.dimen.kb_bar_height_expanded),
                )
                suggestion.layoutParams = FrameLayout.LayoutParams(
                    FrameLayout.LayoutParams.MATCH_PARENT,
                    container.resources.getDimensionPixelSize(R.dimen.kb_bar_height),
                    Gravity.TOP,
                )
                suggestion.visibility = View.VISIBLE
                bar.layoutParams = FrameLayout.LayoutParams(
                    FrameLayout.LayoutParams.MATCH_PARENT,
                    container.resources.getDimensionPixelSize(R.dimen.kb_bar_height),
                    Gravity.BOTTOM,
                )
            }
            BarLayout.OVERLAY_SUGGESTIONS -> {
                container.layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.MATCH_PARENT,
                )
                suggestion.visibility = View.GONE
                bar.layoutParams = FrameLayout.LayoutParams(
                    FrameLayout.LayoutParams.MATCH_PARENT,
                    FrameLayout.LayoutParams.MATCH_PARENT,
                )
            }
        }
        bar.visibility = View.VISIBLE
    }

    private fun hideBar() {
        if (!barActive) return
        barActive = false
        cancelPendingUndo()
        val container = stripContainer ?: return
        val suggestion = suggestionStripView ?: return
        val bar = barView ?: return

        val barHeight = container.resources.getDimensionPixelSize(R.dimen.config_suggestions_strip_height)
        container.layoutParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            barHeight,
        )
        suggestion.layoutParams = FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.MATCH_PARENT,
            FrameLayout.LayoutParams.MATCH_PARENT,
        )
        suggestion.visibility = View.VISIBLE
        bar.visibility = View.GONE
        barView?.clearChips()
    }

    private fun resetToIdle() {
        cancelPendingUndo()
        pendingMessage = null
        insertedLength = 0
        hidePanel()
        hideBar()
    }

    private fun currentBarLayout(container: android.view.ViewGroup): BarLayout {
        container.getWindowVisibleDisplayFrame(windowRect)
        val threshold = container.resources.getDimensionPixelSize(R.dimen.kb_bar_expanded_min_height)
        return BarMode.decide(usableHeightPx = windowRect.height(), expandedThresholdPx = threshold)
    }

    /** Aviso "Catálogo de prueba" mientras el catalogo sea el dummy. */
    private fun renderBadge() {
        val container = stripContainer ?: return
        if (badgeAdded || !catalogIsDummy) return
        val context = container.context
        val badge = DummyCatalogBadgeView(context).apply {
            isClickable = true
            isFocusable = true
            setOnClickListener { togglePanel() }
        }
        val lp = FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.WRAP_CONTENT,
            FrameLayout.LayoutParams.WRAP_CONTENT,
            Gravity.TOP or Gravity.END,
        )
        val paddingX = context.resources.getDimensionPixelSize(R.dimen.spacing_2)
        val paddingY = context.resources.getDimensionPixelSize(R.dimen.spacing_1)
        lp.marginEnd = paddingX
        lp.topMargin = paddingY
        container.addView(badge, lp)
        container.expandTouchTarget(badge, container.resources.getDimensionPixelSize(R.dimen.size_touch_min))
        badgeAdded = true
    }

    companion object {
        private const val TAG = "Vitrina"
        private const val UNDO_TIMEOUT_MS = 8_000L
    }
}