package me.mondiversi.spacecompass

import java.io.File
import java.io.IOException
import java.security.KeyPairGenerator
import java.security.Signature
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class SpaceCompassUpdatePolicyTest {
    @get:Rule val temporary = TemporaryFolder()
    private val abcHash = "ba7816bf8f01cfea414140de5dae2223b00361a396177a9cb410ff61f20015ad"
    private fun release(code: Long, sdk: Int = 26, version: String = "0.1.$code") = SpaceCompassVerifiedRelease(
        SpaceCompassAppRelease(version, code, sdk,
            SpaceCompassUpdateAsset(SpaceCompassUpdatePolicy.apkUrl(version), 3, abcHash)), "signed fixture")

    @Test fun versionsSortNumericallyAndRejectAmbiguousOrOversizedTags() {
        assertTrue(SpaceCompassUpdatePolicy.compareVersions("0.10.0", "0.9.99") > 0)
        assertTrue(SpaceCompassUpdatePolicy.compareVersions("1.0.0", "0.99.99") > 0)
        for (value in listOf("v0.1.1", "01.1.1", "0.1.1-beta", "0.1", "-1.0.0", "999999999999.0.0", "0.1.1 "))
            assertNull(value, SpaceCompassUpdatePolicy.versionParts(value))
    }

    @Test fun onlyProjectHttpsAssetsAreAcceptedAtTheStartOfARequest() {
        assertTrue(SpaceCompassUpdatePolicy.allowedUrl(SpaceCompassUpdatePolicy.RELEASES_API))
        assertTrue(SpaceCompassUpdatePolicy.allowedUrl(SpaceCompassUpdatePolicy.apkUrl("0.1.2")))
        assertTrue(SpaceCompassUpdatePolicy.allowedUrl(SpaceCompassUpdatePolicy.indexUrl("0.1.2")))
        val good = SpaceCompassUpdatePolicy.apkUrl("0.1.2")
        for (url in listOf(good.replace("https:", "http:"), good.replace("github.com", "github.com.evil.example"),
            good.replace("github.com", "attacker@github.com"), good.replace("github.com", "github.com:444"),
            good.replace("SpaceCompass/", "Uvir/"), good + "?redirect=1", good + "#fragment",
            good.replace("/v0.1.2/", "/v0.1.2/../v0.1.2/"), good.replace("/v0.1.2/", "/%76%30.1.2/"),
            "https://release-assets.githubusercontent.com/file.apk", "file:///tmp/update.apk"))
            assertFalse(url, SpaceCompassUpdatePolicy.allowedUrl(url))
    }

    @Test fun redirectHostsRemainHttpsAndDoNotAllowArbitraryDownloads() {
        assertTrue(SpaceCompassUpdatePolicy.allowedUrl("https://release-assets.githubusercontent.com/github-production-release-asset/file?token=public", true))
        assertTrue(SpaceCompassUpdatePolicy.allowedUrl("https://objects.githubusercontent.com/github-production-release-asset/file", true))
        for (url in listOf("https://raw.githubusercontent.com/file", "http://objects.githubusercontent.com/file",
            "https://objects.githubusercontent.com.evil.example/file", "https://user@objects.githubusercontent.com/file", "https://example.com/file"))
            assertFalse(url, SpaceCompassUpdatePolicy.allowedUrl(url, true))
    }

    @Test fun signedCodeChoosesHighestCompatibleUpdateRegardlessOfCatalogOrder() {
        val selected = SpaceCompassUpdatePolicy.select(listOf(release(8), release(2), release(7)), 1, 35)
        assertEquals(8L, selected.release!!.app.code)
        assertFalse(selected.incompatible)
    }

    @Test fun installedOrOlderVersionsNeverTriggerAnUpdate() {
        val selected = SpaceCompassUpdatePolicy.select(listOf(release(1), release(2)), 2, 35)
        assertNull(selected.release)
        assertFalse(selected.incompatible)
    }

    @Test fun incompatibleNewReleaseFallsBackToSupportedOneOrReportsAndroidRequirement() {
        assertEquals(3L, SpaceCompassUpdatePolicy.select(listOf(release(4, 36), release(3)), 2, 35).release!!.app.code)
        val unavailable = SpaceCompassUpdatePolicy.select(listOf(release(4, 36)), 2, 35)
        assertNull(unavailable.release)
        assertTrue(unavailable.incompatible)
    }

    @Test fun emptyAndInvalidMetadataCannotBecomeAnUpdate() {
        assertNull(SpaceCompassUpdatePolicy.select(emptyList(), 1, 35).release)
        val good = release(2).app
        for (bad in listOf(good.copy(code = 0), good.copy(code = Long.MAX_VALUE), good.copy(minSdk = 1),
            good.copy(asset = good.asset.copy(bytes = SpaceCompassUpdatePolicy.MAX_APK_BYTES + 1)),
            good.copy(asset = good.asset.copy(sha256 = "bad")), good.copy(asset = good.asset.copy(url = "https://example.com/app.apk"))))
            assertFalse(SpaceCompassUpdatePolicy.validRelease(bad))
    }

    @Test fun metadataSignatureRejectsTamperingAndDifferentSigningKeys() {
        val generator = KeyPairGenerator.getInstance("RSA").apply { initialize(2048) }
        val trusted = generator.generateKeyPair()
        val attacker = generator.generateKeyPair()
        val payload = "{\"code\":2,\"package\":\"me.mondiversi.spacecompass\"}".toByteArray()
        val signature = Signature.getInstance("SHA256withRSA").run {
            initSign(trusted.private); update(payload); sign()
        }
        assertTrue(SpaceCompassUpdatePolicy.verifySignature(payload, signature, trusted.public))
        assertFalse(SpaceCompassUpdatePolicy.verifySignature(payload + 1.toByte(), signature, trusted.public))
        assertFalse(SpaceCompassUpdatePolicy.verifySignature(payload, signature, attacker.public))
        assertFalse(SpaceCompassUpdatePolicy.verifySignature(payload, ByteArray(0), trusted.public))
    }

    private fun download(bytes: ByteArray, expectedHash: String = abcHash, size: Long = 3): File {
        val target = File(temporary.root, "update.apk")
        val asset = SpaceCompassUpdateAsset(SpaceCompassUpdatePolicy.apkUrl("0.1.2"), size, expectedHash)
        bytes.inputStream().use { SpaceCompassUpdatePolicy.saveDownload(it, target, asset) {} }
        return target
    }

    @Test fun completeVerifiedDownloadBecomesInstallerAndReportsBoundedProgress() {
        val progress = mutableListOf<Float>()
        val target = File(temporary.root, "update.apk")
        val asset = release(2).app.asset
        "abc".byteInputStream().use { SpaceCompassUpdatePolicy.saveDownload(it, target, asset, progress::add) }
        assertEquals("abc", target.readText())
        assertTrue(SpaceCompassUpdatePolicy.matches(target, asset))
        assertEquals(1f, progress.last(), 0f)
        assertTrue(progress.all { it in 0f..1f })
        assertFalse(File(temporary.root, "update.apk.part").exists())
    }

    @Test fun truncatedResponseIsNeverPublishedAndPartialIsRemoved() {
        assertThrows(IllegalArgumentException::class.java) { download("ab".toByteArray()) }
        assertFalse(File(temporary.root, "update.apk").exists())
        assertFalse(File(temporary.root, "update.apk.part").exists())
    }

    @Test fun oversizedResponseIsNeverPublishedAndPartialIsRemoved() {
        assertThrows(IllegalArgumentException::class.java) { download("abcd".toByteArray()) }
        assertFalse(File(temporary.root, "update.apk").exists())
        assertFalse(File(temporary.root, "update.apk.part").exists())
    }

    @Test fun wrongChecksumIsNeverPublishedAndPartialIsRemoved() {
        assertThrows(IllegalArgumentException::class.java) { download("abd".toByteArray()) }
        assertFalse(File(temporary.root, "update.apk").exists())
        assertFalse(File(temporary.root, "update.apk.part").exists())
    }

    @Test fun interruptedTransferRemovesPartialWithoutDamagingExistingVerifiedFile() {
        val target = File(temporary.root, "update.apk").apply { writeText("abc") }
        val input = object : java.io.InputStream() { override fun read(): Int = throw IOException("Interrupted") }
        assertThrows(IOException::class.java) { SpaceCompassUpdatePolicy.saveDownload(input, target, release(2).app.asset) {} }
        assertEquals("abc", target.readText())
        assertFalse(File(temporary.root, "update.apk.part").exists())
    }
}
