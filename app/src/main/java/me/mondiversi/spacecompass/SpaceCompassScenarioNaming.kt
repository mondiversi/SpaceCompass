package me.mondiversi.spacecompass

internal data class SpaceCompassScenarioNameParts(val position: String? = null, val altitude: String? = null,
    val zone: String? = null, val date: String? = null, val time: String? = null, val speed: String? = null)

/** Only enabled configuration enters the suggestion; dormant values and inferred metadata stay out. */
internal fun spaceCompassScenarioSuggestedName(archive: SpaceCompassScenarioArchive,
    parts: SpaceCompassScenarioNameParts, fallback: String): String {
    val c = archive.configuration
    val fields = listOf(
        parts.position.takeIf { c.simulatePosition },
        parts.altitude.takeIf { c.simulateAltitude }, parts.zone.takeIf { c.simulateZone },
        parts.date.takeIf { c.simulateDate }, parts.time.takeIf { c.simulateTime },
        parts.speed.takeIf { archive.timeSettings.enabled }
    ).mapNotNull { it?.let { value -> spaceCompassExportFilePart(value,32) }?.takeIf(String::isNotBlank) }.toMutableList()
    if (fields.isEmpty()) return spaceCompassExportFilePart(fallback,80).ifBlank { "Location" }
    // Preserve every enabled field, shortening the longest component first.
    while (fields.joinToString("_").let { it.length>80 || it.toByteArray(Charsets.UTF_8).size>220 }) {
        val index = fields.indices.maxBy { fields[it].length }
        fields[index] = spaceCompassLimitFileText(fields[index],fields[index].length-1)
    }
    return fields.joinToString("_")
}
