package me.mondiversi.spacecompass

import java.io.File
import java.util.Locale
import javax.xml.parsers.DocumentBuilderFactory
import org.junit.Assert.*
import org.junit.Test

class SpaceCompassCelestialPointTranslationTest {
    private val root = File("src/main/res").takeIf { it.isDirectory } ?: File("app/src/main/res")
    private val directories = listOf("values", "values-it", "values-de", "values-fr", "values-es", "values-pt",
        "values-nl", "values-pl", "values-in", "values-ar", "values-fa", "values-iw", "values-el",
        "values-ru", "values-tr", "values-sw", "values-hi", "values-ja", "values-ko-rKR", "values-zh-rCN")
    private fun strings(directory: String, file: String): Map<String, String> {
        val elements = DocumentBuilderFactory.newInstance().apply {
            setFeature("http://apache.org/xml/features/disallow-doctype-decl", true)
        }.newDocumentBuilder().parse(File(root, "$directory/$file")).getElementsByTagName("string")
        return (0 until elements.length).associate { index ->
            val element = elements.item(index)
            element.attributes.getNamedItem("name").nodeValue to element.textContent
        }
    }

    @Test fun allTwentyLocalesSupplyNumberedAndLiveLabelsWithValidTimePlaceholders() {
        for (directory in directories) {
            val labels = strings(directory, "celestial_time.xml")
            val numbered = labels.getValue("celestial_point_number")
            val current = labels.getValue("celestial_current_position")
            val currentPoint = labels.getValue("celestial_point_current")
            val timed = labels.getValue("celestial_point_time")
            assertTrue(directory, numbered.contains("%1\$s"))
            assertFalse(directory, current.isBlank())
            assertFalse(directory, currentPoint.isBlank())
            if (directory == "values-it") assertEquals("Attuale", currentPoint)
            assertEquals("%1\$s · %2\$s", timed)
            assertTrue(String.format(Locale.ROOT, timed, String.format(Locale.ROOT, numbered, "24"), "23:00")
                .endsWith("24 · 23:00"))
            if (directory == "values-iw") {
                assertFalse(Regex("[\u0591-\u05BD\u05BF-\u05C7]").containsMatchIn(labels.values.joinToString()))
            }
        }
    }

    @Test fun specialNamesExistInEveryLocaleAndItalianMatchesTheRequestedWording() {
        for (directory in directories) {
            val events = strings(directory, "celestial.xml") + strings(directory, "sun_path.xml")
            for (key in listOf("celestial_rise", "sun_path_culmination", "celestial_set", "sun_path_minimum"))
                assertFalse("$directory/$key", events.getValue(key).isBlank())
            if (directory == "values-it") {
                assertEquals("Sorge", events.getValue("celestial_rise"))
                assertEquals("Culmine", events.getValue("sun_path_culmination"))
                assertEquals("Tramonta", events.getValue("celestial_set"))
                assertEquals("Minimo", events.getValue("sun_path_minimum"))
            }
        }
    }
}
