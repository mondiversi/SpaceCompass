package me.mondiversi.spacecompass

import io.github.cosinekitty.astronomy.*
import kotlin.math.*

/** ICRS/J2000 catalogue pointing references; distances are approximate, not live telemetry.
 * Alpha Centauri represents the unresolved AB pair; M31 uses its nuclear position.
 * References, limitations and physical identifications: docs/DEEP_SKY.md. */
internal data class SpaceCompassDeepSkyReference(val raHours: Double, val decDegrees: Double,
    val distanceLy: Double, val pmRaMasYear: Double = 0.0, val pmDecMasYear: Double = 0.0)
internal val SpaceCompassCelestialBody.deepSkyReference: SpaceCompassDeepSkyReference?
    get() = when (this) {
        SpaceCompassCelestialBody.SIRIUS -> SpaceCompassDeepSkyReference(
            6 + 45.0/60 + 8.91728/3600, -(16 + 42.0/60 + 58.0171/3600),
            1000 / 379.21 * 3.261563777, -546.01, -1223.07)
        SpaceCompassCelestialBody.BETELGEUSE -> SpaceCompassDeepSkyReference(
            5 + 55.0/60 + 10.30536/3600, 7 + 24.0/60 + 25.4304/3600,
            168 * 3.261563777, 27.54, 11.30)
        // Extended targets use a catalogue centre, not fabricated component or gas trajectories.
        SpaceCompassCelestialBody.ORION_NEBULA -> SpaceCompassDeepSkyReference(
            5 + 35.0/60 + 16.8/3600, -(5 + 23.0/60 + 15.0/3600), 414 * 3.261563777)
        SpaceCompassCelestialBody.PLEIADES -> SpaceCompassDeepSkyReference(
            3 + 46.0/60 + 24.2/3600, 24 + 6.0/60 + 50.0/3600,
            1000 / 7.364 * 3.261563777, 19.997, -45.548)
        // Fictional placement: use the real host-star direction, never invented moon ephemerides.
        SpaceCompassCelestialBody.LV_426 -> SpaceCompassDeepSkyReference(
            3 + 18.0/60 + 12.8188824117/3600, -(62 + 30.0/60 + 22.904711032/3600),
            39.3, 1331.027, 647.725)
        // Unresolved exoplanet: use the host star direction and proper motion.
        SpaceCompassCelestialBody.TRAPPIST_1_E -> SpaceCompassDeepSkyReference(
            23 + 6.0/60 + 29.3684948589/3600, -(5 + 2.0/60 + 29.037301866/3600),
            1000 / 80.2123 * 3.261563777, 930.788, -479.038)
        SpaceCompassCelestialBody.PROXIMA_CENTAURI -> SpaceCompassDeepSkyReference(
            14 + 29.0/60 + 42.9461331854/3600, -(62 + 40.0/60 + 46.164680672/3600),
            1000 / 768.0665 * 3.261563777, -3781.741, 769.465)
        SpaceCompassCelestialBody.RIGEL -> SpaceCompassDeepSkyReference(
            5 + 14.0/60 + 32.27210/3600, -(8 + 12.0/60 + 5.8981/3600),
            1000 / 3.78 * 3.261563777, 1.31, 0.50)
        SpaceCompassCelestialBody.ALPHA_CENTAURI -> SpaceCompassDeepSkyReference(
            14 + 39.0/60 + 36.50/3600, -(60 + 50.0/60 + 2.3/3600),
            (1000 / 750.81) * 3.261563777, -3608.0, 686.0)
        SpaceCompassCelestialBody.SAGITTARIUS_A -> SpaceCompassDeepSkyReference(
            17 + 45.0/60 + 40.03599/3600, -(29 + 28.1699/3600), 26_400.0)
        SpaceCompassCelestialBody.ANDROMEDA_CORE -> SpaceCompassDeepSkyReference(
            42.0/60 + 44.330/3600, 41 + 16.0/60 + 7.50/3600, 2_500_000.0)
        // Cosmological comoving distance is for display only; pointing uses the catalogue direction.
        SpaceCompassCelestialBody.TON_618 -> SpaceCompassDeepSkyReference(
            12 + 28.0/60 + 24.9659725464/3600, 31 + 28.0/60 + 37.628987592/3600, 17800000000.0)
        SpaceCompassCelestialBody.STEPHENSON_2_18 -> SpaceCompassDeepSkyReference(
            18 + 39.0/60 + 2.3694806904/3600, -(6 + 5.0/60 + 10.556738772/3600),
            5500 * 3.261563777, -1.697, -4.637)
        SpaceCompassCelestialBody.RX_J1856 -> SpaceCompassDeepSkyReference(
            18 + 56.0/60 + 35.11/3600, -(37 + 54.0/60 + 30.5/3600),
            123 * 3.261563777, 325.9, -59.3)
        // Optical catalogue direction traces the unresolved binary; adequate for phone pointing.
        SpaceCompassCelestialBody.PSR_J0437 -> SpaceCompassDeepSkyReference(
            4 + 37.0/60 + 15.7994581776/3600, -(47 + 15.0/60 + 8.544351636/3600),
            156.96 * 3.261563777, 121.646, -70.697)
        else -> null
    }
