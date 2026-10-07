package me.mondiversi.spacecompass

import org.junit.Assert.*
import org.junit.Test
import java.time.Instant
import kotlin.math.*

class SpaceCompassCurveTextLayoutTest {
    private fun straight(y: Double = 100.0) = listOf(SpaceCompassSunPathSegment(
        SpaceCompassSunScenePoint(0.0, y), SpaceCompassSunScenePoint(600.0, y), false))
    @Test fun blockedNamesMoveToAnotherActualAnchorAndNeverCrossTheCaptionOrViewport() {
        val candidates = listOf(SpaceCompassSunScenePoint(150.0,100.0), SpaceCompassSunScenePoint(450.0,100.0))
        val blocked = SpaceCompassSunSceneFrame(60.0,40.0,200.0,120.0)
        val layout = spaceCompassChooseCurveTextLayout(straight(), candidates,100.0,20.0,12.0,600.0,300.0,
            occupied = listOf(blocked))!!
        assertTrue(layout.bounds.left > blocked.left+blocked.width)
        assertTrue(layout.baseline.all { it.y == 88.0 })
        assertNull(spaceCompassChooseCurveTextLayout(straight(), candidates,100.0,20.0,12.0,600.0,300.0, minimumY = 80.0))
        assertNull(spaceCompassChooseCurveTextLayout(straight(), candidates,100.0,20.0,12.0,600.0,300.0,
            occupied = listOf(SpaceCompassSunSceneFrame(0.0,0.0,600.0,300.0))))
    }
    @Test fun labelsRemainUprightOnReversedAndVerticalProjectedTrajectories() {
        for ((a,b) in listOf(SpaceCompassSunScenePoint(600.0,100.0) to SpaceCompassSunScenePoint(0.0,100.0),
            SpaceCompassSunScenePoint(300.0,0.0) to SpaceCompassSunScenePoint(300.0,600.0))) {
            val center = SpaceCompassSunScenePoint((a.x+b.x)/2,(a.y+b.y)/2)
            val layout = spaceCompassCurveTextLayout(listOf(SpaceCompassSunPathSegment(a,b,false)), center,100.0,20.0,12.0)!!
            val first = layout.baseline.first(); val last = layout.baseline.last()
            assertTrue(last.x > first.x || abs(last.x-first.x) < 1e-8 && last.y < first.y)
        }
    }
    @Test fun curvedNamesKeepTheMeasuredGlyphSpanInsteadOfCompressingItsEnds() {
        val points = (0..180).map { val a = Math.toRadians(135.0-it/2.0)
            SpaceCompassSunScenePoint(300+140*cos(a),150+140*sin(a)) }
        val segments = points.zipWithNext().map { (a,b) -> SpaceCompassSunPathSegment(a,b,false) }
        val layout = spaceCompassCurveTextLayout(segments,SpaceCompassSunScenePoint(300.0,290.0),100.0,20.0,14.0)!!
        assertEquals(116.0, layout.baseline.zipWithNext().sumOf { (a,b) -> hypot(b.x-a.x,b.y-a.y) },1e-5)
        assertTrue(layout.baseline.all { abs(hypot(it.x-300,it.y-150)-126) < .02 })
    }
    @Test fun clippedAndWrappedFragmentsCannotInventAConnectingTextBaseline() {
        val fragments = listOf(SpaceCompassSunPathSegment(SpaceCompassSunScenePoint(0.0,100.0),SpaceCompassSunScenePoint(40.0,100.0),false),
            SpaceCompassSunPathSegment(SpaceCompassSunScenePoint(560.0,100.0),SpaceCompassSunScenePoint(600.0,100.0),false))
        assertNull(spaceCompassChooseCurveTextLayout(fragments,listOf(SpaceCompassSunScenePoint(20.0,100.0),
            SpaceCompassSunScenePoint(580.0,100.0)),100.0,20.0,12.0,600.0,300.0))
        assertNull(spaceCompassChooseCurveTextLayout(straight(),listOf(SpaceCompassSunScenePoint(150.0,100.0)),
            Double.NaN,20.0,12.0,600.0,300.0))
    }
    @Test fun allReferenceCirclesUseTheirRealRotatedPanoramaCurve() {
        val time = Instant.parse("2026-10-07T12:00:00Z").toEpochMilli()
        for (latitude in listOf(-90.0,-45.0,0.0,45.0,90.0)) for (center in SpaceCompassPanoramaCenter.entries) {
            for (circle in spaceCompassSkyReferenceCircles(latitude,time)) {
                val segments = spaceCompassSkyReferencePanoramaSegments(circle,4096.0,2048.0,center.azimuth)
                val candidates = segments.map { SpaceCompassSunScenePoint((it.start.x+it.end.x)/2,(it.start.y+it.end.y)/2) }
                val layout = spaceCompassChooseCurveTextLayout(segments,candidates,140.0,24.0,12.0,4096.0,2048.0)!!
                assertTrue(layout.bounds.left >= 0 && layout.bounds.top >= 0)
                assertTrue(layout.bounds.left+layout.bounds.width <= 4096 && layout.bounds.top+layout.bounds.height <= 2048)
                assertTrue(layout.baseline.zipWithNext().all { (a,b) -> abs(a.x-b.x) < 4096/10.0 })
            }
        }
    }
    @Test fun cameraFragmentsUseActualLensRollAndLeaveHeaderAndReservedAxesClear() {
        val time = Instant.parse("2026-10-07T12:00:00Z").toEpochMilli()
        val north = SpaceCompassSunOrientation(SpaceCompassSunVector(1.0,0.0,0.0),
            SpaceCompassSunVector(0.0,0.0,1.0),SpaceCompassSunVector(0.0,1.0,0.0))
        val lens = SpaceCompassPerspective(.8,.8,.45,.55)
        val blocked = listOf(SpaceCompassSunSceneFrame(0.0,0.0,100.0,1080.0),
            SpaceCompassSunSceneFrame(0.0,1000.0,1920.0,80.0))
        for (rotation in listOf(0,90,180,270)) {
            val orientation = spaceCompassCameraScreenOrientation(north,rotation)
            val layouts = spaceCompassSkyReferenceCircles(45.0,time).mapNotNull { circle ->
                val segments = projectSpaceCompassSkyReference(circle,orientation,1920.0,1080.0,lens)
                val candidates = segments.map { SpaceCompassSunScenePoint((it.start.x+it.end.x)/2,(it.start.y+it.end.y)/2) }
                spaceCompassChooseCurveTextLayout(segments,candidates,140.0,24.0,12.0,1920.0,1080.0,minimumY=180.0,occupied=blocked)
            }
            assertTrue("Visible label for rotation $rotation", layouts.isNotEmpty())
            layouts.forEach { layout -> assertTrue(layout.bounds.left >= 100 && layout.bounds.top >= 180)
                assertTrue(layout.bounds.left+layout.bounds.width <= 1920 && layout.bounds.top+layout.bounds.height <= 1000) }
        }
    }
}
