package me.mondiversi.spacecompass

import kotlin.math.max
import kotlin.math.min

/** Focal lengths relative to the pointing viewport, independent of Compose pixel density. */
internal data class SpaceCompassPerspective(val horizontal: Double, val vertical: Double, val principalX: Double = .5, val principalY: Double = .5) {
    init { require(horizontal.isFinite() && vertical.isFinite() && horizontal > 0 && vertical > 0 && principalX.isFinite() && principalY.isFinite()) }
    fun focalX(width: Double) = horizontal * width
    fun focalY(height: Double) = vertical * height
}

internal data class SpaceCompassCameraLens(
    val cropLeft: Double, val cropTop: Double, val cropWidth: Double, val cropHeight: Double,
    val focalX: Double, val focalY: Double, val centerX: Double, val centerY: Double,
    val sensorRotation: Int
)

/** TextureView already applies sensor orientation, but stretches that image into its bounds.
 * The source/destination pairs undo that stretch, compensate display rotation and centre the
 * optical axis on the reticle. Rear previews are never mirrored. No camera pixels are read.
 */
internal data class SpaceCompassCameraGeometry(
    val source: List<SpaceCompassSunScenePoint>, val destination: List<SpaceCompassSunScenePoint>,
    val perspective: SpaceCompassPerspective
)

internal fun spaceCompassCameraGeometry(
    lens: SpaceCompassCameraLens, streamWidth: Double, streamHeight: Double,
    displayRotation: Int, sceneWidth: Double, sceneHeight: Double, frame: SpaceCompassSunSceneFrame
): SpaceCompassCameraGeometry? {
    val positive = listOf(lens.cropWidth, lens.cropHeight, lens.focalX, lens.focalY,
        streamWidth, streamHeight, sceneWidth, sceneHeight, frame.width, frame.height)
    if (positive.any { !it.isFinite() || it <= 0 } ||
        listOf(lens.cropLeft, lens.cropTop, lens.centerX, lens.centerY, frame.left, frame.top).any { !it.isFinite() } ||
        lens.sensorRotation !in listOf(0, 90, 180, 270) || displayRotation !in listOf(0, 90, 180, 270)) return null
    // Camera2 crops the requested active rectangle to the stream's aspect ratio.
    val sensorScale = min(lens.cropWidth / streamWidth, lens.cropHeight / streamHeight)
    val left = lens.cropLeft + (lens.cropWidth - streamWidth * sensorScale) / 2
    val top = lens.cropTop + (lens.cropHeight - streamHeight * sensorScale) / 2
    val cx = (lens.centerX - left) / sensorScale
    val cy = (lens.centerY - top) / sensorScale
    fun rotate(x: Double, y: Double, rotation: Int) = when (rotation) {
        90 -> SpaceCompassSunScenePoint(streamHeight - y, x)
        180 -> SpaceCompassSunScenePoint(streamWidth - x, streamHeight - y)
        270 -> SpaceCompassSunScenePoint(y, streamWidth - x)
        else -> SpaceCompassSunScenePoint(x, y)
    }
    val relative = (lens.sensorRotation - displayRotation + 360) % 360
    val swapped = relative % 180 != 0
    val rw = if (swapped) streamHeight else streamWidth
    val rh = if (swapped) streamWidth else streamHeight
    val optical = rotate(cx, cy, relative)
    val aimX = frame.left + frame.width / 2
    val aimY = frame.top + frame.height / 2
    if (optical.x !in 0.0001..(rw - 0.0001) || optical.y !in 0.0001..(rh - 0.0001) ||
        aimX !in 0.0..sceneWidth || aimY !in 0.0..sceneHeight) return null
    val scale = max(max(aimX / optical.x, (sceneWidth - aimX) / (rw - optical.x)),
        max(aimY / optical.y, (sceneHeight - aimY) / (rh - optical.y)))
    val naturalW = if (lens.sensorRotation % 180 == 0) streamWidth else streamHeight
    val naturalH = if (lens.sensorRotation % 180 == 0) streamHeight else streamWidth
    val corners = listOf(0.0 to 0.0, streamWidth to 0.0, streamWidth to streamHeight, 0.0 to streamHeight)
    val source = corners.map { (x, y) -> rotate(x, y, lens.sensorRotation).let {
        SpaceCompassSunScenePoint(it.x * sceneWidth / naturalW, it.y * sceneHeight / naturalH) } }
    val destination = corners.map { (x, y) -> rotate(x, y, relative).let {
        SpaceCompassSunScenePoint(aimX + (it.x - optical.x) * scale, aimY + (it.y - optical.y) * scale) } }
    val fx = (if (swapped) lens.focalY else lens.focalX) / sensorScale * scale
    val fy = (if (swapped) lens.focalX else lens.focalY) / sensorScale * scale
    return SpaceCompassCameraGeometry(source, destination, SpaceCompassPerspective(fx / frame.width, fy / frame.height))
}
