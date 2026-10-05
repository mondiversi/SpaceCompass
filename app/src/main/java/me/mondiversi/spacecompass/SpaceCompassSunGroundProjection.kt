package me.mondiversi.spacecompass

import kotlin.math.*

internal data class SpaceCompassSunScenePoint(val x: Double, val y: Double)
internal data class SpaceCompassSunSceneFrame(val left: Double, val top: Double, val width: Double, val height: Double)
internal data class SpaceCompassSunGroundProjection(
    val ground: List<SpaceCompassSunScenePoint>, val horizon: List<SpaceCompassSunScenePoint>,
    val groundDirection: SpaceCompassSunScenePoint,
    // The actual horizon can lie outside the screen. Never invent one at screen centre.
    val horizonOrigin: SpaceCompassSunScenePoint?, val depthScale: Double
)

/**
 * Clip the full scene against the gravity horizon, with the pointing viewport's perspective.
 * A ray is forward + right * dx/focal - screenUp * dy/focal. Its world-up component
 * identifies sky (> 0) or ground (<= 0), including roll and views straight up/down.
 * This is a virtual horizontal plane, not reconstructed terrain or GPS altitude.
 */
internal fun projectSpaceCompassSunGround(
    orientation: SpaceCompassSunOrientation, frame: SpaceCompassSunSceneFrame,
    sceneWidth: Double, sceneHeight: Double
): SpaceCompassSunGroundProjection? {
    val numbers = listOf(frame.left, frame.top, frame.width, frame.height, sceneWidth, sceneHeight,
        orientation.right.up, orientation.screenUp.up, orientation.forward.up)
    if (numbers.any { !it.isFinite() } || frame.width <= 0 || frame.height <= 0 ||
        sceneWidth <= 0 || sceneHeight <= 0) return null
    val xUp = orientation.right.up
    val yUp = -orientation.screenUp.up
    val zUp = orientation.forward.up
    if (xUp * xUp + yUp * yUp + zUp * zUp < 1e-12) return null
    val centerX = frame.left + frame.width / 2
    val centerY = frame.top + frame.height / 2
    val focal = spaceCompassSunProjectionFocalLength(frame.height)
    fun up(point: SpaceCompassSunScenePoint) = zUp +
        xUp * (point.x - centerX) / focal + yUp * (point.y - centerY) / focal
    val corners = listOf(SpaceCompassSunScenePoint(0.0, 0.0), SpaceCompassSunScenePoint(sceneWidth, 0.0),
        SpaceCompassSunScenePoint(sceneWidth, sceneHeight), SpaceCompassSunScenePoint(0.0, sceneHeight))
    val ground = mutableListOf<SpaceCompassSunScenePoint>()
    val horizon = mutableListOf<SpaceCompassSunScenePoint>()
    fun addHorizon(point: SpaceCompassSunScenePoint) {
        if (horizon.none { abs(it.x - point.x) + abs(it.y - point.y) < 1e-6 }) horizon += point
    }
    corners.forEachIndexed { index, current ->
        val previous = corners[(index + corners.size - 1) % corners.size]
        val previousUp = up(previous)
        val currentUp = up(current)
        val previousInside = previousUp <= 0
        val currentInside = currentUp <= 0
        if (previousInside != currentInside) {
            val t = (previousUp / (previousUp - currentUp)).coerceIn(0.0, 1.0)
            val crossing = SpaceCompassSunScenePoint(previous.x + (current.x - previous.x) * t,
                previous.y + (current.y - previous.y) * t)
            ground += crossing
            addHorizon(crossing)
        }
        if (currentInside) ground += current
        if (abs(currentUp) < 1e-9) addHorizon(current)
    }
    val length = hypot(xUp, yUp)
    val direction = if (length > 1e-9) SpaceCompassSunScenePoint(-xUp / length, -yUp / length)
        else SpaceCompassSunScenePoint(0.0, 1.0)
    val origin = if (length > 1e-4) SpaceCompassSunScenePoint(
        centerX - xUp * zUp * focal / (length * length),
        centerY - yUp * zUp * focal / (length * length)) else null
    return SpaceCompassSunGroundProjection(ground, horizon.take(2), direction, origin,
        if (origin != null) focal / length else focal)
}
