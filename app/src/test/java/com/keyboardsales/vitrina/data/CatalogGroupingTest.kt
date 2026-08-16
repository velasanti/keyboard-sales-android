package com.keyboardsales.vitrina.data

import org.junit.Assert.assertEquals
import org.junit.Test

class CatalogGroupingTest {

    private fun item(id: String, nombre: String, categoria: String) = CatalogItem(
        id = id,
        nombre = nombre,
        categoria = categoria,
        precio = 100_000,
        moneda = "COP",
        url = "https://catalogo.esculturasdg.invalid/p/$id",
        imagen = null,
        actualizadoEn = "2026-08-07T08:30:00Z",
        busqueda = nombre,
    )

    private fun categories(groups: List<Pair<String, List<CatalogItem>>>): List<String> =
        groups.map { it.first }

    private fun names(items: List<CatalogItem>): List<String> =
        items.map { it.nombre }

    @Test
    fun `lista vacia no produce grupos`() {
        assertEquals(emptyList<Pair<String, List<CatalogItem>>>(), CatalogGrouping.byCategory(emptyList()))
    }

    @Test
    fun `items de una sola categoria quedan en un grupo con el orden de entrada`() {
        val items = listOf(
            item("1", "Poltrona Chicamocha", "Poltronas"),
            item("2", "Poltrona Zipa", "Poltronas"),
            item("3", "Sofa Girardot", "Poltronas"),
        )

        val groups = CatalogGrouping.byCategory(items)

        assertEquals(listOf("Poltronas"), categories(groups))
        assertEquals(listOf("Poltrona Chicamocha", "Poltrona Zipa", "Sofa Girardot"), names(groups.single().second))
    }

    @Test
    fun `categorias intercaladas se agrupan por orden de primera aparicion y no alfabetico`() {
        val items = listOf(
            item("1", "Poltrona Chicamocha", "Poltronas"),
            item("2", "Mesa Auxiliar Zipa", "Mesas"),
            item("3", "Sofa Girardot", "Poltronas"),
            item("4", "Comoda Velez", "Comodas"),
            item("5", "Mesa de Centro Tunja", "Mesas"),
        )

        val groups = CatalogGrouping.byCategory(items)

        assertEquals(listOf("Poltronas", "Mesas", "Comodas"), categories(groups))
        assertEquals(listOf("Poltrona Chicamocha", "Sofa Girardot"), names(groups[0].second))
        assertEquals(listOf("Mesa Auxiliar Zipa", "Mesa de Centro Tunja"), names(groups[1].second))
        assertEquals(listOf("Comoda Velez"), names(groups[2].second))
    }
}