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
 * Orquestador de la Fila 1 (adjuntos). Duenio de [AttachMenuState]; la vista
 * ([AttachStripView]) es pasiva y se re-renderiza por transicion.
 *
 * ── Modelo de fila FIJA (decision David 2026-08-22, Log de Decisiones:
 * "＋ / 📷 / 🎤 se separan de AnchorBar") ──────────────────────────────────
 *
 * Reemplaza el mecanismo anterior de franja colapsable con auto-colapso
 * (timeout 6s / primera tecla), que empujaba la Fila 2 hacia abajo al
 * expandirse — el bug de UX reportado en captura.
 *
 * 1. La fila existe SIEMPRE que el campo anfitrion acepta media
 *    ([ContentCommitPolicy.showsStrip]) y desaparece solo cuando el campo no
 *    la acepta o el teclado cierra. Sin timeout y sin rendimiento al tipeo.
 * 2. El menu (PRINCIPAL/MAS) vive DENTRO del alto fijo de la fila: reemplaza
 *    su contenido; nunca suma pixeles verticales.
 * 3. 📷 y 🎤 del extremo derecho son capa visual nuestra, decorativos hoy
 *    (toast "prximamente"); NO controlan los iconos que WhatsApp dibuja en
 *    su propio compositor.
 *
 * El costo vertical de esta fila es ALTO NUEVO (a diferencia de strip_container,
 * que ya existia vacio cuando se midio el 2026-08-11). Medido en dispositivo
 * real el 2026-08-22 — ver commit y reporte; pendiente repetir en gama baja
 * (caso critico 640dp).
 *
 * ── Insercion del adjunto (sin cambios respecto al diseno previo) ─────────
 *
 * Camino principal: InputConnection.commitContent — probado contra WhatsApp
 * Business (com.whatsapp.w4b): acepta image/png con URI propio de MediaStore.
 * Sharesheet SOLO como fallback ([ContentCommitPolicy.route]).
 *
 * Camara/fotos/documento necesitan RESULTADO de una activity; un IME no
 * recibe onActivityResult, por eso existe [AttachPickActivity] (trampolin).
 */
class AttachHost(private val ime: SalesIME) : AttachStripView.Callbacks {

    private var stripView: AttachStripView? = null
    private var state = AttachMenuState()
    private var franjaHeightPx = 0

    private var editorMimes: List<String>? = null
    private var pendingCameraUri: Uri? = null

    private val executor: ExecutorService = Executors.newSingleThreadExecutor()
    private val mainHandler = Handler(Looper.getMainLooper())

    // ------------------------------------------------------------------
    // Ciclo de vida (llamado desde SalesIME)
    // ------------------------------------------------------------------

