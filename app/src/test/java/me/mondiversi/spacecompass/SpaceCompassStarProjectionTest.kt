package me.mondiversi.spacecompass

import java.time.Instant
import kotlin.math.*
import org.junit.Assert.*
import org.junit.Test

class SpaceCompassStarProjectionTest {
    private val time = Instant.parse("2026-10-07T22:00:00Z").toEpochMilli()

    @Test fun nasaLongitudeRunsLeftwardAndDeclinationRunsFromTopToBottom() {
        fun uv(x: Double, y: Double, z: Double) = spaceCompassStarUv(SpaceCompassViewVector(x, y, z))
        assertEquals(.5, uv(1.0, 0.0, 0.0).first, 1e-12)
        assertEquals(.25, uv(0.0, 1.0, 0.0).first, 1e-12)
        assertEquals(.75, uv(0.0, -1.0, 0.0).first, 1e-12)
        assertEquals(0.0, uv(0.0, 0.0, 1.0).second, 1e-12)
        assertEquals(1.0, uv(0.0, 0.0, -1.0).second, 1e-12)
        assertEquals(.5, uv(1.0, 0.0, 0.0).second, 1e-12)
    }

    @Test fun horizontalBasisIsOrthonormalInBothHemispheresAndAtThePoles() {
        for (latitude in listOf(-90.0, -45.0, 0.0, 45.0, 90.0)) {
            val basis = spaceCompassStarBasis(time, latitude, 8.9, 100.0)
            assertEquals(1.0, basis.east.dot(basis.east), 1e-12)
            assertEquals(1.0, basis.north.dot(basis.north), 1e-12)
            assertEquals(1.0, basis.up.dot(basis.up), 1e-12)
            assertEquals(0.0, basis.east.dot(basis.north), 1e-12)
            assertEquals(0.0, basis.east.dot(basis.up), 1e-12)
        }
    }

    @Test fun movingInLongitudeAndChangingTheMomentBothMoveTheStarField() {
        val zero = spaceCompassStarUv(spaceCompassStarBasis(time, 0.0, 0.0, 0.0).up).first
        val east = spaceCompassStarUv(spaceCompassStarBasis(time, 0.0, 90.0, 0.0).up).first
        assertEquals(.25, ((zero - east) % 1 + 1) % 1, .002)
        val later = spaceCompassStarUv(spaceCompassStarBasis(time + 3_600_000, 0.0, 0.0, 0.0).up).first
        assertEquals(1 / 23.93447, ((zero - later) % 1 + 1) % 1, .0001)
    }

    @Test fun reticleAndOrbitProjectionUseTheSameRayWithPanelOffsetsAndRoll() {
        for (frame in listOf(SpaceCompassSunSceneFrame(10.0, 60.0, 380.0, 600.0),
            SpaceCompassSunSceneFrame(20.0, 55.0, 600.0, 280.0))) {
            for (roll in listOf(-90.0, 0.0, 45.0, 180.0)) {
                val angle = Math.toRadians(roll)
                val orientation = SpaceCompassSunOrientation(SpaceCompassSunVector(cos(angle), 0.0, sin(angle)),
                    SpaceCompassSunVector(-sin(angle), 0.0, cos(angle)), SpaceCompassSunVector(0.0, 1.0, 0.0))
                val position = SpaceCompassSunPosition(5.0, 12.0)
                val point = projectSpaceCompassSun(position, orientation, frame.width, frame.height)
                assertTrue(point.visible)
                val ray = spaceCompassStarViewRay(frame.left + point.x, frame.top + point.y, orientation, frame)
                val expected = SpaceCompassSunVector(cos(Math.toRadians(12.0)) * sin(Math.toRadians(5.0)),
                    cos(Math.toRadians(12.0)) * cos(Math.toRadians(5.0)), sin(Math.toRadians(12.0)))
                assertEquals(1.0, ray.dot(expected), 1e-12)
            }
        }
    }

    @Test fun projectionIncludesTheCorrectCelestialPoleForEachHemisphere() {
        val date = Instant.parse("2000-01-01T12:00:00Z").toEpochMilli()
        for (latitude in listOf(-45.0, 45.0)) {
            val basis = spaceCompassStarBasis(date, latitude, 0.0, 0.0)
            val pole = basis.transform(SpaceCompassSunVector(0.0, cos(Math.toRadians(latitude)), sin(Math.toRadians(latitude))))
            assertEquals(1.0, pole.z, .0001)
        }
    }

    @Test fun twilightIsGradualAndTheDaySkyHasNoStars() {
        assertEquals(0f, spaceCompassStarVisibility(20.0, null), 0f)
        assertEquals(0f, spaceCompassStarVisibility(-3.0, null), 0f)
        assertEquals(1f, spaceCompassStarVisibility(-18.0, null), 0f)
        assertTrue(spaceCompassStarVisibility(-6.0, null) in 0f..0.2f)
        assertTrue(spaceCompassStarVisibility(-12.0, null) > spaceCompassStarVisibility(-6.0, null))
        assertEquals(0f, spaceCompassStarVisibility(null, null), 0f)
        assertEquals(0f, spaceCompassStarVisibility(Double.NaN, null), 0f)
    }

    @Test fun cloudsDimTheMapAndFogAndOvercastHideIt() {
        fun weather(kind: SpaceCompassSunWeatherKind, cover: Float) = SpaceCompassSunWeatherSnapshot(kind, cover, time)
        assertTrue(spaceCompassStarVisibility(-20.0, weather(SpaceCompassSunWeatherKind.PARTLY_CLOUDY, .4f)) < spaceCompassStarVisibility(-20.0, null))
        assertEquals(0f, spaceCompassStarVisibility(-20.0, weather(SpaceCompassSunWeatherKind.FOG, .1f)), 0f)
        assertEquals(0f, spaceCompassStarVisibility(-20.0, weather(SpaceCompassSunWeatherKind.CLOUDY, 1f)), 0f)
    }

    @Test fun starsNeverAppearUnderTheGroundAndFadeNearTheHorizon() {
        assertEquals(0.0, spaceCompassStarHorizonOpacity(-1.0), 0.0)
        assertEquals(0.0, spaceCompassStarHorizonOpacity(0.0), 0.0)
        assertTrue(spaceCompassStarHorizonOpacity(.05) in 0.0..1.0)
        assertEquals(1.0, spaceCompassStarHorizonOpacity(1.0), 0.0)
    }
}
