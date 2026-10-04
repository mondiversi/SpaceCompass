package me.mondiversi.planetcompass

import io.github.cosinekitty.astronomy.*
import java.time.Instant
import java.time.ZoneOffset
import java.time.ZoneId
import kotlin.math.*

internal const val PLANET_COMPASS_AU_KM = 149_597_870.7
internal enum class PlanetCompassCelestialBody(val nameResource: Int, val engine: Body? = null) {
    SUN(R.string.celestial_sun, Body.Sun), MOON(R.string.celestial_moon, Body.Moon),
    MERCURY(R.string.celestial_mercury, Body.Mercury), VENUS(R.string.celestial_venus, Body.Venus),
    MARS(R.string.celestial_mars, Body.Mars), JUPITER(R.string.celestial_jupiter, Body.Jupiter),
    SATURN(R.string.celestial_saturn, Body.Saturn), URANUS(R.string.celestial_uranus, Body.Uranus),
    NEPTUNE(R.string.celestial_neptune, Body.Neptune), PLUTO(R.string.celestial_pluto, Body.Pluto),
    SEDNA(R.string.celestial_sedna), ISS(R.string.celestial_iss),
    VOYAGER_1(R.string.celestial_voyager_1), VOYAGER_2(R.string.celestial_voyager_2),
    POLARIS(R.string.celestial_polaris), IO(R.string.celestial_io), EUROPA(R.string.celestial_europa),
    STARLINK_V3(R.string.celestial_starlink)
}
internal val PlanetCompassCelestialBody.isEarthSatellite: Boolean get() = this == PlanetCompassCelestialBody.ISS || this == PlanetCompassCelestialBody.STARLINK_V3
internal val PlanetCompassCelestialBody.isJovianMoon: Boolean get() = this == PlanetCompassCelestialBody.IO || this == PlanetCompassCelestialBody.EUROPA
internal val PlanetCompassCelestialBody.hasPhysicalFace: Boolean get() = engine != null || isJovianMoon
// Presentation order is stable: spacecraft stay in the explicitly requested order, not a daily re-sort.
internal val planetCompassCelestialCatalogOrder = listOf(PlanetCompassCelestialBody.SUN, PlanetCompassCelestialBody.MERCURY,
    PlanetCompassCelestialBody.VENUS, PlanetCompassCelestialBody.ISS, PlanetCompassCelestialBody.STARLINK_V3, PlanetCompassCelestialBody.MOON, PlanetCompassCelestialBody.MARS,
    PlanetCompassCelestialBody.JUPITER, PlanetCompassCelestialBody.IO, PlanetCompassCelestialBody.EUROPA, PlanetCompassCelestialBody.SATURN,
    PlanetCompassCelestialBody.URANUS, PlanetCompassCelestialBody.NEPTUNE, PlanetCompassCelestialBody.PLUTO, PlanetCompassCelestialBody.SEDNA,
    PlanetCompassCelestialBody.VOYAGER_1, PlanetCompassCelestialBody.VOYAGER_2, PlanetCompassCelestialBody.POLARIS)
internal val PlanetCompassCelestialBody.usesHorizons: Boolean
    get() = this == PlanetCompassCelestialBody.SEDNA || isVoyager
internal val PlanetCompassCelestialBody.isVoyager: Boolean
    get() = this == PlanetCompassCelestialBody.VOYAGER_1 || this == PlanetCompassCelestialBody.VOYAGER_2
internal val PlanetCompassCelestialBody.usesLiveDistance: Boolean
    get() = this == PlanetCompassCelestialBody.MOON || isEarthSatellite || isVoyager
internal val PlanetCompassCelestialBody.supportsDailyPath: Boolean
    get() = !isVoyager
