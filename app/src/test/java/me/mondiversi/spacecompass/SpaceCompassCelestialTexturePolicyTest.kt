package me.mondiversi.spacecompass

import java.io.ByteArrayInputStream
import java.io.File
import java.nio.file.Files
import java.security.MessageDigest
import org.junit.Assert.*
import org.junit.Test

class SpaceCompassCelestialTexturePolicyTest {
    private val bytes = "verified-texture".toByteArray()
    private val texture = SpaceCompassCelestialTexture("moon.webp", bytes.size,
        MessageDigest.getInstance("SHA-256").digest(bytes).joinToString("") { "%02x".format(it.toInt() and 255) }, 2048, 1024)

    @Test fun onlyNamedPublicMapsAndHttpsGitHubDownloadRedirectsAreAccepted() {
        val policy = SpaceCompassCelestialTexturePolicy
        for (image in policy.images) assertTrue(policy.allowedUrl(image.url))
        assertFalse(policy.allowedUrl("https://github.com/other/repo/releases/download/v1/moon.webp"))
        assertFalse(policy.allowedUrl("http://github.com/mondiversi/SpaceCompass/releases/download/celestial-textures-v1/moon.webp"))
        assertFalse(policy.allowedUrl(policy.images.first().url + "?extra=1"))
        assertFalse(policy.allowedUrl("https://release-assets.githubusercontent.com/file", false))
        assertTrue(policy.allowedUrl("https://release-assets.githubusercontent.com/file?download=public", true))
        assertFalse(policy.allowedUrl("https://release-assets.githubusercontent.com.attacker.example/file", true))
        assertFalse(policy.allowedUrl("https://user:secret@release-assets.githubusercontent.com/file", true))
        assertFalse(policy.allowedUrl("https://release-assets.githubusercontent.com:444/file", true))
    }

    @Test fun packCoversEveryRemoteMapWithoutFictionalTextureOrDuplicateNames() {
        val images = SpaceCompassCelestialTexturePolicy.images
        assertEquals(18, images.size)
        assertEquals(18, images.map { it.name }.distinct().size)
        assertFalse(images.any { it.name == "lv426.webp" })
        for (body in SpaceCompassCelestialBody.entries.filter { it.viewerTexture != null && it != SpaceCompassCelestialBody.LV_426 })
            assertNotNull(body.name, SpaceCompassCelestialTexturePolicy.texture(body.viewerTexture))
        assertEquals(3_969_738, images.sumOf { it.bytes })
    }

    @Test fun byteLengthAndDigestAreBothRequired() {
        assertArrayEquals(bytes, SpaceCompassCelestialTexturePolicy.read(texture, ByteArrayInputStream(bytes)))
        assertThrows(IllegalArgumentException::class.java) {
            SpaceCompassCelestialTexturePolicy.read(texture, ByteArrayInputStream(bytes.dropLast(1).toByteArray()))
        }
        assertThrows(IllegalArgumentException::class.java) {
            SpaceCompassCelestialTexturePolicy.read(texture, ByteArrayInputStream(bytes + 0))
        }
        assertThrows(IllegalArgumentException::class.java) {
            SpaceCompassCelestialTexturePolicy.read(texture, ByteArrayInputStream(ByteArray(bytes.size)))
        }
    }

    @Test fun cancellationIsCheckedBeforeReceivingAndBeforePublishingData() {
        var checks = 0
        assertThrows(java.util.concurrent.CancellationException::class.java) {
            SpaceCompassCelestialTexturePolicy.read(texture, ByteArrayInputStream(bytes)) {
                if (++checks == 2) throw java.util.concurrent.CancellationException()
            }
        }
        assertEquals(2, checks)
    }

    @Test fun mapsSurviveAStoreRestartAndAnInvalidReplacementPreservesTheOfflineCopy() {
        val directory = Files.createTempDirectory("celestial-texture-test").toFile()
        try {
            val first = SpaceCompassCelestialTextureFiles(directory)
            assertNull(first.cached(texture))
            val installed = first.store(texture, bytes)
            assertEquals(installed, first.cached(texture))
            val restarted = SpaceCompassCelestialTextureFiles(directory)
            assertArrayEquals(bytes, restarted.cached(texture)!!.readBytes())
            assertThrows(IllegalArgumentException::class.java) { restarted.store(texture, ByteArray(bytes.size)) }
            assertArrayEquals(bytes, installed.readBytes())
            assertEquals(listOf("moon.webp"), directory.listFiles()!!.map { it.name })
        } finally { directory.listFiles()?.forEach(File::delete); directory.delete() }
    }

