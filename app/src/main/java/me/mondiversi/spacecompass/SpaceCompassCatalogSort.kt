package me.mondiversi.spacecompass

import java.text.Collator
import java.util.Locale

internal enum class SpaceCompassCatalogSort { CATALOG, NAME_ASC, NAME_DESC, DISTANCE_ASC, DISTANCE_DESC }

/** Common heliocentric AU values; missing distances remain last in either direction. */
internal fun spaceCompassSortCatalog(bodies: List<SpaceCompassCelestialBody>, sort: SpaceCompassCatalogSort,
    names: Map<SpaceCompassCelestialBody, String>, solarDistancesAu: Map<SpaceCompassCelestialBody, Double?>,
    locale: Locale): List<SpaceCompassCelestialBody> {
    if (sort == SpaceCompassCatalogSort.CATALOG) return bodies
    val collator = Collator.getInstance(locale)
    val original = spaceCompassCelestialCatalogOrder.withIndex().associate { it.value to it.index }
    return bodies.sortedWith(Comparator { a, b ->
        val comparison = when (sort) {
            SpaceCompassCatalogSort.NAME_ASC -> collator.compare(names[a] ?: a.name, names[b] ?: b.name)
            SpaceCompassCatalogSort.NAME_DESC -> collator.compare(names[b] ?: b.name, names[a] ?: a.name)
            else -> {
                val x = solarDistancesAu[a]?.takeIf { it.isFinite() && it >= 0 }
                val y = solarDistancesAu[b]?.takeIf { it.isFinite() && it >= 0 }
                when {
                    x == null && y == null -> 0
                    x == null -> 1
                    y == null -> -1
                    sort == SpaceCompassCatalogSort.DISTANCE_ASC -> x.compareTo(y)
                    else -> y.compareTo(x)
                }
            }
        }
        if (comparison == 0) original.getValue(a).compareTo(original.getValue(b)) else comparison
    })
}
