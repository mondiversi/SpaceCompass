package me.mondiversi.spacecompass

import java.time.Instant
import java.time.ZoneId

internal const val SPACE_COMPASS_FORWARD_PATH_KEY = "sky_forward_24_hours"
internal const val SPACE_COMPASS_FORWARD_PATH_DURATION_MS = 86_400_000L

/** A stable 25-hour buffer is rebuilt hourly, not on sensor frames or every clock tick. */
internal fun spaceCompassForwardPathAnchor(timeMs: Long): Long = Math.floorDiv(timeMs, 3_600_000L) * 3_600_000L

internal fun calculateSpaceCompassForwardPathBuffer(body: SpaceCompassCelestialBody, timeMs: Long, zone: ZoneId,
    latitude: Double, longitude: Double, altitude: Double, remote: SpaceCompassCelestialRemoteData,
    checkCancelled: () -> Unit = {}): SpaceCompassSunDailyPath? {
    if (!body.supportsDailyPath) return null
    val start = spaceCompassForwardPathAnchor(timeMs)
    val end = start + SPACE_COMPASS_FORWARD_PATH_DURATION_MS + 3_600_000L
    fun point(t: Long, event: SpaceCompassSunPathEvent = SpaceCompassSunPathEvent.HOUR): SpaceCompassSunPathPoint? {
        checkCancelled()
        val position = calculateSpaceCompassCelestialObservation(body,t,latitude,longitude,altitude,remote)?.position ?: return null
        return SpaceCompassSunPathPoint(t,position,event)
    }
    val step = if (body.isEarthSatellite) 10_000L else 180_000L
    val samples = buildList {
        var t = start
        while (t < end) { val sample=point(t) ?: break; add(sample); t += step }
        if (t >= end) point(end)?.let { add(it) }
    }
    if (samples.size < 2) return null
    val markers = mutableListOf<SpaceCompassSunPathPoint>()
    var hour = Instant.ofEpochMilli(start).atZone(zone).withMinute(0).withSecond(0).withNano(0).toInstant().toEpochMilli()
    while (hour < start) hour += 3_600_000L
    while (hour <= samples.last().timeMs) { markers += point(hour) ?: return null; hour += 3_600_000L }
    val horizon = if (body == SpaceCompassCelestialBody.SUN) -0.833 else 0.0
    samples.zipWithNext().forEach { (a,b) ->
        val rising = a.position.elevationDegrees < horizon && b.position.elevationDegrees >= horizon
        val setting = a.position.elevationDegrees >= horizon && b.position.elevationDegrees < horizon
        if (rising || setting) {
            var lo = a.timeMs; var hi = b.timeMs
            while (hi-lo > 250L) {
                val mid = lo+(hi-lo)/2
                val above = (point(mid)?.position?.elevationDegrees ?: return null) >= horizon
                if (above == rising) hi=mid else lo=mid
            }
            markers += point(lo+(hi-lo)/2,if (rising) SpaceCompassSunPathEvent.SUNRISE else SpaceCompassSunPathEvent.SUNSET) ?: return null
        }
    }
    // Keep every real extremum in the buffer: trimming must not lose tomorrow's second occurrence.
    for (i in 1 until samples.lastIndex) {
        val previous=samples[i-1].position.elevationDegrees
        val current=samples[i].position.elevationDegrees
        val next=samples[i+1].position.elevationDegrees
        val minimum=current < previous && current <= next || current <= previous && current < next
        val maximum=current > previous && current >= next || current >= previous && current > next
        if (minimum || maximum) markers += refineSpaceCompassPathExtremum(samples.subList(i-1,i+2),
            samples[i+1].timeMs+1,minimum,::point) ?: return null
    }
    val labeled = mergeCoincidentSpaceCompassPathEvents(markers).map {
        if (body == SpaceCompassCelestialBody.MOON) it.copy(moonPhase=calculateSpaceCompassMoonPhase(it.timeMs)) else it
    }
    return SpaceCompassSunDailyPath(Instant.ofEpochMilli(timeMs).atZone(zone).toLocalDate(),zone,
        samples,labeled,body,forward24Hours=true)
}

/** Exact endpoints and one absolute 24-hour interval, including across DST and the date line. */
internal fun trimSpaceCompassForwardPath(buffer: SpaceCompassSunDailyPath, timeMs: Long,
    observe: (Long) -> SpaceCompassSunPosition?): SpaceCompassSunDailyPath? {
    val end=timeMs+SPACE_COMPASS_FORWARD_PATH_DURATION_MS
    if (!buffer.forward24Hours || buffer.samples.isEmpty() ||
        timeMs < buffer.samples.first().timeMs || end > buffer.samples.last().timeMs) return null
    val startPoint=observe(timeMs)?.let { SpaceCompassSunPathPoint(timeMs,it) } ?: return null
    val endPoint=observe(end)?.let { SpaceCompassSunPathPoint(end,it) } ?: return null
    val result=buffer.copy(date=Instant.ofEpochMilli(timeMs).atZone(buffer.zone).toLocalDate(),
        samples=listOf(startPoint)+buffer.samples.filter { it.timeMs > timeMs && it.timeMs < end }+endPoint,
        markers=buffer.markers.filter { it.timeMs > timeMs && it.timeMs <= end },issPass=emptyList())
    return if(result.body.isEarthSatellite) result.copy(issPass=spaceCompassForwardSatellitePass(result)) else result
}

internal fun spaceCompassPathSelectionDate(path: SpaceCompassSunDailyPath, point: SpaceCompassSunPathPoint) =
    if (path.forward24Hours) Instant.ofEpochMilli(point.timeMs).atZone(path.zone).toLocalDate() else path.date

internal fun spaceCompassPathAcceptsSelection(path: SpaceCompassSunDailyPath, saved: SpaceCompassSelectedOrbitPoint): Boolean =
    path.body == saved.key.body && path.comparison?.id == saved.key.comparisonId && path.zone.id == saved.key.zoneId && if (path.forward24Hours)
        path.samples.isNotEmpty() && saved.point.timeMs in path.samples.first().timeMs..path.samples.last().timeMs
    else path.date == saved.key.date
