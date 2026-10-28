package me.mondiversi.spacecompass

import io.github.cosinekitty.astronomy.Aberration
import io.github.cosinekitty.astronomy.Body
import io.github.cosinekitty.astronomy.Observer
import io.github.cosinekitty.astronomy.geoVector
import io.github.cosinekitty.astronomy.rotationEqjHor

/** Earth-fixed globe axes: +X at 90°E, +Y at the north pole, +Z at Greenwich on the equator. */
internal data class SpaceCompassScreensaverSunlight(val x: Double, val y: Double, val z: Double) {
    init { require(x.isFinite() && y.isFinite() && z.isFinite()) }
    val javascript: String get() = "[$x,$y,$z]"
}

/** One offline geocentric calculation per idle minute, independent of GPS, time zone and scenario.
 * At Greenwich on the equator, Astronomy Engine's horizontal axes are north, west and zenith.
 * Rotate the geocentric vector only: never subtract a surface observer or apply refraction.
 * https://github.com/cosinekitty/astronomy/tree/master/source/kotlin#coordinate-transforms
 */
internal fun spaceCompassScreensaverSunlight(nowMs: Long): SpaceCompassScreensaverSunlight {
    val time = spaceCompassAstronomyTime(nowMs)
    val sun = rotationEqjHor(time,Observer(0.0,0.0,0.0)).rotate(geoVector(Body.Sun,time,Aberration.Corrected))
    val length = sun.length()
    return SpaceCompassScreensaverSunlight(-sun.y/length,sun.x/length,sun.z/length)
}
