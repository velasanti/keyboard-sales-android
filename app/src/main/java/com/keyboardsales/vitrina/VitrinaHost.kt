package com.keyboardsales.vitrina

import android.graphics.Rect
import android.os.Handler
import android.os.Looper
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.widget.FrameLayout
import com.keyboardsales.ime.SalesIME
import com.keyboardsales.vitrina.bar.BarLayout
import com.keyboardsales.vitrina.bar.BarMode
import com.keyboardsales.vitrina.bar.DummyCatalogBadgeView
import com.keyboardsales.vitrina.bar.VitrinaBarView
import com.keyboardsales.vitrina.data.CatalogItem
import com.keyboardsales.vitrina.data.CatalogRepository
import com.keyboardsales.vitrina.data.QuickReply
import com.keyboardsales.vitrina.search.CatalogMatcher
import com.keyboardsales.vitrina.search.TriggerDetector
import com.keyboardsales.vitrina.search.VitrinaTrigger
import helium314.keyboard.latin.R
import helium314.keyboard.latin.utils.Log
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

/**
 * Punto unico de contacto entre [SalesIME] y las dos superficies de Vitrina.
 *
 * Toda la escritura a la jerarquia de vistas es aditiva: aca se encuentran por
 * id los contenedores que upstream ya infla (leer ids no es editar) y las
 * superficies propias se agregan como hijos en runtime. Ningun archivo de
 * upstream se modifica salvo AndroidManifest.xml.
 */
class VitrinaHost(private val ime: SalesIME) {

    private var stripContainer: FrameLayout? = null
    private var keyboardViewWrapper: View? = null

    private var catalogIsDummy = false
    private var badgeAdded = false

    private var repository: CatalogRepository? = null
    private var barView: VitrinaBarView? = null
    private var suggestionStripView: View? = null
    private var barActive = false

    private val searchExecutor: ExecutorService = Executors.newSingleThreadExecutor()
    private val mainHandler = Handler(Looper.getMainLooper())
    private val windowRect = Rect()
    private val maxReadAheadChars = 256

    fun onInputView(view: View) {
        stripContainer = view.findViewById<FrameLayout>(R.id.strip_container)
        keyboardViewWrapper = view.findViewById(R.id.keyboard_view_wrapper)
        Log.d(TAG, "onInputView: strip=${stripContainer != null}, wrapper=${keyboardViewWrapper != null}")
        if (stripContainer != null) {
            suggestionStripView = stripContainer?.findViewById(R.id.suggestion_strip_view)
            barView = VitrinaBarView(view.context)
            stripContainer?.addView(
                barView,
                FrameLayout.LayoutParams(
                    FrameLayout.LayoutParams.MATCH_PARENT,
                    FrameLayout.LayoutParams.MATCH_PARENT,
                ),
            )
        }
        renderBadge()
    }

    fun onStartInputView(editorInfo: EditorInfo?, restarting: Boolean) {
        Log.d(TAG, "onStartInputView(restarting=$restarting)")
        hideBar()
    }

    fun onFinishInputView(finishingInput: Boolean) {
        Log.d(TAG, "onFinishInputView(finishingInput=$finishingInput)")
        hideBar()
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
        val textBeforeCursor = ime.getCurrentInputConnection()
            ?.getTextBeforeCursor(maxReadAheadChars, 0)
            ?.toString()
            ?: return
        when (val trigger = TriggerDetector.detect(textBeforeCursor)) {
            is VitrinaTrigger.None -> hideBar()
            is VitrinaTrigger.Product -> startSearch(trigger.query, product = true)
            is VitrinaTrigger.QuickReply -> startSearch(trigger.query, product = false)
        }
    }

    private fun startSearch(query: String, product: Boolean) {
        val repository = this.repository ?: return
        searchExecutor.execute {
            val matches: List<Any> = if (product) {
                CatalogMatcher.matchItems(repository.allItems(), query)
            } else {
                CatalogMatcher.matchQuickReplies(repository.allQuickReplies(), query)
            }
            mainHandler.post {
                showBar()
                if (matches.isEmpty()) {
                    barView?.clear()
                    return@post
                }
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

    /**
     * Activa Vitrina capa: los chips reemplazan la fila de sugerencias.
     *
     * Supuesto (04.8 sin leer): en la rama EXPANDED_ROW la sugerencia queda
     * arriba (48dp) y los chips abajo (48dp) = "suma fila"; en OVERLAY los chips
     * reemplazan la fila entera sin crecer el contenedor. A verificar en A51.
     */
    private fun showBar() {
        val container = stripContainer ?: return
        val bar = barView ?: return
        val suggestion = suggestionStripView ?: return
        barActive = true

        val layout = currentBarLayout(container)
        when (layout) {
            BarLayout.EXPANDED_ROW -> {
                container.layoutParams = FrameLayout.LayoutParams(
                    FrameLayout.LayoutParams.MATCH_PARENT,
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
                container.layoutParams = FrameLayout.LayoutParams(
                    FrameLayout.LayoutParams.MATCH_PARENT,
                    FrameLayout.LayoutParams.MATCH_PARENT,
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

    /** Restaura strip_container y la fila de sugerencias al estado de upstream. */
    private fun hideBar() {
        if (!barActive) return
        barActive = false
        val container = stripContainer ?: return
        val suggestion = suggestionStripView ?: return
        val bar = barView ?: return

        val barHeight = container.resources.getDimensionPixelSize(R.dimen.config_suggestions_strip_height)
        container.layoutParams = FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.MATCH_PARENT,
            barHeight,
        )
        suggestion.layoutParams = FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.MATCH_PARENT,
            FrameLayout.LayoutParams.MATCH_PARENT,
        )
        suggestion.visibility = View.VISIBLE
        bar.visibility = View.GONE
    }

    private fun currentBarLayout(container: ViewGroup): BarLayout {
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
            isClickable = false
            isFocusable = false
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
        badgeAdded = true
    }

    companion object {
        private const val TAG = "Vitrina"
    }
}