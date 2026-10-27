package me.mondiversi.spacecompass

import io.github.cosinekitty.astronomy.Observer

/** Fiction is isolated from measured science. A real reference direction is not a measured fictional orbit. */
internal data class SpaceCompassFictionalReference(val body: SpaceCompassCelestialBody, val universe: String,
    val referenceName: String, val classification: Int, val hostBody: SpaceCompassCelestialBody?,
    private val fixedReference: SpaceCompassDeepSkyReference?, val texture: String, val sourceUrl: String,
    val description: Int, val filterType: SpaceCompassCatalogType, val spacecraft: Boolean = false) {
    val filterTypes: Set<SpaceCompassCatalogType> = setOf(SpaceCompassCatalogType.SCIENCE_FICTION, filterType)
    val stellarReference: SpaceCompassDeepSkyReference? get() = hostBody?.deepSkyReference ?: fixedReference
}

private val zetaReticuli = SpaceCompassDeepSkyReference(3 + 18.0/60 + 12.8188824117/3600,
    -(62 + 30.0/60 + 22.904711032/3600), 39.3, 1331.027, 647.725)
private val deltaPavonis = SpaceCompassDeepSkyReference(20.145446908779224, -66.18206743589695, 19.893115262536416, 1211.761, -1130.237)
private val ophiuchiB = SpaceCompassDeepSkyReference(17.255828840615333, -26.602825956905555, 19.399024310311706, -465.861, -1141.168)
private val eridaniA = SpaceCompassDeepSkyReference(4.254533227498472, -7.652869660838334, 16.33984498116308, -2240.085, -3421.809)

