package me.mondiversi.spacecompass

internal const val SPACE_COMPASS_POINTING_TOP_EDGE_KEY = "sky_pointing_top_edge"

/** A virtual view along the physical top edge; camera calibration always keeps the optical axis. */
internal fun spaceCompassSunViewOrientation(orientation: SpaceCompassSunOrientation?, topEdge: Boolean,
    cameraEnabled: Boolean, displayRotation: Int): SpaceCompassSunOrientation? {
    if (orientation == null || !topEdge || cameraEnabled) return orientation
    fun opposite(v: SpaceCompassSunVector) = SpaceCompassSunVector(-v.east, -v.north, -v.up)
    // Undo Android's display remapping, so landscape never substitutes the phone's short edge.
    val right = when (displayRotation) {
        1 -> orientation.screenUp
        2 -> opposite(orientation.right)
        3 -> opposite(orientation.screenUp)
        else -> orientation.right
    }
    val forward = when (displayRotation) {
        1 -> opposite(orientation.right)
        2 -> opposite(orientation.screenUp)
        3 -> orientation.right
        else -> orientation.screenUp
    }
    return SpaceCompassSunOrientation(right, opposite(orientation.forward), forward)
}
