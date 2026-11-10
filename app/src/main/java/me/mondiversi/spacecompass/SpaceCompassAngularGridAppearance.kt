package me.mondiversi.spacecompass

/** Grids stay subordinate to trajectories; image strokes follow height-filling preview scale, capped by their previous width-based thickness. */
internal object SpaceCompassAngularGridAppearance {
    const val lineArgb = 0x59ffffff
    const val shadowArgb = 0x40000000
    const val majorArgb = 0x7fffffff
    const val liveLineArgb = 0x30ffffff
    const val liveShadowArgb = 0x20000000
    const val liveLineDp = .4f
    const val liveShadowDp = .7f
    fun lineWidth(width: Int, height: Int) = minOf(2f * width, height.toFloat()) / 4096f
}
