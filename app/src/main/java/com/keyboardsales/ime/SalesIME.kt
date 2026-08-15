package com.keyboardsales.ime

import android.view.View
import android.view.inputmethod.EditorInfo
import com.keyboardsales.vitrina.VitrinaHost
import helium314.keyboard.latin.LatinIME

/**
 * IME del producto. Es una subclase aditiva de [LatinIME]: todo el codigo de
 * Vitrina se engancha aca por override, sin tocar el motor de sugerencias.
 * La unica edicion a upstream que exige es el `android:name` del servicio en
 * AndroidManifest.xml (ver docs/UPSTREAM.md).
 */
class SalesIME : LatinIME() {

    private lateinit var vitrinaHost: VitrinaHost

    override fun onCreate() {
        super.onCreate()
        vitrinaHost = VitrinaHost(this)
    }

    override fun onCreateInputView(): View {
        return super.onCreateInputView()
    }

    override fun setInputView(view: View) {
        super.setInputView(view)
        vitrinaHost.onInputView(view)
    }

    override fun onStartInputView(editorInfo: EditorInfo?, restarting: Boolean) {
        super.onStartInputView(editorInfo, restarting)
        vitrinaHost.onStartInputView(editorInfo, restarting)
    }

    override fun onFinishInputView(finishingInput: Boolean) {
        super.onFinishInputView(finishingInput)
        vitrinaHost.onFinishInputView(finishingInput)
    }

    override fun onUpdateSelection(
        oldSelStart: Int,
        oldSelEnd: Int,
        newSelStart: Int,
        newSelEnd: Int,
        composingSpanStart: Int,
        composingSpanEnd: Int,
    ) {
        super.onUpdateSelection(oldSelStart, oldSelEnd, newSelStart, newSelEnd, composingSpanStart, composingSpanEnd)
        vitrinaHost.onUpdateSelection(oldSelStart, oldSelEnd, newSelStart, newSelEnd, composingSpanStart, composingSpanEnd)
    }
}