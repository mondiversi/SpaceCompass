package me.mondiversi.spacecompass

import java.io.File
import java.io.InputStream
import java.net.URI
import java.security.MessageDigest
import java.security.PublicKey
import java.security.Signature

internal data class SpaceCompassUpdateAsset(val url: String, val bytes: Long, val sha256: String)
internal data class SpaceCompassAppRelease(
    val version: String, val code: Long, val minSdk: Int, val asset: SpaceCompassUpdateAsset
)
internal data class SpaceCompassVerifiedRelease(val app: SpaceCompassAppRelease, val encoded: String)
internal data class SpaceCompassUpdateCatalog(val release: SpaceCompassVerifiedRelease?, val incompatible: Boolean = false)

/** App-only release policy; no sensor archive, credentials or firmware dependencies. */
internal object SpaceCompassUpdatePolicy {
    const val REPOSITORY = "mondiversi/SpaceCompass"
    const val REPOSITORY_URL = "https://github.com/$REPOSITORY"
    const val RELEASES_API = "https://api.github.com/repos/$REPOSITORY/releases?per_page=20"
    const val INDEX_NAME = "space-compass-update.json"
    const val MAX_INDEX_BYTES = 64 * 1024
    const val MAX_APK_BYTES = 200L * 1024 * 1024
    private val versionPattern = Regex("(0|[1-9][0-9]*)\\.(0|[1-9][0-9]*)\\.(0|[1-9][0-9]*)")
    private val hashPattern = Regex("[0-9a-f]{64}")

    fun versionParts(version: String): List<Int>? {
        val groups = versionPattern.matchEntire(version)?.groupValues?.drop(1) ?: return null
        return groups.map { it.toIntOrNull() ?: return null }
    }

    fun compareVersions(first: String, second: String): Int {
        val a = requireNotNull(versionParts(first))
        val b = requireNotNull(versionParts(second))
        return a.zip(b).firstOrNull { it.first != it.second }?.let { it.first.compareTo(it.second) } ?: 0
    }

    fun indexUrl(version: String): String {
        require(versionParts(version) != null)
        return "$REPOSITORY_URL/releases/download/v$version/$INDEX_NAME"
    }

    fun apkUrl(version: String): String {
        require(versionParts(version) != null)
        return "$REPOSITORY_URL/releases/download/v$version/space-compass-$version.apk"
    }

    fun allowedUrl(value: String, redirect: Boolean = false): Boolean = runCatching {
        val uri = URI(value)
        if (uri.scheme != "https" || uri.userInfo != null || uri.port !in listOf(-1, 443) || uri.fragment != null) return false
        if (value == RELEASES_API) return true
        if (redirect && uri.host in setOf("release-assets.githubusercontent.com", "objects.githubusercontent.com")) return true
        if (uri.host != "github.com" || uri.query != null || uri.rawPath != uri.path) return false
        val match = Regex("/mondiversi/SpaceCompass/releases/download/v([^/]+)/([^/]+)").matchEntire(uri.path) ?: return false
        val version = match.groupValues[1]
        versionParts(version) != null && match.groupValues[2] in setOf(INDEX_NAME, "space-compass-$version.apk")
    }.getOrDefault(false)

    fun validRelease(app: SpaceCompassAppRelease): Boolean = versionParts(app.version) != null &&
        app.code in 1..Int.MAX_VALUE.toLong() && app.minSdk in 26..100 &&
        app.asset.url == apkUrl(app.version) && app.asset.bytes in 1..MAX_APK_BYTES && hashPattern.matches(app.asset.sha256)

    fun select(releases: List<SpaceCompassVerifiedRelease>, installedCode: Long, sdk: Int): SpaceCompassUpdateCatalog {
        val newer = releases.filter { validRelease(it.app) && it.app.code > installedCode }
        val supported = newer.filter { it.app.minSdk <= sdk }.maxByOrNull { it.app.code }
        return SpaceCompassUpdateCatalog(supported, supported == null && newer.isNotEmpty())
    }

    fun verifySignature(payload: ByteArray, signature: ByteArray, key: PublicKey): Boolean = runCatching {
        payload.size in 1..MAX_INDEX_BYTES && signature.size in 1..1024 && Signature.getInstance("SHA256withRSA").run {
            initVerify(key); update(payload); verify(signature)
        }
    }.getOrDefault(false)

    fun sha256(file: File): String = file.inputStream().use { input ->
        val digest = MessageDigest.getInstance("SHA-256")
        val buffer = ByteArray(32 * 1024)
        while (true) {
            val count = input.read(buffer)
            if (count < 0) break
            digest.update(buffer, 0, count)
        }
        hex(digest.digest())
    }

    fun matches(file: File, asset: SpaceCompassUpdateAsset): Boolean =
        file.isFile && file.length() == asset.bytes && sha256(file) == asset.sha256

    /** Only publish a complete, verified file; interrupted/bad responses never become installers. */
    fun saveDownload(input: InputStream, target: File, asset: SpaceCompassUpdateAsset, progress: (Float) -> Unit) {
        require(asset.bytes in 1..MAX_APK_BYTES && hashPattern.matches(asset.sha256))
        val partial = File(target.parentFile, target.name + ".part")
        try {
            val digest = MessageDigest.getInstance("SHA-256")
            var total = 0L
            var percentage = -1
            partial.outputStream().use { output ->
                val buffer = ByteArray(32 * 1024)
                while (true) {
                    val count = input.read(buffer)
                    if (count < 0) break
                    total += count
                    require(total <= asset.bytes) { "Update exceeds signed size" }
                    digest.update(buffer, 0, count)
                    output.write(buffer, 0, count)
                    val next = (total * 100 / asset.bytes).toInt()
                    if (next != percentage) { percentage = next; progress(total.toFloat() / asset.bytes) }
                }
                output.fd.sync()
            }
            require(total == asset.bytes && hex(digest.digest()) == asset.sha256) { "Update checksum mismatch" }
            check(partial.renameTo(target)) { "Cannot finalize update download" }
        } finally {
            partial.delete()
        }
    }

    private fun hex(bytes: ByteArray): String = bytes.joinToString("") { "%02x".format(it.toInt() and 255) }
}
