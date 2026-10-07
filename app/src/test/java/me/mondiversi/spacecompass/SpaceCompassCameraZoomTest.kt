package me.mondiversi.spacecompass

import org.junit.Assert.*
import org.junit.Test

class SpaceCompassCameraZoomTest {
    private val normal = SpaceCompassCameraZoomOption("normal", 1f, 1f, 8f)
    private val wide = SpaceCompassCameraZoomOption("wide", .5f, 1f, 4f)

    @Test fun separatelyExposedWideLensIsUsedBelowOneAndNormalLensReturnsAtOne() {
        val options = listOf(normal, normal.copy(id = "duplicate"), wide)
        assertEquals(wide, spaceCompassCameraZoomOption(options, .5f))
        assertEquals(wide, spaceCompassCameraZoomOption(options, .8f))
        assertEquals(normal, spaceCompassCameraZoomOption(options, 1f))
        assertEquals(normal, spaceCompassCameraZoomOption(options, 8f))
    }

    @Test fun nativeLogicalWideRangeDoesNotRequireOpeningASecondCamera() {
        val logical = normal.copy(minimum = .5f)
        assertEquals(logical, spaceCompassCameraZoomOption(listOf(logical), .5f))
        assertEquals(logical, spaceCompassCameraZoomOption(listOf(logical), 8f))
    }

    @Test fun stepsReachHardwareLimitsAndDoNotSkipTheNormalLens() {
        val range = SpaceCompassCameraZoomRange(.5f, 8f)
        assertEquals(.5f, range.step(.5f, false), 0f)
        assertEquals(8f, range.step(7.5f, true), 0f)
        assertEquals(1f, range.step(.9f, true), 0f)
        assertEquals(1f, range.step(1.1f, false), 0f)
        assertEquals(1.25f, range.step(1f, true), 0f)
    }

    @Test fun legacyZoomStaysCenteredInsideOffsetSensorCoordinates() {
        assertArrayEquals(intArrayOf(1100, 800, 3100, 2300), spaceCompassCameraZoomCrop(100, 50, 4000, 3000, 2f))
        assertArrayEquals(intArrayOf(100, 50, 4100, 3050), spaceCompassCameraZoomCrop(100, 50, 4000, 3000, 1f))
        assertArrayEquals(intArrayOf(1850, 1362, 2350, 1737), spaceCompassCameraZoomCrop(100, 50, 4000, 3000, 8f))
    }

    @Test fun nativeAndLegacyZoomProjectIdenticalPreviewAndPhotoRaysAtEveryRotation() {
        val reference = SpaceCompassCameraLens(100.0, 50.0, 4000.0, 3000.0, 3000.0, 3100.0, 2140.0, 1480.0, 90)
        val crop = reference.copy(cropLeft = 1100.0, cropTop = 800.0, cropWidth = 2000.0, cropHeight = 1500.0)
        val ratio = reference.withZoomRatio(2f, 100.0, 50.0, 4000.0, 3000.0)
        for (rotation in listOf(0, 90, 180, 270)) {
            val frame = SpaceCompassSunSceneFrame(20.0, 30.0, 380.0, 580.0)
            val legacyPreview = spaceCompassCameraGeometry(crop, 1280.0, 960.0, rotation, 900.0, 650.0, frame)!!
            val nativePreview = spaceCompassCameraGeometry(ratio, 1280.0, 960.0, rotation, 900.0, 650.0, frame)!!
            assertEquals(legacyPreview.perspective, nativePreview.perspective)
            val legacyPhoto = spaceCompassCameraPhotoGeometry(crop, 1600, 1200, rotation)
            val nativePhoto = spaceCompassCameraPhotoGeometry(ratio, 1600, 1200, rotation)
            assertEquals(legacyPhoto, nativePhoto)
        }
    }

