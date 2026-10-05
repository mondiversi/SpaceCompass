package me.mondiversi.spacecompass

import java.io.File
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import kotlin.math.*
import org.junit.Assert.*
import org.junit.Test

class SpaceCompassSunPathDirectionTest {
    private val date = LocalDate.parse("2026-10-04")
    private val zone = ZoneId.of("Europe/Rome")
    private fun synthetic(elevation: Double = 10.0): SpaceCompassSunDailyPath {
        fun point(t: Long) = SpaceCompassSunPathPoint(t, SpaceCompassSunPosition((t / 1000.0 - 12) * 2, elevation))
        return SpaceCompassSunDailyPath(date, zone, (0..100).map { point(it * 240L) },
            (0..23).map { point(it * 1000L) })
    }
    private fun facing(azimuth: Double, elevation: Double = 0.0, roll: Double = 0.0): SpaceCompassSunOrientation {
        val a = Math.toRadians(azimuth); val e = Math.toRadians(elevation); val r = Math.toRadians(roll)
        val right = SpaceCompassSunVector(cos(a), -sin(a), 0.0)
        val up = SpaceCompassSunVector(-sin(e) * sin(a), -sin(e) * cos(a), cos(e))
        fun mix(x: SpaceCompassSunVector, y: SpaceCompassSunVector, c: Double, s: Double) =
            SpaceCompassSunVector(x.east * c + y.east * s, x.north * c + y.north * s, x.up * c + y.up * s)
        return SpaceCompassSunOrientation(mix(right, up, cos(r), sin(r)), mix(up, right, cos(r), -sin(r)),
            SpaceCompassSunVector(cos(e) * sin(a), cos(e) * cos(a), sin(e)))
    }

    @Test fun everyFourthRegularDotGetsOneArrowBetweenDotsWithoutChangingThePath() {
        val path = synthetic()
        val originalSamples = path.samples.toList(); val originalMarkers = path.markers.toList()
        val arrows = spaceCompassSunPathDirections(path)
        assertEquals(listOf(3500L, 7500L, 11500L, 15500L, 19500L, 23500L), arrows.map { it.timeMs })
        assertTrue(arrows.all { arrow -> path.markers.none { it.timeMs == arrow.timeMs } })
        assertEquals(originalSamples, path.samples); assertEquals(originalMarkers, path.markers)
        assertTrue(arrows.zipWithNext().all { (a, b) -> a.timeMs < b.timeMs })
        assertTrue(arrows.all { it.timeMs < path.samples.last().timeMs })
    }

    @Test fun namedEventsAndTheirOrderDoNotMoveTheArrowsOrConsumeTheFourPointCadence() {
        val path = synthetic()
        val events = SpaceCompassSunPathEvent.entries.filter { it != SpaceCompassSunPathEvent.HOUR }.mapIndexed { index, event ->
            SpaceCompassSunPathPoint(3100L + index * 100L, SpaceCompassSunPosition(-17.0, 10.0), event)
        }
        assertEquals(spaceCompassSunPathDirections(path),
            spaceCompassSunPathDirections(path.copy(markers = (path.markers + events).reversed())))
    }

    @Test fun denseSampleInterpolationStaysOnTheExistingProjectedCurveAndFollowsTimeThroughRoll() {
        val path = synthetic()
        val direction = spaceCompassSunPathDirections(path)[2]
        for ((width, height) in listOf(400.0 to 600.0, 900.0 to 300.0, 1200.0 to 1600.0)) {
            for (roll in listOf(0.0, 90.0, 180.0, 270.0)) {
                val orientation = facing(-1.0, 10.0, roll)
                val arrow = projectSpaceCompassSunPathArrow(direction, orientation, width, height, 5.0)!!
                val segment = projectSpaceCompassSunPathSegment(direction.start, direction.end, orientation, width, height, false)!!
                val dx = segment.end.x - segment.start.x; val dy = segment.end.y - segment.start.y
                val cross = (arrow.center.x - segment.start.x) * dy - (arrow.center.y - segment.start.y) * dx
                assertEquals(0.0, cross, 1e-7)
                assertTrue((arrow.tip.x - arrow.center.x) * dx + (arrow.tip.y - arrow.center.y) * dy > 0)
                assertEquals(5.0, hypot(arrow.tip.x - arrow.center.x, arrow.tip.y - arrow.center.y), 1e-7)
                assertFalse(arrow.belowHorizon)
            }
        }
        // Opposite apparent motion must reverse the chevron, not retain a fixed screen-right arrow.
        val reversed = direction.copy(start = direction.end, end = direction.start)
        val orientation = facing(-1.0, 10.0)
        val forward = projectSpaceCompassSunPathArrow(direction, orientation, 400.0, 600.0, 5.0)!!
        val backward = projectSpaceCompassSunPathArrow(reversed, orientation, 400.0, 600.0, 5.0)!!
        assertEquals(forward.center, backward.center)
        assertTrue(forward.tip.x > forward.center.x)
        assertTrue(backward.tip.x < backward.center.x)
    }

    @Test fun arrowsWorkBelowTheHorizonWithoutReflectingTheirDirection() {
        val direction = spaceCompassSunPathDirections(synthetic(-35.0))[2]
        val orientation = facing(-1.0, -35.0)
        val arrow = projectSpaceCompassSunPathArrow(direction, orientation, 400.0, 600.0, 5.0)!!
        assertTrue(arrow.belowHorizon)
        assertTrue(arrow.tip.x > arrow.center.x)
    }

