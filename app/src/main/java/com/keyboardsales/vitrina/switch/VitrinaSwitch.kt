package com.keyboardsales.vitrina.switch

import android.content.Context
import android.util.AttributeSet
import android.view.Gravity
import android.view.ViewGroup
import android.widget.Button
import android.widget.FrameLayout
import android.widget.LinearLayout
import androidx.core.content.ContextCompat
import helium314.keyboard.latin.R

/**
 * Selector de 3 segmentos para Vitrina modo: "Producto" | "Booking" | "Respuestas rapidas".
 *
 * - Segmento por defecto al abrir Vitrina modo: "Producto".
 * - Al tocar un segmento, emite un callback que VitrinaHost captura para reemplazar
 *   el contenido de abajo sin cerrar el panel.
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

    private val segmentWeight = 1.0f / segments.size

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

    init {
        addView(container, FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.MATCH_PARENT,
            FrameLayout.LayoutParams.WRAP_CONTENT,
            Gravity.CENTER_VERTICAL,
        ))

        // Crear 3 segmentos con ancho equitativo usando weight
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
        val btn = Button(context).apply {
            text = segment.label
            contentDescription = segment.contentDescription
            isSingleLine = true
            maxLines = 1
            ellipsize = android.text.TextUtils.TruncateAt.END
            setTextColor(unselectedColor)
            setBackgroundResource(unselectedBgRes)
            setTextSize(
                android.util.TypedValue.COMPLEX_UNIT_PX,
                context.resources.getDimension(R.dimen.type_supporting_label_size),
            )
            gravity = Gravity.CENTER
            // layout: peso equitativo + minima area tactil de 48dp
            layoutParams = LinearLayout.LayoutParams(
                0,  // width = 0 significa wrap_content con weight
                ViewGroup.LayoutParams.WRAP_CONTENT,
            ).apply {
                weight = segmentWeight
                minWidth = context.resources.getDimensionPixelSize(R.dimen.size_touch_min)
            }
        }

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

    private fun selectSegment(index: Int) {
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
