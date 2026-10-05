package me.mondiversi.spacecompass

import java.time.LocalDate
import java.time.ZoneId
import kotlin.math.*
import org.junit.Assert.*
import org.junit.Test

class SpaceCompassCelestialSceneTargetsTest {
    private val point = SpaceCompassSunPathPoint(1_791_112_320_000L, SpaceCompassSunPosition(180.0, 25.0))
    private fun path(body: SpaceCompassCelestialBody, markers: List<SpaceCompassSunPathPoint>) =
        SpaceCompassSunDailyPath(LocalDate.parse("2026-10-04"), ZoneId.of("UTC"), markers, markers, body)
    private fun facing(position: SpaceCompassSunPosition): SpaceCompassSunOrientation {
        val a = Math.toRadians(position.azimuthDegrees); val e = Math.toRadians(position.elevationDegrees)
        return SpaceCompassSunOrientation(SpaceCompassSunVector(cos(a), -sin(a), 0.0),
            SpaceCompassSunVector(-sin(e) * sin(a), -sin(e) * cos(a), cos(e)),
            SpaceCompassSunVector(cos(e) * sin(a), cos(e) * cos(a), sin(e)))
    }
    private fun targets(paths: Map<SpaceCompassCelestialBody, SpaceCompassSunDailyPath>,
        live: Map<SpaceCompassCelestialBody, SpaceCompassSunPathPoint> = emptyMap(),
        pose: SpaceCompassSunOrientation? = facing(point.position)) =
        projectSpaceCompassCelestialSceneTargets(paths, live, pose, 340.0, 400.0)

    @Test fun anyVisibleTrajectoryCanBeFocusedAndTappedRegardlessOfTheInspectedBody() {
        val mars = path(SpaceCompassCelestialBody.MARS, listOf(point))
        val sun = path(SpaceCompassCelestialBody.SUN, listOf(point.copy(position = SpaceCompassSunPosition(0.0, 25.0))))
        val projected = targets(mapOf(sun.body to sun, mars.body to mars))
        val focus = focusedSpaceCompassCelestialSceneTarget(projected, 340.0, 400.0, 19.0)!!
        val tap = tappedSpaceCompassCelestialSceneTarget(projected, SpaceCompassSunScenePoint(170.0, 200.0), 32.0)!!
        for (target in listOf(focus, tap)) {
            assertEquals(mars.body, target.body); assertEquals(point, target.point)
            assertSame(mars, target.path); assertFalse(target.isCurrent)
        }
    }

    @Test fun everyRegularAndSpecialPointRetainsItsOwnBodyTimestampAndAnglesAboveOrBelowHorizon() {
        for (elevation in listOf(-60.0, 0.0, 25.0)) for (event in SpaceCompassSunPathEvent.entries) {
            val marker = point.copy(position = point.position.copy(elevationDegrees = elevation), event = event)
            val moon = path(SpaceCompassCelestialBody.MOON, listOf(marker))
            val focus = focusedSpaceCompassCelestialSceneTarget(targets(mapOf(moon.body to moon),
                pose = facing(marker.position)), 340.0, 400.0, 19.0)!!
            assertEquals(marker, focus.point); assertEquals(moon.body, focus.body)
            assertEquals(marker, tappedSpaceCompassCelestialSceneTarget(listOf(focus), focus.projection, 32.0)!!.point)
        }
    }

    @Test fun unselectedLiveObjectsHaveCurrentCaptionsWithoutInventingTrajectories() {
        val live = targets(emptyMap(), mapOf(SpaceCompassCelestialBody.VOYAGER_1 to point))
        val focus = focusedSpaceCompassCelestialSceneTarget(live, 340.0, 400.0, 19.0)!!
        assertTrue(focus.isCurrent); assertNull(focus.path)
        assertEquals(SpaceCompassCelestialBody.VOYAGER_1, focus.body); assertEquals(point, focus.point)
    }

