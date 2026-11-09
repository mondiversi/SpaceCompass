package me.mondiversi.spacecompass

internal const val SPACE_COMPASS_OVERLAY_GESTURE_MARGIN_DP=12.0

/** Protect nearby misses without moving controls or expanding their click targets. */
internal fun spaceCompassOverlayGestureAllowed(point: SpaceCompassSunScenePoint,
    frames: List<SpaceCompassSunSceneFrame>,margin: Double): Boolean {
    val gap=margin.takeIf { it.isFinite()&&it>=0 } ?: 0.0
    return point.x.isFinite()&&point.y.isFinite()&&frames.none {
        point.x>=it.left-gap&&point.x<=it.left+it.width+gap&&
            point.y>=it.top-gap&&point.y<=it.top+it.height+gap
    }
}
