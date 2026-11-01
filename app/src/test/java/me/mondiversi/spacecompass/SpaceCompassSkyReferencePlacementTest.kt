package me.mondiversi.spacecompass

import java.time.Instant
import kotlin.math.*
import org.junit.Assert.*
import org.junit.Test

class SpaceCompassSkyReferencePlacementTest {
    private fun line(y: Double = 150.0) = listOf(SpaceCompassSunPathSegment(
        SpaceCompassSunScenePoint(0.0, y), SpaceCompassSunScenePoint(600.0, y), false))
    private val candidates = listOf(SpaceCompassSunScenePoint(150.0, 150.0), SpaceCompassSunScenePoint(450.0, 150.0))
    private fun caption(state: SpaceCompassSkyReferencePlacementState, y: Double = 150.0,
        points: List<SpaceCompassSunScenePoint> = candidates, fixed: List<SpaceCompassSunSceneFrame> = emptyList(),
        occupied: List<SpaceCompassSunSceneFrame> = fixed) = state.caption("CANCER", line(y), points,
            1f, 600.0, 400.0, 0.0, fixed, occupied) { 100.0 }

    @Test fun crossNamesMoveContinuouslyAtBothEdgesWithoutChangingSides() {
        for (y in listOf(1.0, 100.0, 399.0)) {
            val frames = (0..120).map { i -> spaceCompassSkyGuideCaptionBounds(
                SpaceCompassSunScenePoint(300.0, y + sin(i.toDouble()) * .2),
                180.0, 34.0, 600.0, 400.0, 48.0, 8.0, 12.0)!! }
            assertTrue(frames.maxOf { it.top } - frames.minOf { it.top } < .41)
            assertTrue(frames.all { it.top >= 56 && it.top + it.height <= 392 })
        }
    }

    @Test fun aGuideUsesAnotherFreeAboveRunBeforeGoingBelowTheFirstCandidate() {
        val state = SpaceCompassSkyReferencePlacementState().apply { viewport(600, 400, 1f) }
        assertEquals(150.0, caption(state)!!.layout.anchor.x, 0.0)
        for (frame in 0..120) {
            val jitter = sin(frame.toDouble()) * .2
            val points = candidates.map { it.copy(y = 150 + jitter) }.let { if (frame % 2 == 0) it.reversed() else it }
            val moving = listOf(SpaceCompassSunSceneFrame(80.0 + frame % 3, 60.0, 150.0, 90.0))
            val current = caption(state, 150 + jitter, points, occupied = moving)!!
            assertEquals(450.0, current.layout.anchor.x, 1e-8)
            assertEquals(150 + jitter, current.layout.anchor.y, 1e-8)
            assertEquals(24f, current.textSize)
            assertFalse(current.layout.below)
        }
    }

    @Test fun aGuideReturnsAboveItsOwnLineInsteadOfStayingBelowANearbyOrbit() {
        for (guide in SpaceCompassSkyReference.entries) {
            val state = SpaceCompassSkyReferencePlacementState().apply { viewport(600, 400, 1f) }
            val center = SpaceCompassSunScenePoint(300.0, 150.0)
            val orbit = spaceCompassCurveTextLayout(line(210.0), center.copy(y = 210.0), 100.0, 24.0, 12.0)!!
            val markers = (0..12).map { index -> SpaceCompassSunSceneFrame(index * 50.0 - 10, 200.0, 20.0, 20.0) }
            val occupied = orbit.collisionFrames + markers
            fun place(y: Double = 150.0, blocked: Boolean = false) = state.caption(guide.name, line(y),
                listOf(center.copy(y = y)), 1f, 600.0, 400.0, 0.0, emptyList(),
                occupied + if (blocked) listOf(SpaceCompassSunSceneFrame(0.0, 0.0, 600.0, 151.0)) else emptyList()) { 260.0 }!!
            val initial = place(blocked = true)
            assertTrue(initial.layout.below)
            assertTrue(initial.layout.additionalOffset > 0)
            repeat(60) { frame ->
                val y = 150.0 + sin(frame.toDouble()) * .2
                val current = place(y)
                assertFalse("The guide belongs above its own line, not below the orbit: $guide", current.layout.below)
                assertEquals(0.0, current.layout.additionalOffset, 0.0)
                assertEquals(300.0, current.layout.anchor.x, 1e-8)
                assertEquals(y - 12, current.layout.baseline.first().y, 1e-8)
            }
            assertFalse(orbit.below)
            assertEquals(198.0, orbit.baseline.first().y, 1e-8)
        }
    }

