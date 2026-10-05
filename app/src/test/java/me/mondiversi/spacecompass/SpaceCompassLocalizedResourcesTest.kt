package me.mondiversi.spacecompass

import java.io.File
import javax.xml.parsers.DocumentBuilderFactory
import org.junit.Assert.*
import org.junit.Test
import org.w3c.dom.Element

/** All copied catalogs, including neutral shared controls, retain complete translations. */
class SpaceCompassLocalizedResourcesTest {
    private val root = File("src/main/res").takeIf { it.isDirectory } ?: File("app/src/main/res")
    private fun catalog(directory: File): Map<String, String> {
        val factory = DocumentBuilderFactory.newInstance().apply {
            setFeature("http://apache.org/xml/features/disallow-doctype-decl", true)
        }
        val result = mutableMapOf<String, String>()
        directory.listFiles().orEmpty().filter { it.extension == "xml" }.forEach { file ->
            val strings = factory.newDocumentBuilder().parse(file).getElementsByTagName("string")
            for (index in 0 until strings.length) {
                val element = strings.item(index) as Element
                val key = element.getAttribute("name")
                assertFalse("Duplicate ${directory.name}/$key", result.containsKey(key))
                if (element.getAttribute("translatable") != "false") result[key] = element.textContent
            }
        }
        return result
    }
    @Test fun twentyCatalogsAreCompleteAndFormattingArgumentsAgree() {
        val base = catalog(File(root, "values"))
        val locales = root.listFiles().orEmpty().filter {
            it.isDirectory && it.name.startsWith("values-") && File(it, "celestial.xml").isFile
        }
        assertEquals(19, locales.size)
        val format = Regex("(?<!%)%(\\d+)\\$[sd]")
        for (locale in locales) {
            val translated = catalog(locale)
            assertEquals("Missing ${locale.name}", emptySet<String>(), base.keys - translated.keys)
            assertEquals("Unknown ${locale.name}", emptySet<String>(), translated.keys - base.keys)
            for ((key, original) in base) {
                assertTrue("Blank ${locale.name}/$key", translated.getValue(key).isNotBlank())
                assertEquals("Arguments ${locale.name}/$key",
                    format.findAll(original).map { it.value }.sorted().toList(),
                    format.findAll(translated.getValue(key)).map { it.value }.sorted().toList())
            }
        }
    }
}
