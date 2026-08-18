package com.keyboardsales.vitrina.switch

import android.content.Context
import android.util.AttributeSet
import android.view.Gravity
import android.view.ViewGroup
import android.widget.Button
import android.widget.FrameLayout
import android.widget.HorizontalScrollView
import android.widget.LinearLayout
import androidx.core.content.ContextCompat
import helium314.keyboard.latin.R

/**
 * Selector de segmentos para Vitrina modo: "Producto" | "Booking" |
 * "Respuestas rapidas".
 *
 * - Segmento por defecto al abrir Vitrina modo: "Producto".
 * - Al tocar un segmento, emite un callback que VitrinaHost captura para
 *   reemplazar el contenido de abajo sin cerrar el panel.
 * - Los segmentos miden su ancho natural (wrap_content) y viven en un
 *   HorizontalScrollView: nunca se truncan, se deslizan si no entran en
 *   pantalla (tambien si a futuro se agregan mas segmentos).
 * - "Booking" y "Respuestas rapidas" muestran placeholder por ahora.
 */
class VitrinaSwitch(
    context: Context,
    attrs: AttributeSet? = null,
    defStyle: Int = 0,
) : FrameLayout(context, attrs, defStyle) {

    // Listeners for segment selection changes
    private var onProductSelected: (() -> Unit)? = null
    private var onBookingSelected: (() -> Unit)? = null
    private var onQuickRepliesSelected: (() -> Unit)? = null

    private var currentSegment = 0  // 0 = Producto, 1 = Booking, 2 = Respuestas rapidas

    // Segment data: label, contentDescription
    private data class Segment(
        val label: String,
        val contentDescription: String,
    )

    private val segments = listOf(
        Segment(
            label = context.getString(R.string.vitrina_switch_producto),
            contentDescription = context.getString(R.string.vitrina_switch_producto_cd),
        ),
        Segment(
            label = context.getString(R.string.vitrina_switch_booking),
            contentDescription = context.getString(R.string.vitrina_switch_booking_cd),
        ),
        Segment(
            label = context.getString(R.string.vitrina_switch_respuestas),
            contentDescription = context.getString(R.string.vitrina_switch_respuestas_cd),
        )
    )

    private val selectedBgRes get() = R.color.vitrina_switch_selected_bg
    private val selectedColor get() = ContextCompat.getColor(context, R.color.vitrina_switch_selected_fg)
    private val unselectedBgRes get() = R.color.vitrina_switch_unselected_bg
    private val unselectedColor get() = ContextCompat.getColor(context, R.color.vitrina_switch_unselected_fg)

    private val container = LinearLayout(context).apply {
        orientation = LinearLayout.HORIZONTAL
        gravity = Gravity.CENTER_VERTICAL
        setPadding(
            0,
            context.resources.getDimensionPixelSize(R.dimen.kb_bar_pad_h),
            0,
            context.resources.getDimensionPixelSize(R.dimen.kb_bar_pad_h),
        )
    }

    private val scroll = HorizontalScrollView(context).apply {
        isHorizontalScrollBarEnabled = false
        isVerticalScrollBarEnabled = false
        addView(
            container,
            FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.WRAP_CONTENT,
                FrameLayout.LayoutParams.WRAP_CONTENT,
            ),
        )
    }

    init {
        addView(scroll, FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.MATCH_PARENT,
            FrameLayout.LayoutParams.WRAP_CONTENT,
            Gravity.CENTER_VERTICAL,
        ))

        // Crear los segmentos con ancho natural: el HorizontalScrollView los
        // deja deslizar si no entran completos en pantalla.
        for (i in 0 until segments.size) {
            val segment = segments[i]
            val btn = createSegmentButton(segment, i)
            container.addView(btn)
        }

        // Establecer segmento por defecto "Producto" (indice 0)
        selectSegment(0)
    }

    private fun createSegmentButton(
        segment: Segment,
        index: Int,
    ): Button {
        val btn = createButton(segment.label, segment.contentDescription)
        btn.setOnClickListener {
            selectSegment(index)
            when (index) {
                0 -> onProductSelected?.invoke()
                1 -> onBookingSelected?.invoke()
                2 -> onQuickRepliesSelected?.invoke()
            }
        }
        return btn
    }

    private fun createButton(label: String, contentDescription: String): Button =
        Button(context).apply {
            text = label
            this.contentDescription = contentDescription
            isSingleLine = true
            maxLines = 1
            setTextColor(unselectedColor)
            setBackgroundResource(unselectedBgRes)
            setTextSize(
                android.util.TypedValue.COMPLEX_UNIT_PX,
                context.resources.getDimension(R.dimen.type_supporting_label_size),
            )
            gravity = Gravity.CENTER
            // Ancho natural (wrap_content + padding): el label nunca se trunca.
            // Si no entran todos, el HorizontalScrollView los deja deslizar.
            setPadding(
                context.resources.getDimensionPixelSize(R.dimen.spacing_2),
                0,
                context.resources.getDimensionPixelSize(R.dimen.spacing_2),
                0,
            )
            // Button trae minWidth=88dp por defecto; lo bajamos a 48dp para que
            // el tab mida su ancho natural (wrap_content) sin ese piso.
            val touchMin = context.resources.getDimensionPixelSize(R.dimen.size_touch_min)
            minWidth = touchMin
            minimumWidth = touchMin
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT,
            )
        }

    /** Agrega un elemento al final del scroll, con el mismo estilo que los
     * segmentos pero con su propia accion (p. ej. "Documentos"). No participa
     * en la seleccion de segmento. */
    fun addTrailingAction(label: String, contentDescription: String, onClick: () -> Unit) {
        val btn = createButton(label, contentDescription)
        btn.setOnClickListener { onClick() }
        container.addView(btn)
    }

    fun selectSegment(index: Int) {
        if (index == currentSegment) return

        currentSegment = index

        // Restablecer todos los botones a estado no seleccionado
        for (i in 0 until container.childCount) {
            val child = container.getChildAt(i)
            if (child is Button) {
                child.setBackgroundResource(unselectedBgRes)
                child.setTextColor(unselectedColor)
            }
        }

        // Seleccionar el boton activo
        val activeBtn = container.getChildAt(index)
        if (activeBtn is Button) {
            activeBtn.setBackgroundResource(selectedBgRes)
            activeBtn.setTextColor(selectedColor)
        }
    }

    // --- Exposicion de callbacks para VitrinaHost ---

    fun setOnProductSelected(listener: (() -> Unit)) {
        onProductSelected = listener
    }

    fun setOnBookingSelected(listener: (() -> Unit)) {
        onBookingSelected = listener
    }

    fun setOnQuickRepliesSelected(listener: (() -> Unit)) {
        onQuickRepliesSelected = listener
    }
}