    /** Inyecta la fila ENCIMA de strip_container (indice 0 del frame raiz). */
    fun onInputView(view: View) {
        val stripContainer = view.findViewById<FrameLayout>(R.id.strip_container) ?: return
        val root = stripContainer.parent as? ViewGroup ?: return
        // Si el root ya trae fila (vista recreada sobre el mismo arbol), se reusa.
        val existing = root.getChildAt(0) as? AttachStripView
        val strip = existing ?: AttachStripView(view.context).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                view.resources.getDimensionPixelSize(R.dimen.kb_bar_height),
            )
            visibility = View.GONE
        }
        franjaHeightPx = view.resources.getDimensionPixelSize(R.dimen.kb_bar_height)
        strip.callbacks = this
        stripView = strip
        if (existing == null) root.addView(strip, 0)
        Log.d(TAG, "fila 1 inyectada encima de strip_container (reusada=${existing != null})")
    }

    fun onFieldFocused(editorInfo: EditorInfo?) {
        editorMimes = editorInfo?.contentMimeTypes?.toList()
        android.util.Log.i(
            TAG,
            "onFieldFocused pkg=${editorInfo?.packageName} " +
                "mimes=${editorMimes?.joinToString(prefix = "[", postfix = "]") ?: "null"} " +
                "muestraFila=${ContentCommitPolicy.showsStrip(editorMimes)}",
        )
        val accepts = ContentCommitPolicy.showsStrip(editorMimes)
        setState(state.onFieldFocused(accepts))
    }

    fun onFinishInput() {
        setState(state.hide())
    }

    /**
     * Exclusion mutua con las superficies ☰/✨ ya no aplica: la fila es fija,
     * parte del cromo del teclado (patron Grammarly), convive con ellas.
     */

    // ------------------------------------------------------------------
    // Callbacks de la fila
    // ------------------------------------------------------------------

    override fun onPlus() = setState(state.onPlusTapped())

    override fun onMore() = setState(state.onMoreTapped())

    override fun onBack() = setState(state.onBackTapped())

    /** ✕: cierra el MENU y vuelve al estado de reposo. No oculta la fila. */
    override fun onCloseMenu() = setState(state.closeMenu())

    override fun onLocation() {
        toast(R.string.plus_coming_soon)
    }

    override fun onContact() {
        toast(R.string.plus_coming_soon)
    }

    override fun onCameraIcon() {
        toast(R.string.plus_coming_soon)
    }

    override fun onMicIcon() {
        toast(R.string.plus_coming_soon)
    }

    override fun onDocument() {
        beginPick(AttachPickActivity.WHICH_DOCUMENT)
    }

    override fun onPhotos() {
        beginPick(AttachPickActivity.WHICH_PHOTOS)
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
     * Lanza el trampolin SIN colapsar nada: el estado del menu persiste
     * durante el ida-y-vuelta (el trampolin cubre la pantalla). En exito,
     * deliverContent vuelve al reposo (ROOT); en cancelacion, la maquina
     * restaura PRINCIPAL.
     */
    private fun beginPick(which: String, outputUri: Uri? = null) {
        val context = ime.applicationContext
        val trampoline = Intent(context, AttachPickActivity::class.java).apply {
            putExtra(AttachPickActivity.EXTRA_WHICH, which)
            outputUri?.let { putExtra(AttachPickActivity.EXTRA_OUTPUT_URI, it) }
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_NO_ANIMATION)
        }
        AttachPickActivity.setHandler { requestCode, resultCode, data ->
            onPickResult(requestCode, resultCode, data)
        }
        try {
            context.startActivity(trampoline)
        } catch (e: Exception) {
            Log.w(TAG, "trampolin no lanzable: $e")
            reopenMenuAfterCancel()
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
        val mimes = ime.currentInputEditorInfo?.contentMimeTypes?.toList() ?: editorMimes
        val ic = ime.currentInputConnection
        val route = ContentCommitPolicy.route(mimes, mime)

        val committed = route == ContentCommitPolicy.Route.COMMIT_CONTENT &&
            ic != null &&
            commitContent(ic, uri, imeOwnsUri, mime)
        Log.i(TAG, "adjunto mime=$mime ruta=$route committed=$committed")

        if (!committed) shareFallback(uri, mime)
        setState(state.closeMenu())
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
        android.util.Log.i(TAG, "fila ${state.panel} -> ${next.panel}")
        state = next
        val strip = stripView ?: return
        strip.render(next.panel)
        strip.visibility = if (next.isVisible) View.VISIBLE else View.GONE
    }

    private fun toast(res: Int) {
        Toast.makeText(ime.applicationContext, res, Toast.LENGTH_SHORT).show()
    }

    /**
     * Alto extra permanente que la Fila 1 agrega sobre el teclado estandar,
     * en px; 0 si la fila esta oculta (campo sin media). Lo consume
     * SalesIME.onComputeInsets para corregir los insets que upstream calcula
     * sin conocer la fila — misma correccion validada en dispositivo para el
     * mecanismo colapsable, ahora con condicion de visibilidad fija.
     */
    fun expansionInsetPx(): Int =
        if (state.isVisible && franjaHeightPx > 0) franjaHeightPx else 0

    private companion object {
        const val TAG = "AttachPlus"
    }
}
