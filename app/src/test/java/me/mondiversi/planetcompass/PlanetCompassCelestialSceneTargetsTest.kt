package me.mondiversi.planetcompass

import java.time.LocalDate
import java.time.ZoneId
import kotlin.math.*
import org.junit.Assert.*
import org.junit.Test

class PlanetCompassCelestialSceneTargetsTest {
    private val point = PlanetCompassSunPathPoint(1_791_112_320_000L, PlanetCompassSunPosition(180.0, 25.0))
    private fun path(body: PlanetCompassCelestialBody, markers: List<PlanetCompassSunPathPoint>) =
        PlanetCompassSunDailyPath(LocalDate.parse("2026-10-04"), ZoneId.of("UTC"), markers, markers, body)
    private fun facing(position: PlanetCompassSunPosition): PlanetCompassSunOrientation {
        val a = Math.toRadians(position.azimuthDegrees); val e = Math.toRadians(position.elevationDegrees)
        return PlanetCompassSunOrientation(PlanetCompassSunVector(cos(a), -sin(a), 0.0),
            PlanetCompassSunVector(-sin(e) * sin(a), -sin(e) * cos(a), cos(e)),
            PlanetCompassSunVector(cos(e) * sin(a), cos(e) * cos(a), sin(e)))
    }
    private fun targets(paths: Map<PlanetCompassCelestialBody, PlanetCompassSunDailyPath>,
        live: Map<PlanetCompassCelestialBody, PlanetCompassSunPathPoint> = emptyMap(),
        pose: PlanetCompassSunOrientation? = facing(point.position)) =
        projectPlanetCompassCelestialSceneTargets(paths, live, pose, 340.0, 400.0)

    @Test fun anyVisibleTrajectoryCanBeFocusedAndTappedRegardlessOfTheInspectedBody() {
        val mars = path(PlanetCompassCelestialBody.MARS, listOf(point))
        val sun = path(PlanetCompassCelestialBody.SUN, listOf(point.copy(position = PlanetCompassSunPosition(0.0, 25.0))))
        val projected = targets(mapOf(sun.body to sun, mars.body to mars))
        val focus = focusedPlanetCompassCelestialSceneTarget(projected, 340.0, 400.0, 19.0)!!
        val tap = tappedPlanetCompassCelestialSceneTarget(projected, PlanetCompassSunScenePoint(170.0, 200.0), 32.0)!!
        for (target in listOf(focus, tap)) {
            assertEquals(mars.body, target.body); assertEquals(point, target.point)
            assertSame(mars, target.path); assertFalse(target.isCurrent)
        }
    }

    @Test fun everyRegularAndSpecialPointRetainsItsOwnBodyTimestampAndAnglesAboveOrBelowHorizon() {
        for (elevation in listOf(-60.0, 0.0, 25.0)) for (event in PlanetCompassSunPathEvent.entries) {
            val marker = point.copy(position = point.position.copy(elevationDegrees = elevation), event = event)
            val moon = path(PlanetCompassCelestialBody.MOON, listOf(marker))
            val focus = focusedPlanetCompassCelestialSceneTarget(targets(mapOf(moon.body to moon),
                pose = facing(marker.position)), 340.0, 400.0, 19.0)!!
            assertEquals(marker, focus.point); assertEquals(moon.body, focus.body)
            assertEquals(marker, tappedPlanetCompassCelestialSceneTarget(listOf(focus), focus.projection, 32.0)!!.point)
        }
    }

    @Test fun unselectedLiveObjectsHaveCurrentCaptionsWithoutInventingTrajectories() {
        val live = targets(emptyMap(), mapOf(PlanetCompassCelestialBody.VOYAGER_1 to point))
        val focus = focusedPlanetCompassCelestialSceneTarget(live, 340.0, 400.0, 19.0)!!
        assertTrue(focus.isCurrent); assertNull(focus.path)
        assertEquals(PlanetCompassCelestialBody.VOYAGER_1, focus.body); assertEquals(point, focus.point)
    }

