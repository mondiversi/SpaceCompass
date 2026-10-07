package me.mondiversi.spacecompass

/** An exact day/night extreme takes precedence. Otherwise keep the reference's true quantity.
 * A range sorts by its midpoint, identically in both day/night views; component A is the
 * stable representative for Alpha Centauri AB and remains explicitly labelled. */
internal data class SpaceCompassCatalogTemperatureReference(
    val temperature: SpaceCompassCelestialTemperature,
    val exactDayNight: Boolean
) {
    val sortCelsius: Double
        get() = temperature.maximumCelsius?.let { (temperature.celsius + it) / 2 } ?: temperature.celsius
}

internal fun spaceCompassCatalogTemperatureReference(body: SpaceCompassCelestialBody,
    field: SpaceCompassCatalogSortField): SpaceCompassCatalogTemperatureReference? {
    val wanted = when (field) {
        SpaceCompassCatalogSortField.DAY_TEMPERATURE -> SpaceCompassCelestialTemperatureKind.DAY_MAXIMUM
        SpaceCompassCatalogSortField.NIGHT_TEMPERATURE -> SpaceCompassCelestialTemperatureKind.NIGHT_MINIMUM
        else -> return null
    }
    val temperatures = spaceCompassCelestialTemperatures(body)
    val exact = temperatures.firstOrNull { it.kind == wanted }
    val reference = exact ?: temperatures.firstOrNull {
        it.kind != SpaceCompassCelestialTemperatureKind.DAY_MAXIMUM &&
            it.kind != SpaceCompassCelestialTemperatureKind.NIGHT_MINIMUM
    } ?: return null
    return SpaceCompassCatalogTemperatureReference(reference, exact != null)
}

/** Short catalog qualifiers preserve the full physical-layer labels in object details. */
internal val SpaceCompassCelestialTemperatureKind.catalogLabelResource: Int
    get() = when (this) {
        SpaceCompassCelestialTemperatureKind.SURFACE_MEAN -> R.string.catalog_temperature_mean
        SpaceCompassCelestialTemperatureKind.SURFACE_RANGE -> R.string.catalog_temperature_range
        SpaceCompassCelestialTemperatureKind.ATMOSPHERE_ONE_BAR -> R.string.catalog_temperature_atmosphere
        SpaceCompassCelestialTemperatureKind.PHOTOSPHERE -> R.string.catalog_temperature_photosphere
        SpaceCompassCelestialTemperatureKind.STELLAR_EFFECTIVE -> R.string.catalog_temperature_effective
        SpaceCompassCelestialTemperatureKind.SURFACE_ESTIMATE -> R.string.catalog_temperature_estimate
        SpaceCompassCelestialTemperatureKind.EQUILIBRIUM_MODEL -> R.string.catalog_temperature_equilibrium
        SpaceCompassCelestialTemperatureKind.HISTORICAL_SURFACE_RANGE -> R.string.catalog_temperature_historical
        SpaceCompassCelestialTemperatureKind.THERMAL_MODEL -> R.string.catalog_temperature_thermal
        SpaceCompassCelestialTemperatureKind.DAY_MAXIMUM, SpaceCompassCelestialTemperatureKind.NIGHT_MINIMUM -> labelResource
    }
