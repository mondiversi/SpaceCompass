package me.mondiversi.spacecompass

import kotlin.math.*

/** Shared topocentric geometry and complete-revolution contract for Earth satellites. */
internal interface SpaceCompassSatelliteOrbit {
    val epochMs: Long
    val periodMs: Long
    fun usable(timeMs: Long): Boolean
    fun teme(timeMs: Long): DoubleArray
    fun speedKmPerSecond(timeMs: Long): Double

    fun observe(timeMs: Long, latitude: Double, longitude: Double, altitude: Double): SpaceCompassCelestialObservation {
        require(latitude.isFinite() && latitude in -90.0..90.0 &&
            longitude.isFinite() && longitude in -180.0..180.0 && altitude.isFinite())
        val r = teme(timeMs)
        val jd = timeMs / 86_400_000.0 + 2440587.5
        val t = (jd - 2451545.0) / 36525
        val gmst = Math.toRadians(((280.46061837 + 360.98564736629 * (jd - 2451545) +
            0.000387933 * t * t - t * t * t / 38710000) % 360 + 360) % 360)
        val x = cos(gmst) * r[0] + sin(gmst) * r[1]
        val y = -sin(gmst) * r[0] + cos(gmst) * r[1]
        val lat = Math.toRadians(latitude); val lon = Math.toRadians(longitude)
        val n = 6378.137 / sqrt(1 - 0.00669437999014 * sin(lat).pow(2))
        val h = altitude / 1000
        val dx = x - (n + h) * cos(lat) * cos(lon)
        val dy = y - (n + h) * cos(lat) * sin(lon)
        val dz = r[2] - (n * (1 - 0.00669437999014) + h) * sin(lat)
        val east = -sin(lon) * dx + cos(lon) * dy
        val north = -sin(lat) * cos(lon) * dx - sin(lat) * sin(lon) * dy + cos(lat) * dz
        val up = cos(lat) * cos(lon) * dx + cos(lat) * sin(lon) * dy + sin(lat) * dz
        return SpaceCompassCelestialObservation(SpaceCompassSunPosition(
            (Math.toDegrees(atan2(east, north)) + 360) % 360,
            Math.toDegrees(atan2(up, hypot(east, north)))), sqrt(east * east + north * north + up * up))
    }
}

internal fun SpaceCompassCelestialRemoteData.satelliteOrbit(body: SpaceCompassCelestialBody): SpaceCompassSatelliteOrbit? = when (body) {
    SpaceCompassCelestialBody.ISS -> iss
    SpaceCompassCelestialBody.STARLINK_V3 -> starlink
    else -> null
}
