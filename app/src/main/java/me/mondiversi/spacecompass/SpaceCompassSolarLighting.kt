package me.mondiversi.spacecompass

import kotlin.math.roundToInt

internal enum class SpaceCompassSolarLightBand { NIGHT, TWILIGHT, HORIZON, DAY }
internal data class SpaceCompassSolarLighting(val from: SpaceCompassSolarLightBand,
    val to: SpaceCompassSolarLightBand = from, val mix: Float = 0f)

/** Continuous illustrative lighting, driven by geometric solar height rather than a phase switch.
 * USNO: astronomical/civil twilight limits are -18/-6 degrees; solar rise/set is near -0.833.
 * https://aa.usno.navy.mil/faq/RST_defs
 * The same height gives the same light on either side of solar noon, including polar days.
 */
internal fun spaceCompassSolarLighting(elevation: Double?): SpaceCompassSolarLighting {
    val h = elevation?.takeIf { it.isFinite() } ?: -90.0
    fun transition(from: SpaceCompassSolarLightBand, to: SpaceCompassSolarLightBand,
        low: Double, high: Double): SpaceCompassSolarLighting {
        val t = ((h - low) / (high - low)).coerceIn(0.0, 1.0)
        return SpaceCompassSolarLighting(from, to, (t * t * (3 - 2 * t)).toFloat())
    }
    return when {
        h <= -18 -> SpaceCompassSolarLighting(SpaceCompassSolarLightBand.NIGHT)
        h < -6 -> transition(SpaceCompassSolarLightBand.NIGHT, SpaceCompassSolarLightBand.TWILIGHT, -18.0, -6.0)
        h < -0.833 -> transition(SpaceCompassSolarLightBand.TWILIGHT, SpaceCompassSolarLightBand.HORIZON, -6.0, -0.833)
        h <= 2 -> SpaceCompassSolarLighting(SpaceCompassSolarLightBand.HORIZON)
        h < 8 -> transition(SpaceCompassSolarLightBand.HORIZON, SpaceCompassSolarLightBand.DAY, 2.0, 8.0)
        else -> SpaceCompassSolarLighting(SpaceCompassSolarLightBand.DAY)
    }
}

/** Preserve legacy phase fixtures when no solar sample was supplied. Live/captured scenes supply one. */
internal fun <T> spaceCompassSolarLightingPalette(phase: SpaceCompassSunSkyPhase,
    lighting: SpaceCompassSolarLighting?, palette: (SpaceCompassSunSkyPhase) -> T,
    blend: (T, T, Float) -> T): T {
    if (lighting == null) return palette(phase)
    fun band(value: SpaceCompassSolarLightBand): T = when (value) {
        SpaceCompassSolarLightBand.NIGHT -> palette(SpaceCompassSunSkyPhase.NIGHT)
        SpaceCompassSolarLightBand.TWILIGHT -> palette(SpaceCompassSunSkyPhase.EVENING)
        SpaceCompassSolarLightBand.HORIZON -> blend(palette(SpaceCompassSunSkyPhase.DAWN), palette(SpaceCompassSunSkyPhase.SUNSET), .5f)
        SpaceCompassSolarLightBand.DAY -> blend(palette(SpaceCompassSunSkyPhase.MORNING), palette(SpaceCompassSunSkyPhase.AFTERNOON), .5f)
    }
    val from = band(lighting.from)
    return if (lighting.from == lighting.to) from else blend(from, band(lighting.to), lighting.mix)
}

internal fun spaceCompassSolarArgb(from: Int, to: Int, fraction: Float): Int {
    val t = fraction.coerceIn(0f, 1f)
    if (t == 0f) return from
    if (t == 1f) return to
    var result = 0
    for (shift in 0..24 step 8) {
        val a = (from ushr shift) and 255
        val b = (to ushr shift) and 255
        result = result or ((a + (b - a) * t).roundToInt() shl shift)
    }
    return result
}
