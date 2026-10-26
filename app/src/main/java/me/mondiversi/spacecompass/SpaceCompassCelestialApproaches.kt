package me.mondiversi.spacecompass

import kotlin.math.*

internal const val SPACE_COMPASS_APPROACH_LIMIT_DEGREES = 1.5
internal data class SpaceCompassApproach(val other: SpaceCompassCelestialBody, val separationDegrees: Double)
internal data class SpaceCompassApproachPair(val first: SpaceCompassCelestialBody, val second: SpaceCompassCelestialBody,
    val timeMs: Long, val firstPosition: SpaceCompassSunPosition, val secondPosition: SpaceCompassSunPosition,
    val separationDegrees: Double)

/** Both paths carry the same event. A nearby duplicate glyph must not cover its partner's rings. */
internal fun spaceCompassApproachMarkerIsDuplicate(timeMs: Long, point: SpaceCompassSunScenePoint,
    occupied: List<Pair<Long, SpaceCompassSunScenePoint>>, spacing: Double, wrapWidth: Double? = null): Boolean =
    occupied.any { (time, other) ->
        var dx = abs(point.x - other.x)
        if (wrapWidth != null && wrapWidth > 0) dx = minOf(dx, abs(wrapWidth - dx))
        time == timeMs && hypot(dx, point.y - other.y) < spacing
    }

/** Great-circle distance, including azimuth wrap and the degenerate zenith/nadir direction. */
internal fun spaceCompassAngularSeparation(a: SpaceCompassSunPosition, b: SpaceCompassSunPosition): Double {
    if (listOf(a.azimuthDegrees, a.elevationDegrees, b.azimuthDegrees, b.elevationDegrees).any { !it.isFinite() } ||
        a.elevationDegrees !in -90.0..90.0 || b.elevationDegrees !in -90.0..90.0) return Double.NaN
    val x = spaceCompassSunCompassDirection(a.azimuthDegrees, a.elevationDegrees)
    val y = spaceCompassSunCompassDirection(b.azimuthDegrees, b.elevationDegrees)
    val cross = sqrt((x.north*y.up-x.up*y.north).pow(2) + (x.up*y.east-x.east*y.up).pow(2) +
        (x.east*y.north-x.north*y.east).pow(2))
    return Math.toDegrees(atan2(cross, x.east*y.east+x.north*y.north+x.up*y.up))
}

/** Both positions are evaluated at exactly t. Curves crossing at different times cannot trigger this. */
internal fun findSpaceCompassApproachMinima(start: Long, end: Long, step: Long,
    sample: (Long) -> Pair<SpaceCompassSunPosition, SpaceCompassSunPosition>?,
    exact: (Long) -> Pair<SpaceCompassSunPosition, SpaceCompassSunPosition>? = sample,
    checkCancelled: () -> Unit = {}): List<Pair<Long, Double>> {
    require(step > 0 && end >= start)
    fun separation(t: Long, precise: Boolean = false) = (if (precise) exact(t) else sample(t))?.let {
        spaceCompassAngularSeparation(it.first, it.second).takeIf(Double::isFinite)
    }
    val samples = buildList {
        val spacing = minOf(step,maxOf(1,(end-start)/4))
        var t = start
        while (t < end) { checkCancelled(); add(t to separation(t)); t += minOf(spacing, end - t) }
        add(end to separation(end))
    }
    if (samples.size < 2) return emptyList()
    val candidates = mutableListOf<Pair<Long, Long>>()
    for (i in samples.indices) {
        val value = samples[i].second ?: continue
        val before = samples.getOrNull(i-1)?.second
        val after = samples.getOrNull(i+1)?.second
        if ((before == null || value <= before) && (after == null || value <= after) &&
            (before == null || value < before || after == null || value < after) && value <= 12.0) {
            candidates += samples[maxOf(0, i-1)].first to samples[minOf(samples.lastIndex, i+1)].first
        }
    }
    // A nearly constant pair has one representative closest instant, not hundreds of numerical minima.
    val finite = samples.mapNotNull { it.second }
    if (finite.isNotEmpty() && finite.max() - finite.min() < .0001) {
        candidates.clear()
        val best = samples.filter { it.second != null }.minBy { it.second!! }.first
        candidates += best to best
    }
    val found = candidates.mapNotNull { (left, right) ->
        checkCancelled()
        var lo = left; var hi = right
        while (hi - lo > 100) {
            val a = lo + ((hi-lo) * .38196601125).toLong()
            val b = lo + ((hi-lo) * .61803398875).toLong()
            val da = separation(a, true) ?: return@mapNotNull null
            val db = separation(b, true) ?: return@mapNotNull null
            if (da <= db) hi = b else lo = a
        }
        val times = listOf(left, right, lo + (hi-lo)/2)
        times.mapNotNull { t -> separation(t, true)?.let { t to it } }.minByOrNull { it.second }
            ?.takeIf { it.second <= SPACE_COMPASS_APPROACH_LIMIT_DEGREES }
    }.sortedBy { it.first }
    return found.fold(mutableListOf()) { result, event ->
        if (result.isEmpty() || event.first - result.last().first > 1_000) result += event
        else if (event.second < result.last().second) result[result.lastIndex] = event
        result
    }
}

