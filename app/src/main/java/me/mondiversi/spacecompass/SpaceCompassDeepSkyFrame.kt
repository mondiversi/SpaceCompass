package me.mondiversi.spacecompass

import io.github.cosinekitty.astronomy.*
import kotlin.math.*

/** One Earth state, observer parallax and horizon rotation per observation batch.
 * Sharing the frame changes neither the astrometric model nor any rounding. */
internal class SpaceCompassDeepSkyFrame(private val timeMs: Long, observer: Observer) {
    private val time = spaceCompassAstronomyTime(timeMs)
    private val earth = helioState(Body.Earth, time)
    private val site = observer.toVector(time, EquatorEpoch.J2000)
    private val origin = SpaceCompassViewVector(earth.x + site.x, earth.y + site.y, earth.z + site.z)
    private val beta = SpaceCompassViewVector(earth.vx, earth.vy, earth.vz) * (1 / 173.144632674240)
    private val rotation = rotationEqjHor(time, observer)

    fun observe(reference: SpaceCompassDeepSkyReference): SpaceCompassCelestialObservation {
        val stellar = spaceCompassStellarReferenceVector(reference, timeMs)
        val ray = if (reference.hasMeasuredDistance) stellar - origin else stellar
        val distanceAu = sqrt(ray.dot(ray))
        val sight = ray.unit()
        val apparent = (sight + beta - sight*sight.dot(beta)).unit()
        val horizontal = rotation.rotate(Vector(apparent.x, apparent.y, apparent.z, time))
        val azimuth = (Math.toDegrees(atan2(-horizontal.y, horizontal.x)) + 360) % 360
        val elevation = Math.toDegrees(atan2(horizontal.z, hypot(horizontal.x, horizontal.y)))
        return SpaceCompassCelestialObservation(SpaceCompassSunPosition(azimuth, elevation),
            if (reference.hasMeasuredDistance) distanceAu * SPACE_COMPASS_AU_KM else Double.NaN)
    }
}
