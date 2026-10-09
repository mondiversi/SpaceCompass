package me.mondiversi.spacecompass

import kotlin.math.*

internal data class SpaceCompassSunPosition(val azimuthDegrees: Double, val elevationDegrees: Double)

/** Meeus/NOAA solar coordinates; UTC instant, east-positive longitude, WGS84 observer height.
 * No terrain or weather inference. Elevation is geometric (no atmospheric refraction).
 * https://gml.noaa.gov/grad/solcalc/calcdetails.html
 */
internal fun calculateSpaceCompassSunPosition(
    timeMs: Long, latitude: Double, longitude: Double, altitudeMeters: Double = 0.0
): SpaceCompassSunPosition {
    require(latitude.isFinite() && latitude in -90.0..90.0)
    require(longitude.isFinite() && longitude in -180.0..180.0)
    require(altitudeMeters.isFinite())
    val t = (timeMs / 86_400_000.0 + 2_440_587.5 - 2_451_545.0) / 36_525.0
    val meanLongitude = wrapSpaceCompassSunDegrees(280.46646 + t * (36_000.76983 + 0.0003032 * t))
    val meanAnomaly = Math.toRadians(357.52911 + t * (35_999.05029 - 0.0001537 * t))
    val eccentricity = 0.016708634 - t * (0.000042037 + 0.0000001267 * t)
    val center = sin(meanAnomaly) * (1.914602 - t * (0.004817 + 0.000014 * t)) +
        sin(2 * meanAnomaly) * (0.019993 - 0.000101 * t) + sin(3 * meanAnomaly) * 0.000289
    val omega = Math.toRadians(125.04 - 1934.136 * t)
    val apparentLongitude = Math.toRadians(meanLongitude + center - 0.00569 - 0.00478 * sin(omega))
    val meanObliquity = 23.0 + (26.0 + (21.448 - t * (46.815 + t * (0.00059 - t * 0.001813))) / 60) / 60
    val obliquity = Math.toRadians(meanObliquity + 0.00256 * cos(omega))
    val declination = asin(sin(obliquity) * sin(apparentLongitude))
    val y = tan(obliquity / 2).pow(2)
    val l = Math.toRadians(meanLongitude)
    val equationOfTime = 4 * Math.toDegrees(y * sin(2 * l) - 2 * eccentricity * sin(meanAnomaly) +
        4 * eccentricity * y * sin(meanAnomaly) * cos(2 * l) -
        0.5 * y * y * sin(4 * l) - 1.25 * eccentricity * eccentricity * sin(2 * meanAnomaly))
    val utcMinutes = Math.floorMod(timeMs, 86_400_000L) / 60_000.0
    val solarMinutes = ((utcMinutes + equationOfTime + 4 * longitude) % 1440 + 1440) % 1440
    val hourAngle = Math.toRadians(solarMinutes / 4 - 180)
    val lat = Math.toRadians(latitude)
    val direction = SpaceCompassSunVector(
        east = -cos(declination) * sin(hourAngle),
        north = cos(lat) * sin(declination) - sin(lat) * cos(declination) * cos(hourAngle),
        up = sin(lat) * sin(declination) + cos(lat) * cos(declination) * cos(hourAngle)
    )
    // Topocentric parallax: subtract the actual observer, including height, from the solar vector.
    // Height barely changes the solar direction; it must not be used to invent a mountain horizon.
    val distance = 149_597_870_700.0 *
        (1.000001018 * (1 - eccentricity * eccentricity)) /
        (1 + eccentricity * cos(meanAnomaly + Math.toRadians(center)))
    val earthEccentricitySquared = 0.00669437999014
    val earthRadius = 6_378_137.0 / sqrt(1 - earthEccentricitySquared * sin(lat).pow(2))
    val observerNorth = -earthRadius * earthEccentricitySquared * sin(lat) * cos(lat)
    val observerUp = earthRadius * (1 - earthEccentricitySquared * sin(lat).pow(2)) + altitudeMeters
    val topocentric = SpaceCompassSunVector(direction.east * distance,
        direction.north * distance - observerNorth, direction.up * distance - observerUp).normalized()
    return SpaceCompassSunPosition(wrapSpaceCompassSunDegrees(Math.toDegrees(atan2(topocentric.east, topocentric.north))),
        Math.toDegrees(asin(topocentric.up.coerceIn(-1.0, 1.0))))
}

