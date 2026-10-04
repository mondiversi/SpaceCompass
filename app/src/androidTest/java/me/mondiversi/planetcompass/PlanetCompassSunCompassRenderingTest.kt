package me.mondiversi.planetcompass

import android.content.res.Configuration
import android.location.Location
import android.os.SystemClock
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.platform.*
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.dp
import androidx.test.platform.app.InstrumentationRegistry
import java.io.File
import java.time.Instant
import java.util.Locale
import kotlin.math.*
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test

/** Synthetic fixtures only. No real GPS, sensor listeners, user archive or settings operations. */
class PlanetCompassSunCompassRenderingTest {
    @get:Rule val compose = createComposeRule()
    private val context = InstrumentationRegistry.getInstrumentation().targetContext
    private val config = Configuration(context.resources.configuration).apply { setLocale(Locale.ITALIAN) }
    private val resources = context.createConfigurationContext(config).resources

    private fun facing(heading: Double, tilt: Double, roll: Double): PlanetCompassSunOrientation {
        val az = Math.toRadians(heading); val el = Math.toRadians(tilt); val r = Math.toRadians(roll)
        val right = PlanetCompassSunVector(cos(az), -sin(az), 0.0)
        val up = PlanetCompassSunVector(-sin(el) * sin(az), -sin(el) * cos(az), cos(el))
        fun mix(a: PlanetCompassSunVector, b: PlanetCompassSunVector, x: Double, y: Double) =
            PlanetCompassSunVector(a.east * x + b.east * y, a.north * x + b.north * y, a.up * x + b.up * y)
        return PlanetCompassSunOrientation(mix(right, up, cos(r), sin(r)), mix(up, right, cos(r), -sin(r)),
            planetCompassSunCompassDirection(heading, tilt))
    }

    @Test fun sphericalCompassChangesWithPitchRollAndThemeWithoutLeavingItsBounds() {
        val pose = mutableIntStateOf(0)
        val dark = mutableStateOf(false)
        val poses = listOf(facing(0.0, 0.0, 0.0), facing(90.0, 35.0, 0.0),
            facing(180.0, 0.0, 180.0), facing(270.0, -90.0, 0.0), facing(45.0, 90.0, 90.0))
        compose.setContent {
            MaterialTheme(colorScheme = if (dark.value) darkColorScheme() else lightColorScheme()) {
                Box(Modifier.size(192.dp).background(MaterialTheme.colorScheme.background).testTag("compass-fixture")) {
                    PlanetCompassSunFinderCompass(poses[pose.intValue], true, MaterialTheme.colorScheme.onSurface,
                        Modifier.size(192.dp))
                }
            }
        }
        var previous: android.graphics.Bitmap? = null
        for (night in listOf(false, true)) for (index in poses.indices) {
            compose.runOnIdle { dark.value = night; pose.intValue = index }
            compose.onNodeWithTag("sun-finder-compass").assertIsDisplayed()
            val bitmap = compose.onNodeWithTag("compass-fixture").captureToImage().asAndroidBitmap()
            previous?.let { assertFalse("Pitch and roll must change the sphere", it.sameAs(bitmap)) }
            previous = bitmap
            File(context.externalCacheDir, "sun-compass-${if (night) "night" else "day"}-$index.png").outputStream().use {
                bitmap.compress(android.graphics.Bitmap.CompressFormat.PNG, 100, it)
            }
        }
    }

    @Test fun unreliableCompassShowsTheExistingLocalizedWarningAndUndefinedBearingIsADash() {
        val verticalAxis = mutableStateOf(false)
        val reliable = mutableStateOf(false)
        val timestamp = Instant.parse("2003-10-17T19:30:30Z").toEpochMilli()
        val fix = Location("synthetic").apply {
            latitude = 39.742476; longitude = -105.1786; altitude = 1830.14
            accuracy = 8f; elapsedRealtimeNanos = SystemClock.elapsedRealtimeNanos()
        }
        compose.setContent {
            CompositionLocalProvider(LocalResources provides resources, LocalPlanetCompassNumericFormat provides PlanetCompassNumericFormat.EUROPEAN) {
                MaterialTheme {
                    val colors = MaterialTheme.colorScheme
                    PlanetCompassSunFinderContent(PlanetCompassSunFinderReadings(fix, PlanetCompassSunLocationStatus.READY,
                        facing(224.7, if (verticalAxis.value) -89.5 else -4.4, 180.0), true, reliable.value),
                        timestamp, colors.onSurface, colors.onSurfaceVariant, colors.background)
                }
            }
        }
        compose.onNodeWithText(resources.getString(R.string.sun_finder_compass_accuracy)).assertIsDisplayed()
        compose.onNodeWithTag("sun-finder-compass").assertIsDisplayed()
        compose.onNodeWithContentDescription("Direzione: —").assertIsDisplayed()
        compose.onNodeWithTag("sun-finder-compass").assertContentDescriptionEquals(
            resources.getString(R.string.sun_finder_compass_accuracy))
        compose.runOnIdle { verticalAxis.value = true }
        compose.onNodeWithContentDescription("Direzione: —").assertIsDisplayed()
        compose.onNodeWithText(resources.getString(R.string.sun_finder_compass_accuracy)).assertIsDisplayed()
        compose.runOnIdle { verticalAxis.value = false; reliable.value = true }
        compose.onNodeWithContentDescription("Direzione: 224,7°").assertIsDisplayed()
        compose.onNodeWithText(resources.getString(R.string.sun_finder_compass_accuracy)).assertDoesNotExist()
    }
}