internal data class PlanetCompassCelestialObservation(val position: PlanetCompassSunPosition, val distanceKm: Double)
internal fun planetCompassCelestialDistanceTime(body: PlanetCompassCelestialBody, timeMs: Long, zone: ZoneId): Long =
    if (body.usesLiveDistance) timeMs
    else Instant.ofEpochMilli(timeMs).atZone(zone).toLocalDate().atTime(12, 0).atZone(zone).toInstant().toEpochMilli()
internal fun planetCompassAstronomyTime(ms: Long): Time {
    val utc = Instant.ofEpochMilli(ms).atZone(ZoneOffset.UTC)
    return Time(utc.year, utc.monthValue, utc.dayOfMonth, utc.hour, utc.minute, utc.second + utc.nano / 1e9)
}

/** Topocentric, aberration/light-time corrected; geometric horizon, matching the virtual ground. */
internal fun calculatePlanetCompassCelestialObservation(body: PlanetCompassCelestialBody, timeMs: Long,
    latitude: Double, longitude: Double, altitude: Double = 0.0,
    remote: PlanetCompassCelestialRemoteData = PlanetCompassCelestialRemoteData()): PlanetCompassCelestialObservation? {
    require(latitude.isFinite() && latitude in -90.0..90.0 && longitude.isFinite() && longitude in -180.0..180.0)
    val observer = Observer(latitude, longitude, altitude)
    val time = planetCompassAstronomyTime(timeMs)
    if (body == PlanetCompassCelestialBody.POLARIS) return calculatePlanetCompassPolarisObservation(timeMs, observer)
    if (body.isEarthSatellite)
        return remote.satelliteOrbit(body)?.takeIf { it.usable(timeMs) }?.observe(timeMs, latitude, longitude, altitude)
    if (body.isJovianMoon) {
        val geo = planetCompassJovianMoonGeoVector(body, time, Aberration.Corrected)
        val site = observer.toVector(time, EquatorEpoch.J2000)
        val topocentric = Vector(geo.x - site.x, geo.y - site.y, geo.z - site.z, time)
        val horizontal = rotationEqjHor(time, observer).rotate(topocentric)
        return PlanetCompassCelestialObservation(PlanetCompassSunPosition(
            ((Math.toDegrees(atan2(-horizontal.y, horizontal.x)) % 360) + 360) % 360,
            Math.toDegrees(atan2(horizontal.z, hypot(horizontal.x, horizontal.y)))), topocentric.length() * PLANET_COMPASS_AU_KM)
    }
    if (body.usesHorizons) {
        val vector = remote.ephemerides[body]?.takeIf { it.body == body }?.at(timeMs) ?: return null
        val site = observer.toVector(time, EquatorEpoch.J2000)
        val topocentric = Vector(vector.x - site.x, vector.y - site.y, vector.z - site.z, time)
        val horizontal = rotationEqjHor(time, observer).rotate(topocentric)
        val distance = sqrt(horizontal.x * horizontal.x + horizontal.y * horizontal.y + horizontal.z * horizontal.z)
        return PlanetCompassCelestialObservation(PlanetCompassSunPosition(
            ((Math.toDegrees(atan2(-horizontal.y, horizontal.x)) % 360) + 360) % 360,
            Math.toDegrees(atan2(horizontal.z, hypot(horizontal.x, horizontal.y)))), distance * PLANET_COMPASS_AU_KM)
    }
    val equatorial = equator(requireNotNull(body.engine), time, observer, EquatorEpoch.OfDate, Aberration.Corrected)
    val horizontal = horizon(time, observer, equatorial.ra, equatorial.dec, Refraction.None)
    // Preserve the already verified solar pointing model. The engine supplies observer-to-Sun distance.
    val position = if (body == PlanetCompassCelestialBody.SUN)
        calculatePlanetCompassSunPosition(timeMs, latitude, longitude, altitude)
        else PlanetCompassSunPosition(horizontal.azimuth, horizontal.altitude)
    return PlanetCompassCelestialObservation(position, equatorial.dist * PLANET_COMPASS_AU_KM)
}
