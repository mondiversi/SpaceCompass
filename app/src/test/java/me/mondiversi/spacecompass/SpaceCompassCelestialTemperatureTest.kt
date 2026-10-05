package me.mondiversi.spacecompass

import java.io.File
import javax.xml.parsers.DocumentBuilderFactory
import org.junit.Assert.*
import org.junit.Test

class SpaceCompassCelestialTemperatureTest {
    @Test fun referenceDataIsFiniteAboveAbsoluteZeroAndDoesNotInventSpacecraftOrSednaReadings() {
        SpaceCompassCelestialBody.entries.forEach { body ->
            val values = spaceCompassCelestialTemperatures(body)
            if (body.isSpacecraft || body == SpaceCompassCelestialBody.EARTH_CENTER || body == SpaceCompassCelestialBody.SEDNA || (body.deepSkyReference != null && body !in setOf(SpaceCompassCelestialBody.PROXIMA_CENTAURI, SpaceCompassCelestialBody.RIGEL))) assertTrue(values.isEmpty())
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
            Triple(SpaceCompassCelestialBody.MERCURY, 430.0, -180.0),
            Triple(SpaceCompassCelestialBody.MOON, 127.0, -173.0))) {
            val values = spaceCompassCelestialTemperatures(body)
            assertEquals(listOf(SpaceCompassCelestialTemperatureKind.DAY_MAXIMUM,
                SpaceCompassCelestialTemperatureKind.NIGHT_MINIMUM), values.map { it.kind })
            assertEquals(day, values[0].celsius, 0.0)
            assertEquals(night, values[1].celsius, 0.0)
        }
        assertEquals(SpaceCompassCelestialTemperatureKind.SURFACE_MEAN,
            spaceCompassCelestialTemperatures(SpaceCompassCelestialBody.VENUS).single().kind)
        assertEquals(SpaceCompassCelestialTemperatureKind.SURFACE_RANGE,
            spaceCompassCelestialTemperatures(SpaceCompassCelestialBody.EUROPA).single().kind)
    }

    @Test fun giantsAndStarsHaveExplicitPhysicalLayersRatherThanSolidSurfaceDayNightValues() {
        for ((body, temperature) in listOf(SpaceCompassCelestialBody.JUPITER to -110.0,
            SpaceCompassCelestialBody.SATURN to -140.0, SpaceCompassCelestialBody.URANUS to -195.0,
            SpaceCompassCelestialBody.NEPTUNE to -200.0)) {
            val value = spaceCompassCelestialTemperatures(body).single()
            assertEquals(SpaceCompassCelestialTemperatureKind.ATMOSPHERE_ONE_BAR, value.kind)
            assertEquals(temperature, value.celsius, 0.0)
        }
        assertEquals(SpaceCompassCelestialTemperatureKind.PHOTOSPHERE,
            spaceCompassCelestialTemperatures(SpaceCompassCelestialBody.SUN).single().kind)
        for ((body, kelvin) in listOf(SpaceCompassCelestialBody.PROXIMA_CENTAURI to 2900.0,
            SpaceCompassCelestialBody.RIGEL to 12100.0)) {
            val stellar = spaceCompassCelestialTemperatures(body).single()
            assertEquals(SpaceCompassCelestialTemperatureKind.STELLAR_EFFECTIVE, stellar.kind)
            assertEquals(kelvin, stellar.celsius + 273.15, 1e-9)
        }
        val polaris = spaceCompassCelestialTemperatures(SpaceCompassCelestialBody.POLARIS).single()
        assertEquals(SpaceCompassCelestialTemperatureKind.STELLAR_EFFECTIVE, polaris.kind)
        assertEquals(6017.0, polaris.celsius + 273.15, 1e-9)
    }

    @Test fun formatterKeepsNegativeAndZeroTemperaturesAndRejectsNonphysicalData() {
        fun value(celsius: Double, max: Double? = null) = SpaceCompassCelestialTemperature(
            SpaceCompassCelestialTemperatureKind.SURFACE_RANGE, celsius, max)
        assertEquals("≈0 °C", formatSpaceCompassCelestialTemperature(value(0.0), SpaceCompassNumericFormat.INTERNATIONAL))
        assertEquals("≈-173 °C", formatSpaceCompassCelestialTemperature(value(-173.0), SpaceCompassNumericFormat.INTERNATIONAL))
        assertEquals("≈-223 … -133 °C", formatSpaceCompassCelestialTemperature(value(-223.0, -133.0), SpaceCompassNumericFormat.INTERNATIONAL))
        for (invalid in listOf(value(Double.NaN), value(Double.POSITIVE_INFINITY),
            value(-273.16), value(-100.0, Double.NaN), value(20.0, -100.0)))
            assertEquals("—", formatSpaceCompassCelestialTemperature(invalid, SpaceCompassNumericFormat.SYSTEM))
        val stellar = spaceCompassCelestialTemperatures(SpaceCompassCelestialBody.POLARIS).single()
        for (numeric in SpaceCompassNumericFormat.entries) {
            val result = formatSpaceCompassCelestialTemperature(stellar, numeric)
            assertEquals("≈${formatSpaceCompassNumber(stellar.celsius, 0, numeric)} °C (${formatSpaceCompassNumber(6017.0, 0, numeric)} K)", result)
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
