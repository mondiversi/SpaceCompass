package me.mondiversi.spacecompass

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import org.junit.Assert.*
import org.junit.Test

class SpaceCompassTutorialTest {
    @Test fun portraitCardIsCenteredInTheSkyInsteadOfFollowingAControl() {
        val area = Rect(10f, 0f, 390f, 420f)
        assertEquals(Offset(40f, 105f), spaceCompassTutorialCardPosition(area, Size(320f, 210f), 12f))
    }
    @Test fun landscapeCardStaysInTheSkyColumn() {
        val area = Rect(10f, 0f, 430f, 350f)
        assertEquals(Offset(40f, 70f), spaceCompassTutorialCardPosition(area, Size(360f, 210f), 12f))
    }
    @Test fun mirroredSkyColumnRetainsItsOwnCenter() {
        val area = Rect(370f, 0f, 790f, 350f)
        assertEquals(Offset(400f, 70f), spaceCompassTutorialCardPosition(area, Size(360f, 210f), 12f))
    }
    @Test fun smallSkyFrameKeepsTheCardWithinItsMargins() {
        val area = Rect(20f, 30f, 300f, 250f)
        val card = Size(256f, 100f)
        val position = spaceCompassTutorialCardPosition(area, card, 12f)
        assertTrue(position.x >= area.left + 12 && position.y >= area.top + 12)
        assertTrue(position.x + card.width <= area.right - 12 && position.y + card.height <= area.bottom - 12)
    }
    @Test fun fixedControlWinsOverNearbyOrbitPoint() {
        val point=SpaceCompassTutorialTarget(SpaceCompassTutorialFeature.POINT,Rect(40f,40f,48f,48f),point=true)
        val control=SpaceCompassTutorialTarget(SpaceCompassTutorialFeature.CAMERA,Rect(20f,20f,68f,68f))
        assertEquals(control,spaceCompassTutorialHit(listOf(point,control),Offset(44f,44f),24f))
    }
    @Test fun nearestVisibleDotWinsAndEmptySkyHasNoHit() {
        val a=SpaceCompassTutorialTarget(SpaceCompassTutorialFeature.POINT,Rect(0f,0f,10f,10f),point=true)
        val b=a.copy(bounds=Rect(20f,0f,30f,10f))
        assertEquals(b,spaceCompassTutorialHit(listOf(a,b),Offset(21f,5f),24f))
        assertNull(spaceCompassTutorialHit(listOf(a,b),Offset(200f,200f),24f))
    }
    @Test fun nestedClickableLinkWinsOverLargerRegion() {
        val outer=SpaceCompassTutorialTarget(SpaceCompassTutorialFeature.GPS,Rect(0f,0f,300f,100f),false)
        val inner=SpaceCompassTutorialTarget(SpaceCompassTutorialFeature.OBJECTS,Rect(10f,10f,60f,60f),false)
        assertEquals(inner,spaceCompassTutorialHit(listOf(outer,inner),Offset(20f,20f),24f))
    }
}
