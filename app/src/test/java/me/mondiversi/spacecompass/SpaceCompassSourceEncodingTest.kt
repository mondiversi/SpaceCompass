package me.mondiversi.spacecompass

import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/** Prevent UTF-8 files accidentally rewritten through Windows' legacy code page. */
class SpaceCompassSourceEncodingTest {
    @Test fun sourceAndResourceTextContainsNoUtf8AsLegacyCodePageArtifacts() {
        val root = File("src").takeIf { it.isDirectory } ?: File("app/src")
        val malformed = Regex("[\\u00c2\\u00c3][\\u0080-\\u00bf]|\\u00e2[\\u0080\\u20ac][\\u0080-\\u00bf\\u2018-\\u2026]")
        val files = root.walkTopDown().filter { it.isFile && it.extension in setOf("kt", "xml") }.toList()
        assertTrue(files.isNotEmpty())
        val affected = files.filter { malformed.containsMatchIn(it.readText(Charsets.UTF_8)) }
        assertTrue("Malformed source encoding in ${affected.map { it.relativeTo(root).path }}", affected.isEmpty())
    }
}
