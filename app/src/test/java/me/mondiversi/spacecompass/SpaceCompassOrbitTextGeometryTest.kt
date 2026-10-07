package me.mondiversi.spacecompass

import org.junit.Assert.*
import org.junit.Test
import kotlin.math.*

class SpaceCompassOrbitTextGeometryTest {
    private fun arc(radius: Double, reversed: Boolean = false): List<SpaceCompassSunPathSegment> {
        val points = (0..180).map {
            val angle = Math.toRadians(135.0 - it / 2.0)
            SpaceCompassSunScenePoint(300 + radius * cos(angle), 150 + radius * sin(angle))
        }.let { if (reversed) it.reversed() else it }
        return points.zipWithNext().map { (a, b) -> SpaceCompassSunPathSegment(a, b, false) }
    }

    @Test fun inwardOffsetHasItsOwnMeasuredLengthWithoutCompressingTheName() {
        for (reversed in listOf(false, true)) {
            val baseline = spaceCompassOrbitTextBaseline(arc(140.0, reversed),
                SpaceCompassSunScenePoint(300.0, 290.0), 100.0, 20.0, 14.0)!!
            assertEquals(116.0, baseline.zipWithNext().sumOf { (a, b) -> hypot(b.x - a.x, b.y - a.y) }, 1e-5)
            assertTrue(baseline.first().x < baseline.last().x)
            assertTrue(baseline.all { abs(hypot(it.x - 300, it.y - 150) - 126) < .02 })
        }
    }

    @Test fun tightBendsThatWouldConvergeGlyphsAreNotLabelled() {
        assertNull(spaceCompassOrbitTextBaseline(arc(40.0), SpaceCompassSunScenePoint(300.0, 190.0), 25.0, 20.0, 12.0))
        assertNotNull(spaceCompassOrbitTextBaseline(arc(140.0), SpaceCompassSunScenePoint(300.0, 290.0), 25.0, 20.0, 12.0))
    }

    @Test fun straightLabelKeepsItsWidthAndOffsetAndCannotBridgeAClippedRun() {
        val segments = listOf(SpaceCompassSunPathSegment(SpaceCompassSunScenePoint(0.0, 100.0),
            SpaceCompassSunScenePoint(300.0, 100.0), false))
        val baseline = spaceCompassOrbitTextBaseline(segments, SpaceCompassSunScenePoint(150.0, 100.0), 100.0, 20.0, 12.0)!!
        assertEquals(116.0, baseline.last().x - baseline.first().x, 1e-5)
        assertTrue(baseline.all { it.y == 88.0 })
        assertNull(spaceCompassOrbitTextBaseline(segments, SpaceCompassSunScenePoint(5.0, 100.0), 100.0, 20.0, 12.0))
        assertNull(spaceCompassOrbitTextBaseline(segments, SpaceCompassSunScenePoint(150.0, 100.0), Double.NaN, 20.0, 12.0))
    }
}
