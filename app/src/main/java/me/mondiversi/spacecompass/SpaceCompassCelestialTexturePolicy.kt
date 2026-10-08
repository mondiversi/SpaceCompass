package me.mondiversi.spacecompass

import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.net.URI
import java.security.MessageDigest

internal data class SpaceCompassCelestialTexture(val name: String, val bytes: Int,
    val sha256: String, val width: Int, val height: Int) {
    init {
        require(Regex("[a-z0-9_]+\\.(webp|jpg)").matches(name))
        require(bytes in 1..1_048_576 && Regex("[a-f0-9]{64}").matches(sha256))
        require(width in 1..2048 && height in 1..1024)
    }
    val url: String get() = "${SpaceCompassCelestialTexturePolicy.BASE_URL}/$name"
}

/** Versioned public maps. Digests and dimensions belong to this APK, not a remote manifest. */
internal object SpaceCompassCelestialTexturePolicy {
    const val PACK = "celestial-textures-v1"
    const val BASE_URL = "https://github.com/mondiversi/SpaceCompass/releases/download/$PACK"
    val images = listOf(
        SpaceCompassCelestialTexture("europa_2048.webp", 389900, "ff03436f223b26cd5930edddd4408b8e4a5fad3826b08d7dedb34b4711667a8f", 2048, 1024),
        SpaceCompassCelestialTexture("io_2048.webp", 288958, "0a85feb12c6d85d178b7e01b2c93eba20a546ed7591cf93620a4ffae7be7c462", 2048, 1024),
        SpaceCompassCelestialTexture("jupiter.webp", 209714, "d931cf6adcef7a6b6de141a096327f1dd626729ea8842c72cbe5e03f4cb2a0e4", 2048, 1024),
        SpaceCompassCelestialTexture("mars.webp", 353206, "12a40a86ed8b5711632e177977dd3f0be0e62e349bb788ee48e46d6517a7d24d", 2048, 1024),
        SpaceCompassCelestialTexture("mercury.webp", 625630, "36c730a082f062b4fde11f341304bb7d917288a97821a55514fcdd3c8bc7bc52", 2048, 1024),
        SpaceCompassCelestialTexture("moon.webp", 426380, "387059f27f6a0bba9c09ba103380819a96ffb12e5d91f9f9936cd4769b2bf65e", 2048, 1024),
        SpaceCompassCelestialTexture("neptune.webp", 27180, "2a1e452cdd803a8cf487363ae43ee58aef910d4713a772f86f0d3d7e45e6400a", 2048, 1024),
        SpaceCompassCelestialTexture("pluto.webp", 282598, "10694489ab13df79c49a2d49e48246e22ae3aa6d99a07f111ac83ea07808d420", 2048, 1024),
        SpaceCompassCelestialTexture("saturn.webp", 59112, "baedddf7db60bea91b8e1c98ca90e6742dd1c131d886fa2e94e305ee8aeb531b", 2048, 1024),
        SpaceCompassCelestialTexture("sun.webp", 368406, "ddfdf9ca2f531dad52c163e012b7b522deb96700e876f87bb74aa0fc42af10a5", 2048, 1024),
        SpaceCompassCelestialTexture("uranus.webp", 11406, "fa9608375c2c5805fdd23d879151e6ad187a142474c8298f28441df66740660c", 2048, 1024),
        SpaceCompassCelestialTexture("venus_atmosphere.webp", 66736, "580dfe532a07f54cea30e608dc6f260de39474dff0cfdfc86643b4bfb51eb85d", 2048, 1024)
    )
    fun texture(name: String?) = images.firstOrNull { it.name == name }
    fun allowedUrl(url: String, redirect: Boolean = false): Boolean = runCatching {
        val uri = URI(url)
        if (uri.scheme != "https" || uri.userInfo != null || uri.fragment != null || uri.port !in listOf(-1, 443)) return false
        if (redirect && uri.host in setOf("release-assets.githubusercontent.com", "objects.githubusercontent.com")) return true
        images.any { it.url == url }
    }.getOrDefault(false)

    fun matches(texture: SpaceCompassCelestialTexture, bytes: ByteArray): Boolean =
        bytes.size == texture.bytes && hex(MessageDigest.getInstance("SHA-256").digest(bytes)) == texture.sha256

    /** Unknown content lengths and interrupted requests obey the same strict size bound. */
    fun read(texture: SpaceCompassCelestialTexture, input: InputStream, checkActive: () -> Unit = {}): ByteArray {
        val output = ByteArrayOutputStream(texture.bytes)
        val buffer = ByteArray(8192)
        while (true) {
            checkActive()
            val count = input.read(buffer)
            if (count < 0) break
            require(output.size() + count <= texture.bytes) { "Texture response exceeds its expected size" }
            output.write(buffer, 0, count)
        }
        checkActive()
        return output.toByteArray().also { require(matches(texture, it)) { "Texture response checksum differs" } }
    }
    private fun hex(bytes: ByteArray) = bytes.joinToString("") { "%02x".format(it.toInt() and 255) }
}
