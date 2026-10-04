package me.mondiversi.planetcompass

import java.time.LocalDate
import java.time.ZoneId
import kotlin.math.*
import org.junit.Assert.*
import org.junit.Test

class PlanetCompassCelestialTimeFocusTest {
    private val path = calculatePlanetCompassSunDailyPath(LocalDate.parse("2026-10-04"),
        ZoneId.of("Europe/Rome"), 45.0, 9.0)
    private fun facing(position: PlanetCompassSunPosition): PlanetCompassSunOrientation {
        val a = Math.toRadians(position.azimuthDegrees)
        val e = Math.toRadians(position.elevationDegrees)
        return PlanetCompassSunOrientation(PlanetCompassSunVector(cos(a), -sin(a), 0.0),
            PlanetCompassSunVector(-sin(e) * sin(a), -sin(e) * cos(a), cos(e)),
            PlanetCompassSunVector(cos(e) * sin(a), cos(e) * cos(a), sin(e)))
    }
    private fun projected(orientation: PlanetCompassSunOrientation, width: Double = 340.0, height: Double = 400.0) =
        path.markers.mapNotNull { marker ->
            projectPlanetCompassSun(marker.position, orientation, width, height).takeIf { it.visible }
                ?.let { marker to PlanetCompassSunScenePoint(it.x, it.y) }
        }

    @Test fun theNearestDotInsideTheReticleIsChosenAndChangesAsThePhoneMoves() {
        val first = listOf("09:00" to PlanetCompassSunScenePoint(170.0, 200.0), "10:00" to PlanetCompassSunScenePoint(180.0, 205.0))
        assertEquals("09:00", focusedPlanetCompassCelestialPathPoint(first, 340.0, 400.0, 19.0))
        val moved = listOf("09:00" to PlanetCompassSunScenePoint(165.0, 190.0), "10:00" to PlanetCompassSunScenePoint(170.0, 200.0))
        assertEquals("10:00", focusedPlanetCompassCelestialPathPoint(moved, 340.0, 400.0, 19.0))
    }

    @Test fun allHourlyDotsIncludingThoseUnderTheGroundRetainTheirOwnTimestamp() {
        val hours = path.markers.filter { it.event == PlanetCompassSunPathEvent.HOUR }
        assertEquals(24, hours.size)
        assertTrue(hours.any { it.position.elevationDegrees < 0 })
        for (hour in hours) {
            val focused = focusedPlanetCompassCelestialPathPoint(projected(facing(hour.position)), 340.0, 400.0, 19.0)
            assertEquals(hour, focused)
            assertEquals(hour.timeMs, focused!!.timeMs)
        }
    }

    @Test fun sunriseCulminationAndSunsetUseTheirPreciseEventTimeNotANeighboringHour() {
        val events = path.markers.filter { it.event != PlanetCompassSunPathEvent.HOUR }
        assertEquals(4, events.size)
        for (event in events) assertEquals(event,
            focusedPlanetCompassCelestialPathPoint(projected(facing(event.position)), 340.0, 400.0, 19.0))
    }

    @Test fun lookingAwayFromEveryDotReturnsNothingInsteadOfTheNearestOffReticleDot() {
        assertNull(focusedPlanetCompassCelestialPathPoint(projected(facing(path.markers.first().position.copy(
            elevationDegrees = 90.0))), 340.0, 400.0, 19.0))
        assertNull(focusedPlanetCompassCelestialPathPoint(listOf("away" to PlanetCompassSunScenePoint(186.0, 216.0)),
            340.0, 400.0, 19.0)) // Outside the circle despite being inside its bounding square.
    }

