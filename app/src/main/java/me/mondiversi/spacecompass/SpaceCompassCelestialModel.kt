package me.mondiversi.spacecompass

import io.github.cosinekitty.astronomy.*
import java.time.Instant
import java.time.ZoneOffset
import java.time.ZoneId
import kotlin.math.*

internal const val SPACE_COMPASS_AU_KM = 149_597_870.7

internal val SpaceCompassCelestialBody.isComet: Boolean get() = largeCatalogEntry?.entry?.type == SpaceCompassCatalogType.COMET || this == SpaceCompassCelestialBody.HALLEY || this == SpaceCompassCelestialBody.COMET_67P || this == SpaceCompassCelestialBody.ATLAS_3I || this == SpaceCompassCelestialBody.HALE_BOPP || this == SpaceCompassCelestialBody.ENCKE
internal val SpaceCompassCelestialBody.isEarthSatellite: Boolean get() = this == SpaceCompassCelestialBody.ISS || this == SpaceCompassCelestialBody.STARLINK_V3 || catalogSatellite != null
internal val SpaceCompassCelestialBody.isJovianMoon: Boolean get() = this == SpaceCompassCelestialBody.IO || this == SpaceCompassCelestialBody.EUROPA
internal val SpaceCompassCelestialBody.hasPhysicalFace: Boolean get() = engine != null || isJovianMoon || this in
    setOf(SpaceCompassCelestialBody.TITAN, SpaceCompassCelestialBody.ENCELADUS)
// EARTH_CENTER is a retired legacy identifier, excluded from selectable catalogs and restored checks.
// Its old calculation remains for backward compatibility and reference-geometry regression checks.
// Presentation order is stable: spacecraft stay in the explicitly requested order, not a daily re-sort.
internal val spaceCompassCelestialCatalogOrder = spaceCompassIncludeAdditionalCatalog(listOf(SpaceCompassCelestialBody.SUN, SpaceCompassCelestialBody.MERCURY,
    SpaceCompassCelestialBody.VENUS, SpaceCompassCelestialBody.ISS, SpaceCompassCelestialBody.STARLINK_V3, SpaceCompassCelestialBody.TIANGONG, SpaceCompassCelestialBody.HUBBLE, SpaceCompassCelestialBody.MOON, SpaceCompassCelestialBody.JWST, SpaceCompassCelestialBody.TESLA_ROADSTER, SpaceCompassCelestialBody.APOPHIS, SpaceCompassCelestialBody.PARKER_SOLAR_PROBE, SpaceCompassCelestialBody.EUROPA_CLIPPER, SpaceCompassCelestialBody.JUICE, SpaceCompassCelestialBody.MARS, SpaceCompassCelestialBody.CERES,
    SpaceCompassCelestialBody.JUPITER, SpaceCompassCelestialBody.IO, SpaceCompassCelestialBody.EUROPA, SpaceCompassCelestialBody.SATURN, SpaceCompassCelestialBody.TITAN, SpaceCompassCelestialBody.ENCELADUS,
    SpaceCompassCelestialBody.URANUS, SpaceCompassCelestialBody.NEPTUNE, SpaceCompassCelestialBody.PLUTO, SpaceCompassCelestialBody.SEDNA, SpaceCompassCelestialBody.HALLEY, SpaceCompassCelestialBody.COMET_67P,
    SpaceCompassCelestialBody.ATLAS_3I, SpaceCompassCelestialBody.NEW_HORIZONS, SpaceCompassCelestialBody.VOYAGER_1, SpaceCompassCelestialBody.VOYAGER_2, SpaceCompassCelestialBody.PROXIMA_CENTAURI, SpaceCompassCelestialBody.PROXIMA_B, SpaceCompassCelestialBody.ALPHA_CENTAURI, SpaceCompassCelestialBody.SIRIUS, SpaceCompassCelestialBody.ALTAIR, SpaceCompassCelestialBody.VEGA,
    SpaceCompassCelestialBody.LV_426, SpaceCompassCelestialBody.TRAPPIST_1_E, SpaceCompassCelestialBody.RX_J1856, SpaceCompassCelestialBody.PLEIADES, SpaceCompassCelestialBody.POLARIS, SpaceCompassCelestialBody.PSR_J0437,
    SpaceCompassCelestialBody.BETELGEUSE, SpaceCompassCelestialBody.RIGEL, SpaceCompassCelestialBody.DENEB, SpaceCompassCelestialBody.SOUTHERN_CROSS, SpaceCompassCelestialBody.ORION_NEBULA, SpaceCompassCelestialBody.STEPHENSON_2_18, SpaceCompassCelestialBody.SAGITTARIUS_A, SpaceCompassCelestialBody.ANDROMEDA_CORE, SpaceCompassCelestialBody.M87, SpaceCompassCelestialBody.TON_618)) + spaceCompassNewFictionalBodies
internal val SpaceCompassCelestialBody.usesHorizons: Boolean
    get() = expandedHorizonsTarget != null || this == SpaceCompassCelestialBody.SEDNA || this == SpaceCompassCelestialBody.TITAN || isVoyager || isComet || this == SpaceCompassCelestialBody.TESLA_ROADSTER
internal val SpaceCompassCelestialBody.isVoyager: Boolean
    get() = this == SpaceCompassCelestialBody.VOYAGER_1 || this == SpaceCompassCelestialBody.VOYAGER_2
internal val SpaceCompassCelestialBody.usesLiveDistance: Boolean
    get() = expandedHorizonsTarget != null || this == SpaceCompassCelestialBody.MOON || this == SpaceCompassCelestialBody.TITAN || isEarthSatellite || isVoyager || isComet || this == SpaceCompassCelestialBody.TESLA_ROADSTER
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
    body.fictionalReference?.let { return calculateSpaceCompassFictionalObservation(it, timeMs, latitude, longitude, altitude, remote) }
    if (body.constellationPattern != null) return SpaceCompassCelestialObservation(
        spaceCompassConstellationCentre(calculateSpaceCompassConstellation(body, timeMs, latitude, longitude, altitude)), Double.NaN)
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
