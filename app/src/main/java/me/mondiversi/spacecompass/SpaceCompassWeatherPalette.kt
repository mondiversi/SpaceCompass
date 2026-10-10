package me.mondiversi.spacecompass

internal data class SpaceCompassWeatherPalette(
    val overcastTopArgb: Int, val overcastBottomArgb: Int,
    val cloudTopArgb: Int, val cloudBottomArgb: Int,
    val fogTopArgb: Int, val fogBottomArgb: Int,
    val rainArgb: Int, val snowArgb: Int
)

/** Illustrative atmospheric lighting follows local solar phase, independently of UI theme.
 * Both the live sky and frozen panorama use this palette; coverage and geometry are unchanged.
 * Twilight illumination is weaker overhead and warmer near the horizon.
 */
internal fun spaceCompassWeatherPalette(phase: SpaceCompassSunSkyPhase,
    storm: Boolean = false): SpaceCompassWeatherPalette {
    val palette = when (phase) {
        SpaceCompassSunSkyPhase.NIGHT -> SpaceCompassWeatherPalette(
            0xFF111B2B.toInt(), 0xFF28374A.toInt(), 0xFF182539.toInt(), 0xFF35455B.toInt(),
            0xFF263346.toInt(), 0xFF425268.toInt(), 0x4D687D99, 0xA68D9FB8.toInt())
        SpaceCompassSunSkyPhase.DAWN -> SpaceCompassWeatherPalette(
            0xFF525B73.toInt(), 0xFFB18D7C.toInt(), 0xFF68718A.toInt(), 0xFFD0AD96.toInt(),
            0xFF828494.toInt(), 0xFFD7BBA3.toInt(), 0x4DACA4AE, 0xA6DED0C6.toInt())
        SpaceCompassSunSkyPhase.MORNING -> SpaceCompassWeatherPalette(
            0xFF536677.toInt(), 0xFFB7C4CE.toInt(), 0xFFBBC8D3.toInt(), 0xFFEDF1F3.toInt(),
            0xFFC4CED6.toInt(), 0xFFDEE4E7.toInt(), 0x4DE3F2FF, 0xA6FFFFFF.toInt())
        SpaceCompassSunSkyPhase.AFTERNOON -> SpaceCompassWeatherPalette(
            0xFF4F6274.toInt(), 0xFFA9BAC7.toInt(), 0xFFA8B7C5.toInt(), 0xFFD8E2E8.toInt(),
            0xFFB9C6D0.toInt(), 0xFFD2DCE2.toInt(), 0x4DCADDEB, 0xA6E5EEF5.toInt())
        SpaceCompassSunSkyPhase.SUNSET -> SpaceCompassWeatherPalette(
            0xFF403B53.toInt(), 0xFF926F66.toInt(), 0xFF594D68.toInt(), 0xFFBA8A71.toInt(),
            0xFF766A7B.toInt(), 0xFFBE9D89.toInt(), 0x4D968A9A, 0xA6C1ACAA.toInt())
        SpaceCompassSunSkyPhase.EVENING -> SpaceCompassWeatherPalette(
            0xFF1D2639.toInt(), 0xFF484555.toInt(), 0xFF30364C.toInt(), 0xFF685E73.toInt(),
            0xFF3F465B.toInt(), 0xFF716A7C.toInt(), 0x4D778096, 0xA6A2A7BE.toInt())
    }
    // Storm clouds absorb more light, while retaining the same solar tint as other clouds.
    return if (!storm) palette else palette.copy(
        cloudTopArgb = shadeSpaceCompassWeatherArgb(palette.cloudTopArgb, .58f),
        cloudBottomArgb = shadeSpaceCompassWeatherArgb(palette.cloudBottomArgb, .58f)
    )
}

private fun shadeSpaceCompassWeatherArgb(argb: Int, light: Float): Int {
    fun channel(shift: Int) = (((argb ushr shift) and 255) * light).toInt() shl shift
    return (argb and 0xFF000000.toInt()) or channel(16) or channel(8) or channel(0)
}

internal fun spaceCompassWeatherPalette(phase: SpaceCompassSunSkyPhase, storm: Boolean,
    solarLighting: SpaceCompassSolarLighting?): SpaceCompassWeatherPalette =
    spaceCompassSolarLightingPalette(phase, solarLighting, { spaceCompassWeatherPalette(it, storm) }) { a, b, t ->
        SpaceCompassWeatherPalette(spaceCompassSolarArgb(a.overcastTopArgb, b.overcastTopArgb, t),
            spaceCompassSolarArgb(a.overcastBottomArgb, b.overcastBottomArgb, t),
            spaceCompassSolarArgb(a.cloudTopArgb, b.cloudTopArgb, t), spaceCompassSolarArgb(a.cloudBottomArgb, b.cloudBottomArgb, t),
            spaceCompassSolarArgb(a.fogTopArgb, b.fogTopArgb, t), spaceCompassSolarArgb(a.fogBottomArgb, b.fogBottomArgb, t),
            spaceCompassSolarArgb(a.rainArgb, b.rainArgb, t), spaceCompassSolarArgb(a.snowArgb, b.snowArgb, t))
    }
