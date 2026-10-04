package me.mondiversi.planetcompass

import java.time.Instant
import kotlin.math.*
import org.junit.Assert.*
import org.junit.Test

class PlanetCompassSunPositionTest {
    private fun time(value: String) = Instant.parse(value).toEpochMilli()
    private fun facing(azimuth: Double, elevation: Double = 0.0): PlanetCompassSunOrientation {
        val az = Math.toRadians(azimuth); val el = Math.toRadians(elevation)
        return PlanetCompassSunOrientation(PlanetCompassSunVector(cos(az), -sin(az), 0.0),
            PlanetCompassSunVector(-sin(el) * sin(az), -sin(el) * cos(az), cos(el)),
            PlanetCompassSunVector(cos(el) * sin(az), cos(el) * cos(az), sin(el)))
    }

    @Test fun matchesNrelDenverReferenceWithinRecreationalPointingTolerance() {
        // NREL SPA worked example: 2003-10-17 12:30:30 MST, lat/lon/height below.
        // Geometric zenith is about 50.127 degrees; refraction is deliberately not fabricated.
        val sun = calculatePlanetCompassSunPosition(time("2003-10-17T19:30:30Z"), 39.742476, -105.1786, 1830.14)
        assertEquals(194.34024, sun.azimuthDegrees, 0.05)
        assertEquals(39.872, sun.elevationDegrees, 0.05)
    }

    @Test fun northernAndSouthernSolsticeAndNightAreCorrect() {
        val instant = time("2026-06-21T12:00:00Z")
        val north = calculatePlanetCompassSunPosition(instant, 45.0, 0.0)
        val south = calculatePlanetCompassSunPosition(instant, -45.0, 0.0)
        assertEquals(180.0, north.azimuthDegrees, 2.0)
        assertEquals(68.44, north.elevationDegrees, 0.1)
        assertTrue(south.azimuthDegrees < 2 || south.azimuthDegrees > 358)
        assertEquals(21.56, south.elevationDegrees, 0.1)
        assertTrue(calculatePlanetCompassSunPosition(time("2026-06-21T00:00:00Z"), 45.0, 0.0).elevationDegrees < 0)
    }

    @Test fun allLocationsIncludingPolesProduceFiniteNormalizedAngles() {
        for (lat in listOf(-90.0, -80.0, 0.0, 80.0, 90.0)) for (lon in listOf(-180.0, 0.0, 180.0)) {
            val sun = calculatePlanetCompassSunPosition(time("2026-12-21T00:00:00Z"), lat, lon)
            assertTrue(sun.azimuthDegrees.isFinite() && sun.azimuthDegrees in 0.0..360.0)
            assertTrue(sun.elevationDegrees.isFinite() && sun.elevationDegrees in -90.0..90.0)
        }
    }

    @Test fun altitudeIsNotMistakenForAChangeInTheTerrainHorizon() {
        val instant = time("2026-10-03T12:00:00Z")
        val sea = calculatePlanetCompassSunPosition(instant, 45.0, 9.0, 0.0)
        val mountain = calculatePlanetCompassSunPosition(instant, 45.0, 9.0, 4000.0)
        assertTrue(abs(sea.elevationDegrees - mountain.elevationDegrees) < 0.00001)
        assertTrue(mountain.elevationDegrees < sea.elevationDegrees)
    }

    @Test fun sunriseIsEastAndSunsetWestAcrossTheDateLine() {
        val rise = calculatePlanetCompassSunPosition(time("2026-03-20T06:00:00Z"), 0.0, 0.0)
        val set = calculatePlanetCompassSunPosition(time("2026-03-20T18:00:00Z"), 0.0, 0.0)
        assertEquals(90.0, rise.azimuthDegrees, 1.0)
        assertEquals(270.0, set.azimuthDegrees, 1.0)
        val east = calculatePlanetCompassSunPosition(time("2026-10-03T00:00:00Z"), 10.0, 180.0)
        val west = calculatePlanetCompassSunPosition(time("2026-10-03T00:00:00Z"), 10.0, -180.0)
        assertEquals(east.azimuthDegrees, west.azimuthDegrees, 1e-8)
        assertEquals(east.elevationDegrees, west.elevationDegrees, 1e-8)
    }