    @Test fun dstCountsUseActualRegularDotsAndUtcInstantsRatherThanAssuming24Hours() {
        for ((localDate, count) in listOf("2026-03-29" to 23, "2026-10-25" to 25)) {
            val path = calculateSpaceCompassSunDailyPath(LocalDate.parse(localDate), zone, 45.0, 9.0)
            val regular = path.markers.filter { it.event == SpaceCompassSunPathEvent.HOUR }
            val arrows = spaceCompassSunPathDirections(path)
            assertEquals(count, regular.size)
            assertEquals(count / 4, arrows.size)
            arrows.forEachIndexed { i, arrow ->
                val index = i * 4 + 3
                val until = regular.getOrNull(index + 1)?.timeMs ?: path.samples.last().timeMs
                assertEquals(regular[index].timeMs + (until - regular[index].timeMs) / 2, arrow.timeMs)
            }
        }
    }

    @Test fun issKeepsSixArrowsOverOneRevolutionRatherThanUsingItsHundredsOfDenseSamples() {
        val orbit = SpaceCompassIssOrbit.parse("1 25544U 98067A   26276.04623379  .00005750  00000+0  11349-3 0  9994\n" +
            "2 25544  51.6314 126.3061 0006899 216.4733 143.5786 15.48722558588472")
        val now = Instant.parse("2026-10-03T18:35:00Z").toEpochMilli()
        val path = calculateSpaceCompassIssPath(date.minusDays(1), zone, now, 45.0, 9.0, 0.0, orbit)!!
        assertTrue(path.samples.size > 500)
        val arrows = spaceCompassSunPathDirections(path)
        assertEquals(6, arrows.size)
        assertTrue(arrows.all { it.timeMs > path.samples.first().timeMs && it.timeMs < path.samples.last().timeMs })
        assertTrue(arrows.any { it.center.up < 0 })
        for (direction in arrows) {
            val az = Math.toDegrees(atan2(direction.center.east, direction.center.north))
            val el = Math.toDegrees(asin(direction.center.up))
            val projected = projectSpaceCompassSunPathArrow(direction, facing(az, el), 400.0, 600.0, 5.0)!!
            assertEquals(direction.center.up < 0, projected.belowHorizon)
        }
    }

    @Test fun hiddenClippedStationaryAndInvalidArrowsAreOmitted() {
        val direction = spaceCompassSunPathDirections(synthetic())[2]
        assertNull(projectSpaceCompassSunPathArrow(direction, facing(180.0), 400.0, 600.0, 5.0))
        assertNull(projectSpaceCompassSunPathArrow(direction.copy(end = direction.start), facing(0.0), 400.0, 600.0, 5.0))
        assertNull(projectSpaceCompassSunPathArrow(direction.copy(center = SpaceCompassSunVector(Double.NaN, 1.0, 0.0)),
            facing(0.0), 400.0, 600.0, 5.0))
        for ((w, h, size) in listOf(Triple(0.0, 600.0, 5.0), Triple(400.0, -1.0, 5.0),
            Triple(Double.NaN, 600.0, 5.0), Triple(400.0, 600.0, 0.0), Triple(400.0, 600.0, Double.NaN),
            Triple(6.0, 6.0, 5.0))) assertNull(projectSpaceCompassSunPathArrow(direction, facing(0.0), w, h, size))
        val path = synthetic()
        assertTrue(spaceCompassSunPathDirections(path.copy(samples = emptyList())).isEmpty())
        assertTrue(spaceCompassSunPathDirections(path.copy(markers = path.markers.take(3))).isEmpty())
        assertTrue(spaceCompassSunPathDirections(path.copy(samples = listOf(path.samples.first()))).isEmpty())
    }

    @Test fun arrowsStayWhollyInsidePortraitLandscapeAndRolledViewports() {
        val path = calculateSpaceCompassSunDailyPath(date, zone, 45.0, 9.0)
        val directions = spaceCompassSunPathDirections(path)
        var visible = 0
        for ((width, height) in listOf(400.0 to 600.0, 900.0 to 300.0, 20.0 to 20.0))
            for (azimuth in 0..330 step 30) for (elevation in listOf(-85.0, -30.0, 0.0, 30.0, 85.0))
                for (roll in listOf(0.0, 90.0, 180.0)) for (direction in directions) {
                    val arrow = projectSpaceCompassSunPathArrow(direction, facing(azimuth.toDouble(), elevation, roll),
                        width, height, 5.0) ?: continue
                    visible++
                    for (point in listOf(arrow.center, arrow.tip, arrow.left, arrow.right)) {
                        assertTrue(point.x.isFinite() && point.x in 2.0..(width - 2))
                        assertTrue(point.y.isFinite() && point.y in 2.0..(height - 2))
                    }
                }
        assertTrue(visible > 50)
    }

    @Test fun renderingCachesAnchorsAndLeavesMarkerHitTestingAndVisibilityUnchanged() {
        val source = listOf(File("src/main/java/me/mondiversi/spacecompass/SpaceCompassSunDailyPathLayer.kt"),
            File("app/src/main/java/me/mondiversi/spacecompass/SpaceCompassSunDailyPathLayer.kt")).first { it.isFile }.readText()
        assertTrue(source.contains("remember(path) { spaceCompassSunPathDirections(path) }"))
        assertFalse(source.contains("state.visibleFor") || source.contains("if (!pathVisible)"))
        assertTrue(source.indexOf("arrows.forEach") < source.indexOf("points.forEach"))
        assertTrue(source.contains("tappedSpaceCompassSunPathPoint(currentPoints"))
        assertTrue(source.contains("< 16.dp.toPx()"))
        assertTrue(source.contains("< 26.dp.toPx()"))
    }
}
