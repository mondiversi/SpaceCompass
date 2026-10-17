package me.mondiversi.spacecompass

import java.text.Collator
import java.util.Locale

internal enum class SpaceCompassCatalogType(val label: Int) {
    ASTEROID(R.string.catalog_type_asteroid), PLANET(R.string.catalog_type_planet), DWARF_PLANET(R.string.catalog_type_dwarf), EXOPLANET(R.string.catalog_type_exoplanet),
    NATURAL_SATELLITE(R.string.catalog_type_moon), COMET(R.string.catalog_type_comet), STAR(R.string.catalog_type_star),
    BLACK_HOLE(R.string.catalog_type_black_hole), NEUTRON_STAR(R.string.catalog_type_neutron),
    SPACE_STATION(R.string.catalog_type_station), ARTIFICIAL_SATELLITE(R.string.catalog_type_satellite),
    SPACE_PROBE(R.string.catalog_type_probe), NEBULA(R.string.catalog_type_nebula), STAR_CLUSTER(R.string.catalog_type_cluster),
    CONSTELLATION(R.string.catalog_type_constellation), GALAXY(R.string.catalog_type_galaxy)
}
internal enum class SpaceCompassCatalogVisibility(val label: Int) {
    ALL(R.string.catalog_all), ABOVE(R.string.catalog_above_horizon), BELOW(R.string.catalog_below_horizon)
}

internal data class SpaceCompassCatalogTypeCount(val type: SpaceCompassCatalogType, val count: Int)

/** Counts describe the available catalogue, independently of the active search or horizon filter. */
internal fun spaceCompassCatalogTypeCounts(available: List<SpaceCompassCelestialBody>,
    labels: Map<SpaceCompassCatalogType, String>, locale: Locale): List<SpaceCompassCatalogTypeCount> {
    val counts = available.groupingBy { it.catalogType }.eachCount()
    val collator = Collator.getInstance(locale)
    return SpaceCompassCatalogType.entries.map { SpaceCompassCatalogTypeCount(it, counts[it] ?: 0) }
        .sortedWith(compareByDescending<SpaceCompassCatalogTypeCount> { it.count }.thenComparator { a, b ->
            collator.compare(labels[a.type] ?: a.type.name, labels[b.type] ?: b.type.name)
        })
}

internal val SpaceCompassCelestialBody.catalogType: SpaceCompassCatalogType get() = additionalCatalogEntry?.type ?: when (this) {
    SpaceCompassCelestialBody.CERES -> SpaceCompassCatalogType.DWARF_PLANET
    SpaceCompassCelestialBody.ENCELADUS -> SpaceCompassCatalogType.NATURAL_SATELLITE
    SpaceCompassCelestialBody.VEGA, SpaceCompassCelestialBody.DENEB, SpaceCompassCelestialBody.ALTAIR -> SpaceCompassCatalogType.STAR
    SpaceCompassCelestialBody.SOUTHERN_CROSS -> SpaceCompassCatalogType.CONSTELLATION
    SpaceCompassCelestialBody.APOPHIS -> SpaceCompassCatalogType.ASTEROID
    SpaceCompassCelestialBody.ATLAS_3I -> SpaceCompassCatalogType.COMET
    SpaceCompassCelestialBody.PROXIMA_B -> SpaceCompassCatalogType.EXOPLANET
    SpaceCompassCelestialBody.M87 -> SpaceCompassCatalogType.BLACK_HOLE
    SpaceCompassCelestialBody.TIANGONG -> SpaceCompassCatalogType.SPACE_STATION
    SpaceCompassCelestialBody.HUBBLE, SpaceCompassCelestialBody.JWST -> SpaceCompassCatalogType.ARTIFICIAL_SATELLITE
    SpaceCompassCelestialBody.NEW_HORIZONS, SpaceCompassCelestialBody.PARKER_SOLAR_PROBE,
    SpaceCompassCelestialBody.EUROPA_CLIPPER, SpaceCompassCelestialBody.JUICE -> SpaceCompassCatalogType.SPACE_PROBE
    SpaceCompassCelestialBody.ORION_NEBULA -> SpaceCompassCatalogType.NEBULA
    SpaceCompassCelestialBody.PLEIADES -> SpaceCompassCatalogType.STAR_CLUSTER
    SpaceCompassCelestialBody.SIRIUS, SpaceCompassCelestialBody.BETELGEUSE -> SpaceCompassCatalogType.STAR
    SpaceCompassCelestialBody.TITAN -> SpaceCompassCatalogType.NATURAL_SATELLITE
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
    SpaceCompassCelestialBody.STARLINK_V3, SpaceCompassCelestialBody.TESLA_ROADSTER -> SpaceCompassCatalogType.ARTIFICIAL_SATELLITE
    SpaceCompassCelestialBody.VOYAGER_1, SpaceCompassCelestialBody.VOYAGER_2 -> SpaceCompassCatalogType.SPACE_PROBE
    else -> error("Missing catalogue category: $this")
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
