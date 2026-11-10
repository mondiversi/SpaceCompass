package me.mondiversi.spacecompass

import kotlin.math.*

internal enum class SpaceCompassCaptureProjection(val key: String) { FRAME("frame"), PANORAMA("panorama") }
internal const val SPACE_COMPASS_SKY_CAPTURE_PANORAMA_KEY="sky_capture_panorama"
internal const val SPACE_COMPASS_GLOBE_CAPTURE_PANORAMA_KEY="globe_capture_panorama"
internal fun spaceCompassCaptureProjectionKey(globe: Boolean)=if(globe) SPACE_COMPASS_GLOBE_CAPTURE_PANORAMA_KEY else SPACE_COMPASS_SKY_CAPTURE_PANORAMA_KEY
internal fun spaceCompassCaptureProjection(panoramic: Boolean)=if(panoramic) SpaceCompassCaptureProjection.PANORAMA else SpaceCompassCaptureProjection.FRAME
internal fun spaceCompassCaptureSize(width: Double,height: Double,maxEdge: Int): Pair<Int,Int> {
    require(width.isFinite()&&height.isFinite()&&width>0&&height>0&&maxEdge in 512..4096)
    val factor=maxEdge/max(width,height)
    return max(1,(width*factor).roundToInt()) to max(1,(height*factor).roundToInt())
}
internal data class SpaceCompassSkyFraming(val width: Double,val height: Double,
    val orientation: SpaceCompassSunOrientation,val perspective: SpaceCompassPerspective)
internal fun spaceCompassSkyFraming(visible: SpaceCompassSunSceneFrame,optical: SpaceCompassSunSceneFrame,
    orientation: SpaceCompassSunOrientation,perspective: SpaceCompassPerspective): SpaceCompassSkyFraming {
    require(visible.width>0&&visible.height>0&&optical.width>0&&optical.height>0)
    return SpaceCompassSkyFraming(visible.width,visible.height,orientation,SpaceCompassPerspective(
        perspective.horizontal*optical.width/visible.width,perspective.vertical*optical.height/visible.height,
        (optical.left+perspective.principalX*optical.width-visible.left)/visible.width,
        (optical.top+perspective.principalY*optical.height-visible.top)/visible.height))
}
/** Normalized live WebView geometry, independent of later panels, rotation and output resolution. */
internal data class SpaceCompassGlobeFraming(val width: Double,val height: Double,
    val centerY: Double,val radiusByWidth: Double, val guideTextSizeByHeight: Double = 12.0/600.0)
internal fun spaceCompassGlobeMapPoint(v: SpaceCompassGlobeVector,width: Double,height: Double): SpaceCompassSunScenePoint {
    require(v.length>0&&width>0&&height>0)
    return SpaceCompassSunScenePoint((atan2(v.x,v.z)/(2*PI)+.5)*width,
        (.5-asin((v.y/v.length).coerceIn(-1.0,1.0))/PI)*height)
}
/** Split a dateline crossing at both map edges; never draw a false line across the world. */
internal fun spaceCompassGlobeMapSegments(a: SpaceCompassSunScenePoint,b: SpaceCompassSunScenePoint,width: Double): List<Pair<SpaceCompassSunScenePoint,SpaceCompassSunScenePoint>> {
    if(abs(b.x-a.x)<=width/2) return listOf(a to b)
    val bx=if(b.x>a.x) b.x-width else b.x+width
    val edge=if(bx<0) 0.0 else width
    val y=a.y+(b.y-a.y)*(edge-a.x)/(bx-a.x)
    return listOf(a to SpaceCompassSunScenePoint(edge,y),SpaceCompassSunScenePoint(if(edge==0.0) width else 0.0,y) to b)
}

internal fun spaceCompassCaptureImageTypes(photo: Boolean)=if(photo) listOf(SpaceCompassCaptureExportType.IMAGE) else
    listOf(SpaceCompassCaptureExportType.FRAMED_IMAGE,SpaceCompassCaptureExportType.PANORAMA_IMAGE)

/** Centre the Earth in the complete captured image, including unequal caption and legend bands. */
internal fun spaceCompassGlobeCaptureCenterY(sceneHeight: Int,topBand: Int,bottomBand: Int): Double {
    require(sceneHeight>0 && topBand>=0 && bottomBand>=0)
    return (sceneHeight+bottomBand-topBand)/2.0
}
