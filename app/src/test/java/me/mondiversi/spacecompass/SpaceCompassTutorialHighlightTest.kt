package me.mondiversi.spacecompass

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import org.junit.Assert.*
import org.junit.Test

class SpaceCompassTutorialHighlightTest {
    @Test fun narrowHighlightsDoNotShrinkTheInteractiveRegion() {
        listOf(SpaceCompassTutorialFeature.GPS, SpaceCompassTutorialFeature.NAVIGATION).forEach { feature ->
            val target = SpaceCompassTutorialTarget(feature, Rect(10f, 20f, 310f, 120f), circular = false,
                visualBounds = Rect(30f, 85f, 180f, 105f))
            val tap = Offset(260f, 40f)
            assertFalse(target.highlightBounds.contains(tap))
            assertEquals(target, spaceCompassTutorialHit(listOf(target), tap, 24f))
        }
    }

    @Test fun overlayOriginMovesBothTheHighlightAndItsTouchArea() {
        val target = SpaceCompassTutorialTarget(SpaceCompassTutorialFeature.GPS,
            Rect(20f, 50f, 300f, 180f), circular = false, visualBounds = Rect(30f, 140f, 180f, 160f))
        val moved = target.translated(Offset(-10f, -30f))
        assertEquals(Rect(10f, 20f, 290f, 150f), moved.bounds)
        assertEquals(Rect(20f, 110f, 170f, 130f), moved.highlightBounds)
        assertEquals(moved, spaceCompassTutorialHit(listOf(moved), Offset(280f, 30f), 24f))
    }

    @Test fun ordinaryControlsAndProjectedDotsRetainTheirDefaultHighlight() {
        val target = SpaceCompassTutorialTarget(SpaceCompassTutorialFeature.POINT, Rect(10f, 20f, 20f, 30f), point = true)
        assertEquals(target.bounds, target.highlightBounds)
        val moved = target.copy(bounds = Rect(40f, 50f, 50f, 60f)).translated(Offset(5f, 10f))
        assertEquals(moved.bounds, moved.highlightBounds)
    }
}
