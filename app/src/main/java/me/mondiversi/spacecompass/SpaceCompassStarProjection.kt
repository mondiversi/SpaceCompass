package me.mondiversi.spacecompass

import io.github.cosinekitty.astronomy.Observer
import io.github.cosinekitty.astronomy.Vector
import io.github.cosinekitty.astronomy.rotationHorEqj
import kotlin.math.*

/** ENU -> ICRF/J2000. The texture's celestial reference stays fixed as Earth turns. */
internal data class SpaceCompassStarBasis(val east: SpaceCompassViewVector,
    val north: SpaceCompassViewVector, val up: SpaceCompassViewVector) {
    fun transform(vector: SpaceCompassSunVector) = east * vector.east + north * vector.north + up * vector.up
}

internal fun spaceCompassStarBasis(timeMs: Long, latitude: Double, longitude: Double, altitude: Double): SpaceCompassStarBasis {
    require(latitude.isFinite() && latitude in -90.0..90.0 && longitude.isFinite() && longitude in -180.0..180.0)
    require(altitude.isFinite())
    val time = spaceCompassAstronomyTime(timeMs)
    val matrix = rotationHorEqj(time, Observer(latitude, longitude, altitude))
    fun axis(north: Double, west: Double, up: Double): SpaceCompassViewVector {
        val vector = matrix.rotate(Vector(north, west, up, time))
        return SpaceCompassViewVector(vector.x, vector.y, vector.z)
    }
    return SpaceCompassStarBasis(axis(0.0, -1.0, 0.0), axis(1.0, 0.0, 0.0), axis(0.0, 0.0, 1.0))
}

/** NASA celestial plate-carree: RA zero at the centre, RA increases left, north at the top. */
internal fun spaceCompassStarUv(direction: SpaceCompassViewVector): Pair<Double, Double> {
    val ray = direction.unit()
    val u = ((0.5 - atan2(ray.y, ray.x) / (2 * PI)) % 1 + 1) % 1
    return u to (0.5 - asin(ray.z.coerceIn(-1.0, 1.0)) / PI)
}

/** Same 60-degree pinhole, principal point and screen basis as the orbit/object projection. */
internal fun spaceCompassStarViewRay(x: Double, y: Double, orientation: SpaceCompassSunOrientation,
    frame: SpaceCompassSunSceneFrame): SpaceCompassSunVector {
    require(frame.width > 0 && frame.height > 0)
    val focal = spaceCompassSunProjectionFocalLength(frame.height)
    val dx = (x - frame.left - frame.width / 2) / focal
    val dy = -(y - frame.top - frame.height / 2) / focal
    return SpaceCompassSunVector(orientation.forward.east + orientation.right.east * dx + orientation.screenUp.east * dy,
        orientation.forward.north + orientation.right.north * dx + orientation.screenUp.north * dy,
        orientation.forward.up + orientation.right.up * dx + orientation.screenUp.up * dy).normalized()
}

/** Physical solar twilight, independent of the selected UI theme or a timezone's wall clock. */
internal fun spaceCompassStarVisibility(solarElevation: Double?, weather: SpaceCompassSunWeatherSnapshot?): Float {
    if (solarElevation == null || !solarElevation.isFinite() || !spaceCompassSunDisplayStars(weather)) return 0f
    val dark = ((-solarElevation - 3) / 15).coerceIn(0.0, 1.0)
    val twilight = dark * dark * (3 - 2 * dark)
    return (twilight * (1 - .85 * spaceCompassSunDisplayCloudCover(weather))).toFloat()
}

internal fun spaceCompassStarHorizonOpacity(up: Double): Double {
    val value = (up / .12).coerceIn(0.0, 1.0)
    return value * value * (3 - 2 * value)
}

internal data class SpaceCompassStarDrawing(val frame: SpaceCompassSunSceneFrame,
    val right: SpaceCompassViewVector, val screenUp: SpaceCompassViewVector, val forward: SpaceCompassViewVector,
    val worldUp: SpaceCompassViewVector, val opacity: Float)

internal fun spaceCompassStarDrawing(basis: SpaceCompassStarBasis, orientation: SpaceCompassSunOrientation,
    frame: SpaceCompassSunSceneFrame, opacity: Float) = SpaceCompassStarDrawing(frame,
    basis.transform(orientation.right), basis.transform(orientation.screenUp), basis.transform(orientation.forward),
    SpaceCompassViewVector(orientation.right.up, orientation.screenUp.up, orientation.forward.up), opacity)