    @Test fun theFocusAreaMatchesTheDrawnReticleAtEveryDensityAndViewportSize() {
        for (density in listOf(1.0, 2.0, 3.0)) for ((width, height) in listOf(340.0 to 400.0, 1000.0 to 700.0)) {
            val w = width * density; val h = height * density
            val r = PLANET_COMPASS_CELESTIAL_RETICLE_RADIUS_DP * density
            assertEquals("inside", focusedPlanetCompassCelestialPathPoint(listOf("inside" to
                PlanetCompassSunScenePoint(w / 2 + 18.0 * density, h / 2)), w, h, r))
            assertNull(focusedPlanetCompassCelestialPathPoint(listOf("outside" to
                PlanetCompassSunScenePoint(w / 2 + 20.0 * density, h / 2)), w, h, r))
        }
    }

    @Test fun missingProjectedPointsInvalidSizesAndOffscreenCoordinatesNeverInventATime() {
        val center = listOf("center" to PlanetCompassSunScenePoint(170.0, 200.0))
        assertNull(focusedPlanetCompassCelestialPathPoint(emptyList<Pair<String, PlanetCompassSunScenePoint>>(), 340.0, 400.0, 19.0))
        for (invalid in listOf(0.0, -1.0, Double.NaN, Double.POSITIVE_INFINITY)) {
            assertNull(focusedPlanetCompassCelestialPathPoint(center, invalid, 400.0, 19.0))
            assertNull(focusedPlanetCompassCelestialPathPoint(center, 340.0, invalid, 19.0))
        }
        assertNull(focusedPlanetCompassCelestialPathPoint(center, 340.0, 400.0, -1.0))
        assertNull(focusedPlanetCompassCelestialPathPoint(center, 340.0, 400.0, Double.NaN))
        assertNull(focusedPlanetCompassCelestialPathPoint(listOf("outside" to PlanetCompassSunScenePoint(-1.0, 200.0)),
            340.0, 400.0, 1000.0))
        assertNull(focusedPlanetCompassCelestialPathPoint(listOf("invalid" to PlanetCompassSunScenePoint(Double.NaN, 200.0)),
            340.0, 400.0, 19.0))
    }

    @Test fun coincidentDotsHaveAStableOrderInsteadOfSwitchingAtEveryRefresh() {
        val points = listOf("first" to PlanetCompassSunScenePoint(170.0, 200.0), "second" to PlanetCompassSunScenePoint(170.0, 200.0))
        repeat(20) { assertEquals("first", focusedPlanetCompassCelestialPathPoint(points, 340.0, 400.0, 19.0)) }
    }

    @Test fun aCoincidentNamedEventWinsTheReticleTieRegardlessOfListOrder() {
        val regular = path.markers.first { it.event == PlanetCompassSunPathEvent.HOUR }
        val center = PlanetCompassSunScenePoint(170.0, 200.0)
        for (event in PlanetCompassSunPathEvent.entries.filter { it != PlanetCompassSunPathEvent.HOUR }) {
            val special = regular.copy(event = event)
            for (markers in listOf(listOf(regular, special), listOf(special, regular))) {
                val points = markers.map { it to center }
                assertEquals(special, focusedPlanetCompassCelestialPathPoint(points, 340.0, 400.0, 19.0) {
                    it.event != PlanetCompassSunPathEvent.HOUR
                })
            }
        }
    }

    @Test fun aNonOverlappingEventDoesNotStealTheFocusOfACloserRegularPoint() {
        val regular = path.markers.first { it.event == PlanetCompassSunPathEvent.HOUR }
        val event = regular.copy(event = PlanetCompassSunPathEvent.CULMINATION)
        val points = listOf(event to PlanetCompassSunScenePoint(172.0, 200.0),
            regular to PlanetCompassSunScenePoint(170.0, 200.0))
        assertEquals(regular, focusedPlanetCompassCelestialPathPoint(points, 340.0, 400.0, 19.0) {
            it.event != PlanetCompassSunPathEvent.HOUR
        })
    }

