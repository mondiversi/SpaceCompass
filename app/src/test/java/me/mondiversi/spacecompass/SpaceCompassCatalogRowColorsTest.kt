package me.mondiversi.spacecompass

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.graphics.luminance
import org.junit.Assert.*
import org.junit.Test

class SpaceCompassCatalogRowColorsTest {
    private fun contrast(a: Color, b: Color) =
        (maxOf(a.luminance(), b.luminance()) + .05f) / (minOf(a.luminance(), b.luminance()) + .05f)

    @Test fun everyCategoryKeepsTextAndSmallValuesReadableInBothThemes() {
        for (dark in listOf(false, true)) {
            val base = spaceCompassSurfacePalette(dark).island
            val text = if (dark) Color.White else Color(0xFF101418)
            for (body in spaceCompassCelestialCatalogOrder) for (checked in listOf(false, true)) {
                val fill = spaceCompassCatalogRowBackground(body, checked, base)
                assertEquals(1f, fill.alpha, 0f)
                assertTrue("$body/$dark/$checked name", contrast(text, fill) >= 7f)
                assertTrue("$body/$dark/$checked value", contrast(text.copy(alpha = .72f).compositeOver(fill), fill) >= 4.5f)
            }
        }
    }

    @Test fun categoryColoursRemainVisibleWhileSelectedInsteadOfOneUniformFill() {
        for (dark in listOf(false, true)) {
            val base = spaceCompassSurfacePalette(dark).island
            val selected = spaceCompassCelestialCatalogOrder.map { spaceCompassCatalogRowBackground(it, true, base) }.toSet()
            assertEquals(SpaceCompassCatalogRowFamily.entries.size, selected.size)
            assertNotEquals(spaceCompassCatalogRowBackground(SpaceCompassCelestialBody.SUN, true, base),
                spaceCompassCatalogRowBackground(SpaceCompassCelestialBody.MOON, true, base))
            for (body in spaceCompassCelestialCatalogOrder) {
                val normal = spaceCompassCatalogRowBackground(body, false, base)
                assertNotEquals(spaceCompassCatalogRowBackground(body, true, base), normal)
                val difference = maxOf(kotlin.math.abs(normal.red-base.red), kotlin.math.abs(normal.green-base.green),
                    kotlin.math.abs(normal.blue-base.blue))
                assertTrue("Category tint must be visible: $body/$dark", difference >= .07f)
            }
        }
    }

    @Test fun fictionalReferencesRetainTheirPhysicalColourFamily() {
        assertEquals(SpaceCompassCelestialBody.MARS.catalogRowFamily, SpaceCompassCelestialBody.ARRAKIS.catalogRowFamily)
        assertEquals(SpaceCompassCelestialBody.MOON.catalogRowFamily, SpaceCompassCelestialBody.LV_426.catalogRowFamily)
        assertEquals(SpaceCompassCelestialBody.ISS.catalogRowFamily, SpaceCompassCelestialBody.NOSTROMO.catalogRowFamily)
        assertEquals(SpaceCompassCatalogRowFamily.entries.toSet(), spaceCompassCelestialCatalogOrder.map { it.catalogRowFamily }.toSet())
    }
}
