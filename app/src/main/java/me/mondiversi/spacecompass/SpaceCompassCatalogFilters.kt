package me.mondiversi.spacecompass

internal enum class SpaceCompassCatalogType(val label: Int) {
    PLANET(R.string.catalog_type_planet), DWARF_PLANET(R.string.catalog_type_dwarf), EXOPLANET(R.string.catalog_type_exoplanet),
    NATURAL_SATELLITE(R.string.catalog_type_moon), COMET(R.string.catalog_type_comet), STAR(R.string.catalog_type_star),
    BLACK_HOLE(R.string.catalog_type_black_hole), NEUTRON_STAR(R.string.catalog_type_neutron),
    SPACE_STATION(R.string.catalog_type_station), ARTIFICIAL_SATELLITE(R.string.catalog_type_satellite),
    SPACE_PROBE(R.string.catalog_type_probe)
}
internal enum class SpaceCompassCatalogVisibility(val label: Int) {
    ALL(R.string.catalog_all), ABOVE(R.string.catalog_above_horizon), BELOW(R.string.catalog_below_horizon)
}
internal val SpaceCompassCelestialBody.catalogType: SpaceCompassCatalogType get() = when (this) {
    SpaceCompassCelestialBody.HALLEY, SpaceCompassCelestialBody.COMET_67P -> SpaceCompassCatalogType.COMET
    SpaceCompassCelestialBody.TRAPPIST_1_E -> SpaceCompassCatalogType.EXOPLANET
    SpaceCompassCelestialBody.MERCURY, SpaceCompassCelestialBody.VENUS, SpaceCompassCelestialBody.EARTH_CENTER,
    SpaceCompassCelestialBody.MARS, SpaceCompassCelestialBody.JUPITER, SpaceCompassCelestialBody.SATURN,
    SpaceCompassCelestialBody.URANUS, SpaceCompassCelestialBody.NEPTUNE -> SpaceCompassCatalogType.PLANET
    SpaceCompassCelestialBody.PLUTO, SpaceCompassCelestialBody.SEDNA -> SpaceCompassCatalogType.DWARF_PLANET
    SpaceCompassCelestialBody.LV_426, SpaceCompassCelestialBody.MOON, SpaceCompassCelestialBody.IO, SpaceCompassCelestialBody.EUROPA -> SpaceCompassCatalogType.NATURAL_SATELLITE
    SpaceCompassCelestialBody.SUN, SpaceCompassCelestialBody.POLARIS, SpaceCompassCelestialBody.ALPHA_CENTAURI,
    SpaceCompassCelestialBody.PROXIMA_CENTAURI, SpaceCompassCelestialBody.RIGEL,
    SpaceCompassCelestialBody.STEPHENSON_2_18 -> SpaceCompassCatalogType.STAR
    SpaceCompassCelestialBody.SAGITTARIUS_A, SpaceCompassCelestialBody.ANDROMEDA_CORE, SpaceCompassCelestialBody.TON_618 -> SpaceCompassCatalogType.BLACK_HOLE
    SpaceCompassCelestialBody.RX_J1856, SpaceCompassCelestialBody.PSR_J0437 -> SpaceCompassCatalogType.NEUTRON_STAR
    SpaceCompassCelestialBody.ISS -> SpaceCompassCatalogType.SPACE_STATION
    SpaceCompassCelestialBody.STARLINK_V3 -> SpaceCompassCatalogType.ARTIFICIAL_SATELLITE
    SpaceCompassCelestialBody.VOYAGER_1, SpaceCompassCelestialBody.VOYAGER_2 -> SpaceCompassCatalogType.SPACE_PROBE
}

/** Unknown positions stay in All; filtering never changes the underlying checked set. */
internal fun spaceCompassFilterCatalog(types: Set<SpaceCompassCatalogType>, visibility: SpaceCompassCatalogVisibility,
    elevations: Map<SpaceCompassCelestialBody, Double?>,
    available: List<SpaceCompassCelestialBody> = spaceCompassCelestialCatalogOrder): List<SpaceCompassCelestialBody> =
    available.filter { body ->
        (types.isEmpty() || body.catalogType in types) && when (visibility) {
            SpaceCompassCatalogVisibility.ALL -> true
            SpaceCompassCatalogVisibility.ABOVE -> elevations[body]?.let { it.isFinite() && it >= 0.0 } == true
            SpaceCompassCatalogVisibility.BELOW -> elevations[body]?.let { it.isFinite() && it < 0.0 } == true
        }
    }
