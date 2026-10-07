package me.mondiversi.spacecompass

/** Join frozen export values into one line without disturbing numeric grouping spaces. */
internal fun formatSpaceCompassPanoramaCaption(timestamp: String, place: String?,
    rows: List<SpaceCompassSunDataRow>, cloudPercent: String?): String {
    fun String.singleLine() = replace(Regex("[\\r\\n\\t]+"), " ").trim()
    fun row(tag: String) = rows.firstOrNull { it.tag == tag }
    val parts = mutableListOf(timestamp.singleLine())
    place?.singleLine()?.takeIf { it.isNotBlank() }?.let(parts::add)
    row("sun-info-coordinates")?.let { coordinates ->
        val value = coordinates.value.lineSequence().map { it.trim() }.filter { it.isNotBlank() }
            .joinToString(" · ")
        val accuracy = row("sun-info-accuracy")?.value?.singleLine()
            ?.removePrefix("±")?.trim()?.takeIf { it.isNotBlank() && it != "—" }
        parts += value + (accuracy?.let { " (±$it)" } ?: "")
    }
    row("sun-info-altitude")?.let { altitude ->
        // Horizontal GPS accuracy was already attached to the coordinates; omit the extra vertical figure.
        parts += "${altitude.label.singleLine()} ${altitude.value.substringBefore(" (±").singleLine()}"
    }
    row("celestial-environment-weather")?.let { weather ->
        val cover = cloudPercent?.singleLine()?.takeIf { it.isNotBlank() }
        parts += weather.value.singleLine() + (cover?.let { " ($it)" } ?: "")
    }
    return parts.filter { it.isNotBlank() }.joinToString(" · ")
}
