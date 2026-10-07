package me.mondiversi.spacecompass

import kotlin.math.abs
import kotlin.math.hypot

/** These are declinations of the projected parallels, never the observer's local elevation. */
internal fun formatSpaceCompassSkyReferenceDegrees(degrees: Double,
    numeric: SpaceCompassNumericFormat = SpaceCompassNumericFormat.INTERNATIONAL,
    systemLocale: java.util.Locale = java.util.Locale.getDefault()): String {
    val magnitude = abs(degrees)
    val digits = if (magnitude == 0.0 || magnitude == 90.0) 0 else 1
    val sign = if (degrees > 0.0) "+" else if (degrees < 0.0) "−" else ""
    return sign + formatSpaceCompassNumber(magnitude, digits, numeric, grouping = false, systemLocale = systemLocale) + "°"
}

/** Pick widely separated visible curve samples. Wrapped panorama edges are neighbors;
 * disconnected clipped fragments never get joined or acquire invented anchors.
 */
internal fun spaceCompassSkyReferenceDegreeAnchors(candidates: List<SpaceCompassSunScenePoint>,
    minimumSpacing: Double, reserved: List<SpaceCompassSunScenePoint> = emptyList(),
    wrapWidth: Double? = null, maximumLabels: Int = 8): List<SpaceCompassSunScenePoint> {
    if (!minimumSpacing.isFinite() || minimumSpacing <= 0.0 || maximumLabels <= 0) return emptyList()
    val wrap = wrapWidth?.takeIf { it.isFinite() && it > 0.0 }
    fun distance(a: SpaceCompassSunScenePoint, b: SpaceCompassSunScenePoint): Double {
        var dx = abs(a.x - b.x)
        if (wrap != null) { dx %= wrap; dx = minOf(dx, wrap - dx) }
        return hypot(dx, a.y - b.y)
    }
    val accepted = mutableListOf<SpaceCompassSunScenePoint>()
    val blocked = reserved.filter { it.x.isFinite() && it.y.isFinite() }
    for (point in candidates) {
        if (!point.x.isFinite() || !point.y.isFinite()) continue
        if (blocked.any { distance(it, point) < minimumSpacing } ||
            accepted.any { distance(it, point) < minimumSpacing }) continue
        accepted += point
        if (accepted.size == maximumLabels) break
    }
    return accepted
}
