package me.mondiversi.spacecompass

import java.time.LocalDate
import java.time.ZoneId
import kotlin.math.*

internal fun spaceCompassCelestialPathTitle(body: SpaceCompassCelestialBody): Int = when {
    body == SpaceCompassCelestialBody.ISS -> R.string.celestial_iss_path
    body.isEarthSatellite -> R.string.celestial_orbit
    else -> R.string.celestial_daily_path
}

/** Cached apparent civil-day curve; Earth satellites use a complete current orbit; probes use their apparent daily sky track. */
internal fun calculateSpaceCompassCelestialPath(body: SpaceCompassCelestialBody, date: LocalDate, zone: ZoneId, now: Long,
    latitude: Double, longitude: Double, altitude: Double, remote: SpaceCompassCelestialRemoteData): SpaceCompassSunDailyPath? {
    if (!body.supportsDailyPath) return null
    if (body == SpaceCompassCelestialBody.SUN) return calculateSpaceCompassSunDailyPath(date, zone, latitude, longitude, altitude)
    if (body.isEarthSatellite) return remote.satelliteOrbit(body)?.let {
        calculateSpaceCompassSatellitePath(body, date, zone, now, latitude, longitude, altitude, it)
    }
    fun observe(t: Long) = calculateSpaceCompassCelestialObservation(body, t, latitude, longitude, altitude, remote)?.position
    observe(now) ?: return null
    val start = date.atStartOfDay(zone).toInstant().toEpochMilli()
    val end = date.plusDays(1).atStartOfDay(zone).toInstant().toEpochMilli()
    fun point(t: Long, event: SpaceCompassSunPathEvent = SpaceCompassSunPathEvent.HOUR) =
        observe(t)?.let { SpaceCompassSunPathPoint(t, it, event) }
    val samples = buildList {
        var t = start
        while (t < end) { add(point(t) ?: return null); t += 180_000 }
        add(point(end) ?: return null)
    }
    val markers = mutableListOf<SpaceCompassSunPathPoint>()
    var t = start
    while (t < end) { markers += point(t) ?: return null; t += 3_600_000 }
    samples.zipWithNext().forEach { (a,b) ->
        val rising = a.position.elevationDegrees < 0 && b.position.elevationDegrees >= 0
        val setting = a.position.elevationDegrees >= 0 && b.position.elevationDegrees < 0
        if (rising || setting) {
            var lo = a.timeMs; var hi = b.timeMs
            while (hi - lo > 250) {
                val mid = lo + (hi - lo) / 2
                val above = (observe(mid)?.elevationDegrees ?: return null) >= 0
                if (above == rising) hi = mid else lo = mid
            }
            markers += point((lo + hi) / 2, if (rising) SpaceCompassSunPathEvent.SUNRISE else SpaceCompassSunPathEvent.SUNSET) ?: return null
        }
    }
    for (minimum in listOf(false, true)) {
        markers += refineSpaceCompassPathExtremum(samples, end, minimum, ::point) ?: return null
    }
    // Each hourly/event phase belongs to its own time, separate from the live miniature's current phase.
    val labeledMarkers = mergeCoincidentSpaceCompassPathEvents(markers).map { point ->
        if (body == SpaceCompassCelestialBody.MOON) point.copy(moonPhase = calculateSpaceCompassMoonPhase(point.timeMs)) else point
    }
    return SpaceCompassSunDailyPath(date, zone, samples, labeledMarkers, body)
}
