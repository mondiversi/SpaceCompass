package me.mondiversi.spacecompass

/** Grids stay subordinate to trajectories; image strokes follow height-filling preview scale, capped by their previous width-based thickness. */
internal object SpaceCompassAngularGridAppearance {
    const val lineArgb = 0xff9c9c9c.toInt()
    const val shadowArgb = 0x40000000
    const val majorArgb = 0xff9c9c9c.toInt()
    const val liveLineArgb = 0xff9c9c9c.toInt()
    const val liveShadowArgb = 0x59000000
    const val liveLineDp = .30f
    const val liveShadowDp = .60f
    fun lineWidth(width: Int, height: Int) = minOf(2f * width, height.toFloat()) / 4096f
}

/** Only reference-grid ink changes. Stroke widths, number labels and named guides keep their policy. */
internal data class SpaceCompassGridPalette(val lineArgb: Int,val shadowArgb: Int,val majorArgb: Int,
    val liveLineArgb: Int,val liveShadowArgb: Int)

/** Civil dawn/day/dusk share one ink; only a Sun below -6 degrees uses night ink. */
internal fun spaceCompassGridUsesDayInk(elevationDegrees: Double?): Boolean =
    elevationDegrees?.takeIf { it.isFinite() }?.let { it>=-6.0 } ?: false

internal const val SPACE_COMPASS_GRID_OPACITY_KEY = "reference_grid_opacity_percent"
internal const val SPACE_COMPASS_GRID_OPACITY_DEFAULT = 80

/** The same eleven stops serve preferences, captures and scene documents. */
internal fun spaceCompassGridOpacity(percent: Int): Int = (percent.coerceIn(0, 100) + 5) / 10 * 10

internal fun spaceCompassGridPalette(elevationDegrees: Double?,
    opacityPercent: Int = SPACE_COMPASS_GRID_OPACITY_DEFAULT): SpaceCompassGridPalette {
    val day=spaceCompassGridUsesDayInk(elevationDegrees)
    val opacity = spaceCompassGridOpacity(opacityPercent)
    fun ink(argb: Int) = (argb and 0xffffff) or ((((argb ushr 24) * opacity + 50) / 100) shl 24)
    return SpaceCompassGridPalette(
        ink(if(day) 0xff707070.toInt() else SpaceCompassAngularGridAppearance.lineArgb),
        ink(SpaceCompassAngularGridAppearance.shadowArgb),
        ink(if(day) 0xff707070.toInt() else SpaceCompassAngularGridAppearance.majorArgb),
        ink(if(day) 0xff707070.toInt() else SpaceCompassAngularGridAppearance.liveLineArgb),
        ink(SpaceCompassAngularGridAppearance.liveShadowArgb))
}

/** A capture keeps its own position and instant, even after the live scenario or theme changes. */
internal fun spaceCompassGridPalette(snapshot: SpaceCompassPanoramaSnapshot): SpaceCompassGridPalette {
    val elevation=if(!snapshot.observerPositionKnown) null else runCatching {
        calculateSpaceCompassSunPosition(snapshot.timeMs,snapshot.latitude,snapshot.longitude,snapshot.altitude).elevationDegrees
    }.getOrNull()
    return spaceCompassGridPalette(elevation, snapshot.gridOpacityPercent)
}

/** Canvas expects RRGGBBAA, whereas the shared Android palette stores AARRGGBB. */
internal fun spaceCompassGridCss(argb: Int)="#%06x%02x".format(argb and 0xffffff,argb ushr 24)
