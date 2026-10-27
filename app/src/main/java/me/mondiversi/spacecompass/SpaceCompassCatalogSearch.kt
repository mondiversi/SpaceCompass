package me.mondiversi.spacecompass

import java.text.Normalizer

/** Search only the supplied visible catalog; scientific IDs also match independently of UI language. */
internal fun spaceCompassSearchCatalog(available: List<SpaceCompassCelestialBody>,
    localizedNames: Map<SpaceCompassCelestialBody, String>, query: String): List<SpaceCompassCelestialBody> {
    fun normalized(value: String) = Normalizer.normalize(value, Normalizer.Form.NFD)
        .filter { it.isLetterOrDigit() }
    val needle = normalized(query)
    if (needle.isEmpty()) return available
    return available.filter { body ->
        normalized(localizedNames[body].orEmpty()).contains(needle, ignoreCase = true) ||
            normalized(body.name).contains(needle, ignoreCase = true) ||
            body.catalogAliases.any { normalized(it).contains(needle, ignoreCase = true) }
    }
}

private val SpaceCompassCelestialBody.catalogAliases: List<String> get() = when (this) {
    SpaceCompassCelestialBody.CERES -> listOf("1 Ceres", "Cerere", "Cérès", "A801 AA")
    SpaceCompassCelestialBody.ENCELADUS -> listOf("Enceladus", "Encelado", "Encelade", "Saturn II", "602")
    SpaceCompassCelestialBody.VEGA -> listOf("Vega", "Wega", "Alpha Lyrae", "HD 172167")
    SpaceCompassCelestialBody.DENEB -> listOf("Deneb", "Alpha Cygni", "HD 197345")
    SpaceCompassCelestialBody.ALTAIR -> listOf("Altair", "Altaïr", "Alpha Aquilae", "HD 187642")
    SpaceCompassCelestialBody.SOUTHERN_CROSS -> listOf("Crux", "Cru", "Southern Cross", "Croce del Sud", "Acrux", "Gacrux", "Mimosa", "Imai")
    SpaceCompassCelestialBody.NEW_HORIZONS -> listOf("New Horizons","NH","2006-001A")
    SpaceCompassCelestialBody.JWST -> listOf("James Webb Space Telescope","Webb","JWST","2021-130A")
    SpaceCompassCelestialBody.PARKER_SOLAR_PROBE -> listOf("Parker Solar Probe","PSP","Solar Probe Plus")
    SpaceCompassCelestialBody.APOPHIS -> listOf("99942","2004 MN4","Apophis")
    SpaceCompassCelestialBody.ATLAS_3I -> listOf("3I/ATLAS","C/2025 N1","interstellar")
    SpaceCompassCelestialBody.EUROPA_CLIPPER -> listOf("Europa Clipper","Clipper")
    SpaceCompassCelestialBody.JUICE -> listOf("Jupiter Icy Moons Explorer","JUICE")
    SpaceCompassCelestialBody.HUBBLE -> listOf("Hubble Space Telescope","HST","20580")
    SpaceCompassCelestialBody.TIANGONG -> listOf("Tiangong","CSS","Tianhe","48274")
    SpaceCompassCelestialBody.M87 -> listOf("M87*","NGC 4486","Virgo A")
    SpaceCompassCelestialBody.PROXIMA_B -> listOf("Proxima Centauri b","Proxima b","Alpha Centauri C b")

    SpaceCompassCelestialBody.TESLA_ROADSTER -> listOf("Starman", "SpaceX Roadster", "2018-017A", "43205")
    SpaceCompassCelestialBody.SIRIUS -> listOf("Sirius", "Sirio", "Alpha Canis Majoris", "HD 48915")
    SpaceCompassCelestialBody.BETELGEUSE -> listOf("Betelgeuse", "Betelgeux", "Alpha Orionis", "HD 39801")
    SpaceCompassCelestialBody.ORION_NEBULA -> listOf("M42", "NGC 1976", "Orion Nebula", "Nebulosa di Orione")
    SpaceCompassCelestialBody.PLEIADES -> listOf("M45", "Melotte 22", "Pleiades", "Pleiadi")
    SpaceCompassCelestialBody.TITAN -> listOf("Titan", "Titano", "Saturn VI")
    else -> fictionalReference?.let { listOf(it.universe, it.referenceName) + spaceCompassFictionalAliases(this) } ?: (spaceCompassAdditionalCatalogAliases(this) + listOfNotNull(additionalCatalogEntry?.designation))
}
