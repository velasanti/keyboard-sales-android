package com.keyboardsales.ime

import android.os.Handler
import android.os.Looper
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.view.inputmethod.EditorInfo
import android.widget.FrameLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import com.keyboardsales.assistant.AssistantHost
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
    private lateinit var catalogRepository: CatalogRepository

    private val mainHandler = Handler(Looper.getMainLooper())
    private val executor: ExecutorService = Executors.newSingleThreadExecutor()

    override fun onCreate() {
        super.onCreate()
        android.util.Log.i("BuildInfo", "commit=${BuildConfig.BUILD_COMMIT} rama=${BuildConfig.BUILD_BRANCH}")
        vitrinaHost = VitrinaHost(this)
        assistantHost = AssistantHost(this, vitrinaHost)
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
        showBuildBadge(view)
    }

    /**
     * Identidad de build visible SOLO en debug: el hash corto en la esquina
     * superior derecha del strip, como texto chico sin fondo (para que no se
     * confunda con un boton/badge). Cualquier captura de pantalla demuestra por
     * si sola que build era, sin necesitar un logcat aparte. No consume altura:
     * es un overlay en el corner del strip.
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
     * Captura de la capa ✨ (Paso 2): cuando el asistente esta activo, el QWERTY
     * escribe al cuadro propio de ✨ y NUNCA al campo de la app anfitriona.
     * Punto de enganche aditivo: el motor no se toca, solo no se le alimenta input
     * mientras la capa ✨ compone (familia 04.10 §9.6.2).
     */
    override fun onEvent(event: Event) {
        if (assistantHost.isCaptureActive) {
            assistantHost.onAssistantEvent(event)
        } else {
            super.onEvent(event)
        }
    }

    override fun onStartInputView(editorInfo: EditorInfo?, restarting: Boolean) {
        super.onStartInputView(editorInfo, restarting)
        vitrinaHost.onStartInputView(editorInfo, restarting)
        assistantHost.resetToIdle()
    }

    override fun onFinishInputView(finishingInput: Boolean) {
        super.onFinishInputView(finishingInput)
        vitrinaHost.onFinishInputView(finishingInput)
        assistantHost.resetToIdle()
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