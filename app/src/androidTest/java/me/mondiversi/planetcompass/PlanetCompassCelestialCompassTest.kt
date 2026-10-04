package me.mondiversi.planetcompass

import android.content.res.Configuration
import android.location.Location
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.platform.*
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.*
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import java.io.File
import java.time.Instant
import java.util.Locale

/** Synthetic location, no sensor commands/archive/preferences. Real HTTPS smoke test sends no GPS. */
class PlanetCompassCelestialCompassTest {
    @get:Rule val compose = createComposeRule()
    private val context = InstrumentationRegistry.getInstrumentation().targetContext
    private val resources = context.createConfigurationContext(Configuration(context.resources.configuration)
        .apply { setLocale(Locale.ITALIAN) }).resources
    private val now = Instant.parse("2026-10-03T12:00:00Z").toEpochMilli()
    private val body = mutableStateOf(PlanetCompassCelestialBody.SUN)
    private val time = mutableLongStateOf(now)
    private val wide = mutableStateOf(false)
    private val night = mutableStateOf(false)
    private val orbit = PlanetCompassIssOrbit.parse("1 25544U 98067A   26276.04623379  .00005750  00000+0  11349-3 0  9994\n" +
        "2 25544  51.6314 126.3061 0006899 216.4733 143.5786 15.48722558588472")
    private fun show(remote: PlanetCompassCelestialRemoteData = PlanetCompassCelestialRemoteData(iss = orbit)) {
        val fix = Location("celestial-fixture").apply { latitude = 45.0; longitude = 9.0; altitude = 100.0; accuracy = 5f }
        compose.setContent {
            CompositionLocalProvider(LocalResources provides resources) {
                val density = LocalDensity.current
                CompositionLocalProvider(LocalDensity provides if (wide.value) Density(density.density * 0.4f, 1f) else density) {
                    MaterialTheme(colorScheme = if (night.value) darkColorScheme() else lightColorScheme()) {
                        Box(if (wide.value) Modifier.size(800.dp, 400.dp) else Modifier.fillMaxSize()) {
                            val c = MaterialTheme.colorScheme
                            PlanetCompassSunFinderContent(PlanetCompassSunFinderReadings(fix, PlanetCompassSunLocationStatus.READY,
                                PlanetCompassSunOrientation(PlanetCompassSunVector(1.0,0.0,0.0), PlanetCompassSunVector(0.0,0.0,1.0),
                                    PlanetCompassSunVector(0.0,1.0,0.0)), true, true),
                                time.longValue, c.onSurface, c.onSurfaceVariant, c.background,
                                body = body.value, remote = remote, onBodyChange = { body.value = it }, onDismissRequest = {})
                        }
                    }
                }
            }
        }
    }
    @Test fun moonPathShowsPhaseIconsBeforeEveryTimeInBothThemes() {
        show()
        compose.runOnIdle { body.value = PlanetCompassCelestialBody.MOON }
        compose.waitUntil(10_000) { compose.onAllNodesWithTag("sun-path-open").fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithTag("sun-path-open").performClick()
        for (dark in listOf(false, true)) {
            compose.runOnIdle { night.value = dark }
            compose.onNodeWithTag("moon-phase-0", useUnmergedTree = true).assertIsDisplayed()
            val icon = compose.onNodeWithTag("moon-phase-0", useUnmergedTree = true).fetchSemanticsNode()
            assertTrue(icon.config[androidx.compose.ui.semantics.SemanticsProperties.ContentDescription].first().contains("%"))
            val row = compose.onNodeWithTag("sun-path-point-0").fetchSemanticsNode()
            assertTrue(icon.boundsInRoot.left >= row.boundsInRoot.left)
            assertTrue(icon.boundsInRoot.right < row.boundsInRoot.right)
        }
    }

    @Test fun orderedSelectorWorksForEveryObjectAndUnavailableDataDoesNotInventAPosition() {
        show()
        for (candidate in PlanetCompassCelestialBody.entries) {
            compose.onNodeWithTag("celestial-select").performClick()
            compose.onNodeWithTag("celestial-body-${candidate.name}").performScrollTo().performClick()
            compose.runOnIdle { assertEquals(candidate, body.value) }
            compose.waitForIdle()
            compose.onNodeWithTag("celestial-select").assertContentDescriptionEquals(
                resources.getString(candidate.nameResource) + ", " + resources.getString(R.string.celestial_select))
            compose.onNodeWithTag("celestial-distance").assertIsDisplayed()
            compose.onNodeWithTag("celestial-speed").assertIsDisplayed()
            if (candidate.hasPhysicalFace || candidate == PlanetCompassCelestialBody.ISS)
                compose.waitUntil(10_000) { compose.onAllNodesWithTag("sun-path-open").fetchSemanticsNodes().isNotEmpty() }
            if (candidate.usesHorizons) {
                compose.onNodeWithText(resources.getString(R.string.celestial_unavailable,
                    resources.getString(candidate.nameResource))).assertExists()
                compose.onNodeWithTag("sun-path-open").assertDoesNotExist()
            }
        }
    }
    @Test fun moonAndIssDistancesChangeLiveAndPassPopupSurvivesTimeRefresh() {
        show()
        for (candidate in listOf(PlanetCompassCelestialBody.MOON, PlanetCompassCelestialBody.ISS)) {
            compose.runOnIdle { body.value = candidate }
            compose.waitUntil(10_000) {
                compose.onAllNodesWithTag("sun-path-open").fetchSemanticsNodes().isNotEmpty() &&
                    compose.onNodeWithTag("celestial-distance").fetchSemanticsNode().config
                        .getOrElse(androidx.compose.ui.semantics.SemanticsProperties.Text) { emptyList() }.toString().contains("km")
            }
            val before = compose.onNodeWithTag("celestial-distance").fetchSemanticsNode().config[
                androidx.compose.ui.semantics.SemanticsProperties.Text]
            compose.runOnIdle { time.longValue += 60_000 }
            compose.waitUntil(10_000) {
                compose.onNodeWithTag("celestial-distance").fetchSemanticsNode().config[
                    androidx.compose.ui.semantics.SemanticsProperties.Text] != before
            }
            compose.onNodeWithTag("sun-path-open").performClick()
            compose.onNodeWithTag("sun-path-title").assertTextEquals(resources.getString(
                if (candidate == PlanetCompassCelestialBody.ISS) R.string.celestial_orbit else R.string.celestial_daily_path))
            repeat(10) { compose.runOnIdle { time.longValue += 2_000 }; compose.onNodeWithTag("sun-path-dialog").assertIsDisplayed() }
            compose.onNodeWithContentDescription(resources.getString(R.string.close)).performClick()
        }
    }
    @Test fun bodyActionsMatchViewerSizeAndStayTogetherBesideItsTitleInBothLayouts() {
        show()
        for (landscape in listOf(false,true)) {
            compose.runOnIdle { wide.value=landscape; body.value=PlanetCompassCelestialBody.EUROPA }
            compose.waitUntil(10_000) { compose.onAllNodesWithTag("sun-path-open").fetchSemanticsNodes().isNotEmpty() }
            val header=compose.onNodeWithTag("celestial-body-header").fetchSemanticsNode().boundsInRoot
            val view=compose.onNodeWithTag("celestial-visualize").fetchSemanticsNode().boundsInRoot
            val path=compose.onNodeWithTag("sun-path-open").fetchSemanticsNode().boundsInRoot
            assertEquals(view.width,path.width,0.1f); assertEquals(view.height,path.height,0.1f)
            assertEquals(view.top,path.top,0.1f); assertEquals(view.right,path.left,0.1f)
            assertTrue(view.left >= header.left && path.right <= header.right)
            compose.onNodeWithTag("celestial-visualize").assert(hasAnyAncestor(hasTestTag("celestial-body-header")))
            compose.onNodeWithTag("sun-path-open").assert(hasAnyAncestor(hasTestTag("celestial-body-header")))
            assertTrue(path.width>=with(compose.density) { 47.dp.toPx() })
            compose.onNodeWithTag("celestial-orientation-overlay").assertDoesNotExist()
            val heading = compose.onNodeWithTag("sun-data-heading").fetchSemanticsNode().boundsInRoot
            val tilt = compose.onNodeWithTag("sun-data-tilt").fetchSemanticsNode().boundsInRoot
            assertEquals(heading.right, tilt.right, 1f)
            assertTrue(heading.bottom <= tilt.top)
            compose.onNodeWithTag("celestial-visualize").assertContentDescriptionEquals(resources.getString(R.string.celestial_view_open))
            compose.onNodeWithTag("sun-path-open").assertContentDescriptionEquals(resources.getString(R.string.celestial_daily_path))
            compose.onNodeWithText(resources.getString(R.string.celestial_view_open)).assertDoesNotExist()
        }
    }
    @Test fun lightDarkAndWideLayoutsKeepSelectorDistanceAndDataUsable() {
        show()
        for (b in listOf(PlanetCompassCelestialBody.MOON, PlanetCompassCelestialBody.ISS)) for (dark in listOf(false,true)) for (landscape in listOf(false,true)) {
            compose.runOnIdle { body.value = b; wide.value = landscape; night.value = dark }
            compose.waitUntil(10_000) { compose.onAllNodesWithTag("sun-path-open").fetchSemanticsNodes().isNotEmpty() }
            compose.onNodeWithTag("celestial-select").assertIsDisplayed()
            compose.onNodeWithTag("sun-data-azimuth").assertIsDisplayed()
            compose.onNodeWithTag("celestial-distance").assertIsDisplayed()
            compose.onNodeWithTag("celestial-speed").assertIsDisplayed()
            for (tag in listOf("celestial-distance", "celestial-speed", "sun-data-azimuth", "sun-data-elevation"))
                compose.onNodeWithTag(tag).assert(hasAnyAncestor(hasTestTag("sun-finder-body-data")))
                    .assert(!hasAnyAncestor(hasTestTag("sun-finder-environment-data")))
            for (tag in listOf("sun-data-heading", "sun-data-tilt"))
                compose.onNodeWithTag(tag).assert(hasAnyAncestor(hasTestTag("sun-finder-environment-data")))
                    .assert(hasAnyAncestor(hasTestTag("sun-finder-location-data")))
            val environment = compose.onNodeWithTag("sun-finder-environment-content").fetchSemanticsNode().boundsInRoot
            val values = compose.onNodeWithTag("sun-finder-environment-values").fetchSemanticsNode().boundsInRoot
            assertEquals(environment.center.y, values.center.y, 1f)
            val tilt = compose.onNodeWithTag("sun-data-tilt").fetchSemanticsNode().boundsInRoot
            val gps = compose.onNodeWithTag("sun-data-altitude").fetchSemanticsNode().boundsInRoot
            assertTrue(tilt.bottom <= gps.top)
            val azimuth = compose.onNodeWithTag("sun-data-azimuth").fetchSemanticsNode().boundsInRoot
            val elevation = compose.onNodeWithTag("sun-data-elevation").fetchSemanticsNode().boundsInRoot
            assertEquals(azimuth.top, elevation.top, 1f)
            compose.onNodeWithTag("celestial-body-island-title").assertTextEquals(resources.getString(b.nameResource))
            compose.onNodeWithTag("celestial-body-previous").assertDoesNotExist()
            compose.onNodeWithTag("celestial-body-next").assertDoesNotExist()
            val bitmap = compose.onNodeWithTag("sun-finder-content").captureToImage().asAndroidBitmap()
            File(context.externalCacheDir, "celestial-${b.name}-$dark-$landscape.png").outputStream().use {
                bitmap.compress(android.graphics.Bitmap.CompressFormat.PNG, 100, it)
            }
        }
    }
    @Test fun issOrbitPopupSeparatesPassDetailsAndRetainsTheOpenListAcrossMinuteRefresh() {
        body.value = PlanetCompassCelestialBody.ISS
        show()
        compose.waitUntil(10_000) { compose.onAllNodesWithTag("sun-path-open").fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithTag("sun-path-open").assertContentDescriptionEquals(resources.getString(R.string.celestial_iss_path))
        compose.onNodeWithTag("sun-path-open").performClick()
        compose.onNodeWithTag("sun-path-title").assertTextEquals(resources.getString(R.string.celestial_orbit))
        compose.onNodeWithTag("iss-pass-heading").assertIsDisplayed()
        for (event in listOf(PlanetCompassSunPathEvent.SUNRISE, PlanetCompassSunPathEvent.CULMINATION, PlanetCompassSunPathEvent.SUNSET)) {
            val node = compose.onNodeWithTag("iss-pass-${event.name}")
            node.assertExists()
            assertFalse(node.fetchSemanticsNode().config.contains(androidx.compose.ui.semantics.SemanticsActions.OnClick))
        }
        val firstPoint = compose.onNodeWithTag("sun-path-point-0").fetchSemanticsNode().config[
            androidx.compose.ui.semantics.SemanticsProperties.Text]
        repeat(3) {
            compose.runOnIdle { time.longValue += 60_000L }
            compose.onNodeWithTag("sun-path-dialog").assertIsDisplayed()
            assertEquals(firstPoint, compose.onNodeWithTag("sun-path-point-0").fetchSemanticsNode().config[
                androidx.compose.ui.semantics.SemanticsProperties.Text])
        }
    }
    @Test fun voyagerWithValidDataHasALiveDistanceButNoPathControlsInPortraitOrWideLayout() {
        val probes = listOf(PlanetCompassCelestialBody.VOYAGER_1, PlanetCompassCelestialBody.VOYAGER_2)
        val data = PlanetCompassCelestialRemoteData(ephemerides = probes.associateWith { probe ->
            // Public-style synthetic vectors; only UI layout/state, no GPS or archive writes.
            val x = if (probe == PlanetCompassCelestialBody.VOYAGER_1) -33.0 else 39.0
            PlanetCompassHorizonsEphemeris(probe, listOf(
                PlanetCompassHorizonsSample(now, x, -165.0, 36.0),
                PlanetCompassHorizonsSample(now + 3_600_000, x, -165.005, 36.001)))
        })
        show(data)
        for (probe in probes) for (landscape in listOf(false, true)) {
            compose.runOnIdle { body.value = probe; wide.value = landscape; time.longValue = now }
            compose.waitUntil(10_000) {
                compose.onNodeWithTag("celestial-distance").fetchSemanticsNode().config[
                    androidx.compose.ui.semantics.SemanticsProperties.Text].toString().contains(" Mkm")
            }
            val before = compose.onNodeWithTag("celestial-distance").fetchSemanticsNode().config[
                androidx.compose.ui.semantics.SemanticsProperties.Text]
            compose.onNodeWithTag("sun-path-open").assertDoesNotExist()
            compose.onNodeWithTag("sun-path-selected").assertDoesNotExist()
            compose.onNodeWithTag("sun-path-dialog").assertDoesNotExist()
            compose.onNodeWithTag("sun-finder-sky").assertIsDisplayed()
            compose.onNodeWithTag("celestial-distance").assertIsDisplayed()
            compose.runOnIdle { time.longValue += 2_000 }
            compose.waitUntil(10_000) {
                compose.onNodeWithTag("celestial-distance").fetchSemanticsNode().config[
                    androidx.compose.ui.semantics.SemanticsProperties.Text] != before
            }
        }
    }
    @Test fun realPublicIssAndSednaEndpointsReturnValidatedFreshDataWithoutSendingLocation() = runBlocking {
        val actualNow = System.currentTimeMillis()
        val iss = PlanetCompassIssOrbit.parse(fetchPlanetCompassCelestialText(
            "https://celestrak.org/NORAD/elements/gp.php?CATNR=25544&FORMAT=TLE"))
        assertTrue(iss.usable(actualNow))
        val sedna = parsePlanetCompassHorizonsEphemeris(PlanetCompassCelestialBody.SEDNA,
            fetchPlanetCompassCelestialText(planetCompassHorizonsUrl(PlanetCompassCelestialBody.SEDNA, actualNow)))
        assertNotNull(sedna.at(actualNow))
        assertTrue(iss.observe(actualNow, 45.0, 9.0, 100.0).distanceKm.isFinite())
    }
}
