package me.mondiversi.spacecompass

import androidx.compose.runtime.mutableStateOf
import org.junit.Assert.*
import org.junit.Test

class SpaceCompassTutorialCameraTest {
    private fun state()=SpaceCompassTutorialState(mutableStateOf(false),mutableStateOf(false),mutableStateOf(false))

    @Test fun openingTheTutorialUsesEachActualViewInsteadOfResettingToSky() {
        for(mode in SpaceCompassViewMode.entries.filter { spaceCompassAvailableViewMode(it)==it }) {
            val guide=state();guide.observeMode(mode);guide.start()
            assertEquals(mode,guide.previewMode)
            assertFalse(spaceCompassTutorialCameraEnabled(true,guide.active))
        }
    }
    @Test fun everyExplanationLeavesTheModeAndSceneControlsUntouched() {
        for(mode in SpaceCompassViewMode.entries.filter { spaceCompassAvailableViewMode(it)==it }) {
            val guide=state();guide.observeMode(mode);guide.start()
            for(feature in SpaceCompassTutorialFeature.entries.filterNot { it==SpaceCompassTutorialFeature.MENU }) {
                guide.explore(feature)
                assertTrue(guide.active);assertEquals(mode,guide.previewMode)
                assertEquals(mode,guide.liveMode)
            }
        }
    }
    @Test fun closeControlClosesTheGuideAndRetainsTheActualViewForTheNextVisit() {
        val guide=state();guide.observeMode(SpaceCompassViewMode.GLOBE_PANORAMA);guide.start()
        guide.explore(SpaceCompassTutorialFeature.GUIDES)
        guide.explore(SpaceCompassTutorialFeature.MENU)
        assertFalse(guide.active);assertFalse(guide.cameraPreview);assertFalse(guide.globePreview)
        assertTrue(guide.targets.isEmpty());assertNull(guide.skyViewport)
        guide.start();assertEquals(SpaceCompassViewMode.GLOBE_PANORAMA,guide.previewMode)
    }
    @Test fun closingGuideRestoresThePreviousRealCameraRequest() {
        val guide=state();guide.observeMode(SpaceCompassViewMode.CAMERA);guide.start()
        assertTrue(guide.cameraPreview);assertFalse(spaceCompassTutorialCameraEnabled(true,guide.active))
        guide.close();assertFalse(guide.cameraPreview)
        assertTrue(spaceCompassTutorialCameraEnabled(true,guide.active))
        assertFalse(spaceCompassTutorialCameraEnabled(false,guide.active))
    }
    @Test fun changingTheActualModeUpdatesDescriptionsWithoutAnInTutorialAction() {
        val guide=state();guide.start();guide.observeMode(SpaceCompassViewMode.GLOBE)
        assertEquals(SpaceCompassViewMode.GLOBE,guide.previewMode)
        guide.explore(SpaceCompassTutorialFeature.CAMERA)
        assertEquals(SpaceCompassViewMode.GLOBE,guide.previewMode)
        guide.observeMode(SpaceCompassViewMode.SKY_PANORAMA)
        assertEquals(SpaceCompassViewMode.SKY_PANORAMA,guide.previewMode)
    }
    @Test fun savedContextSurvivesRecreationAndIgnoresInactiveExplanationTaps() {
        val active=mutableStateOf(true);val welcome=mutableStateOf(false)
        val camera=mutableStateOf(false);val globe=mutableStateOf(false);val panoramic=mutableStateOf(false)
        SpaceCompassTutorialState(active,welcome,camera,globe,panoramic).observeMode(SpaceCompassViewMode.GLOBE_PANORAMA)
        val restored=SpaceCompassTutorialState(active,welcome,camera,globe,panoramic)
        assertEquals(SpaceCompassViewMode.GLOBE_PANORAMA,restored.previewMode)
        restored.close();restored.explore(SpaceCompassTutorialFeature.CAMERA)
        assertFalse(restored.active);assertFalse(restored.cameraPreview)
    }
}
