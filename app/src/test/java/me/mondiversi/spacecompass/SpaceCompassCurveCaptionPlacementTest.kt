package me.mondiversi.spacecompass

import org.junit.Assert.*
import org.junit.Test
import kotlin.math.*

class SpaceCompassCurveCaptionPlacementTest {
    private val candidates = listOf(SpaceCompassSunScenePoint(150.0, 150.0), SpaceCompassSunScenePoint(450.0, 150.0))
    private fun line(y: Double = 150.0) = listOf(SpaceCompassSunPathSegment(
        SpaceCompassSunScenePoint(0.0, y), SpaceCompassSunScenePoint(600.0, y), false))
    private fun state() = SpaceCompassCurveCaptionPlacementState().apply { viewport(600, 400, 1f) }
    private fun place(state: SpaceCompassCurveCaptionPlacementState, key: String = "name", y: Double = 150.0,
        points: List<SpaceCompassSunScenePoint> = candidates, occupied: List<SpaceCompassSunSceneFrame> = emptyList(),
        follow: Boolean = true) = state.place(key, line(y), points, listOf(24f), 12.0,
        600.0, 400.0, 0.0, occupied, 1f, followPreviousAnchor = follow) { 100.0 }
    private fun overlaps(a: SpaceCompassSunSceneFrame, b: SpaceCompassSunSceneFrame) =
        a.left < b.left+b.width && b.left < a.left+a.width && a.top < b.top+b.height && b.top < a.top+a.height

    @Test fun everyGuideCanUseTheOtherSideOfAnOrbitNameAtTheSameAnchor() {
        for (guide in SpaceCompassSkyReference.entries) {
            val orbitName = place(state(), key = "orbit")!!
            val guideName = place(state(), guide.name, occupied = listOf(orbitName.layout.bounds))!!
            assertFalse(orbitName.layout.below)
            assertTrue(guideName.layout.below)
            assertEquals(orbitName.layout.anchor, guideName.layout.anchor)
            assertFalse(overlaps(orbitName.layout.bounds, guideName.layout.bounds))
        }
    }

    @Test fun anotherOrbitNameOnANearbyTrajectoryUsesFreeSideAndKeepsItWhileClear() {
        val first = state(); val second = state()
        for (frame in 0..120) {
            val drift = sin(frame.toDouble()) * .2
            val a = place(first, "orbit-a", 150+drift, follow = false)!!
            val b = place(second, "orbit-b", 153+drift, occupied = listOf(a.layout.bounds), follow = false)!!
            assertFalse(a.layout.below)
            assertTrue(b.layout.below)
            assertFalse(overlaps(a.layout.bounds, b.layout.bounds))
        }
        assertTrue(place(second, "orbit-b", 153.0, follow = false)!!.layout.below)
    }

    @Test fun ifBothSidesAreBlockedANameMovesAlongItsActualCurveAndStaysThere() {
        val s = state()
        val blocked = SpaceCompassSunSceneFrame(60.0, 70.0, 210.0, 210.0)
        val moved = place(s, occupied = listOf(blocked))!!
        assertEquals(450.0, moved.layout.anchor.x, 0.0)
        assertFalse(overlaps(blocked, moved.layout.bounds))
        repeat(30) { assertEquals(moved.layout.anchor, place(s)!!.layout.anchor) }
    }

    @Test fun anOrbitEventNameFollowsItsArrowRatherThanAnOldScreenCoordinate() {
        val s = state()
        place(s, points = listOf(candidates.first()), follow = false)
        val next = place(s, points = listOf(SpaceCompassSunScenePoint(170.0, 160.0)), y = 160.0, follow = false)!!
        assertEquals(SpaceCompassSunScenePoint(170.0, 160.0), next.layout.anchor)
        assertTrue(next.layout.baseline.all { abs(it.y - 148) < 1e-8 })
    }

    @Test fun clippedFragmentsAndCompletelyOccupiedViewportsCannotForceOverlappingText() {
        val s = state()
        assertNull(place(s, occupied = listOf(SpaceCompassSunSceneFrame(0.0, 0.0, 600.0, 400.0))))
        val fragments = listOf(SpaceCompassSunPathSegment(SpaceCompassSunScenePoint(0.0, 150.0),
            SpaceCompassSunScenePoint(30.0, 150.0), false), SpaceCompassSunPathSegment(
            SpaceCompassSunScenePoint(570.0, 150.0), SpaceCompassSunScenePoint(600.0, 150.0), false))
        assertNull(s.place("cut", fragments, candidates, listOf(24f), 12.0,
            600.0, 400.0, 0.0, emptyList(), 1f) { 100.0 })
    }

    @Test fun belowTextKeepsItsGlyphSpanAndReadingDirectionOnRotatedAndReversedCurves() {
        val trajectories = listOf(
            SpaceCompassSunScenePoint(600.0, 150.0) to SpaceCompassSunScenePoint(0.0, 150.0),
            SpaceCompassSunScenePoint(300.0, 0.0) to SpaceCompassSunScenePoint(300.0, 600.0),
            SpaceCompassSunScenePoint(0.0, 0.0) to SpaceCompassSunScenePoint(600.0, 400.0))
        for ((a, b) in trajectories) {
            val center = SpaceCompassSunScenePoint((a.x+b.x)/2, (a.y+b.y)/2)
            val layout = spaceCompassCurveTextLayout(listOf(SpaceCompassSunPathSegment(a, b, false)),
                center, 100.0, 20.0, 12.0, below = true)!!
            val start = layout.baseline.first(); val end = layout.baseline.last()
            assertTrue(end.x > start.x || abs(end.x-start.x) < 1e-8 && end.y < start.y)
            assertEquals(116.0, layout.baseline.zipWithNext().sumOf { (p, q) -> hypot(q.x-p.x, q.y-p.y) }, 1e-6)
            assertTrue(layout.below)
        }
        val below = spaceCompassCurveTextLayout(line(), candidates.first(), 100.0, 20.0, 12.0, below = true)!!
        assertTrue(below.bounds.top > 150.0 + 12.0)
        assertNull(spaceCompassCurveTextLayout(line(), candidates.first(), 100.0, 20.0, -12.0, below = true))
    }
}
