package me.mondiversi.spacecompass

import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import org.junit.Assert.*
import org.junit.Test

class SpaceCompassTutorialCardLimitTest {
    @Test fun explanationsHaveTheSameTallerHeightOnNormalDisplays() {
        assertEquals(240f, spaceCompassTutorialCardHeightLimit(900f), .01f)
        assertEquals(240f, spaceCompassTutorialCardHeightLimit(600f), .01f)
        assertEquals(238f, spaceCompassTutorialCardHeightLimit(350f), .01f)
    }
    @Test fun tinyWindowsStillBoundTheFixedCard() {
        listOf(0f, 20f, 100f, 220f, 350f).forEach { height ->
            val limit = spaceCompassTutorialCardHeightLimit(height)
            assertTrue(limit >= 0f)
            assertTrue(limit <= (height - 24f).coerceAtLeast(0f))
            assertTrue(limit <= 240f)
        }
    }
    @Test fun fixedLandscapeCardLeavesTheControlRowsAccessible() {
        val controls = listOf(Rect(12f, 12f, 320f, 50f), Rect(12f, 306f, 320f, 348f),
            Rect(440f, 12f, 788f, 348f))
        val area = Rect(0f, 0f, 420f, 360f)
        val card = Size(320f, spaceCompassTutorialCardHeightLimit(area.height))
        val position = spaceCompassTutorialCardPosition(area, card, 12f)
        assertTrue(controls.none { it.overlaps(Rect(position, card)) })
    }
    @Test fun tallerCardRetainsClearanceFromTheActualControlRows() {
        val area = Rect(0f, 0f, 420f, 360f)
        val controls = listOf(Rect(12f, 12f, 320f, 72f), Rect(12f, 306f, 320f, 348f))
        val height = spaceCompassTutorialCardClearHeight(area, 360f, controls, 8f, 240f)
        assertEquals(200f, height, .01f)
        val card = Rect(spaceCompassTutorialCardPosition(area, Size(360f, height), 8f), Size(360f, height))
        assertTrue(controls.none { it.inflate(8f).overlaps(card) })
    }
    @Test fun controlsBesideTheCardDoNotReduceItsReadingArea() {
        val height = spaceCompassTutorialCardClearHeight(Rect(0f, 0f, 500f, 500f), 360f,
            listOf(Rect(0f, 220f, 50f, 280f), Rect(450f, 220f, 500f, 280f)), 8f, 240f)
        assertEquals(240f, height, .01f)
    }

}
