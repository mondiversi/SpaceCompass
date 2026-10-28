package me.mondiversi.spacecompass

import java.text.Collator
import java.util.Locale

internal enum class SpaceCompassCatalogType(val label: Int) {
    ASTEROID(R.string.catalog_type_asteroid), PLANET(R.string.catalog_type_planet), DWARF_PLANET(R.string.catalog_type_dwarf), EXOPLANET(R.string.catalog_type_exoplanet),
    NATURAL_SATELLITE(R.string.catalog_type_moon), COMET(R.string.catalog_type_comet), STAR(R.string.catalog_type_star),
    BLACK_HOLE(R.string.catalog_type_black_hole), NEUTRON_STAR(R.string.catalog_type_neutron),
    SPACE_STATION(R.string.catalog_type_station), ARTIFICIAL_SATELLITE(R.string.catalog_type_satellite),
    SPACE_PROBE(R.string.catalog_type_probe), NEBULA(R.string.catalog_type_nebula), STAR_CLUSTER(R.string.catalog_type_cluster),
    CONSTELLATION(R.string.catalog_type_constellation), GALAXY(R.string.catalog_type_galaxy), SCIENCE_FICTION(R.string.catalog_type_scifi),
    SPACECRAFT(R.string.catalog_type_spacecraft)
}
internal enum class SpaceCompassCatalogVisibility(val label: Int) {
    ALL(R.string.catalog_all), ABOVE(R.string.catalog_above_horizon), BELOW(R.string.catalog_below_horizon)
}

internal enum class SpaceCompassCatalogOrigin(val label: Int) {
    ALL(R.string.catalog_all), REAL(R.string.catalog_origin_real), FICTION(R.string.catalog_type_scifi)
}
internal enum class SpaceCompassCatalogPointing(val label: Int) {
    ALL(R.string.catalog_all), AVAILABLE(R.string.catalog_pointing_available), UNAVAILABLE(R.string.catalog_pointing_unavailable)
}

/** A finite direction is enough; direction-only references need not have a measured distance. */
internal fun spaceCompassCatalogHasPointing(observation: SpaceCompassCelestialObservation?): Boolean =
    observation?.position?.let { it.azimuthDegrees.isFinite() && it.elevationDegrees.isFinite() &&
        it.elevationDegrees in -90.0..90.0 } == true

internal data class SpaceCompassCatalogTypeCount(val type: SpaceCompassCatalogType, val count: Int)

/** Counts describe the available catalogue, independently of the active search or horizon filter. */
internal fun spaceCompassCatalogTypeCounts(available: List<SpaceCompassCelestialBody>,
    labels: Map<SpaceCompassCatalogType, String>, locale: Locale): List<SpaceCompassCatalogTypeCount> {
    val counts = mutableMapOf<SpaceCompassCatalogType, Int>()
    for (body in available) for (type in body.catalogFilterTypes) {
        counts[type] = (counts[type] ?: 0) + 1
    }
    val collator = Collator.getInstance(locale)
    return SpaceCompassCatalogType.entries.map { SpaceCompassCatalogTypeCount(it, counts[it] ?: 0) }
        .sortedWith(compareByDescending<SpaceCompassCatalogTypeCount> { it.count }.thenComparator { a, b ->
            collator.compare(labels[a.type] ?: a.type.name, labels[b.type] ?: b.type.name)
        })
}

internal val SpaceCompassCelestialBody.catalogType: SpaceCompassCatalogType get() = if (isFictional) SpaceCompassCatalogType.SCIENCE_FICTION else additionalCatalogEntry?.type ?: when (this) {
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
    SpaceCompassCelestialBody.MOON, SpaceCompassCelestialBody.IO, SpaceCompassCelestialBody.EUROPA -> SpaceCompassCatalogType.NATURAL_SATELLITE
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

private val singleCatalogFilterTypes by lazy {
    SpaceCompassCatalogType.entries.associateWith { setOf(it) }
}

/** Filter membership may overlap; the primary category still governs scientific behaviour. */
internal val SpaceCompassCelestialBody.catalogFilterTypes: Set<SpaceCompassCatalogType> get() =
    fictionalReference?.filterTypes ?: singleCatalogFilterTypes.getValue(catalogType)

private fun SpaceCompassCelestialBody.matchesCatalogTypes(types: Set<SpaceCompassCatalogType>): Boolean =
    types.isEmpty() || catalogFilterTypes.any { it in types }

/** Count the union, never the sum of overlapping category counts. */
internal fun spaceCompassCatalogTypeMatchCount(available: List<SpaceCompassCelestialBody>,
    types: Set<SpaceCompassCatalogType>): Int = available.count { it.matchesCatalogTypes(types) }

/** Unknown positions stay in All; filtering never changes the underlying checked set. */
internal fun spaceCompassFilterCatalog(types: Set<SpaceCompassCatalogType>, visibility: SpaceCompassCatalogVisibility,
    elevations: Map<SpaceCompassCelestialBody, Double?>,
    available: List<SpaceCompassCelestialBody> = spaceCompassCelestialCatalogOrder,
    origin: SpaceCompassCatalogOrigin = SpaceCompassCatalogOrigin.ALL,
    pointing: SpaceCompassCatalogPointing = SpaceCompassCatalogPointing.ALL,
    pointingAvailable: Map<SpaceCompassCelestialBody, Boolean> = emptyMap()): List<SpaceCompassCelestialBody> =
    available.filter { body ->
        body.matchesCatalogTypes(types) && when (origin) {
            SpaceCompassCatalogOrigin.ALL -> true
            SpaceCompassCatalogOrigin.REAL -> !body.isFictional
            SpaceCompassCatalogOrigin.FICTION -> body.isFictional
        } && when (pointing) {
            SpaceCompassCatalogPointing.ALL -> true
            SpaceCompassCatalogPointing.AVAILABLE -> pointingAvailable[body] == true
            // No entry means the current calculation is pending, not that pointing has failed.
            SpaceCompassCatalogPointing.UNAVAILABLE -> pointingAvailable[body] == false
        } && when (visibility) {
            SpaceCompassCatalogVisibility.ALL -> true
            SpaceCompassCatalogVisibility.ABOVE -> elevations[body]?.let { it.isFinite() && it >= 0.0 } == true
            SpaceCompassCatalogVisibility.BELOW -> elevations[body]?.let { it.isFinite() && it < 0.0 } == true
        }
    }
