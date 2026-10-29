package me.mondiversi.spacecompass

internal data class SpaceCompassCatalogFilterCounts(
    val allTypes: Int,
    val types: Map<SpaceCompassCatalogType, Int>,
    val visibility: Map<SpaceCompassCatalogVisibility, Int>,
    val origin: Map<SpaceCompassCatalogOrigin, Int>,
    val pointing: Map<SpaceCompassCatalogPointing, Int>)

/** Count all facets in one pass without allocating a filtered/sorted catalogue for every option.
 * Checked objects still bypass the other filters; the supplied scope already applies favorites. */
internal fun spaceCompassCatalogFilterCounts(available: List<SpaceCompassCelestialBody>,
    selected: Set<SpaceCompassCelestialBody>, types: Set<SpaceCompassCatalogType>,
    visibility: SpaceCompassCatalogVisibility, origin: SpaceCompassCatalogOrigin,
    pointing: SpaceCompassCatalogPointing, elevations: Map<SpaceCompassCelestialBody, Double?>,
    pointingAvailable: Map<SpaceCompassCelestialBody, Boolean>, searchMatches: Set<SpaceCompassCelestialBody>): SpaceCompassCatalogFilterCounts {
    var allTypes = 0
    val typeCounts = IntArray(SpaceCompassCatalogType.entries.size)
    val visibilityCounts = IntArray(SpaceCompassCatalogVisibility.entries.size)
    val originCounts = IntArray(SpaceCompassCatalogOrigin.entries.size)
    val pointingCounts = IntArray(SpaceCompassCatalogPointing.entries.size)
    for (body in available) {
        if (body in selected) {
            allTypes++
            for (i in typeCounts.indices) typeCounts[i]++
            for (i in visibilityCounts.indices) visibilityCounts[i]++
            for (i in originCounts.indices) originCounts[i]++
            for (i in pointingCounts.indices) pointingCounts[i]++
            continue
        }
        if (body !in searchMatches) continue
        val memberships = body.catalogFilterTypes
        val matchesType = types.isEmpty() || memberships.any { it in types }
        val elevation = elevations[body]?.takeIf { it.isFinite() }
        val horizon = elevation?.let { if (it >= 0) SpaceCompassCatalogVisibility.ABOVE else SpaceCompassCatalogVisibility.BELOW }
        val source = if (body.isFictional) SpaceCompassCatalogOrigin.FICTION else SpaceCompassCatalogOrigin.REAL
        val direction = pointingAvailable[body]?.let {
            if (it) SpaceCompassCatalogPointing.AVAILABLE else SpaceCompassCatalogPointing.UNAVAILABLE
        }
        val matchesVisibility = visibility == SpaceCompassCatalogVisibility.ALL || visibility == horizon
        val matchesOrigin = origin == SpaceCompassCatalogOrigin.ALL || origin == source
        val matchesPointing = pointing == SpaceCompassCatalogPointing.ALL || pointing == direction
        if (matchesVisibility && matchesOrigin && matchesPointing) {
            allTypes++
            for (type in memberships) typeCounts[type.ordinal]++
        }
        if (matchesType && matchesOrigin && matchesPointing) {
            visibilityCounts[SpaceCompassCatalogVisibility.ALL.ordinal]++
            horizon?.let { visibilityCounts[it.ordinal]++ }
        }
        if (matchesType && matchesVisibility && matchesPointing) {
            originCounts[SpaceCompassCatalogOrigin.ALL.ordinal]++
            originCounts[source.ordinal]++
        }
        if (matchesType && matchesVisibility && matchesOrigin) {
            pointingCounts[SpaceCompassCatalogPointing.ALL.ordinal]++
            direction?.let { pointingCounts[it.ordinal]++ }
        }
    }
    return SpaceCompassCatalogFilterCounts(allTypes,
        SpaceCompassCatalogType.entries.associateWith { typeCounts[it.ordinal] },
        SpaceCompassCatalogVisibility.entries.associateWith { visibilityCounts[it.ordinal] },
        SpaceCompassCatalogOrigin.entries.associateWith { originCounts[it.ordinal] },
        SpaceCompassCatalogPointing.entries.associateWith { pointingCounts[it.ordinal] })
}
