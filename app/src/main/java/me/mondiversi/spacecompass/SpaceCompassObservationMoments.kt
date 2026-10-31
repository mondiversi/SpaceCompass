package me.mondiversi.spacecompass

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.sin

internal enum class SpaceCompassObservationMomentKind { MAXIMUM, MINIMUM, MID_RISING, MID_SETTING }
internal data class SpaceCompassObservationMoment(val kind: SpaceCompassObservationMomentKind,
    val point: SpaceCompassSunPathPoint)

/** Local turning points of the upper-culmination curve, with at least 0.01 degrees of
 * prominence on both sides. Sub-arcminute ripples must not create extra seasonal events.
 * Plateaus select one actual middle transit. Calendar edges are not joined: an object's
 * next January need not repeat its previous January (especially for the Moon/planets).
 */
private fun spaceCompassAnnualTurningPoints(points: List<SpaceCompassSunPathPoint>, maximum: Boolean):
    List<SpaceCompassSunPathPoint> {
    val sign = if (maximum) 1 else -1
    fun height(index: Int) = sign * points[index].position.elevationDegrees
    val result = mutableListOf<SpaceCompassSunPathPoint>()
    var start = 0
    while (start < points.size) {
        val peak = height(start)
        var end = start
        while (end + 1 < points.size && abs(height(end + 1) - peak) < 1e-9) end++
        if (start > 0 && end < points.lastIndex && peak > height(start - 1) && peak > height(end + 1)) {
            var leftFloor = peak
            var index = start - 1
            while (index >= 0 && height(index) <= peak) {
                leftFloor = minOf(leftFloor, height(index)); index--
            }
            var rightFloor = peak
            index = end + 1
            while (index < points.size && height(index) <= peak) {
                rightFloor = minOf(rightFloor, height(index)); index++
            }
            if (peak - maxOf(leftFloor, rightFloor) >= .01) result += points[(start + end) / 2]
        }
        start = end + 1
    }
    // Always represent the bounded year's absolute extreme, including a monotonic year's
    // endpoint or an almost constant curve, without duplicating an already represented plateau.
    val absolute = if (maximum) points.maxBy { it.position.elevationDegrees }
        else points.minBy { it.position.elevationDegrees }
    if (result.none { abs(it.position.elevationDegrees - absolute.position.elevationDegrees) < 1e-9 })
        result += absolute
    return result.sortedBy { it.timeMs }
}

/** Compare every significant maximum/minimum of the year's upper culminations, never the
 * nightly lower transit. Mid-height uses the absolute annual range and retains every crossing.
 * Select the nearest actual culmination: interpolating between noons would not be a culmination.
 */
internal fun spaceCompassAnnualObservationMoments(culminations: List<SpaceCompassSunPathPoint>):
    List<SpaceCompassObservationMoment>? {
    if (culminations.isEmpty() || culminations.any { !it.position.elevationDegrees.isFinite() } ||
        culminations.zipWithNext().any { (a, b) -> a.timeMs >= b.timeMs }) return null
    val maximum = culminations.maxBy { it.position.elevationDegrees }
    val minimum = culminations.minBy { it.position.elevationDegrees }
    val result = (spaceCompassAnnualTurningPoints(culminations, maximum = true).map {
        SpaceCompassObservationMoment(SpaceCompassObservationMomentKind.MAXIMUM, it)
    } + spaceCompassAnnualTurningPoints(culminations, maximum = false).map {
        SpaceCompassObservationMoment(SpaceCompassObservationMomentKind.MINIMUM, it)
    }).toMutableList()
    if (maximum.position.elevationDegrees - minimum.position.elevationDegrees < 1e-6) return result
    val target = (maximum.position.elevationDegrees + minimum.position.elevationDegrees) / 2
    val midpointTimes = mutableSetOf<Long>()
    for ((a, b) in culminations.zipWithNext()) {
        val from = a.position.elevationDegrees - target
        val to = b.position.elevationDegrees - target
        val rising = from < 0 && to >= 0
        val setting = from > 0 && to <= 0
        if (!rising && !setting) continue
        val selected = if (abs(from) <= abs(to)) a else b
        if (!midpointTimes.add(selected.timeMs)) continue
        result += SpaceCompassObservationMoment(if (rising) SpaceCompassObservationMomentKind.MID_RISING
            else SpaceCompassObservationMomentKind.MID_SETTING, selected)
    }
    return result
}

/** Hour angle from the rendered topocentric ENU direction, for all supported object types.
 * The equatorial meridian projection x = up*cos(latitude) - north*sin(latitude) is positive
 * at upper transit; -east supplies sin(hour angle). No azimuth discontinuity at the zenith.
 * Reference: Astronomy Engine searchHourAngle (0 hours = upper culmination).
 * https://github.com/cosinekitty/astronomy/blob/master/source/kotlin/doc/search-hour-angle.md
 */
