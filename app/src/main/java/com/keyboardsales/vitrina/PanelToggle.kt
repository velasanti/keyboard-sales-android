package com.keyboardsales.vitrina

/**
 * Que hace el tap del chip o del ancla de Vitrina segun el estado actual
 * (04.10: capa y modo se alternan; el chip y el ancla comparten la entrada).
 * Puro para poder testearlo sin Robolectric.
 */
object PanelToggle {
    fun next(panelVisible: Boolean): PanelAction =
        if (panelVisible) PanelAction.HIDE else PanelAction.SHOW
}

enum class PanelAction { SHOW, HIDE }