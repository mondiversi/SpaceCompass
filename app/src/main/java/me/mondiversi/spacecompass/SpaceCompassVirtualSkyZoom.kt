package me.mondiversi.spacecompass

import kotlin.math.tan

internal const val SPACE_COMPASS_VIRTUAL_ZOOM_KEY = "virtual_sky_zoom_ratio"
internal const val SPACE_COMPASS_VIRTUAL_ZOOM_DEFAULT = 1f
internal val spaceCompassVirtualZoomRange = SpaceCompassCameraZoomRange(.5f, 1f)

/** Independent of physical camera optics and the old fixed virtual focal length. */
internal fun spaceCompassVirtualZoom(stored: Float): Float =
    if (stored.isFinite()) spaceCompassVirtualZoomRange.snap(stored) else SPACE_COMPASS_VIRTUAL_ZOOM_DEFAULT

/** The same pinhole lens drives sky markers, terrain and the star atlas. */
internal fun spaceCompassVirtualPerspective(width: Double, height: Double, zoom: Float): SpaceCompassPerspective {
    require(width.isFinite() && height.isFinite() && width > 0 && height > 0)
    val focal = spaceCompassVirtualFocalLength(height, zoom)
    return SpaceCompassPerspective(focal / width, focal / height)
}

internal fun spaceCompassVirtualFocalLength(height: Double, zoom: Float): Double {
    require(height.isFinite() && height > 0 && zoom.isFinite() && zoom > 0)
    return height * zoom / (2 * tan(Math.toRadians(30.0)))
}