internal fun spaceCompassObservationHourAngle(position: SpaceCompassSunPosition, latitude: Double): Double? {
    if (!position.elevationDegrees.isFinite() || !position.azimuthDegrees.isFinite()) return null
    val elevation = Math.toRadians(position.elevationDegrees)
    val azimuth = Math.toRadians(position.azimuthDegrees)
    val lat = Math.toRadians(latitude)
    val east = cos(elevation) * sin(azimuth)
    val north = cos(elevation) * cos(azimuth)
    val meridian = sin(elevation) * cos(lat) - north * sin(lat)
    if (hypot(east, meridian) < 1e-12) return null // Hour angle is undefined at an exact celestial pole.
    return atan2(-east, meridian)
}

/** Enumerate every upper transit inside the observer-zone calendar year, including leap/DST days.
 * Hourly brackets resolve ordinary sky tracks; satellites require a different long-term model.
 * Missing samples invalidate the annual result rather than yielding extrema from a partial year.
 */
internal fun calculateSpaceCompassAnnualCulminations(year: Int, zone: ZoneId, latitude: Double,
    observe: (Long) -> SpaceCompassSunPosition?, checkCancelled: () -> Unit = {}): List<SpaceCompassSunPathPoint>? {
    require(year in 1900..2100 && latitude.isFinite() && latitude in -90.0..90.0)
    val start = LocalDate.of(year, 1, 1).atStartOfDay(zone).toInstant().toEpochMilli()
    val end = LocalDate.of(year + 1, 1, 1).atStartOfDay(zone).toInstant().toEpochMilli()
    val step = 3_600_000L
    // Require coverage of both year edges before doing the full search.
    observe(end + step) ?: return null
    var before = start - step
    var beforeAngle = spaceCompassObservationHourAngle(observe(before) ?: return null, latitude) ?: return null
    val result = mutableListOf<SpaceCompassSunPathPoint>()
    var after = start
    while (after <= end + step) {
        checkCancelled()
        val afterAngle = spaceCompassObservationHourAngle(observe(after) ?: return null, latitude) ?: return null
        if (beforeAngle < 0 && afterAngle >= 0 && afterAngle - beforeAngle < Math.PI) {
            var low = before
            var high = after
            while (high - low > 250) {
                checkCancelled()
                val mid = low + (high - low) / 2
                val angle = spaceCompassObservationHourAngle(observe(mid) ?: return null, latitude) ?: return null
                if (angle >= 0) high = mid else low = mid
            }
            val time = low + (high - low) / 2
            if (time in start until end) result += SpaceCompassSunPathPoint(time, observe(time) ?: return null,
                SpaceCompassSunPathEvent.CULMINATION)
        }
        before = after; beforeAngle = afterAngle; after += step
    }
    return result.takeIf { it.isNotEmpty() }
}

internal fun calculateSpaceCompassObservationMoments(body: SpaceCompassCelestialBody, timeMs: Long,
    zone: ZoneId, latitude: Double, longitude: Double, altitude: Double,
    remote: SpaceCompassCelestialRemoteData, checkCancelled: () -> Unit = {}): List<SpaceCompassObservationMoment>? {
    // Short-lived TLEs must never be propagated across a year to fabricate an annual extreme.
    if (!body.supportsDailyPath || body.isEarthSatellite) return null
    val year = Instant.ofEpochMilli(timeMs).atZone(zone).year
    val culminations = calculateSpaceCompassAnnualCulminations(year, zone, latitude, { time ->
        if (body == SpaceCompassCelestialBody.SUN) calculateSpaceCompassSunPosition(time, latitude, longitude, altitude)
        else calculateSpaceCompassCelestialObservation(body, time, latitude, longitude, altitude, remote)?.position
    }, checkCancelled) ?: return null
    return spaceCompassAnnualObservationMoments(culminations)
}

/** Preserve spatial overrides; inactive saved places must never be silently re-enabled. */
internal fun spaceCompassObserverPlanAtMoment(active: SpaceCompassObserverPlan?, saved: SpaceCompassObserverPlan?,
    instant: Long, deviceZone: ZoneId): SpaceCompassObserverPlan? {
    val zone = active?.observationZone(deviceZone) ?: deviceZone
    if (Instant.ofEpochMilli(instant).atZone(zone).year !in 1900..2100) return null
    // An acceleration-only live clock can carry a synthetic zero-position plan. Keep the
    // prepared inactive spatial fields from storage; their activation still comes from active.
    val base = saved ?: active ?: SpaceCompassObserverPlan(0.0, 0.0, 0.0, instant, deviceZone.id)
    return base.copy(timeMs = instant, simulateDate = true, simulateTime = true, momentZoneId = zone.id,
        simulatePosition = active?.simulatePosition == true, simulateAltitude = active?.simulateAltitude == true,
        simulateZone = active?.simulateZone == true, advanceTime = false, clockOffsetMs = null)
}
