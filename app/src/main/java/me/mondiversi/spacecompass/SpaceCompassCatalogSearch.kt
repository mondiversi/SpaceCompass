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
    SpaceCompassCelestialBody.SIRIUS -> listOf("Sirius", "Sirio", "Alpha Canis Majoris", "HD 48915")
    SpaceCompassCelestialBody.BETELGEUSE -> listOf("Betelgeuse", "Betelgeux", "Alpha Orionis", "HD 39801")
    SpaceCompassCelestialBody.ORION_NEBULA -> listOf("M42", "NGC 1976", "Orion Nebula", "Nebulosa di Orione")
    SpaceCompassCelestialBody.PLEIADES -> listOf("M45", "Melotte 22", "Pleiades", "Pleiadi")
    SpaceCompassCelestialBody.TITAN -> listOf("Titan", "Titano", "Saturn VI")
    else -> emptyList()
}
