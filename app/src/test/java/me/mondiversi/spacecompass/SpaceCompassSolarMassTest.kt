package me.mondiversi.spacecompass

import org.junit.Assert.*
import org.junit.Test

class SpaceCompassSolarMassTest {
    @Test fun compactObjectMassesRetainMeasurementAndModelQualifications() {
        for (numeric in SpaceCompassNumericFormat.entries) {
            fun mass(body: SpaceCompassCelestialBody) =
                formatSpaceCompassCelestialMass(body, spaceCompassCelestialFacts(body), numeric)
            assertEquals("1 M☉", mass(SpaceCompassCelestialBody.SUN))
            assertEquals(formatSpaceCompassNumber(5.13, 3, numeric, minimumDigits = 0) + " ± " + formatSpaceCompassNumber(0.28, 3, numeric, minimumDigits = 0) + " M☉",
                mass(SpaceCompassCelestialBody.POLARIS))
            assertEquals(formatSpaceCompassNumber(1.418, 3, numeric, minimumDigits = 0) + " ± " +
                formatSpaceCompassNumber(0.044, 3, numeric, minimumDigits = 0) + " M☉",
                mass(SpaceCompassCelestialBody.PSR_J0437))
            assertEquals("† " + formatSpaceCompassNumber(1.4, 3, numeric, minimumDigits = 0) + " M☉",
                mass(SpaceCompassCelestialBody.RX_J1856))
            for (body in listOf(SpaceCompassCelestialBody.SAGITTARIUS_A,
                SpaceCompassCelestialBody.ANDROMEDA_CORE, SpaceCompassCelestialBody.TON_618)) {
                val value = mass(body)
                assertTrue(value.startsWith("≈ "))
                assertTrue(value.endsWith(" M☉"))
                assertFalse(value.contains("kg"))
                assertNull(spaceCompassCelestialFacts(body).gravity)
                assertNull(spaceCompassCelestialFacts(body).density)
            }
            assertEquals("— M☉", mass(SpaceCompassCelestialBody.STEPHENSON_2_18))
            assertEquals(formatSpaceCompassNumber(1.988, 3, numeric, minimumDigits = 0) + " M☉ (A+B)", mass(SpaceCompassCelestialBody.ALPHA_CENTAURI))
            assertTrue(mass(SpaceCompassCelestialBody.MOON).endsWith(" kg"))
            assertTrue(mass(SpaceCompassCelestialBody.ISS).endsWith(" kg"))
        }
    }

    @Test fun unknownOrInvalidSolarMassesAndUncertaintiesStayUnknown() {
        val body = SpaceCompassCelestialBody.TON_618
        for (mass in listOf(null, -1.0, 0.0, Double.NaN, Double.POSITIVE_INFINITY))
            assertEquals(if (mass == null) "— M☉" else "—", formatSpaceCompassCelestialMass(body,
                SpaceCompassCelestialFacts(massSolar = mass), SpaceCompassNumericFormat.EUROPEAN))
        for (error in listOf(-1.0, 0.0, Double.NaN, Double.POSITIVE_INFINITY))
            assertEquals("—", formatSpaceCompassCelestialMass(body,
                SpaceCompassCelestialFacts(massSolar = 1.4, massSolarError = error), SpaceCompassNumericFormat.EUROPEAN))
    }
}
