package me.mondiversi.spacecompass

import java.text.Collator
import java.util.Locale

internal enum class SpaceCompassCatalogSortField(val label: Int) {
    NAME(R.string.catalog_sort_alphabetical), DISTANCE(R.string.catalog_distance_from_sun),
    MASS(R.string.celestial_view_mass), DIAMETER(R.string.celestial_view_diameter),
    PRESSURE(R.string.celestial_pressure_surface), GRAVITY(R.string.celestial_view_gravity),
    DAY_TEMPERATURE(R.string.catalog_sort_day_temperature), NIGHT_TEMPERATURE(R.string.catalog_sort_night_temperature)
}

/** Existing stored names remain stable across updates; new pairs use the same preference. */
internal enum class SpaceCompassCatalogSort(val field: SpaceCompassCatalogSortField? = null, val descending: Boolean = false) {
    CATALOG,
    NAME_ASC(SpaceCompassCatalogSortField.NAME), NAME_DESC(SpaceCompassCatalogSortField.NAME, true),
    DISTANCE_ASC(SpaceCompassCatalogSortField.DISTANCE), DISTANCE_DESC(SpaceCompassCatalogSortField.DISTANCE, true),
    MASS_ASC(SpaceCompassCatalogSortField.MASS), MASS_DESC(SpaceCompassCatalogSortField.MASS, true),
    DIAMETER_ASC(SpaceCompassCatalogSortField.DIAMETER), DIAMETER_DESC(SpaceCompassCatalogSortField.DIAMETER, true),
    PRESSURE_ASC(SpaceCompassCatalogSortField.PRESSURE), PRESSURE_DESC(SpaceCompassCatalogSortField.PRESSURE, true),
    GRAVITY_ASC(SpaceCompassCatalogSortField.GRAVITY), GRAVITY_DESC(SpaceCompassCatalogSortField.GRAVITY, true),
    DAY_TEMPERATURE_ASC(SpaceCompassCatalogSortField.DAY_TEMPERATURE), DAY_TEMPERATURE_DESC(SpaceCompassCatalogSortField.DAY_TEMPERATURE, true),
    NIGHT_TEMPERATURE_ASC(SpaceCompassCatalogSortField.NIGHT_TEMPERATURE), NIGHT_TEMPERATURE_DESC(SpaceCompassCatalogSortField.NIGHT_TEMPERATURE, true)
}

/** Canonical physical units. Temperature fallbacks are resolved once and labelled in the UI. */
internal fun spaceCompassCatalogPhysicalSortValue(body: SpaceCompassCelestialBody, field: SpaceCompassCatalogSortField): Double? {
    val facts = spaceCompassCelestialFacts(body)
    val kind = when (field) {
        SpaceCompassCatalogSortField.DAY_TEMPERATURE -> SpaceCompassCelestialTemperatureKind.DAY_MAXIMUM
        SpaceCompassCatalogSortField.NIGHT_TEMPERATURE -> SpaceCompassCelestialTemperatureKind.NIGHT_MINIMUM
        else -> null
    }
    val value = when (field) {
        SpaceCompassCatalogSortField.MASS -> spaceCompassCelestialMassKilograms(facts)
        SpaceCompassCatalogSortField.DIAMETER -> facts.diameterKm
        SpaceCompassCatalogSortField.PRESSURE -> spaceCompassAtmosphericPressure(body)?.pascals
        SpaceCompassCatalogSortField.GRAVITY -> facts.gravity
        else -> spaceCompassCatalogTemperatureReference(body, field)?.sortCelsius
    }
    return value?.takeIf { it.isFinite() && if (kind == null) it >= 0 else it >= -273.15 }
}

/** Missing values stay last in both directions; equal values retain catalogue order. */
internal fun spaceCompassSortCatalog(bodies: List<SpaceCompassCelestialBody>, sort: SpaceCompassCatalogSort,
    names: Map<SpaceCompassCelestialBody, String>, solarDistancesAu: Map<SpaceCompassCelestialBody, Double?>,
    locale: Locale): List<SpaceCompassCelestialBody> {
    if (sort == SpaceCompassCatalogSort.CATALOG) return bodies
    val field = requireNotNull(sort.field)
    val collator = Collator.getInstance(locale)
    val original = spaceCompassAllCelestialOrder.withIndex().associate { it.value to it.index }
    val values = if (field == SpaceCompassCatalogSortField.NAME) emptyMap() else bodies.associateWith { body ->
        if (field == SpaceCompassCatalogSortField.DISTANCE) solarDistancesAu[body]?.takeIf { it.isFinite() && it >= 0 }
        else spaceCompassCatalogPhysicalSortValue(body, field)
    }
    return bodies.sortedWith(Comparator { a, b ->
        val comparison = if (field == SpaceCompassCatalogSortField.NAME) {
            if (sort.descending) collator.compare(names[b] ?: b.name, names[a] ?: a.name)
            else collator.compare(names[a] ?: a.name, names[b] ?: b.name)
        } else {
            val x = values[a]; val y = values[b]
            when {
                x == null && y == null -> 0
                x == null -> 1
                y == null -> -1
                sort.descending -> y.compareTo(x)
                else -> x.compareTo(y)
            }
        }
        if (comparison == 0) original.getValue(a).compareTo(original.getValue(b)) else comparison
    })
}
