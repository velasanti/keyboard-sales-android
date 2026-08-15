package com.keyboardsales.vitrina.data

import com.keyboardsales.vitrina.search.TextNormalizer
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class CatalogDummyFileTest {

    private val json = Json { ignoreUnknownKeys = true }

    private fun load(): CatalogDummyFile {
        val file = File("../design/catalog-dummy.json")
        assertTrue("no encuentro design/catalog-dummy.json desde app/", file.exists())
        return json.decodeFromString(file.readText())
    }

    private fun searchable(item: CatalogDummyItem): String =
        TextNormalizer.normalize(listOf(item.nombre, item.categoria, *item.alias.toTypedArray()).joinToString(" "))

    @Test
    fun `diez items y cinco respuestas rapidas`() {
        val catalog = load()
        assertEquals(10, catalog.items.size)
        assertEquals(5, catalog.respuestasRapidas.size)
    }

    @Test
    fun `tenant y moneda del escenario canonico`() {
        val catalog = load()
        assertEquals("esculturas-dg", catalog.tenant)
        assertEquals("COP", catalog.monedaDefault)
    }

    @Test
    fun `el alias sin letras comunes entra en la busqueda (la comoda)`() {
        val poltrona = load().items.first { it.id == "edg-poltrona-chicamocha" }
        val searchable = searchable(poltrona)
        assertTrue(searchable.contains("comoda"))
        assertTrue(searchable.contains("chicamocha"))
    }

    @Test
    fun `el alias sin letras comunes entra en la busqueda (la chiquita)`() {
        val zipa = load().items.first { it.id == "edg-mesa-auxiliar-zipa" }
        val searchable = searchable(zipa)
        assertTrue(searchable.contains("chiquita"))
        assertTrue(searchable.contains("auxiliar"))
    }

    @Test
    fun `los chips gemelos no colapsan en la busqueda`() {
        val catalog = load()
        val g180 = catalog.items.first { it.id == "edg-mesa-granada-nogal-180" }
        val g220 = catalog.items.first { it.id == "edg-mesa-granada-nogal-220" }
        val s180 = searchable(g180)
        val s220 = searchable(g220)
        assertTrue(s180 != s220)
        assertTrue(s180.startsWith("mesa granada nogal"))
        assertTrue(s220.startsWith("mesa granada nogal"))
    }

    @Test
    fun `la busqueda de cada item contiene nombre y categoria normalizados`() {
        val catalog = load()
        for (item in catalog.items) {
            val searchable = searchable(item)
            assertTrue(
                "busqueda de ${item.id} debe contener el nombre normalizado",
                searchable.contains(TextNormalizer.normalize(item.nombre)),
            )
            assertTrue(
                "busqueda de ${item.id} debe contener la categoria normalizada",
                searchable.contains(TextNormalizer.normalize(item.categoria)),
            )
        }
    }
}