package com.keyboardsales.vitrina.search

import org.junit.Assert.assertEquals
import org.junit.Test

class TextNormalizerTest {

    @Test
    fun `minusculas y sin tildes en los dos lados`() {
        assertEquals("mesa granada nogal", TextNormalizer.normalize("Mesa Granada Nogal"))
        assertEquals("comedor boyaca 8 puestos", TextNormalizer.normalize("Comedor Boyacá 8 puestos"))
    }

    @Test
    fun `colapsa espacios repetidos`() {
        assertEquals("silla tunja cuero", TextNormalizer.normalize("Silla   Tunja  Cuero"))
    }

    @Test
    fun `preserva la enie como letra`() {
        // La ñ no se descompone en NFD: es letra del espanol, no diacritico.
        assertEquals("nino", TextNormalizer.normalize("niño"))
    }

    @Test
    fun `alias con tilde queda busquedable sin tilde`() {
        assertEquals(
            "poltrona chicamocha la comoda poltrona grande sillon lectura",
            TextNormalizer.normalize("Poltrona Chicamocha la comoda Poltrona grande sillón lectura"),
        )
    }

    @Test
    fun `nombre vacio y espacios solos`() {
        assertEquals("", TextNormalizer.normalize(""))
        assertEquals("", TextNormalizer.normalize("   "))
    }
}