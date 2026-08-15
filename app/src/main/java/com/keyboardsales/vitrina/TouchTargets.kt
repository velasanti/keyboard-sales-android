package com.keyboardsales.vitrina

import android.graphics.Rect
import android.view.TouchDelegate
import android.view.View
import android.view.ViewGroup

/**
 * Expande el area tactil real de [child] al menos a [minSizePx] (regla 8:
 * 48dp para todo lo que no es una tecla), sin cambiar su tamaño visual.
 *
 * TouchDelegate se fija en el padre y solo se consulta cuando ningun otro
 * hijo consume el toque; depende del layout en runtime, por eso no tiene
 * cobertura unitaria.
 */
fun ViewGroup.expandTouchTarget(child: View, minSizePx: Int) {
    child.post {
        val rect = Rect()
        child.getHitRect(rect)
        val dx = (minSizePx - rect.width()).coerceAtLeast(0) / 2
        val dy = (minSizePx - rect.height()).coerceAtLeast(0) / 2
        rect.left -= dx
        rect.top -= dy
        rect.right += dx
        rect.bottom += dy
        touchDelegate = TouchDelegate(rect, child)
    }
}