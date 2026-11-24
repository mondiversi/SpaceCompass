package me.mondiversi.spacecompass

/** Constant white reference lines without an outline. */
internal object SpaceCompassAngularGridAppearance {
    const val lineArgb = 0xffffffff.toInt()
    const val majorArgb = lineArgb
    const val liveLineArgb = lineArgb
    const val liveLineDp = .30f
    fun lineWidth(width: Int, height: Int) = minOf(2f * width, height.toFloat()) / 4096f
}

/** Coordinate numbers and named planetary guides keep their independent presentation. */
internal data class SpaceCompassGridPalette(val lineArgb: Int,val majorArgb: Int,val liveLineArgb: Int)

internal const val SPACE_COMPASS_GRID_OPACITY_KEY = "reference_grid_opacity_percent"
internal const val SPACE_COMPASS_GRID_OPACITY_DEFAULT = 50

/** The same eleven stops serve preferences, captures and scene documents. */
internal fun spaceCompassGridOpacity(percent: Int): Int = (percent.coerceIn(0, 100) + 5) / 10 * 10

/** Scale the white lines independently of the Sun, theme or observer position. */
internal fun spaceCompassGridPalette(opacityPercent: Int = SPACE_COMPASS_GRID_OPACITY_DEFAULT): SpaceCompassGridPalette {
    val opacity=spaceCompassGridOpacity(opacityPercent)
    fun ink(argb: Int)=(argb and 0xffffff) or ((((argb ushr 24)*opacity+50)/100) shl 24)
    return SpaceCompassGridPalette(ink(SpaceCompassAngularGridAppearance.lineArgb),
        ink(SpaceCompassAngularGridAppearance.majorArgb),ink(SpaceCompassAngularGridAppearance.liveLineArgb))
}

/** Captures retain their saved opacity without recalculating observer or solar data. */
internal fun spaceCompassGridPalette(snapshot: SpaceCompassPanoramaSnapshot): SpaceCompassGridPalette =
    spaceCompassGridPalette(snapshot.gridOpacityPercent)

/** Canvas expects RRGGBBAA, whereas the shared Android palette stores AARRGGBB. */
internal fun spaceCompassGridCss(argb: Int)="#%06x%02x".format(argb and 0xffffff,argb ushr 24)

/** The effective Location's solar phase controls Sky ink, independently of weather and phone theme. */
internal fun spaceCompassSkyGridBlack(phase: SpaceCompassSunSkyPhase): Boolean =
    phase==SpaceCompassSunSkyPhase.MORNING || phase==SpaceCompassSunSkyPhase.AFTERNOON

/** One ink across the complete grid, preserving the chosen opacity and fine widths. */
internal fun spaceCompassAngularGridPalette(dark: Boolean,
    opacityPercent: Int = SPACE_COMPASS_GRID_OPACITY_DEFAULT): SpaceCompassGridPalette {
    val white=spaceCompassGridPalette(opacityPercent)
    val black=white.lineArgb and 0xff000000.toInt()
    return if(dark) SpaceCompassGridPalette(black,black,black) else white
}

/** Only synthetic Sky follows this policy; real camera and geographic Globe grids stay white. */
internal fun spaceCompassSkyGridPalette(snapshot: SpaceCompassPanoramaSnapshot, enabled: Boolean = true) =
    spaceCompassAngularGridPalette(enabled && spaceCompassSkyGridBlack(snapshot.phase),snapshot.gridOpacityPercent)
