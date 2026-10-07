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
            normalized(body.name).contains(needle, ignoreCase = true)
    }
}
