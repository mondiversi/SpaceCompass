package me.mondiversi.spacecompass

import java.time.LocalDate
import java.time.ZoneId
import kotlin.math.*
import org.junit.Assert.*
import org.junit.Test

class SpaceCompassCelestialTimeFocusTest {
    private val path = calculateSpaceCompassSunDailyPath(LocalDate.parse("2026-10-04"),
        ZoneId.of("Europe/Rome"), 45.0, 9.0)
    private fun facing(position: SpaceCompassSunPosition): SpaceCompassSunOrientation {
        val a = Math.toRadians(position.azimuthDegrees)
        val e = Math.toRadians(position.elevationDegrees)
        return SpaceCompassSunOrientation(SpaceCompassSunVector(cos(a), -sin(a), 0.0),
            SpaceCompassSunVector(-sin(e) * sin(a), -sin(e) * cos(a), cos(e)),
            SpaceCompassSunVector(cos(e) * sin(a), cos(e) * cos(a), sin(e)))
    }
    private fun projected(orientation: SpaceCompassSunOrientation, width: Double = 340.0, height: Double = 400.0) =
        path.markers.mapNotNull { marker ->
            projectSpaceCompassSun(marker.position, orientation, width, height).takeIf { it.visible }
                ?.let { marker to SpaceCompassSunScenePoint(it.x, it.y) }
        }

    @Test fun theNearestDotInsideTheReticleIsChosenAndChangesAsThePhoneMoves() {
        val first = listOf("09:00" to SpaceCompassSunScenePoint(170.0, 200.0), "10:00" to SpaceCompassSunScenePoint(180.0, 205.0))
        assertEquals("09:00", focusedSpaceCompassCelestialPathPoint(first, 340.0, 400.0, 19.0))
        val moved = listOf("09:00" to SpaceCompassSunScenePoint(165.0, 190.0), "10:00" to SpaceCompassSunScenePoint(170.0, 200.0))
        assertEquals("10:00", focusedSpaceCompassCelestialPathPoint(moved, 340.0, 400.0, 19.0))
    }

    @Test fun allHourlyDotsIncludingThoseUnderTheGroundRetainTheirOwnTimestamp() {
        val hours = path.markers.filter { it.event == SpaceCompassSunPathEvent.HOUR }
        assertEquals(24, hours.size)
        assertTrue(hours.any { it.position.elevationDegrees < 0 })
        for (hour in hours) {
            val focused = focusedSpaceCompassCelestialPathPoint(projected(facing(hour.position)), 340.0, 400.0, 19.0)
            assertEquals(hour, focused)
            assertEquals(hour.timeMs, focused!!.timeMs)
        }
    }

    @Test fun sunriseCulminationAndSunsetUseTheirPreciseEventTimeNotANeighboringHour() {
        val events = path.markers.filter { it.event != SpaceCompassSunPathEvent.HOUR }
        assertEquals(4, events.size)
        for (event in events) assertEquals(event,
            focusedSpaceCompassCelestialPathPoint(projected(facing(event.position)), 340.0, 400.0, 19.0))
    }

    @Test fun lookingAwayFromEveryDotReturnsNothingInsteadOfTheNearestOffReticleDot() {
        assertNull(focusedSpaceCompassCelestialPathPoint(projected(facing(path.markers.first().position.copy(
            elevationDegrees = 90.0))), 340.0, 400.0, 19.0))
        assertNull(focusedSpaceCompassCelestialPathPoint(listOf("away" to SpaceCompassSunScenePoint(186.0, 216.0)),
            340.0, 400.0, 19.0)) // Outside the circle despite being inside its bounding square.
    }

