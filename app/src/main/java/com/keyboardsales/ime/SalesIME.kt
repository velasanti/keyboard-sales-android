package com.keyboardsales.ime

import android.os.Handler
import android.os.Looper
import android.view.View
import android.view.inputmethod.EditorInfo
import com.keyboardsales.vitrina.VitrinaHost
import com.keyboardsales.vitrina.data.CatalogLoader
import com.keyboardsales.vitrina.data.CatalogRepository
import helium314.keyboard.event.Event
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
    private lateinit var catalogRepository: CatalogRepository

    private val mainHandler = Handler(Looper.getMainLooper())
    private val executor: ExecutorService = Executors.newSingleThreadExecutor()

    override fun onCreate() {
        super.onCreate()
        vitrinaHost = VitrinaHost(this)
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
    }

    /**
     * Captura del QWERTY hacia la lupa de Vitrina modo (mismo mecanismo que la
     * capa ✨): cuando la lupa esta enfocada, las teclas escriben al campo de
     * busqueda y NUNCA al campo de la app anfitriona. El motor no se toca; solo
     * no se le alimenta input mientras la lupa compone.
     */
    override fun onEvent(event: Event) {
        if (vitrinaHost.isSearchCaptureActive) {
            vitrinaHost.onSearchEvent(event)
        } else {
            super.onEvent(event)
        }
    }

    override fun onStartInputView(editorInfo: EditorInfo?, restarting: Boolean) {
        super.onStartInputView(editorInfo, restarting)
        vitrinaHost.onStartInputView(editorInfo, restarting)
    }

    override fun onFinishInputView(finishingInput: Boolean) {
        super.onFinishInputView(finishingInput)
        vitrinaHost.onFinishInputView(finishingInput)
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