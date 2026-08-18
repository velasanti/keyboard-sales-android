package com.keyboardsales.ime

import android.os.Handler
import android.os.Looper
import android.view.View
import android.view.inputmethod.EditorInfo
import com.keyboardsales.assistant.AssistantHost
import com.keyboardsales.vitrina.VitrinaHost
import com.keyboardsales.vitrina.data.CatalogLoader
import com.keyboardsales.vitrina.data.CatalogRepository
import helium314.keyboard.event.Event
import helium314.keyboard.latin.BuildConfig
import helium314.keyboard.latin.LatinIME
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