    @Test fun theLiveBodyIsCurrentEvenWhenItEqualsAnHourlyPointExactly() {
        val live = path.markers.first { it.event == PlanetCompassSunPathEvent.HOUR }
        val center = PlanetCompassSunScenePoint(170.0, 200.0)
        val focus = focusedPlanetCompassCelestialPoint(listOf(live to center), live, center, 340.0, 400.0, 19.0)
        assertEquals(live, focus!!.point)
        assertTrue(focus.isCurrent)
    }

    @Test fun theLiveBodyWinsAnExactTieWithEveryNamedEvent() {
        val live = path.markers.first { it.event == PlanetCompassSunPathEvent.HOUR }
        val center = PlanetCompassSunScenePoint(170.0, 200.0)
        for (event in PlanetCompassSunPathEvent.entries) {
            val focus = focusedPlanetCompassCelestialPoint(listOf(live.copy(event = event) to center),
                live, center, 340.0, 400.0, 19.0)
            assertTrue(focus!!.isCurrent)
            assertEquals(live, focus.point)
        }
    }

    @Test fun aLiveBodyNeedsNoVisibleTrajectoryToShowItsOwnTimeAndAngles() {
        val live = path.markers.first()
        val focus = focusedPlanetCompassCelestialPoint(emptyList(), live, PlanetCompassSunScenePoint(170.0, 200.0),
            340.0, 400.0, 19.0)
        assertTrue(focus!!.isCurrent)
        assertEquals(live.timeMs, focus.point.timeMs)
        assertEquals(live.position, focus.point.position)
    }

    @Test fun aCloserHourlyDotStillHasItsOwnIdentityInsteadOfTheLiveLabel() {
        val hour = path.markers.first()
        val live = hour.copy(timeMs = hour.timeMs + 60_000L)
        val focus = focusedPlanetCompassCelestialPoint(listOf(hour to PlanetCompassSunScenePoint(170.0, 200.0)),
            live, PlanetCompassSunScenePoint(180.0, 200.0), 340.0, 400.0, 19.0)
        assertFalse(focus!!.isCurrent)
        assertEquals(hour, focus.point)
    }

    @Test fun missingOrOffReticleLiveDataNeverInventsACurrentCaption() {
        val live = path.markers.first()
        val center = PlanetCompassSunScenePoint(170.0, 200.0)
        assertNull(focusedPlanetCompassCelestialPoint(emptyList(), null, center, 340.0, 400.0, 19.0))
        assertNull(focusedPlanetCompassCelestialPoint(emptyList(), live, null, 340.0, 400.0, 19.0))
        assertNull(focusedPlanetCompassCelestialPoint(emptyList(), live, PlanetCompassSunScenePoint(190.0, 200.0),
            340.0, 400.0, 19.0))
    }

    @Test fun liveFocusUsesEachFreshInstantWithoutChangingAnEarlierSelectionSnapshot() {
        val original = path.markers.first()
        val center = PlanetCompassSunScenePoint(170.0, 200.0)
        for (second in 0L..3L) {
            val updated = original.copy(timeMs = original.timeMs + second * 1000L,
                position = original.position.copy(azimuthDegrees = original.position.azimuthDegrees + second))
            val focus = focusedPlanetCompassCelestialPoint(emptyList(), updated, center, 340.0, 400.0, 19.0)
            assertEquals(updated, focus!!.point)
            assertTrue(focus.isCurrent)
        }
        assertEquals(path.markers.first(), original)
    }

    @Test fun pathEventTiePreferenceIsPreservedWithAnOffReticleLiveBody() {
        val hour = path.markers.first()
        val event = hour.copy(event = PlanetCompassSunPathEvent.CULMINATION)
        val center = PlanetCompassSunScenePoint(170.0, 200.0)
        val focus = focusedPlanetCompassCelestialPoint(listOf(hour to center, event to center),
            hour.copy(timeMs = hour.timeMs + 1000L), PlanetCompassSunScenePoint(200.0, 200.0), 340.0, 400.0, 19.0)
        assertFalse(focus!!.isCurrent)
        assertEquals(event, focus.point)
    }
}
