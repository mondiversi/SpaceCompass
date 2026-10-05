package me.mondiversi.spacecompass

import org.junit.Assert.*
import org.junit.Test

class SpaceCompassPressureUnitsTest {
    private val numeric = SpaceCompassNumericFormat.AMERICAN

    @Test fun standardPressureConversionsUseForceAndAreaRatherThanMassDensity() {
        assertEquals(6894.757293168361, SPACE_COMPASS_PASCALS_PER_PSI, 1e-9)
        assertEquals("1 psi", formatSpaceCompassPressure(SPACE_COMPASS_PASCALS_PER_PSI, numeric, SpaceCompassPressureUnit.PSI))
        assertEquals("1 bar", formatSpaceCompassPressure(100_000.0, numeric, SpaceCompassPressureUnit.BAR))
        assertEquals("100,000 Pa", formatSpaceCompassPressure(100_000.0, numeric, SpaceCompassPressureUnit.PASCAL))
        assertEquals("14.7 psi", formatSpaceCompassPressure(101_325.0, numeric, SpaceCompassPressureUnit.PSI))
    }

    @Test fun explicitAndAutomaticChoicesFollowRegionIndependentlyOfOtherUnits() {
        for (region in listOf("US", "us", "LR", "MM", "IT", "GB", "")) {
            val expected = if (region.uppercase() in setOf("US", "LR", "MM")) SpaceCompassPressureUnit.PSI else SpaceCompassPressureUnit.BAR
            for (automatic in listOf(null, "system", "invalid"))
                assertEquals(expected, spaceCompassResolvePressureUnit(region, automatic))
            for (unit in SpaceCompassPressureUnit.entries) for (mass in listOf("kg", "lb")) for (length in listOf("metric", "imperial")) {
                val saved = mapOf(SPACE_COMPASS_PRESSURE_UNIT_KEY to unit.storedValue,
                    SPACE_COMPASS_MASS_UNIT_KEY to mass, SPACE_COMPASS_DISTANCE_SPEED_UNIT_KEY to length)
                val resolved = spaceCompassResolveUnits(region, saved::get)
                assertEquals(unit, resolved.pressure)
                assertEquals(mass == "lb", resolved.pounds)
                assertEquals(length == "imperial", resolved.feet)
            }
        }
    }

    @Test fun pressureChoiceIsObservedAndRestoredWithoutChangingSelections() {
        val saved = mutableMapOf(SPACE_COMPASS_PRESSURE_UNIT_KEY to "bar", "celestial_active" to "VENUS", "language" to "it")
        val before = spaceCompassReadPresentationSettings(saved::get)
        saved[SPACE_COMPASS_PRESSURE_UNIT_KEY] = "psi"
        val after = spaceCompassReadPresentationSettings(saved::get)
        assertNotEquals(before, after)
        assertEquals("psi", after[SPACE_COMPASS_PRESSURE_UNIT_KEY])
        assertEquals(SpaceCompassPressureUnit.PSI, spaceCompassResolveUnits("IT", saved.toMap()::get).pressure)
        assertEquals("VENUS", saved["celestial_active"])
        assertEquals("it", after["language"])
    }

    @Test fun invalidOrUnavailablePressureIsOmittedRatherThanInventingZero() {
        for (value in listOf(0.0, -1.0, Double.NaN, Double.POSITIVE_INFINITY)) for (unit in SpaceCompassPressureUnit.entries) {
            assertNull(formatSpaceCompassPressure(value, numeric, unit))
            assertNull(formatSpaceCompassAtmosphericPressure(SpaceCompassAtmosphericPressure(value), numeric, unit))
        }
    }

    @Test fun surfaceReferenceFactsKeepTheirOriginalPublishedUnitScale() {
        assertEquals(9_200_000.0, spaceCompassAtmosphericPressure(SpaceCompassCelestialBody.VENUS)!!.pascals, 0.0)
        assertEquals(636.0, spaceCompassAtmosphericPressure(SpaceCompassCelestialBody.MARS)!!.pascals, 0.0)
        assertEquals(1.3, spaceCompassAtmosphericPressure(SpaceCompassCelestialBody.PLUTO)!!.pascals, 0.0)
        assertEquals(2e-4, spaceCompassAtmosphericPressure(SpaceCompassCelestialBody.IO)!!.pascals, 0.0)
        assertEquals(2e-5, spaceCompassAtmosphericPressure(SpaceCompassCelestialBody.EUROPA)!!.pascals, 0.0)
    }

