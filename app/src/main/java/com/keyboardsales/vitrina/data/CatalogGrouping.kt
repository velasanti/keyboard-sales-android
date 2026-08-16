package com.keyboardsales.vitrina.data

/** Agrupaciones puras sobre el catalogo, sin dependencias de Android. */
object CatalogGrouping {

    /**
     * Agrupa por [CatalogItem.categoria] preservando el orden de aparicion: la
     * primera vez que se ve una categoria en la entrada define su posicion en
     * el resultado. Dentro de cada grupo los items conservan el orden de
     * entrada (ya vienen ordenados por nombre desde selectAllItems).
     */
    fun byCategory(items: List<CatalogItem>): List<Pair<String, List<CatalogItem>>> {
        val groups = LinkedHashMap<String, MutableList<CatalogItem>>()
        for (item in items) {
            groups.getOrPut(item.categoria) { mutableListOf() }.add(item)
        }
        return groups.map { (categoria, grouped) -> categoria to grouped }
    }
}