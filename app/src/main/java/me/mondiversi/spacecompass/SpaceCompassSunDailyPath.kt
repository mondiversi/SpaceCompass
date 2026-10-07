package me.mondiversi.spacecompass

import java.time.LocalDate
import java.time.ZoneId
import kotlin.math.*

internal enum class SpaceCompassSunPathEvent { HOUR, SUNRISE, CULMINATION, SUNSET, MINIMUM }

internal const val SPACE_COMPASS_SUN_PATH_TOUCH_RADIUS_DP = 32f

internal data class SpaceCompassSunPathPoint(
    val timeMs: Long, val position: SpaceCompassSunPosition, val event: SpaceCompassSunPathEvent = SpaceCompassSunPathEvent.HOUR,
    val moonPhase: SpaceCompassMoonPhase? = null,
    val coincidentEvents: Set<SpaceCompassSunPathEvent> = emptySet()
) {
    val events: Set<SpaceCompassSunPathEvent> get() = coincidentEvents + event
    // Calculate once per day/location, not trigonometry on every orientation frame.
    val direction: SpaceCompassSunVector = Math.toRadians(position.elevationDegrees).let { el ->
        val az = Math.toRadians(position.azimuthDegrees)
        SpaceCompassSunVector(cos(el) * sin(az), cos(el) * cos(az), sin(el))
    }
}

internal data class SpaceCompassSunDailyPath(
    val date: LocalDate, val zone: ZoneId, val samples: List<SpaceCompassSunPathPoint>,
    val markers: List<SpaceCompassSunPathPoint>, val body: SpaceCompassCelestialBody = SpaceCompassCelestialBody.SUN,
    // Separate from the current orbit: the next geometric pass can be many hours later.
    val issPass: List<SpaceCompassSunPathPoint> = emptyList()
)

/** UTC solar calculations, local civil-day bounds. DST days correctly contain 23/25 hourly markers.
 * Sunrise/set use the conventional -0.833° geometric centre altitude (ideal unobstructed horizon).
 * https://gml.noaa.gov/grad/solcalc/calcdetails.html ; not a terrain/weather visibility prediction.
 */
internal fun calculateSpaceCompassSunDailyPath(
    date: LocalDate, zone: ZoneId, latitude: Double, longitude: Double, altitudeMeters: Double = 0.0
): SpaceCompassSunDailyPath {
    val start = date.atStartOfDay(zone).toInstant().toEpochMilli()
    val end = date.plusDays(1).atStartOfDay(zone).toInstant().toEpochMilli()
    fun point(time: Long, event: SpaceCompassSunPathEvent = SpaceCompassSunPathEvent.HOUR) = SpaceCompassSunPathPoint(time,
        calculateSpaceCompassSunPosition(time, latitude, longitude, altitudeMeters), event)
    val step = 180_000L
    val samples = buildList {
        var time = start
        while (time < end) { add(point(time)); time += step }
        add(point(end))
    }
    val markers = mutableListOf<SpaceCompassSunPathPoint>()
    var hour = start
    while (hour < end) { markers += point(hour); hour += 3_600_000L }
    samples.zipWithNext().forEach { (a, b) ->
        val rising = a.position.elevationDegrees < -0.833 && b.position.elevationDegrees >= -0.833
        val setting = a.position.elevationDegrees >= -0.833 && b.position.elevationDegrees < -0.833
        if (rising || setting) {
            var low = a.timeMs; var high = b.timeMs
            while (high - low > 250L) {
                val mid = low + (high - low) / 2
                val above = point(mid).position.elevationDegrees >= -0.833
                if (above == rising) high = mid else low = mid
            }
            markers += point(low + (high - low) / 2,
                if (rising) SpaceCompassSunPathEvent.SUNRISE else SpaceCompassSunPathEvent.SUNSET)
        }
    }
    for (minimum in listOf(false, true)) {
        markers += requireNotNull(refineSpaceCompassPathExtremum(samples, end, minimum, ::point))
    }
    return SpaceCompassSunDailyPath(date, zone, samples, mergeCoincidentSpaceCompassPathEvents(markers))
}

internal data class SpaceCompassSunPathSegment(
    val start: SpaceCompassSunScenePoint, val end: SpaceCompassSunScenePoint, val belowHorizon: Boolean
)

/** Invisible circular hit area; overlapping targets select the nearest dot, with stable ties. */
internal fun tappedSpaceCompassSunPathPoint(
    points: List<Pair<SpaceCompassSunPathPoint, SpaceCompassSunScenePoint>>, tap: SpaceCompassSunScenePoint, radius: Double
): SpaceCompassSunPathPoint? {
    if (!tap.x.isFinite() || !tap.y.isFinite() || !radius.isFinite() || radius < 0) return null
    val radiusSquared = radius * radius
    var selected: SpaceCompassSunPathPoint? = null
    var nearestSquared = radiusSquared
    for ((marker, point) in points) {
        if (!point.x.isFinite() || !point.y.isFinite()) continue
        val dx = tap.x - point.x
        val dy = tap.y - point.y
        val distanceSquared = dx * dx + dy * dy
        if (distanceSquared <= radiusSquared && (selected == null || distanceSquared < nearestSquared)) {
            selected = marker
            nearestSquared = distanceSquared
        }
    }
    return selected
}

