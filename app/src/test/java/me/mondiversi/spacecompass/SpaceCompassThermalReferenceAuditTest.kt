package me.mondiversi.spacecompass

import java.io.File
import java.util.Locale
import javax.xml.parsers.DocumentBuilderFactory
import org.junit.Assert.*
import org.junit.Test

class SpaceCompassThermalReferenceAuditTest {
    private val numeric = SpaceCompassNumericFormat.INTERNATIONAL
    private val day = SpaceCompassCatalogSortField.DAY_TEMPERATURE
    private val night = SpaceCompassCatalogSortField.NIGHT_TEMPERATURE

    @Test fun allTwentySixPhysicalObjectsWithSupportedTemperaturesAreAvailableWithoutNetwork() {
        val known = spaceCompassCelestialCatalogOrder.filter { spaceCompassCelestialTemperatures(it).isNotEmpty() }
        assertEquals(26, known.size)
        for (body in known) for (field in listOf(day, night)) {
            assertNotNull("$body / $field", spaceCompassCatalogPhysicalSortValue(body, field))
            assertNotEquals("—", formatSpaceCompassCatalogPhysicalValue(body, field, numeric, SpaceCompassUnits()) { "reference" })
        }
    }

    @Test fun sednaIsAnExplicitDiscoveryEraSurfaceEstimateNotMeasuredDayNightTelemetry() {
        val t = spaceCompassCelestialTemperatures(SpaceCompassCelestialBody.SEDNA).single()
        assertEquals(SpaceCompassCelestialTemperatureKind.SURFACE_ESTIMATE, t.kind)
        assertEquals(-240.0, t.celsius, 0.0)
        assertEquals(2004, t.epochYear)
        assertFalse(spaceCompassCatalogTemperatureReference(SpaceCompassCelestialBody.SEDNA, day)!!.exactDayNight)
        assertEquals("≈-240 °C (2004)", formatSpaceCompassCelestialTemperature(t, numeric))
    }

    @Test fun cometObservationsRetainTheirEncounterEpochsAndEndpoints() {
        data class Encounter(val body: SpaceCompassCelestialBody, val year: Int, val low: Double, val high: Double)
        for ((body, epoch, low, high) in listOf(
            Encounter(SpaceCompassCelestialBody.HALLEY, 1986, 300.0, 400.0),
            Encounter(SpaceCompassCelestialBody.COMET_67P, 2014, 205.0, 230.0))) {
            val t = spaceCompassCelestialTemperatures(body).single()
            assertEquals(SpaceCompassCelestialTemperatureKind.HISTORICAL_SURFACE_RANGE, t.kind)
            assertEquals(epoch, t.epochYear)
            assertEquals(low, t.celsius + 273.15, 1e-9)
            assertEquals(high, t.maximumCelsius!! + 273.15, 1e-9)
            assertTrue(formatSpaceCompassCelestialTemperature(t, numeric).endsWith("($epoch)"))
        }
    }

    @Test fun equilibriumModelKeepsUncertaintyAndConvertsAnIntervalWidthWithoutAddingThirtyTwo() {
        val t = spaceCompassCelestialTemperatures(SpaceCompassCelestialBody.TRAPPIST_1_E).single()
        assertEquals(SpaceCompassCelestialTemperatureKind.EQUILIBRIUM_MODEL, t.kind)
        assertEquals(251.3, t.celsius + 273.15, 1e-9)
        assertEquals(4.9, t.uncertaintyCelsius!!, 0.0)
        assertEquals("≈-22 ±5 °C", formatSpaceCompassCelestialTemperature(t, numeric))
        assertEquals("≈-7 ±9 °F", formatSpaceCompassCelestialTemperature(t, numeric, true))
        assertNull(spaceCompassAtmosphericPressure(SpaceCompassCelestialBody.TRAPPIST_1_E))
    }

    @Test fun compactStarsUseSpectralThermalModelsRatherThanPlanetaryExtrema() {
        val rx = spaceCompassCelestialTemperatures(SpaceCompassCelestialBody.RX_J1856).single()
        val pulsar = spaceCompassCelestialTemperatures(SpaceCompassCelestialBody.PSR_J0437).single()
        assertEquals(SpaceCompassCelestialTemperatureKind.THERMAL_MODEL, rx.kind)
        assertEquals(SpaceCompassCelestialTemperatureKind.THERMAL_MODEL, pulsar.kind)
        assertEquals(451415.754928, rx.celsius + 273.15, 10.0)
        assertEquals(724121.930785, rx.maximumCelsius!! + 273.15, 10.0)
        assertEquals(125000.0, pulsar.celsius + 273.15, 1e-9)
        assertEquals(350000.0, pulsar.maximumCelsius!! + 273.15, 1e-9)
        for (body in listOf(SpaceCompassCelestialBody.RX_J1856, SpaceCompassCelestialBody.PSR_J0437))
            assertFalse(spaceCompassCatalogTemperatureReference(body, night)!!.exactDayNight)
    }

