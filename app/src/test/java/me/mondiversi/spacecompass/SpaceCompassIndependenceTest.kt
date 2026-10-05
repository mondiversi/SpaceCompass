package me.mondiversi.spacecompass

import java.io.File
import org.junit.Assert.*
import org.junit.Test

class SpaceCompassIndependenceTest {
    private val root = File("src/main").takeIf { it.isDirectory } ?: File("app/src/main")

    @Test fun independentLauncherAndNamespaceDoNotNeedUvir() {
        val main = File(root, "java/me/mondiversi/spacecompass/MainActivity.kt").readText()
        assertTrue(main.contains("SpaceCompassSunFinderScreen(background, primary, secondary)"))
        assertFalse(main.contains("SunFinderEntry"))
        root.walkTopDown().filter { it.extension == "kt" }.forEach {
            val source = it.readText()
            assertFalse(it.name, source.contains("me.mondiversi.uvir"))
            for (type in listOf("UvirDatabase", "UsbSensorManager", "WirelessSensorManager", "SensorProfile"))
                assertFalse("${it.name}: $type", source.contains(type))
        }
    }

    @Test fun onlyForegroundLocationAndInternetPermissionsAreDeclared() {
        val manifest = File(root, "AndroidManifest.xml").readText()
        for (permission in listOf("ACCESS_FINE_LOCATION", "ACCESS_COARSE_LOCATION", "INTERNET"))
            assertTrue(manifest.contains("android.permission.$permission"))
        for (permission in listOf("CAMERA", "BLUETOOTH_CONNECT", "ACCESS_BACKGROUND_LOCATION",
            "REQUEST_INSTALL_PACKAGES", "FOREGROUND_SERVICE_CONNECTED_DEVICE"))
            assertFalse(manifest.contains("android.permission.$permission"))
        assertFalse(manifest.contains("usb.host"))
        assertFalse(manifest.contains("<service"))
    }

    @Test fun originalMapsAndSatelliteImplementationHaveTheirCredits() {
        for (name in listOf("moon", "sun", "mercury", "venus_atmosphere", "mars", "jupiter",
            "io", "europa", "saturn", "uranus", "neptune", "pluto"))
            assertTrue(name, File(root, "assets/celestial/$name.jpg").length() > 0)
        val project = File("..").takeIf { File(it,"ASSET_CREDITS.md").isFile } ?: File(".")
        assertTrue(File(project, "ASSET_CREDITS.md").isFile)
        assertTrue(File(project, "third_party/sgp4/LICENSE").isFile)
        assertTrue(File(root, "assets/licenses/celestial-textures.txt").isFile)
        assertTrue(File(root, "assets/licenses/astronomy-engine.txt").readText().contains("MIT License"))
    }
}
