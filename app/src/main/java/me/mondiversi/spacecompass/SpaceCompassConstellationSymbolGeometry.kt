package me.mondiversi.spacecompass

import java.util.concurrent.ConcurrentHashMap
import kotlin.math.*

private val constellationSymbols = ConcurrentHashMap<SpaceCompassCelestialBody,List<SpaceCompassSunScenePoint>>()

/** A north-up reference symbol from real star directions, not the Southern Cross reused 88 times.
 * The live sky/photo/panorama layers separately project each star at the observer's actual instant. */
internal fun spaceCompassConstellationSymbolPoints(body: SpaceCompassCelestialBody): List<SpaceCompassSunScenePoint> =
    constellationSymbols.computeIfAbsent(body) {
        val pattern = requireNotNull(body.constellationPattern)
        val vectors = pattern.stars.map { spaceCompassStellarReferenceVector(it.reference,946728000000L).unit() }
        val centre = SpaceCompassViewVector(vectors.sumOf { it.x },vectors.sumOf { it.y },vectors.sumOf { it.z }).unit()
        val ra = atan2(centre.y,centre.x); val dec = atan2(centre.z,hypot(centre.x,centre.y))
        val east = SpaceCompassViewVector(-sin(ra),cos(ra),0.0)
        val north = SpaceCompassViewVector(-sin(dec)*cos(ra),-sin(dec)*sin(ra),cos(dec))
        val points = vectors.map { SpaceCompassSunScenePoint(-it.dot(east),-it.dot(north)) }
        val left=points.minOf { it.x };val right=points.maxOf { it.x }
        val top=points.minOf { it.y };val bottom=points.maxOf { it.y }
        val scale=max(right-left,bottom-top).coerceAtLeast(1e-9)/2
        points.map { SpaceCompassSunScenePoint((it.x-(left+right)/2)/scale,(it.y-(top+bottom)/2)/scale) }
    }
