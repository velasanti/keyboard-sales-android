package com.keyboardsales.vitrina.search

import org.junit.Assert.assertEquals
import org.junit.Test

class TriggerDetectorTest {

    @Test
    fun `texto vacio no dispara`() {
        assertEquals(VitrinaTrigger.None, TriggerDetector.detect(""))
        assertEquals(VitrinaTrigger.None, TriggerDetector.detect("   "))
    }

    @Test
    fun `texto sin marcadores no dispara`() {
        assertEquals(VitrinaTrigger.None, TriggerDetector.detect("Hola"))
        assertEquals(VitrinaTrigger.None, TriggerDetector.detect("¿Cómo estás?"))
    }

    @Test
    fun `producto simple`() {
        assertEquals(VitrinaTrigger.Product("mesa"), TriggerDetector.detect("Hola #mesa"))
        assertEquals(VitrinaTrigger.Product(""), TriggerDetector.detect("Hola #"))
    }

    @Test
    fun `producto con espacio al final sigue activo`() {
        assertEquals(VitrinaTrigger.Product("mesa"), TriggerDetector.detect("Hola #mesa "))
    }

    @Test
    fun `respuesta rapida simple`() {
        assertEquals(VitrinaTrigger.QuickReply("garantia"), TriggerDetector.detect("Hola /garantia"))
        assertEquals(VitrinaTrigger.QuickReply(""), TriggerDetector.detect("/"))
    }

    @Test
    fun `marcador a mitad de palabra no dispara`() {
        assertEquals(VitrinaTrigger.None, TriggerDetector.detect("hola@algo#tag"))
        assertEquals(VitrinaTrigger.None, TriggerDetector.detect("www.site.com/ruta"))
    }

    @Test
    fun `el termino es una sola palabra`() {
        // Supuesto 04.8: un espacio cierra el disparador.
        assertEquals(VitrinaTrigger.None, TriggerDetector.detect("Hola #mesa granada"))
        assertEquals(VitrinaTrigger.None, TriggerDetector.detect("#granada 180"))
    }

    @Test
    fun `longitud a borrar cubre el disparador completo`() {
        val t1 = TriggerDetector.detect("Hola #mesa")
        assertEquals(5, TriggerDetector.deleteLength("Hola #mesa", t1))
        val t2 = TriggerDetector.detect("Hola #mesa ")
        assertEquals(6, TriggerDetector.deleteLength("Hola #mesa ", t2))
        val t3 = TriggerDetector.detect("#granada")
        assertEquals(8, TriggerDetector.deleteLength("#granada", t3))
        val t4 = TriggerDetector.detect("x /envio")
        assertEquals(6, TriggerDetector.deleteLength("x /envio", t4))
    }

    @Test
    fun `sin disparador no se borra nada`() {
        assertEquals(0, TriggerDetector.deleteLength("Hola", VitrinaTrigger.None))
    }
}