    @Test fun alphaCentauriRetainsBothComponentsAndTheCatalogLabelsItsStableRepresentative() {
        val values = spaceCompassCelestialTemperatures(SpaceCompassCelestialBody.ALPHA_CENTAURI)
        assertEquals(listOf("A", "B"), values.map { it.component })
        assertTrue(values[0].celsius + 273.15 in 5700.0..5900.0)
        assertTrue(values[1].celsius + 273.15 in 5100.0..5300.0)
        val value = formatSpaceCompassCatalogPhysicalValue(SpaceCompassCelestialBody.ALPHA_CENTAURI,
            day, numeric, SpaceCompassUnits()) { "Effective" }
        assertTrue(value.endsWith("\nEffective (A)"))
        assertEquals(values[0].celsius, spaceCompassCatalogPhysicalSortValue(SpaceCompassCelestialBody.ALPHA_CENTAURI, day)!!, 0.0)
    }

    @Test fun fallbackRangesSortByOneMidpointRatherThanInventingDayAndNightExtremes() {
        val body = SpaceCompassCelestialBody.EUROPA
        assertEquals(-178.0, spaceCompassCatalogPhysicalSortValue(body, day)!!, 0.0)
        assertEquals(-178.0, spaceCompassCatalogPhysicalSortValue(body, night)!!, 0.0)
        assertEquals("≈-223 … -133 °C\nSurface range",
            formatSpaceCompassCatalogPhysicalValue(body, night, numeric, SpaceCompassUnits()) { "Surface range" })
        val bodies = listOf(SpaceCompassCelestialBody.SAGITTARIUS_A, body, SpaceCompassCelestialBody.VENUS)
        assertEquals(listOf(body, SpaceCompassCelestialBody.VENUS, bodies[0]),
            spaceCompassSortCatalog(bodies, SpaceCompassCatalogSort.DAY_TEMPERATURE_ASC, emptyMap(), emptyMap(), Locale.ROOT))
        assertEquals(listOf(SpaceCompassCelestialBody.VENUS, body, bodies[0]),
            spaceCompassSortCatalog(bodies, SpaceCompassCatalogSort.DAY_TEMPERATURE_DESC, emptyMap(), emptyMap(), Locale.ROOT))
    }

    @Test fun stephensonDiameterFollowsItsLuminosityAndTemperatureModelWithoutInventingCurrentMass() {
        val facts = spaceCompassCelestialFacts(SpaceCompassCelestialBody.STEPHENSON_2_18)
        val t = spaceCompassCelestialTemperatures(SpaceCompassCelestialBody.STEPHENSON_2_18).single()
        assertEquals(3200.0, t.celsius + 273.15, 1e-9)
        assertEquals(321000.0, facts.luminositySolar!!, 0.0)
        val radiusSolar = facts.diameterKm!! / (2 * 695700)
        assertEquals(facts.luminositySolar!!, radiusSolar * radiusSolar * Math.pow(3200.0 / 5772.0, 4.0), 1e-8)
        assertTrue(facts.diameterEstimated)
        assertTrue(facts.diameterErrorMinusKm!! > facts.diameterErrorPlusKm!!)
        assertNull(facts.massSolar)
        assertNull(facts.massKg)
    }

    @Test fun invalidUncertaintiesCannotBeFormattedAsPhysicalData() {
        for (bad in listOf(-1.0, Double.NaN, Double.POSITIVE_INFINITY))
            assertEquals("—", formatSpaceCompassCelestialTemperature(SpaceCompassCelestialTemperature(
                SpaceCompassCelestialTemperatureKind.EQUILIBRIUM_MODEL, -22.0, uncertaintyCelsius = bad), numeric))
    }

    @Test fun allTwentyLanguagesContainTheSixQualifiedReferenceLabelsAndNotes() {
        val folders = File("src/main/res").listFiles()!!.filter { File(it, "common.xml").isFile }
        assertEquals(20, folders.size)
        var keys: Set<String>? = null
        for (folder in folders) {
            val nodes = DocumentBuilderFactory.newInstance().newDocumentBuilder()
                .parse(File(folder, "celestial_temperature_references.xml")).getElementsByTagName("string")
            val entries = (0 until nodes.length).map { nodes.item(it) }
            val actual = entries.map { it.attributes.getNamedItem("name").nodeValue }.toSet()
            assertEquals(6, actual.size)
            assertEquals(6, nodes.length)
            if (keys == null) keys = actual else assertEquals(keys, actual)
            for (entry in entries) assertTrue(entry.textContent.isNotBlank())
        }
    }
}
