package me.mondiversi.spacecompass

import java.io.File
import javax.xml.parsers.DocumentBuilderFactory
import org.junit.Assert.*
import org.junit.Test

class SpaceCompassAppPresentationTest {
    @Test fun tabletAndPhoneDimensionsScaleWithoutShrinkingSmallScreens() {
        assertEquals(1f, spaceCompassAutomaticScreenScale(320), 0f)
        assertEquals(1f, spaceCompassAutomaticScreenScale(400), 0f)
        assertEquals(2f, spaceCompassAutomaticScreenScale(800), 0f)
        assertEquals(2.5f, spaceCompassAutomaticScreenScale(1200), 0f)
    }

    @Test fun everyCatalogIncludesAllNewInfoAndSettingsText() {
        val folders = File("src/main/res").listFiles()!!.filter { File(it, "common.xml").isFile }
        assertEquals(20, folders.size)
        fun entries(folder: File, name: String): Map<String, String> {
            val nodes = DocumentBuilderFactory.newInstance().newDocumentBuilder()
                .parse(File(folder, name)).getElementsByTagName("string")
            return (0 until nodes.length).associate { index ->
                val node = nodes.item(index)
                node.attributes.getNamedItem("name").nodeValue to node.textContent
            }
        }
        val base = folders.single { it.name == "values" }
        for (name in listOf("app_pages.xml", "settings.xml")) {
            val expected = entries(base, name).keys
            for (folder in folders) {
                val text = entries(folder, name)
                assertEquals("${folder.name}/$name", expected, text.keys)
                assertTrue("${folder.name}/$name contains an empty translation", text.values.none { it.isBlank() })
                if (name == "app_pages.xml") assertTrue(text.getValue("pc_info_earth").endsWith("🙂"))
            }
        }
    }
}
