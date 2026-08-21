package com.keyboardsales.plus

import android.content.ClipDescription
import android.content.Intent
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.provider.MediaStore
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputContentInfo
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.Toast
import com.keyboardsales.ime.SalesIME
import helium314.keyboard.latin.R
import helium314.keyboard.latin.utils.Log
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

/**
 * Orquestador de la Franja 1 (adjuntos). Duenio de [AttachMenuState]; la
 * vista ([AttachStripView]) es pasiva y se re-renderiza por transicion.
 *
 * ── Mecanismo de expansion/colapso (decision de esta implementacion) ──────
 *
 * "Expansion contextual con rendimiento al tipeo":
 *
 * 1. COLAPSADA ES EL ESTADO CANONICO: GONE, costo permanente 0dp. La medicion
 *    del spike cerro el tema: una fila fija de 48dp lleva al 53.5% del caso
 *    critico (480x854 hdpi, 569dp utiles), muy arriba del tope del 45%.
 * 2. EXPANSION AUTOMATICA CONTEXTUAL: solo en onStartInputView y SOLO si el
 *    campo anfitrion declara contentMimeTypes compatibles con imagenes
 *    ([ContentCommitPolicy.showsStrip]). En la practica: los composers de los
 *    chats, que es donde adjuntar tiene sentido. Campos de busqueda, URL o
 *    numericos nunca la muestran.
 * 3. COLAPSO AUTOMATICO por cualquiera de estas senales:
 *      - primera tecla del QWERTY hacia el anfitrion ([onKeyboardEvent]):
 *        el vendedor paso a modo texto, la conversacion vuelve a su lugar;
 *      - timeout de inactividad en ROOT (6 s): si no eligio nada, libera;
 *      - ✕ explicito; accion ejecutada; onFinishInputView;
 *      - apertura de Vitrina modo o de la capa ✨ (exclusion mutua, mismos
 *        criterios que entre esas dos superficies).
 *
 * Alternativas descartadas y por que (para no re-decidir en cada PR):
 *   - Gatillo permanente (chip propio fuera de Franja 2): no hay presupuesto.
 *     Al peor caso le quedan ~16dp libres y un control exige 48dp tactiles.
 *   - Chip ＋ junto a las anclas ☰/✦ (anchor_slot): cero costo de alto, PERO
 *     viola la decision explicita "Franja 2 no se toca".
 *   - Gestos/long-press sobre el QWERTY: indescubribles y rozan la tuberia de
 *     gestos de upstream.
 *
 * LIMITACION CONOCIDA, a decidir por producto despues de probar en dispositivo:
 * colapsada a mitad de sesion, la franja no tiene como reabrirse hasta el
 * proximo cambio de foco (enviar un mensaje cierra el teclado y lo re-dispara;
 * tambien cerrar/abrir el teclado con atras). Acceso persistente exigiria
 * ceder UNA de dos restricciones hoy vigentes: la congelacion de Franja 2 o el
 * presupuesto vertical. Ninguna se cede por iniciativa de este codigo.
 *
 * ── Insercion del adjunto ─────────────────────────────────────────────────
 *
 * Camino principal: InputConnection.commitContent — probado contra WhatsApp
 * Business (com.whatsapp.w4b) en el spike: acepta image/png con URI propio de
 * MediaStore y flag 0. Sharesheet SOLO como fallback cuando el editor no
 * declara mimes compatibles o rechaza el commit ([ContentCommitPolicy.route]).
 *
 * La camara/fotos/documento necesitan RESULTADO de una activity; un IME no
 * recibe onActivityResult, por eso existe [AttachPickActivity] (trampolin).
 */
class AttachHost(private val ime: SalesIME) : AttachStripView.Callbacks {

    private var stripView: AttachStripView? = null
    private var state = AttachMenuState()

    private var editorMimes: List<String>? = null
    private var pendingResult = false
    private var suppressNextAutoExpand = false
    private var pendingCameraUri: Uri? = null

