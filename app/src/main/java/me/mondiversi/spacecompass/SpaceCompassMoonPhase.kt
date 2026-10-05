package me.mondiversi.spacecompass

import io.github.cosinekitty.astronomy.Body
import io.github.cosinekitty.astronomy.illumination
import io.github.cosinekitty.astronomy.moonPhase
import kotlin.math.floor

internal enum class SpaceCompassMoonPhaseKind(val nameResource: Int) {
    NEW(R.string.moon_phase_new), WAXING_CRESCENT(R.string.moon_phase_waxing_crescent),
    FIRST_QUARTER(R.string.moon_phase_first_quarter), WAXING_GIBBOUS(R.string.moon_phase_waxing_gibbous),
    FULL(R.string.moon_phase_full), WANING_GIBBOUS(R.string.moon_phase_waning_gibbous),
    LAST_QUARTER(R.string.moon_phase_last_quarter), WANING_CRESCENT(R.string.moon_phase_waning_crescent)
}

/** UTC phase and illuminated fraction, independent of the compass and observer's horizon.
 * The icon is a conventional phase diagram, not the Moon's orientation in the camera view.
 */
internal data class SpaceCompassMoonPhase(val angleDegrees: Double, val illuminatedFraction: Double) {
    init {
        require(angleDegrees.isFinite() && angleDegrees >= 0 && angleDegrees < 360)
        require(illuminatedFraction.isFinite() && illuminatedFraction in 0.0..1.0)
    }
    val waxing: Boolean get() = angleDegrees < 180
    val kind: SpaceCompassMoonPhaseKind get() = SpaceCompassMoonPhaseKind.entries[floor((angleDegrees + 22.5) / 45).toInt() % 8]

    // On the unit disc, the lit boundary is an ellipse. Its area equals the physical lit fraction.
    fun litHorizontalBounds(y: Double): Pair<Double, Double> {
        require(y.isFinite() && y in -1.0..1.0)
        val limb = kotlin.math.sqrt((1 - y * y).coerceAtLeast(0.0))
        val terminator = (1 - 2 * illuminatedFraction) * limb
        return if (waxing) terminator to limb else -limb to -terminator
    }
}

internal fun calculateSpaceCompassMoonPhase(timeMs: Long): SpaceCompassMoonPhase {
    val time = spaceCompassAstronomyTime(timeMs)
    return SpaceCompassMoonPhase(moonPhase(time), illumination(Body.Moon, time).phaseFraction.coerceIn(0.0, 1.0))
}