    @Test fun damagedDiskCopiesAreRejectedAndCanBeReplaced() {
        val directory = Files.createTempDirectory("celestial-texture-corruption").toFile()
        try {
            File(directory, texture.name).writeBytes(ByteArray(bytes.size))
            val files = SpaceCompassCelestialTextureFiles(directory)
            assertNull(files.cached(texture))
            files.store(texture, bytes)
            assertArrayEquals(bytes, files.cached(texture)!!.readBytes())
        } finally { directory.listFiles()?.forEach(File::delete); directory.delete() }
    }

    @Test fun cacheFilenamesCannotEscapeTheirDirectory() {
        for (name in listOf("../moon.webp", "/moon.webp", "moon.webp/extra", "moon%2ewebp", "moon.png"))
            assertThrows(IllegalArgumentException::class.java) { texture.copy(name = name) }
    }

    @Test fun matchingPreviousPackIsImportedWithoutRemovingTheOriginal() {
        val root = Files.createTempDirectory("texture-pack-import").toFile()
        val old = File(root, "old").apply { mkdir() }
        val next = File(root, "new")
        try {
            File(old, texture.name).writeBytes(bytes)
            val files = SpaceCompassCelestialTextureFiles(next)
            assertArrayEquals(bytes, files.importVerified(texture, listOf(old))!!.readBytes())
            assertArrayEquals(bytes, File(old, texture.name).readBytes())
            assertArrayEquals(bytes, SpaceCompassCelestialTextureFiles(next).cached(texture)!!.readBytes())
        } finally { root.walkBottomUp().forEach(File::delete) }
    }

    @Test fun wrongLegacyDigestIsSkippedAndLaterMatchingPackCanBeReused() {
        val root = Files.createTempDirectory("texture-pack-corruption").toFile()
        val wrong = File(root, "wrong").apply { mkdir() }
        val valid = File(root, "valid").apply { mkdir() }
        val next = File(root, "new")
        try {
            File(wrong, texture.name).writeBytes(ByteArray(bytes.size))
            val files = SpaceCompassCelestialTextureFiles(next)
            assertNull(files.importVerified(texture, listOf(wrong)))
            assertFalse(next.exists())
            File(valid, texture.name).writeBytes(bytes)
            assertArrayEquals(bytes, files.importVerified(texture, listOf(wrong, valid))!!.readBytes())
            assertArrayEquals(ByteArray(bytes.size), File(wrong, texture.name).readBytes())
        } finally { root.walkBottomUp().forEach(File::delete) }
    }

    @Test fun failedMigrationLeavesPreviouslyDownloadedMapIntact() {
        val root = Files.createTempDirectory("texture-pack-failure").toFile()
        val old = File(root, "old").apply { mkdir() }
        val blocked = File(root, "blocked").apply { writeText("not a directory") }
        try {
            File(old, texture.name).writeBytes(bytes)
            assertThrows(IllegalStateException::class.java) {
                SpaceCompassCelestialTextureFiles(blocked).importVerified(texture, listOf(old))
            }
            assertArrayEquals(bytes, File(old, texture.name).readBytes())
            assertEquals("not a directory", blocked.readText())
        } finally { root.walkBottomUp().forEach(File::delete) }
    }

    @Test fun allImagesUseTheExactNewPackAndOldReleaseUrlsAreNotDownloaded() {
        val policy = SpaceCompassCelestialTexturePolicy
        assertEquals("celestial-textures-v1.1", policy.PACK)
        for (image in policy.images) {
            assertTrue(image.name.endsWith(".webp"))
            assertEquals(2048, image.width)
            assertEquals(1024, image.height)
            assertEquals("https://github.com/mondiversi/SpaceCompass/releases/download/celestial-textures-v1.1/${image.name}", image.url)
            assertFalse(policy.allowedUrl(image.url.replace("celestial-textures-v1.1/", "celestial-textures-v1/")))
        }
        assertEquals(listOf("celestial-textures-v1"), policy.legacyPacks)
    }
}
