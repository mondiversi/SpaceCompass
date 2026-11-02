package me.mondiversi.spacecompass

import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import org.junit.Assert.*
import org.junit.Test

class SpaceCompassTutorialPlacementTest {
    private fun target(feature: SpaceCompassTutorialFeature, bounds: Rect) =
        SpaceCompassTutorialTarget(feature, bounds)

    @Test fun portraitCardHasEqualGapsToLocationAndWeatherInsteadOfUsingTheSkyCenter() {
        val sky = Rect(0f, 0f, 400f, 600f)
        val location = target(SpaceCompassTutorialFeature.LOCATION, Rect(200f, 70f, 240f, 110f))
        val weather = target(SpaceCompassTutorialFeature.WEATHER, Rect(200f, 530f, 240f, 570f))
        val area = spaceCompassTutorialCardBand(sky, listOf(location, weather))
        val size = Size(360f, 240f)
        val card = Rect(spaceCompassTutorialCardPosition(area, size, 8f), size)
        assertEquals(320f, card.center.y, .01f)
        assertEquals(card.top - location.bounds.bottom, weather.bounds.top - card.bottom, .01f)
        assertTrue(card.center.y > sky.center.y)
    }

    @Test fun wrappedControlsAndLiftedBottomButtonsStillHaveEqualClearance() {
        val sky = Rect(0f, 0f, 400f, 500f)
        val controls = listOf(
            target(SpaceCompassTutorialFeature.LOCATION, Rect(200f, 20f, 240f, 60f)),
            target(SpaceCompassTutorialFeature.OBJECTS, Rect(180f, 80f, 230f, 130f)),
            target(SpaceCompassTutorialFeature.WEATHER, Rect(200f, 410f, 250f, 460f)),
            target(SpaceCompassTutorialFeature.CAPTURE, Rect(330f, 450f, 390f, 490f)))
        val area = spaceCompassTutorialCardBand(sky, controls)
        val height = spaceCompassTutorialCardClearHeight(area, 360f, controls.map { it.bounds }, 8f, 240f)
        val size = Size(360f, height)
        val card = Rect(spaceCompassTutorialCardPosition(area, size, 8f), size)
        assertEquals(270f, card.center.y, .01f)
        assertEquals(card.top - 130f, 410f - card.bottom, .01f)
        assertTrue(controls.none { it.bounds.inflate(8f).overlaps(card) })
    }

    @Test fun mirroredLandscapeSkyIgnoresOtherColumnControlsAndTransientPointCaptions() {
        val sky = Rect(500f, 0f, 1000f, 400f)
        val controls = listOf(
            target(SpaceCompassTutorialFeature.LOCATION, Rect(700f, 20f, 740f, 60f)),
            target(SpaceCompassTutorialFeature.WEATHER, Rect(700f, 340f, 740f, 380f)),
            target(SpaceCompassTutorialFeature.CAPTURE, Rect(20f, 160f, 60f, 200f)),
            target(SpaceCompassTutorialFeature.REMINDER, Rect(700f, 180f, 740f, 220f)),
            target(SpaceCompassTutorialFeature.OBJECTS, Rect(520f, 350f, 850f, 398f)).copy(circular = false),
            target(SpaceCompassTutorialFeature.POINT, Rect(700f, 230f, 710f, 240f)).copy(point = true))
        val area = spaceCompassTutorialCardBand(sky, controls)
        assertEquals(Rect(500f, 60f, 1000f, 340f), area)
        val position = spaceCompassTutorialCardPosition(area, Size(360f, 240f), 8f)
        assertEquals(750f, position.x + 180f, .01f)
        assertEquals(200f, position.y + 120f, .01f)
    }
}