/** The exact live instant is first so it wins ties with an overlapping hourly dot. */
internal fun selectableSpaceCompassCelestialPathPoints(
    hourly: List<Pair<SpaceCompassSunPathPoint, SpaceCompassSunScenePoint>>,
    current: SpaceCompassSunPathPoint?, projectedCurrent: SpaceCompassSunScenePoint?
): List<Pair<SpaceCompassSunPathPoint, SpaceCompassSunScenePoint>> {
    val pathTargets = hourly.sortedBy { if (it.first.event == SpaceCompassSunPathEvent.HOUR) 1 else 0 }
    return if (current == null || projectedCurrent == null) pathTargets
        else listOf(current to projectedCurrent) + pathTargets
}

/** Step chronologically from either an hourly/event marker or an arbitrary tapped live instant. */
internal fun adjacentSpaceCompassSunPathPoint(markers: List<SpaceCompassSunPathPoint>, selected: SpaceCompassSunPathPoint,
    forward: Boolean): SpaceCompassSunPathPoint? {
    val index = markers.indexOf(selected)
    if (index >= 0) return markers[(index + (if (forward) 1 else -1) + markers.size) % markers.size]
    return if (forward) {
        markers.filter { it.timeMs > selected.timeMs }.minByOrNull { it.timeMs }
            ?: markers.minByOrNull { it.timeMs }
    } else {
        markers.filter { it.timeMs < selected.timeMs }.maxByOrNull { it.timeMs }
            ?: markers.maxByOrNull { it.timeMs }
    }
}

/** Clip in camera space BEFORE perspective division; no false chords across the back of the phone. */
internal fun projectSpaceCompassSunPathSegment(
    a: SpaceCompassSunVector, b: SpaceCompassSunVector, orientation: SpaceCompassSunOrientation,
    width: Double, height: Double, belowHorizon: Boolean, perspective: SpaceCompassPerspective? = null
): SpaceCompassSunPathSegment? {
    if (width <= 0 || height <= 0 || !width.isFinite() || !height.isFinite()) return null
    val focal = perspective?.focalY(height) ?: spaceCompassSunProjectionFocalLength(height)
    val focalX = perspective?.focalX(width) ?: focal
    val centerX = (perspective?.principalX ?: .5) * width
    val centerY = (perspective?.principalY ?: .5) * height
    fun camera(v: SpaceCompassSunVector) = doubleArrayOf(v.dot(orientation.right),
        -v.dot(orientation.screenUp), v.dot(orientation.forward))
    val from = camera(a); val to = camera(b)
    var lo = 0.0; var hi = 1.0
    val planes = listOf(
        doubleArrayOf(0.0, 0.0, 1.0, -1e-5),
        doubleArrayOf(focalX, 0.0, centerX, 0.0),
        doubleArrayOf(-focalX, 0.0, width - centerX, 0.0),
        doubleArrayOf(0.0, focal, centerY, 0.0),
        doubleArrayOf(0.0, -focal, height - centerY, 0.0)
    )
    for (p in planes) {
        fun side(v: DoubleArray) = p[0] * v[0] + p[1] * v[1] + p[2] * v[2] + p[3]
        val x = side(from); val y = side(to)
        if (x < 0 && y < 0) return null
        if ((x < 0) != (y < 0)) {
            val t = x / (x - y)
            if (x < 0) lo = max(lo, t) else hi = min(hi, t)
        }
        if (lo > hi) return null
    }
    fun screen(t: Double): SpaceCompassSunScenePoint {
        val v = DoubleArray(3) { from[it] + (to[it] - from[it]) * t }
        return SpaceCompassSunScenePoint((centerX + focalX * v[0] / v[2]).coerceIn(0.0, width),
            (centerY + focal * v[1] / v[2]).coerceIn(0.0, height))
    }
    return SpaceCompassSunPathSegment(screen(lo), screen(hi), belowHorizon)
}

internal fun projectSpaceCompassSunDailyPath(
    path: SpaceCompassSunDailyPath, orientation: SpaceCompassSunOrientation, width: Double, height: Double,
    perspective: SpaceCompassPerspective? = null
): List<SpaceCompassSunPathSegment> = buildList {
    path.samples.zipWithNext().forEach { (a, b) ->
        // Split at the real horizontal horizon so solid/dashed strokes switch exactly there.
        if ((a.direction.up < 0) != (b.direction.up < 0)) {
            val t = a.direction.up / (a.direction.up - b.direction.up)
            val crossing = SpaceCompassSunVector(a.direction.east + (b.direction.east - a.direction.east) * t,
                a.direction.north + (b.direction.north - a.direction.north) * t, 0.0).normalized()
            projectSpaceCompassSunPathSegment(a.direction, crossing, orientation, width, height,
                a.direction.up < 0, perspective)?.let { add(it) }
            projectSpaceCompassSunPathSegment(crossing, b.direction, orientation, width, height,
                b.direction.up < 0, perspective)?.let { add(it) }
        } else projectSpaceCompassSunPathSegment(a.direction, b.direction, orientation, width, height,
            a.direction.up < 0, perspective)?.let { add(it) }
    }
}
