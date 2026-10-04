package me.mondiversi.planetcompass

import kotlin.math.atan2
import kotlin.math.hypot
import kotlin.math.min
import kotlin.math.floor

/** A directional locator belongs only to the inspected body, not every checked trajectory. */
internal fun planetCompassCelestialDirectionalProjections(
    projections: Map<PlanetCompassCelestialBody, PlanetCompassSunProjection>, activeBody: PlanetCompassCelestialBody?
): Map<PlanetCompassCelestialBody, PlanetCompassSunProjection> {
    if (activeBody == null) return emptyMap()
    val projection = projections[activeBody] ?: return emptyMap()
    if (projection.visible || !projection.x.isFinite() || !projection.y.isFinite()) return emptyMap()
    return mapOf(activeBody to projection)
}

private fun celestialEdgeSlots(width: Double, height: Double, diameter: Double,
    excluded: List<PlanetCompassSunSceneFrame>, ring: Int = 0): List<PlanetCompassSunScenePoint> {
    val inset = diameter / 2 + ring * diameter
    // A full-width point panel occupies the lower edge: use the free sky above it.
    val bottom = excluded.filter { it.left <= 0 && it.width >= width && it.top + it.height >= height }
        .minOfOrNull { it.top }?.coerceIn(0.0, height) ?: height
    if (width < inset * 2 || bottom < inset * 2) return emptyList()
    fun coordinates(end: Double): List<Double> {
        val count = floor((end - inset) / (diameter + 2)).toInt()
        return if (count == 0) listOf(inset) else (0..count).map {
            when (it) { 0 -> inset; count -> end; else -> inset + (end - inset) * it / count }
        }
    }
    return buildList {
        coordinates(width - inset).forEach { x -> add(PlanetCompassSunScenePoint(x, inset)); add(PlanetCompassSunScenePoint(x, bottom - inset)) }
        coordinates(bottom - inset).forEach { y -> add(PlanetCompassSunScenePoint(inset, y)); add(PlanetCompassSunScenePoint(width - inset, y)) }
    }.distinct()
}

private fun celestialMarkerFrame(point: PlanetCompassSunScenePoint, diameter: Double) =
    PlanetCompassSunSceneFrame(point.x - diameter / 2, point.y - diameter / 2, diameter, diameter)

private fun celestialMarkerClear(point: PlanetCompassSunScenePoint, diameter: Double, occupied: List<PlanetCompassSunSceneFrame>): Boolean {
    val rect = celestialMarkerFrame(point, diameter)
    val tolerance = 1e-7 // Floating-point edge equality is touching, not an overlap.
    return occupied.none { rect.left < it.left + it.width - tolerance && rect.left + rect.width > it.left + tolerance &&
        rect.top < it.top + it.height - tolerance && rect.top + rect.height > it.top + tolerance }
}

/** Many simultaneous locators may need smaller previews to stay on the perimeter, not over the reticle. */
internal fun fitPlanetCompassCelestialOffscreenDiameter(projections: Map<PlanetCompassCelestialBody, PlanetCompassSunProjection>,
    width: Double, height: Double, maximum: Double, excluded: List<PlanetCompassSunSceneFrame>): Double {
    if (listOf(width, height, maximum).any { !it.isFinite() || it <= 0 }) return maximum
    val count = projections.values.count { !it.visible && it.x.isFinite() && it.y.isFinite() }
    if (count <= 1) return maximum
    for (step in 0..18) {
        val diameter = maximum * (1 - step * 0.02)
        if (celestialEdgeSlots(width, height, diameter, excluded).count { celestialMarkerClear(it, diameter, excluded) } >= count)
            return diameter
    }
    return maximum * 0.64
}

/** Pack simultaneous directional markers around the edges, without changing their true bearings. */
internal fun placePlanetCompassCelestialOffscreenMarkers(projections: Map<PlanetCompassCelestialBody, PlanetCompassSunProjection>,
    width: Double, height: Double, diameter: Double,
    excluded: List<PlanetCompassSunSceneFrame> = emptyList()): Map<PlanetCompassCelestialBody, PlanetCompassCelestialOffscreenGeometry> {
    if (listOf(width, height, diameter).any { !it.isFinite() || it <= 0 }) return emptyMap()
    val extent = min(diameter, min(width, height))
    val occupied = excluded.toMutableList()
    val edgeRings = (0..2).map { celestialEdgeSlots(width, height, extent, excluded, it) }
    return buildMap {
        planetCompassCelestialCatalogOrder.forEach { body ->
            val projection = projections[body] ?: return@forEach
            val original = placePlanetCompassCelestialOffscreenMarker(projection, width, height, extent, excluded) ?: return@forEach
            // Fill the outer ring before any inward fallback; arbitrary initial positions fragment its capacity.
            val center = if (projections.values.count { !it.visible } == 1) original.center else edgeRings.firstNotNullOfOrNull { ring ->
                ring.filter { celestialMarkerClear(it, extent, occupied) }
                    .minByOrNull { hypot(it.x - original.center.x, it.y - original.center.y) }
            } ?: original.center
            put(body, original.copy(center = center))
            occupied += celestialMarkerFrame(center, extent)
        }
    }
}

internal data class PlanetCompassCelestialOffscreenGeometry(val center: PlanetCompassSunScenePoint, val bearingDegrees: Double)

/** Absolute sky coordinates, independent of RTL. Keep the full miniature and pointer inside the
 * viewport and clear of sky controls; moving the locator must not change the target's bearing.
 */
internal fun placePlanetCompassCelestialOffscreenMarker(
    projection: PlanetCompassSunProjection, width: Double, height: Double, diameter: Double,
    excluded: List<PlanetCompassSunSceneFrame> = emptyList()
): PlanetCompassCelestialOffscreenGeometry? {
    if (projection.visible || listOf(width, height, diameter).any { !it.isFinite() || it <= 0 } ||
        !projection.x.isFinite() || !projection.y.isFinite()) return null
    val half = min(diameter, min(width, height)) / 2
    fun clamp(x: Double, y: Double) = PlanetCompassSunScenePoint(x.coerceIn(half, width - half), y.coerceIn(half, height - half))
    val initial = clamp(projection.x, projection.y)
    val candidates = listOf(initial) + excluded.flatMap { frame -> listOf(
        clamp(frame.left - half, initial.y), clamp(frame.left + frame.width + half, initial.y),
        clamp(initial.x, frame.top - half), clamp(initial.x, frame.top + frame.height + half)) }
    val center = candidates.filter { point -> excluded.none { frame ->
        point.x - half < frame.left + frame.width && point.x + half > frame.left &&
            point.y - half < frame.top + frame.height && point.y + half > frame.top
    } }.minByOrNull { hypot(it.x - initial.x, it.y - initial.y) } ?: initial
    return PlanetCompassCelestialOffscreenGeometry(center,
        Math.toDegrees(atan2(projection.y - height / 2, projection.x - width / 2)))
}