    @Test fun onlyKnownAtmospheresAndExospheresHavePressureRows() {
        val known = setOf(SpaceCompassCelestialBody.VENUS, SpaceCompassCelestialBody.MARS,
            SpaceCompassCelestialBody.PLUTO, SpaceCompassCelestialBody.MOON, SpaceCompassCelestialBody.MERCURY,
            SpaceCompassCelestialBody.IO, SpaceCompassCelestialBody.EUROPA)
        assertEquals(known, SpaceCompassCelestialBody.entries.filter { spaceCompassAtmosphericPressure(it) != null }.toSet())
        // Gas giants have no defined solid-surface pressure; 1-bar temperature levels are not surface readings.
        for (body in listOf(SpaceCompassCelestialBody.JUPITER, SpaceCompassCelestialBody.SATURN,
            SpaceCompassCelestialBody.URANUS, SpaceCompassCelestialBody.NEPTUNE,
            SpaceCompassCelestialBody.ISS, SpaceCompassCelestialBody.SUN, SpaceCompassCelestialBody.EARTH_CENTER,
            SpaceCompassCelestialBody.TRAPPIST_1_E, SpaceCompassCelestialBody.TON_618))
            assertNull(spaceCompassAtmosphericPressure(body))
    }

    @Test fun mercuryUpperBoundLunarNightAndJovianConstituentsRemainQualified() {
        val mercury = spaceCompassAtmosphericPressure(SpaceCompassCelestialBody.MERCURY)!!
        val moon = spaceCompassAtmosphericPressure(SpaceCompassCelestialBody.MOON)!!
        assertEquals(SpaceCompassAtmosphericPressureKind.EXOSPHERE, mercury.kind)
        assertEquals("< ≈ 5 × 10⁻¹⁵ bar", formatSpaceCompassAtmosphericPressure(mercury, numeric, SpaceCompassPressureUnit.BAR))
        assertEquals(SpaceCompassAtmosphericPressureKind.NIGHT_EXOSPHERE, moon.kind)
        assertEquals("≈ 3 × 10⁻¹⁵ bar", formatSpaceCompassAtmosphericPressure(moon, numeric, SpaceCompassPressureUnit.BAR))
        assertEquals("SO₂", spaceCompassAtmosphericPressure(SpaceCompassCelestialBody.IO)!!.constituent)
        assertEquals("O₂", spaceCompassAtmosphericPressure(SpaceCompassCelestialBody.EUROPA)!!.constituent)
    }

    @Test fun tinyAtmospheresDoNotRoundToZeroInAnyPressureUnit() {
        for (body in listOf(SpaceCompassCelestialBody.MOON, SpaceCompassCelestialBody.MERCURY,
            SpaceCompassCelestialBody.IO, SpaceCompassCelestialBody.EUROPA))
            for (format in SpaceCompassNumericFormat.entries) for (unit in SpaceCompassPressureUnit.entries) {
                val text = formatSpaceCompassAtmosphericPressure(spaceCompassAtmosphericPressure(body)!!, format, unit)!!
                assertTrue(text.contains("10⁻"))
                assertFalse(text.startsWith("≈ 0 "))
                assertTrue(text.contains(" " + unit.symbol))
            }
    }

    @Test fun atmosphericOutputUsesChosenUnitsAndRegionalNumberConventions() {
        val mars = spaceCompassAtmosphericPressure(SpaceCompassCelestialBody.MARS)!!
        val venus = spaceCompassAtmosphericPressure(SpaceCompassCelestialBody.VENUS)!!
        val pluto = spaceCompassAtmosphericPressure(SpaceCompassCelestialBody.PLUTO)!!
        assertEquals("≈ 0.00636 bar", formatSpaceCompassAtmosphericPressure(mars, numeric, SpaceCompassPressureUnit.BAR))
        assertEquals("≈ 0,00636 bar", formatSpaceCompassAtmosphericPressure(mars, SpaceCompassNumericFormat.EUROPEAN, SpaceCompassPressureUnit.BAR))
        assertEquals("≈ 636 Pa", formatSpaceCompassAtmosphericPressure(mars, numeric, SpaceCompassPressureUnit.PASCAL))
        assertEquals("≈ 1.3 Pa", formatSpaceCompassAtmosphericPressure(pluto, numeric, SpaceCompassPressureUnit.PASCAL))
        assertEquals("≈ 9.2 × 10⁶ Pa", formatSpaceCompassAtmosphericPressure(venus, numeric, SpaceCompassPressureUnit.PASCAL))
        assertEquals("≈ 92 bar", formatSpaceCompassAtmosphericPressure(venus, numeric, SpaceCompassPressureUnit.BAR))
    }
}
