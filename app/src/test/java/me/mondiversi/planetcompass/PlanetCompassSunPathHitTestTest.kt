package me.mondiversi.planetcompass

import org.junit.Assert.*
import org.junit.Test

class PlanetCompassSunPathHitTestTest {
    private val first = PlanetCompassSunPathPoint(1L, PlanetCompassSunPosition(90.0, 10.0))
    private val second = PlanetCompassSunPathPoint(2L, PlanetCompassSunPosition(105.0, 15.0))
    private val center = PlanetCompassSunScenePoint(170.0, 200.0)

    @Test fun increasedCircularAreaAcceptsTapsThatMissedTheOldTargetAtEveryDensity() {
        for (density in listOf(1.0, 2.0, 3.0)) {
            val point = PlanetCompassSunScenePoint(center.x * density, center.y * density)
            val radius = PLANET_COMPASS_SUN_PATH_TOUCH_RADIUS_DP * density
            assertEquals(first, tappedPlanetCompassSunPathPoint(listOf(first to point),
                PlanetCompassSunScenePoint(point.x, point.y + 30.0 * density), radius))
            assertEquals(first, tappedPlanetCompassSunPathPoint(listOf(first to point),
                PlanetCompassSunScenePoint(point.x + 32.0 * density, point.y), radius))
            assertNull(tappedPlanetCompassSunPathPoint(listOf(first to point),
                PlanetCompassSunScenePoint(point.x + 33.0 * density, point.y), radius))
        }
    }

    @Test fun circularTargetDoesNotAcceptTheCornersOfItsBoundingSquare() {
        assertNull(tappedPlanetCompassSunPathPoint(listOf(first to center),
            PlanetCompassSunScenePoint(center.x + 25.0, center.y + 25.0), 32.0))
    }

    @Test fun overlappingTargetsChooseTheNearestDotNotTheFirstInTheList() {
        val points = listOf(first to center, second to PlanetCompassSunScenePoint(190.0, 200.0))
        assertEquals(second, tappedPlanetCompassSunPathPoint(points, PlanetCompassSunScenePoint(187.0, 200.0), 32.0))
        assertEquals(first, tappedPlanetCompassSunPathPoint(points.reversed(), PlanetCompassSunScenePoint(173.0, 200.0), 32.0))
    }

    @Test fun coincidentAndEquidistantDotsKeepAStableSelection() {
        repeat(10) {
            assertEquals(first, tappedPlanetCompassSunPathPoint(listOf(first to center, second to center), center, 32.0))
            assertEquals(first, tappedPlanetCompassSunPathPoint(listOf(first to center,
                second to PlanetCompassSunScenePoint(190.0, 200.0)), PlanetCompassSunScenePoint(180.0, 200.0), 32.0))
        }
    }

    @Test fun emptyOrInvalidHitDataNeverCreatesASelection() {
        val points = listOf(first to center)
        assertNull(tappedPlanetCompassSunPathPoint(emptyList(), center, 32.0))
        for (radius in listOf(-1.0, Double.NaN, Double.POSITIVE_INFINITY))
            assertNull(tappedPlanetCompassSunPathPoint(points, center, radius))
        assertNull(tappedPlanetCompassSunPathPoint(points, PlanetCompassSunScenePoint(Double.NaN, 200.0), 32.0))
        assertNull(tappedPlanetCompassSunPathPoint(points, PlanetCompassSunScenePoint(170.0, Double.POSITIVE_INFINITY), 32.0))
        assertEquals(second, tappedPlanetCompassSunPathPoint(listOf(first to PlanetCompassSunScenePoint(Double.NaN, 200.0),
            second to center), center, 32.0))
    }

    @Test fun exactLiveInstantWinsAVisibleHourlyDotAtTheSameLocation() {
        val current = first.copy(timeMs = 1_500L)
        val points = selectablePlanetCompassCelestialPathPoints(listOf(first to center), current, center)
        assertEquals(current, tappedPlanetCompassSunPathPoint(points, center, 32.0))
        assertEquals(2, points.size)
    }

    @Test fun aNearbyHourlyDotStillWinsWhenItIsCloserThanTheLivePreview() {
        val current = first.copy(timeMs = 1_500L)
        val hourly = PlanetCompassSunScenePoint(center.x + 20, center.y)
        val points = selectablePlanetCompassCelestialPathPoints(listOf(second to hourly), current, center)
        assertEquals(second, tappedPlanetCompassSunPathPoint(points, hourly, 32.0))
        assertEquals(current, tappedPlanetCompassSunPathPoint(points, center, 32.0))
    }

    @Test fun anOffscreenLivePointCannotBecomeAHiddenTapTarget() {
        val points = listOf(first to center)
        assertEquals(points, selectablePlanetCompassCelestialPathPoints(points, second, null))
        assertEquals(points, selectablePlanetCompassCelestialPathPoints(points, null, center))
        assertEquals(second, tappedPlanetCompassSunPathPoint(selectablePlanetCompassCelestialPathPoints(emptyList(), second, center),
            center, 32.0))
    }

    @Test fun steppingFromALiveInstantUsesThePreviousAndNextTimesAndWrapsSafely() {
        val markers = listOf(first.copy(timeMs = 100), second.copy(timeMs = 200), first.copy(timeMs = 300))
        val live = first.copy(timeMs = 150)
        assertEquals(markers[0], adjacentPlanetCompassSunPathPoint(markers, live, false))
        assertEquals(markers[1], adjacentPlanetCompassSunPathPoint(markers, live, true))
        assertEquals(markers[2], adjacentPlanetCompassSunPathPoint(markers, markers[0], false))
        assertEquals(markers[0], adjacentPlanetCompassSunPathPoint(markers, markers[2], true))
        assertNull(adjacentPlanetCompassSunPathPoint(emptyList(), live, true))
        assertNull(adjacentPlanetCompassSunPathPoint(emptyList(), live, false))
    }

    @Test fun existingStepControlsKeepCoincidentHourlyAndSpecialMarkersSelectable() {
        val event = first.copy(event = PlanetCompassSunPathEvent.SUNRISE)
        val markers = listOf(first, event, second)
        assertEquals(event, adjacentPlanetCompassSunPathPoint(markers, first, true))
        assertEquals(first, adjacentPlanetCompassSunPathPoint(markers, event, false))
        assertEquals(second, adjacentPlanetCompassSunPathPoint(markers, event, true))
    }
}
