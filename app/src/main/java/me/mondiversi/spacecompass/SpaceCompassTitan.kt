package me.mondiversi.spacecompass

import io.github.cosinekitty.astronomy.*
import kotlin.math.*

/** IAU 2015 coefficients distributed in NASA/NAIF pck00011.tpc (no nutation terms for Titan). */
internal fun spaceCompassTitanAxis(time: Time): AxisInfo {
    val ra = 39.4827; val dec = 83.4279
    val r = Math.toRadians(ra); val c = Math.toRadians(dec)
    return AxisInfo(ra / 15, dec, 186.5855 + 22.5769768 * time.tt,
        Vector(cos(c) * cos(r), cos(c) * sin(r), sin(c), time))
}

/** Subtract Saturn's heliocentric velocity: Titan's orbital speed is relative to its parent. */
internal fun spaceCompassTitanOrbitalSpeed(timeMs: Long, remote: SpaceCompassCelestialRemoteData): Double? {
    val samples = remote.motions[SpaceCompassCelestialBody.TITAN]?.takeIf {
        it.body == SpaceCompassCelestialBody.TITAN
    }?.samples ?: return null
    if (timeMs !in samples.first().timeMs..samples.last().timeMs) return null
    val index = samples.indexOfFirst { it.timeMs >= timeMs }.coerceAtLeast(1)
    val a = samples[index - 1]; val b = samples[index]
    val f = (timeMs - a.timeMs).toDouble() / (b.timeMs - a.timeMs)
    val saturn = helioState(Body.Saturn, spaceCompassAstronomyTime(timeMs))
    return spaceCompassCelestialVelocityMagnitude(a.vx + (b.vx - a.vx) * f - saturn.vx,
        a.vy + (b.vy - a.vy) * f - saturn.vy, a.vz + (b.vz - a.vz) * f - saturn.vz,
        SPACE_COMPASS_AU_DAY_TO_KM_SECOND)
}
