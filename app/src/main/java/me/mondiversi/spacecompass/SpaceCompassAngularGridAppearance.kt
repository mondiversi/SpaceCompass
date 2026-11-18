package me.mondiversi.spacecompass

/** Dawn through dusk shares bright daytime ink; night retains the reviewed grey and fine strokes. */
internal object SpaceCompassAngularGridAppearance {
    const val dayArgb = 0xffffffff.toInt()
    const val lineArgb = 0xffbdbdbd.toInt()
    const val shadowArgb = 0x40000000
    const val majorArgb = 0xffbdbdbd.toInt()
    const val liveLineArgb = 0xffbdbdbd.toInt()
    const val liveShadowArgb = 0x59000000
    const val liveLineDp = .30f
    const val liveShadowDp = .60f
    fun lineWidth(width: Int, height: Int) = minOf(2f * width, height.toFloat()) / 4096f
}

/** Coordinate numbers and named planetary guides keep their independent presentation. */
internal data class SpaceCompassGridPalette(val lineArgb: Int,val shadowArgb: Int,val majorArgb: Int,
    val liveLineArgb: Int,val liveShadowArgb: Int)

internal const val SPACE_COMPASS_GRID_OPACITY_KEY = "reference_grid_opacity_percent"
internal const val SPACE_COMPASS_GRID_OPACITY_DEFAULT = 80

/** The same eleven stops serve preferences, captures and scene documents. */
internal fun spaceCompassGridOpacity(percent: Int): Int = (percent.coerceIn(0, 100) + 5) / 10 * 10

/** Match the backdrop: bright at the first twilight above -18 degrees; night keeps the previous ink. */
internal fun spaceCompassGridUsesDayInk(elevationDegrees: Double?): Boolean =
    elevationDegrees?.takeIf { it.isFinite() }?.let { it > -18.0 } ?: false

internal fun spaceCompassGridPalette(solarElevation: Double? = null,
    opacityPercent: Int = SPACE_COMPASS_GRID_OPACITY_DEFAULT): SpaceCompassGridPalette {
    val opacity=spaceCompassGridOpacity(opacityPercent)
    val day=spaceCompassGridUsesDayInk(solarElevation)
    fun core(fallback: Int)=if(day) SpaceCompassAngularGridAppearance.dayArgb else fallback
    fun ink(argb: Int)=(argb and 0xffffff) or ((((argb ushr 24)*opacity+50)/100) shl 24)
    return SpaceCompassGridPalette(ink(core(SpaceCompassAngularGridAppearance.lineArgb)),
        ink(SpaceCompassAngularGridAppearance.shadowArgb),ink(core(SpaceCompassAngularGridAppearance.majorArgb)),
        ink(core(SpaceCompassAngularGridAppearance.liveLineArgb)),ink(SpaceCompassAngularGridAppearance.liveShadowArgb))
}

/** Frozen time/location determine the Sun, independently of theme, objects and location disclosure. */
internal fun spaceCompassGridPalette(snapshot: SpaceCompassPanoramaSnapshot): SpaceCompassGridPalette {
    val elevation=if(snapshot.observerPositionKnown) runCatching {
        calculateSpaceCompassSunPosition(snapshot.timeMs,snapshot.latitude,snapshot.longitude,snapshot.altitude).elevationDegrees
    }.getOrNull() else null
    return spaceCompassGridPalette(elevation,snapshot.gridOpacityPercent)
}

/** Canvas expects RRGGBBAA, whereas the shared Android palette stores AARRGGBB. */
internal fun spaceCompassGridCss(argb: Int)="#%06x%02x".format(argb and 0xffffff,argb ushr 24)
