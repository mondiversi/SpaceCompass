package me.mondiversi.spacecompass

import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.os.Build
import org.json.JSONArray
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.io.File
import java.net.HttpURLConnection
import java.net.URI
import java.security.cert.CertificateFactory
import java.util.Base64

internal class SpaceCompassUpdateClient(private val context: Context) {
    // The signing identity comes from this installed app, never from the remote index.
    private val certificateBytes: ByteArray by lazy {
        val installed = context.packageManager.getPackageInfo(context.packageName, signingFlags())
        val signers = signers(installed)
        require(signers.size == 1) { "Unsupported installed signing identity" }
        signers.single()
    }

    fun catalog(): SpaceCompassUpdateCatalog {
        val releases = JSONArray(request(SpaceCompassUpdatePolicy.RELEASES_API, 2 * 1024 * 1024).toString(Charsets.UTF_8))
        val candidates = buildList {
            for (index in 0 until releases.length()) {
                val release = releases.getJSONObject(index)
                if (release.optBoolean("draft", true)) continue
                // List releases includes public previews, unlike /releases/latest.
                val version = release.optString("tag_name").removePrefix("v")
                if (SpaceCompassUpdatePolicy.versionParts(version) == null) continue
                val assets = release.optJSONArray("assets") ?: continue
                for (assetIndex in 0 until assets.length()) {
                    val asset = assets.getJSONObject(assetIndex)
                    if (asset.optString("name") != SpaceCompassUpdatePolicy.INDEX_NAME) continue
                    require(asset.optString("state") == "uploaded" &&
                        asset.getLong("size") in 1..SpaceCompassUpdatePolicy.MAX_INDEX_BYTES.toLong() &&
                        asset.getString("browser_download_url") == SpaceCompassUpdatePolicy.indexUrl(version)) { "Invalid release index asset" }
                    add(version)
                }
            }
        }.distinct().sortedWith { a, b -> SpaceCompassUpdatePolicy.compareVersions(b, a) }.take(3)
        val verified = candidates.map { version ->
            val encoded = request(SpaceCompassUpdatePolicy.indexUrl(version), SpaceCompassUpdatePolicy.MAX_INDEX_BYTES).toString(Charsets.UTF_8)
            verifyIndex(encoded).also { require(it.app.version == version) { "Release tag/index mismatch" } }
        }
        return SpaceCompassUpdatePolicy.select(verified, installedCode(), Build.VERSION.SDK_INT)
    }

    fun verifyIndex(encoded: String): SpaceCompassVerifiedRelease {
        require(encoded.toByteArray(Charsets.UTF_8).size <= SpaceCompassUpdatePolicy.MAX_INDEX_BYTES)
        val envelope = JSONObject(encoded)
        require(envelope.getString("algorithm") == "SHA256withRSA")
        val payload = Base64.getDecoder().decode(envelope.getString("payload"))
        val signature = Base64.getDecoder().decode(envelope.getString("signature"))
        val certificate = CertificateFactory.getInstance("X.509").generateCertificate(certificateBytes.inputStream())
        require(SpaceCompassUpdatePolicy.verifySignature(payload, signature, certificate.publicKey)) { "Invalid update signature" }
        val json = JSONObject(payload.toString(Charsets.UTF_8))
        require(json.getInt("schema") == 1 && json.getString("repository") == SpaceCompassUpdatePolicy.REPOSITORY)
        val app = json.getJSONObject("app")
        require(app.getString("package") == context.packageName)
        val result = SpaceCompassAppRelease(app.getString("version"), app.getLong("code"), app.getInt("min_sdk"),
            SpaceCompassUpdateAsset(app.getString("url"), app.getLong("bytes"), app.getString("sha256")))
        require(SpaceCompassUpdatePolicy.validRelease(result)) { "Invalid app update metadata" }
        return SpaceCompassVerifiedRelease(result, encoded)
    }

    fun download(release: SpaceCompassVerifiedRelease, progress: (Float) -> Unit): File {
        val app = release.app
        require(app.code > installedCode() && Build.VERSION.SDK_INT >= app.minSdk)
        val directory = File(context.cacheDir, "updates").apply { check(isDirectory || mkdirs()) }
        val target = File(directory, "${app.asset.sha256}.apk")
        if (!SpaceCompassUpdatePolicy.matches(target, app.asset)) {
            target.delete()
            val connection = connection(app.asset.url)
            try {
                require(connection.responseCode == 200) { "Update HTTP ${connection.responseCode}" }
                require(connection.contentLengthLong == -1L || connection.contentLengthLong == app.asset.bytes) { "Update size mismatch" }
                connection.inputStream.use { SpaceCompassUpdatePolicy.saveDownload(it, target, app.asset, progress) }
            } finally { connection.disconnect() }
        }
        verifyApk(target, app)
        File(directory, "pending-index.json").writeText(release.encoded)
        return target
    }