    private val executor: ExecutorService = Executors.newSingleThreadExecutor()
    private val mainHandler = Handler(Looper.getMainLooper())
    private var collapseRunnable: Runnable? = null

    // ------------------------------------------------------------------
    // Ciclo de vida (llamado desde SalesIME)
    // ------------------------------------------------------------------

    /** Inyecta la franja ENCIMA de strip_container (indice 0 del frame raiz). */
    fun onInputView(view: View) {
        val stripContainer = view.findViewById<FrameLayout>(R.id.strip_container) ?: return
        val root = stripContainer.parent as? ViewGroup ?: return
        // Si el root ya trae franja (vista recreada sobre el mismo arbol), se reusa.
        val existing = root.getChildAt(0) as? AttachStripView
        val strip = existing ?: AttachStripView(view.context).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                view.resources.getDimensionPixelSize(R.dimen.kb_bar_height),
            )
            visibility = View.GONE
        }
        strip.callbacks = this
        stripView = strip
        if (existing == null) root.addView(strip, 0)
        Log.d(TAG, "franja 1 inyectada encima de strip_container (reusada=${existing != null})")
    }

    fun onFieldFocused(editorInfo: EditorInfo?) {
        editorMimes = editorInfo?.contentMimeTypes?.toList()
        // Diagnostico en dispositivo (android.util.Log.i directo, no el wrapper
        // de HeliBoard que gatea Log.d): sin esta linea, un showsStrip=false
        // es silencioso y la franja "no aparece" sin rastro.
        android.util.Log.i(
            TAG,
            "onFieldFocused pkg=${editorInfo?.packageName} " +
                "mimes=${editorMimes?.joinToString(prefix = "[", postfix = "]") ?: "null"} " +
                "muestraFranja=${ContentCommitPolicy.showsStrip(editorMimes)} " +
                "pendienteResultado=$pendingResult suprimir=$suppressNextAutoExpand",
        )
        if (pendingResult || suppressNextAutoExpand) {
            // Volviendo de un picker, o recien se inserto algo: la franja no
            // se re-abre sola para no titilar justo despues de usarla.
            pendingResult = false
            suppressNextAutoExpand = false
            setState(state.collapse())
            return
        }
        val accepts = ContentCommitPolicy.showsStrip(editorMimes)
        setState(state.onFieldFocused(accepts))
    }

    fun onFinishInput() {
        setState(state.collapse())
    }

    /** Primera tecla del QWERTY hacia el anfitrion: rinde el alto ocupado. */
    fun onKeyboardEvent() {
        if (state.isVisible && !pendingResult) setState(state.collapse())
    }

    /** Exclusion mutua: lo llaman VitrinaHost y AssistantHost al abrir sus superficies. */
    fun collapse() {
        setState(state.collapse())
    }

    // ------------------------------------------------------------------
    // Callbacks de la franja
    // ------------------------------------------------------------------

    override fun onPlus() = setState(state.onPlusTapped())

    override fun onMore() = setState(state.onMoreTapped())

    override fun onBack() = setState(state.onBackTapped())

    override fun onDismiss() = setState(state.collapse())

    override fun onLocation() {
        toast(R.string.plus_coming_soon)
    }

    override fun onContact() {
        toast(R.string.plus_coming_soon)
    }

    override fun onDocument() {
        launchPick(AttachPickActivity.WHICH_DOCUMENT, null)
    }

    override fun onPhotos() {
        launchPick(AttachPickActivity.WHICH_PHOTOS, null)
    }

    override fun onCamera() {
        // Chequeo previo de resolucion: si no hay app de camara, feedback
        // especifico sin abrir el trampolin ni ensuciar el MediaStore.
        if (!AttachIntents.resolves(ime.applicationContext, AttachIntents.camera(null))) {
            toast(R.string.plus_camera_unavailable)
            return
        }
        executor.execute {
            val uri = prepareCameraUri()
            mainHandler.post {
                if (uri == null) {
                    toast(R.string.plus_action_failed)
                    return@post
                }
                pendingCameraUri = uri
                beginPick(AttachPickActivity.WHICH_CAMERA, uri)
            }
        }
    }

    /**
     * Inserta la fila del MediaStore que recibira la foto de la camara.
     * Sin permisos: el URI es nuestro. IS_PENDING existe desde API 24; antes,
     * la fila aparece en la galeria de inmediato (aceptado en gama muy baja).
     */
    private fun prepareCameraUri(): Uri? = try {
        val values = android.content.ContentValues().apply {
            put(
                MediaStore.Images.Media.DISPLAY_NAME,
                "ks_adjunto_" + SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date()) + ".jpg",
            )
            put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
            if (android.os.Build.VERSION.SDK_INT >= 29) {
                put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/Keyboard Sales")
            }
            if (android.os.Build.VERSION.SDK_INT >= 24) {
                put(MediaStore.Images.Media.IS_PENDING, 1)
            }
        }
        ime.contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
    } catch (e: Exception) {
        Log.w(TAG, "no se pudo preparar el URI de camara: $e")
        null
    }

    // ------------------------------------------------------------------
    // Lanzamiento del trampolin
    // ------------------------------------------------------------------

    /**
     * Lanza el trampolin SIN colapsar antes: el estado del menu persiste
     * durante el ida-y-vuelta (el trampolin cubre la pantalla). En exito,
     * deliverContent colapsa con supresion del auto-expand; en cancelacion,
     * reopenMenuAfterCancel restaura PRINCIPAL.
     */
    private fun launchPick(which: String, outputUri: Uri?) {
        beginPick(which, outputUri)
    }

    private fun beginPick(which: String, outputUri: Uri?) {
        val context = ime.applicationContext
        pendingResult = true
        AttachPickActivity.setHandler { requestCode, resultCode, data ->
            onPickResult(requestCode, resultCode, data)
        }
        val trampoline = Intent(context, AttachPickActivity::class.java).apply {
            putExtra(AttachPickActivity.EXTRA_WHICH, which)
            outputUri?.let { putExtra(AttachPickActivity.EXTRA_OUTPUT_URI, it) }
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_NO_ANIMATION)
        }
        try {
            context.startActivity(trampoline)
        } catch (e: Exception) {
            Log.w(TAG, "trampolin no lanzable: $e")
            pendingResult = false
            toast(R.string.plus_action_failed)
        }
    }

    private fun onPickResult(requestCode: Int, resultCode: Int, data: Intent?) {
        when (requestCode) {
            AttachIntents.REQUEST_CAMERA -> finishCamera(resultCode == android.app.Activity.RESULT_OK)
            AttachIntents.REQUEST_PHOTOS -> finishMedia(resultCode == android.app.Activity.RESULT_OK, data, "image/jpeg")
            AttachIntents.REQUEST_DOCUMENT -> finishMedia(
                resultCode == android.app.Activity.RESULT_OK,
                data,
                "application/octet-stream",
            )
        }
    }

    private fun finishCamera(ok: Boolean) {
        val uri = pendingCameraUri
        pendingCameraUri = null
        if (!ok || uri == null) {
            cleanupCameraUri(uri)
            reopenMenuAfterCancel()
            return
        }
        executor.execute {
            try {
                if (android.os.Build.VERSION.SDK_INT >= 24) {
                    val values = android.content.ContentValues().apply {
                        put(MediaStore.Images.Media.IS_PENDING, 0)
                    }
                    ime.contentResolver.update(uri, values, null, null)
                }
            } catch (e: Exception) {
                Log.w(TAG, "IS_PENDING=0 fallo (se entrega igual): $e")
            }
            mainHandler.post { deliverContent(uri, imeOwnsUri = true, mime = "image/jpeg") }
        }
    }

    private fun finishMedia(ok: Boolean, data: Intent?, defaultMime: String) {
        val uri = data?.data
        if (!ok || uri == null) {
            reopenMenuAfterCancel()
            return
        }
        executor.execute {
            val mime = runCatching { ime.contentResolver.getType(uri) }.getOrNull() ?: defaultMime
            mainHandler.post { deliverContent(uri, imeOwnsUri = false, mime = mime) }
        }
    }

    private fun reopenMenuAfterCancel() {
        pendingResult = false
        setState(state.onPickCancelled())
    }

    private fun cleanupCameraUri(uri: Uri?) {
        val safe = uri ?: return
        executor.execute {
            try {
                ime.contentResolver.delete(safe, null, null)
            } catch (e: Exception) {
                Log.w(TAG, "limpieza de URI de camara fallo: $e")
            }
        }
    }

    // ------------------------------------------------------------------
    // Entrega: commitContent con fallback a sharesheet
    // ------------------------------------------------------------------

    private fun deliverContent(uri: Uri, imeOwnsUri: Boolean, mime: String) {
        pendingResult = false
        val mimes = ime.currentInputEditorInfo?.contentMimeTypes?.toList() ?: editorMimes
        val ic = ime.currentInputConnection
        val route = ContentCommitPolicy.route(mimes, mime)

        val committed = route == ContentCommitPolicy.Route.COMMIT_CONTENT &&
            ic != null &&
            commitContent(ic, uri, imeOwnsUri, mime)
        Log.i(TAG, "adjunto mime=$mime ruta=$route committed=$committed")

        if (!committed) shareFallback(uri, mime)
        suppressNextAutoExpand = true
        setState(state.collapse())
    }

    private fun commitContent(
        ic: android.view.inputmethod.InputConnection,
        uri: Uri,
        imeOwnsUri: Boolean,
        mime: String,
    ): Boolean = try {
        val description = ClipDescription(ime.getString(R.string.plus_clip_label), arrayOf(mime))
        val info = InputContentInfo(uri, description, null)
        ic.commitContent(info, ContentCommitPolicy.commitFlags(imeOwnsUri), null)
    } catch (e: Exception) {
        Log.w(TAG, "commitContent lanzo: $e")
        false
    }

    /**
     * Fallback: sharesheet (mismo patron que QuoteShare). El IME es un
     * Service: sin NEW_TASK no arranca actividad. El usuario elige app y chat.
     */
    private fun shareFallback(uri: Uri, mime: String) {
        val send = Intent(Intent.ACTION_SEND).apply {
            type = mime
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        val chooser = Intent.createChooser(send, ime.getString(R.string.plus_attach)).apply {
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        try {
            ime.startActivity(chooser)
        } catch (e: Exception) {
            Log.w(TAG, "sharesheet no lanzable: $e")
            toast(R.string.plus_action_failed)
        }
    }

    // ------------------------------------------------------------------
    // Render del estado
    // ------------------------------------------------------------------

    private fun setState(next: AttachMenuState) {
        if (next.panel == state.panel) return
        android.util.Log.i(TAG, "franja ${state.panel} -> ${next.panel}")
        state = next
        val strip = stripView ?: return
        strip.render(next.panel)
        strip.visibility = if (next.isVisible) View.VISIBLE else View.GONE
        rescheduleCollapseTimeout(next)
    }

    /**
     * El timeout SOLO corre en ROOT: es el estado "nadie esta eligiendo nada".
     * Con el menu abierto (PRINCIPAL/MAS) el vendedor esta decidiendo; no se
     * le cierra el menu mientras lee las opciones.
     */
    private fun rescheduleCollapseTimeout(current: AttachMenuState) {
        collapseRunnable?.let { mainHandler.removeCallbacks(it) }
        collapseRunnable = null
        if (current.panel != AttachPanel.ROOT) return
        val runnable = Runnable {
            if (state.panel == AttachPanel.ROOT) setState(state.collapse())
        }
        collapseRunnable = runnable
        mainHandler.postDelayed(runnable, COLLAPSE_TIMEOUT_MS)
    }

    private fun toast(res: Int) {
        Toast.makeText(ime.applicationContext, res, Toast.LENGTH_SHORT).show()
    }

    private companion object {
        const val TAG = "AttachPlus"
        const val COLLAPSE_TIMEOUT_MS = 6_000L
    }
}
