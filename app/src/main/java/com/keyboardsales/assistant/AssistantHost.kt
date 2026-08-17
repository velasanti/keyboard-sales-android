package com.keyboardsales.assistant

import android.view.Gravity
import android.view.View
import android.widget.FrameLayout
import android.widget.LinearLayout
import androidx.core.content.ContextCompat
import com.keyboardsales.ime.SalesIME
import com.keyboardsales.vitrina.VitrinaHost
import helium314.keyboard.latin.R
import helium314.keyboard.latin.utils.Log

/**
 * Punto de contacto entre [SalesIME] y la capa ✨ (asistente conversacional).
 *
 * Escritura aditiva sobre la jerarquia de upstream, mismo patron que
 * [VitrinaHost]: contenedores encontrados por id y superficies propias
 * agregadas en runtime. Ningun archivo de upstream se modifica.
 *
 * Paso 1 (shell): alterna la franja superior entre las sugerencias de
 * autocompletado y la capa ✨ (historial + cuadro propio), y aloja la barra de
 * anclas compartida [☰][✨] (04.4 §3) sobre el area del teclado.
 *
 * La convivencia con los chips de Vitrina (dueño de superficie) es el Paso 7;
 * aca la capa solo alterna contra el autocompletado.
 */
class AssistantHost(
    private val ime: SalesIME,
    private val vitrinaHost: VitrinaHost,
) {

    private var stripContainer: FrameLayout? = null
    private var suggestionStripView: View? = null
    private var keyboardViewWrapper: android.view.ViewGroup? = null

    private var layerView: AssistantLayerView? = null
    private var layerActive = false

    fun onInputView(view: View) {
        stripContainer = view.findViewById<FrameLayout>(R.id.strip_container)
        suggestionStripView = stripContainer?.findViewById(R.id.suggestion_strip_view)
        keyboardViewWrapper = view.findViewById<android.view.ViewGroup>(R.id.keyboard_view_wrapper)
        Log.d(TAG, "onInputView: strip=${stripContainer != null}, wrapper=${keyboardViewWrapper != null}")
        injectLayer()
        injectAnchorBar()
    }

    private fun injectLayer() {
        val container = stripContainer ?: return
        val layer = AssistantLayerView(container.context).apply {
            visibility = View.GONE
            onClose = { hideLayer() }
        }
        layerView = layer
        container.addView(
            layer,
            FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT,
            ),
        )
    }

    /**
     * Barra de anclas compartida [☰][✨] (04.4 §3: ☰ primero a la izquierda,
     * ✨ a su derecha). El ☰ alterna Vitrina modo; el ✨ alterna la capa.
     * Ubicacion flotante: misma esquina que tenia el ancla de Vitrina
     * (SUPUESTO — NO VERIFICADO, pendiente de 04.10).
     */
    private fun injectAnchorBar() {
        val wrapper = keyboardViewWrapper ?: return

        val catalogAnchor = android.widget.TextView(wrapper.context).apply {
            text = "☰"
            gravity = Gravity.CENTER
            isClickable = true
            isFocusable = true
            setSingleLine(true)
            maxLines = 1
            setTextColor(ContextCompat.getColor(wrapper.context, R.color.content_primary))
            setTextSize(
                android.util.TypedValue.COMPLEX_UNIT_PX,
                wrapper.context.resources.getDimension(R.dimen.type_body_large_size),
            )
            contentDescription = wrapper.context.getString(R.string.vitrina_anchor_open)
            setOnClickListener { vitrinaHost.togglePanel() }
        }
        catalogAnchor.background = anchorBackground(wrapper.context)

        val assistantAnchor = AssistantAnchorView(wrapper.context).apply {
            contentDescription = wrapper.context.getString(R.string.assistant_anchor_open)
            setOnClickListener { toggleLayer() }
        }

        val bar = LinearLayout(wrapper.context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            val gap = resources.getDimensionPixelSize(R.dimen.kb_bar_gap)
            setPadding(0, 0, gap, 0)
            addView(catalogAnchor)
            addView(assistantAnchor)
        }

        val pad = wrapper.context.resources.getDimensionPixelSize(R.dimen.kb_bar_pad_h)
        val lp = FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.WRAP_CONTENT,
            FrameLayout.LayoutParams.WRAP_CONTENT,
            Gravity.TOP or Gravity.START,
        ).apply {
            marginStart = pad
            topMargin = pad
        }
        wrapper.addView(bar, lp)
    }

    private fun anchorBackground(context: android.content.Context): android.graphics.drawable.GradientDrawable {
        val resources = context.resources
        val radius = resources.getDimension(R.dimen.radius_pill)
        val stroke = resources.getDimension(R.dimen.border_width_hairline).toInt()
        return android.graphics.drawable.GradientDrawable().apply {
            shape = android.graphics.drawable.GradientDrawable.RECTANGLE
            cornerRadius = radius
            setColor(ContextCompat.getColor(context, R.color.surface_raised))
            setStroke(stroke, ContextCompat.getColor(context, R.color.border_subtle))
        }
    }

    // ------------------------------------------------------------------
    // Alternancia de la capa sobre la franja superior
    // ------------------------------------------------------------------

    private fun toggleLayer() {
        if (layerActive) hideLayer() else showLayer()
    }

    private fun showLayer() {
        val container = stripContainer ?: return
        val suggestion = suggestionStripView ?: return
        val layer = layerView ?: return
        layerActive = true

        container.layoutParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            container.resources.getDimensionPixelSize(R.dimen.kb_bar_height_expanded),
        )
        suggestion.layoutParams = FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.MATCH_PARENT,
            container.resources.getDimensionPixelSize(R.dimen.kb_bar_height),
            Gravity.TOP,
        )
        suggestion.visibility = View.GONE
        layer.visibility = View.VISIBLE
    }

    private fun hideLayer() {
        if (!layerActive) return
        layerActive = false
        val container = stripContainer ?: return
        val suggestion = suggestionStripView ?: return
        val layer = layerView ?: return

        container.layoutParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            container.resources.getDimensionPixelSize(R.dimen.config_suggestions_strip_height),
        )
        suggestion.layoutParams = FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.MATCH_PARENT,
            FrameLayout.LayoutParams.MATCH_PARENT,
        )
        suggestion.visibility = View.VISIBLE
        layer.visibility = View.GONE
    }

    companion object {
        private const val TAG = "Assistant"
    }
}
