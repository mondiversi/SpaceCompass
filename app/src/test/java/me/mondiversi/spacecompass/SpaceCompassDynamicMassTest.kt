package me.mondiversi.spacecompass

import org.junit.Assert.*
import org.junit.Test

class SpaceCompassDynamicMassTest {
    private val numeric = SpaceCompassNumericFormat.AMERICAN

    @Test fun exactTenSolarMassBoundaryIsIndependentOfBodyAndPoundChoice() {
        val solarBoundary = 10.0
        val kilogramBoundary = 10.0 * SPACE_COMPASS_SOLAR_MASS_KG
        for (reference in listOf(
            SpaceCompassCelestialFacts(massSolar = Math.nextDown(solarBoundary)) to false,
            SpaceCompassCelestialFacts(massSolar = solarBoundary) to true,
            SpaceCompassCelestialFacts(massSolar = Math.nextUp(solarBoundary)) to true,
            SpaceCompassCelestialFacts(massKg = Math.nextDown(kilogramBoundary)) to false,
            SpaceCompassCelestialFacts(massKg = kilogramBoundary) to true,
            SpaceCompassCelestialFacts(massKg = Math.nextUp(kilogramBoundary)) to true)) {
            assertEquals(reference.second, spaceCompassUsesSolarMass(reference.first))
            for (body in listOf(SpaceCompassCelestialBody.SUN, SpaceCompassCelestialBody.RIGEL,
                SpaceCompassCelestialBody.TON_618, SpaceCompassCelestialBody.ISS)) for (pounds in listOf(false, true)) {
                val result = formatSpaceCompassCelestialMass(body, reference.first, numeric, pounds)
                assertTrue(result.endsWith(if (reference.second) " M☉" else if (pounds) " lb" else " kg"))
            }
        }
    }

    @Test fun sunUsesSelectedMassUnitsAndLocalizedScientificNumber() {
        val sun = spaceCompassCelestialFacts(SpaceCompassCelestialBody.SUN)
        assertEquals("1.988 × 10³⁰ kg", formatSpaceCompassCelestialMass(SpaceCompassCelestialBody.SUN, sun, numeric))
        assertEquals("4.384 × 10³⁰ lb", formatSpaceCompassCelestialMass(SpaceCompassCelestialBody.SUN, sun, numeric, true))
        assertEquals("4,384 × 10³⁰ lb", formatSpaceCompassCelestialMass(SpaceCompassCelestialBody.SUN, sun, SpaceCompassNumericFormat.EUROPEAN, true))
    }

    @Test fun lowerStellarAndCompactMassesAllFollowKilogramsOrPounds() {
        for (body in listOf(SpaceCompassCelestialBody.SUN, SpaceCompassCelestialBody.PROXIMA_CENTAURI,
            SpaceCompassCelestialBody.POLARIS, SpaceCompassCelestialBody.ALPHA_CENTAURI,
            SpaceCompassCelestialBody.RX_J1856, SpaceCompassCelestialBody.PSR_J0437)) {
            val facts = spaceCompassCelestialFacts(body)
            assertFalse(spaceCompassUsesSolarMass(facts))
            for (pounds in listOf(false, true)) {
                val text = formatSpaceCompassCelestialMass(body, facts, numeric, pounds)
                assertTrue(text.contains(if (pounds) " lb" else " kg"))
                assertFalse(text.contains("M☉"))
                assertNotEquals("—", text)
            }
        }
    }

