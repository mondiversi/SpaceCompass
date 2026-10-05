package me.mondiversi.spacecompass

import org.junit.Assert.*
import org.junit.Test
import java.util.Locale

class SpaceCompassCelestialGravityFormattingTest {
    @Test fun standardEarthGravityIsExactlyOneG() {
        assertEquals("9.8 m/s² (1 g)", formatSpaceCompassCelestialGravity(9.80665, SpaceCompassNumericFormat.INTERNATIONAL))
    }

    @Test fun polarisRetainsItsSmallNonzeroComparison() {
        val gravity = spaceCompassCelestialFacts(SpaceCompassCelestialBody.POLARIS).gravity
        assertEquals("0.66 m/s² (0.067 g)", formatSpaceCompassCelestialGravity(gravity, SpaceCompassNumericFormat.INTERNATIONAL, 2))
        assertEquals("0,66 m/s² (0,067 g)", formatSpaceCompassCelestialGravity(gravity, SpaceCompassNumericFormat.EUROPEAN, 2))
    }

    @Test fun sunAndMoonUseTheirExistingReferenceAccelerations() {
        assertEquals("274.0 m/s² (27.94 g)", formatSpaceCompassCelestialGravity(
            spaceCompassCelestialFacts(SpaceCompassCelestialBody.SUN).gravity, SpaceCompassNumericFormat.INTERNATIONAL))
        assertEquals("1.6 m/s² (0.163 g)", formatSpaceCompassCelestialGravity(
            spaceCompassCelestialFacts(SpaceCompassCelestialBody.MOON).gravity, SpaceCompassNumericFormat.INTERNATIONAL))
    }

    @Test fun unavailableOrInvalidGravityNeverInventsAComparison() {
        for (value in listOf(null, Double.NaN, Double.POSITIVE_INFINITY, Double.NEGATIVE_INFINITY, -1.0, 0.0))
            assertEquals("—", formatSpaceCompassCelestialGravity(value, SpaceCompassNumericFormat.INTERNATIONAL))
        for (body in listOf(SpaceCompassCelestialBody.SEDNA, SpaceCompassCelestialBody.ISS, SpaceCompassCelestialBody.VOYAGER_1, SpaceCompassCelestialBody.VOYAGER_2))
            assertEquals("—", formatSpaceCompassCelestialGravity(spaceCompassCelestialFacts(body).gravity, SpaceCompassNumericFormat.INTERNATIONAL))
    }

    @Test fun everyAvailableReferenceUsesTheSameConversion() {
        for (body in SpaceCompassCelestialBody.entries) {
            val gravity = spaceCompassCelestialFacts(body).gravity ?: continue
            val formatted = formatSpaceCompassCelestialGravity(gravity, SpaceCompassNumericFormat.INTERNATIONAL)
            val earthG = formatted.substringAfter('(').substringBefore(" g)").toDouble()
            assertEquals(body.name, gravity / 9.80665, earthG, 0.000501)
        }
    }

    @Test fun systemNumericFormatFollowsTheUsersLocale() {
        val original = Locale.getDefault()
        try {
            Locale.setDefault(Locale.ITALIAN)
            assertEquals("1,6 m/s² (0,163 g)", formatSpaceCompassCelestialGravity(1.6, SpaceCompassNumericFormat.SYSTEM))
            Locale.setDefault(Locale.US)
            assertEquals("1.6 m/s² (0.163 g)", formatSpaceCompassCelestialGravity(1.6, SpaceCompassNumericFormat.SYSTEM))
        } finally {
            Locale.setDefault(original)
        }
    }
}
