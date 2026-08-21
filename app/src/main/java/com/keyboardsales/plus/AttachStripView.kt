package com.keyboardsales.plus

import android.content.Context
import android.graphics.drawable.GradientDrawable
import android.view.Gravity
import android.view.View
import android.widget.LinearLayout
import android.widget.Space
import android.widget.TextView
import androidx.core.content.ContextCompat
import com.keyboardsales.vitrina.bar.VitrinaChipView
import com.keyboardsales.vitrina.CompositeTouchDelegate
import helium314.keyboard.latin.R

/**
 * La Franja 1: la fila de adjuntos que vive ENCIMA de strip_container
 * (Franja 2, intocable). Alto kb/bar/height (48dp), fondo surface_bar.
 *
 * Renderiza segun [AttachPanel]:
 *   ROOT      [＋ Adjuntar] .......................... [✕]
 *   PRINCIPAL [Cámara] [Fotos] [Más ›] .............. [✕]
 *   MAS       [‹] [Documento] [Ubicación] [Contacto]. [✕]
 *
 * Mismas decisiones de estilo que las anclas ☰/✦ (2026-08-17): glifos de
 * TEXTO, no vectores — el vector como foreground no pinta su relleno en
 * este arbol de vistas. Los chips reutilizan VitrinaChipView (tinte
 * accent/subtle, que NO cuenta contra el tope de dos acentos saturados).
 *
 * El area tactil de cada control es 48dp (regla 8) via expandTouchTarget;
 * el tamano visual queda en 36dp (kb/chip/height).
 */
class AttachStripView(context: Context) : LinearLayout(context) {

    interface Callbacks {
        fun onPlus()
        fun onCamera()
        fun onPhotos()
        fun onMore()
        fun onDocument()
        fun onLocation()
        fun onContact()
        fun onBack()
        fun onDismiss()
    }

    var callbacks: Callbacks? = null

    /**
     * Un ViewGroup solo soporta UN touchDelegate: con varios controles por
     * fila hace falta el compuesto, no expandTouchTarget (que pisa).
     */
    private val touchTargets = CompositeTouchDelegate(this)

    init {
        orientation = HORIZONTAL
        gravity = Gravity.CENTER_VERTICAL
        val padH = context.resources.getDimensionPixelSize(R.dimen.kb_bar_pad_h)
        setPadding(padH, 0, padH, 0)
        background = GradientDrawable().apply {
            setColor(ContextCompat.getColor(context, R.color.surface_bar))
        }
    }

    /** Reconstruye los hijos para el estado dado. Pocas vistas: reconstruir es barato. */
    fun render(panel: AttachPanel) {
        removeAllViews()
        touchTargets.clear()
        when (panel) {
            AttachPanel.HIDDEN -> return
            AttachPanel.ROOT -> {
                addChip(context.getString(R.string.plus_attach), context.getString(R.string.plus_attach_cd)) { callbacks?.onPlus() }
                addCloseGlyph()
            }
            AttachPanel.PRINCIPAL -> {
                addChip(context.getString(R.string.plus_camera), context.getString(R.string.plus_camera_cd)) { callbacks?.onCamera() }
                addChip(context.getString(R.string.plus_photos), context.getString(R.string.plus_photos_cd)) { callbacks?.onPhotos() }
                addChip(
                    "${context.getString(R.string.plus_more)} ›",
                    context.getString(R.string.plus_more_cd),
                ) { callbacks?.onMore() }
                addCloseGlyph()
            }
            AttachPanel.MAS -> {
                addGlyph(context.getString(R.string.plus_back_cd), "‹") { callbacks?.onBack() }
                addChip(context.getString(R.string.plus_document), context.getString(R.string.plus_document_cd)) { callbacks?.onDocument() }
                addChip(context.getString(R.string.plus_location), context.getString(R.string.plus_location_cd)) { callbacks?.onLocation() }
                addChip(context.getString(R.string.plus_contact), context.getString(R.string.plus_contact_cd)) { callbacks?.onContact() }
                addCloseGlyph()
            }
        }
    }

    private fun addChip(label: String, description: String, onClick: () -> Unit) {
        val chip = VitrinaChipView(context, label, description, accent = true)
        chip.setOnClickListener { onClick() }
        addView(chip)
        expandToMinTouch(chip)
    }

    /** Glifo de texto con pildora, misma anatomia que las anclas ☰/✦. */
    private fun addGlyph(description: String, glyph: String, onClick: () -> Unit) {
        val resources = context.resources
        val button = TextView(context).apply {
            text = glyph
            contentDescription = description
            gravity = Gravity.CENTER
            isClickable = true
            isFocusable = true
            setSingleLine(true)
            maxLines = 1
            setTextColor(ContextCompat.getColor(context, R.color.content_primary))
            setTextSize(
                android.util.TypedValue.COMPLEX_UNIT_PX,
                resources.getDimension(R.dimen.type_body_large_size),
            )
            background = GradientDrawable().apply {
                cornerRadius = resources.getDimension(R.dimen.radius_pill)
                setColor(ContextCompat.getColor(context, R.color.surface_raised))
                setStroke(
                    resources.getDimension(R.dimen.border_width_hairline).toInt(),
                    ContextCompat.getColor(context, R.color.border_subtle),
                )
            }
            layoutParams = MarginLayoutParams(
                resources.getDimensionPixelSize(R.dimen.kb_chip_height),
                resources.getDimensionPixelSize(R.dimen.kb_chip_height),
            ).apply {
                marginStart = resources.getDimensionPixelSize(R.dimen.kb_bar_gap)
            }
            setOnClickListener { onClick() }
        }
        addView(button)
        expandToMinTouch(button)
    }

    /** Espacio flexible entre el contenido y el ✕, sin pesos magicos. */
    private fun addSpacer() {
        addView(Space(context), LayoutParams(0, 1, 1f))
    }

    /** El ✕ siempre va al final de la fila, pegado al borde derecho. */
    private fun addCloseGlyph() {
        addSpacer()
        addGlyph(context.getString(R.string.plus_close_cd), "✕") { callbacks?.onDismiss() }
    }

    private fun expandToMinTouch(view: View) {
        touchTargets.add(view, resources.getDimensionPixelSize(R.dimen.size_touch_min))
    }
}