    @Test fun recoveringTheAboveSideNeedsMoreClearanceThanSmallHandJitter() {
        val state = SpaceCompassSkyReferencePlacementState().apply { viewport(600, 400, 1f) }
        fun obstacle(y: Double) = listOf(SpaceCompassSunSceneFrame(0.0, y, 600.0, 2.0))
        assertTrue(caption(state, occupied = obstacle(149.9))!!.layout.below)
        repeat(120) { frame ->
            assertTrue(caption(state, occupied = obstacle(149.9 + sin(frame.toDouble()) * .2))!!.layout.below)
        }
        assertFalse(caption(state, occupied = obstacle(160.0))!!.layout.below)
        repeat(120) { frame ->
            assertFalse(caption(state, occupied = obstacle(160.0 + sin(frame.toDouble()) * .2))!!.layout.below)
        }
    }

    @Test fun aFixedControlMayRelocateANameOnceAndFreedSpaceDoesNotPullItBack() {
        val state = SpaceCompassSkyReferencePlacementState().apply { viewport(600, 400, 1f) }
        assertEquals(150.0, caption(state)!!.layout.anchor.x, 0.0)
        val obstacle = listOf(SpaceCompassSunSceneFrame(60.0, 70.0, 210.0, 330.0))
        assertEquals(450.0, caption(state, fixed = obstacle)!!.layout.anchor.x, 0.0)
        repeat(20) { assertEquals(450.0, caption(state)!!.layout.anchor.x, 0.0) }
    }

    @Test fun retainedNamesFollowTheCurveAndDoNotStickToOldScreenPixels() {
        val state = SpaceCompassSkyReferencePlacementState().apply { viewport(600, 400, 1f) }
        caption(state)
        for (i in 1..80) {
            val y = 150.0 + i * .5
            val current = caption(state, y)!!
            assertEquals(y, current.layout.anchor.y, 1e-8)
            assertTrue(current.layout.baseline.all { abs(it.y - (y - 12)) < 1e-8 })
        }
    }

    @Test fun viewportChangesResetOldPlacementAndClippedFragmentsNeverBecomeFalseBaselines() {
        val state = SpaceCompassSkyReferencePlacementState().apply { viewport(600, 400, 1f) }
        caption(state, fixed = listOf(SpaceCompassSunSceneFrame(60.0, 70.0, 210.0, 330.0)))
        state.viewport(400, 600, 1f)
        assertEquals(150.0, caption(state)!!.layout.anchor.x, 0.0)
        val fragments = listOf(SpaceCompassSunPathSegment(SpaceCompassSunScenePoint(0.0, 150.0),
            SpaceCompassSunScenePoint(30.0, 150.0), false), SpaceCompassSunPathSegment(
            SpaceCompassSunScenePoint(570.0, 150.0), SpaceCompassSunScenePoint(600.0, 150.0), false))
        assertNull(state.caption("CANCER", fragments, candidates, 1f, 600.0, 400.0,
            0.0, emptyList(), emptyList()) { 100.0 })
    }

    @Test fun repeatedDegreesRetainTheirAnchorsWhenSamplingOrderChanges() {
        val state = SpaceCompassSkyReferencePlacementState().apply { viewport(600, 400, 1f) }
        val points = listOf(100.0, 200.0, 300.0).map { SpaceCompassSunScenePoint(it, 150.0) }
        val first = state.degreeAnchors(SpaceCompassSkyReference.CANCER, line(), points, 100.0,
            emptyList(), 600.0, 400.0, 0.0, 20.0)
        val next = state.degreeAnchors(SpaceCompassSkyReference.CANCER, line(150.2), points.reversed(), 100.0,
            emptyList(), 600.0, 400.0, 0.0, 20.0)
        assertEquals(first.map { it.x }, next.map { it.x })
        assertTrue(next.all { abs(it.y - 150.2) < 1e-8 })
    }

    @Test fun capricornCaptionDoesNotJumpAcrossRealProjectedCurveDuringSmallHandRoll() {
        val time = Instant.parse("2026-10-17T12:00:00Z").toEpochMilli()
        val circle = spaceCompassSkyReferenceCircles(45.0, time, 180)
            .single { it.reference == SpaceCompassSkyReference.CAPRICORN }
        val state = SpaceCompassSkyReferencePlacementState().apply { viewport(1200, 900, 1f) }
        var last: SpaceCompassSunScenePoint? = null
        for (i in 0..120) {
            val roll = Math.toRadians(sin(i.toDouble()) * .02)
            val orientation = SpaceCompassSunOrientation(SpaceCompassSunVector(-cos(roll), 0.0, sin(roll)),
                SpaceCompassSunVector(sin(roll), 0.0, cos(roll)), SpaceCompassSunVector(0.0, -1.0, 0.0))
            val segments = projectSpaceCompassSkyReference(circle, orientation, 1200.0, 900.0)
            val points = segments.map { SpaceCompassSunScenePoint((it.start.x + it.end.x) / 2, (it.start.y + it.end.y) / 2) }
                .sortedBy { abs(it.x - 600.0) }
            val current = state.caption("CAPRICORN", segments, points, 1f, 1200.0, 900.0,
                0.0, emptyList(), emptyList()) { 160.0 }!!.layout.anchor
            last?.let { assertTrue(hypot(it.x - current.x, it.y - current.y) < 2.0) }
            last = current
        }
    }
}
