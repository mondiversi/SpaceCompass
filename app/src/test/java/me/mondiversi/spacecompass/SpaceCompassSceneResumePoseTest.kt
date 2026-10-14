package me.mondiversi.spacecompass

import org.junit.Assert.*
import org.junit.Test

class SpaceCompassSceneResumePoseTest {
    private val first = SpaceCompassSunOrientation(SpaceCompassSunVector(1.0, 0.0, 0.0),
        SpaceCompassSunVector(0.0, 0.0, 1.0), SpaceCompassSunVector(0.0, 1.0, 0.0))
    private val second = SpaceCompassSunOrientation(SpaceCompassSunVector(0.0, 1.0, 0.0),
        SpaceCompassSunVector(0.0, 0.0, 1.0), SpaceCompassSunVector(-1.0, 0.0, 0.0))

    @Test fun coldStartDoesNotInventTerrainOrATrustedBearing() {
        val state = SpaceCompassSceneResumePose()
        val absent = state.resolve(null, false, true, 0)
        assertNull(absent.orientation); assertNull(absent.pointing)
        val initial = state.resolve(first, false, true, 0)
        assertSame(first, initial.orientation); assertNull(initial.pointing)
    }

    @Test fun pauseAndSensorRecoveryKeepTheVisualPoseWithoutChangingMeasurementTrust() {
        val state = SpaceCompassSceneResumePose()
        state.accept(first, 0)
        for (current in listOf(null, second)) {
            val held = state.resolve(current, false, true, 0)
            assertEquals(first, held.orientation); assertEquals(first, held.pointing)
            assertNull(spaceCompassSunTrustedPointingOrientation(current, false))
        }
        val fresh = state.resolve(second, true, true, 0)
        assertSame(second, fresh.orientation); assertSame(second, fresh.pointing)
        state.accept(second, 0)
        assertEquals(second, state.resolve(null, false, true, 0).orientation)
    }

    @Test fun aForegroundSensorFailureDoesNotReintroduceAnOldBearing() {
        val state = SpaceCompassSceneResumePose()
        state.accept(first, 0)
        assertNull(state.resolve(null, false, false, 0).orientation)
        val unreliable = state.resolve(second, false, false, 0)
        assertSame(second, unreliable.orientation); assertNull(unreliable.pointing)
    }

    @Test fun heldFramesRemapAllScreenRotationsWithoutChangingThePhysicalCameraAxis() {
        val state = SpaceCompassSceneResumePose()
        for (from in 0..3) for (to in 0..3) {
            state.accept(spaceCompassSceneRemapOrientation(first, 0, from), from)
            val held = state.resolve(null, false, true, to).orientation!!
            assertEquals(first, spaceCompassSceneRemapOrientation(held, to, 0))
            assertEquals(first.forward, held.forward)
            assertEquals(spaceCompassSunViewOrientation(first, true, false, 0),
                spaceCompassSunViewOrientation(held, true, false, to))
        }
    }
}