    @Test fun exactTiesPreferLiveThenNamedEventsWithStableCatalogueOrder() {
        val event = point.copy(event = PlanetCompassSunPathEvent.CULMINATION)
        val mars = path(PlanetCompassCelestialBody.MARS, listOf(point, event))
        val named = targets(mapOf(mars.body to mars))
        assertEquals(event, tappedPlanetCompassCelestialSceneTarget(named, PlanetCompassSunScenePoint(170.0, 200.0), 32.0)!!.point)
        val live = targets(mapOf(mars.body to mars), mapOf(PlanetCompassCelestialBody.MOON to point))
        assertTrue(focusedPlanetCompassCelestialSceneTarget(live, 340.0, 400.0, 19.0)!!.isCurrent)
        assertTrue(tappedPlanetCompassCelestialSceneTarget(live, PlanetCompassSunScenePoint(170.0, 200.0), 32.0)!!.isCurrent)
    }

    @Test fun aCloserRegularPointIsNotStolenByANamedEventOrCurrentPosition() {
        val regular = PlanetCompassCelestialSceneTarget(PlanetCompassCelestialBody.MARS, point, PlanetCompassSunScenePoint(170.0, 200.0))
        val special = regular.copy(point = point.copy(event = PlanetCompassSunPathEvent.SUNRISE),
            projection = PlanetCompassSunScenePoint(173.0, 200.0))
        val current = regular.copy(isCurrent = true, projection = PlanetCompassSunScenePoint(178.0, 200.0))
        val list = listOf(current, special, regular)
        assertEquals(regular, focusedPlanetCompassCelestialSceneTarget(list, 340.0, 400.0, 19.0))
        assertEquals(regular, tappedPlanetCompassCelestialSceneTarget(list, PlanetCompassSunScenePoint(170.0, 200.0), 32.0))
    }

    @Test fun aNamedEventAlsoWinsATieAgainstAnotherBodiesRegularPoint() {
        val sun = path(PlanetCompassCelestialBody.SUN, listOf(point))
        val event = point.copy(event = PlanetCompassSunPathEvent.SUNRISE)
        val moon = path(PlanetCompassCelestialBody.MOON, listOf(event))
        val projected = targets(mapOf(sun.body to sun, moon.body to moon))
        assertEquals(event, tappedPlanetCompassCelestialSceneTarget(projected, PlanetCompassSunScenePoint(170.0, 200.0), 32.0)!!.point)
        assertEquals(event, focusedPlanetCompassCelestialSceneTarget(projected, 340.0, 400.0, 19.0)!!.point)
    }

    @Test fun tapTiePriorityDoesNotDependOnCallerListOrder() {
        val regular = PlanetCompassCelestialSceneTarget(PlanetCompassCelestialBody.SUN, point, PlanetCompassSunScenePoint(170.0, 200.0))
        val event = regular.copy(point = point.copy(event = PlanetCompassSunPathEvent.MINIMUM))
        val live = regular.copy(isCurrent = true)
        assertEquals(event, tappedPlanetCompassCelestialSceneTarget(listOf(regular, event), regular.projection, 32.0))
        assertEquals(live, tappedPlanetCompassCelestialSceneTarget(listOf(regular, event, live), regular.projection, 32.0))
    }

    @Test fun missingOrientationOffscreenOrInvalidWindowsDoNotInventTargets() {
        val paths = mapOf(PlanetCompassCelestialBody.SUN to path(PlanetCompassCelestialBody.SUN, listOf(point)))
        assertTrue(targets(paths, pose = null).isEmpty())
        assertTrue(targets(paths, pose = facing(point.position.copy(azimuthDegrees = 0.0))).isEmpty())
        for (invalid in listOf(0.0, -1.0, Double.NaN, Double.POSITIVE_INFINITY)) {
            assertTrue(projectPlanetCompassCelestialSceneTargets(paths, emptyMap(), facing(point.position), invalid, 400.0).isEmpty())
            assertTrue(projectPlanetCompassCelestialSceneTargets(paths, emptyMap(), facing(point.position), 340.0, invalid).isEmpty())
        }
        assertNull(tappedPlanetCompassCelestialSceneTarget(targets(paths), PlanetCompassSunScenePoint(Double.NaN, 200.0), 32.0))
        assertNull(tappedPlanetCompassCelestialSceneTarget(targets(paths), PlanetCompassSunScenePoint(170.0, 200.0), -1.0))
    }
}