internal fun wrapSpaceCompassSunDegrees(value: Double): Double = ((value % 360) + 360) % 360

internal data class SpaceCompassSunVector(val east: Double, val north: Double, val up: Double) {
    fun dot(other: SpaceCompassSunVector) = east * other.east + north * other.north + up * other.up
    fun normalized(): SpaceCompassSunVector {
        val length = sqrt(dot(this)).coerceAtLeast(1e-12)
        return SpaceCompassSunVector(east / length, north / length, up / length)
    }
    fun trueNorth(declinationDegrees: Double): SpaceCompassSunVector {
        val d = Math.toRadians(declinationDegrees)
        return SpaceCompassSunVector(east * cos(d) + north * sin(d), north * cos(d) - east * sin(d), up)
    }
}

/** View axes in east/north/up. Sensor poses use the rear camera; virtual views may use the top edge. */
internal data class SpaceCompassSunOrientation(
    val right: SpaceCompassSunVector, val screenUp: SpaceCompassSunVector, val forward: SpaceCompassSunVector
) {
    val headingDegrees get() = wrapSpaceCompassSunDegrees(Math.toDegrees(atan2(forward.east, forward.north)))
    val tiltDegrees get() = Math.toDegrees(asin(forward.up.coerceIn(-1.0, 1.0)))
}

internal data class SpaceCompassSunProjection(
    val visible: Boolean, val x: Double, val y: Double, val separationDegrees: Double
)

/** Sun and horizon must use exactly the same perspective. */
internal fun spaceCompassSunProjectionFocalLength(height: Double): Double = height / (2 * tan(Math.toRadians(30.0)))

/** Virtual 60-degree vertical perspective by default; camera mode supplies the actual lens/crop. */
internal fun projectSpaceCompassSun(
    sun: SpaceCompassSunPosition, orientation: SpaceCompassSunOrientation, width: Double, height: Double,
    perspective: SpaceCompassPerspective? = null
): SpaceCompassSunProjection {
    require(width > 0 && height > 0)
    val az = Math.toRadians(sun.azimuthDegrees)
    val el = Math.toRadians(sun.elevationDegrees)
    val vector = SpaceCompassSunVector(cos(el) * sin(az), cos(el) * cos(az), sin(el))
    val right = vector.dot(orientation.right)
    val up = vector.dot(orientation.screenUp)
    val depth = vector.dot(orientation.forward)
    val focal = perspective?.focalY(height) ?: spaceCompassSunProjectionFocalLength(height)
    val focalX = perspective?.focalX(width) ?: focal
    val centerX = (perspective?.principalX ?: .5) * width
    val centerY = (perspective?.principalY ?: .5) * height
    val dx = right * focalX / depth.coerceAtLeast(1e-9)
    val dy = -up * focal / depth.coerceAtLeast(1e-9)
    val visible = depth > 0 && centerX + dx in 0.0..width && centerY + dy in 0.0..height
    if (visible) return SpaceCompassSunProjection(true, centerX + dx, centerY + dy,
        Math.toDegrees(acos(depth.coerceIn(-1.0, 1.0))))
    // Off-screen bearing never divides by a negative depth (which would invert the arrow).
    var ex = right * focalX
    var ey = -up * focal
    if (abs(ex) + abs(ey) < 1e-8) { ex = 1.0; ey = 0.0 } // Directly behind: choose a stable turn.
    val radiusX = max(1.0, width / 2 - 28)
    val radiusY = max(1.0, height / 2 - 28)
    val scale = min(radiusX / abs(ex).coerceAtLeast(1e-12), radiusY / abs(ey).coerceAtLeast(1e-12))
    return SpaceCompassSunProjection(false, width / 2 + ex * scale, height / 2 + ey * scale,
        Math.toDegrees(acos(depth.coerceIn(-1.0, 1.0))))
}
