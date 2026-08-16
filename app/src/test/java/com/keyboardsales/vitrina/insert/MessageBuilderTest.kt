package com.keyboardsales.vitrina.insert

import com.keyboardsales.vitrina.data.CatalogItem
import com.keyboardsales.vitrina.data.QuickReply
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MessageBuilderTest {

    private val poltrona = CatalogItem(
        id = "edg-poltrona-chicamocha",
        nombre = "Poltrona Chicamocha",
        categoria = "Poltronas",
        precio = 1_290_000,
        moneda = "COP",
        url = "https://catalogo.esculturasdg.invalid/p/poltrona-chicamocha?v=3",
        imagen = "poltrona-chicamocha.webp",
        actualizadoEn = "2026-08-07T08:30:00Z",
        busqueda = "poltrona chicamocha",
    )

    @Test
    fun `mensaje de producto lleva una sola URL`() {
        val message = MessageBuilder.productMessage(poltrona)
        val urls = Regex("https?://\\S+").findAll(message).count()
        assertEquals("ADR-017: una sola URL por mensaje", 1, urls)
    }

    @Test
    fun `mensaje de producto tiene nombre precio y url`() {
        val message = MessageBuilder.productMessage(poltrona)
        assertTrue(message.contains("Poltrona Chicamocha"))
        assertTrue(message.contains("$1.290.000 COP"))
        assertTrue(message.contains(poltrona.url))
    }

    @Test
    fun `la URL va en su propia linea`() {
        val message = MessageBuilder.productMessage(poltrona)
        val lines = message.lines()
        assertEquals(poltrona.url, lines.last())
        assertTrue(lines.size >= 2)
    }

    @Test
    fun `respuesta rapida es el texto tal cual, sin URL`() {
        val reply = QuickReply("rr-horario", "horario", "Atendemos de lunes a viernes.")
        val message = MessageBuilder.quickReplyMessage(reply)
        assertEquals("Atendemos de lunes a viernes.", message)
        assertEquals(0, Regex("https?://\\S+").findAll(message).count())
    }
}