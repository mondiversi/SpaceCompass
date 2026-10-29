package me.mondiversi.spacecompass

import java.io.ByteArrayInputStream
import java.io.File
import java.nio.file.Files
import org.junit.Assert.*
import org.junit.Test

class SpaceCompassCelestialThumbnailPolicyTest {
    private val policy = SpaceCompassCelestialTexturePolicy
    private val previews = SpaceCompassCelestialThumbnailPolicy
    private val project = File("..").takeIf { File(it, "ASSET_CREDITS.md").isFile } ?: File(".")
    private fun bytes(name: String) = File(project, "textures/${policy.PACK}/$name").readBytes()

    @Test fun everyViewerImageHasAnIndependentSmallVerifiedExternalPreview() {
        assertEquals(policy.images.size, previews.images.size)
        assertEquals(previews.images.size, previews.images.map { it.name }.distinct().size)
        for (parent in policy.images) {
            val preview = requireNotNull(previews.forImage(parent.name))
            assertEquals(128, preview.width); assertEquals(128, preview.height)
            assertNotEquals(parent.name, preview.name)
            assertTrue(preview.bytes < parent.bytes)
            assertTrue(policy.matches(preview, bytes(preview.name)))
            assertSame(preview, policy.texture(preview.name))
            assertFalse(File(project, "app/src/main/assets/celestial/${preview.name}").exists())
        }
        for (body in SpaceCompassCelestialBody.entries.filter { it.viewerTexture != null })
            assertNotNull(body.name, previews.forImage(body.viewerTexture))
        assertNull(previews.forImage(null))
        assertNull(previews.forImage("unknown.webp"))
    }

    @Test fun previewOnlyStorageWorksWithoutInstallingTheFullImageAndSurvivesRestart() {
        val directory = Files.createTempDirectory("celestial-preview-only").toFile()
        val preview = requireNotNull(previews.forImage("moon.webp"))
        try {
            SpaceCompassCelestialTextureFiles(directory).store(preview, bytes(preview.name))
            val restarted = SpaceCompassCelestialTextureFiles(directory)
            assertNotNull(restarted.cached(preview))
            assertNull(restarted.cached(requireNotNull(policy.texture("moon.webp"))))
            assertEquals(listOf(preview.name), directory.listFiles()!!.map { it.name })
        } finally { directory.walkBottomUp().forEach(File::delete) }
    }

    @Test fun previewImportKeepsLegacyFullImageBytesAndTheirIndependentPinsIntact() {
        val directory = Files.createTempDirectory("celestial-preview-legacy").toFile()
        val legacy = File(directory, "legacy").apply { mkdir() }
        val current = SpaceCompassCelestialTextureFiles(File(directory, "current"))
        val parent = requireNotNull(policy.texture("moon.webp"))
        val preview = requireNotNull(previews.forImage(parent.name))
        try {
            File(legacy, parent.name).writeBytes(bytes(parent.name))
            current.importVerified(parent, listOf(legacy))
            current.store(preview, bytes(preview.name))
            assertTrue(policy.matches(parent, current.cached(parent)!!.readBytes()))
            assertTrue(policy.matches(parent, File(legacy, parent.name).readBytes()))
            assertThrows(IllegalArgumentException::class.java) {
                current.store(preview, bytes(parent.name))
            }
            assertTrue(policy.matches(preview, current.cached(preview)!!.readBytes()))
        } finally { directory.walkBottomUp().forEach(File::delete) }
    }

    @Test fun publishedPreviewsUseIndependentVersionedUrlsAndRejectCorruption() {
        for (preview in previews.images.filter { it.downloadPack != null }) {
                assertEquals(policy.images.single { "thumb_" + it.name == preview.name }.downloadPack, preview.downloadPack)
                assertEquals(preview.name, preview.downloadName)
                assertEquals("https://github.com/mondiversi/SpaceCompass/releases/download/${preview.downloadPack}/${preview.downloadName}", preview.url)
                assertTrue(policy.allowedUrl(requireNotNull(preview.url)))
                assertFalse(policy.allowedUrl(preview.url + "?extra=1"))
        }
        val preview = requireNotNull(previews.forImage("moon.webp"))
        assertThrows(IllegalArgumentException::class.java) {
            policy.read(preview, ByteArrayInputStream(ByteArray(preview.bytes)))
        }
    }
}
