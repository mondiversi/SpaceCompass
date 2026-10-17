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
    PROXIMA_CENTAURI(R.string.celestial_proxima_centauri), RIGEL(R.string.celestial_rigel), EARTH_CENTER(R.string.celestial_earth_center), TRAPPIST_1_E(R.string.celestial_trappist_1_e),
    HALLEY(R.string.celestial_halley), COMET_67P(R.string.celestial_comet_67p), LV_426(R.string.celestial_lv426),
    SIRIUS(R.string.celestial_sirius), BETELGEUSE(R.string.celestial_betelgeuse), TITAN(R.string.celestial_titan),
    ORION_NEBULA(R.string.celestial_orion_nebula), PLEIADES(R.string.celestial_pleiades),
    TESLA_ROADSTER(R.string.celestial_tesla_roadster),
    NEW_HORIZONS(R.string.celestial_new_horizons), JWST(R.string.celestial_jwst), PARKER_SOLAR_PROBE(R.string.celestial_parker_solar_probe), APOPHIS(R.string.celestial_apophis), ATLAS_3I(R.string.celestial_atlas_3i), EUROPA_CLIPPER(R.string.celestial_europa_clipper), JUICE(R.string.celestial_juice), HUBBLE(R.string.celestial_hubble), TIANGONG(R.string.celestial_tiangong), M87(R.string.celestial_m87), PROXIMA_B(R.string.celestial_proxima_b),
    CERES(R.string.celestial_ceres), ENCELADUS(R.string.celestial_enceladus), VEGA(R.string.celestial_vega),
    DENEB(R.string.celestial_deneb), ALTAIR(R.string.celestial_altair), SOUTHERN_CROSS(R.string.celestial_southern_cross),
    GANYMEDE(R.string.celestial_ganymede),
    CALLISTO(R.string.celestial_callisto),
    PHOBOS(R.string.celestial_phobos),
    DEIMOS(R.string.celestial_deimos),
    RHEA(R.string.celestial_rhea),
    IAPETUS(R.string.celestial_iapetus),
    TITANIA(R.string.celestial_titania),
    MIRANDA(R.string.celestial_miranda),
    TRITON(R.string.celestial_triton),
    CHARON(R.string.celestial_charon),
    ERIS(R.string.celestial_eris),
    HAUMEA(R.string.celestial_haumea),
    MAKEMAKE(R.string.celestial_makemake),
    VESTA(R.string.celestial_vesta),
    BENNU(R.string.celestial_bennu),
    RYUGU(R.string.celestial_ryugu),
    HALE_BOPP(R.string.celestial_hale_bopp),
    ENCKE(R.string.celestial_encke),
    ARCTURUS(R.string.celestial_arcturus),
    CANOPUS(R.string.celestial_canopus),
    CAPELLA(R.string.celestial_capella),
    ANTARES(R.string.celestial_antares),
    ALDEBARAN(R.string.celestial_aldebaran),
    SPICA(R.string.celestial_spica),
    FOMALHAUT(R.string.celestial_fomalhaut),
    ACHERNAR(R.string.celestial_achernar),
    HADAR(R.string.celestial_hadar),
    SIGMA_OCTANTIS(R.string.celestial_sigma_octantis),
    M13(R.string.celestial_m13),
    OMEGA_CENTAURI(R.string.celestial_omega_centauri),
    M57(R.string.celestial_m57),
    M27(R.string.celestial_m27),
    M1(R.string.celestial_m1),
    CARINA_NEBULA(R.string.celestial_carina_nebula),
    HORSEHEAD_NEBULA(R.string.celestial_horsehead_nebula),
    LARGE_MAGELLANIC_CLOUD(R.string.celestial_large_magellanic_cloud),
    SMALL_MAGELLANIC_CLOUD(R.string.celestial_small_magellanic_cloud),
    M51(R.string.celestial_m51),
    BEPICOLOMBO(R.string.celestial_bepicolombo),
    SOLAR_ORBITER(R.string.celestial_solar_orbiter),
    LUCY(R.string.celestial_lucy),
    PSYCHE(R.string.celestial_psyche),
    CYGNUS_X1(R.string.celestial_cygnus_x1),
    V404_CYGNI(R.string.celestial_v404_cygni),
    VELA_PULSAR(R.string.celestial_vela_pulsar),
    TOI_700_D(R.string.celestial_toi_700_d),
    KEPLER_186_F(R.string.celestial_kepler_186_f),
    ANDROMEDA_GALAXY(R.string.celestial_andromeda_galaxy_title),
    M87_GALAXY(R.string.celestial_m87_galaxy),
    MILKY_WAY_CENTER(R.string.celestial_milky_way_center),
    M51_CORE(R.string.celestial_m51_core)
}
internal val SpaceCompassCelestialBody.isComet: Boolean get() = this == SpaceCompassCelestialBody.HALLEY || this == SpaceCompassCelestialBody.COMET_67P || this == SpaceCompassCelestialBody.ATLAS_3I || this == SpaceCompassCelestialBody.HALE_BOPP || this == SpaceCompassCelestialBody.ENCKE
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
    SpaceCompassCelestialBody.TRAPPIST_1_E, SpaceCompassCelestialBody.RX_J1856, SpaceCompassCelestialBody.PLEIADES, SpaceCompassCelestialBody.POLARIS, SpaceCompassCelestialBody.PSR_J0437,
    SpaceCompassCelestialBody.BETELGEUSE, SpaceCompassCelestialBody.RIGEL, SpaceCompassCelestialBody.DENEB, SpaceCompassCelestialBody.SOUTHERN_CROSS, SpaceCompassCelestialBody.ORION_NEBULA, SpaceCompassCelestialBody.STEPHENSON_2_18, SpaceCompassCelestialBody.SAGITTARIUS_A, SpaceCompassCelestialBody.ANDROMEDA_CORE, SpaceCompassCelestialBody.M87, SpaceCompassCelestialBody.TON_618))
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
    if (body == SpaceCompassCelestialBody.SOUTHERN_CROSS) return SpaceCompassCelestialObservation(
        spaceCompassSouthernCrossCentre(calculateSpaceCompassSouthernCross(timeMs, latitude, longitude, altitude)), Double.NaN)
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