    @Test fun theFocusAreaMatchesTheDrawnReticleAtEveryDensityAndViewportSize() {
        for (density in listOf(1.0, 2.0, 3.0)) for ((width, height) in listOf(340.0 to 400.0, 1000.0 to 700.0)) {
            val w = width * density; val h = height * density
            val r = SPACE_COMPASS_CELESTIAL_RETICLE_RADIUS_DP * density
            assertEquals("inside", focusedSpaceCompassCelestialPathPoint(listOf("inside" to
                SpaceCompassSunScenePoint(w / 2 + 18.0 * density, h / 2)), w, h, r))
            assertNull(focusedSpaceCompassCelestialPathPoint(listOf("outside" to
                SpaceCompassSunScenePoint(w / 2 + 20.0 * density, h / 2)), w, h, r))
        }
    }

    @Test fun missingProjectedPointsInvalidSizesAndOffscreenCoordinatesNeverInventATime() {
        val center = listOf("center" to SpaceCompassSunScenePoint(170.0, 200.0))
        assertNull(focusedSpaceCompassCelestialPathPoint(emptyList<Pair<String, SpaceCompassSunScenePoint>>(), 340.0, 400.0, 19.0))
        for (invalid in listOf(0.0, -1.0, Double.NaN, Double.POSITIVE_INFINITY)) {
            assertNull(focusedSpaceCompassCelestialPathPoint(center, invalid, 400.0, 19.0))
            assertNull(focusedSpaceCompassCelestialPathPoint(center, 340.0, invalid, 19.0))
        }
        assertNull(focusedSpaceCompassCelestialPathPoint(center, 340.0, 400.0, -1.0))
        assertNull(focusedSpaceCompassCelestialPathPoint(center, 340.0, 400.0, Double.NaN))
        assertNull(focusedSpaceCompassCelestialPathPoint(listOf("outside" to SpaceCompassSunScenePoint(-1.0, 200.0)),
            340.0, 400.0, 1000.0))
        assertNull(focusedSpaceCompassCelestialPathPoint(listOf("invalid" to SpaceCompassSunScenePoint(Double.NaN, 200.0)),
            340.0, 400.0, 19.0))
    }

    @Test fun coincidentDotsHaveAStableOrderInsteadOfSwitchingAtEveryRefresh() {
        val points = listOf("first" to SpaceCompassSunScenePoint(170.0, 200.0), "second" to SpaceCompassSunScenePoint(170.0, 200.0))
        repeat(20) { assertEquals("first", focusedSpaceCompassCelestialPathPoint(points, 340.0, 400.0, 19.0)) }
    }

    @Test fun aCoincidentNamedEventWinsTheReticleTieRegardlessOfListOrder() {
        val regular = path.markers.first { it.event == SpaceCompassSunPathEvent.HOUR }
        val center = SpaceCompassSunScenePoint(170.0, 200.0)
        for (event in SpaceCompassSunPathEvent.entries.filter { it != SpaceCompassSunPathEvent.HOUR }) {
            val special = regular.copy(event = event)
            for (markers in listOf(listOf(regular, special), listOf(special, regular))) {
                val points = markers.map { it to center }
                assertEquals(special, focusedSpaceCompassCelestialPathPoint(points, 340.0, 400.0, 19.0) {
                    it.event != SpaceCompassSunPathEvent.HOUR
                })
            }
        }
    }

    @Test fun aNonOverlappingEventDoesNotStealTheFocusOfACloserRegularPoint() {
        val regular = path.markers.first { it.event == SpaceCompassSunPathEvent.HOUR }
        val event = regular.copy(event = SpaceCompassSunPathEvent.CULMINATION)
        val points = listOf(event to SpaceCompassSunScenePoint(172.0, 200.0),
            regular to SpaceCompassSunScenePoint(170.0, 200.0))
        assertEquals(regular, focusedSpaceCompassCelestialPathPoint(points, 340.0, 400.0, 19.0) {
            it.event != SpaceCompassSunPathEvent.HOUR
        })
    }

