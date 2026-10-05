package me.mondiversi.spacecompass

import org.junit.Assert.*
import org.junit.Test

class SpaceCompassSunPathHitTestTest {
    private val first = SpaceCompassSunPathPoint(1L, SpaceCompassSunPosition(90.0, 10.0))
    private val second = SpaceCompassSunPathPoint(2L, SpaceCompassSunPosition(105.0, 15.0))
    private val center = SpaceCompassSunScenePoint(170.0, 200.0)

    @Test fun increasedCircularAreaAcceptsTapsThatMissedTheOldTargetAtEveryDensity() {
        for (density in listOf(1.0, 2.0, 3.0)) {
            val point = SpaceCompassSunScenePoint(center.x * density, center.y * density)
            val radius = SPACE_COMPASS_SUN_PATH_TOUCH_RADIUS_DP * density
            assertEquals(first, tappedSpaceCompassSunPathPoint(listOf(first to point),
                SpaceCompassSunScenePoint(point.x, point.y + 30.0 * density), radius))
            assertEquals(first, tappedSpaceCompassSunPathPoint(listOf(first to point),
                SpaceCompassSunScenePoint(point.x + 32.0 * density, point.y), radius))
            assertNull(tappedSpaceCompassSunPathPoint(listOf(first to point),
                SpaceCompassSunScenePoint(point.x + 33.0 * density, point.y), radius))
        }
    }

    @Test fun circularTargetDoesNotAcceptTheCornersOfItsBoundingSquare() {
        assertNull(tappedSpaceCompassSunPathPoint(listOf(first to center),
            SpaceCompassSunScenePoint(center.x + 25.0, center.y + 25.0), 32.0))
    }

    @Test fun overlappingTargetsChooseTheNearestDotNotTheFirstInTheList() {
        val points = listOf(first to center, second to SpaceCompassSunScenePoint(190.0, 200.0))
        assertEquals(second, tappedSpaceCompassSunPathPoint(points, SpaceCompassSunScenePoint(187.0, 200.0), 32.0))
        assertEquals(first, tappedSpaceCompassSunPathPoint(points.reversed(), SpaceCompassSunScenePoint(173.0, 200.0), 32.0))
    }

    @Test fun coincidentAndEquidistantDotsKeepAStableSelection() {
        repeat(10) {
            assertEquals(first, tappedSpaceCompassSunPathPoint(listOf(first to center, second to center), center, 32.0))
            assertEquals(first, tappedSpaceCompassSunPathPoint(listOf(first to center,
                second to SpaceCompassSunScenePoint(190.0, 200.0)), SpaceCompassSunScenePoint(180.0, 200.0), 32.0))
        }
    }

    @Test fun emptyOrInvalidHitDataNeverCreatesASelection() {
        val points = listOf(first to center)
        assertNull(tappedSpaceCompassSunPathPoint(emptyList(), center, 32.0))
        for (radius in listOf(-1.0, Double.NaN, Double.POSITIVE_INFINITY))
            assertNull(tappedSpaceCompassSunPathPoint(points, center, radius))
        assertNull(tappedSpaceCompassSunPathPoint(points, SpaceCompassSunScenePoint(Double.NaN, 200.0), 32.0))
        assertNull(tappedSpaceCompassSunPathPoint(points, SpaceCompassSunScenePoint(170.0, Double.POSITIVE_INFINITY), 32.0))
        assertEquals(second, tappedSpaceCompassSunPathPoint(listOf(first to SpaceCompassSunScenePoint(Double.NaN, 200.0),
            second to center), center, 32.0))
    }

    @Test fun exactLiveInstantWinsAVisibleHourlyDotAtTheSameLocation() {
        val current = first.copy(timeMs = 1_500L)
        val points = selectableSpaceCompassCelestialPathPoints(listOf(first to center), current, center)
        assertEquals(current, tappedSpaceCompassSunPathPoint(points, center, 32.0))
        assertEquals(2, points.size)
    }

    @Test fun aNearbyHourlyDotStillWinsWhenItIsCloserThanTheLivePreview() {
        val current = first.copy(timeMs = 1_500L)
        val hourly = SpaceCompassSunScenePoint(center.x + 20, center.y)
        val points = selectableSpaceCompassCelestialPathPoints(listOf(second to hourly), current, center)
        assertEquals(second, tappedSpaceCompassSunPathPoint(points, hourly, 32.0))
        assertEquals(current, tappedSpaceCompassSunPathPoint(points, center, 32.0))
    }

    @Test fun anOffscreenLivePointCannotBecomeAHiddenTapTarget() {
        val points = listOf(first to center)
        assertEquals(points, selectableSpaceCompassCelestialPathPoints(points, second, null))
        assertEquals(points, selectableSpaceCompassCelestialPathPoints(points, null, center))
        assertEquals(second, tappedSpaceCompassSunPathPoint(selectableSpaceCompassCelestialPathPoints(emptyList(), second, center),
            center, 32.0))
    }

    @Test fun steppingFromALiveInstantUsesThePreviousAndNextTimesAndWrapsSafely() {
        val markers = listOf(first.copy(timeMs = 100), second.copy(timeMs = 200), first.copy(timeMs = 300))
        val live = first.copy(timeMs = 150)
        assertEquals(markers[0], adjacentSpaceCompassSunPathPoint(markers, live, false))
        assertEquals(markers[1], adjacentSpaceCompassSunPathPoint(markers, live, true))
        assertEquals(markers[2], adjacentSpaceCompassSunPathPoint(markers, markers[0], false))
        assertEquals(markers[0], adjacentSpaceCompassSunPathPoint(markers, markers[2], true))
        assertNull(adjacentSpaceCompassSunPathPoint(emptyList(), live, true))
        assertNull(adjacentSpaceCompassSunPathPoint(emptyList(), live, false))
    }

    @Test fun existingStepControlsKeepCoincidentHourlyAndSpecialMarkersSelectable() {
        val event = first.copy(event = SpaceCompassSunPathEvent.SUNRISE)
        val markers = listOf(first, event, second)
        assertEquals(event, adjacentSpaceCompassSunPathPoint(markers, first, true))
        assertEquals(first, adjacentSpaceCompassSunPathPoint(markers, event, false))
        assertEquals(second, adjacentSpaceCompassSunPathPoint(markers, event, true))
    }
}
