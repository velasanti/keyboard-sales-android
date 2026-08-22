package com.keyboardsales.ime

import android.os.Handler
import android.os.Looper
import android.inputmethodservice.InputMethodService
import android.graphics.Region
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.view.inputmethod.EditorInfo
import android.widget.FrameLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import com.keyboardsales.assistant.AssistantHost
import com.keyboardsales.plus.AttachHost
import com.keyboardsales.vitrina.VitrinaHost
import com.keyboardsales.vitrina.data.CatalogLoader
import com.keyboardsales.vitrina.data.CatalogRepository
import helium314.keyboard.event.Event
import helium314.keyboard.latin.BuildConfig
import helium314.keyboard.latin.LatinIME
import helium314.keyboard.latin.R
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

/**
 * IME del producto. Es una subclase aditiva de [LatinIME]: todo el codigo de
 * Vitrina se engancha aca por override, sin tocar el motor de sugerencias.
 * La unica edicion a upstream que exige es el `android:name` del servicio en
 * AndroidManifest.xml (ver docs/UPSTREAM.md).
 *
 * El catalogo se siembra al primer arranque en [executor], fuera del hilo de
 * UI (regla: nada bloqueante en el arranque del InputMethodService).
 */
class SalesIME : LatinIME() {

    private lateinit var vitrinaHost: VitrinaHost
    private lateinit var assistantHost: AssistantHost
    private lateinit var attachHost: AttachHost
    private lateinit var catalogRepository: CatalogRepository

    private val mainHandler = Handler(Looper.getMainLooper())
    private val executor: ExecutorService = Executors.newSingleThreadExecutor()

    override fun onCreate() {
        super.onCreate()
        android.util.Log.i("BuildInfo", "commit=${BuildConfig.BUILD_COMMIT} rama=${BuildConfig.BUILD_BRANCH}")
        vitrinaHost = VitrinaHost(this)
        assistantHost = AssistantHost(this, vitrinaHost)
        attachHost = AttachHost(this)
        // Exclusion mutua de superficies: cuando abre Vitrina modo o la capa ✨,
        // la franja de adjuntos colapsa (mismo criterio entre ☰ y ✨).
        vitrinaHost.onSurfaceOpened = { attachHost.collapse() }
        assistantHost.onSurfaceOpened = { attachHost.collapse() }
        catalogRepository = CatalogRepository(applicationContext)
        executor.execute {
            CatalogLoader.seedIfEmpty(applicationContext, catalogRepository)
            val esDummy = catalogRepository.isDummy()
            mainHandler.post {
                vitrinaHost.onRepositoryReady(catalogRepository)
                vitrinaHost.onCatalogLoaded(esDummy)
            }
        }
    }

    override fun onDestroy() {
        executor.shutdown()
        super.onDestroy()
    }

    override fun onCreateInputView(): View {
        return super.onCreateInputView()
    }

    override fun setInputView(view: View) {
        super.setInputView(view)
        vitrinaHost.onInputView(view)
        assistantHost.onInputView(view)
        attachHost.onInputView(view)
        showBuildBadge(view)
    }

