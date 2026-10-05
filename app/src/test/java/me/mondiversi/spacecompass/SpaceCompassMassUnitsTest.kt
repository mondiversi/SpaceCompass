package me.mondiversi.spacecompass

import org.junit.Assert.*
import org.junit.Test

class SpaceCompassMassUnitsTest {
    private val numeric = SpaceCompassNumericFormat.AMERICAN

    @Test fun internationalPoundUsesExactDefinitionRatherThanARoundedFactor() {
        assertEquals(1.0, spaceCompassMassForDisplay(0.45359237, true), 1e-12)
        assertEquals(2.2046226218487757, spaceCompassMassForDisplay(1.0, true), 1e-12)
        assertEquals(1.0, spaceCompassMassForDisplay(1.0, false), 0.0)
    }

    @Test fun explicitMassChoiceOverridesRegionAndRemainsIndependentOfLength() {
        for (region in listOf("US", "IT")) for (length in listOf("metric", "imperial"))
            for (mass in listOf("kg", "lb")) {
                val saved = mapOf(SPACE_COMPASS_DISTANCE_SPEED_UNIT_KEY to length, SPACE_COMPASS_MASS_UNIT_KEY to mass)
                val units = spaceCompassResolveUnits(region, saved::get)
                assertEquals(mass == "lb", units.pounds)
                assertEquals(length == "imperial", units.feet)
            }
    }

    @Test fun automaticMassFollowsDeviceRegionIncludingMissingAndInvalidPreferences() {
        for (choice in listOf(null, "system", "invalid")) for (region in listOf("US", "us", "LR", "MM", "IT", "GB", "")) {
            val units = spaceCompassResolveUnits(region) { if (it == SPACE_COMPASS_MASS_UNIT_KEY) choice else null }
            assertEquals(region.uppercase() in setOf("US", "LR", "MM"), units.pounds)
        }
    }

    @Test fun massChangesParticipateInPresentationSnapshotsAndSurviveReload() {
        val saved = mutableMapOf(SPACE_COMPASS_MASS_UNIT_KEY to "kg", "language" to "it")
        val before = spaceCompassReadPresentationSettings(saved::get)
        saved[SPACE_COMPASS_MASS_UNIT_KEY] = "lb"
        val after = spaceCompassReadPresentationSettings(saved::get)
        assertNotEquals(before, after)
        assertEquals("lb", after[SPACE_COMPASS_MASS_UNIT_KEY])
        val reloaded = saved.toMap()
        assertTrue(spaceCompassResolveUnits("IT", reloaded::get).pounds)
        assertEquals("it", after["language"])
    }

    @Test fun massReadoutsWithoutUncertaintyConvertAndKeepApproximationAndNumericConventions() {
        for (format in SpaceCompassNumericFormat.entries) for (body in SpaceCompassCelestialBody.entries) {
            val facts = spaceCompassCelestialFacts(body)
            if (spaceCompassUsesSolarMass(facts) || facts.massKg == null || facts.massSolarError != null) continue
            val prefix = if (facts.massEstimated) "≈ " else ""
            assertEquals(prefix + formatSpaceCompassScientificNumber(facts.massKg / 0.45359237, format) + " lb",
                formatSpaceCompassCelestialMass(body, facts, format, pounds = true))
            assertEquals(prefix + formatSpaceCompassScientificNumber(facts.massKg, format) + " kg",
                formatSpaceCompassCelestialMass(body, facts, format, pounds = false))
        }
        assertEquals("1 lb", formatSpaceCompassCelestialMass(SpaceCompassCelestialBody.ISS,
            SpaceCompassCelestialFacts(massKg = 0.45359237), numeric, pounds = true))
    }

    @Test fun largeSolarMassUnitsAndUncertaintiesAreUnchangedByPoundSelection() {
        for (body in SpaceCompassCelestialBody.entries) for (format in SpaceCompassNumericFormat.entries) {
            val facts = spaceCompassCelestialFacts(body)
            if (!spaceCompassUsesSolarMass(facts)) continue
            assertEquals(formatSpaceCompassCelestialMass(body, facts, format),
                formatSpaceCompassCelestialMass(body, facts, format, pounds = true))
        }
    }

    @Test fun missingAndInvalidMassOrDensityValuesRemainUnavailable() {
        for (value in listOf(null, 0.0, -1.0, Double.NaN, Double.POSITIVE_INFINITY)) for (pounds in listOf(false, true)) {
            assertEquals("—", formatSpaceCompassCelestialMass(SpaceCompassCelestialBody.ISS,
                SpaceCompassCelestialFacts(massKg = value), numeric, pounds))
            assertEquals("—", formatSpaceCompassCelestialDensity(value, numeric, SpaceCompassUnits(pounds = pounds)))
        }
    }

    @Test fun densityUsesBothIndependentUnitsWithCorrectCubicVolumeConversion() {
        assertEquals("1 kg/m³", formatSpaceCompassCelestialDensity(1.0, numeric, SpaceCompassUnits()))
        assertEquals("1 lb/m³", formatSpaceCompassCelestialDensity(0.45359237, numeric, SpaceCompassUnits(pounds = true)))
        assertEquals("1.0 kg/ft³", formatSpaceCompassCelestialDensity(35.31466672148859, numeric, SpaceCompassUnits(feet = true)))
        assertEquals("1.0 lb/ft³", formatSpaceCompassCelestialDensity(16.01846337396014, numeric, SpaceCompassUnits(feet = true, pounds = true)))
        assertEquals("87.9 lb/ft³", formatSpaceCompassCelestialDensity(1408.0, numeric, SpaceCompassUnits(feet = true, pounds = true)))
        assertEquals("87,9 lb/ft³", formatSpaceCompassCelestialDensity(1408.0, SpaceCompassNumericFormat.EUROPEAN,
            SpaceCompassUnits(feet = true, pounds = true)))
        for (feet in listOf(false, true)) for (pounds in listOf(false, true)) {
            val polaris = formatSpaceCompassCelestialDensity(spaceCompassCelestialFacts(SpaceCompassCelestialBody.POLARIS).density,
                numeric, SpaceCompassUnits(feet = feet, pounds = pounds), 3)
            assertFalse(polaris.startsWith("0.000 "))
        }
    }
}
