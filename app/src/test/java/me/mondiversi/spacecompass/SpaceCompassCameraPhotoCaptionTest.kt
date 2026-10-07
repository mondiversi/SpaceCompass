package me.mondiversi.spacecompass

import java.util.Locale
import org.junit.Assert.*
import org.junit.Test

class SpaceCompassCameraPhotoCaptionTest {
    private fun attitude(heading: Double, tilt: Double, usable: Boolean = true): SpaceCompassCameraAttitude {
        val forward = spaceCompassSunCompassDirection(heading, tilt)
        return SpaceCompassCameraAttitude(1_000_000_000L, SpaceCompassSunOrientation(
            SpaceCompassSunVector(1.0, 0.0, 0.0), SpaceCompassSunVector(0.0, 0.0, 1.0), forward), usable)
    }
    private fun format(pointing: SpaceCompassCameraPhotoPointing,
        formatting: SpaceCompassPanoramaFormatting = spaceCompassPanoramaInternationalFormatting) =
        formatSpaceCompassCameraPhotoPointing(pointing, "Direction: $SPACE_COMPASS_SUN_DATA_MARKER",
            "Tilt: $SPACE_COMPASS_SUN_DATA_MARKER", formatting)

    @Test fun thePhotographedRearAxisKeepsBearingAndSignedInclinationAtEveryDisplayRotation() {
        for (rotation in listOf(0, 90, 180, 270)) {
            val source = attitude(237.5, -22.3)
            val pointing = spaceCompassCameraPhotoPointing(source.copy(
                orientation = spaceCompassCameraScreenOrientation(source.orientation, rotation)))
            assertEquals(237.5, pointing.headingDegrees!!, 1e-8)
            assertEquals(-22.3, pointing.tiltDegrees!!, 1e-8)
            assertEquals("Direction: 237.5° · Tilt: -22.3°", format(pointing))
        }
        assertEquals("Direction: 45.0° · Tilt: +12.5°", format(spaceCompassCameraPhotoPointing(attitude(45.0, 12.5))))
    }

    @Test fun missingStaleUnusableAndVerticalPointingCannotPretendToHaveANorthBearing() {
        assertEquals("Direction: — · Tilt: —", format(spaceCompassCameraPhotoPointing(null)))
        val unreliable = spaceCompassCameraPhotoPointing(attitude(45.0, 12.5, false))
        assertNull(unreliable.headingDegrees)
        assertEquals("Direction: — · Tilt: +12.5°", format(unreliable))
        for (tilt in listOf(-90.0, 90.0)) {
            assertNull(spaceCompassCameraPhotoPointing(attitude(45.0, tilt)).headingDegrees)
        }
        val invalid = attitude(45.0, 0.0).let { it.copy(orientation = it.orientation.copy(
            forward = SpaceCompassSunVector(Double.NaN, 1.0, 0.0))) }
        assertEquals("Direction: — · Tilt: —", format(spaceCompassCameraPhotoPointing(invalid)))
        val history = SpaceCompassCameraAttitudeHistory()
        history.add(attitude(45.0, 12.5))
        assertEquals("Direction: — · Tilt: —", format(spaceCompassCameraPhotoPointing(history.at(2_000_000_000L, 0))))
    }

    @Test fun bothExportProfilesUseTheirFrozenFormattingEvenAfterTheProcessLocaleChanges() {
        val selected = SpaceCompassPanoramaFormatting(locale = Locale.ITALIAN,
            numeric = SpaceCompassNumericFormat.SYSTEM, deviceLocale = Locale.ITALIAN)
        val captured = spaceCompassCameraPhotoPointing(attitude(125.5, -.001))
        val previous = Locale.getDefault()
        try {
            Locale.setDefault(Locale.US)
            assertEquals("Direction: 125,5° · Tilt: 0,0°", format(captured, selected))
            assertEquals("Direction: 125.5° · Tilt: 0.0°", format(captured))
        } finally { Locale.setDefault(previous) }
    }

    @Test fun exposureHistoryRatherThanLaterLiveOrientationSuppliesTheFrozenHeader() {
        val history = SpaceCompassCameraAttitudeHistory()
        history.add(attitude(35.0, 20.0))
        val captured = spaceCompassCameraPhotoPointing(history.at(1_005_000_000L, 0))
        history.add(attitude(240.0, -30.0).copy(timeNanos = 1_100_000_000L))
        assertEquals("Direction: 35.0° · Tilt: +20.0°", format(captured))
    }

    @Test fun hidingLocationAndPointLabelsNeverDropsOrChangesTheFrozenCameraPointing() {
        val pointing = format(spaceCompassCameraPhotoPointing(attitude(35.0, 20.0)))
        val data = SpaceCompassPanoramaCaptionData("Frozen time", "Private city", "Region",
            listOf(SpaceCompassSunDataRow("", "45° N", "", "sun-info-coordinates")), null, pointing)
        for (position in SpaceCompassPanoramaPosition.entries) {
            val text = spaceCompassPanoramaCaptionForPosition(data, position)
            assertTrue(text.endsWith(pointing))
            assertFalse(text.contains('\n'))
            if (position != SpaceCompassPanoramaPosition.COMPLETE) assertFalse(text.contains("45° N"))
        }
        assertFalse(spaceCompassPanoramaCaptionForPosition(data.copy(cameraPointing = null),
            SpaceCompassPanoramaPosition.HIDDEN).contains("Direction"))
    }
}
