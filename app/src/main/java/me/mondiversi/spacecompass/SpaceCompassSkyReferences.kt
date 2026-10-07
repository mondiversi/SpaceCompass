package me.mondiversi.spacecompass

import kotlin.math.*

/** Export captions deliberately use the existing international English convention.
 * Tropics/polar circles are terrestrial latitude parallels projected onto the celestial sphere,
 * not local elevation circles, the ecliptic, or terrain boundaries.
 * https://aa.usno.navy.mil/faq/asa_glossary
 */
internal enum class SpaceCompassSkyReference(val label: String, val tint: Int) {
    EQUATOR("Celestial equator", 0xff8be5d1.toInt()),
    CANCER("Tropic of Cancer (sky projection)", 0xffffd18a.toInt()),
    CAPRICORN("Tropic of Capricorn (sky projection)", 0xffffd18a.toInt()),
    ARCTIC("Arctic Circle (sky projection)", 0xff9fd4ff.toInt()),
    ANTARCTIC("Antarctic Circle (sky projection)", 0xff9fd4ff.toInt())
}

internal data class SpaceCompassSkyReferenceCircle(val reference: SpaceCompassSkyReference,
    val declinationDegrees: Double, val directions: List<SpaceCompassSunVector>)

/** Mean obliquity of date, matching the Meeus polynomial used by the solar calculation.
 * Intended for the app's supported 1900–2100 simulation interval.
 */
internal fun spaceCompassSkyReferenceObliquity(timeMs: Long): Double {
    val t = (timeMs / 86_400_000.0 + 2_440_587.5 - 2_451_545.0) / 36_525.0
    return 23.0 + (26.0 + (21.448 - t * (46.815 + t * (0.00059 - t * 0.001813))) / 60) / 60
}

internal fun spaceCompassNorthCelestialPole(latitude: Double): SpaceCompassSunVector {
    require(latitude.isFinite() && latitude in -90.0..90.0)
    val lat = Math.toRadians(latitude)
    return SpaceCompassSunVector(0.0, cos(lat), sin(lat))
}

/** Closed samples around the true terrestrial rotation axis in local ENU (0.5° for exports, 2° live).
 * Complete declination circles are independent of longitude and sidereal time.
 */
internal fun spaceCompassSkyReferenceCircles(latitude: Double, timeMs: Long,
    samplesPerCircle: Int = 720): List<SpaceCompassSkyReferenceCircle> {
    require(samplesPerCircle in 36..720)
    val pole = spaceCompassNorthCelestialPole(latitude)
    val lat = Math.toRadians(latitude)
    val obliquity = spaceCompassSkyReferenceObliquity(timeMs)
    return SpaceCompassSkyReference.entries.map { reference ->
        val declination = when (reference) {
            SpaceCompassSkyReference.EQUATOR -> 0.0
            SpaceCompassSkyReference.CANCER -> obliquity
            SpaceCompassSkyReference.CAPRICORN -> -obliquity
            SpaceCompassSkyReference.ARCTIC -> 90.0 - obliquity
            SpaceCompassSkyReference.ANTARCTIC -> obliquity - 90.0
        }
        val d = Math.toRadians(declination)
        val directions = (0..samplesPerCircle).map { index ->
            val angle = Math.toRadians((index % samplesPerCircle) * 360.0 / samplesPerCircle)
            val meridian = cos(d) * cos(angle)
            SpaceCompassSunVector(cos(d) * sin(angle),
                -meridian * sin(lat) + sin(d) * pole.north,
                meridian * cos(lat) + sin(d) * pole.up)
        }
        SpaceCompassSkyReferenceCircle(reference, declination, directions)
    }
}

internal fun projectSpaceCompassSkyReference(circle: SpaceCompassSkyReferenceCircle,
    orientation: SpaceCompassSunOrientation, width: Double, height: Double,
    perspective: SpaceCompassPerspective? = null): List<SpaceCompassSunPathSegment> = circle.directions.zipWithNext().mapNotNull { (a, b) ->
    projectSpaceCompassSunPathSegment(a, b, orientation, width, height, (a.up + b.up) < 0, perspective)
}

/** Handle the north wrap and undefined azimuth at exact zenith/nadir without spurious chords. */
internal fun spaceCompassSkyReferencePanoramaSegments(circle: SpaceCompassSkyReferenceCircle,
    width: Double, height: Double, centerAzimuthDegrees: Double = 180.0): List<SpaceCompassSunPathSegment> = buildList {
    if (!width.isFinite() || !height.isFinite() || width <= 0 || height <= 0) return@buildList
    circle.directions.zipWithNext().forEach { (a, b) ->
        var start = spaceCompassPanoramaVectorPoint(a, width, height, centerAzimuthDegrees) ?: return@forEach
        var finish = spaceCompassPanoramaVectorPoint(b, width, height, centerAzimuthDegrees) ?: return@forEach
        if (hypot(a.east, a.north) < 1e-10) start = start.copy(x = finish.x)
        if (hypot(b.east, b.north) < 1e-10) finish = finish.copy(x = start.x)
        val dx = finish.x - start.x
        val end = finish.copy(x = finish.x + if (dx > width / 2) -width else if (dx < -width / 2) width else 0.0)
        fun lerp(t: Double) = SpaceCompassSunScenePoint(start.x + (end.x - start.x) * t, start.y + (end.y - start.y) * t)
        val cuts = mutableListOf(0.0, 1.0)
        if (end.x < 0) cuts += -start.x / (end.x - start.x)
        if (end.x > width) cuts += (width - start.x) / (end.x - start.x)
        cuts.distinct().sorted().zipWithNext().forEach { (lo, hi) ->
            if (hi - lo < 1e-9) return@forEach
            val mid = lerp((lo + hi) / 2)
            val shift = if (mid.x < 0) width else if (mid.x > width) -width else 0.0
            add(SpaceCompassSunPathSegment(lerp(lo).let { it.copy(x = (it.x + shift).coerceIn(0.0, width)) },
                lerp(hi).let { it.copy(x = (it.x + shift).coerceIn(0.0, width)) }, mid.y > height / 2))
        }
    }
}
