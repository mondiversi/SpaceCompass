package me.mondiversi.planetcompass

import java.io.File
import org.junit.Assert.*
import org.junit.Test

/** Appearance policy only: orbital calculation and pointing are intentionally unchanged. */
class PlanetCompassCelestialMarkerContractTest {
    private fun source(name: String): String = listOf(
        File("src/main/java/me/mondiversi/planetcompass/$name"),
        File("app/src/main/java/me/mondiversi/planetcompass/$name")
    ).first { it.isFile }.readText()

    @Test fun edgePlacementAndCaptionExclusionsUseOnlyTheInspectedBodyWithoutRemovingPathsOrVisibleObjects() {
        val sky = source("PlanetCompassSunFinderSky.kt")
        assertTrue(sky.contains("planetCompassCelestialDirectionalProjections(pixelProjections, offscreenBody)"))
        assertTrue(sky.contains("fitPlanetCompassCelestialOffscreenDiameter(directionalProjections"))
        assertTrue(sky.contains("placePlanetCompassCelestialOffscreenMarkers(directionalProjections"))
        assertTrue(sky.contains("paths.forEach"))
        assertTrue(sky.contains("planetCompassCelestialCatalogOrder.forEach"))
        assertTrue(sky.contains("if (projected.visible) PlanetCompassCelestialLiveMarker"))
        assertTrue(source("PlanetCompassSunFinderScreen.kt").contains("offscreenBody = body.takeIf { hasActiveBody }"))
    }

    @Test fun everyTargetReusesItsCataloguePreviewAtTheSameSize() {
        val sky = source("PlanetCompassSunFinderSky.kt")
        val marker = source("PlanetCompassCelestialLiveMarker.kt")
        assertTrue(sky.contains("PlanetCompassCelestialLiveMarker(candidate, projected"))
        assertTrue(marker.contains("PlanetCompassCelestialThumbnail(body"))
        assertEquals(32f, PLANET_COMPASS_CELESTIAL_LIVE_PREVIEW_SIZE_DP, 0f)
        assertFalse(marker.contains("when (body)"))
        assertFalse(sky.contains("drawPlanetCompassCelestialMarker"))
        assertTrue(marker.contains("marker.place("))
        assertFalse(marker.contains("placeRelative("))
    }

    @Test fun belowHorizonPreviewsUseOneDesaturationPolicyWithoutChangingTheCache() {
        val common = planetCompassSunAppearance(-20.0, true)
        for (elevation in listOf(-0.001, -1.0, -20.0, -90.0)) for (rising in listOf(false, true)) {
            assertEquals(common, planetCompassSunAppearance(elevation, rising))
        }
        assertTrue(common.belowHorizon)
        assertEquals(0f, common.glowStrength, 0f)
        assertEquals(0xFFBBA6FF.toInt(), common.edgeArgb)
        val thumbnail = source("PlanetCompassCelestialThumbnail.kt")
        assertTrue(source("PlanetCompassCelestialLiveMarker.kt").contains("muted = belowHorizon"))
        assertTrue(thumbnail.contains("setToSaturation(0f)"))
        assertTrue(thumbnail.contains("colorFilter = colorFilter, alpha = opacity"))
        assertTrue(thumbnail.contains("modifier.clip(CircleShape), colorFilter, opacity"))
        assertTrue(source("PlanetCompassCelestialCraftModel.kt").contains("colorFilter = colorFilter, alpha = opacity"))
    }

    @Test fun offscreenArrowAndAlignedReticleUseTheSameCommonTargetColour() {
        val sky = source("PlanetCompassSunFinderSky.kt")
        assertTrue(sky.contains("val sunTint = appearance?.let { Color(it.edgeArgb) }"))
        assertTrue(sky.contains("val reticle = if (aligned) sunTint else Color.White"))
        assertTrue(sky.contains("PlanetCompassCelestialOffscreenMarker(candidate, projected, below"))
        val locator = source("PlanetCompassCelestialOffscreenMarker.kt")
        assertTrue(locator.contains("PlanetCompassCelestialThumbnail(body"))
        assertTrue(locator.contains("muted = belowHorizon"))
        assertTrue(locator.contains("rotate(bearing, center)"))
        assertTrue(locator.contains("drawPath(arrow, pointerColor)"))
        assertTrue(locator.contains("marker.place("))
        assertFalse(locator.contains("placeRelative("))
    }

    @Test fun visualizeUsesATelescopeWithoutChangingTheSharedControlOrItsAccessibleLabel() {
        val actions = source("PlanetCompassSunDailyPathLayer.kt").substringAfter("internal fun PlanetCompassCelestialSkyActions(")
        assertTrue(actions.contains("R.string.celestial_view_open"))
        assertTrue(actions.contains("PlanetCompassCelestialIconControl(false, viewTitle, onVisualize"))
        assertTrue(actions.contains("PlanetCompassCelestialTelescopeIcon(Modifier.size(22.dp))"))
        assertFalse(actions.contains("PlanetCompassCelestialThumbnail("))
        assertTrue(source("PlanetCompassCelestialTelescopeIcon.kt").contains("1.5.dp.toPx()"))
    }

    @Test fun moonMiniaturesMaskTheSamePhaseSilhouetteWithoutReplacingTheSharedTextureCache() {
        val thumbnail = source("PlanetCompassCelestialThumbnail.kt")
        assertTrue(thumbnail.contains("moonPhase.takeIf { body == PlanetCompassCelestialBody.MOON }"))
        assertTrue(thumbnail.contains("phase?.let(::planetCompassMoonPhaseSilhouette)"))
        assertTrue(source("PlanetCompassMoonPhaseIcon.kt").contains("remember(phase) { planetCompassMoonPhaseSilhouette(phase) }"))
        assertTrue(thumbnail.contains("clipPath(litArea)"))
        assertTrue(thumbnail.contains("alpha = opacity * 0.10f"))
        assertTrue(thumbnail.contains("celestialThumbnailCache[body]"))
        assertFalse(thumbnail.contains("calculatePlanetCompassMoonPhase"))
    }

    @Test fun liveAndEdgeMoonShareOneClockPhaseWithoutOrientationOrSelectedPointRecalculations() {
        val sky = source("PlanetCompassSunFinderSky.kt")
        assertTrue(sky.contains("rememberPlanetCompassMoonMarkerPhase(timeMs.takeIf { PlanetCompassCelestialBody.MOON in positions })"))
        assertEquals(2, Regex("moonPhase = moonPhase").findAll(sky).count())
        for (file in listOf("PlanetCompassCelestialLiveMarker.kt", "PlanetCompassCelestialOffscreenMarker.kt"))
            assertTrue(source(file).contains("muted = belowHorizon, moonPhase = moonPhase"))
        val state = source("PlanetCompassMoonMarkerPhase.kt")
        assertTrue(state.contains("Math.floorDiv(it, 60_000L)"))
        assertTrue(state.contains("produceState<PlanetCompassMoonPhase?>(null, minute)"))
        assertTrue(state.contains("withContext(Dispatchers.Default)"))
        assertFalse(state.contains("orientation") && state.contains("produceState<PlanetCompassMoonPhase?>(null, orientation"))
        assertTrue(source("PlanetCompassCelestialPath.kt").contains("point.copy(moonPhase = calculatePlanetCompassMoonPhase(point.timeMs))"))
    }
}
