package me.mondiversi.spacecompass

import java.text.SimpleDateFormat
import java.time.Instant
import java.time.ZoneId
import java.util.Date
import java.util.Locale
import java.util.TimeZone

internal const val SPACE_COMPASS_CELESTIAL_RETICLE_RADIUS_DP = 19f

/** Keep provenance even when the live instant coincides exactly with an hourly/event point. */
internal data class SpaceCompassCelestialFocusedPoint(val point: SpaceCompassSunPathPoint, val isCurrent: Boolean)

internal fun focusedSpaceCompassCelestialPoint(
    pathPoints: List<Pair<SpaceCompassSunPathPoint, SpaceCompassSunScenePoint>>,
    currentPoint: SpaceCompassSunPathPoint?, projectedCurrent: SpaceCompassSunScenePoint?,
    width: Double, height: Double, radius: Double
): SpaceCompassCelestialFocusedPoint? {
    val targets = buildList {
        // The live body wins an exact tie; among path dots, named events still precede hours.
        if (currentPoint != null && projectedCurrent != null)
            add(SpaceCompassCelestialFocusedPoint(currentPoint, true) to projectedCurrent)
        pathPoints.forEach { (point, projection) -> add(SpaceCompassCelestialFocusedPoint(point, false) to projection) }
    }
    return focusedSpaceCompassCelestialPathPoint(targets, width, height, radius) {
        it.isCurrent || it.point.event != SpaceCompassSunPathEvent.HOUR
    }
}

/** Choose the visible target inside the reticle independently of a tapped selection. */
internal fun <T : Any> focusedSpaceCompassCelestialPathPoint(
    points: List<Pair<T, SpaceCompassSunScenePoint>>, width: Double, height: Double, radius: Double
): T? = focusedSpaceCompassCelestialPathPoint(points, width, height, radius, preferredOnTie = { false })

/** Optional preference only resolves a true tie; it never overrides a closer point. */
internal fun <T : Any> focusedSpaceCompassCelestialPathPoint(
    points: List<Pair<T, SpaceCompassSunScenePoint>>, width: Double, height: Double, radius: Double,
    preferredOnTie: (T) -> Boolean
): T? {
    if (!width.isFinite() || !height.isFinite() || !radius.isFinite() ||
        width <= 0 || height <= 0 || radius < 0) return null
    val radiusSquared = radius * radius
    var focused: T? = null
    var nearestSquared = radiusSquared
    for ((marker, point) in points) {
        if (!point.x.isFinite() || !point.y.isFinite() ||
            point.x !in 0.0..width || point.y !in 0.0..height) continue
        val dx = point.x - width / 2
        val dy = point.y - height / 2
        val distanceSquared = dx * dx + dy * dy
        // A named event at the same projected position must not be masked by an hourly dot.
        // Otherwise retain the nearest dot and the original stable order.
        val preferredTie = focused?.let {
            distanceSquared == nearestSquared && preferredOnTie(marker) && !preferredOnTie(it)
        } ?: false
        if (distanceSquared <= radiusSquared && (focused == null || distanceSquared < nearestSquared || preferredTie)) {
            focused = marker
            nearestSquared = distanceSquared
        }
    }
    return focused
}

/** Display only: never substitutes a path point's time for the live ephemeris time. */
internal fun formatSpaceCompassCelestialMoment(
    timeMs: Long, nowMs: Long, zone: ZoneId, timeFormat: SpaceCompassTimeFormat,
    dateFormat: SpaceCompassDateFormat, locale: Locale, deviceLocale: Locale = locale
): String {
    require(timeFormat != SpaceCompassTimeFormat.SYSTEM) // Resolved from Android before entering this pure formatter.
    val local = Instant.ofEpochMilli(timeMs).atZone(zone)
    val nowDate = Instant.ofEpochMilli(nowMs).atZone(zone).toLocalDate()
    val timezone = TimeZone.getTimeZone(zone)
    val time = SimpleDateFormat(if (timeFormat == SpaceCompassTimeFormat.H12) "h:mm a" else "HH:mm", locale)
        .apply { timeZone = timezone }.format(Date(timeMs))
    // Distinguish the two occurrences of a repeated local hour when daylight saving ends.
    val offset = if (zone.rules.getValidOffsets(local.toLocalDateTime()).size > 1)
        " (UTC${local.offset.id})" else ""
    val date = if (local.toLocalDate() != nowDate)
        " · ${formatSpaceCompassDateOnly(timeMs, dateFormat,
            if (dateFormat == SpaceCompassDateFormat.SYSTEM) deviceLocale else locale, timezone)}" else ""
    return time + offset + date
}

/** Clamp to the viewport and avoid both controls and the locator itself. Null means no safe room. */
internal fun placeSpaceCompassCelestialTimeBadge(
    width: Double, height: Double, labelWidth: Double, labelHeight: Double,
    anchor: SpaceCompassSunScenePoint?, margin: Double, gap: Double,
    excluded: List<SpaceCompassSunSceneFrame> = emptyList()
): SpaceCompassSunScenePoint? {
    val dimensions = listOf(width, height, labelWidth, labelHeight, margin, gap)
    if (dimensions.any { !it.isFinite() || it < 0 } || labelWidth <= 0 || labelHeight <= 0 ||
        width < labelWidth + 2 * margin || height < labelHeight + 2 * margin) return null
    if (anchor != null && (!anchor.x.isFinite() || !anchor.y.isFinite())) return null
    val marker = anchor?.let { SpaceCompassSunSceneFrame(it.x - gap / 2, it.y - gap / 2, gap, gap) }
    val obstacles = excluded + listOfNotNull(marker)
    val candidates = buildList {
        anchor?.let {
            add(SpaceCompassSunScenePoint(it.x - labelWidth / 2, it.y + gap))
            add(SpaceCompassSunScenePoint(it.x - labelWidth / 2, it.y - gap - labelHeight))
            add(SpaceCompassSunScenePoint(it.x + gap, it.y - labelHeight / 2))
            add(SpaceCompassSunScenePoint(it.x - gap - labelWidth, it.y - labelHeight / 2))
        }
        add(SpaceCompassSunScenePoint(margin, margin))
        add(SpaceCompassSunScenePoint(margin, height - margin - labelHeight))
        add(SpaceCompassSunScenePoint(width - margin - labelWidth, height - margin - labelHeight))
    }
    return candidates.map { SpaceCompassSunScenePoint(
        it.x.coerceIn(margin, width - margin - labelWidth),
        it.y.coerceIn(margin, height - margin - labelHeight))
    }.firstOrNull { point -> obstacles.none { obstacle ->
        point.x < obstacle.left + obstacle.width && point.x + labelWidth > obstacle.left &&
            point.y < obstacle.top + obstacle.height && point.y + labelHeight > obstacle.top
    } }
}
