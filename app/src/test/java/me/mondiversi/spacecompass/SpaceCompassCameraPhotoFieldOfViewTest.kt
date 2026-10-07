package me.mondiversi.spacecompass

import java.util.Locale
import kotlin.math.atan
import org.junit.Assert.*
import org.junit.Test

class SpaceCompassCameraPhotoFieldOfViewTest {
    private val lens = SpaceCompassCameraLens(0.0, 0.0, 4000.0, 3000.0,
        3000.0, 3000.0, 2000.0, 1500.0, 90)
    private fun field(lens: SpaceCompassCameraLens = this.lens, width: Int = 1600, height: Int = 1200,
        rotation: Int = 0) = spaceCompassCameraPhotoFieldOfView(
            spaceCompassCameraPhotoGeometry(lens, width, height, rotation).perspective)

    @Test fun fullJpegFieldRatherThanTheMainViewportSwapsAxesAtEveryDisplayRotation() {
        val h = Math.toDegrees(2 * atan(2000.0 / 3000.0))
        val v = Math.toDegrees(2 * atan(1500.0 / 3000.0))
        for (sensor in listOf(0, 90, 180, 270)) for (display in listOf(0, 90, 180, 270)) {
            val swapped = (sensor - display + 360) % 180 != 0
            val field = field(lens.copy(sensorRotation = sensor), rotation = display)
            assertEquals(if (swapped) v else h, field.horizontalDegrees, 1e-10)
            assertEquals(if (swapped) h else v, field.verticalDegrees, 1e-10)
        }
    }

    @Test fun zoomAndJpegAspectCropChangeTheFieldWithoutUsingNominalBadgeLabels() {
        val ordinary = lens.copy(sensorRotation = 0)
        val base = field(ordinary)
        val zoomed = field(ordinary.copy(cropLeft = 1000.0, cropTop = 750.0,
            cropWidth = 2000.0, cropHeight = 1500.0))
        assertEquals(Math.toDegrees(2 * atan(1000.0 / 3000.0)), zoomed.horizontalDegrees, 1e-10)
        assertEquals(Math.toDegrees(2 * atan(750.0 / 3000.0)), zoomed.verticalDegrees, 1e-10)
        assertTrue(zoomed.horizontalDegrees < base.horizontalDegrees)
        assertTrue(zoomed.verticalDegrees < base.verticalDegrees)
        val wideJpeg = field(ordinary, width = 1600, height = 900)
        assertEquals(base.horizontalDegrees, wideJpeg.horizontalDegrees, 1e-10)
        assertTrue(wideJpeg.verticalDegrees < base.verticalDegrees)
        val ultrawide = field(ordinary.copy(focalX = 1500.0, focalY = 1500.0))
        assertTrue(ultrawide.horizontalDegrees > base.horizontalDegrees)
        assertTrue(ultrawide.verticalDegrees > base.verticalDegrees)
    }

    @Test fun offCentreOpticsUseBothEdgesAndMirroringThePrincipalPointKeepsTheSpan() {
        val a = spaceCompassCameraPhotoFieldOfView(SpaceCompassPerspective(.75, 1.0, .6, .4))
        val b = spaceCompassCameraPhotoFieldOfView(SpaceCompassPerspective(.75, 1.0, .4, .6))
        assertEquals(Math.toDegrees(atan(.6 / .75) + atan(.4 / .75)), a.horizontalDegrees, 1e-10)
        assertEquals(a.horizontalDegrees, b.horizontalDegrees, 1e-10)
        assertEquals(a.verticalDegrees, b.verticalDegrees, 1e-10)
        assertNotEquals(Math.toDegrees(2 * atan(.5 / .75)), a.horizontalDegrees, 1e-6)
    }

    @Test fun selectedAndScientificProfilesKeepTheirCapturedNumberFormats() {
        val fov = SpaceCompassCameraPhotoFieldOfView(67.38, 53.13)
        val template = "Field of view H×V: %1\$s × %2\$s"
        val selected = SpaceCompassPanoramaFormatting(locale = Locale.ITALIAN,
            numeric = SpaceCompassNumericFormat.SYSTEM, deviceLocale = Locale.ITALIAN)
        val previous = Locale.getDefault()
        try {
            Locale.setDefault(Locale.US)
            assertEquals("Field of view H×V: 67,4° × 53,1°",
                formatSpaceCompassCameraPhotoFieldOfView(fov, template, selected))
            assertEquals("Field of view H×V: 67.4° × 53.1°",
                formatSpaceCompassCameraPhotoFieldOfView(fov, template, spaceCompassPanoramaInternationalFormatting))
        } finally { Locale.setDefault(previous) }
    }

    @Test fun photoFieldRemainsAfterHidingLocationWhileVirtualPanoramasHaveNoField() {
        val data = SpaceCompassPanoramaCaptionData("Time", "Town", "Region", emptyList(), null,
            cameraFieldOfView = "Field of view H×V: 67.4° × 53.1°")
        for (position in SpaceCompassPanoramaPosition.entries) {
            val caption = spaceCompassPanoramaCaptionForPosition(data, position)
            assertTrue(caption.endsWith(data.cameraFieldOfView!!))
            assertFalse(caption.contains("\n"))
        }
        assertFalse(spaceCompassPanoramaCaptionForPosition(data.copy(cameraFieldOfView = null),
            SpaceCompassPanoramaPosition.AREA).contains("Field of view"))
    }
}
