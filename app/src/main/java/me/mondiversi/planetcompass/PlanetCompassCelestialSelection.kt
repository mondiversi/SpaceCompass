package me.mondiversi.planetcompass

/** Filter the scene immediately, including while cached observations/paths are refreshing. */
internal fun <T> planetCompassSelectedCelestialEntries(entries: Map<PlanetCompassCelestialBody, T>,
    selectedBodies: Set<PlanetCompassCelestialBody>): Map<PlanetCompassCelestialBody, T> =
    entries.filterKeys { it in selectedBodies }

/** Visible objects and the object consulted by the controls are independent. */
internal data class PlanetCompassCelestialSelection(
    val selected: Set<PlanetCompassCelestialBody> = setOf(PlanetCompassCelestialBody.SUN, PlanetCompassCelestialBody.MOON),
    val active: PlanetCompassCelestialBody? = selected.firstOrNull()
) {
    init { require(active == null && selected.isEmpty() || active in selected) }
    val ordered: List<PlanetCompassCelestialBody> get() = planetCompassCelestialCatalogOrder.filter { it in selected }

    fun toggle(body: PlanetCompassCelestialBody): PlanetCompassCelestialSelection {
        val updated = if (body in selected) selected - body else selected + body
        return PlanetCompassCelestialSelection(updated, active?.takeIf { it in updated }
            ?: planetCompassCelestialCatalogOrder.firstOrNull { it in updated })
    }

    fun toggleAll(): PlanetCompassCelestialSelection = if (selected.size == planetCompassCelestialCatalogOrder.size)
        PlanetCompassCelestialSelection(emptySet(), null) else PlanetCompassCelestialSelection(planetCompassCelestialCatalogOrder.toSet(),
            active ?: planetCompassCelestialCatalogOrder.first())

    fun step(delta: Int): PlanetCompassCelestialSelection {
        if (ordered.isEmpty()) return this
        val index = ordered.indexOf(active)
        return copy(active = ordered[Math.floorMod(index + delta, ordered.size)])
    }
}
