package me.mondiversi.spacecompass

import java.io.File
import javax.xml.parsers.DocumentBuilderFactory
import org.junit.Assert.*
import org.junit.Test

class SpaceCompassCelestialPointCaptionTest {
    private fun source(name: String) = File("src/main/java/me/mondiversi/spacecompass/$name").readText()

    @Test fun bothViewportRoutesPassTheBodyOfTheFocusedPointWithoutAssumingTheSun() {
        val badge = source("SpaceCompassCelestialTimeBadge.kt")
        assertTrue(badge.contains("pointName: String, body: SpaceCompassCelestialBody,"))
        assertTrue(badge.indexOf("Text(stringResource(body.nameResource)") < badge.indexOf("SpaceCompassPointTimeCaption("))
        assertTrue(source("SpaceCompassCelestialSceneInteraction.kt").contains("name, focused.body, excluded)"))
        assertTrue(source("SpaceCompassSunDailyPathLayer.kt").contains("path.body, excluded)"))
    }

    @Test fun tappedPanelKeepsOnlyCurrentOrPathCaptionInNormalWeight() {
        val panel = source("SpaceCompassSunPathSelectedPanel.kt")
        assertTrue(panel.contains("R.string.celestial_point_current) else labels.name(selected)"))
        assertTrue(panel.contains("SpaceCompassPointTimeCaption(pointName, labels.moment(selected)"))
        assertTrue(panel.contains("Modifier.testTag(\"celestial-selected-caption\")"))
        assertFalse(panel.contains("stringResource(body.nameResource)"))
        assertTrue(source("SpaceCompassPointTimeCaption.kt").contains("fontWeight = FontWeight.Normal"))
        assertFalse(source("SpaceCompassSunDailyPathControls.kt").contains("pointCaption"))
    }

    @Test fun allTwentyLocalesCanPrefixExistingTranslatedBodyAndPointNamesWithoutNewStrings() {
        val folders = File("src/main/res").listFiles()!!.filter { File(it, "celestial_time.xml").isFile }
        assertEquals(20, folders.size)
        for (folder in folders) {
            val nodes = DocumentBuilderFactory.newInstance().newDocumentBuilder()
                .parse(File(folder, "celestial_time.xml")).getElementsByTagName("string")
            val template = (0 until nodes.length).map { nodes.item(it) }
                .single { it.attributes.getNamedItem("name").nodeValue == "celestial_point_time" }.textContent
            for (name in listOf("Sole", "Luna", "Voyager 1", "Stella polare")) {
                val caption = String.format(template, name, "Punto 23 · 15:00")
                assertTrue(caption.startsWith(name))
                assertTrue(caption.endsWith("Punto 23 · 15:00"))
            }
        }
    }
}
