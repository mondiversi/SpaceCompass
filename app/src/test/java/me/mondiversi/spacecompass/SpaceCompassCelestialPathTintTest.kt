package me.mondiversi.spacecompass

import androidx.compose.ui.graphics.Color
import org.junit.Assert.*
import org.junit.Test

class SpaceCompassCelestialPathTintTest {
    @Test fun sunIsYellowAndEveryBodyHasItsOwnStableOpaqueColor() {
        assertEquals(Color(0xFFFFD447), spaceCompassCelestialPathTint(SpaceCompassCelestialBody.SUN))
        val colors = SpaceCompassCelestialBody.entries.map(::spaceCompassCelestialPathTint)
        assertEquals(colors.size, colors.distinct().size)
        colors.forEach { assertEquals(1f, it.alpha, 0f) }
        SpaceCompassCelestialBody.entries.reversed().forEach {
            assertEquals(colors[it.ordinal], spaceCompassCelestialPathTint(it))
        }
    }
}
