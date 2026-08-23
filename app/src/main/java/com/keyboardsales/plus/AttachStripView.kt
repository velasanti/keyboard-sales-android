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
 * La Fila 1 (decision David 2026-08-22, Log de Decisiones): fila FIJA de
 * adjuntos encima de strip_container (Fila 2), alto fijo kb/bar/height.
 * El menu reemplaza su contenido, nunca suma pixeles verticales.
 *
 *   ROOT      [+ Adjuntar] ......................... [cam] [mic]
 *   PRINCIPAL [Camara] [Fotos] [Mas >] .............. [X]
 *   MAS       [<] [Documento] [Ubicacion] [Contacto]. [X]
 *
 * Los iconos de camara y microfono de esta fila son capa visual nuestra:
 * NO controlan los que WhatsApp dibuja en su propio compositor (UI del
 * host, fuera de alcance). Hoy son decorativos (toast "prximamente");
 * no hay spec de comportamiento al toque.
 *
 * Mismas decisiones de estilo que las anclas (2026-08-17): glifos de TEXTO,
 * no vectores — el vector como foreground no pinta su relleno en este arbol
 * de vistas. Chips reutilizando VitrinaChipView (tinte accent/subtle).
 *
 * El area tactil de cada control es 48dp (regla 8) via CompositeTouchDelegate;
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
        fun onCloseMenu()
        fun onCameraIcon()
        fun onMicIcon()
    }

    var callbacks: Callbacks? = null

    /** Un ViewGroup solo soporta UN touchDelegate: con varios controles por
     *  fila hace falta el compuesto, no expandTouchTarget (que pisa). */
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
                addPlusChip()
                addCloseGlyphSpacerless()
                addRowIcon(
                    context.getString(R.string.plus_row_camera_cd),
                    CAMERA_GLYPH,
                ) { callbacks?.onCameraIcon() }
                addRowIcon(
                    context.getString(R.string.plus_row_mic_cd),
                    MIC_GLYPH,
                ) { callbacks?.onMicIcon() }
            }
            AttachPanel.PRINCIPAL -> {
                addChip(context.getString(R.string.plus_camera), context.getString(R.string.plus_camera_cd)) { callbacks?.onCamera() }
                addChip(context.getString(R.string.plus_photos), context.getString(R.string.plus_photos_cd)) { callbacks?.onPhotos() }
                addChip(
                    context.getString(R.string.plus_more) + " " + CHEVRON,
                    context.getString(R.string.plus_more_cd),
                ) { callbacks?.onMore() }
                addCloseGlyph()
            }
            AttachPanel.MAS -> {
                addGlyph(context.getString(R.string.plus_back_cd), BACK_GLYPH) { callbacks?.onBack() }
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

    /**
     * Chip del disparador + en estado de reposo. Glifo U+FF0B (plus de ancho
     * completo) como texto: mismo criterio que las anclas.
     */
    private fun addPlusChip() {
        val chip = VitrinaChipView(
            context,
            PLUS_GLYPH + " " + context.getString(R.string.plus_attach),
            context.getString(R.string.plus_attach_cd),
            accent = true,
        )
        chip.setOnClickListener { callbacks?.onPlus() }
        addView(chip)
        expandToMinTouch(chip)
    }

    /** Icono decorativo de la Fila 1 (glifo de texto con pildora, sin fondo blanco). */
    private fun addRowIcon(description: String, glyph: String, onClick: () -> Unit) {
        val resources = context.resources
        val icon = TextView(context).apply {
            text = glyph
            contentDescription = description
            gravity = Gravity.CENTER
            isClickable = true
            isFocusable = true
            setSingleLine(true)
            maxLines = 1
            setTextColor(ContextCompat.getColor(context, R.color.content_secondary))
            setTextSize(
                android.util.TypedValue.COMPLEX_UNIT_PX,
                resources.getDimension(R.dimen.type_body_large_size),
            )
            layoutParams = MarginLayoutParams(
                resources.getDimensionPixelSize(R.dimen.kb_chip_height),
                resources.getDimensionPixelSize(R.dimen.kb_chip_height),
            ).apply {
                marginStart = resources.getDimensionPixelSize(R.dimen.kb_bar_gap)
            }
            setOnClickListener { onClick() }
        }
        addView(icon)
        expandToMinTouch(icon)
    }

    /** Glifo de texto con pildora blanca, misma anatomia que las anclas. */
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

    private fun addSpacer() {
        addView(Space(context), LayoutParams(0, 1, 1f))
    }

    /** El X siempre al final, pegado al borde derecho: cierra el MENU (vuelve a ROOT), no oculta la fila. */
    private fun addCloseGlyph() {
        addSpacer()
        addGlyph(context.getString(R.string.plus_close_menu_cd), CLOSE_GLYPH) { callbacks?.onCloseMenu() }
    }

    /** En ROOT el X no existe; los iconos de la derecha van pegados al borde. */
    private fun addCloseGlyphSpacerless() {
        addSpacer()
    }

    private fun expandToMinTouch(view: View) {
        touchTargets.add(view, resources.getDimensionPixelSize(R.dimen.size_touch_min))
    }

    private companion object {
        const val PLUS_GLYPH = "\uFF0B"
        const val CHEVRON = "\u203A"
        const val BACK_GLYPH = "\u2039"
        const val CLOSE_GLYPH = "\u2715"
        const val CAMERA_GLYPH = "\uD83D\uDCF7"
        const val MIC_GLYPH = "\uD83C\uDFA4"
    }
}
