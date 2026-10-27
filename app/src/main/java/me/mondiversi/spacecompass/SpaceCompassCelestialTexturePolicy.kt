package me.mondiversi.spacecompass

import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.net.URI
import java.security.MessageDigest

internal data class SpaceCompassCelestialTexture(val name: String, val bytes: Int,
    val sha256: String, val width: Int, val height: Int,
    val downloadPack: String? = SpaceCompassCelestialTexturePolicy.PUBLISHED_PACK, val downloadName: String = name) {
    init {
        require(Regex("[a-z0-9_]+\\.(webp|jpg)").matches(name) && Regex("[a-z0-9_]+\\.(webp|jpg)").matches(downloadName))
        require(bytes in 1..1_048_576 && Regex("[a-f0-9]{64}").matches(sha256))
        require(width in 1..2048 && height in 1..1024)
        require(downloadPack == null || Regex("celestial-textures-v[0-9]+(?:\\.[0-9]+)*").matches(downloadPack))
    }
    val url: String? get() = downloadPack?.let { "https://github.com/mondiversi/SpaceCompass/releases/download/$it/$downloadName" }
}

/** Versioned public maps. Digests and dimensions belong to this APK, not a remote manifest. */
internal object SpaceCompassCelestialTexturePolicy {
    const val PACK = "celestial-textures-v1.7"
    // Versioned individual downloads are published before the matching application release.
    const val PUBLISHED_PACK = PACK
    val legacyPacks = listOf("celestial-textures-v1.6", "celestial-textures-v1.5", "celestial-textures-v1.4", "celestial-textures-v1.3", "celestial-textures-v1.2.1", "celestial-textures-v1.2", "celestial-textures-v1.1", "celestial-textures-v1")
    const val BASE_URL = "https://github.com/mondiversi/SpaceCompass/releases/download/$PUBLISHED_PACK"
    val images = listOf(
        SpaceCompassCelestialTexture("moon_jpl_0502.webp", 389900, "ff03436f223b26cd5930edddd4408b8e4a5fad3826b08d7dedb34b4711667a8f", 2048, 1024, downloadPack = "celestial-textures-v1.6", downloadName = "moon_jpl_0502.webp"),
        SpaceCompassCelestialTexture("moon_jpl_0501.webp", 288958, "0a85feb12c6d85d178b7e01b2c93eba20a546ed7591cf93620a4ffae7be7c462", 2048, 1024, downloadPack = "celestial-textures-v1.6", downloadName = "moon_jpl_0501.webp"),
        SpaceCompassCelestialTexture("planet_jpl_0599.webp", 209714, "d931cf6adcef7a6b6de141a096327f1dd626729ea8842c72cbe5e03f4cb2a0e4", 2048, 1024, downloadPack = "celestial-textures-v1.6", downloadName = "planet_jpl_0599.webp"),
        SpaceCompassCelestialTexture("planet_jpl_0499.webp", 353206, "12a40a86ed8b5711632e177977dd3f0be0e62e349bb788ee48e46d6517a7d24d", 2048, 1024, downloadPack = "celestial-textures-v1.6", downloadName = "planet_jpl_0499.webp"),
        SpaceCompassCelestialTexture("planet_jpl_0199.webp", 625630, "36c730a082f062b4fde11f341304bb7d917288a97821a55514fcdd3c8bc7bc52", 2048, 1024, downloadPack = "celestial-textures-v1.6", downloadName = "planet_jpl_0199.webp"),
        SpaceCompassCelestialTexture("moon_jpl_0301.webp", 426380, "387059f27f6a0bba9c09ba103380819a96ffb12e5d91f9f9936cd4769b2bf65e", 2048, 1024, downloadPack = "celestial-textures-v1.6", downloadName = "moon_jpl_0301.webp"),
        SpaceCompassCelestialTexture("planet_jpl_0899.webp", 27180, "2a1e452cdd803a8cf487363ae43ee58aef910d4713a772f86f0d3d7e45e6400a", 2048, 1024, downloadPack = "celestial-textures-v1.6", downloadName = "planet_jpl_0899.webp"),
        SpaceCompassCelestialTexture("dwarf_134340.webp", 282598, "10694489ab13df79c49a2d49e48246e22ae3aa6d99a07f111ac83ea07808d420", 2048, 1024, downloadPack = "celestial-textures-v1.6", downloadName = "dwarf_134340.webp"),
        SpaceCompassCelestialTexture("planet_jpl_0699.webp", 59112, "baedddf7db60bea91b8e1c98ca90e6742dd1c131d886fa2e94e305ee8aeb531b", 2048, 1024, downloadPack = "celestial-textures-v1.6", downloadName = "planet_jpl_0699.webp"),
        SpaceCompassCelestialTexture("star_jpl_0010.webp", 368406, "ddfdf9ca2f531dad52c163e012b7b522deb96700e876f87bb74aa0fc42af10a5", 2048, 1024, downloadPack = "celestial-textures-v1.6", downloadName = "star_jpl_0010.webp"),
        SpaceCompassCelestialTexture("planet_jpl_0799.webp", 11406, "fa9608375c2c5805fdd23d879151e6ad187a142474c8298f28441df66740660c", 2048, 1024, downloadPack = "celestial-textures-v1.6", downloadName = "planet_jpl_0799.webp"),
        SpaceCompassCelestialTexture("planet_jpl_0299.webp", 66736, "580dfe532a07f54cea30e608dc6f260de39474dff0cfdfc86643b4bfb51eb85d", 2048, 1024, downloadPack = "celestial-textures-v1.6", downloadName = "planet_jpl_0299.webp"),
        SpaceCompassCelestialTexture("moon_jpl_0606.webp", 159238, "fbad4d29d878a5d9b9652ab63924eca29b438fbafdfe1c4b43cd7f68d0371aa0", 2048, 1024, downloadPack = "celestial-textures-v1.6", downloadName = "moon_jpl_0606.webp"),
        SpaceCompassCelestialTexture("star_hip_032349.webp", 69180, "fafdbd730eb1123152eca8740d312a2eb29f8e6ba9edc6ec259aa585b5e2e91a", 2048, 1024, downloadPack = "celestial-textures-v1.6", downloadName = "star_hip_032349.webp"),
        SpaceCompassCelestialTexture("star_hip_027989.webp", 16418, "a53115d305780f7f3f941873993a8ae5a660ae12ec4c760fb7febfd3a8c8731b", 2048, 1024, downloadPack = "celestial-textures-v1.6", downloadName = "star_hip_027989.webp"),
        SpaceCompassCelestialTexture("nebula_ngc_1976.webp", 87134, "e5cf7e78919661ae4c020d41c73119bab8e8e0cda9a9d828303f2a71b04cf441", 2048, 1024, downloadPack = "celestial-textures-v1.6", downloadName = "nebula_ngc_1976.webp"),
        SpaceCompassCelestialTexture("cluster_melotte_0022.webp", 356448, "a99de30f51e4d8f21e4d4cf1037a6ef7e7a0d64077fb15a3e91be826eb1a0c63", 2048, 1024, downloadPack = "celestial-textures-v1.6", downloadName = "cluster_melotte_0022.webp"),
        SpaceCompassCelestialTexture("galaxy_ngc_0224.webp", 172094, "50f5f3a8cd87cf76558830fba4f051bb3bf0d9d3f817421fe8b1f6c0f2817e5e", 2048, 1024, downloadPack = "celestial-textures-v1.6", downloadName = "galaxy_ngc_0224.webp"),
        SpaceCompassCelestialTexture("satellite_jpl_m143205.webp", 157614, "54c1a76dcb92b1512d9dcb1cd273af4771f7f347ff3abdd124383df934de19fe", 2048, 1024, downloadPack = "celestial-textures-v1.6", downloadName = "satellite_jpl_m143205.webp"),
        SpaceCompassCelestialTexture("fictional_lv_426.webp", 372474, "31ab7d2e87cb27a51e80b00f5dd3b2e21581c617bec900841a97f7b3309ca7fd", 1774, 887, downloadPack = "celestial-textures-v1.6", downloadName = "fictional_lv_426.webp"),
        SpaceCompassCelestialTexture("probe_jpl_m0098.webp", 188446, "a091840a32cf3bf82e5cfe7b4e1a3c1d12835272fc4f8f63af08f54fd42dc332", 2048, 1024, downloadPack = "celestial-textures-v1.6", downloadName = "probe_jpl_m0098.webp"),
        SpaceCompassCelestialTexture("satellite_jpl_m0170.webp", 257716, "cdf8ec0a9125b7c922a5aaeaaa54c752d898dbd616e953145a501016bf05e6d0", 2048, 1024, downloadPack = "celestial-textures-v1.6", downloadName = "satellite_jpl_m0170.webp"),
        SpaceCompassCelestialTexture("probe_jpl_m0096.webp", 112934, "5860724c8fe54da3ada7058c4f9d9ba71d3bc01362886ecaf9c236585c01995b", 2048, 1024, downloadPack = "celestial-textures-v1.6", downloadName = "probe_jpl_m0096.webp"),
        SpaceCompassCelestialTexture("asteroid_99942.webp", 183724, "224e6e89cbe2c13c43d68feff6923dfd3b9a4d44060571139c51b4bd700d0933", 2048, 1024, downloadPack = "celestial-textures-v1.6", downloadName = "asteroid_99942.webp"),
        SpaceCompassCelestialTexture("probe_jpl_m0159.webp", 255362, "40ed6980917388248647466d2ee45d131c401a23467c0ec9a88e3170b2db04ea", 2048, 1024, downloadPack = "celestial-textures-v1.6", downloadName = "probe_jpl_m0159.webp"),
        SpaceCompassCelestialTexture("satellite_norad_20580.webp", 270612, "0384b18e3daa731c024070757ca39368b45dae456227aa0057b7d90c08ddba13", 2048, 1024, downloadPack = "celestial-textures-v1.6", downloadName = "satellite_norad_20580.webp"),
        SpaceCompassCelestialTexture("black_hole_m87.webp", 15118, "a285aa6ec25bdc3791164aebf00515b841adc331c33a856f0305a18527a7e1bb", 2048, 1024, downloadPack = "celestial-textures-v1.6", downloadName = "black_hole_m87.webp"),
        SpaceCompassCelestialTexture("dwarf_1.webp", 35136, "b0d85dd18be3431f30bb4e4d6b319204fa8c6f539353e9fca53fa9af9795bcbd", 2048, 1024, downloadPack = "celestial-textures-v1.6", downloadName = "dwarf_1.webp"),
        SpaceCompassCelestialTexture("moon_jpl_0602.webp", 952862, "d3686e9b972e7d60a08397798b009a4d1f0907832e6ffb416e100a05b3dfb0d7", 2048, 1024, downloadPack = "celestial-textures-v1.6", downloadName = "moon_jpl_0602.webp"),
        SpaceCompassCelestialTexture("moon_jpl_0504.webp", 96988, "af481ff12b976507a11dedfef8c78c53b41d929ef37e546cadaada28d3a819dd", 2048, 1024, downloadPack = "celestial-textures-v1.6", downloadName = "moon_jpl_0504.webp"),
        SpaceCompassCelestialTexture("nebula_ngc_3372.webp", 390554, "0b9221079b503e12824db652d9dd000cb62addc3fa80368a2aad96769b1935bb", 2048, 1024, downloadPack = "celestial-textures-v1.6", downloadName = "nebula_ngc_3372.webp"),
        SpaceCompassCelestialTexture("moon_jpl_0901.webp", 129682, "07190b4b634b3064dde79a6cba644bd143ad8528a2aaf4c0fe02ccd198b0a255", 2048, 1024, downloadPack = "celestial-textures-v1.6", downloadName = "moon_jpl_0901.webp"),
        SpaceCompassCelestialTexture("moon_jpl_0402.webp", 54414, "75de62d3de407bcf7bf17640ce2755403cf6e62c163464be61816a8925ac6ff6", 2048, 1024, downloadPack = "celestial-textures-v1.6", downloadName = "moon_jpl_0402.webp"),
        SpaceCompassCelestialTexture("moon_jpl_0503.webp", 56570, "156aecd764b6e61aa89699ee905f79aa1e84b0c72a02279345ace74557301fa2", 2048, 1024, downloadPack = "celestial-textures-v1.6", downloadName = "moon_jpl_0503.webp"),
        SpaceCompassCelestialTexture("comet_c_1995_o1.webp", 246890, "27d25a69994b3592f1e81ce6a5c5f3fc3f2cba7659f5c82850e8e7f6459564b4", 2048, 1024, downloadPack = "celestial-textures-v1.6", downloadName = "comet_c_1995_o1.webp"),
        SpaceCompassCelestialTexture("nebula_ic_0434.webp", 104174, "351b57fed85d58ca47b5feae939fb0696142f55ce99c08db3fe56e984cc8eb3a", 2048, 1024, downloadPack = "celestial-textures-v1.6", downloadName = "nebula_ic_0434.webp"),
        SpaceCompassCelestialTexture("moon_jpl_0608.webp", 146076, "5f75c718b688d805678b58176f0644348e75239074d51b226972b85a6d064a76", 2048, 1024, downloadPack = "celestial-textures-v1.6", downloadName = "moon_jpl_0608.webp"),
        SpaceCompassCelestialTexture("galaxy_lmc.webp", 355414, "64e0427ec40b014c6fa2eb2594e9b7b92b360e726dc1bed25df48822bf77fd23", 2048, 1024, downloadPack = "celestial-textures-v1.6", downloadName = "galaxy_lmc.webp"),
        SpaceCompassCelestialTexture("probe_jpl_m0049.webp", 112384, "02038241ed60b59d163740574dfe7bffbcdf02a8b5f914ad4f2e936ce34614c0", 2048, 1024, downloadPack = "celestial-textures-v1.6", downloadName = "probe_jpl_m0049.webp"),
        SpaceCompassCelestialTexture("nebula_ngc_1952.webp", 230040, "ea7273ab1e3a794cea9a72d99d3aa8b925bf36279468e48baf66f46463e33afe", 2048, 1024, downloadPack = "celestial-textures-v1.6", downloadName = "nebula_ngc_1952.webp"),
        SpaceCompassCelestialTexture("cluster_ngc_6205.webp", 581348, "386857ec93fc90ab480cd02980e82e4cb4308a869f471bc77690da68e2907d1f", 2048, 1024, downloadPack = "celestial-textures-v1.6", downloadName = "cluster_ngc_6205.webp"),
        SpaceCompassCelestialTexture("nebula_ngc_6853.webp", 117660, "3f547112d700ff603fc06576d7e82dfe5f4c7a8039aef61a6756d24794c83d6f", 2048, 1024, downloadPack = "celestial-textures-v1.6", downloadName = "nebula_ngc_6853.webp"),
        SpaceCompassCelestialTexture("galaxy_ngc_5194.webp", 131488, "e36a403971f90e820db34addc8d35f63b759e3717a0f22009731515f08476dc3", 2048, 1024, downloadPack = "celestial-textures-v1.6", downloadName = "galaxy_ngc_5194.webp"),
        SpaceCompassCelestialTexture("nebula_ngc_6720.webp", 70996, "a6631fd9cdf01b4a1995065d9872cd44230e275ac6fc313a63bd20cf96334693", 2048, 1024, downloadPack = "celestial-textures-v1.6", downloadName = "nebula_ngc_6720.webp"),
        SpaceCompassCelestialTexture("moon_jpl_0705.webp", 89462, "62864b0555f2296fe8b3d5899255e3aed314018a964e0bccc3215f8191fd02b2", 2048, 1024, downloadPack = "celestial-textures-v1.6", downloadName = "moon_jpl_0705.webp"),
        SpaceCompassCelestialTexture("cluster_ngc_5139.webp", 318454, "7fd79eadeb3a4b51d3f5e740bd98d9d941a541b4e43781dbe3e34e02b0238631", 2048, 1024, downloadPack = "celestial-textures-v1.6", downloadName = "cluster_ngc_5139.webp"),
        SpaceCompassCelestialTexture("moon_jpl_0401.webp", 102498, "f7f2895886d8f223cc468082912fff28e891b049c15ad3997dcec63d469eb6c0", 2048, 1024, downloadPack = "celestial-textures-v1.6", downloadName = "moon_jpl_0401.webp"),
        SpaceCompassCelestialTexture("probe_jpl_m0255.webp", 280462, "fcb8abec2e56d6c53c639bce08533a416eb1cdbc467b4fdb28aac7be90324606", 2048, 1024, downloadPack = "celestial-textures-v1.6", downloadName = "probe_jpl_m0255.webp"),
        SpaceCompassCelestialTexture("moon_jpl_0605.webp", 252646, "c0a22f2e5cea42fe6cdd2a519e67502b9ad513bbadbc0b62c0591a4c77008964", 2048, 1024, downloadPack = "celestial-textures-v1.6", downloadName = "moon_jpl_0605.webp"),
        SpaceCompassCelestialTexture("galaxy_smc.webp", 403268, "7a13d94d931b0860f648677a28db66609bade99d69ca61d14a48e1cbc1199939", 2048, 1024, downloadPack = "celestial-textures-v1.6", downloadName = "galaxy_smc.webp"),
        SpaceCompassCelestialTexture("probe_jpl_m0144.webp", 140190, "675092f8ee104c113dafb75f2d514e7936474fec91a56a0ef64ac594de511cf5", 2048, 1024, downloadPack = "celestial-textures-v1.6", downloadName = "probe_jpl_m0144.webp"),
        SpaceCompassCelestialTexture("moon_jpl_0703.webp", 61490, "dd062b12e79780356b81357a1a60acbe6c598140939c0ec18ae2ff3f7944a0ab", 2048, 1024, downloadPack = "celestial-textures-v1.6", downloadName = "moon_jpl_0703.webp"),
        SpaceCompassCelestialTexture("moon_jpl_0801.webp", 248432, "40a915b3d29f8498265692b37804db868397361af432a35043da77f9c49644be", 2048, 1024, downloadPack = "celestial-textures-v1.6", downloadName = "moon_jpl_0801.webp"),
        SpaceCompassCelestialTexture("asteroid_4.webp", 81086, "85bd4f69a0088462011ef98fba111f735b03e844faf0506c2a408c5ada47ecaa", 2048, 1024, downloadPack = "celestial-textures-v1.6", downloadName = "asteroid_4.webp"),
        SpaceCompassCelestialTexture("galaxy_ngc_4486.webp", 34326, "6424ca05724563be0eb078846bdea16c641e5643b0c17582c11b2969fa55f155", 2048, 1024, downloadPack = "celestial-textures-v1.6", downloadName = "galaxy_ngc_4486.webp"),
        SpaceCompassCelestialTexture("galaxy_milky_way.webp", 526782, "ae5eca8e9b89a5445cf7be63040db0c2ee369334deb88b2844b7d14a6fb6961b", 2048, 1024, downloadPack = "celestial-textures-v1.6", downloadName = "galaxy_milky_way.webp"),
        SpaceCompassCelestialTexture("asteroid_243.webp", 24674, "dd72ea884f037c365b74478c6913ae5884b4403fc475ee9a65005ca7a3a3fca3", 2048, 1024, downloadPack = "celestial-textures-v1.6", downloadName = "asteroid_243.webp"),
        SpaceCompassCelestialTexture("asteroid_3200.webp", 289724, "9ecc3556b405d0736837370d79a4070259fa12ab250bdcb8da0a96283edc6bce", 2048, 1024, downloadPack = "celestial-textures-v1.6", downloadName = "asteroid_3200.webp"),
        SpaceCompassCelestialTexture("cluster_ngc_1904.webp", 405894, "8c0d516488b27636d960b397c386c9be24ee53129cce896aed7ba1cd502ba3bb", 2048, 1024, downloadPack = "celestial-textures-v1.6", downloadName = "cluster_ngc_1904.webp"),
        SpaceCompassCelestialTexture("cluster_ngc_2168.webp", 70098, "8d3e1a2dee4484f01c62ae14166369ecc59df0c29a9d68d7ce7c1aedda389aed", 2048, 1024, downloadPack = "celestial-textures-v1.6", downloadName = "cluster_ngc_2168.webp"),
        SpaceCompassCelestialTexture("cluster_ngc_2548.webp", 72130, "b0fef4f13d66234e07f4c0a113ae6de7575c6fc99e5e2e5e8ec4246c6097a653", 2048, 1024, downloadPack = "celestial-textures-v1.6", downloadName = "cluster_ngc_2548.webp"),
        SpaceCompassCelestialTexture("cluster_ngc_2632.webp", 62112, "cd00eccdd5772b774f078917fd593ce4aed8ce30dab6abf29479f8196b37f270", 2048, 1024, downloadPack = "celestial-textures-v1.6", downloadName = "cluster_ngc_2632.webp"),
        SpaceCompassCelestialTexture("cluster_ngc_2682.webp", 97572, "4b4b3985e83d9e2de73bf8d79049bfb1d7b79d195228e4d6d5f5d4a620f686d3", 2048, 1024, downloadPack = "celestial-textures-v1.6", downloadName = "cluster_ngc_2682.webp"),
        SpaceCompassCelestialTexture("cluster_ngc_4590.webp", 498434, "6618a3defbe20512ec65dd52fe62026baf0e987975ecc9503ffea8dba533d39e", 2048, 1024, downloadPack = "celestial-textures-v1.6", downloadName = "cluster_ngc_4590.webp"),
        SpaceCompassCelestialTexture("cluster_ngc_5024.webp", 531266, "ebb09c2e91a24e31106066d7b8c31f56dc5fb3f3faa1ac91b1b3344c16c40b54", 2048, 1024, downloadPack = "celestial-textures-v1.6", downloadName = "cluster_ngc_5024.webp"),
        SpaceCompassCelestialTexture("cluster_ngc_5272.webp", 617064, "1de97621521ad3ce3d5d77ba9620083cf1027925c901c4294606eff0de6ff22c", 2048, 1024, downloadPack = "celestial-textures-v1.6", downloadName = "cluster_ngc_5272.webp"),
        SpaceCompassCelestialTexture("cluster_ngc_5904.webp", 473316, "8d060e77612a75732b97af7f69d0667dd04bce963b095e15d9ff271f7bc1dec0", 2048, 1024, downloadPack = "celestial-textures-v1.6", downloadName = "cluster_ngc_5904.webp"),
        SpaceCompassCelestialTexture("cluster_ngc_6093.webp", 370008, "5f701413707ed4928e58807758552065c296fbd725a618e21e2c5e6d2bef4544", 2048, 1024, downloadPack = "celestial-textures-v1.6", downloadName = "cluster_ngc_6093.webp"),
        SpaceCompassCelestialTexture("cluster_ngc_6121.webp", 304408, "97855653c8fdfc1169ddeed91734400427cd1b74b70f83fc2ee5f0a596f5398b", 2048, 1024, downloadPack = "celestial-textures-v1.6", downloadName = "cluster_ngc_6121.webp"),
        SpaceCompassCelestialTexture("cluster_ngc_6171.webp", 367012, "44599adc66acd1d0c23f82b6be2f5cb4f5cc2661adfd33cd30230a9217094b84", 2048, 1024, downloadPack = "celestial-textures-v1.6", downloadName = "cluster_ngc_6171.webp"),
        SpaceCompassCelestialTexture("cluster_ngc_6218.webp", 495254, "d6582392f6aa5fedad709dc784ce157248f75150049da3763929ea6a903a34ed", 2048, 1024, downloadPack = "celestial-textures-v1.6", downloadName = "cluster_ngc_6218.webp"),
        SpaceCompassCelestialTexture("cluster_ngc_6266.webp", 489018, "b4f77e083df84fc9289b34a8561be3751c062f3f078bb25714e80c23d2b3c717", 2048, 1024, downloadPack = "celestial-textures-v1.6", downloadName = "cluster_ngc_6266.webp"),
        SpaceCompassCelestialTexture("cluster_ngc_6273.webp", 515928, "e371295a544d23a760d3739483e51b69ced38d558aeb4446dd1f77d8e7bdf0e4", 2048, 1024, downloadPack = "celestial-textures-v1.6", downloadName = "cluster_ngc_6273.webp"),
        SpaceCompassCelestialTexture("cluster_ngc_6333.webp", 501674, "4558ecebd79acd8d8bd8abf03d9352e0a760e568d4529d986662ef53e340ba31", 2048, 1024, downloadPack = "celestial-textures-v1.6", downloadName = "cluster_ngc_6333.webp"),
        SpaceCompassCelestialTexture("cluster_ngc_6341.webp", 508720, "ddcb0e6f8ca1fcba3bd5c7218375d894221e0b80f45ef1190bb3fefb468172b3", 2048, 1024, downloadPack = "celestial-textures-v1.6", downloadName = "cluster_ngc_6341.webp"),
        SpaceCompassCelestialTexture("cluster_ngc_6402.webp", 488516, "c557bfcbe9beed7ac83a2aa47a9f8a5030bc7490e20388665724cbd0c0db8ec5", 2048, 1024, downloadPack = "celestial-textures-v1.6", downloadName = "cluster_ngc_6402.webp"),
        SpaceCompassCelestialTexture("cluster_ngc_6475.webp", 306586, "df7d55b9b544cd9fbe502fac467d3ab34fe51fe9786b67ead84d102575f9fe3c", 2048, 1024, downloadPack = "celestial-textures-v1.6", downloadName = "cluster_ngc_6475.webp"),
        SpaceCompassCelestialTexture("cluster_ngc_6626.webp", 503130, "9c3b32be1a16afae275ea78030990c394bb401f48e657524f82f43171659b9a9", 2048, 1024, downloadPack = "celestial-textures-v1.6", downloadName = "cluster_ngc_6626.webp"),
        SpaceCompassCelestialTexture("cluster_ngc_6637.webp", 406168, "9b4f738212c23d3e4386d8aea7ba56344d8c8286acc7c0d0a19ca64637625e18", 2048, 1024, downloadPack = "celestial-textures-v1.6", downloadName = "cluster_ngc_6637.webp"),
        SpaceCompassCelestialTexture("cluster_ngc_6656.webp", 506350, "55d0f6bd863342fb1ac5df558accc663ba102a3a83f10981877f8e4d62677dae", 2048, 1024, downloadPack = "celestial-textures-v1.6", downloadName = "cluster_ngc_6656.webp"),
        SpaceCompassCelestialTexture("cluster_ngc_6681.webp", 418204, "d85e145fbefe66fc38cda30387d511f1e9326a2f193da7eccb19abd460fde34c", 2048, 1024, downloadPack = "celestial-textures-v1.6", downloadName = "cluster_ngc_6681.webp"),
        SpaceCompassCelestialTexture("cluster_ngc_6705.webp", 302084, "4c70982836e36c13261f1d79664054d93282658001a237573c3a27dc407c894d", 2048, 1024, downloadPack = "celestial-textures-v1.6", downloadName = "cluster_ngc_6705.webp"),
        SpaceCompassCelestialTexture("cluster_ngc_6715.webp", 518144, "08544e527b2d78c2c0f6ded9027e816ffd26524e9add76166a20ec7454998a3d", 2048, 1024, downloadPack = "celestial-textures-v1.6", downloadName = "cluster_ngc_6715.webp"),
        SpaceCompassCelestialTexture("cluster_ngc_6779.webp", 456306, "1f4b06ea8844a139f0a8f353e15b2e873d2ed9c12419059635bd57815af6534e", 2048, 1024, downloadPack = "celestial-textures-v1.6", downloadName = "cluster_ngc_6779.webp"),
        SpaceCompassCelestialTexture("cluster_ngc_6809.webp", 527252, "0c187e459999457b4a84d0b1855922dad6ddee91dc6ffe77bfe9bba9d66c5135", 2048, 1024, downloadPack = "celestial-textures-v1.6", downloadName = "cluster_ngc_6809.webp"),
        SpaceCompassCelestialTexture("cluster_ngc_6838.webp", 224490, "2311eb86a9d4d30521c612d146a2eff77866babb659e2263eca278d9cd3006d6", 2048, 1024, downloadPack = "celestial-textures-v1.6", downloadName = "cluster_ngc_6838.webp"),
        SpaceCompassCelestialTexture("cluster_ngc_6864.webp", 616160, "cbff3c89172acb7e3faa6097f11dbe486adf24ad7de47284d173d024e6bbfaac", 2048, 1024, downloadPack = "celestial-textures-v1.6", downloadName = "cluster_ngc_6864.webp"),
        SpaceCompassCelestialTexture("cluster_ngc_6981.webp", 436392, "916292d9cae174a32af8c89ffdec101d8a34b8f49490713e18df8f7ee290db2e", 2048, 1024, downloadPack = "celestial-textures-v1.6", downloadName = "cluster_ngc_6981.webp"),
        SpaceCompassCelestialTexture("cluster_ngc_7078.webp", 530016, "04c06373eee6e234a000eef8e08849c058ffc77e28833b71dcb61a7fcc38ac1f", 2048, 1024, downloadPack = "celestial-textures-v1.6", downloadName = "cluster_ngc_7078.webp"),
        SpaceCompassCelestialTexture("cluster_ngc_7089.webp", 478496, "b13277afa64bbcdebd2dfe24bd73e34418414ec7790c9d0119da38469ff2e446", 2048, 1024, downloadPack = "celestial-textures-v1.6", downloadName = "cluster_ngc_7089.webp"),
        SpaceCompassCelestialTexture("cluster_ngc_7099.webp", 321306, "54d9af6f5a4bff3bfd451747f349e1d497d53902877771789249f0d44a8c830b", 2048, 1024, downloadPack = "celestial-textures-v1.6", downloadName = "cluster_ngc_7099.webp"),
        SpaceCompassCelestialTexture("galaxy_ngc_0205.webp", 375990, "81eec80f93782604b4858138058e159bcb90f8534597d1d70d2b988a2967dbe2", 2048, 1024, downloadPack = "celestial-textures-v1.6", downloadName = "galaxy_ngc_0205.webp"),
        SpaceCompassCelestialTexture("galaxy_ngc_0221.webp", 161462, "37113cd8601e63a920c8ad6fcc55599b25a56f19500aa7443b03d3f99b872e1a", 2048, 1024, downloadPack = "celestial-textures-v1.6", downloadName = "galaxy_ngc_0221.webp"),
        SpaceCompassCelestialTexture("galaxy_ngc_0247.webp", 299034, "5d933898c514130198b540c025197f6551527ebaa6396195b66109280dfce3ec", 2048, 1024, downloadPack = "celestial-textures-v1.6", downloadName = "galaxy_ngc_0247.webp"),
        SpaceCompassCelestialTexture("galaxy_ngc_0598.webp", 780796, "5a5e22c120bf8a764a01513dd4c9be5a07ea09769c26a3fc329721ffdb15b799", 2048, 1024, downloadPack = "celestial-textures-v1.6", downloadName = "galaxy_ngc_0598.webp"),
        SpaceCompassCelestialTexture("galaxy_ngc_0628.webp", 437084, "b9e014ed050ed8026cfa4bf0cd701693d6bd78038ba0666c7e6e6f1869553cca", 2048, 1024, downloadPack = "celestial-textures-v1.6", downloadName = "galaxy_ngc_0628.webp"),
        SpaceCompassCelestialTexture("galaxy_ngc_1068.webp", 178306, "ebe95dbb02249a109280ab6c346fa6b30e637ab2462759df42bc87f4faf1e1f9", 2048, 1024, downloadPack = "celestial-textures-v1.6", downloadName = "galaxy_ngc_1068.webp"),
        SpaceCompassCelestialTexture("galaxy_ngc_3031.webp", 182536, "c3327ffa0ce2bb22803482f5498eff3edc7cb65cb83a141e362052fef8fa968a", 2048, 1024, downloadPack = "celestial-textures-v1.6", downloadName = "galaxy_ngc_3031.webp"),
        SpaceCompassCelestialTexture("galaxy_ngc_3034.webp", 218562, "bfbaa5b0188ed3860845b3e573e949ef77fb8936c35aa46d3b3b6f772679ad46", 2048, 1024, downloadPack = "celestial-textures-v1.6", downloadName = "galaxy_ngc_3034.webp"),
        SpaceCompassCelestialTexture("galaxy_ngc_3351.webp", 284450, "f9b8d40b8116590f31532fa639f0867cd26b731a7370985b625066e04d42002b", 2048, 1024, downloadPack = "celestial-textures-v1.6", downloadName = "galaxy_ngc_3351.webp"),
        SpaceCompassCelestialTexture("galaxy_ngc_3368.webp", 199780, "d66aed8d6bdac09b18fc6ff91539b3e82b4b61c0baf593e9beddb6b4cf323bbe", 2048, 1024, downloadPack = "celestial-textures-v1.6", downloadName = "galaxy_ngc_3368.webp"),
        SpaceCompassCelestialTexture("galaxy_ngc_3379.webp", 28622, "15996a6cd932b1783f3e2ce537f53a1fdf98759135bc0ae202395886df2a35c0", 2048, 1024, downloadPack = "celestial-textures-v1.6", downloadName = "galaxy_ngc_3379.webp"),
        SpaceCompassCelestialTexture("galaxy_ngc_3556.webp", 236146, "e65b0d7c08bc5b3078324ea60405a538ffe4aac0d3fe07c930e414049b11f1a6", 2048, 1024, downloadPack = "celestial-textures-v1.6", downloadName = "galaxy_ngc_3556.webp"),
        SpaceCompassCelestialTexture("galaxy_ngc_3623.webp", 76288, "b8b0a007991037e8975561ab1bfc7018d5e51d9bf82fcc03efbc2c8b59be002a", 2048, 1024, downloadPack = "celestial-textures-v1.6", downloadName = "galaxy_ngc_3623.webp"),
        SpaceCompassCelestialTexture("galaxy_ngc_3627.webp", 291092, "9f95e778987c857064e52dc4a1d23def89d32d1c75d6685ebedf224b0be3beef", 2048, 1024, downloadPack = "celestial-textures-v1.6", downloadName = "galaxy_ngc_3627.webp"),
        SpaceCompassCelestialTexture("galaxy_ngc_3992.webp", 166658, "55e36b6872071d79077e8b7f04122bd09a2e5cd71c671fe89565b03a2ad91113", 2048, 1024, downloadPack = "celestial-textures-v1.6", downloadName = "galaxy_ngc_3992.webp"),
        SpaceCompassCelestialTexture("galaxy_ngc_4192.webp", 396014, "76b3500bf89161c7104e69f6b6bbf372563454521d9a69d1e4394f013baa0c58", 2048, 1024, downloadPack = "celestial-textures-v1.6", downloadName = "galaxy_ngc_4192.webp"),
        SpaceCompassCelestialTexture("galaxy_ngc_4254.webp", 378230, "b43f34e15e64a029bf41c13c514b273ce822f1d6dc0cb91bf70d4eefd18aacec", 2048, 1024, downloadPack = "celestial-textures-v1.6", downloadName = "galaxy_ngc_4254.webp"),
        SpaceCompassCelestialTexture("galaxy_ngc_4258.webp", 176368, "e4eb43c3fdde32db3c574b891ab2d92ebb2e2323836dfda288f2f95e80638302", 2048, 1024, downloadPack = "celestial-textures-v1.6", downloadName = "galaxy_ngc_4258.webp"),
        SpaceCompassCelestialTexture("galaxy_ngc_4303.webp", 370318, "176733986ffe25e3e776fae6e06fcffd3b1b7838303f676085ec20328e7c8d4a", 2048, 1024, downloadPack = "celestial-textures-v1.6", downloadName = "galaxy_ngc_4303.webp"),
        SpaceCompassCelestialTexture("galaxy_ngc_4321.webp", 223710, "234191a56af20fd83adf1bf22ddddd3cbec63a3fcae27c4e253e8b757416f761", 2048, 1024, downloadPack = "celestial-textures-v1.6", downloadName = "galaxy_ngc_4321.webp"),
        SpaceCompassCelestialTexture("galaxy_ngc_4374.webp", 29840, "69736a1c70e351c56e42fbcfd4a160a18cbc9a8e1d6f13c23bd151514378eee0", 2048, 1024, downloadPack = "celestial-textures-v1.6", downloadName = "galaxy_ngc_4374.webp"),
        SpaceCompassCelestialTexture("galaxy_ngc_4382.webp", 23528, "7faae53092fe0ff2349507868aee414c175fd557c1df0821930fdb404bf95de4", 2048, 1024, downloadPack = "celestial-textures-v1.6", downloadName = "galaxy_ngc_4382.webp"),
        SpaceCompassCelestialTexture("galaxy_ngc_4406.webp", 25796, "9ea7c40776f8fcc4e41e609d5921d33edd53e83e48ab4e0944a4bb975e5bd112", 2048, 1024, downloadPack = "celestial-textures-v1.6", downloadName = "galaxy_ngc_4406.webp"),
        SpaceCompassCelestialTexture("galaxy_ngc_4472.webp", 49228, "72e87d8fbf6dc07e06f0f6bf2a63019512a03ebbb2b51178c158120fb0c631a8", 2048, 1024, downloadPack = "celestial-textures-v1.6", downloadName = "galaxy_ngc_4472.webp"),
        SpaceCompassCelestialTexture("galaxy_ngc_4501.webp", 516910, "0bd56dc035a221f06793ea82b0e3f83f49450ab43a9259d7cea41736a311305c", 2048, 1024, downloadPack = "celestial-textures-v1.6", downloadName = "galaxy_ngc_4501.webp"),
        SpaceCompassCelestialTexture("galaxy_ngc_4548.webp", 352798, "4288e02a42114f04ca155b1ecedf26bcace6d474b116f5632ad5892332cc9cd1", 2048, 1024, downloadPack = "celestial-textures-v1.6", downloadName = "galaxy_ngc_4548.webp"),
        SpaceCompassCelestialTexture("galaxy_ngc_4552.webp", 42712, "affbaba63c3cd8ae36b9ca4778a06be0796c13002f7fefbbc31596d617c83f3d", 2048, 1024, downloadPack = "celestial-textures-v1.6", downloadName = "galaxy_ngc_4552.webp"),
        SpaceCompassCelestialTexture("galaxy_ngc_4569.webp", 147050, "3e52455f895950d69095bfceb6ef911e2b6997b2a5220229b0bf1f066d12c320", 2048, 1024, downloadPack = "celestial-textures-v1.6", downloadName = "galaxy_ngc_4569.webp"),
        SpaceCompassCelestialTexture("galaxy_ngc_4579.webp", 243458, "c6573a734316d3b18aaf2f286d5d2e5b9667a33a1503df118b51ccf87ebd94ec", 2048, 1024, downloadPack = "celestial-textures-v1.6", downloadName = "galaxy_ngc_4579.webp"),
        SpaceCompassCelestialTexture("galaxy_ngc_4594.webp", 108026, "667902121cc92db61a202777a8af97e6d061674bc85bcfcab7fa1688aa53dead", 2048, 1024, downloadPack = "celestial-textures-v1.6", downloadName = "galaxy_ngc_4594.webp"),
        SpaceCompassCelestialTexture("galaxy_ngc_4621.webp", 36462, "f1ab763d0edaa5276381b45376286815db1461ac68918f53f45590edbb9508ac", 2048, 1024, downloadPack = "celestial-textures-v1.6", downloadName = "galaxy_ngc_4621.webp"),
        SpaceCompassCelestialTexture("galaxy_ngc_4649.webp", 80470, "a7fa1c96682698fed0844ed81624c0dbe33d0f041cb9ac9e739ced782798fd5d", 2048, 1024, downloadPack = "celestial-textures-v1.6", downloadName = "galaxy_ngc_4649.webp"),
        SpaceCompassCelestialTexture("galaxy_ngc_4736.webp", 395626, "0fd62e675c90b0ac3e35be3d7c582c0c80b4c65c14f8a30d7539228a32126045", 2048, 1024, downloadPack = "celestial-textures-v1.6", downloadName = "galaxy_ngc_4736.webp"),
        SpaceCompassCelestialTexture("galaxy_ngc_4826.webp", 109044, "626dc4335d0c823f8144aa691967c21c8fd37043058a6d111185130aabfce021", 2048, 1024, downloadPack = "celestial-textures-v1.6", downloadName = "galaxy_ngc_4826.webp"),
        SpaceCompassCelestialTexture("galaxy_ngc_5055.webp", 360974, "ce07eb17515161909aad751029c908ecf3c174093c731ecbb800fd89c7d131d2", 2048, 1024, downloadPack = "celestial-textures-v1.6", downloadName = "galaxy_ngc_5055.webp"),
        SpaceCompassCelestialTexture("galaxy_ngc_5236.webp", 618964, "26fa5cb4922f61708ac19719c1d597dfeb72544aa3d0068652131458e01d9622", 2048, 1024, downloadPack = "celestial-textures-v1.6", downloadName = "galaxy_ngc_5236.webp"),
        SpaceCompassCelestialTexture("galaxy_ngc_5457.webp", 304598, "64834d36f47a5318ce007804cd0717e461290b5996dd491a5ec7feb13f2e9faa", 2048, 1024, downloadPack = "celestial-textures-v1.6", downloadName = "galaxy_ngc_5457.webp"),
        SpaceCompassCelestialTexture("moon_jpl_0505.webp", 7694, "f084d373d0fe186ea872887022f488cdacd10a65df47059aa753a8f46852c95c", 2048, 1024, downloadPack = "celestial-textures-v1.6", downloadName = "moon_jpl_0505.webp"),
        SpaceCompassCelestialTexture("moon_jpl_0506.webp", 65646, "9812b6bcfbaeb71980f346dcfe67219f65285c012cad73d4044e36fe18c359a8", 2048, 1024, downloadPack = "celestial-textures-v1.6", downloadName = "moon_jpl_0506.webp"),
        SpaceCompassCelestialTexture("moon_jpl_0601.webp", 33058, "94224b6c88529b440292456ebc9d43ef4ca69f13db815b903104d3648e4038bd", 2048, 1024, downloadPack = "celestial-textures-v1.6", downloadName = "moon_jpl_0601.webp"),
        SpaceCompassCelestialTexture("moon_jpl_0603.webp", 148034, "b9629cd305048bc9cd50340cda7f95f51b79feda3665f6d4514f15494df99ed0", 2048, 1024, downloadPack = "celestial-textures-v1.6", downloadName = "moon_jpl_0603.webp"),
        SpaceCompassCelestialTexture("moon_jpl_0604.webp", 35206, "2400a948f071edf78a2c19e5e94bf2301cab15c884bab0b6f67978e607d7519c", 2048, 1024, downloadPack = "celestial-textures-v1.6", downloadName = "moon_jpl_0604.webp"),
        SpaceCompassCelestialTexture("moon_jpl_0607.webp", 134842, "16f7842e7cdab7e90eab915449c70b278ff1e4ec143a0f7087dab958b3222b02", 2048, 1024, downloadPack = "celestial-textures-v1.6", downloadName = "moon_jpl_0607.webp"),
        SpaceCompassCelestialTexture("moon_jpl_0609.webp", 65706, "72b9c683a0bc8d185926b6ea3b7913ddd20cf5d953e79b565dc25e5205b2b06e", 2048, 1024, downloadPack = "celestial-textures-v1.6", downloadName = "moon_jpl_0609.webp"),
        SpaceCompassCelestialTexture("moon_jpl_0610.webp", 21482, "11273c1d3923d05e13caad634c49772c137de485d097a1fbeb6e90c63615517c", 2048, 1024, downloadPack = "celestial-textures-v1.6", downloadName = "moon_jpl_0610.webp"),
        SpaceCompassCelestialTexture("moon_jpl_0611.webp", 37052, "a9fb084aeb605efee8bd55d61d52018591954d7d3715a269f9da0a0d16b71726", 2048, 1024, downloadPack = "celestial-textures-v1.6", downloadName = "moon_jpl_0611.webp"),
        SpaceCompassCelestialTexture("moon_jpl_0612.webp", 25846, "10504833e82807b30d0ccea36326688db65d06b22ebaf66fa2537b94d602b0ee", 2048, 1024, downloadPack = "celestial-textures-v1.6", downloadName = "moon_jpl_0612.webp"),
        SpaceCompassCelestialTexture("moon_jpl_0613.webp", 20906, "0e6e26198083c951ebdd299601db05e3cd421b649a055f6b08c050c39b8929d4", 2048, 1024, downloadPack = "celestial-textures-v1.6", downloadName = "moon_jpl_0613.webp"),
        SpaceCompassCelestialTexture("moon_jpl_0614.webp", 9078, "0791939188d7657820b4a0864b62ccb404ffb45808859c673d7560fdfd74434b", 2048, 1024, downloadPack = "celestial-textures-v1.6", downloadName = "moon_jpl_0614.webp"),
        SpaceCompassCelestialTexture("moon_jpl_0615.webp", 53730, "69042caabd3e024e48168076398e0b166beb57ca9e325650bbe479d2110448e7", 2048, 1024, downloadPack = "celestial-textures-v1.6", downloadName = "moon_jpl_0615.webp"),
        SpaceCompassCelestialTexture("moon_jpl_0616.webp", 17806, "fef9f256879194525d973a03e1457e4257ae363af5ead29ecb49c8eeffd20705", 2048, 1024, downloadPack = "celestial-textures-v1.6", downloadName = "moon_jpl_0616.webp"),
        SpaceCompassCelestialTexture("moon_jpl_0617.webp", 35794, "391c9cdb41070e5c3e6cfde3cf93fa0635b18bbe321ed493e41e105da1e7675c", 2048, 1024, downloadPack = "celestial-textures-v1.6", downloadName = "moon_jpl_0617.webp"),
        SpaceCompassCelestialTexture("moon_jpl_0619.webp", 13806, "f8adac5d8900067a79bbc8d87fee69ea02eff8fe74527dd0d6c79ab0a959773b", 2048, 1024, downloadPack = "celestial-textures-v1.6", downloadName = "moon_jpl_0619.webp"),
        SpaceCompassCelestialTexture("moon_jpl_0621.webp", 16766, "3f3140aa7e35268e640eaec3a597b70c245954568993e240185f5dc1930bd294", 2048, 1024, downloadPack = "celestial-textures-v1.6", downloadName = "moon_jpl_0621.webp"),
        SpaceCompassCelestialTexture("moon_jpl_0622.webp", 28966, "e7ef4dcce0397a33c47cf452f70b57ab37be5df19fb0cd2cddad3c09fb6dd6a6", 2048, 1024, downloadPack = "celestial-textures-v1.6", downloadName = "moon_jpl_0622.webp"),
        SpaceCompassCelestialTexture("moon_jpl_0623.webp", 29866, "f7e75782977840f8e23e4b496c45ac3c11a25265525b584363750ea147197da4", 2048, 1024, downloadPack = "celestial-textures-v1.6", downloadName = "moon_jpl_0623.webp"),
        SpaceCompassCelestialTexture("moon_jpl_0624.webp", 29120, "6203d7004f70b7b585ade05c12798e50428ac1d116685873db53c7769ff94b8b", 2048, 1024, downloadPack = "celestial-textures-v1.6", downloadName = "moon_jpl_0624.webp"),
        SpaceCompassCelestialTexture("moon_jpl_0625.webp", 35422, "316d8fbd2df009860b15776facc3e1ef28018e853ec7aec0383d6d6a1d33e70a", 2048, 1024, downloadPack = "celestial-textures-v1.6", downloadName = "moon_jpl_0625.webp"),
        SpaceCompassCelestialTexture("moon_jpl_0701.webp", 56504, "6e9413c36caef4cb9577758016fc3f0e2e1734aaa623647e73385f9feda4d56f", 2048, 1024, downloadPack = "celestial-textures-v1.6", downloadName = "moon_jpl_0701.webp"),
        SpaceCompassCelestialTexture("moon_jpl_0702.webp", 23362, "2a730d495e6e27bc8102061997fcecb526ecefd4eda984dc7d35e67d042eca8b", 2048, 1024, downloadPack = "celestial-textures-v1.6", downloadName = "moon_jpl_0702.webp"),
        SpaceCompassCelestialTexture("moon_jpl_0704.webp", 35966, "d6af87e460c7e5d689698b876be4d9d35b0e7cda6b9602de3ddeb7d9c8a1f91f", 2048, 1024, downloadPack = "celestial-textures-v1.6", downloadName = "moon_jpl_0704.webp"),
        SpaceCompassCelestialTexture("moon_jpl_0715.webp", 9774, "642f3ae68020d64677a19cd724859268ee88332c58a234f01400bc5643cad75d", 2048, 1024, downloadPack = "celestial-textures-v1.6", downloadName = "moon_jpl_0715.webp"),
        SpaceCompassCelestialTexture("moon_jpl_0802.webp", 4480, "2a5f2d7e5e4ab5417be83e3b574b2d7e13b9ba60c01dac0eda9d59df299d5567", 2048, 1024, downloadPack = "celestial-textures-v1.6", downloadName = "moon_jpl_0802.webp"),
        SpaceCompassCelestialTexture("moon_jpl_0803.webp", 8014, "80e73679e54a4eb701cce838dc831f4a23a8ea25eeb76fc0bd28fe1611e2056d", 2048, 1024, downloadPack = "celestial-textures-v1.6", downloadName = "moon_jpl_0803.webp"),
        SpaceCompassCelestialTexture("moon_jpl_0804.webp", 173510, "675ed858eb7dbd11f20f16ae4841d335aa7d91737818c90c6a3b8030187b90a8", 2048, 1024, downloadPack = "celestial-textures-v1.6", downloadName = "moon_jpl_0804.webp"),
        SpaceCompassCelestialTexture("moon_jpl_0805.webp", 153526, "46ec5016eae801ddc792483cb8b0e278094a17ab79d9a282929a2bd0b5f4a287", 2048, 1024, downloadPack = "celestial-textures-v1.6", downloadName = "moon_jpl_0805.webp"),
        SpaceCompassCelestialTexture("moon_jpl_0806.webp", 151450, "fdd7f993ce5e4b8e42030dc1ee45d6adfefdca5292cd0227a3763328b2f6a92f", 2048, 1024, downloadPack = "celestial-textures-v1.6", downloadName = "moon_jpl_0806.webp"),
        SpaceCompassCelestialTexture("moon_jpl_0807.webp", 10890, "70ccb8184fd019136d47bf5cb2154bf0c831db66d3786c6555aa35719116b4fa", 2048, 1024, downloadPack = "celestial-textures-v1.6", downloadName = "moon_jpl_0807.webp"),
        SpaceCompassCelestialTexture("moon_jpl_0808.webp", 32914, "b5bf1485558a415fef16dde9404c012e98de95a8c8faf2661b4002b371e40321", 2048, 1024, downloadPack = "celestial-textures-v1.6", downloadName = "moon_jpl_0808.webp"),
        SpaceCompassCelestialTexture("moon_jpl_0904.webp", 9354, "34c381dbd17fdb8a3744621ebcb3acf460da968873728cde59469ab3d8cf90e1", 2048, 1024, downloadPack = "celestial-textures-v1.6", downloadName = "moon_jpl_0904.webp"),
        SpaceCompassCelestialTexture("nebula_ic_5146.webp", 43306, "b507afc4c9c86eb13ac6f273663ed4c111229996094969358492a55417e5c845", 2048, 1024, downloadPack = "celestial-textures-v1.6", downloadName = "nebula_ic_5146.webp"),
        SpaceCompassCelestialTexture("nebula_ngc_0650.webp", 114888, "e1d2e42d542368f95fce6b18249391835d30b50922ca08616173152831ed5f7b", 2048, 1024, downloadPack = "celestial-textures-v1.6", downloadName = "nebula_ngc_0650.webp"),
        SpaceCompassCelestialTexture("nebula_ngc_1982.webp", 83908, "f9853d7e55f6790c8e84f8efe4db0bdd31064f42db4ad72b4d855cfebd4c8959", 2048, 1024, downloadPack = "celestial-textures-v1.6", downloadName = "nebula_ngc_1982.webp"),
        SpaceCompassCelestialTexture("nebula_ngc_2068.webp", 30130, "d664e71f4e09ec86ac10cc8798268c94d1c8221ed262e9e6a981e590147ebf7e", 2048, 1024, downloadPack = "celestial-textures-v1.6", downloadName = "nebula_ngc_2068.webp"),
        SpaceCompassCelestialTexture("nebula_ngc_6302.webp", 81436, "f7517ba5354fe058f354321a0668cfb410ec6accca550935d6dde264b1009f89", 2048, 1024, downloadPack = "celestial-textures-v1.6", downloadName = "nebula_ngc_6302.webp"),
        SpaceCompassCelestialTexture("nebula_ngc_6514.webp", 98344, "e8fdf7f27a1666cb972e65ed77bb9250290c938eec629dc5576c1efd4ca26758", 2048, 1024, downloadPack = "celestial-textures-v1.6", downloadName = "nebula_ngc_6514.webp"),
        SpaceCompassCelestialTexture("nebula_ngc_6523.webp", 115806, "f0e8b102e0dcb383be8ef6745eb638544bada16e91e710f134b245d41c8e89ff", 2048, 1024, downloadPack = "celestial-textures-v1.6", downloadName = "nebula_ngc_6523.webp"),
        SpaceCompassCelestialTexture("nebula_ngc_6537.webp", 117120, "540901763c6b508d584b13aaef1da818acdc8a1dbecfbfb401d0b4473c35d98d", 2048, 1024, downloadPack = "celestial-textures-v1.6", downloadName = "nebula_ngc_6537.webp"),
        SpaceCompassCelestialTexture("nebula_ngc_6611.webp", 144254, "3bf0594ec4ec4eb4c32fab4e00b51db9a7ae19670cf7e6a787214f6f53bebfd0", 2048, 1024, downloadPack = "celestial-textures-v1.6", downloadName = "nebula_ngc_6611.webp"),
        SpaceCompassCelestialTexture("nebula_ngc_6618.webp", 372382, "26fba1c5212fe0d1ddd3e0e10e9e88855eb12260c49066c6ac813d6d03efc50e", 2048, 1024, downloadPack = "celestial-textures-v1.6", downloadName = "nebula_ngc_6618.webp"),
        SpaceCompassCelestialTexture("nebula_ngc_6818.webp", 75680, "bc91cada1cf6586ae2f84284e4c049c6df07c1f46eeb497e7f89b1b481c3492a", 2048, 1024, downloadPack = "celestial-textures-v1.6", downloadName = "nebula_ngc_6818.webp"),
        SpaceCompassCelestialTexture("probe_jpl_m0064.webp", 277048, "f8e14913ab5878286b5f26c4f7aa116b8e36b8d253d320d3d652512d1f8fa502", 2048, 1024, downloadPack = "celestial-textures-v1.6", downloadName = "probe_jpl_m0064.webp"),
        SpaceCompassCelestialTexture("probe_jpl_m0202.webp", 135114, "7c3b4980fe60e12ee2fd8fae4e649d3c6f1775a3aac5f8e9c541a20374c8821e", 2048, 1024, downloadPack = "celestial-textures-v1.6", downloadName = "probe_jpl_m0202.webp"),
        SpaceCompassCelestialTexture("fictional_arrakis.webp", 395806, "5319316cd184d54fdf1e16a106e275ef6a71c11e9e688600b1a6dec17edef208", 2048, 1024, downloadPack = "celestial-textures-v1.7", downloadName = "fictional_arrakis.webp"),
        SpaceCompassCelestialTexture("fictional_caladan.webp", 573986, "c1ea9b745bc4b28f2cb261e61acce77a3250d72a81ca73b01b28d420a5cf00eb", 2048, 1024, downloadPack = "celestial-textures-v1.7", downloadName = "fictional_caladan.webp"),
        SpaceCompassCelestialTexture("fictional_giedi_prime.webp", 542460, "01f4cbae2e93aa00f18e893b5efc6e662db251d59689d6cd54bdbd679bc4b9ca", 2048, 1024, downloadPack = "celestial-textures-v1.7", downloadName = "fictional_giedi_prime.webp"),
        SpaceCompassCelestialTexture("fictional_lv_223.webp", 582650, "51ed28617b360f5f420fbd5c5a8b5a25b1ebc2b25ff122ad455de84e0faf98d3", 2048, 1024, downloadPack = "celestial-textures-v1.7", downloadName = "fictional_lv_223.webp"),
        SpaceCompassCelestialTexture("fictional_calpamos.webp", 312312, "0b47650bd36bbba03041ae2e2c39521f0a0b606fddd01ed4829f9b304f897afd", 2048, 1024, downloadPack = "celestial-textures-v1.7", downloadName = "fictional_calpamos.webp"),
        SpaceCompassCelestialTexture("fictional_pandora.webp", 724932, "86cfc6ed5eada7a5d8de5d4f7bb23ade6c1988c87c832ee552bc72c812506e5e", 2048, 1024, downloadPack = "celestial-textures-v1.7", downloadName = "fictional_pandora.webp"),
        SpaceCompassCelestialTexture("fictional_polyphemus.webp", 403552, "19ffc9a9e21c954da95b8036f3ab687ee86e0d6d92d3f943a6d7c20aef43d1f0", 2048, 1024, downloadPack = "celestial-textures-v1.7", downloadName = "fictional_polyphemus.webp"),
        SpaceCompassCelestialTexture("fictional_vulcan.webp", 568534, "d81c4553c8ae2cebd9094098d0850a8aedd2fc30e8b24dd7951da4766a60ec29", 2048, 1024, downloadPack = "celestial-textures-v1.7", downloadName = "fictional_vulcan.webp"),
        SpaceCompassCelestialTexture("fictional_nostromo.webp", 246646, "da096667d63fef15fc52de07f6b9f67e52dd3b79d97ae8355e2d2e920aac1c1e", 2048, 1024, downloadPack = "celestial-textures-v1.7", downloadName = "fictional_nostromo.webp"),
        SpaceCompassCelestialTexture("fictional_discovery_one.webp", 78672, "1665dd32ecdc227075808d8a8ae165aff58a49ea79d61a306af5c1d74255129e", 2048, 1024, downloadPack = "celestial-textures-v1.7", downloadName = "fictional_discovery_one.webp")
    )
    private val byName = (images + SpaceCompassCelestialThumbnailPolicy.images).associateBy { it.name }
    fun texture(name: String?) = byName[SpaceCompassTextureAliases.canonical(name)]
    fun allowedUrl(url: String, redirect: Boolean = false): Boolean = runCatching {
        val uri = URI(url)
        if (uri.scheme != "https" || uri.userInfo != null || uri.fragment != null || uri.port !in listOf(-1, 443)) return false
        if (redirect && uri.host in setOf("release-assets.githubusercontent.com", "objects.githubusercontent.com")) return true
        byName.values.any { it.url == url }
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
