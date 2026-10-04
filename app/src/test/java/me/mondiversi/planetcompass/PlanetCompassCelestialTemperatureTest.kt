package me.mondiversi.planetcompass

import java.io.File
import javax.xml.parsers.DocumentBuilderFactory
import org.junit.Assert.*
import org.junit.Test

class PlanetCompassCelestialTemperatureTest {
    @Test fun referenceDataIsFiniteAboveAbsoluteZeroAndDoesNotInventSpacecraftOrSednaReadings() {
        PlanetCompassCelestialBody.entries.forEach { body ->
            val values = planetCompassCelestialTemperatures(body)
            if (body.isSpacecraft || body == PlanetCompassCelestialBody.SEDNA) assertTrue(values.isEmpty())
            else assertTrue("Missing reference temperature: $body", values.isNotEmpty())
            assertEquals(values.size, values.map { it.kind }.distinct().size)
            values.forEach {
                assertTrue(it.celsius.isFinite() && it.celsius >= -273.15)
                it.maximumCelsius?.let { maximum -> assertTrue(maximum.isFinite() && maximum >= it.celsius) }
            }
        }
    }

    @Test fun dayAndNightAreLocalSurfaceExtremesNotAveragesOrLunarPhaseTelemetry() {
        for ((body, day, night) in listOf(
            Triple(PlanetCompassCelestialBody.MERCURY, 430.0, -180.0),
            Triple(PlanetCompassCelestialBody.MOON, 127.0, -173.0))) {
            val values = planetCompassCelestialTemperatures(body)
            assertEquals(listOf(PlanetCompassCelestialTemperatureKind.DAY_MAXIMUM,
                PlanetCompassCelestialTemperatureKind.NIGHT_MINIMUM), values.map { it.kind })
            assertEquals(day, values[0].celsius, 0.0)
            assertEquals(night, values[1].celsius, 0.0)
        }
        assertEquals(PlanetCompassCelestialTemperatureKind.SURFACE_MEAN,
            planetCompassCelestialTemperatures(PlanetCompassCelestialBody.VENUS).single().kind)
        assertEquals(PlanetCompassCelestialTemperatureKind.SURFACE_RANGE,
            planetCompassCelestialTemperatures(PlanetCompassCelestialBody.EUROPA).single().kind)
    }

    @Test fun giantsAndStarsHaveExplicitPhysicalLayersRatherThanSolidSurfaceDayNightValues() {
        for ((body, temperature) in listOf(PlanetCompassCelestialBody.JUPITER to -110.0,
            PlanetCompassCelestialBody.SATURN to -140.0, PlanetCompassCelestialBody.URANUS to -195.0,
            PlanetCompassCelestialBody.NEPTUNE to -200.0)) {
            val value = planetCompassCelestialTemperatures(body).single()
            assertEquals(PlanetCompassCelestialTemperatureKind.ATMOSPHERE_ONE_BAR, value.kind)
            assertEquals(temperature, value.celsius, 0.0)
        }
        assertEquals(PlanetCompassCelestialTemperatureKind.PHOTOSPHERE,
            planetCompassCelestialTemperatures(PlanetCompassCelestialBody.SUN).single().kind)
        val polaris = planetCompassCelestialTemperatures(PlanetCompassCelestialBody.POLARIS).single()
        assertEquals(PlanetCompassCelestialTemperatureKind.STELLAR_EFFECTIVE, polaris.kind)
        assertEquals(6017.0, polaris.celsius + 273.15, 1e-9)
    }

    @Test fun formatterKeepsNegativeAndZeroTemperaturesAndRejectsNonphysicalData() {
        fun value(celsius: Double, max: Double? = null) = PlanetCompassCelestialTemperature(
            PlanetCompassCelestialTemperatureKind.SURFACE_RANGE, celsius, max)
        assertEquals("≈0 °C", formatPlanetCompassCelestialTemperature(value(0.0), PlanetCompassNumericFormat.INTERNATIONAL))
        assertEquals("≈-173 °C", formatPlanetCompassCelestialTemperature(value(-173.0), PlanetCompassNumericFormat.INTERNATIONAL))
        assertEquals("≈-223 … -133 °C", formatPlanetCompassCelestialTemperature(value(-223.0, -133.0), PlanetCompassNumericFormat.INTERNATIONAL))
        for (invalid in listOf(value(Double.NaN), value(Double.POSITIVE_INFINITY),
            value(-273.16), value(-100.0, Double.NaN), value(20.0, -100.0)))
            assertEquals("—", formatPlanetCompassCelestialTemperature(invalid, PlanetCompassNumericFormat.SYSTEM))
        val stellar = planetCompassCelestialTemperatures(PlanetCompassCelestialBody.POLARIS).single()
        for (numeric in PlanetCompassNumericFormat.entries) {
            val result = formatPlanetCompassCelestialTemperature(stellar, numeric)
            assertEquals("≈${formatPlanetCompassNumber(6017.0, 0, numeric)} K", result)
            assertFalse(result.contains("°K"))
        }
    }

    @Test fun allTwentyLocalesHaveTheSameCompleteTemperatureLabelsWithoutHebrewVowelMarks() {
        val folders = File("src/main/res").listFiles()!!.filter { File(it, "celestial_viewer.xml").isFile }
        assertEquals(20, folders.size)
        var referenceKeys: Set<String>? = null
        folders.forEach { folder ->
            val nodes = DocumentBuilderFactory.newInstance().newDocumentBuilder()
                .parse(File(folder, "celestial_temperatures.xml")).getElementsByTagName("string")
            val entries = (0 until nodes.length).map { nodes.item(it) }
            val keys = entries.map { it.attributes.getNamedItem("name").nodeValue }.toSet()
            assertEquals(9, keys.size)
            assertEquals(9, nodes.length)
            if (referenceKeys == null) referenceKeys = keys else assertEquals(referenceKeys, keys)
            entries.forEach {
                assertTrue(it.textContent.isNotBlank())
                if (folder.name == "values-iw")
                    assertFalse(it.textContent.any { c -> c in '\u0591'..'\u05BD' || c == '\u05BF' ||
                        c in '\u05C1'..'\u05C2' || c in '\u05C4'..'\u05C5' || c == '\u05C7' })
            }
        }
    }
}
