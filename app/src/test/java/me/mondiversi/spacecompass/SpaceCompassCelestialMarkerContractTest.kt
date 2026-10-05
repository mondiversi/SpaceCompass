package me.mondiversi.spacecompass

import java.io.File
import org.junit.Assert.*
import org.junit.Test

/** Appearance policy only: orbital calculation and pointing are intentionally unchanged. */
class SpaceCompassCelestialMarkerContractTest {
    private fun source(name: String): String = listOf(
        File("src/main/java/me/mondiversi/spacecompass/$name"),
        File("app/src/main/java/me/mondiversi/spacecompass/$name")
    ).first { it.isFile }.readText()

    @Test fun edgePlacementAndCaptionExclusionsUseOnlyTheInspectedBodyWithoutRemovingPathsOrVisibleObjects() {
        val sky = source("SpaceCompassSunFinderSky.kt")
        assertTrue(sky.contains("spaceCompassCelestialDirectionalProjections(pixelProjections, offscreenBody)"))
        assertTrue(sky.contains("fitSpaceCompassCelestialOffscreenDiameter(directionalProjections"))
        assertTrue(sky.contains("placeSpaceCompassCelestialOffscreenMarkers(directionalProjections"))
        assertTrue(sky.contains("paths.forEach"))
        assertTrue(sky.contains("spaceCompassCelestialCatalogOrder.forEach"))
        assertTrue(sky.contains("if (projected.visible) SpaceCompassCelestialLiveMarker"))
        assertTrue(source("SpaceCompassSunFinderScreen.kt").contains("offscreenBody = body.takeIf { hasActiveBody }"))
    }

    @Test fun everyTargetReusesItsCataloguePreviewAtTheSameSize() {
        val sky = source("SpaceCompassSunFinderSky.kt")
        val marker = source("SpaceCompassCelestialLiveMarker.kt")
        assertTrue(sky.contains("SpaceCompassCelestialLiveMarker(candidate, projected"))
        assertTrue(marker.contains("SpaceCompassCelestialThumbnail(body"))
        assertEquals(32f, SPACE_COMPASS_CELESTIAL_LIVE_PREVIEW_SIZE_DP, 0f)
        assertFalse(marker.contains("when (body)"))
        assertFalse(sky.contains("drawSpaceCompassCelestialMarker"))
        assertTrue(marker.contains("marker.place("))
        assertFalse(marker.contains("placeRelative("))
    }

    @Test fun belowHorizonPreviewsUseOneDesaturationPolicyWithoutChangingTheCache() {
        val common = spaceCompassSunAppearance(-20.0, true)
        for (elevation in listOf(-0.001, -1.0, -20.0, -90.0)) for (rising in listOf(false, true)) {
            assertEquals(common, spaceCompassSunAppearance(elevation, rising))
        }
        assertTrue(common.belowHorizon)
        assertEquals(0f, common.glowStrength, 0f)
        assertEquals(0xFFBBA6FF.toInt(), common.edgeArgb)
        val thumbnail = source("SpaceCompassCelestialThumbnail.kt")
        assertTrue(source("SpaceCompassCelestialLiveMarker.kt").contains("muted = belowHorizon"))
        assertTrue(thumbnail.contains("setToSaturation(0f)"))
        assertTrue(thumbnail.contains("colorFilter = colorFilter, alpha = opacity"))
        assertTrue(thumbnail.contains("modifier.clip(CircleShape), colorFilter, opacity"))
        assertTrue(source("SpaceCompassCelestialCraftModel.kt").contains("colorFilter = colorFilter, alpha = opacity"))
    }

    @Test fun offscreenArrowKeepsItsBearingAndAlignedReticleKeepsItsTargetColour() {
        val sky = source("SpaceCompassSunFinderSky.kt")
        assertTrue(sky.contains("val sunTint = appearance?.let { Color(it.edgeArgb) }"))
        assertTrue(sky.contains("val reticle = if (aligned) sunTint else Color.White"))
        assertTrue(sky.contains("SpaceCompassCelestialOffscreenMarker(candidate, projected, below"))
        val locator = source("SpaceCompassCelestialOffscreenMarker.kt")
        assertTrue(locator.contains("SpaceCompassCelestialThumbnail(body"))
        assertTrue(locator.contains("muted = belowHorizon"))
        assertTrue(locator.contains("rotate(bearing, center)"))
        assertTrue(locator.contains("drawPath(arrow, pointerColor)"))
        assertTrue(locator.contains("marker.place("))
        assertFalse(locator.contains("placeRelative("))
    }

    @Test fun visualizeUsesATelescopeWithoutChangingTheSharedControlOrItsAccessibleLabel() {
        val actions = source("SpaceCompassSunDailyPathLayer.kt").substringAfter("internal fun SpaceCompassCelestialSkyActions(")
        assertTrue(actions.contains("R.string.celestial_view_open"))
        assertTrue(actions.contains("SpaceCompassCelestialIconControl(false, viewTitle, onVisualize"))
        assertTrue(actions.contains("SpaceCompassCelestialTelescopeIcon(Modifier.size(22.dp))"))
        assertFalse(actions.contains("SpaceCompassCelestialThumbnail("))
        assertTrue(source("SpaceCompassCelestialTelescopeIcon.kt").contains("1.5.dp.toPx()"))
    }

    @Test fun moonMiniaturesMaskTheSamePhaseSilhouetteWithoutReplacingTheSharedTextureCache() {
        val thumbnail = source("SpaceCompassCelestialThumbnail.kt")
        assertTrue(thumbnail.contains("moonPhase.takeIf { body == SpaceCompassCelestialBody.MOON }"))
        assertTrue(thumbnail.contains("phase?.let(::spaceCompassMoonPhaseSilhouette)"))
        assertTrue(source("SpaceCompassMoonPhaseIcon.kt").contains("remember(phase) { spaceCompassMoonPhaseSilhouette(phase) }"))
        assertTrue(thumbnail.contains("clipPath(litArea)"))
        assertTrue(thumbnail.contains("alpha = opacity * 0.10f"))
        assertTrue(thumbnail.contains("celestialThumbnailCache[body]"))
        assertFalse(thumbnail.contains("calculateSpaceCompassMoonPhase"))
    }

    @Test fun liveAndEdgeMoonShareOneClockPhaseWithoutOrientationOrSelectedPointRecalculations() {
        val sky = source("SpaceCompassSunFinderSky.kt")
        assertTrue(sky.contains("rememberSpaceCompassMoonMarkerPhase(timeMs.takeIf { SpaceCompassCelestialBody.MOON in positions })"))
        assertEquals(2, Regex("moonPhase = moonPhase").findAll(sky).count())
        for (file in listOf("SpaceCompassCelestialLiveMarker.kt", "SpaceCompassCelestialOffscreenMarker.kt"))
            assertTrue(source(file).contains("muted = belowHorizon, moonPhase = moonPhase"))
        val state = source("SpaceCompassMoonMarkerPhase.kt")
        assertTrue(state.contains("Math.floorDiv(it, 60_000L)"))
        assertTrue(state.contains("produceState<SpaceCompassMoonPhase?>(null, minute)"))
        assertTrue(state.contains("withContext(Dispatchers.Default)"))
        assertFalse(state.contains("orientation") && state.contains("produceState<SpaceCompassMoonPhase?>(null, orientation"))
        assertTrue(source("SpaceCompassCelestialPath.kt").contains("point.copy(moonPhase = calculateSpaceCompassMoonPhase(point.timeMs))"))
    }
}
