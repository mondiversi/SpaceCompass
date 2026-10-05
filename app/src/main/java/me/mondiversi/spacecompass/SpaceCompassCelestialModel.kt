package me.mondiversi.spacecompass

import io.github.cosinekitty.astronomy.*
import java.time.Instant
import java.time.ZoneOffset
import java.time.ZoneId
import kotlin.math.*

internal const val SPACE_COMPASS_AU_KM = 149_597_870.7
internal enum class SpaceCompassCelestialBody(val nameResource: Int, val engine: Body? = null) {
    SUN(R.string.celestial_sun, Body.Sun), MOON(R.string.celestial_moon, Body.Moon),
    MERCURY(R.string.celestial_mercury, Body.Mercury), VENUS(R.string.celestial_venus, Body.Venus),
    MARS(R.string.celestial_mars, Body.Mars), JUPITER(R.string.celestial_jupiter, Body.Jupiter),
    SATURN(R.string.celestial_saturn, Body.Saturn), URANUS(R.string.celestial_uranus, Body.Uranus),
    NEPTUNE(R.string.celestial_neptune, Body.Neptune), PLUTO(R.string.celestial_pluto, Body.Pluto),
    SEDNA(R.string.celestial_sedna), ISS(R.string.celestial_iss),
    VOYAGER_1(R.string.celestial_voyager_1), VOYAGER_2(R.string.celestial_voyager_2),
    POLARIS(R.string.celestial_polaris), IO(R.string.celestial_io), EUROPA(R.string.celestial_europa),
    STARLINK_V3(R.string.celestial_starlink),
    ALPHA_CENTAURI(R.string.celestial_alpha_centauri), SAGITTARIUS_A(R.string.celestial_sagittarius_a),
    ANDROMEDA_CORE(R.string.celestial_andromeda_core), TON_618(R.string.celestial_ton_618),
    STEPHENSON_2_18(R.string.celestial_stephenson_2_18), RX_J1856(R.string.celestial_rx_j1856),
    PSR_J0437(R.string.celestial_psr_j0437),
    PROXIMA_CENTAURI(R.string.celestial_proxima_centauri), RIGEL(R.string.celestial_rigel), EARTH_CENTER(R.string.celestial_earth_center), TRAPPIST_1_E(R.string.celestial_trappist_1_e)
}
internal val SpaceCompassCelestialBody.isEarthSatellite: Boolean get() = this == SpaceCompassCelestialBody.ISS || this == SpaceCompassCelestialBody.STARLINK_V3
internal val SpaceCompassCelestialBody.isJovianMoon: Boolean get() = this == SpaceCompassCelestialBody.IO || this == SpaceCompassCelestialBody.EUROPA
internal val SpaceCompassCelestialBody.hasPhysicalFace: Boolean get() = engine != null || isJovianMoon
// Presentation order is stable: spacecraft stay in the explicitly requested order, not a daily re-sort.
internal val spaceCompassCelestialCatalogOrder = listOf(SpaceCompassCelestialBody.SUN, SpaceCompassCelestialBody.MERCURY,
    SpaceCompassCelestialBody.VENUS, SpaceCompassCelestialBody.EARTH_CENTER, SpaceCompassCelestialBody.ISS, SpaceCompassCelestialBody.STARLINK_V3, SpaceCompassCelestialBody.MOON, SpaceCompassCelestialBody.MARS,
    SpaceCompassCelestialBody.JUPITER, SpaceCompassCelestialBody.IO, SpaceCompassCelestialBody.EUROPA, SpaceCompassCelestialBody.SATURN,
    SpaceCompassCelestialBody.URANUS, SpaceCompassCelestialBody.NEPTUNE, SpaceCompassCelestialBody.PLUTO, SpaceCompassCelestialBody.SEDNA,
    SpaceCompassCelestialBody.VOYAGER_1, SpaceCompassCelestialBody.VOYAGER_2, SpaceCompassCelestialBody.PROXIMA_CENTAURI, SpaceCompassCelestialBody.ALPHA_CENTAURI,
    SpaceCompassCelestialBody.TRAPPIST_1_E, SpaceCompassCelestialBody.RX_J1856, SpaceCompassCelestialBody.POLARIS, SpaceCompassCelestialBody.PSR_J0437,
    SpaceCompassCelestialBody.RIGEL, SpaceCompassCelestialBody.STEPHENSON_2_18, SpaceCompassCelestialBody.SAGITTARIUS_A, SpaceCompassCelestialBody.ANDROMEDA_CORE, SpaceCompassCelestialBody.TON_618)
internal val SpaceCompassCelestialBody.usesHorizons: Boolean
    get() = this == SpaceCompassCelestialBody.SEDNA || isVoyager
internal val SpaceCompassCelestialBody.isVoyager: Boolean
    get() = this == SpaceCompassCelestialBody.VOYAGER_1 || this == SpaceCompassCelestialBody.VOYAGER_2
internal val SpaceCompassCelestialBody.usesLiveDistance: Boolean
    get() = this == SpaceCompassCelestialBody.MOON || isEarthSatellite || isVoyager
