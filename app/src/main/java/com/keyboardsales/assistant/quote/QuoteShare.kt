package com.keyboardsales.assistant.quote

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider
import helium314.keyboard.latin.R
import java.io.File

/**
 * Comparte la cotización generada con ACTION_SEND + chooser (decisión Santi,
 * 2026-08-19): el PDF se adjunta directo desde el dispositivo, sin subida ni
 * servidor. El usuario elige la app y el chat (el IME funciona en cualquier app
 * de mensajería, no hardcodea WhatsApp).
 *
 * ADR-016: no se dispara acá — el llamador ya pasó por la tarjeta de
 * confirmación. Este objeto solo arma el intent y lo lanza.
 */
object QuoteShare {

    fun share(context: Context, file: File) {
        val uri = uriFor(context, file)
        val send = Intent(Intent.ACTION_SEND).apply {
            type = "application/pdf"
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        val chooser = Intent.createChooser(send, context.getString(R.string.quote_share_title)).apply {
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        // El contexto es el IME (un Service): sin NEW_TASK no se puede abrir la
        // actividad desde un contexto que no es Activity.
        chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(chooser)
    }

    private fun uriFor(context: Context, file: File): Uri = FileProvider.getUriForFile(
        context,
        context.getString(R.string.quote_provider_authority),
        file,
    )
}
