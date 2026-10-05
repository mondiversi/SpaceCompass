package me.mondiversi.spacecompass

import kotlin.math.min

internal const val SPACE_COMPASS_CELESTIAL_MIN_ZOOM = 0.75
internal const val SPACE_COMPASS_CELESTIAL_MAX_ZOOM = 3.0

/** Screen-normalized pan survives resizing; a pinch anchors the point under the fingers. */
internal data class SpaceCompassCelestialViewportState(
    val zoom: Double = 1.0, val panX: Double = 0.0, val panY: Double = 0.0
) {
    fun transform(factor: Double, centerX: Double = 0.0, centerY: Double = 0.0,
        deltaX: Double = 0.0, deltaY: Double = 0.0): SpaceCompassCelestialViewportState {
        if (!factor.isFinite() || !centerX.isFinite() || !centerY.isFinite() ||
            !deltaX.isFinite() || !deltaY.isFinite() || factor <= 0) return this
        val nextZoom = (zoom * factor).coerceIn(SPACE_COMPASS_CELESTIAL_MIN_ZOOM, SPACE_COMPASS_CELESTIAL_MAX_ZOOM)
        val ratio = nextZoom / zoom
        // Keep the object reachable, and recenter as it shrinks back to its original size.
        val limit = (nextZoom - 1.0).coerceAtLeast(0.0) * 0.8
        if (limit == 0.0) return SpaceCompassCelestialViewportState(nextZoom)
        return SpaceCompassCelestialViewportState(nextZoom,
            (panX + (panX - centerX) * (ratio - 1.0) + deltaX).coerceIn(-limit, limit),
            (panY + (panY - centerY) * (ratio - 1.0) + deltaY).coerceIn(-limit, limit))
    }
}

/** Power-of-two map size, capped at 2K and at the graphics device's actual texture limit. */
internal fun spaceCompassCelestialTextureWidth(sourceWidth: Int, maximumTextureSize: Int): Int {
    val limit = min(2048, min(sourceWidth, maximumTextureSize)).coerceAtLeast(1)
    var width = 1
    while (width <= limit / 2) width *= 2
    return width
}
