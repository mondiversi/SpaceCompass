package me.mondiversi.spacecompass

import org.junit.Assert.*
import org.junit.Test

class SpaceCompassGlobeCaptureContextTest {
    @Test fun globeScopesRetainDefaultsAndMigrateOldStoredChoices() {
        assertEquals(SpaceCompassPanoramaLabelMode.KEY_SELECTED,spaceCompassGlobeLabelMode(null))
        assertEquals(SpaceCompassPanoramaLabelMode.NONE,spaceCompassGlobeLabelMode(null,false))
        assertEquals(SpaceCompassPanoramaLabelMode.ALL,spaceCompassGlobeLabelMode("all-objects"))
        assertEquals(SpaceCompassPanoramaLabelMode.KEY_SELECTED,spaceCompassGlobeLabelMode("selected-object"))
        for(mode in SpaceCompassPanoramaLabelMode.entries) {
            val live=spaceCompassGlobeLabelMode(mode.key)
            assertEquals(if(mode==SpaceCompassPanoramaLabelMode.OBJECTS_SELECTED) SpaceCompassPanoramaLabelMode.KEY_SELECTED else mode,live)
            assertTrue(live in SPACE_COMPASS_GLOBE_MAIN_LABEL_MODES)
        }
        for(mode in SpaceCompassPanoramaLabelMode.entries) {
            val frozen=SpaceCompassGlobeCapture(SpaceCompassGlobePose(),"Directions",labelMode=mode)
            assertEquals(mode,frozen.labelMode)
        }
    }
    @Test fun allFiveScopesMatchTheOrdinaryLabelPolicyForHourlyKeyCurrentAndAntipodalPoints() {
        for(mode in SpaceCompassPanoramaLabelMode.entries) for(active in listOf(false,true))
            for(selected in listOf(false,true)) for(current in listOf(false,true)) for(antipode in listOf(false,true))
                for(event in SpaceCompassSunPathEvent.entries) {
                    val expected=mode.showsPoint(selected,active,!antipode&&(current||event!=SpaceCompassSunPathEvent.HOUR))
                    assertEquals(expected,spaceCompassGlobeCaptureShowsLabel(mode,active,current,antipode,setOf(event),selected))
                }
    }
    @Test fun liveLabelsFollowActiveTrackButKeepSelectionsFromOtherTracks() {
        val key=setOf(SpaceCompassSunPathEvent.SUNRISE)
        val hour=setOf(SpaceCompassSunPathEvent.HOUR)
        for(mode in SPACE_COMPASS_GLOBE_MAIN_LABEL_MODES) {
            // Swapping the island transfers ordinary labels; explicit pins remain readable on every track.
            for(selected in listOf(false,true)) for(current in listOf(false,true)) for(antipode in listOf(false,true)) {
                assertEquals(selected && mode!=SpaceCompassPanoramaLabelMode.NONE,
                    spaceCompassGlobeMainShowsLabel(mode,false,current,antipode,key,selected))
                assertEquals(spaceCompassGlobeCaptureShowsLabel(mode,true,current,antipode,key,selected),
                    spaceCompassGlobeMainShowsLabel(mode,true,current,antipode,key,selected))
            }
        }
        assertTrue(spaceCompassGlobeMainShowsLabel(SpaceCompassPanoramaLabelMode.ALL,true,false,false,hour))
        assertFalse(spaceCompassGlobeMainShowsLabel(SpaceCompassPanoramaLabelMode.KEY_SELECTED,true,false,false,hour))
        assertTrue(spaceCompassGlobeMainShowsLabel(SpaceCompassPanoramaLabelMode.KEY_SELECTED,true,false,false,key))
        assertFalse(spaceCompassGlobeMainShowsLabel(SpaceCompassPanoramaLabelMode.SELECTED,true,true,false,key))
        assertTrue(spaceCompassGlobeMainShowsLabel(SpaceCompassPanoramaLabelMode.SELECTED,true,false,true,hour,true))
        assertTrue(spaceCompassGlobeCaptureShowsLabel(SpaceCompassPanoramaLabelMode.ALL,false,false,false,hour))
        assertTrue(spaceCompassGlobeCaptureShowsLabel(SpaceCompassPanoramaLabelMode.KEY_SELECTED,false,false,false,key))
    }
    @Test fun geographicMarkerHasOneAzureBaseAndAFadedGreyBackFace() {
        assertEquals(0xff55c8db.toInt(),spaceCompassGlobeLocationMarkerColor(1.0))
        assertEquals(spaceCompassGlobeLocationMarkerColor(.08),spaceCompassGlobeLocationMarkerColor(1.0))
        val back=spaceCompassGlobeLocationMarkerColor(-1.0)
        assertTrue((back ushr 24) in 1..254)
        assertEquals((back ushr 16) and 255,(back ushr 8) and 255)
        assertEquals((back ushr 8) and 255,back and 255)
    }
    @Test fun adaptivePointDetailsPreserveClockCountdownAndRestoreFullAnglesAndIdentity() {
        val text="14:00 · T−0h58m"
        val details=formatSpaceCompassGlobeMainLabelDetails("Sun","Culmination",text,
            SpaceCompassSunPosition(207.0,59.8),spaceCompassPanoramaInternationalFormatting)
        assertTrue(details.full.startsWith("Sun · Culmination\n"))
        assertTrue(details.full.contains(text));assertTrue(details.full.contains("↔"));assertTrue(details.full.contains("↕ +59.8°"))
        assertEquals("Culmination\n"+text,details.compact)
        assertEquals("Culmination · "+text,details.minimal)
        val hourly=formatSpaceCompassGlobeMainLabelDetails("Moon","",text,
            SpaceCompassSunPosition(0.0,-20.0),spaceCompassPanoramaInternationalFormatting)
        assertEquals(text,hourly.minimal);assertTrue(hourly.full.contains("Moon"));assertTrue(hourly.full.contains("↕ -20.0°"))
    }

}
