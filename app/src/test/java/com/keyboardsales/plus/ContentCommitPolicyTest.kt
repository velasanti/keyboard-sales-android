package com.keyboardsales.plus

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ContentCommitPolicyTest {

    // --------------------------------------------------------------
    // route
    // --------------------------------------------------------------

    @Test
    fun `sin mimes declarados va por sharesheet`() {
        assertEquals(
            ContentCommitPolicy.Route.SHARE_SHEET,
            ContentCommitPolicy.route(null, "image/jpeg"),
        )
    }

    @Test
    fun `mimes vacios van por sharesheet`() {
        assertEquals(
            ContentCommitPolicy.Route.SHARE_SHEET,
            ContentCommitPolicy.route(emptyList(), "image/jpeg"),
        )
    }

    @Test
    fun `comodin de imagen acepta jpeg`() {
        assertEquals(
            ContentCommitPolicy.Route.COMMIT_CONTENT,
            ContentCommitPolicy.route(listOf("image/*"), "image/jpeg"),
        )
    }

    @Test
    fun `mime exacto distinto va por sharesheet`() {
        assertEquals(
            ContentCommitPolicy.Route.SHARE_SHEET,
            ContentCommitPolicy.route(listOf("image/png"), "image/jpeg"),
        )
    }

    @Test
    fun `comodin total acepta pdf`() {
        assertEquals(
            ContentCommitPolicy.Route.COMMIT_CONTENT,
            ContentCommitPolicy.route(listOf("*/*"), "application/pdf"),
        )
    }

    @Test
    fun `pdf contra lista que solo acepta imagenes va por sharesheet`() {
        assertEquals(
            ContentCommitPolicy.Route.SHARE_SHEET,
            ContentCommitPolicy.route(listOf("image/png", "image/jpeg"), "application/pdf"),
        )
    }

    @Test
    fun `la comparacion ignora mayusculas`() {
        assertEquals(
            ContentCommitPolicy.Route.COMMIT_CONTENT,
            ContentCommitPolicy.route(listOf("IMAGE/JPEG"), "Image/Jpeg"),
        )
    }

    @Test
    fun `un mime compatible entre varios alcanza`() {
        assertEquals(
            ContentCommitPolicy.Route.COMMIT_CONTENT,
            ContentCommitPolicy.route(listOf("text/plain", "image/*"), "image/webp"),
        )
    }

    // --------------------------------------------------------------
    // showsStrip (visibilidad de la franja)
    // --------------------------------------------------------------

    @Test
    fun `campo sin mimes nunca muestra la franja`() {
        assertFalse(ContentCommitPolicy.showsStrip(null))
        assertFalse(ContentCommitPolicy.showsStrip(emptyList()))
    }

    @Test
    fun `campo que acepta imagenes muestra la franja`() {
        assertTrue(ContentCommitPolicy.showsStrip(listOf("image/*")))
        assertTrue(ContentCommitPolicy.showsStrip(listOf("*/*")))
        assertTrue(ContentCommitPolicy.showsStrip(listOf("image/jpeg")))
    }

    @Test
    fun `campo de texto plano no muestra la franja`() {
        assertFalse(ContentCommitPolicy.showsStrip(listOf("text/plain")))
    }

    // --------------------------------------------------------------
    // accepts (patron vs mime)
    // --------------------------------------------------------------

    @Test
    fun `asterisco barra asterisco acepta todo`() {
        assertTrue(ContentCommitPolicy.accepts("*/*", "video/mp4"))
    }

    @Test
    fun `comodin de tipo compara el prefijo con barra incluida`() {
        assertTrue(ContentCommitPolicy.accepts("image/*", "image/webp"))
        assertFalse(ContentCommitPolicy.accepts("image/*", "imaginary/jpeg"))
    }

    @Test
    fun `patron sin comodin exige igualdad exacta`() {
        assertTrue(ContentCommitPolicy.accepts("application/pdf", "application/pdf"))
        assertFalse(ContentCommitPolicy.accepts("application/pdf", "application/octet-stream"))
    }

    // --------------------------------------------------------------
    // commitFlags
    // --------------------------------------------------------------

    @Test
    fun `uri propio no pide permiso`() {
        assertEquals(0, ContentCommitPolicy.commitFlags(imeOwnsUri = true))
    }

    @Test
    fun `uri ajeno pide permiso de lectura`() {
        assertEquals(
            ContentCommitPolicy.FLAG_GRANT_READ,
            ContentCommitPolicy.commitFlags(imeOwnsUri = false),
        )
    }
}