private fun SpaceCompassSunDailyPath.directionAt(t: Long): SpaceCompassSunPosition? {
    val index = samples.binarySearchBy(t) { it.timeMs }
    if (index >= 0) return samples[index].position
    val i = -index - 1
    if (i == 0 || i == samples.size) return null
    val a = samples[i-1]; val b = samples[i]
    val u = (t-a.timeMs).toDouble()/(b.timeMs-a.timeMs)
    val x = a.direction.east*(1-u)+b.direction.east*u
    val y = a.direction.north*(1-u)+b.direction.north*u
    val z = a.direction.up*(1-u)+b.direction.up*u
    if (sqrt(x*x+y*y+z*z) < .1) return null
    return SpaceCompassSunPosition((Math.toDegrees(atan2(x,y))+360)%360, Math.toDegrees(atan2(z,hypot(x,y))))
}

internal fun spaceCompassApproachPaths(paths: Map<SpaceCompassCelestialBody, SpaceCompassSunDailyPath>,
    observe: (SpaceCompassCelestialBody, Long) -> SpaceCompassSunPosition?, checkCancelled: () -> Unit = {}): Map<SpaceCompassCelestialBody, SpaceCompassSunDailyPath> {
    val cache = object : LinkedHashMap<Pair<SpaceCompassCelestialBody,Long>,SpaceCompassSunPosition?>(1024,.75f,true) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<Pair<SpaceCompassCelestialBody,Long>,SpaceCompassSunPosition?>?) = size > 20_000
    }
    fun exact(body: SpaceCompassCelestialBody,t: Long): SpaceCompassSunPosition? {
        val key = body to t
        if (cache.containsKey(key)) return cache[key]
        return observe(body,t).also { cache[key] = it }
    }
    val markers = paths.mapValues { mutableListOf<SpaceCompassSunPathPoint>() }
    val bodies = paths.keys.sortedBy { it.ordinal }
    for (i in bodies.indices) for (j in i+1 until bodies.size) {
        checkCancelled()
        val a = bodies[i]; val b = bodies[j]
        if (spaceCompassSharesGalacticNucleus(a,b)) continue
        val pa = paths.getValue(a); val pb = paths.getValue(b)
        if (pa.samples.isEmpty() || pb.samples.isEmpty()) continue
        val start = maxOf(pa.samples.first().timeMs, pb.samples.first().timeMs)
        val end = minOf(pa.samples.last().timeMs, pb.samples.last().timeMs)
        if (end <= start) continue
        if (a.deepSkyReference != null && b.deepSkyReference != null) {
            val first = exact(a, start); val second = exact(b, start)
            if (first != null && second != null &&
                spaceCompassAngularSeparation(first, second) > SPACE_COMPASS_APPROACH_LIMIT_DEGREES + .1) continue
        }
        fun pair(t: Long, precise: Boolean): Pair<SpaceCompassSunPosition,SpaceCompassSunPosition>? {
            // Do not bracket a fast pass from the much coarser display-orbit polyline.
            val actual = precise || a.isEarthSatellite || b.isEarthSatellite
            val first = if (actual) exact(a,t) else pa.directionAt(t)
            val second = if (actual) exact(b,t) else pb.directionAt(t)
            return if (first == null || second == null) null else first to second
        }
        findSpaceCompassApproachMinima(start, end, if (a.isEarthSatellite || b.isEarthSatellite) 5_000 else 180_000,
            { pair(it,false) }, { pair(it,true) }, checkCancelled).forEach { (t, distance) ->
            for ((body,other) in listOf(a to b,b to a)) exact(body,t)?.let { position ->
                markers.getValue(body) += SpaceCompassSunPathPoint(t, position, SpaceCompassSunPathEvent.APPROACH,
                    moonPhase = if (body == SpaceCompassCelestialBody.MOON) calculateSpaceCompassMoonPhase(t) else null,
                    approach = SpaceCompassApproach(other,distance))
            }
        }
    }
    return paths.mapValues { (body,path) -> path.copy(markers = (path.markers.filter { it.approach == null } +
        markers.getValue(body)).sortedBy { it.timeMs }) }
}

internal fun spaceCompassCurrentApproaches(timeMs: Long, positions: Map<SpaceCompassCelestialBody,SpaceCompassSunPosition>): List<SpaceCompassApproachPair> {
    return buildList {
        for ((a, b) in spaceCompassApproachCandidates(positions)) {
            if (spaceCompassSharesGalacticNucleus(a,b)) continue
            val first = positions.getValue(a); val second = positions.getValue(b)
            val distance = spaceCompassAngularSeparation(first,second)
            if (distance.isFinite() && distance <= SPACE_COMPASS_APPROACH_LIMIT_DEGREES)
                add(SpaceCompassApproachPair(a,b,timeMs,first,second,distance))
        }
    }
}
