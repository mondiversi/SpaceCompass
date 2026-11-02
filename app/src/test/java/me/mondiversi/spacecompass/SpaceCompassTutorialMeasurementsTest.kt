package me.mondiversi.spacecompass

import androidx.compose.ui.geometry.Rect
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.*
import org.junit.Test

class SpaceCompassTutorialMeasurementsTest {
    private val sky = Rect(0f, 0f, 400f, 600f)
    private fun target(feature: SpaceCompassTutorialFeature, bounds: Rect) = SpaceCompassTutorialTarget(feature, bounds)

    @Test fun availableGroupMembersDoNotWaitForSpecificLocationOrWeatherButtons() = runBlocking {
        val controls = listOf(target(SpaceCompassTutorialFeature.FORWARD, Rect(100f, 10f, 140f, 50f)),
            target(SpaceCompassTutorialFeature.AXIS, Rect(100f, 540f, 140f, 580f)))
        val measured = spaceCompassTutorialMeasurements(sky, controls)
        assertNotNull(measured)
        assertEquals(measured, awaitSpaceCompassTutorialMeasurements(flowOf(null, measured)))
    }

    @Test fun islandTitlesAndEmptyViewportsAreNotMistakenForReadyControlGroups() {
        val controls = listOf(target(SpaceCompassTutorialFeature.OBJECTS, Rect(20f, 400f, 300f, 450f)).copy(circular = false),
            target(SpaceCompassTutorialFeature.WEATHER, Rect(100f, 540f, 140f, 580f)))
        assertNull(spaceCompassTutorialMeasurements(sky, controls))
        assertNull(spaceCompassTutorialMeasurements(Rect.Zero, controls))
    }

    @Test fun missingLayoutCallbacksReleaseTheWaitForFallbackRendering() = runBlocking {
        withTimeout(5_000) {
            val neverMeasured = flow<SpaceCompassTutorialMeasurements?> { awaitCancellation() }
            assertNull(awaitSpaceCompassTutorialMeasurements(neverMeasured, timeoutMillis = 1))
        }
    }

    @Test fun staleOverlappingGroupsKeepAnAreaForTheExplanationInsteadOfZeroHeight() {
        val controls = listOf(target(SpaceCompassTutorialFeature.LOCATION, Rect(100f, 340f, 140f, 400f)),
            target(SpaceCompassTutorialFeature.WEATHER, Rect(100f, 250f, 140f, 300f)))
        val area = spaceCompassTutorialCardBand(sky, controls)
        assertEquals(sky, area)
        assertTrue(area.height > 0)
        assertTrue(spaceCompassTutorialCardHeightLimit(area.height) > 0)
    }
}