    @Test fun exactTiesPreferLiveThenNamedEventsWithStableCatalogueOrder() {
        val event = point.copy(event = SpaceCompassSunPathEvent.CULMINATION)
        val mars = path(SpaceCompassCelestialBody.MARS, listOf(point, event))
        val named = targets(mapOf(mars.body to mars))
        assertEquals(event, tappedSpaceCompassCelestialSceneTarget(named, SpaceCompassSunScenePoint(170.0, 200.0), 32.0)!!.point)
        val live = targets(mapOf(mars.body to mars), mapOf(SpaceCompassCelestialBody.MOON to point))
        assertTrue(focusedSpaceCompassCelestialSceneTarget(live, 340.0, 400.0, 19.0)!!.isCurrent)
        assertTrue(tappedSpaceCompassCelestialSceneTarget(live, SpaceCompassSunScenePoint(170.0, 200.0), 32.0)!!.isCurrent)
    }

    @Test fun aCloserRegularPointIsNotStolenByANamedEventOrCurrentPosition() {
        val regular = SpaceCompassCelestialSceneTarget(SpaceCompassCelestialBody.MARS, point, SpaceCompassSunScenePoint(170.0, 200.0))
        val special = regular.copy(point = point.copy(event = SpaceCompassSunPathEvent.SUNRISE),
            projection = SpaceCompassSunScenePoint(173.0, 200.0))
        val current = regular.copy(isCurrent = true, projection = SpaceCompassSunScenePoint(178.0, 200.0))
        val list = listOf(current, special, regular)
        assertEquals(regular, focusedSpaceCompassCelestialSceneTarget(list, 340.0, 400.0, 19.0))
        assertEquals(regular, tappedSpaceCompassCelestialSceneTarget(list, SpaceCompassSunScenePoint(170.0, 200.0), 32.0))
    }

    @Test fun aNamedEventAlsoWinsATieAgainstAnotherBodiesRegularPoint() {
        val sun = path(SpaceCompassCelestialBody.SUN, listOf(point))
        val event = point.copy(event = SpaceCompassSunPathEvent.SUNRISE)
        val moon = path(SpaceCompassCelestialBody.MOON, listOf(event))
        val projected = targets(mapOf(sun.body to sun, moon.body to moon))
        assertEquals(event, tappedSpaceCompassCelestialSceneTarget(projected, SpaceCompassSunScenePoint(170.0, 200.0), 32.0)!!.point)
        assertEquals(event, focusedSpaceCompassCelestialSceneTarget(projected, 340.0, 400.0, 19.0)!!.point)
    }

    @Test fun tapTiePriorityDoesNotDependOnCallerListOrder() {
        val regular = SpaceCompassCelestialSceneTarget(SpaceCompassCelestialBody.SUN, point, SpaceCompassSunScenePoint(170.0, 200.0))
        val event = regular.copy(point = point.copy(event = SpaceCompassSunPathEvent.MINIMUM))
        val live = regular.copy(isCurrent = true)
        assertEquals(event, tappedSpaceCompassCelestialSceneTarget(listOf(regular, event), regular.projection, 32.0))
        assertEquals(live, tappedSpaceCompassCelestialSceneTarget(listOf(regular, event, live), regular.projection, 32.0))
    }

    @Test fun missingOrientationOffscreenOrInvalidWindowsDoNotInventTargets() {
        val paths = mapOf(SpaceCompassCelestialBody.SUN to path(SpaceCompassCelestialBody.SUN, listOf(point)))
        assertTrue(targets(paths, pose = null).isEmpty())
        assertTrue(targets(paths, pose = facing(point.position.copy(azimuthDegrees = 0.0))).isEmpty())
        for (invalid in listOf(0.0, -1.0, Double.NaN, Double.POSITIVE_INFINITY)) {
            assertTrue(projectSpaceCompassCelestialSceneTargets(paths, emptyMap(), facing(point.position), invalid, 400.0).isEmpty())
            assertTrue(projectSpaceCompassCelestialSceneTargets(paths, emptyMap(), facing(point.position), 340.0, invalid).isEmpty())
        }
        assertNull(tappedSpaceCompassCelestialSceneTarget(targets(paths), SpaceCompassSunScenePoint(Double.NaN, 200.0), 32.0))
        assertNull(tappedSpaceCompassCelestialSceneTarget(targets(paths), SpaceCompassSunScenePoint(170.0, 200.0), -1.0))
    }
}