    @Test fun centerReticleAndOffscreenDirectionsAreCorrect() {
        val orientation = facing(0.0)
        val centered = projectPlanetCompassSun(PlanetCompassSunPosition(0.0, 0.0), orientation, 400.0, 600.0)
        assertTrue(centered.visible); assertEquals(200.0, centered.x, 1e-9)
        assertEquals(300.0, centered.y, 1e-9); assertEquals(0.0, centered.separationDegrees, 1e-9)
        assertTrue(projectPlanetCompassSun(PlanetCompassSunPosition(70.0, 0.0), orientation, 400.0, 600.0).x > 200)
        assertTrue(projectPlanetCompassSun(PlanetCompassSunPosition(290.0, 0.0), orientation, 400.0, 600.0).x < 200)
        assertTrue(projectPlanetCompassSun(PlanetCompassSunPosition(0.0, 70.0), orientation, 400.0, 600.0).y < 300)
        assertTrue(projectPlanetCompassSun(PlanetCompassSunPosition(0.0, -70.0), orientation, 400.0, 600.0).y > 300)
    }

    @Test fun behindThePhoneNeverAppearsAsACenteredSunOrReversesItsArrow() {
        val orientation = facing(0.0)
        val behind = projectPlanetCompassSun(PlanetCompassSunPosition(180.0, 0.0), orientation, 400.0, 600.0)
        assertFalse(behind.visible); assertEquals(180.0, behind.separationDegrees, 1e-6)
        assertTrue(behind.x > 200)
        assertTrue(projectPlanetCompassSun(PlanetCompassSunPosition(150.0, 0.0), orientation, 400.0, 600.0).x > 200)
        assertTrue(projectPlanetCompassSun(PlanetCompassSunPosition(210.0, 0.0), orientation, 400.0, 600.0).x < 200)
    }

    @Test fun pointerStaysOnTheEdgeInPortraitLandscapeAndSmallViewports() {
        for ((w, h) in listOf(400.0 to 600.0, 900.0 to 300.0, 20.0 to 20.0)) {
            val p = projectPlanetCompassSun(PlanetCompassSunPosition(100.0, 40.0), facing(0.0), w, h)
            assertFalse(p.visible); assertTrue(p.x in 0.0..w); assertTrue(p.y in 0.0..h)
        }
    }

    @Test fun cameraAxesAndRollAreUsedInsteadOfJustPhoneTopEdge() {
        val orientation = facing(120.0, 50.0)
        assertEquals(120.0, orientation.headingDegrees, 1e-9)
        assertEquals(50.0, orientation.tiltDegrees, 1e-9)
        assertTrue(projectPlanetCompassSun(PlanetCompassSunPosition(120.0, 50.0), orientation, 400.0, 600.0).visible)
        val rolled = PlanetCompassSunOrientation(orientation.screenUp,
            PlanetCompassSunVector(-orientation.right.east, -orientation.right.north, -orientation.right.up), orientation.forward)
        val p = projectPlanetCompassSun(PlanetCompassSunPosition(140.0, 50.0), rolled, 400.0, 600.0)
        assertTrue(p.y > 300) // A 90-degree roll rotates the rightward direction down.
    }

    @Test fun magneticNorthIsCorrectedToTrueNorth() {
        assertEquals(10.0, wrapPlanetCompassSunDegrees(Math.toDegrees(atan2(
            PlanetCompassSunVector(0.0, 1.0, 0.0).trueNorth(10.0).east,
            PlanetCompassSunVector(0.0, 1.0, 0.0).trueNorth(10.0).north))), 1e-9)
    }

    @Test fun smoothingWrapsNorthWithoutFlippingAndRetainsOrthonormalAxes() {
        val smoothed = smoothPlanetCompassSunOrientation(facing(359.0), facing(1.0))
        assertTrue(smoothed.headingDegrees > 358 || smoothed.headingDegrees < 2)
        assertEquals(0.0, smoothed.right.dot(smoothed.forward), 1e-9)
        assertEquals(0.0, smoothed.screenUp.dot(smoothed.forward), 1e-9)
        assertEquals(1.0, smoothed.screenUp.dot(smoothed.screenUp), 1e-9)
    }

    @Test fun invalidCoordinatesCannotGeneratePlausibleFakeData() {
        for (lat in listOf(Double.NaN, 91.0, -91.0)) {
            assertThrows(IllegalArgumentException::class.java) { calculatePlanetCompassSunPosition(0, lat, 0.0) }
        }
    }
}
