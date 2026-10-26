package me.mondiversi.spacecompass

/** Authoritative target identifiers, separate from localized names and catalogue ordering.
 * Live Horizons headers and sources are recorded in docs/CATALOG_1_4_2.md.
 */
internal val SpaceCompassCelestialBody.expandedHorizonsTarget: SpaceCompassHorizonsTarget?
    get() = when (this) {
        SpaceCompassCelestialBody.CERES -> SpaceCompassHorizonsTarget("1;", "1 Ceres (A801 AA)", 0.0..10.0)
        SpaceCompassCelestialBody.ENCELADUS -> SpaceCompassHorizonsTarget("602", "Enceladus (602)", 0.0..20.0)
        SpaceCompassCelestialBody.NEW_HORIZONS -> SpaceCompassHorizonsTarget("-98", "New Horizons (spacecraft) (-98)", 0.0..500.0)
        SpaceCompassCelestialBody.JWST -> SpaceCompassHorizonsTarget("-170", "James Webb Space Telescope (spacecraft) (-170)", 0.0..5.0)
        SpaceCompassCelestialBody.PARKER_SOLAR_PROBE -> SpaceCompassHorizonsTarget("-96", "Parker Solar Probe (spacecraft) (-96)", 0.0..5.0)
        SpaceCompassCelestialBody.APOPHIS -> SpaceCompassHorizonsTarget("99942;", "99942 Apophis (2004 MN4)", 0.0..10.0)
        // Resolve the designated comet, never a transient database record number or another ATLAS.
        SpaceCompassCelestialBody.ATLAS_3I -> SpaceCompassHorizonsTarget("DES=3I;NOFRAG;CAP;", "ATLAS (C/2025 N1)", 0.0..10_000.0)
        SpaceCompassCelestialBody.EUROPA_CLIPPER -> SpaceCompassHorizonsTarget("-159", "Europa Clipper (spacecraft) (-159)", 0.0..20.0)
        SpaceCompassCelestialBody.JUICE -> SpaceCompassHorizonsTarget("-28", "JUICE (spacecraft) (-28)", 0.0..20.0)
        else -> additionalCatalogEntry?.target
    }

internal val SpaceCompassCelestialBody.isInterplanetaryCraft: Boolean
    get() = when (this) {
        SpaceCompassCelestialBody.NEW_HORIZONS, SpaceCompassCelestialBody.JWST,
        SpaceCompassCelestialBody.PARKER_SOLAR_PROBE, SpaceCompassCelestialBody.EUROPA_CLIPPER,
        SpaceCompassCelestialBody.JUICE, SpaceCompassCelestialBody.BEPICOLOMBO, SpaceCompassCelestialBody.SOLAR_ORBITER, SpaceCompassCelestialBody.LUCY, SpaceCompassCelestialBody.PSYCHE -> true
        else -> largeCatalogEntry?.entry?.type == SpaceCompassCatalogType.SPACE_PROBE
    }

internal data class SpaceCompassCatalogSatellite(val norad: String, val international: String,
    val publicName: String, val inclination: ClosedFloatingPointRange<Double>,
    val meanMotion: ClosedFloatingPointRange<Double> = 14.0..17.0,
    val radiusKm: ClosedFloatingPointRange<Double> = 6400.0..9000.0,
    val speedKmSecond: ClosedFloatingPointRange<Double> = 6.0..9.0)

/** Tiangong tracks the Tianhe core, not a visiting vehicle with a similar name. */
internal val SpaceCompassCelestialBody.catalogSatellite: SpaceCompassCatalogSatellite?
    get() = when (this) {
        SpaceCompassCelestialBody.HUBBLE -> SpaceCompassCatalogSatellite("20580", "90037B", "HST", 27.0..30.0)
        SpaceCompassCelestialBody.TIANGONG -> SpaceCompassCatalogSatellite("48274", "21035A", "CSS (TIANHE-1)", 40.0..43.0)
        else -> largeCatalogEntry?.satellite
    }
