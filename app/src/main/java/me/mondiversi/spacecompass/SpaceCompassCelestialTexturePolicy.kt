package me.mondiversi.spacecompass

import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.net.URI
import java.security.MessageDigest

internal data class SpaceCompassCelestialTexture(val name: String, val bytes: Int,
    val sha256: String, val width: Int, val height: Int,
    val downloadPack: String? = SpaceCompassCelestialTexturePolicy.PUBLISHED_PACK) {
    init {
        require(Regex("[a-z0-9_]+\\.(webp|jpg)").matches(name))
        require(bytes in 1..1_048_576 && Regex("[a-f0-9]{64}").matches(sha256))
        require(width in 1..2048 && height in 1..1024)
        require(downloadPack == null || Regex("celestial-textures-v[0-9]+(?:\\.[0-9]+)*").matches(downloadPack))
    }
    val url: String? get() = downloadPack?.let { "https://github.com/mondiversi/SpaceCompass/releases/download/$it/$name" }
}

/** Versioned public maps. Digests and dimensions belong to this APK, not a remote manifest. */
internal object SpaceCompassCelestialTexturePolicy {
    const val PACK = "celestial-textures-v1.4"
    const val PUBLISHED_PACK = PACK
    val legacyPacks = listOf("celestial-textures-v1.3", "celestial-textures-v1.2.1", "celestial-textures-v1.2", "celestial-textures-v1.1", "celestial-textures-v1")
    const val BASE_URL = "https://github.com/mondiversi/SpaceCompass/releases/download/$PUBLISHED_PACK"
    val images = listOf(
        SpaceCompassCelestialTexture("ceres.webp", 35136, "b0d85dd18be3431f30bb4e4d6b319204fa8c6f539353e9fca53fa9af9795bcbd", 2048, 1024),
        SpaceCompassCelestialTexture("enceladus.webp", 952862, "d3686e9b972e7d60a08397798b009a4d1f0907832e6ffb416e100a05b3dfb0d7", 2048, 1024),
        SpaceCompassCelestialTexture("new_horizons.webp", 188446, "a091840a32cf3bf82e5cfe7b4e1a3c1d12835272fc4f8f63af08f54fd42dc332", 2048, 1024),
        SpaceCompassCelestialTexture("jwst.webp", 257716, "cdf8ec0a9125b7c922a5aaeaaa54c752d898dbd616e953145a501016bf05e6d0", 2048, 1024),
        SpaceCompassCelestialTexture("parker_solar_probe.webp", 112934, "5860724c8fe54da3ada7058c4f9d9ba71d3bc01362886ecaf9c236585c01995b", 2048, 1024),
        SpaceCompassCelestialTexture("apophis.webp", 183724, "224e6e89cbe2c13c43d68feff6923dfd3b9a4d44060571139c51b4bd700d0933", 2048, 1024),
        SpaceCompassCelestialTexture("europa_clipper.webp", 255362, "40ed6980917388248647466d2ee45d131c401a23467c0ec9a88e3170b2db04ea", 2048, 1024),
        SpaceCompassCelestialTexture("hubble.webp", 270612, "0384b18e3daa731c024070757ca39368b45dae456227aa0057b7d90c08ddba13", 2048, 1024),
        SpaceCompassCelestialTexture("m87.webp", 15118, "a285aa6ec25bdc3791164aebf00515b841adc331c33a856f0305a18527a7e1bb", 2048, 1024),
        SpaceCompassCelestialTexture("lv426.webp", 372474, "31ab7d2e87cb27a51e80b00f5dd3b2e21581c617bec900841a97f7b3309ca7fd", 1774, 887),
        SpaceCompassCelestialTexture("tesla_roadster.webp", 157614, "54c1a76dcb92b1512d9dcb1cd273af4771f7f347ff3abdd124383df934de19fe", 2048, 1024),
        SpaceCompassCelestialTexture("titan.webp", 159238, "fbad4d29d878a5d9b9652ab63924eca29b438fbafdfe1c4b43cd7f68d0371aa0", 2048, 1024),
        SpaceCompassCelestialTexture("sirius.webp", 69180, "fafdbd730eb1123152eca8740d312a2eb29f8e6ba9edc6ec259aa585b5e2e91a", 2048, 1024),
        SpaceCompassCelestialTexture("betelgeuse.webp", 16418, "a53115d305780f7f3f941873993a8ae5a660ae12ec4c760fb7febfd3a8c8731b", 2048, 1024),
        SpaceCompassCelestialTexture("orion_nebula.webp", 87134, "e5cf7e78919661ae4c020d41c73119bab8e8e0cda9a9d828303f2a71b04cf441", 2048, 1024),
        SpaceCompassCelestialTexture("pleiades.webp", 356448, "a99de30f51e4d8f21e4d4cf1037a6ef7e7a0d64077fb15a3e91be826eb1a0c63", 2048, 1024),
        SpaceCompassCelestialTexture("andromeda_galaxy.webp", 172094, "50f5f3a8cd87cf76558830fba4f051bb3bf0d9d3f817421fe8b1f6c0f2817e5e", 2048, 1024),
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
    private val byName = images.associateBy { it.name }
    fun texture(name: String?) = byName[name]
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
