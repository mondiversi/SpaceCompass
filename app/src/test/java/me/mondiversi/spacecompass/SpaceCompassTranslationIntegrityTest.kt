package me.mondiversi.spacecompass

import java.io.File
import javax.xml.parsers.DocumentBuilderFactory
import org.junit.Assert.*
import org.junit.Test

class SpaceCompassTranslationIntegrityTest {
    private data class Entry(val value: String, val translatable: Boolean)
    private fun catalog(folder: File): Map<String, Entry> {
        val result = mutableMapOf<String, Entry>()
        folder.listFiles()!!.filter { it.extension == "xml" }.forEach { file ->
            val nodes = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(file).getElementsByTagName("string")
            for (index in 0 until nodes.length) {
                val node = nodes.item(index)
                val key = node.attributes.getNamedItem("name").nodeValue
                assertFalse("Duplicate $key in ${folder.name}", result.containsKey(key))
                result[key] = Entry(node.textContent, node.attributes.getNamedItem("translatable")?.nodeValue != "false")
            }
        }
        return result
    }
    @Test fun everyLanguageHasCompleteNonemptyTextAndMatchingFormatArguments() {
        val folders = File("src/main/res").listFiles()!!.filter { File(it, "common.xml").isFile }
        assertEquals(setOf("values", "values-ar", "values-de", "values-el", "values-es", "values-fa",
            "values-fr", "values-hi", "values-in", "values-it", "values-iw", "values-ja", "values-ko-rKR",
            "values-nl", "values-pl", "values-pt", "values-ru", "values-sw", "values-tr", "values-zh-rCN"),
            folders.map { it.name }.toSet())
        val base = catalog(folders.single { it.name == "values" })
        val expected = base.filterValues { it.translatable }.keys
        val placeholders = Regex("%(?:[0-9]+\\$)?[dsf]|%%")
        for (folder in folders) {
            val translated = catalog(folder)
            assertTrue("Missing translations in ${folder.name}", translated.keys.containsAll(expected))
            assertEquals("Resource key parity in ${folder.name}", expected,
                translated.filterValues { it.translatable }.keys)
            for (key in expected) {
                val value = translated.getValue(key).value
                assertTrue("Empty ${folder.name}/$key", value.isNotBlank())
                assertFalse("Invalid encoding ${folder.name}/$key", value.contains('\uFFFD'))
                assertFalse("Broken UTF-8 ${folder.name}/$key",
                    Regex("Ã[\u0080-\u00BF]|Â[\u0080-\u00BF]|â[€€™œ]").containsMatchIn(value))
                if (folder.name != "values" && base.getValue(key).value.length > 40)
                    assertNotEquals("Untranslated sentence ${folder.name}/$key", base.getValue(key).value, value)
                assertEquals("Format arguments ${folder.name}/$key",
                    placeholders.findAll(base.getValue(key).value).map { it.value }.sorted().toList(),
                    placeholders.findAll(value).map { it.value }.sorted().toList())
            }
        }
    }
}
