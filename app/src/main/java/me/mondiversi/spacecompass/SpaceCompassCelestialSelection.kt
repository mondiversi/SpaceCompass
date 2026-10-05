package me.mondiversi.spacecompass

/** Filter the scene immediately, including while cached observations/paths are refreshing. */
internal fun <T> spaceCompassSelectedCelestialEntries(entries: Map<SpaceCompassCelestialBody, T>,
    selectedBodies: Set<SpaceCompassCelestialBody>): Map<SpaceCompassCelestialBody, T> =
    entries.filterKeys { it in selectedBodies }

/** Visible objects and the object consulted by the controls are independent. */
internal data class SpaceCompassCelestialSelection(
    val selected: Set<SpaceCompassCelestialBody> = setOf(SpaceCompassCelestialBody.SUN, SpaceCompassCelestialBody.MOON),
    val active: SpaceCompassCelestialBody? = selected.firstOrNull()
) {
    init { require(active == null && selected.isEmpty() || active in selected) }
    val ordered: List<SpaceCompassCelestialBody> get() = spaceCompassCelestialCatalogOrder.filter { it in selected }

    fun toggle(body: SpaceCompassCelestialBody): SpaceCompassCelestialSelection {
        val updated = if (body in selected) selected - body else selected + body
        return SpaceCompassCelestialSelection(updated, active?.takeIf { it in updated }
            ?: spaceCompassCelestialCatalogOrder.firstOrNull { it in updated })
    }

    fun toggleAll(): SpaceCompassCelestialSelection = if (selected.size == spaceCompassCelestialCatalogOrder.size)
        SpaceCompassCelestialSelection(emptySet(), null) else SpaceCompassCelestialSelection(spaceCompassCelestialCatalogOrder.toSet(),
            active ?: spaceCompassCelestialCatalogOrder.first())

    /** Select/deselect only the displayed subset, preserving hidden checks and a valid active body. */
    fun toggleVisible(visible: Set<SpaceCompassCelestialBody>): SpaceCompassCelestialSelection {
        if (visible.isEmpty()) return this
        val updated = if (selected.containsAll(visible)) selected - visible else selected + visible
        return SpaceCompassCelestialSelection(updated, active?.takeIf { it in updated }
            ?: spaceCompassCelestialCatalogOrder.firstOrNull { it in updated })
    }

    fun step(delta: Int): SpaceCompassCelestialSelection {
        if (ordered.isEmpty()) return this
        val index = ordered.indexOf(active)
        return copy(active = ordered[Math.floorMod(index + delta, ordered.size)])
    }
}

/** Unknown/removed catalog IDs are discarded, but an explicitly empty selection stays empty. */
internal fun restoreSpaceCompassCelestialSelection(names: Set<String>?, activeName: String?): SpaceCompassCelestialSelection {
    if (names == null) return SpaceCompassCelestialSelection()
    val selected = spaceCompassCelestialCatalogOrder.filter { it.name in names }.toSet()
    val active = selected.firstOrNull { it.name == activeName }
        ?: spaceCompassCelestialCatalogOrder.firstOrNull { it in selected }
    return SpaceCompassCelestialSelection(selected, active)
}
