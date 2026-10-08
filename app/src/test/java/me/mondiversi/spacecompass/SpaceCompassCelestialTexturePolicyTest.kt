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
        assertEquals(12, images.size)
        assertEquals(12, images.map { it.name }.distinct().size)
        assertFalse(images.any { it.name == "lv426.webp" })
        for (body in SpaceCompassCelestialBody.entries.filter { it.viewerTexture != null && it != SpaceCompassCelestialBody.LV_426 })
            assertNotNull(body.name, SpaceCompassCelestialTexturePolicy.texture(body.viewerTexture))
        assertEquals(3_109_226, images.sumOf { it.bytes })
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
}
