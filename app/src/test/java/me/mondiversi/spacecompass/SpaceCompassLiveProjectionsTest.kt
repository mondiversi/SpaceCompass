package me.mondiversi.spacecompass

import androidx.compose.runtime.mutableStateOf
import org.junit.Assert.*
import org.junit.Test
import kotlin.math.*

class SpaceCompassLiveProjectionsTest {
    @Test fun viewChoicesKeepLegacySkyAndGlobeDefaultsAndCameraSeparate() {
        assertEquals(listOf(SpaceCompassViewMode.PANORAMA,SpaceCompassViewMode.SKY_PANORAMA,SpaceCompassViewMode.CAMERA,
            SpaceCompassViewMode.GLOBE,SpaceCompassViewMode.GLOBE_PANORAMA,SpaceCompassViewMode.OBJECT),SpaceCompassViewMode.entries)
        assertEquals(SpaceCompassViewMode.PANORAMA,spaceCompassLiveViewMode(false,false))
        assertEquals(SpaceCompassViewMode.GLOBE,spaceCompassLiveViewMode(true,false))
        assertEquals(SpaceCompassViewMode.SKY_PANORAMA,spaceCompassLiveViewMode(false,true))
        assertEquals(SpaceCompassViewMode.GLOBE_PANORAMA,spaceCompassLiveViewMode(true,true))
        assertFalse(SpaceCompassViewMode.CAMERA.panoramic);assertFalse(SpaceCompassViewMode.CAMERA.globe)
    }
    @Test fun disabledObjectModeLeavesTheExistingThreeFamiliesAvailable() {
        assertEquals(listOf(SpaceCompassViewMode.PANORAMA,SpaceCompassViewMode.CAMERA,SpaceCompassViewMode.GLOBE),SPACE_COMPASS_VIEW_MODE_FAMILIES)
        assertFalse(spaceCompassObjectViewEnabled(true))
        assertEquals(SpaceCompassViewMode.GLOBE,spaceCompassAvailableViewMode(SpaceCompassViewMode.OBJECT))
        val tutorial=SpaceCompassTutorialState(mutableStateOf(true),mutableStateOf(false),mutableStateOf(false))
        tutorial.chooseMode(SpaceCompassViewMode.OBJECT)
        assertEquals(SpaceCompassViewMode.GLOBE,tutorial.previewMode)
    }
    @Test fun angularVectorsRoundTripToTheExistingAzimuthElevationProjection() {
        for(az in listOf(0.0,1.0,90.0,179.0,180.0,270.0,359.0)) for(el in listOf(-89.9,-45.0,0.0,45.0,89.9)) {
            val v=spaceCompassSkyPanoramaVector(SpaceCompassSunPosition(az,el))!!
            val point=spaceCompassPanoramaVectorPoint(SpaceCompassSunVector(v.x,v.z,v.y),800.0,400.0)!!
            val expected=spaceCompassPanoramaPoint(SpaceCompassSunPosition(az,el),800.0,400.0)!!
            assertEquals(expected.x,point.x,1e-8);assertEquals(expected.y,point.y,1e-8)
            assertEquals(1.0,v.length,1e-12)
        }
    }
    @Test fun invalidAngularDirectionsDoNotBecomeInventedMarkers() {
        assertNull(spaceCompassSkyPanoramaVector(SpaceCompassSunPosition(0.0,90.1)))
        assertNull(spaceCompassSkyPanoramaVector(SpaceCompassSunPosition(Double.NaN,0.0)))
    }
    @Test fun tutorialCanExploreAllChoicesWithoutEnablingTheActualCamera() {
        val state=SpaceCompassTutorialState(mutableStateOf(false),mutableStateOf(false),mutableStateOf(false))
        state.start()
        for(mode in SpaceCompassViewMode.entries.filter { spaceCompassAvailableViewMode(it)==it }) {
            state.chooseMode(mode);assertEquals(mode,state.previewMode)
            assertFalse(spaceCompassTutorialCameraEnabled(true,state.active))
        }
        state.close();state.start();assertEquals(SpaceCompassViewMode.PANORAMA,state.previewMode)
    }
    @Test fun tutorialPanoramicChoiceSurvivesRecreationWithoutAffectingTheRealCamera() {
        val active=mutableStateOf(true);val welcome=mutableStateOf(false)
        val camera=mutableStateOf(false);val globe=mutableStateOf(false);val panoramic=mutableStateOf(false)
        SpaceCompassTutorialState(active,welcome,camera,globe,panoramic).chooseMode(SpaceCompassViewMode.GLOBE_PANORAMA)
        val restored=SpaceCompassTutorialState(active,welcome,camera,globe,panoramic)
        assertEquals(SpaceCompassViewMode.GLOBE_PANORAMA,restored.previewMode)
        assertFalse(spaceCompassTutorialCameraEnabled(true,restored.active));assertFalse(restored.modeMenuOpen)
        restored.close();assertFalse(panoramic.value);assertFalse(globe.value)
    }

}
