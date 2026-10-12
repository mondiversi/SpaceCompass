package me.mondiversi.spacecompass

import org.junit.Assert.*
import org.junit.Test

class SpaceCompassOrbitBadgePlacementTest {
    private val point = SpaceCompassSunScenePoint(170.0, 140.0)
    private fun overlaps(a: SpaceCompassSunScenePoint, b: SpaceCompassSunSceneFrame) =
        a.x < b.left + b.width && a.x + 140 > b.left && a.y < b.top + b.height && a.y + 70 > b.top

    @Test fun severalClosePinsStayInsideTheViewportAndAvoidEachOtherAndControls() {
        val blocked = listOf(SpaceCompassSunSceneFrame(0.0, 0.0, 340.0, 40.0),
            SpaceCompassSunSceneFrame(280.0, 330.0, 60.0, 70.0))
        val occupied = mutableListOf<SpaceCompassSunSceneFrame>()
        repeat(4) { index ->
            val placed = placeSpaceCompassOrbitBadge(340.0, 400.0, 140.0, 70.0,
                point.copy(x = point.x + index * 5), 4.0, 18.0, blocked, occupied, true)!!
            assertTrue(placed.x in 4.0..196.0 && placed.y in 4.0..326.0)
            assertFalse((blocked + occupied).any { overlaps(placed, it) })
            occupied += SpaceCompassSunSceneFrame(placed.x, placed.y, 140.0, 70.0)
        }
    }
    @Test fun crowdedPinsRemainOpenButAnUnselectedReticlePreviewMayYieldSpace() {
        val full = listOf(SpaceCompassSunSceneFrame(0.0, 0.0, 340.0, 400.0))
        assertNotNull(placeSpaceCompassOrbitBadge(340.0, 400.0, 140.0, 70.0, point, 4.0, 18.0, emptyList(), full, true))
        assertNull(placeSpaceCompassOrbitBadge(340.0, 400.0, 140.0, 70.0, point, 4.0, 18.0, emptyList(), full, false))
    }
    @Test fun invalidOrTooSmallFramesCannotPlaceACaptionOutsideTheApp() {
        assertNull(placeSpaceCompassOrbitBadge(40.0, 30.0, 140.0, 70.0, point, 4.0, 18.0, emptyList(), emptyList(), true))
        assertNull(placeSpaceCompassOrbitBadge(Double.NaN, 400.0, 140.0, 70.0, point, 4.0, 18.0, emptyList(), emptyList(), true))
    }
}
