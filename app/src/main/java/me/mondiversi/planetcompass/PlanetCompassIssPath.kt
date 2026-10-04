package me.mondiversi.planetcompass

import java.time.LocalDate
import java.time.ZoneId

/** One TLE-derived revolution around now, not merely the next above-horizon pass.
 * Earth rotates during the revolution, so its apparent endpoints must NOT be joined artificially.
 * A missing future pass must not remove a valid current orbit (e.g. at polar latitudes).
 */
internal fun calculatePlanetCompassIssPath(date: LocalDate, zone: ZoneId, now: Long,
    latitude: Double, longitude: Double, altitude: Double, orbit: PlanetCompassIssOrbit): PlanetCompassSunDailyPath? =
    calculatePlanetCompassSatellitePath(PlanetCompassCelestialBody.ISS, date, zone, now, latitude, longitude, altitude, orbit)

internal fun calculatePlanetCompassSatellitePath(body: PlanetCompassCelestialBody, date: LocalDate, zone: ZoneId, now: Long,
    latitude: Double, longitude: Double, altitude: Double, orbit: PlanetCompassSatelliteOrbit): PlanetCompassSunDailyPath? {
    require(body.isEarthSatellite)
    require(latitude.isFinite() && latitude in -90.0..90.0 && longitude.isFinite() && longitude in -180.0..180.0)
    if (!orbit.usable(now)) return null
    fun point(t: Long, event: PlanetCompassSunPathEvent = PlanetCompassSunPathEvent.HOUR): PlanetCompassSunPathPoint? =
        if (orbit.usable(t)) PlanetCompassSunPathPoint(t, orbit.observe(t, latitude, longitude, altitude).position, event) else null

    // Anchor to a minute for stable point selection. Shift, rather than truncate, at TLE validity edges.
    val anchor = Math.floorDiv(now, 60_000L) * 60_000L
    val start = (anchor - orbit.periodMs / 2).coerceIn(
        orbit.epochMs - PLANET_COMPASS_ISS_FUTURE_EPOCH_ALLOWANCE_MS,
        orbit.epochMs + PLANET_COMPASS_ISS_MAX_AGE_MS - orbit.periodMs)
    val end = start + orbit.periodMs
    val samples = buildList {
        var t = start
        while (t < end) { add(point(t) ?: return null); t += 10_000L }
        add(point(end) ?: return null)
        if (anchor in start..end && none { it.timeMs == anchor }) add(point(anchor) ?: return null)
    }.sortedBy { it.timeMs }
    val markers = mutableListOf<PlanetCompassSunPathPoint>()
    // 24 equally spaced locator points over ONE revolution, not 24 hourly positions.
    // The dense ten-second samples still draw a smooth curve, including below the horizon.
    repeat(24) { index -> markers += point(start + orbit.periodMs * index / 24) ?: return null }
    samples.zipWithNext().forEach { (a, b) ->
        val rising = a.position.elevationDegrees < 0 && b.position.elevationDegrees >= 0
        val setting = a.position.elevationDegrees >= 0 && b.position.elevationDegrees < 0
        if (rising || setting) markers += refinePlanetCompassIssHorizon(a.timeMs, b.timeMs, rising, ::point) ?: return null
    }
    // Do not label an interval endpoint as a culmination if the actual maximum is outside this revolution.
    val peaks = (1 until samples.lastIndex).filter { i ->
        samples[i].position.elevationDegrees >= samples[i - 1].position.elevationDegrees &&
            samples[i].position.elevationDegrees > samples[i + 1].position.elevationDegrees
    }
    for (i in peaks) markers += refinePlanetCompassIssPeak(samples[i - 1].timeMs, samples[i + 1].timeMs, ::point) ?: return null
    markers += refinePlanetCompassPathExtremum(samples, end + 1, true, ::point) ?: return null
    return PlanetCompassSunDailyPath(date, zone, samples, mergeCoincidentPlanetCompassPathEvents(markers), body,
        findPlanetCompassIssPass(now, ::point).orEmpty())
}

/** Current or next geometric pass within 24 h, independent of the current-orbit drawing. */
private fun findPlanetCompassIssPass(now: Long,
    point: (Long, PlanetCompassSunPathEvent) -> PlanetCompassSunPathPoint?): List<PlanetCompassSunPathPoint>? {
    fun above(t: Long) = point(t, PlanetCompassSunPathEvent.HOUR)?.position?.elevationDegrees?.let { it >= 0 }
    var before = now
    var after = now
    if (above(now) == true) {
        val limit = now - 30 * 60_000L
        while (above(before) == true && before > limit) before -= 10_000L
        if (above(before) != false) return null
        after = before + 10_000L
    } else {
        val limit = now + 24 * 3_600_000L
        while (after < limit) {
            val visible = above(after) ?: return null
            if (visible) break
            before = after
            after = (after + 30_000L).coerceAtMost(limit)
        }
        if (above(after) != true) return null
    }
    val rise = refinePlanetCompassIssHorizon(before, after, true, point) ?: return null
    var setBefore = maxOf(now, after)
    var setAfter = setBefore
    while (above(setAfter) == true && setAfter < rise.timeMs + 30 * 60_000L) {
        setBefore = setAfter; setAfter += 10_000L
    }
    if (above(setAfter) != false) return null
    val set = refinePlanetCompassIssHorizon(setBefore, setAfter, false, point) ?: return null
    val peak = refinePlanetCompassIssPeak(rise.timeMs, set.timeMs, point) ?: return null
    return listOf(rise, peak, set)
}

private fun refinePlanetCompassIssHorizon(start: Long, end: Long, rising: Boolean,
    point: (Long, PlanetCompassSunPathEvent) -> PlanetCompassSunPathPoint?): PlanetCompassSunPathPoint? {
    var lo = start; var hi = end
    while (hi - lo > 250L) {
        val mid = lo + (hi - lo) / 2
        val above = (point(mid, PlanetCompassSunPathEvent.HOUR)?.position?.elevationDegrees ?: return null) >= 0
        if (above == rising) hi = mid else lo = mid
    }
    return point(lo + (hi - lo) / 2, if (rising) PlanetCompassSunPathEvent.SUNRISE else PlanetCompassSunPathEvent.SUNSET)
}

private fun refinePlanetCompassIssPeak(start: Long, end: Long,
    point: (Long, PlanetCompassSunPathEvent) -> PlanetCompassSunPathPoint?): PlanetCompassSunPathPoint? {
    var lo = start; var hi = end
    while (hi - lo > 250L) {
        val third = (hi - lo) / 3; val a = lo + third; val b = hi - third
        if ((point(a, PlanetCompassSunPathEvent.HOUR)?.position?.elevationDegrees ?: return null) <
            (point(b, PlanetCompassSunPathEvent.HOUR)?.position?.elevationDegrees ?: return null)) lo = a else hi = b
    }
    return point(lo + (hi - lo) / 2, PlanetCompassSunPathEvent.CULMINATION)
}
