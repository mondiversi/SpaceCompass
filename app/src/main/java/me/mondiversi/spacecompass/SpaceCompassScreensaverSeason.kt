package me.mondiversi.spacecompass

import io.github.cosinekitty.astronomy.sunPosition
import kotlin.math.*

/** Sun-facing seasonal view, with ecliptic north up. The geographic axis keeps its obliquity;
 * its apparent roll and its lean toward the viewer change continuously around the orbit.
 * Visual spin is accelerated; Earth-fixed lighting always uses the real solar direction.
 * https://science.nasa.gov/earth/earth-observatory/an-epic-view-of-the-seasons/
 */
internal data class SpaceCompassScreensaverSeason(val rollRadians: Double, val pitchRadians: Double,
    val obliquityDegrees: Double) {
    init { require(rollRadians.isFinite() && pitchRadians.isFinite() && obliquityDegrees.isFinite()) }
    val javascript: String get() = "[$rollRadians,$pitchRadians]"
}

/** Independent of GPS, locale, hemisphere and simulated dates: the saver uses the real season. */
internal fun spaceCompassScreensaverSeason(timeMs: Long): SpaceCompassScreensaverSeason {
    val obliquity = spaceCompassSkyReferenceObliquity(timeMs)
    val epsilon = Math.toRadians(obliquity)
    val longitude = Math.toRadians(sunPosition(spaceCompassAstronomyTime(timeMs)).elon)
    val axisX = sin(epsilon) * cos(longitude)
    val axisY = cos(epsilon)
    val axisZ = sin(epsilon) * sin(longitude)
    return SpaceCompassScreensaverSeason(atan2(axisX, axisY), asin(axisZ.coerceIn(-1.0, 1.0)), obliquity)
}
