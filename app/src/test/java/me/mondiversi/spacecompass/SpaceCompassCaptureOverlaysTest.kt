package me.mondiversi.spacecompass

import java.time.Instant
import org.junit.Assert.*
import org.junit.Test

class SpaceCompassCaptureOverlaysTest {
    private fun snapshot() = SpaceCompassPanoramaSnapshot(
        Instant.parse("2026-11-13T12:34:56Z").toEpochMilli(), 45.1, 9.2, 120.0, SpaceCompassSunSkyPhase.MORNING,
        null, listOf(SpaceCompassPanoramaObject(SpaceCompassCelestialBody.SUN, "Sun", null)),
        SpaceCompassCelestialRemoteData(), "Frozen caption", emptyList(), emptyList(), zoneId = "Europe/Rome",showGridNumbers = false)

    @Test fun firstInstallEnablesOnlyTheGrid() {
        val defaults = SpaceCompassCaptureOverlays.fromSnapshot(null)
        assertTrue(defaults.grid)
        assertFalse(defaults.references)
        assertEquals(SpaceCompassCaptureOverlays(), defaults)
    }

    @Test fun everyCaptureInheritsBothLiveChoicesWithoutReusingAnEarlierPreviewEdit() {
        for (grid in listOf(false, true)) for (references in listOf(false, true)) {
            val source = snapshot().copy(showGrid = grid, showSkyReferences = references)
            val captured = SpaceCompassCaptureOverlays.fromSnapshot(source)
            assertEquals(SpaceCompassCaptureOverlays(grid, references), captured)
            val edited = captured.copy(grid = !grid, references = !references).applyTo(source)
            assertEquals(SpaceCompassCaptureOverlays(!grid, !references), SpaceCompassCaptureOverlays.fromSnapshot(edited))
            assertEquals(captured, SpaceCompassCaptureOverlays.fromSnapshot(source))
            assertEquals(source.timeMs, edited.timeMs)
            assertEquals(source.latitude, edited.latitude, 0.0)
            assertEquals(source.longitude, edited.longitude, 0.0)
            assertEquals(source.objects, edited.objects)
            assertEquals(source.caption, edited.caption)
            assertEquals(source.zoneId, edited.zoneId)
        }
    }

    @Test fun gridAndReferenceImageCachesHaveFourDistinctIdentities() {
        val keys = mutableSetOf<String>()
        for (grid in listOf(false, true)) for (references in listOf(false, true))
            assertTrue(keys.add(SpaceCompassCaptureOverlays(grid, references).cacheKey))
        assertEquals(4, keys.size)
    }

    @Test fun rotatingThePreviewRestoresBothAppliedChoicesTogether() {
        val original = SpaceCompassCaptureOverlays(false, true)
        val restored = spaceCompassCaptureOverlaysSaver.restore(listOf(original.grid, original.references))
        assertEquals(original, restored)
    }

    @Test fun gridNumberChoicesSurvivePreviewRotationAndNeverChangeFrozenAstronomy() {
        for(grid in listOf(false,true)) for(references in listOf(false,true)) for(numbers in listOf(false,true)) {
            val source=snapshot().copy(showGrid=grid,showSkyReferences=references,showGridNumbers=numbers)
            val captured=SpaceCompassCaptureOverlays.fromSnapshot(source)
            assertEquals(numbers,captured.numbers)
            assertEquals(captured,spaceCompassCaptureOverlaysSaver.restore(listOf(grid,references,numbers)))
            val edited=captured.copy(numbers=!numbers).applyTo(source)
            assertEquals(!numbers,edited.showGridNumbers)
            assertEquals(source,edited.copy(showGridNumbers=numbers))
        }
    }
    @Test fun numberedAndUnnumberedImagesCannotShareACacheEntry() {
        val keys=mutableSetOf<String>()
        for(grid in listOf(false,true)) for(references in listOf(false,true)) for(numbers in listOf(false,true))
            assertTrue(keys.add(SpaceCompassCaptureOverlays(grid,references,numbers).cacheKey))
        assertEquals(8,keys.size)
    }
}
