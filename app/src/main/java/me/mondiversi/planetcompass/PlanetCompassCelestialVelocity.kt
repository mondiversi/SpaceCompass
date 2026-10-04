package me.mondiversi.planetcompass

import io.github.cosinekitty.astronomy.Body
import io.github.cosinekitty.astronomy.geoMoonState
import io.github.cosinekitty.astronomy.helioState
import kotlin.math.sqrt

internal const val PLANET_COMPASS_AU_DAY_TO_KM_SECOND = PLANET_COMPASS_AU_KM / 86_400.0

/** Orbital speed about the primary; Voyager uses outward radial speed from the Sun. */
internal fun calculatePlanetCompassCelestialSpeed(body: PlanetCompassCelestialBody, timeMs: Long,
    remote: PlanetCompassCelestialRemoteData = PlanetCompassCelestialRemoteData()): Double? {
    if (body == PlanetCompassCelestialBody.POLARIS) return null // No fictitious solar orbital speed for a star.
    if (body.isEarthSatellite)
        return remote.satelliteOrbit(body)?.takeIf { it.usable(timeMs) }?.speedKmPerSecond(timeMs)
    if (body.usesHorizons)
        return remote.motions[body]?.takeIf { it.body == body }?.speedAt(timeMs)
    val time = planetCompassAstronomyTime(timeMs)
    if (body.isJovianMoon) {
        val moon = planetCompassJovianMoonState(body, time)
        return planetCompassCelestialVelocityMagnitude(moon.vx, moon.vy, moon.vz, PLANET_COMPASS_AU_DAY_TO_KM_SECOND)
    }
    val state = when (body) {
        PlanetCompassCelestialBody.SUN -> helioState(Body.Earth, time)
        PlanetCompassCelestialBody.MOON -> geoMoonState(time)
        else -> helioState(requireNotNull(body.engine), time)
    }
    return planetCompassCelestialVelocityMagnitude(state.vx, state.vy, state.vz, PLANET_COMPASS_AU_DAY_TO_KM_SECOND)
}

internal fun planetCompassCelestialVelocityMagnitude(x: Double, y: Double, z: Double, scale: Double = 1.0): Double {
    require(x.isFinite() && y.isFinite() && z.isFinite() && scale.isFinite() && scale > 0)
    return (sqrt(x * x + y * y + z * z) * scale).also { require(it.isFinite() && it in 0.0..1_000.0) }
}

/** Sun-centered geometric position (AU) and velocity (AU/day), independent of apparent pointing. */
internal data class PlanetCompassHorizonsStateSample(val timeMs: Long, val x: Double, val y: Double, val z: Double,
    val vx: Double, val vy: Double, val vz: Double)
internal data class PlanetCompassHorizonsMotion(val body: PlanetCompassCelestialBody, val samples: List<PlanetCompassHorizonsStateSample>) {
    init {
        require(body.usesHorizons && samples.size >= 2)
        require(samples.zipWithNext().all { (a, b) -> a.timeMs < b.timeMs })
        samples.forEach {
            val distance = sqrt(it.x * it.x + it.y * it.y + it.z * it.z)
            require(distance.isFinite() && distance > 0)
            planetCompassCelestialVelocityMagnitude(it.vx, it.vy, it.vz, PLANET_COMPASS_AU_DAY_TO_KM_SECOND)
        }
    }

    fun speedAt(timeMs: Long): Double? {
        if (timeMs !in samples.first().timeMs..samples.last().timeMs) return null
        val b = samples.indexOfFirst { it.timeMs >= timeMs }.coerceAtLeast(1)
        val a = samples[b - 1]; val next = samples[b]
        val fraction = (timeMs - a.timeMs).toDouble() / (next.timeMs - a.timeMs)
        val vx = a.vx + (next.vx - a.vx) * fraction
        val vy = a.vy + (next.vy - a.vy) * fraction
        val vz = a.vz + (next.vz - a.vz) * fraction
        if (!body.isVoyager) return planetCompassCelestialVelocityMagnitude(vx, vy, vz, PLANET_COMPASS_AU_DAY_TO_KM_SECOND)
        val x = a.x + (next.x - a.x) * fraction
        val y = a.y + (next.y - a.y) * fraction
        val z = a.z + (next.z - a.z) * fraction
        // dr/dt, not |v| or an Earth-relative range-rate. Keep the sign rather than taking abs().
        return ((x * vx + y * vy + z * vz) / sqrt(x * x + y * y + z * z) * PLANET_COMPASS_AU_DAY_TO_KM_SECOND)
            .takeIf { it.isFinite() && it in -1_000.0..1_000.0 }
    }
}

internal fun planetCompassCelestialSpeedLabel(body: PlanetCompassCelestialBody): Int = when {
    body == PlanetCompassCelestialBody.SUN -> R.string.celestial_speed_earth_orbit
    body == PlanetCompassCelestialBody.MOON || body.isEarthSatellite -> R.string.celestial_speed_orbit_earth
    body.isJovianMoon -> R.string.celestial_speed_orbit_jupiter
    body.isVoyager -> R.string.celestial_speed_outward_sun
    body == PlanetCompassCelestialBody.POLARIS -> R.string.celestial_speed_unavailable
    else -> R.string.celestial_speed_orbit_sun
}