    @Test fun theLiveBodyIsCurrentEvenWhenItEqualsAnHourlyPointExactly() {
        val live = path.markers.first { it.event == SpaceCompassSunPathEvent.HOUR }
        val center = SpaceCompassSunScenePoint(170.0, 200.0)
        val focus = focusedSpaceCompassCelestialPoint(listOf(live to center), live, center, 340.0, 400.0, 19.0)
        assertEquals(live, focus!!.point)
        assertTrue(focus.isCurrent)
    }

    @Test fun theLiveBodyWinsAnExactTieWithEveryNamedEvent() {
        val live = path.markers.first { it.event == SpaceCompassSunPathEvent.HOUR }
        val center = SpaceCompassSunScenePoint(170.0, 200.0)
        for (event in SpaceCompassSunPathEvent.entries) {
            val focus = focusedSpaceCompassCelestialPoint(listOf(live.copy(event = event) to center),
                live, center, 340.0, 400.0, 19.0)
            assertTrue(focus!!.isCurrent)
            assertEquals(live, focus.point)
        }
    }

    @Test fun aLiveBodyNeedsNoVisibleTrajectoryToShowItsOwnTimeAndAngles() {
        val live = path.markers.first()
        val focus = focusedSpaceCompassCelestialPoint(emptyList(), live, SpaceCompassSunScenePoint(170.0, 200.0),
            340.0, 400.0, 19.0)
        assertTrue(focus!!.isCurrent)
        assertEquals(live.timeMs, focus.point.timeMs)
        assertEquals(live.position, focus.point.position)
    }

    @Test fun aCloserHourlyDotStillHasItsOwnIdentityInsteadOfTheLiveLabel() {
        val hour = path.markers.first()
        val live = hour.copy(timeMs = hour.timeMs + 60_000L)
        val focus = focusedSpaceCompassCelestialPoint(listOf(hour to SpaceCompassSunScenePoint(170.0, 200.0)),
            live, SpaceCompassSunScenePoint(180.0, 200.0), 340.0, 400.0, 19.0)
        assertFalse(focus!!.isCurrent)
        assertEquals(hour, focus.point)
    }

    @Test fun missingOrOffReticleLiveDataNeverInventsACurrentCaption() {
        val live = path.markers.first()
        val center = SpaceCompassSunScenePoint(170.0, 200.0)
        assertNull(focusedSpaceCompassCelestialPoint(emptyList(), null, center, 340.0, 400.0, 19.0))
        assertNull(focusedSpaceCompassCelestialPoint(emptyList(), live, null, 340.0, 400.0, 19.0))
        assertNull(focusedSpaceCompassCelestialPoint(emptyList(), live, SpaceCompassSunScenePoint(190.0, 200.0),
            340.0, 400.0, 19.0))
    }

    @Test fun liveFocusUsesEachFreshInstantWithoutChangingAnEarlierSelectionSnapshot() {
        val original = path.markers.first()
        val center = SpaceCompassSunScenePoint(170.0, 200.0)
        for (second in 0L..3L) {
            val updated = original.copy(timeMs = original.timeMs + second * 1000L,
                position = original.position.copy(azimuthDegrees = original.position.azimuthDegrees + second))
            val focus = focusedSpaceCompassCelestialPoint(emptyList(), updated, center, 340.0, 400.0, 19.0)
            assertEquals(updated, focus!!.point)
            assertTrue(focus.isCurrent)
        }
        assertEquals(path.markers.first(), original)
    }

    @Test fun pathEventTiePreferenceIsPreservedWithAnOffReticleLiveBody() {
        val hour = path.markers.first()
        val event = hour.copy(event = SpaceCompassSunPathEvent.CULMINATION)
        val center = SpaceCompassSunScenePoint(170.0, 200.0)
        val focus = focusedSpaceCompassCelestialPoint(listOf(hour to center, event to center),
            hour.copy(timeMs = hour.timeMs + 1000L), SpaceCompassSunScenePoint(200.0, 200.0), 340.0, 400.0, 19.0)
        assertFalse(focus!!.isCurrent)
        assertEquals(event, focus.point)
    }
}
