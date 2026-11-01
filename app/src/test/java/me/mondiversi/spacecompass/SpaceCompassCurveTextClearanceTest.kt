package me.mondiversi.spacecompass

import org.junit.Assert.*
import org.junit.Test
import kotlin.math.hypot

class SpaceCompassCurveTextClearanceTest {
    private fun overlaps(a: SpaceCompassSunSceneFrame, b: SpaceCompassSunSceneFrame) =
        a.left < b.left+b.width && b.left < a.left+a.width && a.top < b.top+b.height && b.top < a.top+a.height

    @Test fun aLongGuideNameFitsBelowAnInclinedOrbitDespiteHourlyMarkersInsideItsOverallBox() {
        for (slope in listOf(-.43, .43, -1.4, 1.4)) {
            val a = SpaceCompassSunScenePoint(0.0, 1000.0-slope*500)
            val b = SpaceCompassSunScenePoint(1000.0, 1000.0+slope*500)
            val center = SpaceCompassSunScenePoint(500.0, 1000.0)
            val segments = listOf(SpaceCompassSunPathSegment(a, b, false))
            val orbit = spaceCompassCurveTextLayout(segments, center, 120.0, 44.0, 30.0)!!
            val markers = (0..16).map { i -> val x = i*62.5
                SpaceCompassSunSceneFrame(x-30, 1000+(x-500)*slope-30, 60.0, 60.0) }
            val occupied = markers + orbit.collisionFrames
            val state = SpaceCompassCurveCaptionPlacementState().apply { viewport(1000, 2000, 1.6875f) }
            val caption = state.place("guide", segments, listOf(center), listOf(40.5f), 20.25,
                1000.0, 2000.0, 0.0, occupied, 1.6875f) { 650.0 }!!
            assertTrue("Below the unchanged orbit name at slope $slope", caption.layout.below)
            assertEquals(center, caption.layout.anchor)
            assertTrue("The old overall box would incorrectly suppress this name",
                markers.any { overlaps(caption.layout.bounds, it) })
            assertFalse(caption.layout.collisionFrames.any { run -> occupied.any { overlaps(run, it) } })
        }
    }

    @Test fun realGlyphCollisionsStillRejectBothSidesInsteadOfDrawingThroughText() {
        val segments = listOf(SpaceCompassSunPathSegment(SpaceCompassSunScenePoint(0.0, 700.0),
            SpaceCompassSunScenePoint(1000.0, 300.0), false))
        val center = SpaceCompassSunScenePoint(500.0, 500.0)
        val above = spaceCompassCurveTextLayout(segments, center, 600.0, 40.0, 20.0)!!
        val below = spaceCompassCurveTextLayout(segments, center, 600.0, 40.0, 20.0, below = true)!!
        for (side in listOf(false, true)) {
            assertNull(spaceCompassChooseCurveTextLayout(segments, listOf(center), 600.0, 40.0, 20.0,
                1000.0, 1000.0, occupied = above.collisionFrames+below.collisionFrames,
                below = side, preciseClearance = true))
        }
        assertEquals(632.0, below.baseline.zipWithNext().sumOf { (a,b) -> hypot(b.x-a.x,b.y-a.y) }, 1e-6)
    }

    @Test fun aGuideCanClearAnOrbitRunningJustBelowItWithoutMovingTheOrbitName() {
        val a = SpaceCompassSunScenePoint(0.0, 900.0)
        val b = SpaceCompassSunScenePoint(1000.0, 400.0)
        val center = SpaceCompassSunScenePoint(500.0, 650.0)
        val segments = listOf(SpaceCompassSunPathSegment(a, b, false))
        val length = hypot(b.x-a.x, b.y-a.y)
        val dx = -(b.y-a.y)/length * 65; val dy = (b.x-a.x)/length * 65
        fun shifted(p: SpaceCompassSunScenePoint) = p.copy(x = p.x+dx, y = p.y+dy)
        val orbit = spaceCompassCurveTextLayout(listOf(SpaceCompassSunPathSegment(shifted(a), shifted(b), false)),
            shifted(center), 120.0, 44.0, 30.0)!!
        val markers = (0..16).map { i -> val x = i*62.5
            SpaceCompassSunSceneFrame(x+dx-30, 900-x*.5+dy-30, 60.0, 60.0) }
        val occupied = markers+orbit.collisionFrames
        val state = SpaceCompassCurveCaptionPlacementState().apply { viewport(1000, 2000, 1.6875f) }
        val caption = state.place("nearby", segments, listOf(center), listOf(40.5f), 20.25,
            1000.0, 2000.0, 0.0, occupied, 1.6875f) { 650.0 }!!
        assertTrue(caption.layout.below)
        assertTrue(caption.layout.additionalOffset > 0)
        assertFalse(caption.layout.collisionFrames.any { run -> occupied.any { overlaps(run, it) } })
        repeat(60) {
            val retained = state.place("nearby", segments, listOf(center), listOf(40.5f), 20.25,
                1000.0, 2000.0, 0.0, emptyList(), 1.6875f) { 650.0 }!!
            assertEquals(caption.layout.additionalOffset, retained.layout.additionalOffset, 0.0)
            assertEquals(caption.layout.baseline, retained.layout.baseline)
        }
        assertFalse(orbit.below)
    }
}
