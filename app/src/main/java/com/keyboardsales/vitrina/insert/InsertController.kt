package com.keyboardsales.vitrina.insert

import android.view.inputmethod.InputConnection
import com.keyboardsales.ime.SalesIME

/**
 * Insercion del mensaje en el campo del chat y deshacer.
 *
 * Todo en UNA transaccion (plan paso 5): beginBatchEdit -> borra el disparador
 * (#termino o /atajo) -> commitText -> endBatchEdit. Asi el editor recibe un
 * solo cambio compuesto y el IME no queda en medio de un estado a medio aplicar.
 *
 * [deleteLength] lo calcula TriggerDetector.deleteLength: los caracteres desde
 * el inicio del disparador hasta el cursor, incluyendo un espacio final si
 * quedó ("Hola #mesa " -> borra "#mesa " y queda "Hola " + mensaje).
 */
object InsertController {

    fun insert(ime: SalesIME, deleteLength: Int, message: String): Int {
        val connection = ime.getCurrentInputConnection() ?: return -1
        val ok = connection.runBatchEdit {
            deleteSurroundingText(deleteLength, 0)
            commitText(message, 1)
        }
        return if (ok) message.length else -1
    }

    /** Borra el mensaje recien insertado (el cursor sigue justo despues). */
    fun undo(ime: SalesIME, insertedLength: Int): Boolean {
        val connection = ime.getCurrentInputConnection() ?: return false
        return connection.runBatchEdit {
            deleteSurroundingText(insertedLength, 0)
        }
    }

    private inline fun InputConnection.runBatchEdit(block: InputConnection.() -> Boolean): Boolean {
        beginBatchEdit()
        val result = block()
        endBatchEdit()
        return result
    }
}