    @Test fun widerAndNarrowerPhotoFieldsKeepTheOpticalAxisAndScaleOffCentreDirections() {
        val reference = SpaceCompassCameraLens(0.0, 0.0, 4000.0, 3000.0, 3000.0, 3000.0, 2000.0, 1500.0, 0)
        val widePhoto = spaceCompassCameraPhotoGeometry(reference.withZoomRatio(.5f, 0.0, 0.0, 4000.0, 3000.0), 1600, 1200, 0)
        val normalPhoto = spaceCompassCameraPhotoGeometry(reference, 1600, 1200, 0)
        val zoomPhoto = spaceCompassCameraPhotoGeometry(reference.withZoomRatio(4f, 0.0, 0.0, 4000.0, 3000.0), 1600, 1200, 0)
        assertEquals(.5, widePhoto.perspective.horizontal / normalPhoto.perspective.horizontal, 1e-12)
        assertEquals(4.0, zoomPhoto.perspective.horizontal / normalPhoto.perspective.horizontal, 1e-12)
        for (photo in listOf(widePhoto, normalPhoto, zoomPhoto)) {
            assertEquals(.5, photo.perspective.principalX, 0.0)
            assertEquals(.5, photo.perspective.principalY, 0.0)
        }
    }

    @Test fun directZoomChoicesRespectHardwareBoundsAndKeepArbitraryCurrentRatiosOut() {
        val range = SpaceCompassCameraZoomRange(.51f, 8f)
        val choices = range.presets()
        assertEquals(.51f, choices.first(), 0f)
        assertEquals(8f, choices.last(), 0f)
        assertTrue(choices.contains(1f))
        assertFalse(choices.contains(1.5625f))
        assertEquals(1.5f, range.snap(1.5625f), 0f)
        assertTrue(choices.all { it in range.minimum..range.maximum })
        assertFalse(choices.contains(.5f))
        assertFalse(choices.contains(10f))
        assertEquals(choices.sorted(), choices)
    }

    @Test fun directZoomChoicesKeepEndpointsWithoutDuplicateNearLimitRatios() {
        assertEquals(listOf(1f), SpaceCompassCameraZoomRange(1f, 1f).presets())
        val range = SpaceCompassCameraZoomRange(.51f, 8f)
        val choices = range.presets()
        assertEquals(8f, choices.last(), 0f)
        assertFalse(choices.contains(7.999f))
        assertEquals(8f, range.snap(100f), 0f)
        assertEquals(1f, range.snap(Float.NaN), 0f)
        assertTrue(range.presets().contains(1f))
    }

    @Test fun continuousPinchesCrossTheNormalLensInBothDirections() {
        val range = SpaceCompassCameraZoomRange(.5f, 8f)
        val inward = range.pinch(.75f, 2f)
        assertEquals(1.5f, inward, 0f)
        assertEquals(normal, spaceCompassCameraZoomOption(listOf(normal, wide), inward))
        val outward = range.pinch(inward, .5f)
        assertEquals(.75f, outward, 0f)
        assertEquals(wide, spaceCompassCameraZoomOption(listOf(normal, wide), outward))
    }

    @Test fun reversingAPinchAtHardwareLimitsRespondsWithoutAccumulatedOvershoot() {
        val range = SpaceCompassCameraZoomRange(.5f, 8f)
        assertEquals(8f, range.pinch(7f, 100f), 0f)
        assertEquals(4f, range.pinch(8f, .5f), 0f)
        assertEquals(.5f, range.pinch(.6f, .01f), 0f)
        assertEquals(.75f, range.pinch(.5f, 1.5f), 0f)
    }

    @Test fun unstableOrExtremePointerRatiosNeverProduceAnInvalidCameraRequest() {
        val range = SpaceCompassCameraZoomRange(.5f, 8f)
        for (invalid in listOf(Float.NaN, Float.POSITIVE_INFINITY, 0f, -1f)) {
            assertEquals(2f, range.pinch(2f, invalid), 0f)
        }
        assertEquals(8f, range.pinch(8f, Float.MAX_VALUE), 0f)
        assertEquals(.5f, range.pinch(.5f, Float.MIN_VALUE), 0f)
        assertEquals(1f, range.pinch(Float.NaN, 1f), 0f)
    }

