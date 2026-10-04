package me.mondiversi.planetcompass

import org.junit.Assert.*
import org.junit.Test
import java.io.File
import javax.xml.parsers.DocumentBuilderFactory

class PlanetCompassCelestialCatalogTranslationTest {
    @Test fun newCatalogueNamesAndJupiterSpeedAreTranslatedInAllTwentyLocales() {
        val root=File("src/main/res").takeIf { it.isDirectory } ?: File("app/src/main/res")
        val directories=listOf("values","values-it","values-de","values-fr","values-es","values-pt",
            "values-nl","values-pl","values-in","values-ar","values-fa","values-iw","values-el",
            "values-ru","values-tr","values-sw","values-hi","values-ja","values-ko-rKR","values-zh-rCN")
        val keys=listOf("celestial_io","celestial_europa","celestial_catalog_distance","celestial_speed_orbit_jupiter")
        for(directory in directories) {
            val file=File(root,"$directory/celestial_catalog.xml")
            assertTrue(directory,file.isFile)
            val elements=DocumentBuilderFactory.newInstance().apply {
                setFeature("http://apache.org/xml/features/disallow-doctype-decl",true)
            }.newDocumentBuilder().parse(file).getElementsByTagName("string")
            val values=(0 until elements.length).associate { index ->
                val element=elements.item(index)
                element.attributes.getNamedItem("name").nodeValue to element.textContent
            }
            keys.forEach { assertFalse("$directory/$it",values[it].isNullOrBlank()) }
            assertFalse("$directory/header must not imply all distances use AU",
                values.getValue("celestial_catalog_distance").contains("AU"))
            assertTrue(values.getValue("celestial_speed_orbit_jupiter").contains("%1\$s"))
            if(directory=="values-iw") assertFalse(Regex("[\\u0591-\\u05BD\\u05BF-\\u05C7]").containsMatchIn(values.values.joinToString()))
        }
    }
}
