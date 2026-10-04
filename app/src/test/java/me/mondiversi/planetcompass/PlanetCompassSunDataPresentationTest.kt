package me.mondiversi.planetcompass

import java.io.File
import javax.xml.parsers.DocumentBuilderFactory
import org.junit.Assert.*
import org.junit.Test

class PlanetCompassSunDataPresentationTest {
    @Test fun labelAndNumericValueAreSeparatedWithoutLosingTheSpokenSentence() {
        val row = planetCompassSunDataRow("Azimut del sole: $PLANET_COMPASS_SUN_DATA_MARKER", "194,3°", "azimuth")
        assertEquals("Azimut del sole", row.label)
        assertEquals("194,3°", row.value)
        assertEquals("Azimut del sole: 194,3°", row.announcement)
        assertEquals("azimuth", row.tag)
    }

    @Test fun frenchFullWidthAndRtlPunctuationArePreservedForAccessibility() {
        for ((label, template) in listOf("Azimut solaire" to "Azimut solaire : $PLANET_COMPASS_SUN_DATA_MARKER",
            "太陽の方位角" to "太陽の方位角：$PLANET_COMPASS_SUN_DATA_MARKER",
            "אזימוט השמש" to "אזימוט השמש: $PLANET_COMPASS_SUN_DATA_MARKER")) {
            val row = planetCompassSunDataRow(template, "—", "azimuth")
            assertEquals(label, row.label); assertEquals("—", row.value)
            assertEquals(template.replace(PLANET_COMPASS_SUN_DATA_MARKER, "—"), row.announcement)
        }
    }

    @Test fun accuracySignStaysWithTheNumberAndAltitudeAccuracyIsNotDiscarded() {
        val accuracy = planetCompassSunDataRow("Precisione posizione: ±$PLANET_COMPASS_SUN_DATA_MARKER", "8 m", "accuracy")
        assertEquals("Precisione posizione", accuracy.label); assertEquals("±8 m", accuracy.value)
        assertEquals("Precisione posizione: ±8 m", accuracy.announcement)
        val altitude = planetCompassSunDataRow("Quota GPS: $PLANET_COMPASS_SUN_DATA_MARKER", "1.830 m (±12 m)", "altitude")
        assertEquals("1.830 m (±12 m)", altitude.value)
    }

    @Test fun unavailableSpeedDoesNotRetainAUnitSuffixOrInventAValue() {
        val template = "Velocità orbitale della Terra: $PLANET_COMPASS_SUN_DATA_MARKER km/s"
        val unavailable = planetCompassSunOptionalDataRow(template, null, "speed")
        assertEquals("Velocità orbitale della Terra", unavailable.label)
        assertEquals("—", unavailable.value)
        assertEquals("Velocità orbitale della Terra: —", unavailable.announcement)
        val known = planetCompassSunOptionalDataRow(template, "29,77", "speed")
        assertEquals("29,77 km/s", known.value)
    }

    @Test fun allTwentyLanguagesHaveCompleteCompatibleTableTemplatesAndInfoLabel() {
        val files = File("src/main/res").listFiles()!!.map { File(it, "sun_finder.xml") }.filter { it.isFile }
        assertEquals(20, files.size)
        val keys = listOf("heading", "tilt", "azimuth", "elevation", "altitude", "location_accuracy")
        for (file in files) {
            val strings = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(file).getElementsByTagName("string")
            val map = (0 until strings.length).associate { index ->
                val node = strings.item(index)
                node.attributes.getNamedItem("name").nodeValue to node.textContent
            }
            assertFalse(file.path, map.getValue("sun_finder_info_action").isBlank())
            for (key in keys) {
                val template = map.getValue("sun_finder_$key").replace("%1\$s", PLANET_COMPASS_SUN_DATA_MARKER)
                val row = planetCompassSunDataRow(template, "8 m", key)
                assertFalse(file.path + key, row.label.isBlank())
                assertTrue(file.path + key, row.value.endsWith("8 m"))
                assertFalse(row.label.endsWith(':') || row.label.endsWith('：') || row.label.endsWith('±'))
            }
        }
    }
}
