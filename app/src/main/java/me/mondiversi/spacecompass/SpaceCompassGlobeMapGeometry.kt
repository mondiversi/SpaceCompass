package me.mondiversi.spacecompass

import kotlin.math.*

internal const val SPACE_COMPASS_GLOBE_MAP_MAX_ZOOM = 8.0

/** Flat-map pan is independent from the spherical Earth's tilt and seasonal rotation. */
internal fun spaceCompassGlobeMapPose(value: SpaceCompassGlobePose, width: Double? = null, height: Double? = null): SpaceCompassGlobePose {
    val base = value.normalized(SPACE_COMPASS_GLOBE_MAP_MAX_ZOOM).copy(roll = 0.0)
    if (width == null || height == null || width <= 0 || height <= 0) return base
    val mapHeight = width * base.zoom / 2
    val latitudeLimit = ((mapHeight - height) / 2).coerceAtLeast(0.0) / mapHeight * PI
    return base.copy(latitude = base.latitude.coerceIn(-latitudeLimit, latitudeLimit))
}

/** Pan in rendered map pixels without moving either polar edge inside the viewport. */
internal fun spaceCompassGlobeMapDrag(pose: SpaceCompassGlobePose, dx: Double, dy: Double,
    width: Double, height: Double): SpaceCompassGlobePose {
    if (width <= 0 || height <= 0) return spaceCompassGlobeMapPose(pose)
    val base = spaceCompassGlobeMapPose(pose, width, height)
    val mapWidth = width * base.zoom
    return spaceCompassGlobeMapPose(base.copy(longitude = base.longitude + dx / mapWidth * PI * 2,
        latitude = base.latitude - dy / (mapWidth / 2) * PI), width, height)
}

/** Landscape panoramas span the scene behind panels; portrait panels still bound the map. */
internal fun spaceCompassGlobeMapViewport(pointing: SpaceCompassSunSceneFrame, sceneWidth: Double,
    sceneHeight: Double, gutter: Double, footerTop: Double? = null): SpaceCompassSunSceneFrame {
    if (sceneWidth > sceneHeight) return SpaceCompassSunSceneFrame(0.0, 0.0, sceneWidth, sceneHeight)
    val left = (pointing.left - gutter).coerceAtLeast(0.0)
    val right = (pointing.left + pointing.width + gutter).coerceAtMost(sceneWidth)
    val bottom = min(sceneHeight, pointing.top + pointing.height)
    val availableBottom = footerTop?.let { min(bottom, (it - gutter).coerceAtLeast(1.0)) } ?: bottom
    return SpaceCompassSunSceneFrame(left, 0.0, (right - left).coerceAtLeast(1.0), availableBottom.coerceAtLeast(1.0))
}