internal val SpaceCompassCelestialBody.supportsDailyPath: Boolean
    get() = this != SpaceCompassCelestialBody.EARTH_CENTER
internal data class SpaceCompassCelestialObservation(val position: SpaceCompassSunPosition, val distanceKm: Double)
internal fun spaceCompassCelestialDistanceTime(body: SpaceCompassCelestialBody, timeMs: Long, zone: ZoneId): Long =
    if (body.usesLiveDistance) timeMs
    else Instant.ofEpochMilli(timeMs).atZone(zone).toLocalDate().atTime(12, 0).atZone(zone).toInstant().toEpochMilli()
internal fun spaceCompassAstronomyTime(ms: Long): Time {
    val utc = Instant.ofEpochMilli(ms).atZone(ZoneOffset.UTC)
    return Time(utc.year, utc.monthValue, utc.dayOfMonth, utc.hour, utc.minute, utc.second + utc.nano / 1e9)
}

/** Topocentric, aberration/light-time corrected; geometric horizon, matching the virtual ground. */
internal fun calculateSpaceCompassCelestialObservation(body: SpaceCompassCelestialBody, timeMs: Long,
    latitude: Double, longitude: Double, altitude: Double = 0.0,
    remote: SpaceCompassCelestialRemoteData = SpaceCompassCelestialRemoteData()): SpaceCompassCelestialObservation? {
    require(latitude.isFinite() && latitude in -90.0..90.0 && longitude.isFinite() && longitude in -180.0..180.0)
    val observer = Observer(latitude, longitude, altitude)
    val time = spaceCompassAstronomyTime(timeMs)
    if (body == SpaceCompassCelestialBody.EARTH_CENTER) {
        val site = observer.toVector(time, EquatorEpoch.J2000)
        val horizontal = rotationEqjHor(time, observer).rotate(Vector(-site.x, -site.y, -site.z, time))
        val planar = hypot(horizontal.x, horizontal.y)
        val azimuth = if (planar < 1e-12) 0.0 else ((Math.toDegrees(atan2(-horizontal.y, horizontal.x)) % 360) + 360) % 360
        return SpaceCompassCelestialObservation(SpaceCompassSunPosition(azimuth,
            Math.toDegrees(atan2(horizontal.z, planar))), site.length() * SPACE_COMPASS_AU_KM)
    }
    if (body.deepSkyReference != null) return calculateSpaceCompassDeepSkyObservation(body, timeMs, observer)
    if (body == SpaceCompassCelestialBody.POLARIS) return calculateSpaceCompassPolarisObservation(timeMs, observer)
    if (body.isEarthSatellite)
        return remote.satelliteOrbit(body)?.takeIf { it.usable(timeMs) }?.observe(timeMs, latitude, longitude, altitude)
    if (body.isJovianMoon) {
        val geo = spaceCompassJovianMoonGeoVector(body, time, Aberration.Corrected)
        val site = observer.toVector(time, EquatorEpoch.J2000)
        val topocentric = Vector(geo.x - site.x, geo.y - site.y, geo.z - site.z, time)
        val horizontal = rotationEqjHor(time, observer).rotate(topocentric)
        return SpaceCompassCelestialObservation(SpaceCompassSunPosition(
            ((Math.toDegrees(atan2(-horizontal.y, horizontal.x)) % 360) + 360) % 360,
            Math.toDegrees(atan2(horizontal.z, hypot(horizontal.x, horizontal.y)))), topocentric.length() * SPACE_COMPASS_AU_KM)
    }
    if (body.usesHorizons) {
        val vector = remote.ephemerides[body]?.takeIf { it.body == body }?.at(timeMs) ?: return null
        val site = observer.toVector(time, EquatorEpoch.J2000)
        val topocentric = Vector(vector.x - site.x, vector.y - site.y, vector.z - site.z, time)
        val horizontal = rotationEqjHor(time, observer).rotate(topocentric)
        val distance = sqrt(horizontal.x * horizontal.x + horizontal.y * horizontal.y + horizontal.z * horizontal.z)
        return SpaceCompassCelestialObservation(SpaceCompassSunPosition(
            ((Math.toDegrees(atan2(-horizontal.y, horizontal.x)) % 360) + 360) % 360,
            Math.toDegrees(atan2(horizontal.z, hypot(horizontal.x, horizontal.y)))), distance * SPACE_COMPASS_AU_KM)
    }
    val equatorial = equator(requireNotNull(body.engine), time, observer, EquatorEpoch.OfDate, Aberration.Corrected)
    val horizontal = horizon(time, observer, equatorial.ra, equatorial.dec, Refraction.None)
    // Preserve the already verified solar pointing model. The engine supplies observer-to-Sun distance.
    val position = if (body == SpaceCompassCelestialBody.SUN)
        calculateSpaceCompassSunPosition(timeMs, latitude, longitude, altitude)
        else SpaceCompassSunPosition(horizontal.azimuth, horizontal.altitude)
    return SpaceCompassCelestialObservation(position, equatorial.dist * SPACE_COMPASS_AU_KM)
}
