package me.mondiversi.spacecompass

import io.github.cosinekitty.astronomy.Aberration
import io.github.cosinekitty.astronomy.geoVector
import kotlin.math.atan2
import kotlin.math.hypot

/** Coordinate axes and origin are retained; TEME must never be labelled J2000/ICRS. */
internal enum class SpaceCompassCelestialCoordinateFrame(val reference: String) {
    CATALOG_ICRS("ICRS · J2000.0"), GEOCENTRIC_EQJ("J2000 · EQJ"),
    GEOCENTRIC_ICRF("ICRF"), GEOCENTRIC_TEME("TEME")
}

internal data class SpaceCompassCelestialCoordinates(
    val rightAscensionHours: Double,
    val declinationDegrees: Double,
    val frame: SpaceCompassCelestialCoordinateFrame,
    val timeMs: Long? = null
)

internal enum class SpaceCompassCelestialGalaxy { MILKY_WAY, ANDROMEDA, TON_618_HOST, M87 }

internal data class SpaceCompassCelestialMembership(
    val systemResource: Int? = null,
    val systemDesignation: String? = null,
    val galaxy: SpaceCompassCelestialGalaxy = SpaceCompassCelestialGalaxy.MILKY_WAY
)

/** Names describe confirmed systems, not a conjectural parent for isolated objects. */
internal fun spaceCompassCelestialMembership(body: SpaceCompassCelestialBody): SpaceCompassCelestialMembership = when (body) {
    SpaceCompassCelestialBody.M87 -> SpaceCompassCelestialMembership(galaxy = SpaceCompassCelestialGalaxy.M87)
    SpaceCompassCelestialBody.PROXIMA_B -> SpaceCompassCelestialMembership(systemDesignation = "Proxima Centauri · α Centauri")
    SpaceCompassCelestialBody.ANDROMEDA_CORE -> SpaceCompassCelestialMembership(galaxy = SpaceCompassCelestialGalaxy.ANDROMEDA)
    SpaceCompassCelestialBody.TON_618 -> SpaceCompassCelestialMembership(galaxy = SpaceCompassCelestialGalaxy.TON_618_HOST)
    SpaceCompassCelestialBody.ALPHA_CENTAURI, SpaceCompassCelestialBody.PROXIMA_CENTAURI ->
        SpaceCompassCelestialMembership(systemDesignation = "α Centauri")
    SpaceCompassCelestialBody.POLARIS, SpaceCompassCelestialBody.SIRIUS ->
        SpaceCompassCelestialMembership(systemResource = body.nameResource)
    SpaceCompassCelestialBody.TRAPPIST_1_E -> SpaceCompassCelestialMembership(systemDesignation = "TRAPPIST-1")
    SpaceCompassCelestialBody.PSR_J0437 -> SpaceCompassCelestialMembership(systemDesignation = "PSR J0437−4715")
    SpaceCompassCelestialBody.RIGEL -> SpaceCompassCelestialMembership(systemDesignation = "β Orionis")
    SpaceCompassCelestialBody.LV_426 -> SpaceCompassCelestialMembership(systemDesignation = "ζ² Reticuli")
    else -> if (!body.isExtrasolar) SpaceCompassCelestialMembership(systemResource = R.string.celestial_spatial_solar_system)
        else SpaceCompassCelestialMembership()
}

internal fun spaceCompassCelestialCoordinatesFromVector(x: Double, y: Double, z: Double,
    frame: SpaceCompassCelestialCoordinateFrame, timeMs: Long): SpaceCompassCelestialCoordinates? {
    if (!x.isFinite() || !y.isFinite() || !z.isFinite()) return null
    val horizontal = hypot(x, y)
    if (!horizontal.isFinite() || !hypot(horizontal, z).isFinite() || hypot(horizontal, z) == 0.0) return null
    val ra = (Math.toDegrees(atan2(y, x)) / 15.0 + 24.0) % 24.0
    return SpaceCompassCelestialCoordinates(ra, Math.toDegrees(atan2(z, horizontal)), frame, timeMs)
}

/** Catalogue coordinates are fixed at their reference epoch. Moving nearby bodies use
 * existing geocentric ephemerides, never the user's GPS or a stale remote extrapolation. */
internal fun calculateSpaceCompassCelestialCoordinates(body: SpaceCompassCelestialBody, timeMs: Long,
    remote: SpaceCompassCelestialRemoteData): SpaceCompassCelestialCoordinates? {
    if (body == SpaceCompassCelestialBody.SOUTHERN_CROSS) return spaceCompassSouthernCrossCoordinates
    body.deepSkyReference?.let { return SpaceCompassCelestialCoordinates(it.raHours, it.decDegrees,
        SpaceCompassCelestialCoordinateFrame.CATALOG_ICRS) }
    if (body == SpaceCompassCelestialBody.POLARIS) return SpaceCompassCelestialCoordinates(
        SPACE_COMPASS_POLARIS_RA_HOURS, SPACE_COMPASS_POLARIS_DEC_DEGREES,
        SpaceCompassCelestialCoordinateFrame.CATALOG_ICRS)
    if (body == SpaceCompassCelestialBody.EARTH_CENTER) return null // No direction from the geocentre to itself.
    if (body.isEarthSatellite) {
        val orbit = remote.satelliteOrbit(body)?.takeIf { it.usable(timeMs) } ?: return null
        val vector = orbit.teme(timeMs)
        return spaceCompassCelestialCoordinatesFromVector(vector[0], vector[1], vector[2],
            SpaceCompassCelestialCoordinateFrame.GEOCENTRIC_TEME, timeMs)
    }
    val time = spaceCompassAstronomyTime(timeMs)
    val frame: SpaceCompassCelestialCoordinateFrame
    val vector = when {
        body.engine != null -> {
            frame = SpaceCompassCelestialCoordinateFrame.GEOCENTRIC_EQJ
            geoVector(body.engine, time, Aberration.Corrected)
        }
        body.isJovianMoon -> {
            frame = SpaceCompassCelestialCoordinateFrame.GEOCENTRIC_EQJ
            spaceCompassJovianMoonGeoVector(body, time, Aberration.Corrected)
        }
        body.usesHorizons -> {
            frame = SpaceCompassCelestialCoordinateFrame.GEOCENTRIC_ICRF
            remote.ephemerides[body]?.takeIf { it.body == body }?.at(timeMs) ?: return null
        }
        else -> return null
    }
    return spaceCompassCelestialCoordinatesFromVector(vector.x, vector.y, vector.z, frame, timeMs)
}