internal val spaceCompassFictionalReferences: List<SpaceCompassFictionalReference> by lazy { listOf(
    SpaceCompassFictionalReference(SpaceCompassCelestialBody.LV_426, "Alien", "ζ² Reticuli",
        R.string.scifi_type_moon, null, zetaReticuli,
        "lv426.webp", "https://www.avpcentral.com/calpamos", R.string.celestial_lv426_description, filterType = SpaceCompassCatalogType.NATURAL_SATELLITE, spacecraft = false),
    SpaceCompassFictionalReference(SpaceCompassCelestialBody.ARRAKIS, "Dune", "Canopus · α Carinae",
        R.string.scifi_type_planet, SpaceCompassCelestialBody.CANOPUS, null,
        "fictional_arrakis.webp", "https://andrejjh.github.io/duneToI.html", R.string.scifi_description_arrakis, filterType = SpaceCompassCatalogType.PLANET, spacecraft = false),
    SpaceCompassFictionalReference(SpaceCompassCelestialBody.CALADAN, "Dune", "δ Pavonis",
        R.string.scifi_type_planet, null, deltaPavonis,
        "fictional_caladan.webp", "https://andrejjh.github.io/duneToI.html", R.string.scifi_description_caladan, filterType = SpaceCompassCatalogType.PLANET, spacecraft = false),
    SpaceCompassFictionalReference(SpaceCompassCelestialBody.GIEDI_PRIME, "Dune", "36 Ophiuchi B",
        R.string.scifi_type_planet, null, ophiuchiB,
        "fictional_giedi_prime.webp", "https://andrejjh.github.io/duneToI.html", R.string.scifi_description_giedi_prime, filterType = SpaceCompassCatalogType.PLANET, spacecraft = false),
    SpaceCompassFictionalReference(SpaceCompassCelestialBody.LV_223, "Alien", "ζ² Reticuli",
        R.string.scifi_type_moon, null, zetaReticuli,
        "fictional_lv_223.webp", "https://www.avpcentral.com/calpamos", R.string.scifi_description_lv_223, filterType = SpaceCompassCatalogType.NATURAL_SATELLITE, spacecraft = false),
    SpaceCompassFictionalReference(SpaceCompassCelestialBody.CALPAMOS, "Alien", "ζ² Reticuli",
        R.string.scifi_type_giant, null, zetaReticuli,
        "fictional_calpamos.webp", "https://www.avpcentral.com/calpamos", R.string.scifi_description_calpamos, filterType = SpaceCompassCatalogType.PLANET, spacecraft = false),
    SpaceCompassFictionalReference(SpaceCompassCelestialBody.PANDORA, "Avatar", "α Centauri",
        R.string.scifi_type_moon, SpaceCompassCelestialBody.ALPHA_CENTAURI, null,
        "fictional_pandora.webp", "https://www.avatar.com/pandorapedia/pandora", R.string.scifi_description_pandora, filterType = SpaceCompassCatalogType.NATURAL_SATELLITE, spacecraft = false),
    SpaceCompassFictionalReference(SpaceCompassCelestialBody.POLYPHEMUS, "Avatar", "α Centauri",
        R.string.scifi_type_giant, SpaceCompassCelestialBody.ALPHA_CENTAURI, null,
        "fictional_polyphemus.webp", "https://www.avatar.com/pandorapedia/polyphemus", R.string.scifi_description_polyphemus, filterType = SpaceCompassCatalogType.PLANET, spacecraft = false),
    SpaceCompassFictionalReference(SpaceCompassCelestialBody.VULCAN, "Star Trek", "40 Eridani A",
        R.string.scifi_type_planet, null, eridaniA,
        "fictional_vulcan.webp", "https://science.nasa.gov/universe/exoplanets/vulcan-may-not-be-real-but-spocks-home-system-is/", R.string.scifi_description_vulcan, filterType = SpaceCompassCatalogType.PLANET, spacecraft = false),
    SpaceCompassFictionalReference(SpaceCompassCelestialBody.NOSTROMO, "Alien", "ζ² Reticuli",
        R.string.scifi_type_spacecraft, null, zetaReticuli,
        "fictional_nostromo.webp", "https://www.avpgalaxy.net/files/scripts/aliens-1985-02-26.pdf", R.string.scifi_description_nostromo, filterType = SpaceCompassCatalogType.SPACECRAFT, spacecraft = true),
    SpaceCompassFictionalReference(SpaceCompassCelestialBody.DISCOVERY_ONE, "2001: A Space Odyssey", "Jupiter · Solar System",
        R.string.scifi_type_spacecraft, SpaceCompassCelestialBody.JUPITER, null,
        "fictional_discovery_one.webp", "https://catalog.afi.com/Catalog/moviedetails/23399", R.string.scifi_description_discovery_one, filterType = SpaceCompassCatalogType.SPACECRAFT, spacecraft = true),
) }

private val fictionalByBody by lazy { spaceCompassFictionalReferences.associateBy { it.body } }
internal val SpaceCompassCelestialBody.fictionalReference: SpaceCompassFictionalReference? get() = fictionalByBody[this]
internal val spaceCompassNewFictionalBodies: List<SpaceCompassCelestialBody> get() =
    spaceCompassFictionalReferences.map { it.body }.filterNot { it == SpaceCompassCelestialBody.LV_426 }

internal fun calculateSpaceCompassFictionalObservation(reference: SpaceCompassFictionalReference, timeMs: Long,
    latitude: Double, longitude: Double, altitude: Double, remote: SpaceCompassCelestialRemoteData): SpaceCompassCelestialObservation? {
    val observation = reference.hostBody?.let {
        calculateSpaceCompassCelestialObservation(it, timeMs, latitude, longitude, altitude, remote)
    } ?: reference.stellarReference?.let { calculateSpaceCompassDeepSkyObservation(it, timeMs, Observer(latitude, longitude, altitude)) }
    // The distance belongs to the reference, not to an observed fictional planet or ship.
    return observation?.copy(distanceKm = Double.NaN)
}

internal val SpaceCompassCelestialBody.hasIllustratedRings: Boolean get() =
    this == SpaceCompassCelestialBody.SATURN || this == SpaceCompassCelestialBody.CALPAMOS