internal val SpaceCompassCelestialBody.isExtrasolar: Boolean
    get() = this == SpaceCompassCelestialBody.POLARIS || deepSkyReference != null

internal fun calculateSpaceCompassDeepSkyObservation(body: SpaceCompassCelestialBody, timeMs: Long,
    observer: Observer): SpaceCompassCelestialObservation {
    val reference = requireNotNull(body.deepSkyReference)
    val time = spaceCompassAstronomyTime(timeMs)
    val ra = Math.toRadians(reference.raHours * 15)
    val dec = Math.toRadians(reference.decDegrees)
    val direction = SpaceCompassViewVector(cos(dec)*cos(ra), cos(dec)*sin(ra), sin(dec))
    val east = SpaceCompassViewVector(-sin(ra), cos(ra), 0.0)
    val north = SpaceCompassViewVector(-sin(dec)*cos(ra), -sin(dec)*sin(ra), cos(dec))
    val years = (timeMs - 946_728_000_000L) / (365.25 * 86_400_000)
    val masToRadians = PI / (180 * 3_600_000)
    // pmRA is already multiplied by cos(dec): use the tangent basis, not raw RA addition.
    val stellar = (direction + east*(reference.pmRaMasYear*masToRadians*years) + north*(reference.pmDecMasYear*masToRadians*years)).unit() *
        (reference.distanceLy * SPACE_COMPASS_LIGHT_YEAR_KM / SPACE_COMPASS_AU_KM)
    val earth = helioState(Body.Earth, time)
    val site = observer.toVector(time, EquatorEpoch.J2000)
    val ray = stellar - SpaceCompassViewVector(earth.x + site.x, earth.y + site.y, earth.z + site.z)
    val distanceAu = sqrt(ray.dot(ray))
    // First-order annual aberration; residual is far below a phone compass's accuracy.
    val sight = ray.unit()
    val beta = SpaceCompassViewVector(earth.vx, earth.vy, earth.vz) * (1 / 173.144632674240)
    val apparent = (sight + beta - sight*sight.dot(beta)).unit()
    // EQJ -> horizontal includes precession, nutation and sidereal rotation for this UTC instant.
    val horizontal = rotationEqjHor(time, observer).rotate(Vector(apparent.x, apparent.y, apparent.z, time))
    val azimuth = (Math.toDegrees(atan2(-horizontal.y, horizontal.x)) + 360) % 360
    val elevation = Math.toDegrees(atan2(horizontal.z, hypot(horizontal.x, horizontal.y)))
    return SpaceCompassCelestialObservation(SpaceCompassSunPosition(azimuth, elevation), distanceAu * SPACE_COMPASS_AU_KM)
}

internal val SpaceCompassCelestialBody.deepSkyNoteResource: Int
    get() = when (this) {
        SpaceCompassCelestialBody.SIRIUS -> R.string.celestial_sirius_note
        SpaceCompassCelestialBody.BETELGEUSE -> R.string.celestial_betelgeuse_note
        SpaceCompassCelestialBody.ORION_NEBULA -> R.string.celestial_orion_note
        SpaceCompassCelestialBody.PLEIADES -> R.string.celestial_pleiades_note
        SpaceCompassCelestialBody.LV_426 -> R.string.celestial_lv426_description
        SpaceCompassCelestialBody.TRAPPIST_1_E -> R.string.celestial_trappist_note
        SpaceCompassCelestialBody.TON_618 -> R.string.celestial_ton_618_note
        SpaceCompassCelestialBody.PROXIMA_CENTAURI -> R.string.celestial_proxima_note
        SpaceCompassCelestialBody.RIGEL -> R.string.celestial_rigel_note
        SpaceCompassCelestialBody.STEPHENSON_2_18 -> R.string.celestial_stephenson_note
        SpaceCompassCelestialBody.RX_J1856 -> R.string.celestial_rx_note
        SpaceCompassCelestialBody.PSR_J0437 -> R.string.celestial_psr_note
        else -> R.string.celestial_deep_sky_note
    }
