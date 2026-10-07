package me.mondiversi.spacecompass

import org.junit.Assert.*
import org.junit.Test
import java.time.Instant
import kotlin.math.*

class SpaceCompassSkyGuidePointsTest {
    private val retired = SpaceCompassCelestialBody.EARTH_CENTER
    @Test fun retiredCentreCannotReturnThroughCatalogFiltersOrLegacyPreferences() {
        assertEquals(30, spaceCompassCelestialCatalogOrder.size)
        assertFalse(retired in spaceCompassAllCelestialOrder)
        assertFalse(retired in spaceCompassAvailableCelestialCatalog(SpaceCompassCelestialSelection().revealHiddenObject().selected))
        for (visibility in SpaceCompassCatalogVisibility.entries)
            assertFalse(retired in spaceCompassFilterCatalog(emptySet(), visibility, mapOf(retired to -89.8)))
        val restored = restoreSpaceCompassCelestialSelection(setOf("EARTH_CENTER", "MOON", "SUN", "LV_426"), "EARTH_CENTER")
        assertEquals(setOf(SpaceCompassCelestialBody.SUN, SpaceCompassCelestialBody.MOON, SpaceCompassCelestialBody.LV_426), restored.selected)
        assertEquals(SpaceCompassCelestialBody.SUN, restored.active)
        val onlyRetired = restoreSpaceCompassCelestialSelection(setOf("EARTH_CENTER"), "EARTH_CENTER")
        assertTrue(onlyRetired.selected.isEmpty()); assertNull(onlyRetired.active)
    }
    @Test fun retiredSelectionEventsAreIgnoredWithoutChangingUserChecksOrActiveObject() {
        val initial = SpaceCompassCelestialSelection(setOf(SpaceCompassCelestialBody.MOON), SpaceCompassCelestialBody.MOON)
        assertEquals(initial, initial.toggle(retired))
        assertEquals(initial, initial.toggleVisible(setOf(retired)))
        assertFalse(retired in initial.toggleAll().selected)
        val updated = initial.toggleVisible(setOf(retired, SpaceCompassCelestialBody.SUN))
        assertEquals(setOf(SpaceCompassCelestialBody.MOON, SpaceCompassCelestialBody.SUN), updated.selected)
        assertEquals(initial.active, updated.active)
    }
    @Test fun zenithIsExactlyOverheadAndPolesKeepTheirExistingCelestialDirections() {
        for (latitude in listOf(-90.0, -45.0, 0.0, 45.0, 90.0)) {
            val points = spaceCompassSkyGuidePoints(latitude).associateBy { it.kind }
            assertEquals(4, points.size)
            assertEquals(SpaceCompassSunVector(0.0, 0.0, 1.0), points.getValue(SpaceCompassSkyGuidePointKind.ZENITH).direction)
            assertEquals(spaceCompassNorthCelestialPole(latitude), points.getValue(SpaceCompassSkyGuidePointKind.NORTH_POLE).direction)
            val n = points.getValue(SpaceCompassSkyGuidePointKind.NORTH_POLE).direction
            val s = points.getValue(SpaceCompassSkyGuidePointKind.SOUTH_POLE).direction
            assertEquals(-1.0, n.dot(s), 1e-12)
        }
    }
    @Test fun geocentreAgreesWithFormerAstronomicalCalculationAcrossHemispheresAndAltitude() {
        val time = Instant.parse("2026-10-07T12:00:00Z").toEpochMilli()
        for (latitude in listOf(-90.0, -60.0, -45.0, 0.0, 45.0, 60.0, 90.0)) for (altitude in listOf(-400.0, 0.0, 3000.0)) {
            val vector = spaceCompassSkyGuidePoints(latitude, altitude).single { it.kind == SpaceCompassSkyGuidePointKind.EARTH_CENTER }.direction
            val legacy = calculateSpaceCompassCelestialObservation(retired, time, latitude, 23.0, altitude)!!.position
            assertEquals(1.0, vector.dot(vector), 1e-12)
            assertEquals(0.0, vector.east, 0.0)
            val elevation = Math.toDegrees(atan2(vector.up, hypot(vector.east, vector.north)))
            assertTrue(elevation < -89.7)
            assertEquals(legacy.elevationDegrees, elevation, 1e-6)
            if (abs(latitude) in 1.0..89.0) {
                val azimuth = wrapSpaceCompassSunDegrees(Math.toDegrees(atan2(vector.east, vector.north)))
                assertEquals(0.0, wrapSpaceCompassSunDegrees(legacy.azimuthDegrees - azimuth + 180.0) - 180.0, 1e-6)
            }
        }
    }
    @Test fun verticalPanoramaAnchorsHaveNoInventedAzimuthOrCenterDependentHorizontalJump() {
        val points = spaceCompassSkyGuidePoints(0.0)
        val zenith = points.single { it.kind == SpaceCompassSkyGuidePointKind.ZENITH }
        val centre = points.single { it.kind == SpaceCompassSkyGuidePointKind.EARTH_CENTER }
        for (direction in SpaceCompassPanoramaCenter.entries) {
            val top = spaceCompassSkyGuidePointProjection(zenith, null, 4096.0, 2048.0, centerAzimuthDegrees = direction.azimuth)!!
            val bottom = spaceCompassSkyGuidePointProjection(centre, null, 4096.0, 2048.0, centerAzimuthDegrees = direction.azimuth)!!
            assertEquals(2048.0, top.x, 0.0); assertEquals(0.0, top.y, 1e-9)
            assertEquals(2048.0, bottom.x, 0.0); assertEquals(2048.0, bottom.y, 1e-9)
        }
    }
    @Test fun photographsOnlyShowCrossesWithinTheirActualFieldOfView() {
        val points = spaceCompassSkyGuidePoints(45.0)
        val perspective = SpaceCompassPerspective(.8, .8, .45, .55)
        for (point in points) {
            val forward = point.direction
            val lookingAt = SpaceCompassSunOrientation(SpaceCompassSunVector(1.0, 0.0, 0.0),
                SpaceCompassSunVector(0.0, -forward.up, forward.north), forward)
            val p = spaceCompassSkyGuidePointProjection(point, lookingAt, 1200.0, 900.0, perspective)!!
            assertEquals(540.0, p.x, 1e-8); assertEquals(495.0, p.y, 1e-8)
            val lookingAway = lookingAt.copy(forward = SpaceCompassSunVector(-forward.east, -forward.north, -forward.up))
            assertNull(spaceCompassSkyGuidePointProjection(point, lookingAway, 1200.0, 900.0, perspective))
        }
    }
}
