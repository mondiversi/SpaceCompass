package me.mondiversi.spacecompass

internal data class SpaceCompassCatalogFilterCounts(
    val allTypes: Int,
    val types: Map<SpaceCompassCatalogType, Int>,
    val visibility: Map<SpaceCompassCatalogVisibility, Int>,
    val origin: Map<SpaceCompassCatalogOrigin, Int>,
    val pointing: Map<SpaceCompassCatalogPointing, Int>)

/** Each option counts its visible result with the other facets, search and retained checked objects.
 * Type rows describe that category alone; the field itself displays the current multi-type union.
 * The supplied scope already applies the independent favorites filter. */
internal fun spaceCompassCatalogFilterCounts(available: List<SpaceCompassCelestialBody>,
    selected: Set<SpaceCompassCelestialBody>, types: Set<SpaceCompassCatalogType>,
    visibility: SpaceCompassCatalogVisibility, origin: SpaceCompassCatalogOrigin,
    pointing: SpaceCompassCatalogPointing, elevations: Map<SpaceCompassCelestialBody, Double?>,
    pointingAvailable: Map<SpaceCompassCelestialBody, Boolean>, searchMatches: Set<SpaceCompassCelestialBody>): SpaceCompassCatalogFilterCounts {
    fun count(t: Set<SpaceCompassCatalogType> = types, v: SpaceCompassCatalogVisibility = visibility,
        o: SpaceCompassCatalogOrigin = origin, p: SpaceCompassCatalogPointing = pointing): Int {
        val matching = spaceCompassFilterCatalog(t,v,elevations,available,o,p,pointingAvailable)
            .filter { it in searchMatches }.toSet()
        return spaceCompassCatalogDisplayOrder(available,matching,selected).size
    }
    return SpaceCompassCatalogFilterCounts(count(t=emptySet()),
        SpaceCompassCatalogType.entries.associateWith { count(t=setOf(it)) },
        SpaceCompassCatalogVisibility.entries.associateWith { count(v=it) },
        SpaceCompassCatalogOrigin.entries.associateWith { count(o=it) },
        SpaceCompassCatalogPointing.entries.associateWith { count(p=it) })
}
