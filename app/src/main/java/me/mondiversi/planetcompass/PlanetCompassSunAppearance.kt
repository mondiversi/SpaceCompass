package me.mondiversi.planetcompass

import kotlin.math.roundToInt

internal data class PlanetCompassSunAppearance(
    val edgeArgb: Int, val coreArgb: Int, val glowStrength: Float,
    val glowRadiusDp: Float, val belowHorizon: Boolean
)

/** Shared schematic target palette, not an object's physical colour or atmospheric measurement. */
internal fun planetCompassSunAppearance(elevation: Double, rising: Boolean): PlanetCompassSunAppearance {
    require(elevation.isFinite())
    if (elevation < 0) return PlanetCompassSunAppearance(0xFFBBA6FF.toInt(), 0xFFE9E0FF.toInt(), 0f, 24f, true)
    val height = elevation.coerceIn(0.0, 90.0)
    val stops = listOf(
        0.0 to if (rising) 0xFFFF9458.toInt() else 0xFFFF7953.toInt(),
        6.0 to 0xFFFFC76B.toInt(), 20.0 to 0xFFFFE494.toInt(),
        55.0 to 0xFFFFF1CC.toInt(), 90.0 to 0xFFFFF9E8.toInt()
    )
    val upper = stops.indexOfFirst { it.first >= height }.coerceAtLeast(0)
    val edge = if (upper == 0) stops[0].second else {
        val before = stops[upper - 1]; val after = stops[upper]
        mixPlanetCompassSunArgb(before.second, after.second, (height - before.first) / (after.first - before.first))
    }
    val glowProgress = ((height - 6) / 34).coerceIn(0.0, 1.0)
    val smoothGlow = glowProgress * glowProgress * (3 - 2 * glowProgress)
    return PlanetCompassSunAppearance(edge, mixPlanetCompassSunArgb(edge, 0xFFFFFEF7.toInt(), 0.25 + height / 90 * 0.65),
        (smoothGlow * 0.48).toFloat(), (30 + smoothGlow * 34).toFloat(), false)
}

private fun mixPlanetCompassSunArgb(first: Int, second: Int, amount: Double): Int {
    var result = 0xFF000000.toInt()
    for (shift in listOf(16, 8, 0)) {
        val a = (first ushr shift) and 0xFF; val b = (second ushr shift) and 0xFF
        result = result or ((a + (b - a) * amount).roundToInt().coerceIn(0, 255) shl shift)
    }
    return result
}
