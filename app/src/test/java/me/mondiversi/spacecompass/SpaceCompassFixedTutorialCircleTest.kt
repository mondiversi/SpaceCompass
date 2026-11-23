package me.mondiversi.spacecompass

import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import org.junit.Assert.*
import org.junit.Test

class SpaceCompassFixedTutorialCircleTest {
    @Test fun compactAndExpandedIslandsUseTheSameWindowAnchorInEachOrientation() {
        for(root in listOf(Rect(0f,0f,400f,800f),Rect(0f,0f,900f,400f))) {
            val circle=spaceCompassTutorialFixedFreeArea(root,24f,60f)
            for(sky in listOf(root,Rect(root.left,root.top,root.right*.55f,root.bottom*.7f))) {
                val layout=spaceCompassTutorialExplanationLayout(sky,emptyList(),circle,360f,240f,8f,150f)
                val size=Size(minOf(360f,layout.area.width-16f),layout.height)
                val card=Rect(spaceCompassTutorialCardPosition(layout.area,size,8f),size)
                assertEquals(root.center.x,circle.center.x,.01f)
                assertEquals(60f,circle.top,.01f)
                assertTrue(layout.height>0)
                assertFalse(card.overlaps(circle.inflate(7f)))
                assertTrue(card.left>=sky.left);assertTrue(card.right<=sky.right)
            }
        }
    }

    @Test fun shortLandscapeCanKeepTheCardBesideTheCircleAndAwayFromSideControls() {
        val sky=Rect(0f,0f,600f,300f)
        val circle=spaceCompassTutorialFixedFreeArea(sky,24f,60f)
        val targets=listOf(
            SpaceCompassTutorialTarget(SpaceCompassTutorialFeature.MENU,Rect(10f,55f,45f,90f)),
            SpaceCompassTutorialTarget(SpaceCompassTutorialFeature.GLOBE_LABELS,Rect(550f,100f,585f,135f)),
            SpaceCompassTutorialTarget(SpaceCompassTutorialFeature.ZOOM,Rect(550f,155f,585f,190f)),
            SpaceCompassTutorialTarget(SpaceCompassTutorialFeature.OBJECTS,Rect(10f,150f,45f,185f)))
        val layout=spaceCompassTutorialExplanationLayout(sky,targets,circle,360f,184f,8f,150f)
        val size=Size(minOf(360f,layout.area.width-16),layout.height)
        val card=Rect(spaceCompassTutorialCardPosition(layout.area,size,8f),size)
        assertTrue(layout.height>=80f)
        assertFalse(card.overlaps(circle.inflate(7f)))
        assertTrue(targets.none { card.overlaps(it.bounds.inflate(7f)) })
    }
}
