package com.keyboardsales.plus

/**
 * Decisiones de insercion de adjuntos, puras y testeables.
 *
 * Camino principal: InputConnection.commitContent (probado contra WhatsApp
 * Business en el spike: acepta image/png declarado por el composer).
 * Sharesheet SOLO como fallback cuando el anfitrion no declara mimes
 * compatibles o rechaza el commitContent en runtime.
 */
object ContentCommitPolicy {

    enum class Route { COMMIT_CONTENT, SHARE_SHEET }

    /** Valor de InputConnection.INPUT_CONTENT_GRANT_READ_URI_PERMISSION. */
    const val FLAG_GRANT_READ = 1

    /**
     * Por donde viaja el adjunto segun los mimes que declara el editor
     * (EditorInfo.contentMimeTypes) y el mime real del archivo.
     */
    fun route(editorMimeTypes: List<String>?, contentMime: String): Route =
        if (!editorMimeTypes.isNullOrEmpty() && editorMimeTypes.any { accepts(it, contentMime) }) {
            Route.COMMIT_CONTENT
        } else {
            Route.SHARE_SHEET
        }

    /**
     * Si la franja se muestra en este campo: basta con que el editor acepte
     * imagenes (exacto, "image/*" o "*/*"). Campos sin mimes (busquedas,
     * URL, numericos) nunca la muestran.
     */
    fun showsStrip(editorMimeTypes: List<String>?): Boolean =
        !editorMimeTypes.isNullOrEmpty() && editorMimeTypes.any { accepts(it, MIME_IMAGE) }

    /**
     * Patron del editor contra mime concreto: exacto, comodin de tipo
     * ("image/*") o comodin total ("*/*"), sin distinguir mayusculas.
     */
    fun accepts(pattern: String, mime: String): Boolean = when {
        pattern.equals(WILDCARD_ALL, ignoreCase = true) -> true
        pattern.endsWith("/*", ignoreCase = true) ->
            mime.regionMatches(0, pattern, 0, pattern.length - 1, ignoreCase = true)
        else -> pattern.equals(mime, ignoreCase = true)
    }

    /**
     * Flags de commitContent: si el IME es duenio del URI (MediaStore propio,
     * camino camara) el editor ya puede leerlo; si el URI viene de un picker
     * hay que pedirle permiso de lectura explicitamente.
     */
    fun commitFlags(imeOwnsUri: Boolean): Int = if (imeOwnsUri) 0 else FLAG_GRANT_READ

    private const val WILDCARD_ALL = "*/*"
    private const val MIME_IMAGE = "image/jpeg"
}
