package me.mondiversi.spacecompass

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import org.junit.Assert.*
import org.junit.Test

class SpaceCompassMainFreeAreaGestureTest {
    private val scene = SpaceCompassSunSceneFrame(10.0, 10.0, 380.0, 400.0)

    @Test fun blankIslandAndGapsWorkWhileTheSkyRetainsPointPicking() {
        assertFalse(spaceCompassMainFreeAreaGestureAllowed(Offset(200f,200f), Offset.Zero, scene, emptyList(),12f))
        assertTrue(spaceCompassMainFreeAreaGestureAllowed(Offset(200f,460f), Offset.Zero, scene, emptyList(),12f))
        assertTrue(spaceCompassMainFreeAreaGestureAllowed(Offset(4f,260f), Offset.Zero, scene, emptyList(),12f))
    }

    @Test fun linksAndButtonsHaveTheirExistingSafetyGapInRootCoordinates() {
        val origin = Offset(0f,30f)
        val controls = listOf(Rect(80f,450f,160f,490f))
        assertFalse(spaceCompassMainFreeAreaGestureAllowed(Offset(74f,430f),origin,scene,controls,12f))
        assertTrue(spaceCompassMainFreeAreaGestureAllowed(Offset(60f,430f),origin,scene,controls,12f))
        assertFalse(spaceCompassMainFreeAreaGestureAllowed(Offset(200f,460f),origin,null,controls,12f))
    }

    @Test fun freeCircleAvoidsTheExplanationAndEveryControlInBothOrientations() {
        for(viewport in listOf(Rect(0f,0f,400f,800f),Rect(0f,0f,900f,400f))) {
            val card=Rect(viewport.center.x-150,viewport.center.y-100,viewport.center.x+150,viewport.center.y+100)
            val blocked=listOf(card,Rect(0f,0f,180f,90f),Rect(viewport.right-180,viewport.bottom-90,viewport.right,viewport.bottom))
            val circle=spaceCompassTutorialFreeArea(viewport,blocked,24f)!!
            assertTrue(viewport.contains(circle.topLeft));assertTrue(viewport.contains(circle.bottomRight))
            assertTrue(blocked.none(circle::overlaps));assertEquals(circle.width,circle.height,.001f)
        }
    }

    @Test fun crowdedLayoutsNeverHighlightAButtonAsBlankSpace() {
        val viewport=Rect(0f,0f,100f,100f)
        assertNull(spaceCompassTutorialFreeArea(viewport,listOf(viewport),24f))
    }
}