    @Test fun lowerMassUncertaintiesConvertAlongWithTheValueWithoutLosingQualifiers() {
        val pulsar = spaceCompassCelestialFacts(SpaceCompassCelestialBody.PSR_J0437)
        assertEquals("(2.820 ± 0.087) × 10³⁰ kg", formatSpaceCompassCelestialMass(SpaceCompassCelestialBody.PSR_J0437, pulsar, numeric))
        assertEquals("(6.216 ± 0.193) × 10³⁰ lb", formatSpaceCompassCelestialMass(SpaceCompassCelestialBody.PSR_J0437, pulsar, numeric, true))
        val proxima = spaceCompassCelestialFacts(SpaceCompassCelestialBody.PROXIMA_CENTAURI)
        assertEquals("≈ (2.428 ± 0.044) × 10²⁹ kg", formatSpaceCompassCelestialMass(SpaceCompassCelestialBody.PROXIMA_CENTAURI, proxima, numeric))
        val model = spaceCompassCelestialFacts(SpaceCompassCelestialBody.RX_J1856)
        assertEquals("† 2.784 × 10³⁰ kg", formatSpaceCompassCelestialMass(SpaceCompassCelestialBody.RX_J1856, model, numeric))
    }

    @Test fun resolvedBinaryTotalAndComponentsEachUseTheirOwnMassThreshold() {
        val binary = SpaceCompassCelestialBody.ALPHA_CENTAURI
        assertEquals("3.953 × 10³⁰ kg (A+B)", formatSpaceCompassCelestialMass(binary, spaceCompassCelestialFacts(binary), numeric))
        val component = spaceCompassStellarComponents(binary).first()
        val facts = SpaceCompassCelestialFacts(massSolar = component.massSolar, massSolarError = component.massError)
        assertEquals("(2.145 ± 0.0058) × 10³⁰ kg", formatSpaceCompassCelestialMass(SpaceCompassCelestialBody.SUN, facts, numeric))
        assertTrue(formatSpaceCompassCelestialMass(SpaceCompassCelestialBody.SUN, facts, numeric, true).endsWith(" lb"))
        assertFalse(formatSpaceCompassCelestialMass(SpaceCompassCelestialBody.SUN, facts, numeric, true).contains("M☉"))
        // A large binary total must not force each of its lower-mass components to use solar masses.
        assertTrue(spaceCompassUsesSolarMass(SpaceCompassCelestialFacts(massSolar = 12.0)))
        assertFalse(spaceCompassUsesSolarMass(SpaceCompassCelestialFacts(massSolar = 6.0)))
    }

    @Test fun highMassReferencesRemainSolarWithTheirUncertaintiesAndEstimates() {
        val rigel = spaceCompassCelestialFacts(SpaceCompassCelestialBody.RIGEL)
        for (pounds in listOf(false, true))
            assertEquals("≈ 21 ± 3 M☉", formatSpaceCompassCelestialMass(SpaceCompassCelestialBody.RIGEL, rigel, numeric, pounds))
        for (body in listOf(SpaceCompassCelestialBody.SAGITTARIUS_A,
            SpaceCompassCelestialBody.ANDROMEDA_CORE, SpaceCompassCelestialBody.TON_618)) {
            val facts = spaceCompassCelestialFacts(body)
            assertTrue(spaceCompassUsesSolarMass(facts))
            assertEquals(formatSpaceCompassCelestialMass(body, facts, numeric), formatSpaceCompassCelestialMass(body, facts, numeric, true))
        }
    }

    @Test fun unknownMassDoesNotAssignAUnitOrInventAThresholdAndInvalidDataDoesNotFallBack() {
        val body = SpaceCompassCelestialBody.STEPHENSON_2_18
        assertFalse(spaceCompassUsesSolarMass(spaceCompassCelestialFacts(body)))
        for (pounds in listOf(false, true))
            assertEquals("—", formatSpaceCompassCelestialMass(body, spaceCompassCelestialFacts(body), numeric, pounds))
        for (invalid in listOf(0.0, -1.0, Double.NaN, Double.POSITIVE_INFINITY)) {
            val facts = SpaceCompassCelestialFacts(massSolar = invalid, massKg = 20 * SPACE_COMPASS_SOLAR_MASS_KG)
            assertFalse(spaceCompassUsesSolarMass(facts))
            assertNull(spaceCompassCelestialMassKilograms(facts))
            assertEquals("—", formatSpaceCompassCelestialMass(body, facts, numeric))
        }
    }
}
