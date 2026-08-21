package com.keyboardsales.vitrina

import android.graphics.Rect
import android.view.MotionEvent
import android.view.TouchDelegate
import android.view.View

/**
 * Delegado tactil COMPUESTO: un ViewGroup solo tiene un touchDelegate, asi
 * que [expandTouchTarget] (un hijo por llamada) pisa al anterior cuando hay
 * varios controles hermanos que necesitan area tactil expandida (regla 8).
 *
 * Mantiene N rects y reenvia cada toque al hijo correspondiente con el mismo
 * contrato que TouchDelegate nativo: coordenadas trasladadas al espacio del
 * hijo, y el gesto completo (DOWN..UP) va SIEMPRE al hijo que capturo el DOWN,
 * aunque el dedo se deslice fuera del rect mientras dura la pulsacion.
 *
 * Lo consume la franja de adjuntos (+); las anclas ☰/✦ siguen en el mecanismo
 * simple hasta revisarlas (hallazgo aparte: ahi el segundo expandTouchTarget
 * pisa al primero).
 */
class CompositeTouchDelegate(
    private val parent: View,
    private val targets: MutableList<Pair<Rect, View>> = mutableListOf(),
) : TouchDelegate(Rect(), parent) {

    private var activeChild: View? = null

    /** Vacia los registros (llamar al reconstruir los hijos de la fila). */
    fun clear() {
        synchronized(targets) { targets.clear() }
        activeChild = null
    }

    /** Registra/expande el hijo y reconstruye el delegado del padre. */
    fun add(child: View, minSizePx: Int) {
        child.post {
            val rect = Rect()
            child.getHitRect(rect)
            val dx = (minSizePx - rect.width()).coerceAtLeast(0) / 2
            val dy = (minSizePx - rect.height()).coerceAtLeast(0) / 2
            rect.left -= dx
            rect.top -= dy
            rect.right += dx
            rect.bottom += dy
            synchronized(targets) {
                targets.removeAll { it.second == child }
                targets.add(rect to child)
            }
            parent.touchDelegate = this
        }
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        val actionMasked = event.actionMasked
        val child = when {
            actionMasked == MotionEvent.ACTION_DOWN -> {
                val hit = findTargetAt(event.x.toInt(), event.y.toInt())
                activeChild = hit
                hit
            }
            actionMasked == MotionEvent.ACTION_CANCEL -> {
                val hit = activeChild
                activeChild = null
                hit
            }
            else -> activeChild
        } ?: return false

        val rect = rectFor(child) ?: return false
        // Igual que el TouchDelegate nativo: el hijo ve el toque como si
        // ocurriera dentro de sus propios limites.
        val offsetX = (rect.left - child.left).toFloat()
        val offsetY = (rect.top - child.top).toFloat()
        event.setLocation(event.x - offsetX, event.y - offsetY)
        val handled = child.dispatchTouchEvent(event)
        if (actionMasked == MotionEvent.ACTION_UP || actionMasked == MotionEvent.ACTION_CANCEL) {
            activeChild = null
        }
        return handled
    }

    private fun findTargetAt(x: Int, y: Int): View? = synchronized(targets) {
        targets.firstOrNull { (rect, _) -> rect.contains(x, y) }?.second
    }

    private fun rectFor(child: View): Rect? = synchronized(targets) {
        targets.firstOrNull { it.second == child }?.first
    }
}