    @Test fun bothButtonDirectionsTraverseExactlyTheSameLevelsAsDirectSelection() {
        val range = SpaceCompassCameraZoomRange(.51f, 8f)
        val levels = range.presets()
        val forward = mutableListOf(range.minimum)
        repeat(levels.size - 1) { forward += range.step(forward.last(), true) }
        assertEquals(levels, forward)
        val reverse = mutableListOf(range.maximum)
        repeat(levels.size - 1) { reverse += range.step(reverse.last(), false) }
        assertEquals(levels.reversed(), reverse)
        assertTrue(listOf(1f, 1.25f, 1.5f, 1.75f, 2f, 2.5f, 3f, 3.5f, 4f, 5f, 6f, 7f, 8f).all { it in levels })
    }

    @Test fun tinyFingerChangesAccumulateThroughTheLadderInsteadOfStallingAtTheFirstLevel() {
        val range = SpaceCompassCameraZoomRange(.51f, 8f)
        var continuous = range.minimum
        var emitted = range.minimum
        val forward = mutableListOf(emitted)
        repeat(800) {
            continuous = range.pinch(continuous, 1.01f)
            val next = range.snap(continuous, emitted)
            if (next != emitted) { emitted = next; forward += next }
        }
        assertEquals(range.presets(), forward)
        val reverse = mutableListOf(emitted)
        repeat(800) {
            continuous = range.pinch(continuous, .99f)
            val next = range.snap(continuous, emitted)
            if (next != emitted) { emitted = next; reverse += next }
        }
        assertEquals(range.presets().reversed(), reverse)
    }

    @Test fun smallFingerJitterCannotRepeatedlyHandOffTheCameraAcrossOneTimes() {
        val range = SpaceCompassCameraZoomRange(.51f, 8f)
        assertEquals(1f, range.snap(.949f, 1f), 0f)
        assertEquals(.9f, range.snap(.939f, 1f), 0f)
        assertEquals(.9f, range.snap(.951f, .9f), 0f)
        assertEquals(1f, range.snap(.961f, .9f), 0f)
    }

    @Test fun nominalLabelsDoNotReplacePhysicalWideLimitsOrActualOpticalZoom() {
        val range = SpaceCompassCameraZoomRange(.51f, 8.01f)
        assertEquals(.51f, range.presets().first(), 0f)
        assertEquals(8.01f, range.presets().last(), 0f)
        assertEquals(.5f, range.displayRatio(.51f), 0f)
        assertEquals(8f, range.displayRatio(7.96f), 0f)
        val labels = range.presets().map(range::displayRatio)
        assertEquals(labels.size, labels.toSet().size)
        assertEquals(0, spaceCompassCameraZoomDigits(8f))
        assertEquals(1, spaceCompassCameraZoomDigits(.5f))
        assertEquals(2, spaceCompassCameraZoomDigits(1.25f))
        val lens = SpaceCompassCameraLens(0.0, 0.0, 4000.0, 3000.0, 3000.0, 3000.0, 2000.0, 1500.0, 0)
        assertEquals(3000.0 * 7.96f, lens.withZoomRatio(7.96f, 0.0, 0.0, 4000.0, 3000.0).focalX, 1e-8)
    }

    @Test fun largerHardwareRangesKeepRegularRelativeStepsAndBothPhysicalEndpoints() {
        for (range in listOf(SpaceCompassCameraZoomRange(.25f, 100f), SpaceCompassCameraZoomRange(.51f, 512f))) {
            val levels = range.presets()
            assertEquals(range.minimum, levels.first(), 0f)
            assertEquals(range.maximum, levels.last(), 0f)
            assertTrue(levels.zipWithNext().all { (a, b) -> b > a && b / a < 1.51f })
            assertTrue(1f in levels)
        }
    }
}
