package me.mondiversi.spacecompass

import androidx.compose.runtime.mutableStateOf
import org.junit.Assert.*
import org.junit.Test

class SpaceCompassTutorialCameraTest {
    private fun state() = SpaceCompassTutorialState(mutableStateOf(false), mutableStateOf(false), mutableStateOf(false))

    @Test fun cameraExplorationOpensAndClosesOnlyTheGuidePreview() {
        val guide = state()
        guide.start()
        guide.explore(SpaceCompassTutorialFeature.CAMERA)
        assertTrue(guide.active)
        assertTrue(guide.cameraPreview)
        assertFalse(spaceCompassTutorialCameraEnabled(true, guide.active))
        guide.explore(SpaceCompassTutorialFeature.ZOOM)
        guide.explore(SpaceCompassTutorialFeature.TRANSPARENCY)
        assertTrue(guide.cameraPreview)
        guide.explore(SpaceCompassTutorialFeature.CAMERA)
        assertFalse(guide.cameraPreview)
        assertTrue(guide.active)
    }

    @Test fun menuClosesGuideAndClearsItsCameraPreview() {
        val guide = state()
        guide.start()
        guide.explore(SpaceCompassTutorialFeature.CAMERA)
        guide.explore(SpaceCompassTutorialFeature.MENU)
        assertFalse(guide.active)
        assertFalse(guide.cameraPreview)
        assertTrue(guide.targets.isEmpty())
        assertNull(guide.skyViewport)
    }

    @Test fun closingGuideRestoresThePreviousRealCameraRequest() {
        val guide = state()
        val requested = true
        guide.start()
        guide.explore(SpaceCompassTutorialFeature.CAMERA)
        assertFalse(spaceCompassTutorialCameraEnabled(requested, guide.active))
        guide.close()
        assertFalse(guide.cameraPreview)
        assertTrue(spaceCompassTutorialCameraEnabled(requested, guide.active))
        assertFalse(spaceCompassTutorialCameraEnabled(false, guide.active))
    }

    @Test fun newVisitStartsWithNormalControlsAndClosedCamera() {
        val guide = state()
        guide.start(); guide.explore(SpaceCompassTutorialFeature.CAMERA); guide.close(); guide.start()
        assertFalse(guide.cameraPreview)
        assertFalse(spaceCompassTutorialCameraEnabled(false, guide.active))
        guide.close(); guide.explore(SpaceCompassTutorialFeature.CAMERA)
        assertFalse(guide.cameraPreview)
    }

    @Test fun savedGuideCameraChoiceSurvivesStateRecreation() {
        val active = mutableStateOf(true)
        val welcome = mutableStateOf(false)
        val preview = mutableStateOf(false)
        SpaceCompassTutorialState(active, welcome, preview).explore(SpaceCompassTutorialFeature.CAMERA)
        val restored = SpaceCompassTutorialState(active, welcome, preview)
        assertTrue(restored.active)
        assertTrue(restored.cameraPreview)
        assertFalse(spaceCompassTutorialCameraEnabled(true, restored.active))
        restored.close()
        assertFalse(active.value)
        assertFalse(preview.value)
    }
}
