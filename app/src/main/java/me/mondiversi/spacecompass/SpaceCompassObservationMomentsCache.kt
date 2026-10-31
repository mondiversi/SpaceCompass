package me.mondiversi.spacecompass

import java.time.ZoneId
import kotlin.math.*

internal data class SpaceCompassObservationCalendar(val body: SpaceCompassCelestialBody, val year: Int, val zone: ZoneId)
internal data class SpaceCompassObservationPlace(val latitude: Double, val longitude: Double, val altitude: Double,
    val fixedPosition: Boolean = false, val fixedAltitude: Boolean = false) {
    val valid: Boolean get() = latitude.isFinite() && latitude in -90.0..90.0 &&
        longitude.isFinite() && longitude in -180.0..180.0 && altitude.isFinite()

    fun equivalentTo(other: SpaceCompassObservationPlace): Boolean {
        if (fixedPosition != other.fixedPosition || fixedAltitude != other.fixedAltitude) return false
        if (fixedAltitude && altitude != other.altitude || !fixedAltitude && abs(altitude-other.altitude) > 50) return false
        if (fixedPosition) return latitude == other.latitude && longitude == other.longitude
        val lat = Math.toRadians(latitude); val otherLat = Math.toRadians(other.latitude)
        val halfLat = (otherLat-lat)/2; val halfLon = Math.toRadians(other.longitude-longitude)/2
        val a = (sin(halfLat).pow(2) + cos(lat)*cos(otherLat)*sin(halfLon).pow(2)).coerceIn(0.0, 1.0)
        return 2 * 6_371_000 * atan2(sqrt(a), sqrt(1-a)) <= 100
    }
}
internal data class SpaceCompassObservationRequest(val calendar: SpaceCompassObservationCalendar,
    val place: SpaceCompassObservationPlace, val orbit: SpaceCompassSatelliteOrbit?,
    val ephemeris: SpaceCompassHorizonsEphemeris?)
internal data class SpaceCompassObservationResult(val request: SpaceCompassObservationRequest,
    val moments: List<SpaceCompassObservationMoment>?)

/** Main-thread session cache; no preferences, exports, live-sky inputs or partial jobs are stored.
 * GPS-only hysteresis is relative to an accepted anchor, not rounding-cell boundaries. A real
 * movement beyond 100 m horizontally / 50 m vertically advances it; manual overrides stay exact.
 */
internal class SpaceCompassObservationMomentsCache(private val limit: Int = 16) {
    init { require(limit > 0) }
    private val anchors = LinkedHashMap<SpaceCompassObservationCalendar, SpaceCompassObservationPlace>(16, .75f, true)
    private val results = LinkedHashMap<SpaceCompassObservationRequest, SpaceCompassObservationResult>(16, .75f, true)

    fun request(calendar: SpaceCompassObservationCalendar, place: SpaceCompassObservationPlace,
        orbit: SpaceCompassSatelliteOrbit? = null, ephemeris: SpaceCompassHorizonsEphemeris? = null): SpaceCompassObservationRequest? {
        if (!place.valid) return null
        val previous = anchors[calendar]
        val anchor = previous?.takeIf { it.equivalentTo(place) } ?: place
        anchors[calendar] = anchor
        while (anchors.size > limit) anchors.remove(anchors.keys.first())
        return SpaceCompassObservationRequest(calendar, anchor, orbit, ephemeris)
    }

    fun get(request: SpaceCompassObservationRequest?): SpaceCompassObservationResult? = request?.let { results[it] }
    fun put(result: SpaceCompassObservationResult) {
        results[result.request] = result
        while (results.size > limit) results.remove(results.keys.first())
    }
}
