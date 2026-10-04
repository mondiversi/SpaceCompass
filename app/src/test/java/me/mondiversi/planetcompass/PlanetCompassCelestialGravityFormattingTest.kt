package me.mondiversi.planetcompass

import org.junit.Assert.*
import org.junit.Test
import java.util.Locale

class PlanetCompassCelestialGravityFormattingTest {
    @Test fun standardEarthGravityIsExactlyOneG() {
        assertEquals("9.8 m/s² (1 g)", formatPlanetCompassCelestialGravity(9.80665, PlanetCompassNumericFormat.INTERNATIONAL))
    }

    @Test fun polarisRetainsItsSmallNonzeroComparison() {
        val gravity = planetCompassCelestialFacts(PlanetCompassCelestialBody.POLARIS).gravity
        assertEquals("0.66 m/s² (0.067 g)", formatPlanetCompassCelestialGravity(gravity, PlanetCompassNumericFormat.INTERNATIONAL, 2))
        assertEquals("0,66 m/s² (0,067 g)", formatPlanetCompassCelestialGravity(gravity, PlanetCompassNumericFormat.EUROPEAN, 2))
    }

    @Test fun sunAndMoonUseTheirExistingReferenceAccelerations() {
        assertEquals("274.0 m/s² (27.94 g)", formatPlanetCompassCelestialGravity(
            planetCompassCelestialFacts(PlanetCompassCelestialBody.SUN).gravity, PlanetCompassNumericFormat.INTERNATIONAL))
        assertEquals("1.6 m/s² (0.163 g)", formatPlanetCompassCelestialGravity(
            planetCompassCelestialFacts(PlanetCompassCelestialBody.MOON).gravity, PlanetCompassNumericFormat.INTERNATIONAL))
    }

    @Test fun unavailableOrInvalidGravityNeverInventsAComparison() {
        for (value in listOf(null, Double.NaN, Double.POSITIVE_INFINITY, Double.NEGATIVE_INFINITY, -1.0, 0.0))
            assertEquals("—", formatPlanetCompassCelestialGravity(value, PlanetCompassNumericFormat.INTERNATIONAL))
        for (body in listOf(PlanetCompassCelestialBody.SEDNA, PlanetCompassCelestialBody.ISS, PlanetCompassCelestialBody.VOYAGER_1, PlanetCompassCelestialBody.VOYAGER_2))
            assertEquals("—", formatPlanetCompassCelestialGravity(planetCompassCelestialFacts(body).gravity, PlanetCompassNumericFormat.INTERNATIONAL))
    }

    @Test fun everyAvailableReferenceUsesTheSameConversion() {
        for (body in PlanetCompassCelestialBody.entries) {
            val gravity = planetCompassCelestialFacts(body).gravity ?: continue
            val formatted = formatPlanetCompassCelestialGravity(gravity, PlanetCompassNumericFormat.INTERNATIONAL)
            val earthG = formatted.substringAfter('(').substringBefore(" g)").toDouble()
            assertEquals(body.name, gravity / 9.80665, earthG, 0.000501)
        }
    }

    @Test fun systemNumericFormatFollowsTheUsersLocale() {
        val original = Locale.getDefault()
        try {
            Locale.setDefault(Locale.ITALIAN)
            assertEquals("1,6 m/s² (0,163 g)", formatPlanetCompassCelestialGravity(1.6, PlanetCompassNumericFormat.SYSTEM))
            Locale.setDefault(Locale.US)
            assertEquals("1.6 m/s² (0.163 g)", formatPlanetCompassCelestialGravity(1.6, PlanetCompassNumericFormat.SYSTEM))
        } finally {
            Locale.setDefault(original)
        }
    }
}
