package me.mondiversi.spacecompass

internal data class SpaceCompassScenarioNameParts(val position: String? = null, val altitude: String? = null,
    val zone: String? = null, val date: String? = null, val time: String? = null, val speed: String? = null)

/** Only enabled configuration enters the suggestion; dormant values and inferred metadata stay out. */
internal fun spaceCompassScenarioSuggestedName(archive: SpaceCompassScenarioArchive,
    parts: SpaceCompassScenarioNameParts, fallback: String): String {
    fun clean(value: String) = value.replace(Regex("[\\s\u00a0\u202f]+"), " ")
        .replace(Regex("[\\\\/:*?\"<>|\\p{Cntrl}]"), "-").trim().trim('.')
    fun limited(value: String, max: Int): String = value.take(max).let {
        if (it.isNotEmpty() && Character.isHighSurrogate(it.last())) it.dropLast(1) else it
    }.trimEnd()
    val c = archive.configuration
    val fields = listOf(
        parts.position.takeIf { c.simulatePosition },
        parts.altitude.takeIf { c.simulateAltitude }, parts.zone.takeIf { c.simulateZone },
        parts.date.takeIf { c.simulateDate }, parts.time.takeIf { c.simulateTime },
        parts.speed.takeIf { archive.timeSettings.enabled }
    ).mapNotNull { it?.let(::clean)?.takeIf(String::isNotBlank) }.map { limited(it, 32) }.toMutableList()
    if (fields.isEmpty()) return limited(clean(fallback).ifBlank { "Scenario" }, 80)
    // Preserve compact numerical parameters; shorten the leading place name first.
    while (fields.joinToString(" · ").length > 80) {
        val index = fields.indices.maxBy { fields[it].length }
        fields[index] = limited(fields[index], fields[index].length - 1)
    }
    return fields.joinToString(" · ")
}
