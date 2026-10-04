package me.mondiversi.planetcompass

import java.io.File
import javax.xml.parsers.DocumentBuilderFactory
import org.junit.Assert.*
import org.junit.Test

class PlanetCompassCelestialPointCaptionTest {
    private fun source(name: String) = File("src/main/java/me/mondiversi/planetcompass/$name").readText()

    @Test fun bothViewportRoutesPassTheBodyOfTheFocusedPointWithoutAssumingTheSun() {
        val badge = source("PlanetCompassCelestialTimeBadge.kt")
        assertTrue(badge.contains("pointName: String, body: PlanetCompassCelestialBody,"))
        assertTrue(badge.indexOf("Text(stringResource(body.nameResource)") < badge.indexOf("Text(caption"))
        assertTrue(source("PlanetCompassCelestialSceneInteraction.kt").contains("name, focused.body, excluded)"))
        assertTrue(source("PlanetCompassSunDailyPathLayer.kt").contains("path.body, excluded)"))
    }

    @Test fun tappedPanelPrefixesTheBodyToBothCurrentAndPathCaptionsInNormalWeight() {
        val panel = source("PlanetCompassSunPathSelectedPanel.kt")
        assertTrue(panel.contains("labels.current(selected) else labels.point(selected)"))
        assertTrue(panel.contains("R.string.celestial_point_time, stringResource(body.nameResource), pointCaption"))
        assertTrue(panel.contains("fontWeight = FontWeight.Normal"))
        assertFalse(source("PlanetCompassSunDailyPathControls.kt").contains("pointCaption"))
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
