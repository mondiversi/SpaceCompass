package me.mondiversi.spacecompass

import java.io.File
import javax.xml.parsers.DocumentBuilderFactory
import org.junit.Assert.*
import org.junit.Test

class SpaceCompassStarlinkTranslationTest {
    @Test fun satelliteWarningAndIdentityNoteExistInAllTwentyLocales() {
        val root = File("src/main/res").takeIf { it.isDirectory } ?: File("app/src/main/res")
        val directories = listOf("values","values-it","values-de","values-fr","values-es","values-pt",
            "values-nl","values-pl","values-in","values-ar","values-fa","values-iw","values-el",
            "values-ru","values-tr","values-sw","values-hi","values-ja","values-ko-rKR","values-zh-rCN")
        val parser = DocumentBuilderFactory.newInstance().apply {
            setFeature("http://apache.org/xml/features/disallow-doctype-decl",true)
        }.newDocumentBuilder()
        for (directory in directories) {
            val elements = parser.parse(File(root,"$directory/starlink.xml")).getElementsByTagName("string")
            val values = (0 until elements.length).associate { i ->
                elements.item(i).attributes.getNamedItem("name").nodeValue to elements.item(i).textContent
            }
            assertEquals(directory,2,values.size)
            val warning = values.getValue("celestial_satellite_old")
            assertFalse("Generic warning must not require an object name: $directory", warning.contains("%"))
            assertTrue("Warning has a localized sentence ending: $directory", warning.last() in ".。।۔")
            assertTrue(values.getValue("celestial_starlink_note").contains("STARLINK-40083 · NORAD 100855 · 2026-225A"))
            assertTrue(values.getValue("celestial_starlink_note").contains("CelesTrak"))
            if (directory == "values-iw") assertFalse(Regex("[\\u0591-\\u05BD\\u05BF-\\u05C7]").containsMatchIn(values.values.joinToString()))
        }
        val name = parser.parse(File(root,"values/starlink_name.xml")).getElementsByTagName("string").item(0)
        assertEquals("false",name.attributes.getNamedItem("translatable").nodeValue)
        assertEquals("Starlink V3 · 40083",name.textContent)
    }
}
