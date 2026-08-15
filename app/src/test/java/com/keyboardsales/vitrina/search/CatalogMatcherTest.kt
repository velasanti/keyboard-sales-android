package com.keyboardsales.vitrina.search

import com.keyboardsales.vitrina.data.CatalogDummyFile
import com.keyboardsales.vitrina.data.CatalogItem
import com.keyboardsales.vitrina.data.CatalogDummyItem
import com.keyboardsales.vitrina.data.QuickReply
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.io.File

class CatalogMatcherTest {

    private lateinit var items: List<CatalogItem>
    private lateinit var replies: List<QuickReply>

    @Before
    fun setUp() {
        val json = Json { ignoreUnknownKeys = true }
        val file = File("../design/catalog-dummy.json")
        assertTrue("no encuentro design/catalog-dummy.json desde app/", file.exists())
        val catalog: CatalogDummyFile = json.decodeFromString(file.readText())
        items = catalog.items.map { it.toCatalogItem() }
        replies = catalog.respuestasRapidas.map { QuickReply(it.id, it.atajo, it.texto) }
    }

    // items 1 y 2: prefijo colisionante y truncado identico en el chip.
    @Test
    fun `los chips gemelos coexisten y van primero`() {
        val result = CatalogMatcher.matchItems(items, "Mesa Granada")
        assertEquals(2, result.size)
        val ids = result.map { it.id }.toSet()
        assertTrue(ids.contains("edg-mesa-granada-nogal-180"))
        assertTrue(ids.contains("edg-mesa-granada-nogal-220"))
    }

    @Test
    fun `el 180 no colapsa con el 220 en la busqueda`() {
        val result = CatalogMatcher.matchItems(items, "granada 180")
        assertEquals(1, result.size)
        assertEquals("edg-mesa-granada-nogal-180", result.first().id)
    }

    // item 3: mismo prefijo que 1 y 2 pero otra categoria.
    @Test
    fun `la banca granada aparece junto a las mesas en multi-categoria`() {
        val result = CatalogMatcher.matchItems(items, "granada")
        val ids = result.map { it.id }.toSet()
        assertTrue(ids.contains("edg-mesa-granada-nogal-180"))
        assertTrue(ids.contains("edg-mesa-granada-nogal-220"))
        assertTrue(ids.contains("edg-banca-granada-roble"))
    }

    // items 7 y 8: alias que no comparten ninguna letra con el nombre.
    @Test
    fun `la comoda encuentra la poltrona por alias`() {
        val result = CatalogMatcher.matchItems(items, "comoda")
        assertEquals(1, result.size)
        assertEquals("edg-poltrona-chicamocha", result.first().id)
    }

    @Test
    fun `la chiquita encuentra la mesa auxiliar por alias`() {
        val result = CatalogMatcher.matchItems(items, "chiquita")
        assertEquals(1, result.size)
        assertEquals("edg-mesa-auxiliar-zipa", result.first().id)
    }

    @Test
    fun `mesita como alias`() {
        val result = CatalogMatcher.matchItems(items, "mesita")
        assertEquals(1, result.size)
        assertEquals("edg-mesa-auxiliar-zipa", result.first().id)
    }

    // item 6: sin imagen, matchea normal por nombre/categoria.
    @Test
    fun `la repisa sin imagen matchea por categoria`() {
        val result = CatalogMatcher.matchItems(items, "repisa")
        assertTrue(result.map { it.id }.contains("edg-repisa-monserrate"))
    }

    // item 9: precio viejo, matchea por nombre.
    @Test
    fun `el comedor con precio viejo matchea por nombre`() {
        val result = CatalogMatcher.matchItems(items, "comedor")
        assertTrue(result.map { it.id }.contains("edg-comedor-boyaca-8"))
    }

    // item 10: nombre largo, matchea por nombre y por alias.
    @Test
    fun `la biblioteca de nombre largo matchea por nombre`() {
        val result = CatalogMatcher.matchItems(items, "biblioteca")
        assertTrue(result.map { it.id }.contains("edg-biblioteca-catedral-modular"))
    }

    @Test
    fun `la biblioteca matchea por alias catedral`() {
        val result = CatalogMatcher.matchItems(items, "catedral")
        assertEquals(1, result.size)
        assertEquals("edg-biblioteca-catedral-modular", result.first().id)
    }

    @Test
    fun `el prefijo solitario del ancla no matchea nada`() {
        assertEquals(0, CatalogMatcher.matchItems(items, "").size)
        assertEquals(0, CatalogMatcher.matchItems(items, "   ").size)
    }

    @Test
    fun `sin coincidencias devuelve vacio`() {
        assertEquals(0, CatalogMatcher.matchItems(items, "mesa inexistente 999").size)
    }

    @Test
    fun `el alias de la poltrona no roba la busqueda por nombre`() {
        val result = CatalogMatcher.matchItems(items, "poltrona")
        assertEquals(1, result.size)
        assertEquals("edg-poltrona-chicamocha", result.first().id)
    }

    // --- respuestas rapidas ---

    @Test
    fun `atajo exacto de respuesta rapida`() {
        val result = CatalogMatcher.matchQuickReplies(replies, "horario")
        assertEquals(1, result.size)
        assertEquals("rr-horario", result.first().id)
    }

    @Test
    fun `prefijo de atajo de respuesta rapida`() {
        val result = CatalogMatcher.matchQuickReplies(replies, "garantia")
        assertEquals(1, result.size)
        assertEquals("rr-garantia", result.first().id)
    }

    @Test
    fun `respuestas rapidas sin coincidencia devuelven vacio`() {
        assertEquals(0, CatalogMatcher.matchQuickReplies(replies, "nada").size)
    }

    @Test
    fun `respuestas rapidas vacias devuelven vacio`() {
        assertEquals(0, CatalogMatcher.matchQuickReplies(replies, "").size)
    }
}

private fun CatalogDummyItem.toCatalogItem() = CatalogItem(
    id = id,
    nombre = nombre,
    categoria = categoria,
    precio = precio,
    moneda = moneda,
    url = url,
    imagen = imagen,
    actualizadoEn = actualizadoEn,
    busqueda = TextNormalizer.normalize(
        listOf(nombre, categoria, *alias.toTypedArray()).joinToString(" "),
    ),
)