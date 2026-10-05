package me.mondiversi.spacecompass

import org.junit.Assert.*
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneId
import kotlin.math.hypot

class SpaceCompassPanoramaGeometryTest {
    private fun path(vararg positions: SpaceCompassSunPosition) = SpaceCompassSunDailyPath(LocalDate.parse("2026-10-05"),
        ZoneId.of("Europe/Rome"), positions.mapIndexed { i, p -> SpaceCompassSunPathPoint(i * 1000L, p) }, emptyList())
    @Test fun cardinalHorizonZenithAndNadirHaveTheirTruePanoramaCoordinates() {
        for ((az, x) in listOf(0.0 to 0.0, 90.0 to 100.0, 180.0 to 200.0, 270.0 to 300.0, 360.0 to 0.0, -90.0 to 300.0)) {
            assertEquals(SpaceCompassSunScenePoint(x, 100.0), spaceCompassPanoramaPoint(SpaceCompassSunPosition(az, 0.0), 400.0, 200.0))
        }
        assertEquals(0.0, spaceCompassPanoramaPoint(SpaceCompassSunPosition(180.0, 90.0), 400.0, 200.0)!!.y, 0.0)
        assertEquals(200.0, spaceCompassPanoramaPoint(SpaceCompassSunPosition(180.0, -90.0), 400.0, 200.0)!!.y, 0.0)
    }
    @Test fun northSeamSplitsInBothTimeDirectionsWithoutAFalseAcrossImageChord() {
        for ((a,b) in listOf(359.0 to 1.0, 1.0 to 359.0)) {
            val segments = spaceCompassPanoramaSegments(path(SpaceCompassSunPosition(a, 20.0), SpaceCompassSunPosition(b, 20.0)), 360.0, 180.0)
            assertEquals(2, segments.size)
            assertTrue(segments.all { kotlin.math.abs(it.end.x - it.start.x) <= 1.00001 })
            assertEquals(2.0, segments.sumOf { kotlin.math.abs(it.end.x - it.start.x) }, 1e-6)
            assertTrue(segments.all { !it.belowHorizon })
        }
    }
    @Test fun horizonAndSeamCanSplitTheSameSampleWithoutChangingSolidDashedSide() {
        val segments = spaceCompassPanoramaSegments(path(SpaceCompassSunPosition(359.0, 1.0), SpaceCompassSunPosition(1.0, -3.0)), 360.0, 180.0)
        assertEquals(3, segments.size)
        assertTrue(segments.first().end.y == 90.0)
        assertFalse(segments.first().belowHorizon)
        assertTrue(segments.drop(1).all { it.belowHorizon })
        assertTrue(segments.all { it.start.x in 0.0..360.0 && it.end.x in 0.0..360.0 })
    }
    @Test fun invalidProjectionCannotProduceCoordinatesOrSegments() {
        assertNull(spaceCompassPanoramaPoint(SpaceCompassSunPosition(Double.NaN, 20.0),360.0,180.0))
        assertNull(spaceCompassPanoramaPoint(SpaceCompassSunPosition(20.0, 91.0),360.0,180.0))
        assertNull(spaceCompassPanoramaPoint(SpaceCompassSunPosition(20.0, 20.0),0.0,180.0))
        assertTrue(spaceCompassPanoramaSegments(path(SpaceCompassSunPosition(20.0, 20.0),SpaceCompassSunPosition(30.0, 30.0)),Double.NaN,180.0).isEmpty())
    }
    @Test fun labelsFollowTheCurveAndKeepTheMeasuredTextSpan() {
        val points = (0..30).map { SpaceCompassSunScenePoint(it * 10.0, 100 + it * it * .1) }
        val segments = points.zipWithNext().map { (a,b) -> SpaceCompassSunPathSegment(a,b,false) }
        val label = spaceCompassOrbitLabelBaseline(segments,points[15],100.0)!!
        assertTrue(label.size > 3)
        assertEquals(100.0,label.zipWithNext().sumOf { (a,b) -> hypot(a.x-b.x,a.y-b.y) },1e-5)
        assertTrue(label.first().x < label.last().x)
        assertTrue(label.zipWithNext().map { (a,b) -> (b.y-a.y)/(b.x-a.x) }.distinct().size > 2)
    }
    @Test fun reversingTimeDoesNotTurnLabelsUpsideDown() {
        val segments = listOf(SpaceCompassSunPathSegment(SpaceCompassSunScenePoint(300.0,100.0),SpaceCompassSunScenePoint(0.0,100.0),false))
        val label = spaceCompassOrbitLabelBaseline(segments,SpaceCompassSunScenePoint(150.0,100.0),100.0)!!
        assertEquals(100.0,label.first().x,1e-5); assertEquals(200.0,label.last().x,1e-5)
    }
    @Test fun labelsNeverBridgeClippedRunsOrASeamAndRejectInsufficientSpace() {
        val segments = listOf(SpaceCompassSunPathSegment(SpaceCompassSunScenePoint(0.0,100.0),SpaceCompassSunScenePoint(30.0,100.0),false),
            SpaceCompassSunPathSegment(SpaceCompassSunScenePoint(330.0,100.0),SpaceCompassSunScenePoint(360.0,100.0),false))
        assertNull(spaceCompassOrbitLabelBaseline(segments,SpaceCompassSunScenePoint(15.0,100.0),60.0))
        assertNull(spaceCompassOrbitLabelBaseline(segments,SpaceCompassSunScenePoint(150.0,100.0),10.0))
        assertNull(spaceCompassOrbitLabelBaseline(segments,SpaceCompassSunScenePoint(15.0,100.0),Double.NaN))
    }
    @Test fun polarAndDstDailyPathsRemainBoundedAndKeepAllSamplesUnmodified() {
        for (day in listOf("2026-03-29","2026-10-25")) for (latitude in listOf(0.0,45.0,89.9,-89.9)) {
            val daily = calculateSpaceCompassSunDailyPath(LocalDate.parse(day),ZoneId.of("Europe/Rome"),latitude,12.5)
            val samples = daily.samples.toList()
            val segments = spaceCompassPanoramaSegments(daily,4096.0,2048.0)
            assertTrue(segments.isNotEmpty())
            assertTrue(segments.all { it.start.x in 0.0..4096.0 && it.end.x in 0.0..4096.0 && it.start.y in 0.0..2048.0 && it.end.y in 0.0..2048.0 })
            assertEquals(samples,daily.samples)
        }
    }
}
