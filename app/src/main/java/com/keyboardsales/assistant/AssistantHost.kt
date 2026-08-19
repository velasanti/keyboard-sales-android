package com.keyboardsales.assistant

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.view.Gravity
import android.view.View
import android.view.accessibility.AccessibilityManager
import android.widget.FrameLayout
import android.widget.LinearLayout
import androidx.core.content.ContextCompat
import com.keyboardsales.assistant.action.ActionType
import com.keyboardsales.assistant.action.DummyActionResolver
import com.keyboardsales.assistant.consult.DummyConsultant
import com.keyboardsales.assistant.intent.AssistantIntentType
import com.keyboardsales.assistant.intent.DummyIntentDetector
import com.keyboardsales.assistant.redact.DummyRedactor
import com.keyboardsales.ime.SalesIME
import com.keyboardsales.vitrina.VitrinaHost
import com.keyboardsales.vitrina.expandTouchTarget
import com.keyboardsales.vitrina.insert.InsertController
import helium314.keyboard.event.Event
import helium314.keyboard.keyboard.internal.keyboard_parser.floris.KeyCode
import helium314.keyboard.latin.R
import helium314.keyboard.latin.utils.Log
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

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
    private var stripContent: FrameLayout? = null
    private var anchorSlot: FrameLayout? = null
    private var suggestionStripView: View? = null
    private var keyboardViewWrapper: android.view.ViewGroup? = null

    private var layerView: AssistantLayerView? = null
    private var layerActive = false

    private val searchExecutor: ExecutorService = Executors.newSingleThreadExecutor()
    private val mainHandler = Handler(Looper.getMainLooper())
    private var sendSequence = 0
    private var pendingMessage: String? = null
    private var pendingActionType: ActionType? = null
    private var insertedLength = 0
    private var undoShowing = false
    private var undoRunnable: Runnable? = null

    /** Si el teclado captura hacia el cuadro de ✨ en vez de al campo anfitrion. */
    val isCaptureActive: Boolean get() = layerActive

    fun onInputView(view: View) {
        stripContainer = view.findViewById<FrameLayout>(R.id.strip_container)
        stripContent = view.findViewById<FrameLayout>(R.id.strip_content)
        anchorSlot = view.findViewById<FrameLayout>(R.id.anchor_slot)
        suggestionStripView = stripContent?.findViewById(R.id.suggestion_strip_view)
        keyboardViewWrapper = view.findViewById<android.view.ViewGroup>(R.id.keyboard_view_wrapper)
        Log.d(TAG, "onInputView: strip=${stripContainer != null}, wrapper=${keyboardViewWrapper != null}")
        injectLayer()
        injectAnchorBar()
    }

    private fun injectLayer() {
        val container = stripContent ?: return
        val layer = AssistantLayerView(container.context).apply {
            visibility = View.GONE
            onClose = { hideLayer() }
            onSend = { handleSend() }
            onConfirm = { onConfirmCard() }
            onCancelConfirm = { cancelConfirm() }
            onUndo = { performUndo() }
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
     * ✨ a su derecha) en la barra superior (strip_container), NO flotando sobre
     * el QWERTY. Nivel B (decisión Santi 2026-08-17): anclas de 40dp de alto
     * (alto de la strip) con área táctil expandida a 48dp; cero cambio al
     * presupuesto vertical en ningún estado.
     *
     * Las anclas viven en anchor_slot, su propio espacio dedicado dentro de
     * strip_container (hermano de strip_content). Nada de lo que vive en
     * strip_content (sugerencias, chips, capa ✨) puede quedar detrás ni debajo
     * de las anclas: es un problema de layout resuelto estructuralmente, no con
     * margen ni opacidad.
     */
    private fun injectAnchorBar() {
        val container = anchorSlot ?: return
        val context = container.context
        val anchorHeight = context.resources.getDimensionPixelSize(R.dimen.config_suggestions_strip_height)
        val anchorWidth = context.resources.getDimensionPixelSize(R.dimen.kb_anchor_size)

        val catalogAnchor = android.widget.TextView(context).apply {
            text = "☰"
            gravity = Gravity.CENTER
            isClickable = true
            isFocusable = true
            setSingleLine(true)
            maxLines = 1
            setTextColor(ContextCompat.getColor(context, R.color.content_primary))
            setTextSize(
                android.util.TypedValue.COMPLEX_UNIT_PX,
                context.resources.getDimension(R.dimen.type_body_large_size),
            )
            contentDescription = context.getString(R.string.vitrina_anchor_open)
            setOnClickListener {
                // Exclusion mutua: si ✨ esta activo, se cierra antes de abrir Vitrina.
                if (layerActive) hideLayer()
                vitrinaHost.togglePanel()
            }
            background = anchorBackground(context)
        }

        val assistantAnchor = android.widget.TextView(context).apply {
            // Mismo mecanismo que el ☰ (glifo de TEXTO, no vector): el vector
            // como foreground no pinto su relleno (evidencia: 0 px oscuros en la
            // pill), mientras el glifo de texto ☰ si pinta. Se usa el simbolo de
            // estrella de 4 puntas U+2726, monochrome como ☰ (no emoji de color),
            // que renderiza con ancho real y contrasta (content_primary sobre
            // surface_raised).
            text = "✦"
            gravity = Gravity.CENTER
            isClickable = true
            isFocusable = true
            setSingleLine(true)
            maxLines = 1
            setTextColor(ContextCompat.getColor(context, R.color.content_primary))
            setTextSize(
                android.util.TypedValue.COMPLEX_UNIT_PX,
                context.resources.getDimension(R.dimen.type_body_large_size),
            )
            background = anchorBackground(context)
            contentDescription = context.getString(R.string.assistant_anchor_open)
            setOnClickListener { toggleLayer() }
        }

        val bar = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            val gap = resources.getDimensionPixelSize(R.dimen.kb_bar_gap)
            addView(
                catalogAnchor,
                LinearLayout.LayoutParams(anchorWidth, anchorHeight).apply { marginEnd = gap },
            )
            addView(assistantAnchor, LinearLayout.LayoutParams(anchorWidth, anchorHeight))
        }

        val pad = context.resources.getDimensionPixelSize(R.dimen.kb_bar_pad_h)
        val lp = FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.WRAP_CONTENT,
            FrameLayout.LayoutParams.WRAP_CONTENT,
            Gravity.TOP or Gravity.START,
        ).apply {
            marginStart = pad
        }
        container.addView(bar, lp)

        // Área táctil 48dp (regla 8): expande el hit rect de cada ancla. El
        // vertical queda acotado al alto de la strip (40dp); el horizontal llega a 48dp.
        val touchMin = context.resources.getDimensionPixelSize(R.dimen.size_touch_min)
        container.expandTouchTarget(catalogAnchor, touchMin)
        container.expandTouchTarget(assistantAnchor, touchMin)

        Log.d(TAG, "injectAnchorBar: anclas en anchor_slot=${container.childCount} (☰ y ✨)")
        bar.post {
            val catalog = bar.getChildAt(0)
            val assistant = bar.getChildAt(1)
            val kv = stripContent?.findViewById<View>(R.id.suggestion_strip_view)
            Log.d(
                TAG,
                "layoutQ: anchor_slot bounds=[${container.left},${container.top}]-" +
                    "[${container.right},${container.bottom}] h=${container.height}",
            )
            Log.d(
                TAG,
                "layoutQ: wrapper(keyboard) top=${keyboardViewWrapper?.top} bottom=${keyboardViewWrapper?.bottom} " +
                    "(strip bottom == wrapper top si no se solapan)",
            )
            Log.d(
                TAG,
                "layoutQ: suggestion_strip_view bounds=[${kv?.left},${kv?.top}]-[${kv?.right},${kv?.bottom}]",
            )
            Log.d(
                TAG,
                "anchorBar: bar medidas=${bar.width}x${bar.height} X=${bar.x} Y=${bar.y} " +
                    "elevation=${bar.elevation}",
            )
            Log.d(
                TAG,
                "anchorBar: ☰ (child0) medidas=${catalog.width}x${catalog.height} " +
                    "X=${catalog.x} Y=${catalog.y} left=${catalog.left} top=${catalog.top}",
            )
            Log.d(
                TAG,
                "anchorBar: ✨ (child1) medidas=${assistant.width}x${assistant.height} " +
                    "X=${assistant.x} Y=${assistant.y} left=${assistant.left} top=${assistant.top} " +
                    "foreground=${assistant.foreground}",
            )
        }
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

    /**
     * Corre el intent dummy FUERA del hilo de UI (mismo criterio que la busqueda
     * de Vitrina) y muestra el resultado en el historial de ✨. Paso 3: solo
     * clasifica; el armado del mensaje (Paso 4), el calculo (Paso 5) y la tarjeta
     * de accion (Paso 6) entran despues.
     */
    private fun handleSend() {
        val layer = layerView ?: return
        val text = layer.currentInput()
        if (text.isBlank()) return
        val seq = ++sendSequence
        searchExecutor.execute {
            val intent = DummyIntentDetector.detect(text)
            Log.d(TAG, "handleSend text='$text' intent=${intent.type}")
            val line = when (intent.type) {
                // Paso 4: la redaccion no solo se clasifica, arma el mensaje y
                // exige confirmacion ADR-016 antes de insertar al chat.
                AssistantIntentType.REDACT -> {
                    val message = DummyRedactor.redact(text)
                    mainHandler.post {
                        if (seq == sendSequence) {
                            pendingMessage = message
                            layer.showConfirm(message)
                        }
                    }
                    return@execute
                }
                // Paso 5: la consulta de conocimiento se responde EN EL HISTORIAL
                // de ✨; nada se inserta al chat.
                AssistantIntentType.CONSULT -> {
                    val answer = DummyConsultant.answer(
                        text,
                        ime.getString(R.string.assistant_consult_cuotas),
                        ime.getString(R.string.assistant_consult_fallback),
                    )
                    mainHandler.post {
                        if (seq == sendSequence) layer.addHistory(answer)
                    }
                    return@execute
                }
                // Paso 6: la accion ejecutable exige confirmacion explicita ADR-016.
                // En esta fase no hay efecto real (no PDF, no backend); se construye
                // la UI de confirmacion, la pieza que despues nunca se salta.
                AssistantIntentType.ACTION -> {
                    val proposal = DummyActionResolver.resolve(text)
                    val label = actionLabel(proposal.type)
                    val summary = ime.getString(R.string.assistant_action_confirm, label)
                    mainHandler.post {
                        if (seq == sendSequence) {
                            pendingActionType = proposal.type
                            layer.showConfirm(summary)
                        }
                    }
                    return@execute
                }
                AssistantIntentType.NONE -> "No entendí todavía (dummy)"
            }
            // Descarta el resultado si entre tanto hubo un envío mas nuevo: la
            // clasificacion vieja nunca postea despues de la actual (anti-carrera
            // de doble envío). La clasificacion es por envío, no por pulsación.
            mainHandler.post {
                if (seq == sendSequence) layer.addHistory(line)
            }
        }
    }

    // ------------------------------------------------------------------
    // Confirmacion ADR-016: dispatch REDACT (inserta) vs ACTION (sin efecto)
    // ------------------------------------------------------------------

    /**
     * La tarjeta de confirmacion (ADR-016) se ejecuto. Segun que este pendiente:
     * REDACT -> inserta el mensaje al chat (Paso 4); ACTION -> accion dummy sin
     * efecto real (Paso 6), se registra en el historial de ✨ y no toca el chat.
     */
    private fun onConfirmCard() {
        val layer = layerView ?: return
        when {
            pendingMessage != null -> performInsert()
            pendingActionType != null -> {
                val type = pendingActionType!!
                pendingActionType = null
                Log.d(TAG, "onConfirmCard ACTION confirmado: $type")
                layer.addHistory(ime.getString(R.string.assistant_action_done, actionLabel(type)))
                layer.showInput()
            }
        }
    }

    /** Cancelar la confirmacion (ADR-016): descarta lo pendiente y vuelve al input. */
    private fun cancelConfirm() {
        pendingMessage = null
        pendingActionType = null
        layerView?.showInput()
    }

    private fun actionLabel(type: ActionType): String = when (type) {
        ActionType.PDF -> ime.getString(R.string.assistant_action_pdf)
        ActionType.DERIVE -> ime.getString(R.string.assistant_action_derive)
        ActionType.ORDER -> ime.getString(R.string.assistant_action_order)
        ActionType.GENERIC -> ime.getString(R.string.assistant_action_generic)
    }

    // ------------------------------------------------------------------
    // Insercion (reusa InsertController de Vitrina, no se reinventa) + Deshacer
    // ------------------------------------------------------------------

    private fun performInsert() {
        cancelPendingUndo()
        val layer = layerView ?: return
        val message = pendingMessage ?: return
        val insertResult = InsertController.insert(ime, 0, message)
        if (insertResult < 0) return
        insertedLength = insertResult
        pendingMessage = null
        undoShowing = true
        layer.showUndo()
        scheduleUndoTimeout()
    }

    private fun performUndo() {
        cancelPendingUndo()
        val layer = layerView ?: return
        InsertController.undo(ime, insertedLength)
        undoShowing = false
        insertedLength = 0
        layer.showInput()
    }

    private fun cancelPendingUndo() {
        undoRunnable?.let { mainHandler.removeCallbacks(it) }
        undoRunnable = null
        undoShowing = false
    }

    private fun scheduleUndoTimeout() {
        val layer = layerView ?: return
        val resources = layer.resources
        val a11y = (ime.applicationContext.getSystemService(Context.ACCESSIBILITY_SERVICE) as? AccessibilityManager)
            ?.isTouchExplorationEnabled == true
        val timeout = resources.getInteger(
            if (a11y) R.integer.motion_undo_duration_a11y else R.integer.motion_undo_duration,
        ).toLong()
        val runnable = Runnable { if (undoShowing) layer.showInput() }
        undoRunnable = runnable
        mainHandler.postDelayed(runnable, timeout)
    }

    /**
     * Redirige un [Event] del QWERTY al cuadro de ✨. Devuelve true si lo consumio.
     * Llamado desde [SalesIME.onEvent] solo cuando [isCaptureActive].
     *
     * El texto NUNCA llega al campo de la app anfitriona: al no reenviar el evento
     * al motor, el InputConnection del chat no recibe commit ni delete (04.4 §3).
     */
    fun onAssistantEvent(event: Event): Boolean {
        val layer = layerView ?: return false
        when {
            event.keyCode == KeyCode.DELETE -> layer.inputBackspace()
            event.codePoint == '\n'.code -> layer.send()
            event.codePoint >= 0 && event.codePoint != Event.NOT_A_CODE_POINT -> layer.inputCharacter(event.codePoint)
            else -> return false
        }
        return true
    }

    private fun showLayer() {
        val container = stripContainer ?: return
        val suggestion = suggestionStripView ?: return
        val layer = layerView ?: return
        // Exclusion mutua: si Vitrina modo esta abierto, se cierra antes de abrir ✨.
        vitrinaHost.closePanel()
        layerActive = true

        // Cierra la composicion pendiente del chat (la "media palabra" con subrayado
        // de autocorreccion) que el vendedor pudo dejar a medio escribir antes de
        // activar ✨. Se hace UNA vez al activar, no por pulsacion.
        ime.getCurrentInputConnection()?.finishComposingText()
        layer.clearInput()
        layer.showInput()

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
        cancelPendingUndo()
        pendingMessage = null
        pendingActionType = null
        insertedLength = 0
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

    fun resetToIdle() {
        if (layerActive) hideLayer()
    }

    companion object {
        private const val TAG = "Assistant"
    }
}
