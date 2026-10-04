package me.mondiversi.planetcompass

import org.junit.Assert.*
import org.junit.Test

class PlanetCompassCelestialViewportStateTest {
    @Test fun pinchZoomIsBoundedInBothDirections() {
        assertEquals(3.0, PlanetCompassCelestialViewportState().transform(100.0).zoom, 0.0)
        assertEquals(0.75, PlanetCompassCelestialViewportState().transform(0.001).zoom, 0.0)
        assertEquals(1.5, PlanetCompassCelestialViewportState().transform(1.5).zoom, 0.0)
    }
    @Test fun invalidGestureValuesAreIgnored() {
        val state = PlanetCompassCelestialViewportState(1.5, 0.1, 0.2)
        for (factor in listOf(Double.NaN, Double.POSITIVE_INFINITY, 0.0, -1.0)) assertEquals(state, state.transform(factor))
        assertEquals(state, state.transform(1.5, centerX = Double.NaN))
        assertEquals(state, state.transform(1.5, deltaY = Double.POSITIVE_INFINITY))
    }
    @Test fun pinchKeepsThePointUnderTheFingersAnchored() {
        val centerX = 0.2; val centerY = -0.1
        val state = PlanetCompassCelestialViewportState().transform(1.5, centerX, centerY)
        assertEquals(centerX, state.panX + centerX * state.zoom, 1e-12)
        assertEquals(centerY, state.panY + centerY * state.zoom, 1e-12)
    }
    @Test fun movementIsBoundedAndShrinkingRecentersTheObject() {
        val state = PlanetCompassCelestialViewportState(3.0).transform(1.0, deltaX = 100.0, deltaY = -100.0)
        assertEquals(1.6, state.panX, 1e-12); assertEquals(-1.6, state.panY, 1e-12)
        assertEquals(PlanetCompassCelestialViewportState(), state.transform(1.0/3.0))
        assertEquals(PlanetCompassCelestialViewportState(0.75), state.transform(0.1))
    }
    @Test fun hittingTheZoomLimitDoesNotKeepShiftingTheImage() {
        val state = PlanetCompassCelestialViewportState(3.0, 0.2, 0.1)
        assertEquals(state, state.transform(2.0, 0.8, -0.4))
    }
    @Test fun textureResolutionNeverExceedsSourceDeviceOrTwoKilopixels() {
        assertEquals(2048, planetCompassCelestialTextureWidth(5926, 16384))
        assertEquals(2048, planetCompassCelestialTextureWidth(2048, 4096))
        assertEquals(1024, planetCompassCelestialTextureWidth(1024, 4096))
        assertEquals(512, planetCompassCelestialTextureWidth(2048, 512))
        assertEquals(1, planetCompassCelestialTextureWidth(0, 0))
    }
    @Test fun higherResolutionDecodeRemainsBoundedWithoutUpsamplingSmallMaps() {
        assertEquals(1, planetCompassCelestialTextureSampleSize(2048, 4096))
        assertEquals(2, planetCompassCelestialTextureSampleSize(5926, 4096))
        assertTrue(Int.MAX_VALUE / planetCompassCelestialTextureSampleSize(Int.MAX_VALUE, 4096) <= 4096)
    }
}
