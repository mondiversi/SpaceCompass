package me.mondiversi.spacecompass

import androidx.compose.runtime.mutableStateOf
import org.junit.Assert.*
import org.junit.Test

class SpaceCompassTutorialCameraTest {
    private fun state()=SpaceCompassTutorialState(mutableStateOf(false),mutableStateOf(false),mutableStateOf(false))

    @Test fun selectorOpensTheMenuWithoutChangingThePreviewUntilAChoiceIsMade() {
        val guide=state();guide.start()
        guide.explore(SpaceCompassTutorialFeature.CAMERA)
        assertTrue(guide.modeMenuOpen)
        assertEquals(SpaceCompassViewMode.PANORAMA,guide.previewMode)
        guide.dismissModeMenu()
        assertFalse(guide.modeMenuOpen)
        assertEquals(SpaceCompassViewMode.PANORAMA,guide.previewMode)
        for(mode in listOf(SpaceCompassViewMode.GLOBE,SpaceCompassViewMode.CAMERA,SpaceCompassViewMode.PANORAMA)) {
            guide.explore(SpaceCompassTutorialFeature.CAMERA);assertTrue(guide.modeMenuOpen)
            guide.chooseMode(mode)
            assertEquals(mode,guide.previewMode);assertFalse(guide.modeMenuOpen)
            assertFalse(spaceCompassTutorialCameraEnabled(true,guide.active))
            SpaceCompassTutorialFeature.entries.filterNot { it in setOf(SpaceCompassTutorialFeature.CAMERA,
                SpaceCompassTutorialFeature.MENU) }.forEach { feature ->
                guide.explore(feature);assertEquals(mode,guide.previewMode)
            }
        }
    }
    @Test fun dismissingTheChooserKeepsTheSelectedControlSet() {
        val guide=state();guide.start();guide.chooseMode(SpaceCompassViewMode.GLOBE)
        guide.explore(SpaceCompassTutorialFeature.CAMERA);guide.dismissModeMenu()
        assertTrue(guide.globePreview);assertFalse(guide.cameraPreview)
    }
    @Test fun menuClosesGuideAndClearsBothPreviewAndChooser() {
        val guide=state();guide.start();guide.chooseMode(SpaceCompassViewMode.GLOBE)
        guide.explore(SpaceCompassTutorialFeature.CAMERA);guide.explore(SpaceCompassTutorialFeature.MENU)
        assertFalse(guide.active);assertFalse(guide.cameraPreview);assertFalse(guide.globePreview)
        assertFalse(guide.modeMenuOpen);assertTrue(guide.targets.isEmpty());assertNull(guide.skyViewport)
    }
    @Test fun closingGuideRestoresThePreviousRealCameraRequest() {
        val guide=state();guide.start();guide.chooseMode(SpaceCompassViewMode.CAMERA)
        assertFalse(spaceCompassTutorialCameraEnabled(true,guide.active))
        guide.close();assertFalse(guide.cameraPreview)
        assertTrue(spaceCompassTutorialCameraEnabled(true,guide.active))
        assertFalse(spaceCompassTutorialCameraEnabled(false,guide.active))
    }
    @Test fun newVisitStartsWithNormalControlsAndInactiveChoicesAreIgnored() {
        val guide=state();guide.start();guide.chooseMode(SpaceCompassViewMode.GLOBE)
        guide.close();guide.start();assertEquals(SpaceCompassViewMode.PANORAMA,guide.previewMode)
        guide.close();guide.chooseMode(SpaceCompassViewMode.CAMERA);guide.explore(SpaceCompassTutorialFeature.CAMERA)
        assertFalse(guide.cameraPreview);assertFalse(guide.modeMenuOpen)
    }
    @Test fun savedGuideChoiceSurvivesRecreationWithoutReopeningTheMenu() {
        val active=mutableStateOf(true);val welcome=mutableStateOf(false)
        val camera=mutableStateOf(false);val globe=mutableStateOf(false)
        SpaceCompassTutorialState(active,welcome,camera,globe).chooseMode(SpaceCompassViewMode.GLOBE)
        val restored=SpaceCompassTutorialState(active,welcome,camera,globe)
        assertEquals(SpaceCompassViewMode.GLOBE,restored.previewMode)
        assertFalse(restored.modeMenuOpen);assertFalse(spaceCompassTutorialCameraEnabled(true,restored.active))
        restored.close();assertFalse(active.value);assertFalse(globe.value)
    }
}
