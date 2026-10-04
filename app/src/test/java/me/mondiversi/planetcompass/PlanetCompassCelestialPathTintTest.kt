package me.mondiversi.planetcompass

import androidx.compose.ui.graphics.Color
import org.junit.Assert.*
import org.junit.Test

class PlanetCompassCelestialPathTintTest {
    @Test fun sunIsYellowAndEveryBodyHasItsOwnStableOpaqueColor() {
        assertEquals(Color(0xFFFFD447), planetCompassCelestialPathTint(PlanetCompassCelestialBody.SUN))
        val colors = PlanetCompassCelestialBody.entries.map(::planetCompassCelestialPathTint)
        assertEquals(colors.size, colors.distinct().size)
        colors.forEach { assertEquals(1f, it.alpha, 0f) }
        PlanetCompassCelestialBody.entries.reversed().forEach {
            assertEquals(colors[it.ordinal], planetCompassCelestialPathTint(it))
        }
    }
}
