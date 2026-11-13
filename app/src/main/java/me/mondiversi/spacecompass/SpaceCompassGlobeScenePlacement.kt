package me.mondiversi.spacecompass

/** Separate a full-scene drawing surface from the inset optical/control viewport. */
internal data class SpaceCompassGlobeScenePlacement(
    val pointing: SpaceCompassSunSceneFrame,
    val viewport: SpaceCompassSunSceneFrame,
    val exclusions: List<SpaceCompassSunSceneFrame>,
    val noteBottomPixels: Double,
    val showDirectionNote: Boolean
) {
    fun sceneViewport() = viewport.copy(left=viewport.left+pointing.left,top=viewport.top+pointing.top)

    fun sceneExclusions(width: Double,height: Double): List<SpaceCompassSunSceneFrame> = buildList {
        exclusions.forEach { add(it.copy(left=it.left+pointing.left,top=it.top+pointing.top)) }
        // Paths can paint beneath transparent panels; their points and labels cannot intercept panel taps.
        val right=pointing.left+pointing.width
        val bottom=pointing.top+pointing.height
        if(pointing.left>0) add(SpaceCompassSunSceneFrame(0.0,0.0,pointing.left,height))
        if(right<width) add(SpaceCompassSunSceneFrame(right,0.0,width-right,height))
        if(pointing.top>0) add(SpaceCompassSunSceneFrame(pointing.left,0.0,pointing.width,pointing.top))
        if(bottom<height) add(SpaceCompassSunSceneFrame(pointing.left,bottom,pointing.width,height-bottom))
    }
}

/** Keep frozen exports in the original pointing coordinates when the native drawing canvas is larger. */
internal fun SpaceCompassGlobeFraming.inPointingFrame(pointing: SpaceCompassSunSceneFrame) = copy(
    width=pointing.width,height=pointing.height,
    centerY=(centerY*height-pointing.top)/pointing.height,
    radiusByWidth=radiusByWidth*width/pointing.width)
