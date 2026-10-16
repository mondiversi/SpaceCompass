package me.mondiversi.spacecompass

/** Stable catalogue designations; major planets already have their accepted names.
 * Sources: USGS/IAU nomenclature, NASA/JPL and the existing SIMBAD references.
 * A constellation designation is not a resolved component or a new pointing target. */
internal val SpaceCompassCelestialBody.scientificDesignation: String? get() = when (this) {
    SpaceCompassCelestialBody.NEW_HORIZONS -> "2006-001A · JPL −98"
    SpaceCompassCelestialBody.JWST -> "2021-130A · JPL −170"
    SpaceCompassCelestialBody.PARKER_SOLAR_PROBE -> "2018-065A · JPL −96"
    SpaceCompassCelestialBody.APOPHIS -> "(99942) Apophis · 2004 MN4"
    SpaceCompassCelestialBody.ATLAS_3I -> "3I/ATLAS · C/2025 N1"
    SpaceCompassCelestialBody.EUROPA_CLIPPER -> "2024-182A · JPL −159"
    SpaceCompassCelestialBody.JUICE -> "2023-053A · JPL −28"
    SpaceCompassCelestialBody.HUBBLE -> "1990-037B · NORAD 20580"
    SpaceCompassCelestialBody.TIANGONG -> "2021-035A · NORAD 48274"
    SpaceCompassCelestialBody.M87 -> "M87* · NGC 4486 nucleus"
    SpaceCompassCelestialBody.PROXIMA_B -> "Proxima Centauri b · α Centauri C b"

    SpaceCompassCelestialBody.TESLA_ROADSTER -> "2018-017A · Starman"
    SpaceCompassCelestialBody.MOON -> "Earth I"
    SpaceCompassCelestialBody.IO -> "Jupiter I"
    SpaceCompassCelestialBody.EUROPA -> "Jupiter II"
    SpaceCompassCelestialBody.TITAN -> "Saturn VI"
    SpaceCompassCelestialBody.PLUTO -> "(134340) Pluto"
    SpaceCompassCelestialBody.SEDNA -> "(90377) Sedna"
    SpaceCompassCelestialBody.HALLEY -> "1P/Halley"
    SpaceCompassCelestialBody.COMET_67P -> "67P/Churyumov–Gerasimenko"
    SpaceCompassCelestialBody.POLARIS -> "α Ursae Minoris"
    SpaceCompassCelestialBody.ALPHA_CENTAURI -> "α Centauri AB"
    SpaceCompassCelestialBody.PROXIMA_CENTAURI -> "α Centauri C"
    SpaceCompassCelestialBody.SIRIUS -> "α Canis Majoris"
    SpaceCompassCelestialBody.BETELGEUSE -> "α Orionis"
    SpaceCompassCelestialBody.RIGEL -> "β Orionis"
    SpaceCompassCelestialBody.ORION_NEBULA -> "M42 · NGC 1976"
    SpaceCompassCelestialBody.PLEIADES -> "M45 · Melotte 22"
    SpaceCompassCelestialBody.ANDROMEDA_CORE -> "M31*"
    // Do not invent a fixed identifier for the dynamically chosen Starlink object,
    // an extra formal solar name, or an astronomical catalogue ID for fiction.
    else -> null
}

internal val SpaceCompassCelestialBody.scientificTypeResource: Int? get() = when (this) {
    SpaceCompassCelestialBody.TESLA_ROADSTER -> R.string.celestial_roadster_type
    SpaceCompassCelestialBody.EARTH_CENTER -> null
    SpaceCompassCelestialBody.SUN -> R.string.celestial_identity_solar_star
    SpaceCompassCelestialBody.SEDNA -> R.string.celestial_identity_candidate
    SpaceCompassCelestialBody.PSR_J0437 -> R.string.celestial_identity_pulsar
    SpaceCompassCelestialBody.LV_426 -> R.string.celestial_identity_fictional_moon
    else -> when (catalogType) {
        SpaceCompassCatalogType.ASTEROID -> R.string.catalog_type_asteroid
        SpaceCompassCatalogType.PLANET -> R.string.celestial_identity_planet
        SpaceCompassCatalogType.DWARF_PLANET -> R.string.celestial_identity_dwarf
        SpaceCompassCatalogType.EXOPLANET -> R.string.celestial_identity_exoplanet
        SpaceCompassCatalogType.NATURAL_SATELLITE -> R.string.celestial_identity_moon
        SpaceCompassCatalogType.COMET -> R.string.celestial_identity_comet
        SpaceCompassCatalogType.STAR -> R.string.celestial_identity_star
        SpaceCompassCatalogType.BLACK_HOLE -> R.string.celestial_identity_black_hole
        SpaceCompassCatalogType.NEUTRON_STAR -> R.string.celestial_identity_neutron
        SpaceCompassCatalogType.SPACE_STATION -> R.string.celestial_identity_station
        SpaceCompassCatalogType.ARTIFICIAL_SATELLITE -> R.string.celestial_identity_satellite
        SpaceCompassCatalogType.SPACE_PROBE -> R.string.celestial_identity_probe
        SpaceCompassCatalogType.NEBULA -> R.string.celestial_identity_nebula
        SpaceCompassCatalogType.STAR_CLUSTER -> R.string.celestial_identity_cluster
    }
}
