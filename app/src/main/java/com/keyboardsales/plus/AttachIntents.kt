package com.keyboardsales.plus

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.MediaStore

/**
 * Builders de intents de adjuntos, en un solo lugar porque los consumen DOS
 * lados: AttachHost (para verificar resolucion antes de lanzar) y
 * AttachPickActivity (para disparar el picker con startActivityForResult).
 */
object AttachIntents {

    const val REQUEST_CAMERA = 4101
    const val REQUEST_PHOTOS = 4102
    const val REQUEST_DOCUMENT = 4103

    /**
     * Camara: captura con salida directa al MediaStore (sin permisos de
     * almacenamiento: el URI lo inserta nuestra app). output nulo solo si el
     * caller no pudo preparar el URI; la captura vuelve por extra thumbnail,
     * camino que NO usamos — el caller lo trata como cancelacion.
     */
    fun camera(output: Uri?): Intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE).apply {
        if (output != null) putExtra(MediaStore.EXTRA_OUTPUT, output)
        addFlags(
            Intent.FLAG_GRANT_WRITE_URI_PERMISSION or
                Intent.FLAG_GRANT_READ_URI_PERMISSION,
        )
    }

    /**
     * Fotos: photo picker del sistema en API 33+, ACTION_PICK clasico antes.
     * Los dos devuelven content:// con permiso de lectura temporal y no
     * requieren READ_EXTERNAL_STORAGE.
     */
    fun photos(): Intent {
        if (Build.VERSION.SDK_INT >= 33) {
            return Intent("android.intent.action.PICK_IMAGES").apply { type = "image/*" }
        }
        return Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
    }

    /** Documento: file picker generico (decision cerrada: cualquier archivo del dispositivo). */
    fun document(): Intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
        addCategory(Intent.CATEGORY_OPENABLE)
        type = "*/*"
        putExtra(Intent.EXTRA_LOCAL_ONLY, true)
    }

    /** False si ninguna app sabe manejar el intent (p. ej. sin camara). */
    fun resolves(context: Context, intent: Intent): Boolean =
        intent.resolveActivity(context.packageManager) != null
}
