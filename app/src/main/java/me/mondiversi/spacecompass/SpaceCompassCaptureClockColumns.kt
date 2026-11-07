package me.mondiversi.spacecompass

internal data class SpaceCompassCaptureClockColumns(val clock: String, val countdown: String)
private val captureCountdownPattern = Regex("""T[+−-]\p{Nd}+h\p{Nd}+m""")

/** Only the row immediately before the angle row is a clock row; user-defined titles stay untouched. */
internal fun spaceCompassCaptureClockColumns(lines: List<String>, index: Int): SpaceCompassCaptureClockColumns? {
    if (index != lines.lastIndex - 1 || lines.lastOrNull()?.startsWith("↔ ") != true) return null
    val line = lines.getOrNull(index) ?: return null
    val divider = line.lastIndexOf(" · ")
    if (divider < 0) return null
    val clock = line.substring(0, divider)
    val countdown = line.substring(divider + 3)
    if (clock.isBlank() || !captureCountdownPattern.matches(countdown)) return null
    return SpaceCompassCaptureClockColumns(clock, countdown)
}

/** Measure the actual two columns plus a font-relative minimum gap, rather than the visible separator. */
internal fun spaceCompassCaptureCaptionContentWidth(lines: List<String>, minimumGap: Float,
    measure: (String) -> Float): Float = lines.indices.maxOfOrNull { index ->
    spaceCompassCaptureClockColumns(lines, index)?.let { measure(it.clock) + minimumGap + measure(it.countdown) }
        ?: measure(lines[index])
} ?: 0f