    fun pendingInstaller(): Pair<SpaceCompassVerifiedRelease, File> {
        val directory = File(context.cacheDir, "updates")
        val index = File(directory, "pending-index.json")
        require(index.length() in 1..SpaceCompassUpdatePolicy.MAX_INDEX_BYTES.toLong())
        val release = verifyIndex(index.readText())
        val target = File(directory, "${release.app.asset.sha256}.apk")
        verifyApk(target, release.app)
        return release to target
    }

    fun verifyApk(file: File, release: SpaceCompassAppRelease) {
        require(SpaceCompassUpdatePolicy.matches(file, release.asset)) { "APK checksum/size mismatch" }
        val info = context.packageManager.getPackageArchiveInfo(file.absolutePath, signingFlags()) ?: error("Invalid APK")
        val application = requireNotNull(info.applicationInfo)
        require(info.packageName == context.packageName && code(info) == release.code && info.versionName == release.version &&
            release.code > installedCode() && application.minSdkVersion == release.minSdk && Build.VERSION.SDK_INT >= release.minSdk) { "APK identity/version mismatch" }
        require(application.flags and (ApplicationInfo.FLAG_DEBUGGABLE or ApplicationInfo.FLAG_TEST_ONLY) == 0) { "Not a distribution APK" }
        val signers = signers(info)
        require(signers.size == 1 && signers.single().contentEquals(certificateBytes)) { "APK signing identity mismatch" }
    }

    private fun installedCode(): Long = code(context.packageManager.getPackageInfo(context.packageName, 0))

    private fun request(url: String, limit: Int): ByteArray {
        val connection = connection(url)
        try {
            require(connection.responseCode == 200) { "Update HTTP ${connection.responseCode}" }
            require(connection.contentLengthLong <= limit || connection.contentLengthLong == -1L)
            return connection.inputStream.use { input ->
                val output = ByteArrayOutputStream()
                val buffer = ByteArray(8192)
                while (output.size() <= limit) {
                    val count = input.read(buffer, 0, minOf(buffer.size, limit + 1 - output.size()))
                    if (count < 0) break
                    output.write(buffer, 0, count)
                }
                require(output.size() <= limit) { "Update response too large" }
                output.toByteArray()
            }
        } finally { connection.disconnect() }
    }

    private fun connection(initial: String): HttpURLConnection {
        require(SpaceCompassUpdatePolicy.allowedUrl(initial))
        var url = initial
        repeat(6) {
            val connection = URI(url).toURL().openConnection() as HttpURLConnection
            connection.connectTimeout = 10_000
            connection.readTimeout = 20_000
            connection.instanceFollowRedirects = false
            connection.setRequestProperty("User-Agent", "SpaceCompass/${BuildConfig.VERSION_NAME}")
            connection.setRequestProperty("Accept", if (url == SpaceCompassUpdatePolicy.RELEASES_API) "application/vnd.github+json" else "application/octet-stream")
            if (url == SpaceCompassUpdatePolicy.RELEASES_API) connection.setRequestProperty("X-GitHub-Api-Version", "2022-11-28")
            try {
                if (connection.responseCode !in setOf(301, 302, 303, 307, 308)) return connection
                val location = connection.getHeaderField("Location") ?: error("Missing update redirect")
                val next = URI(url).resolve(location).toString()
                require(SpaceCompassUpdatePolicy.allowedUrl(next, redirect = true)) { "Unsafe update redirect" }
                url = next
            } catch (error: Exception) { connection.disconnect(); throw error }
            connection.disconnect()
        }
        error("Too many update redirects")
    }

    @Suppress("DEPRECATION")
    private fun code(info: PackageInfo): Long = if (Build.VERSION.SDK_INT >= 28) info.longVersionCode else info.versionCode.toLong()

    @Suppress("DEPRECATION")
    private fun signingFlags(): Int = if (Build.VERSION.SDK_INT >= 28) PackageManager.GET_SIGNING_CERTIFICATES else PackageManager.GET_SIGNATURES

    @Suppress("DEPRECATION")
    private fun signers(info: PackageInfo): List<ByteArray> =
        (if (Build.VERSION.SDK_INT >= 28) info.signingInfo?.apkContentsSigners.orEmpty() else info.signatures.orEmpty()).map { it.toByteArray() }
}
