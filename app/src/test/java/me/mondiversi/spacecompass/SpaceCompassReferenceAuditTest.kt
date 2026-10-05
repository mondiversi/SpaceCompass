package me.mondiversi.spacecompass

import org.junit.Assert.*
import org.junit.Test

class SpaceCompassReferenceAuditTest {
    @Test fun pulsePeriodsNeverRoundToZeroAndMassesRetainQualifications() {
        for (format in SpaceCompassNumericFormat.entries) {
            val pulsar = spaceCompassCelestialFacts(SpaceCompassCelestialBody.PSR_J0437)
            val isolated = spaceCompassCelestialFacts(SpaceCompassCelestialBody.RX_J1856)
            assertEquals(formatSpaceCompassNumber(5.7574519367, 6, format, minimumDigits = 0) + " ms", formatSpaceCompassRotationPeriod(pulsar, format))
            assertEquals(formatSpaceCompassNumber(7.055, 3, format, minimumDigits = 0) + " s", formatSpaceCompassRotationPeriod(isolated, format))
            assertTrue(formatSpaceCompassCelestialMass(SpaceCompassCelestialBody.VOYAGER_1,
                spaceCompassCelestialFacts(SpaceCompassCelestialBody.VOYAGER_1), format).startsWith("≈ "))
        }
        assertEquals(22.72, spaceCompassCelestialFacts(SpaceCompassCelestialBody.PSR_J0437).diameterKm!!, 1e-9)
        assertEquals(1.26, spaceCompassCelestialFacts(SpaceCompassCelestialBody.PSR_J0437).diameterErrorMinusKm!!, 1e-9)
        assertEquals(1.90, spaceCompassCelestialFacts(SpaceCompassCelestialBody.PSR_J0437).diameterErrorPlusKm!!, 1e-9)
        assertNull(spaceCompassCelestialFacts(SpaceCompassCelestialBody.PSR_J0437).gravity)
        assertNull(spaceCompassCelestialFacts(SpaceCompassCelestialBody.STEPHENSON_2_18).massSolar)
    }
    @Test fun componentAndHorizonValuesNeverLeakIntoUnrelatedObjects() {
        val components = spaceCompassStellarComponents(SpaceCompassCelestialBody.ALPHA_CENTAURI)
        assertEquals(1.988, components.sumOf { it.massSolar }, 1e-12)
        assertEquals(listOf("A", "B"), components.map { it.name })
        assertTrue(spaceCompassStellarComponentTemperature(components[0]) + 273.15 in 5700.0..5900.0)
        assertTrue(spaceCompassStellarComponentTemperature(components[1]) + 273.15 in 5100.0..5300.0)
        val small = spaceCompassHorizonDiameterKm(SpaceCompassCelestialBody.SAGITTARIUS_A)!!
        val large = spaceCompassHorizonDiameterKm(SpaceCompassCelestialBody.TON_618)!!
        assertEquals(6.6e10 / 4.297e6, large / small, 1e-8)
        for (body in SpaceCompassCelestialBody.entries) {
            if (body != SpaceCompassCelestialBody.ALPHA_CENTAURI) assertTrue(spaceCompassStellarComponents(body).isEmpty())
            if (body !in setOf(SpaceCompassCelestialBody.SAGITTARIUS_A, SpaceCompassCelestialBody.TON_618, SpaceCompassCelestialBody.ANDROMEDA_CORE))
                assertNull(spaceCompassHorizonDiameterKm(body))
            val facts = spaceCompassCelestialFacts(body)
            for (value in listOf(facts.massSolar, facts.rotationSeconds, facts.binaryPeriodDays, facts.diameterKm))
                value?.let { assertTrue("$body: $it", it.isFinite() && it > 0) }
        }
    }
}
