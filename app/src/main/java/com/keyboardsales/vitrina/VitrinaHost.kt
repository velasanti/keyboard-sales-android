package com.keyboardsales.vitrina

import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.widget.FrameLayout
import com.keyboardsales.ime.SalesIME
import com.keyboardsales.vitrina.bar.DummyCatalogBadgeView
import helium314.keyboard.latin.R
import helium314.keyboard.latin.utils.Log

/**
 * Punto unico de contacto entre [SalesIME] y las dos superficies de Vitrina.
 *
 * Toda la escritura a la jerarquia de vistas es aditiva: aca se encuentran por
 * id los contenedores que upstream ya infla (leer ids no es editar) y las
 * superficies propias se agregan como hijos en runtime. Ningun archivo de
 * upstream se modifica salvo AndroidManifest.xml.
 */
class VitrinaHost(private val ime: SalesIME) {

    private var stripContainer: ViewGroup? = null
    private var keyboardViewWrapper: View? = null

    private var catalogIsDummy = false
    private var badgeAdded = false

    fun onInputView(view: View) {
        stripContainer = view.findViewById<ViewGroup>(R.id.strip_container)
        keyboardViewWrapper = view.findViewById(R.id.keyboard_view_wrapper)
        Log.d(TAG, "onInputView: strip=${stripContainer != null}, wrapper=${keyboardViewWrapper != null}")
        renderBadge()
    }

    fun onStartInputView(editorInfo: EditorInfo?, restarting: Boolean) {
        Log.d(TAG, "onStartInputView(restarting=$restarting)")
    }

    fun onFinishInputView(finishingInput: Boolean) {
        Log.d(TAG, "onFinishInputView(finishingInput=$finishingInput)")
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
        Log.d(
            TAG,
            "onUpdateSelection: nss=$newSelStart nse=$newSelEnd cs=$composingSpanStart ce=$composingSpanEnd",
        )
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