package me.mondiversi.spacecompass

import androidx.compose.ui.geometry.Offset
import org.junit.Assert.*
import org.junit.Test
import kotlin.math.cos
import kotlin.math.sin

class SpaceCompassChevronOpticalCenteringTest {
    @Test fun opticalBalanceSurvivesFittingInAllFourDirectionsAndMirrorsForRtl() {
        for (angle in listOf(0f, 180f, 90f, -90f)) {
            val ink = spaceCompassChevronFloatingInk(angle)
            val offset = spaceCompassChevronOpticalOffset(angle)
            val transform = spaceCompassFloatingGlyphTransform(ink, 48f, offset)
            val radians = Math.toRadians(angle.toDouble())
            // Centroid of the open triangle, relative to the source canvas centre.
            val centroid = Offset(sin(radians).toFloat() * 3.64f / 3f,
                -cos(radians).toFloat() * 3.64f / 3f)
            val afterFitting = centroid * transform.scale + transform.translation
            assertEquals(0f, afterFitting.x, .0001f)
            assertEquals(0f, afterFitting.y, .0001f)
            assertEquals(-offset.x, spaceCompassChevronOpticalOffset(angle + 180f).x, .0001f)
            assertEquals(-offset.y, spaceCompassChevronOpticalOffset(angle + 180f).y, .0001f)
        }
    }

    @Test fun opticalTranslationCannotAlterTheRequestedSizeOrOriginalStroke() {
        val ink = spaceCompassChevronFloatingInk(0f)
        val geometric = spaceCompassFloatingGlyphTransform(ink, 48f)
        val optical = spaceCompassFloatingGlyphTransform(ink, 48f, spaceCompassChevronOpticalOffset(0f))
        assertEquals(geometric.scale, optical.scale, 0f)
        val visibleWidth = (ink.bounds.width - ink.edgeWidth) * optical.scale + ink.edgeWidth
        assertEquals(48f * .53f, visibleWidth, .0001f)
        assertEquals(2.21f, ink.edgeWidth, 0f)
    }
}
