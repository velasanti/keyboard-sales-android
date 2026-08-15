package com.keyboardsales.vitrina

import android.view.View
import android.view.inputmethod.EditorInfo
import com.keyboardsales.ime.SalesIME
import helium314.keyboard.latin.R
import helium314.keyboard.latin.utils.Log

/**
 * Punto unico de contacto entre [SalesIME] y las dos superficies de Vitrina.
 *
 * Toda la escritura a la jerarquia de vistas es aditiva: aca se encuentran por
 * id los contenedores que upstream ya infla (leer ids no es editar) y las
 * superficies propias se agregan como hijos en runtime. Ningun archivo de
 * upstream se modifica salvo AndroidManifest.xml.
 */
class VitrinaHost(private val ime: SalesIME) {

    private var stripContainer: View? = null
    private var keyboardViewWrapper: View? = null

    fun onInputView(view: View) {
        stripContainer = view.findViewById(R.id.strip_container)
        keyboardViewWrapper = view.findViewById(R.id.keyboard_view_wrapper)
        Log.d(TAG, "onInputView: strip=${stripContainer != null}, wrapper=${keyboardViewWrapper != null}")
    }

    fun onStartInputView(editorInfo: EditorInfo?, restarting: Boolean) {
        Log.d(TAG, "onStartInputView(restarting=$restarting)")
    }

    fun onFinishInputView(finishingInput: Boolean) {
        Log.d(TAG, "onFinishInputView(finishingInput=$finishingInput)")
    }

    fun onUpdateSelection(
        oldSelStart: Int,
        oldSelEnd: Int,
        newSelStart: Int,
        newSelEnd: Int,
        composingSpanStart: Int,
        composingSpanEnd: Int,
    ) {
        Log.d(
            TAG,
            "onUpdateSelection: nss=$newSelStart nse=$newSelEnd cs=$composingSpanStart ce=$composingSpanEnd",
        )
    }

    companion object {
        private const val TAG = "Vitrina"
    }
}