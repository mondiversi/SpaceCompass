package me.mondiversi.spacecompass

import kotlin.math.*

internal data class SpaceCompassCameraPhotoGeometry(val width: Int, val height: Int, val rotation: Int,
    val perspective: SpaceCompassPerspective)

/** Full lens image, with no crop/offset from main-page columns or the reticle viewport. */
internal fun spaceCompassCameraPhotoGeometry(lens: SpaceCompassCameraLens, rawWidth: Int, rawHeight: Int,
    displayRotation: Int): SpaceCompassCameraPhotoGeometry {
    require(rawWidth > 0 && rawHeight > 0 && displayRotation in listOf(0,90,180,270))
    val scale = min(lens.cropWidth / rawWidth, lens.cropHeight / rawHeight)
    require(scale.isFinite() && scale > 0)
    val cx = (lens.centerX - lens.cropLeft - (lens.cropWidth-rawWidth*scale)/2)/scale
    val cy = (lens.centerY - lens.cropTop - (lens.cropHeight-rawHeight*scale)/2)/scale
    val rotation = (lens.sensorRotation - displayRotation + 360) % 360
    val swapped = rotation % 180 != 0
    val width = if (swapped) rawHeight else rawWidth
    val height = if (swapped) rawWidth else rawHeight
    val centre = when (rotation) {
        90 -> (rawHeight - cy) to cx
        180 -> (rawWidth - cx) to (rawHeight - cy)
        270 -> cy to (rawWidth - cx)
        else -> cx to cy
    }
    val fx = (if (swapped) lens.focalY else lens.focalX)/scale
    val fy = (if (swapped) lens.focalX else lens.focalY)/scale
    return SpaceCompassCameraPhotoGeometry(width,height,rotation,
        SpaceCompassPerspective(fx/width,fy/height,centre.first/width,centre.second/height))
}

internal data class SpaceCompassCameraAttitude(val timeNanos: Long, val orientation: SpaceCompassSunOrientation,
    val usable: Boolean)

/** Small shutter-time history in device axes; never stored as user location or camera media. */
internal class SpaceCompassCameraAttitudeHistory {
    private val samples = ArrayDeque<SpaceCompassCameraAttitude>()
    fun add(sample: SpaceCompassCameraAttitude) {
        if (samples.lastOrNull()?.timeNanos?.let { it >= sample.timeNanos } == true) return
        samples.addLast(sample)
        while (samples.size > 64) samples.removeFirst()
    }
    fun at(timeNanos: Long, displayRotation: Int): SpaceCompassCameraAttitude? {
        val sample = samples.minByOrNull { abs(it.timeNanos-timeNanos) } ?: return null
        if (abs(sample.timeNanos-timeNanos) > 250_000_000L) return null
        return sample.copy(orientation=spaceCompassCameraScreenOrientation(sample.orientation,displayRotation))
    }
}

internal fun spaceCompassCameraScreenOrientation(device: SpaceCompassSunOrientation, rotation: Int): SpaceCompassSunOrientation {
    fun negate(v: SpaceCompassSunVector) = SpaceCompassSunVector(-v.east,-v.north,-v.up)
    return when (rotation) {
        // Match SensorManager.remapCoordinateSystem used by the live screen: Y/-X
        // places -device Y on screen X and device X on screen Y (and vice versa for 270).
        90 -> SpaceCompassSunOrientation(negate(device.screenUp),device.right,device.forward)
        180 -> SpaceCompassSunOrientation(negate(device.right),negate(device.screenUp),device.forward)
        270 -> SpaceCompassSunOrientation(device.screenUp,negate(device.right),device.forward)
        else -> device
    }
}

/** Portrait starts filled and pannable; landscape starts fitted, without changing orientation. */
internal fun spaceCompassCaptureBaseScale(viewWidth: Float, viewHeight: Float, imageWidth: Int, imageHeight: Int): Float {
    if (viewWidth<=0 || viewHeight<=0 || imageWidth<=0 || imageHeight<=0) return 1f
    val fit=min(viewWidth/imageWidth,viewHeight/imageHeight)
    return if (viewHeight>viewWidth) max(viewWidth/imageWidth,viewHeight/imageHeight)/fit else 1f
}
