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

/** Enter a flat view centred without discarding the user's chosen magnification. */
internal fun spaceCompassGlobeMapCenteredPose(value: SpaceCompassGlobePose): SpaceCompassGlobePose =
    spaceCompassGlobeMapPose(value.copy(longitude=0.0,latitude=0.0))

/** Symmetric breathing room within the measured upper/lower control gap. */
internal fun spaceCompassGlobeMapClearBand(width: Double,height: Double,upper: Double,lower: Double,
    gap: Double): SpaceCompassSunSceneFrame {
    val top=upper.coerceIn(0.0,height)
    val bottom=lower.coerceIn(top,height)
    val inset=gap.coerceIn(0.0,((bottom-top-1.0)/2).coerceAtLeast(0.0))
    return SpaceCompassSunSceneFrame(0.0,top+inset,width,(bottom-top-inset*2).coerceAtLeast(1.0))
}

/** Preserve full-scene panoramic width while centring on the unobstructed pointing area. */
internal fun spaceCompassGlobeMapViewport(pointing: SpaceCompassSunSceneFrame, sceneWidth: Double,
    sceneHeight: Double, gutter: Double, footerTop: Double? = null,
    optical: SpaceCompassSunSceneFrame? = null): SpaceCompassSunSceneFrame {
    if(optical!=null) {
        val fitWidth=if(sceneWidth>sceneHeight) sceneWidth else
            (min(sceneWidth,pointing.left+pointing.width+gutter)-max(0.0,pointing.left-gutter)).coerceAtLeast(1.0)
        val bottom=min(optical.top+optical.height,footerTop?.minus(gutter) ?: sceneHeight)
        val centreX=if(sceneWidth>sceneHeight) optical.left+optical.width/2 else sceneWidth/2
        return SpaceCompassSunSceneFrame(centreX-fitWidth/2,optical.top,
            fitWidth,(bottom-optical.top).coerceAtLeast(1.0))
    }
    if (sceneWidth > sceneHeight) return SpaceCompassSunSceneFrame(0.0, 0.0, sceneWidth, sceneHeight)
    val left = (pointing.left - gutter).coerceAtLeast(0.0)
    val right = (pointing.left + pointing.width + gutter).coerceAtMost(sceneWidth)
    val bottom = min(sceneHeight, pointing.top + pointing.height)
    val availableBottom = footerTop?.let { min(bottom, (it - gutter).coerceAtLeast(1.0)) } ?: bottom
    return SpaceCompassSunSceneFrame(left, 0.0, (right - left).coerceAtLeast(1.0), availableBottom.coerceAtLeast(1.0))
}