    /**
     * Identidad de build visible SOLO en debug: el hash corto en la esquina
     * superior derecha del strip, como texto chico sin fondo (para que no se
     * confunda con un boton/badge). Cualquier captura de pantalla demuestra por
     * si sola que build era, sin necesitar un logcat aparte. No consume altura:
     * es un overlay en el corner del strip.
     *
     * Restaurado en 4502666a..: la reescritura aditiva de SalesIME durante el
     * fix de insets lo borro sin darse cuenta (regresion silenciosa — el tipo
     * de error que esta seccion existe para no repetir).
     */
    private fun showBuildBadge(view: View) {
        if (!BuildConfig.DEBUG) return
        val container = view.findViewById<FrameLayout>(R.id.strip_container) ?: return
        val resources = container.resources

        // setInputView puede correr varias veces en la vida del IME (cambio de
        // app, rotacion, recreacion del input view): reusa el badge por su id
        // fijo en vez de agregar otro encima.
        val existing = container.findViewById<TextView>(R.id.build_badge)
        if (existing != null) {
            existing.text = BuildConfig.BUILD_COMMIT
            existing.contentDescription = "Build ${BuildConfig.BUILD_COMMIT}"
            return
        }

        val badge = TextView(container.context).apply {
            id = R.id.build_badge
            text = BuildConfig.BUILD_COMMIT
            gravity = Gravity.END
            setSingleLine(true)
            maxLines = 1
            setTextColor(ContextCompat.getColor(container.context, R.color.content_secondary))
            setTextSize(
                TypedValue.COMPLEX_UNIT_PX,
                resources.getDimension(R.dimen.type_supporting_label_size),
            )
            contentDescription = "Build ${BuildConfig.BUILD_COMMIT}"
        }
        container.addView(
            badge,
            FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.WRAP_CONTENT,
                FrameLayout.LayoutParams.WRAP_CONTENT,
                Gravity.TOP or Gravity.END,
            ).apply {
                marginEnd = resources.getDimensionPixelSize(R.dimen.kb_bar_pad_h)
            },
        )
    }

    /**
     * Captura del QWERTY hacia ✨ o hacia la lupa de Vitrina modo, exclusiva
     * entre si (mismo mecanismo en los dos casos): mientras cualquiera de las
     * dos este activa, las teclas escriben a su cuadro propio y NUNCA al campo
     * de la app anfitriona. El motor no se toca, solo no se le alimenta input
     * mientras una de las dos compone (04.10 §9.6.2). ✨ tiene prioridad porque
     * las dos superficies ya se cierran mutuamente al abrirse (VitrinaHost.closePanel()
     * / AssistantHost), asi que en la practica solo una puede estar activa a la vez.
     */
    override fun onEvent(event: Event) {
        if (assistantHost.isCaptureActive) {
            assistantHost.onAssistantEvent(event)
        } else if (vitrinaHost.isSearchCaptureActive) {
            vitrinaHost.onSearchEvent(event)
        } else {
            // Tecla hacia el anfitrion: para la franja de adjuntos es la señal
            // canonica de "el vendedor paso a modo texto" y rinde su alto.
            attachHost.onKeyboardEvent()
            super.onEvent(event)
        }
    }

    override fun onStartInputView(editorInfo: EditorInfo?, restarting: Boolean) {
        super.onStartInputView(editorInfo, restarting)
        vitrinaHost.onStartInputView(editorInfo, restarting)
        assistantHost.resetToIdle()
        attachHost.onFieldFocused(editorInfo)
    }

    override fun onFinishInputView(finishingInput: Boolean) {
        super.onFinishInputView(finishingInput)
        vitrinaHost.onFinishInputView(finishingInput)
        assistantHost.resetToIdle()
        attachHost.onFinishInput()
    }

    /**
     * Causa raiz medida (SM-A515F, dumpsys window, 2026-08-21): upstream fija
     * ventana e inputView a MATCH_PARENT (Ktx.updateSoftInputWindowLayoutParameters),
     * entonces mInputView.getHeight() es constante (~2312px) y su formula de
     * insets (LatinIME.onComputeInsets:1182) — visibleTopY = inputHeight -
     * wrapper.height - stripContainer.height — NO contempla la franja de
     * adjuntos, que es un hijo nuevo del root frame. Resultado medido:
     * contentTopInsets=[0,1414] y touchableRegion desde y=1502 IDENTICOS con
     * franja visible y oculta; la franja dibuja fuera de la region tactil y
     * los insets no crecen, asique WhatsApp deja su composer solapado a ella.
     *
     * Fix: despues de super(), restar el alto de la franja a los tres valores.
     * Es el punto de extension diseñado por AOSP: el framework consume
     * outInsets cuando onComputeInsets retorna. Sin editar upstream, sin
     * tocar strip_container (Franja 2).
     */
    override fun onComputeInsets(outInsets: InputMethodService.Insets) {
        super.onComputeInsets(outInsets)
        if (!::attachHost.isInitialized) return
        val extra = attachHost.expansionInsetPx()
        if (extra <= 0) return
        outInsets.contentTopInsets -= extra
        outInsets.visibleTopInsets -= extra
        if (outInsets.touchableInsets == InputMethodService.Insets.TOUCHABLE_INSETS_REGION) {
            val adjusted = Region(outInsets.touchableRegion)
            adjusted.translate(0, -extra)
            outInsets.touchableRegion.set(adjusted)
        }
        android.util.Log.i(
            "AttachPlus",
            "onComputeInsets +$extra -> contentTop=${outInsets.contentTopInsets} " +
                "touchTop=${outInsets.touchableRegion.bounds.top}",
        )
    }

    override fun onUpdateSelection(
        oldSelStart: Int,
        oldSelEnd: Int,
        newSelStart: Int,
        newSelEnd: Int,
        composingSpanStart: Int,
        composingSpanEnd: Int,
    ) {
        super.onUpdateSelection(oldSelStart, oldSelEnd, newSelStart, newSelEnd, composingSpanStart, composingSpanEnd)
        vitrinaHost.onUpdateSelection(oldSelStart, oldSelEnd, newSelStart, newSelEnd, composingSpanStart, composingSpanEnd)
    }
}
