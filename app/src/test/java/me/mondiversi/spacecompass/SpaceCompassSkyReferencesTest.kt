package me.mondiversi.spacecompass

import org.junit.Assert.*
import org.junit.Test
import java.time.Instant
import kotlin.math.*

class SpaceCompassSkyReferencesTest {
    private val j2000 = Instant.parse("2000-01-01T12:00:00Z").toEpochMilli()
    private val north = SpaceCompassSunOrientation(SpaceCompassSunVector(1.0, 0.0, 0.0),
        SpaceCompassSunVector(0.0, 0.0, 1.0), SpaceCompassSunVector(0.0, 1.0, 0.0))

    @Test fun closedCirclesKeepTheirDeclinationForEveryObserverHemisphere() {
        for (latitude in listOf(-90.0, -66.56, -45.0, 0.0, 23.44, 45.0, 90.0)) {
            val pole = spaceCompassNorthCelestialPole(latitude)
            val circles = spaceCompassSkyReferenceCircles(latitude, j2000)
            assertEquals(5, circles.size)
            circles.forEach { circle ->
                assertEquals(721, circle.directions.size)
                assertEquals(circle.directions.first(), circle.directions.last())
                circle.directions.forEach { vector ->
                    assertEquals(1.0, vector.dot(vector), 1e-12)
                    assertEquals(sin(Math.toRadians(circle.declinationDegrees)), vector.dot(pole), 1e-12)
                }
            }
        }
    }
    @Test fun poleCrossIsAtTrueAxisRatherThanPolarisOrTheCompassHorizon() {
        for (latitude in listOf(-90.0, -45.0, 0.0, 45.0, 90.0)) {
            val pole = spaceCompassNorthCelestialPole(latitude)
            val elevation = Math.toDegrees(atan2(pole.up, hypot(pole.east, pole.north)))
            assertEquals(latitude, elevation, 1e-10)
            assertEquals(0.0, pole.east, 0.0)
            assertEquals(1.0, pole.dot(pole), 1e-12)
            val south = SpaceCompassSunVector(-pole.east, -pole.north, -pole.up)
            assertEquals(-1.0, pole.dot(south), 1e-12)
        }
    }
    @Test fun equatorCrossesEastWestAndHasExpectedMeridianHeight() {
        val equator = spaceCompassSkyReferenceCircles(45.0, j2000).first()
        assertEquals(0.0, equator.directions[180].up, 1e-12)
        assertEquals(1.0, equator.directions[180].east, 1e-12)
        assertEquals(-1.0, equator.directions[540].east, 1e-12)
        val transit = equator.directions.first()
        assertEquals(45.0, Math.toDegrees(asin(transit.up)), 1e-10)
        assertTrue(transit.north < 0)
    }
    @Test fun parallelsUseObliquityOfTheSimulatedDateAndComplementaryPolarLatitudes() {
        assertEquals(23.439291111111, spaceCompassSkyReferenceObliquity(j2000), 1e-10)
        val future = Instant.parse("2100-01-01T12:00:00Z").toEpochMilli()
        assertTrue(spaceCompassSkyReferenceObliquity(future) < spaceCompassSkyReferenceObliquity(j2000))
        val circles = spaceCompassSkyReferenceCircles(45.0, future).associateBy { it.reference }
        assertEquals(90.0, circles.getValue(SpaceCompassSkyReference.CANCER).declinationDegrees +
            circles.getValue(SpaceCompassSkyReference.ARCTIC).declinationDegrees, 1e-12)
        assertEquals(-90.0, circles.getValue(SpaceCompassSkyReference.CAPRICORN).declinationDegrees +
            circles.getValue(SpaceCompassSkyReference.ANTARCTIC).declinationDegrees, 1e-12)
    }
    @Test fun northSeamIsSplitWithoutACrossImageChord() {
        val circles = spaceCompassSkyReferenceCircles(45.0, j2000)
        circles.forEach { circle ->
            val segments = spaceCompassSkyReferencePanoramaSegments(circle, 4096.0, 2048.0)
            assertTrue(segments.isNotEmpty())
            segments.forEach { segment ->
                assertTrue(abs(segment.end.x - segment.start.x) < 4096 / 20.0)
                listOf(segment.start, segment.end).forEach { point ->
                    assertTrue(point.x in 0.0..4096.0); assertTrue(point.y in 0.0..2048.0)
                }
            }
        }
    }
    @Test fun zenithAndNadirDoNotCreateArtificialHorizontalEquatorLines() {
        val equator = spaceCompassSkyReferenceCircles(0.0, j2000).first()
        val segments = spaceCompassSkyReferencePanoramaSegments(equator, 4096.0, 2048.0)
        assertEquals(720, segments.size)
        segments.forEach { assertEquals(it.start.x, it.end.x, 1e-8) }
        assertTrue(segments.any { min(it.start.y, it.end.y) < 1e-8 })
        assertTrue(segments.any { max(it.start.y, it.end.y) > 2048 - 1e-8 })
    }
    @Test fun photographsClipAllReferenceCurvesWithTheActualLensAndDisplayRoll() {
        val lens = SpaceCompassCameraLens(0.0, 0.0, 4000.0, 3000.0, 3000.0, 3000.0, 2100.0, 1400.0, 90)
        for (rotation in listOf(0, 90, 180, 270)) {
            val geometry = spaceCompassCameraPhotoGeometry(lens, 1600, 1200, rotation)
            val orientation = spaceCompassCameraScreenOrientation(north, rotation)
            val segments = spaceCompassSkyReferenceCircles(45.0, j2000).flatMap {
                projectSpaceCompassSkyReference(it, orientation, geometry.width.toDouble(), geometry.height.toDouble(), geometry.perspective)
            }
            assertTrue(segments.isNotEmpty())
            segments.forEach { s -> listOf(s.start, s.end).forEach { point ->
                assertTrue(point.x.isFinite() && point.x in 0.0..geometry.width.toDouble())
                assertTrue(point.y.isFinite() && point.y in 0.0..geometry.height.toDouble())
            } }
            val behind = projectSpaceCompassSun(SpaceCompassSunPosition(180.0, -45.0), north,
                geometry.width.toDouble(), geometry.height.toDouble(), geometry.perspective)
            assertFalse(behind.visible)
        }
    }
    @Test fun invalidObserversAndCanvasSizesCannotInventReferenceGeometry() {
        for (latitude in listOf(Double.NaN, Double.POSITIVE_INFINITY, -91.0, 91.0)) {
            try { spaceCompassSkyReferenceCircles(latitude, j2000); fail("Invalid latitude accepted") }
            catch (_: IllegalArgumentException) { }
        }
        val equator = spaceCompassSkyReferenceCircles(45.0, j2000).first()
        assertTrue(spaceCompassSkyReferencePanoramaSegments(equator, 0.0, 2048.0).isEmpty())
        assertTrue(spaceCompassSkyReferencePanoramaSegments(equator, Double.NaN, 2048.0).isEmpty())
    }
    @Test fun virtualLiveGuidesShareTheBodyPerspectiveWithoutCameraCalibration() {
        val equator = spaceCompassSkyReferenceCircles(75.0, j2000, samplesPerCircle = 180).first()
        val segments = projectSpaceCompassSkyReference(equator, north, 1280.0, 720.0)
        assertTrue(segments.isNotEmpty())
        val body = projectSpaceCompassSun(SpaceCompassSunPosition(0.0, -15.0), north, 1280.0, 720.0)
        assertTrue(body.visible)
        val endpoints = segments.flatMap { listOf(it.start, it.end) }
        assertTrue(endpoints.any { hypot(it.x - body.x, it.y - body.y) < 1e-8 })
        assertEquals(640.0, body.x, 1e-8)
        assertEquals(360.0 + spaceCompassSunProjectionFocalLength(720.0) * tan(Math.toRadians(15.0)), body.y, 1e-8)
    }
    @Test fun cachedLiveSamplesStayOnTheSameClosedCirclesAsTheExport() {
        for (latitude in listOf(-90.0, -45.0, 0.0, 45.0, 90.0)) {
            val export = spaceCompassSkyReferenceCircles(latitude, j2000)
            val live = spaceCompassSkyReferenceCircles(latitude, j2000, samplesPerCircle = 180)
            export.zip(live).forEach { (dense, sparse) ->
                assertEquals(dense.declinationDegrees, sparse.declinationDegrees, 0.0)
                assertEquals(181, sparse.directions.size)
                assertEquals(sparse.directions.first(), sparse.directions.last())
                sparse.directions.forEachIndexed { index, vector ->
                    val reference = dense.directions[index * 4]
                    assertEquals(reference.east, vector.east, 1e-12)
                    assertEquals(reference.north, vector.north, 1e-12)
                    assertEquals(reference.up, vector.up, 1e-12)
                }
            }
        }
    }
}
