package com.keyboardsales.plus

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import helium314.keyboard.latin.utils.Log

/**
 * Activity trampolin (translucida) para obtener RESULTADOS de pickers desde
 * un IME. Un InputMethodService es un Service: puede startActivity pero no
 * recibe onActivityResult; esta activity aloja la llamada y entrega el
 * resultado al host via registro estatico.
 *
 * Patron ya probado en este repo: QuoteShare lanza actividades desde el IME
 * con FLAG_ACTIVITY_NEW_TASK sin problemas.
 *
 * Ciclo: AttachHost la lanza con EXTRA_WHICH (+ EXTRA_OUTPUT_URI para la
 * camara) -> onCreate dispara el picker real con startActivityForResult ->
 * onActivityResult reenvia (requestCode, resultCode, data) al handler y
 * termina. Sin UI propia: el tema es Translucent.NoTitleBar.
 *
 * Recreacion por configuracion NO relanza el picker: si hay
 * savedInstanceState, se termina sin entregar.
 */
class AttachPickActivity : Activity() {

    companion object {
        private const val TAG = "AttachPlus"

        const val EXTRA_WHICH = "ks_plus_which"
        const val EXTRA_OUTPUT_URI = "ks_plus_output_uri"
        const val WHICH_CAMERA = "camera"
        const val WHICH_PHOTOS = "photos"
        const val WHICH_DOCUMENT = "document"

        /** Registro estatico del callback del host. Uno a la vez: los flujos son secuenciales. */
        @Volatile
        private var resultHandler: ((requestCode: Int, resultCode: Int, data: Intent?) -> Unit)? = null

        fun setHandler(handler: ((Int, Int, Intent?) -> Unit)?) {
            resultHandler = handler
        }
    }

    private var resultDelivered = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (savedInstanceState != null) {
            Log.d(TAG, "trampoline recreado: termino sin relanzar")
            finish()
            return
        }
        val which = intent?.getStringExtra(EXTRA_WHICH)
        when (which) {
            WHICH_CAMERA -> launchCamera()
            WHICH_PHOTOS -> start(AttachIntents.photos(), AttachIntents.REQUEST_PHOTOS)
            WHICH_DOCUMENT -> start(AttachIntents.document(), AttachIntents.REQUEST_DOCUMENT)
            else -> {
                Log.w(TAG, "trampoline sin EXTRA_WHICH valido: $which")
                deliver(AttachIntents.REQUEST_DOCUMENT, RESULT_CANCELED, null)
            }
        }
    }

    private fun launchCamera() {
        val output = intent?.getParcelableExtra<Uri>(EXTRA_OUTPUT_URI)
        val capture = AttachIntents.camera(output)
        if (!AttachIntents.resolves(this, capture)) {
            Log.w(TAG, "sin app de camara")
            deliver(AttachIntents.REQUEST_CAMERA, RESULT_CANCELED, null)
            return
        }
        start(capture, AttachIntents.REQUEST_CAMERA)
    }

    private fun start(payload: Intent, requestCode: Int) {
        try {
            startActivityForResult(payload, requestCode)
        } catch (e: Exception) {
            Log.w(TAG, "picker no lanzable rc=$requestCode: $e")
            deliver(requestCode, RESULT_CANCELED, null)
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        deliver(requestCode, resultCode, data)
    }

    private fun deliver(requestCode: Int, resultCode: Int, data: Intent?) {
        val handler = resultHandler ?: run {
            Log.d(TAG, "entrega sin handler (proceso nuevo?): rc=$requestCode")
            return
        }
        resultHandler = null
        resultDelivered = true
        handler(requestCode, resultCode, data)
    }

    override fun onDestroy() {
        super.onDestroy()
        // Si murio sin pasar por onActivityResult (caso raro), liberar el
        // registro para no filtrar el callback hacia una instancia vieja.
        if (!resultDelivered) {
            resultHandler = null
        }
    }
}
