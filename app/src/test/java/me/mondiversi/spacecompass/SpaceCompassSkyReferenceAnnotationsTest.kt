package me.mondiversi.spacecompass

import org.junit.Assert.*
import org.junit.Test
import java.time.Instant
import kotlin.math.abs
import kotlin.math.hypot

class SpaceCompassSkyReferenceAnnotationsTest {
    private val time = Instant.parse("2027-04-07T00:00:00Z").toEpochMilli()

    @Test fun degreesDescribeTheParallelRatherThanItsLocalElevation() {
        for (latitude in listOf(-90.0, -45.0, 0.0, 45.0, 90.0)) {
            val values = spaceCompassSkyReferenceCircles(latitude, time).map {
                formatSpaceCompassSkyReferenceDegrees(it.declinationDegrees)
            }
            assertEquals(listOf("0°", "+23.4°", "−23.4°", "+66.6°", "−66.6°"), values)
        }
        assertEquals("+90°", formatSpaceCompassSkyReferenceDegrees(90.0))
        assertEquals("−90°", formatSpaceCompassSkyReferenceDegrees(-90.0))
        assertEquals("0°", formatSpaceCompassSkyReferenceDegrees(-0.0))
        assertEquals("+23,4°", formatSpaceCompassSkyReferenceDegrees(23.439, SpaceCompassNumericFormat.EUROPEAN))
    }

    @Test fun fullPanoramaRepeatsStayFarApartIncludingAcrossTheNorthSeam() {
        for (latitude in listOf(-90.0, -45.0, 0.0, 45.0, 90.0)) {
            spaceCompassSkyReferenceCircles(latitude, time).forEach { circle ->
                val samples = spaceCompassSkyReferencePanoramaSegments(circle, 4096.0, 2048.0).map {
                    SpaceCompassSunScenePoint((it.start.x + it.end.x) / 2, (it.start.y + it.end.y) / 2)
                }
                val labels = spaceCompassSkyReferenceDegreeAnchors(samples, 640.0, wrapWidth = 4096.0)
                // A compact polar circle may legitimately fit only one sparse annotation.
                assertTrue("$latitude / ${circle.reference}", labels.size in 1..8)
                assertTrue(labels.all { it in samples })
                labels.forEachIndexed { index, point -> labels.drop(index + 1).forEach { other ->
                    val dx = abs(point.x - other.x)
                    assertTrue(hypot(minOf(dx, 4096.0 - dx), point.y - other.y) >= 640.0)
                } }
            }
        }
    }

    @Test fun clippedCameraFragmentsUseOnlyVisibleSamplesAndRespectNameSpacing() {
        val orientation = SpaceCompassSunOrientation(SpaceCompassSunVector(1.0, 0.0, 0.0),
            SpaceCompassSunVector(0.0, 0.0, 1.0), SpaceCompassSunVector(0.0, 1.0, 0.0))
        val samples = spaceCompassSkyReferenceCircles(45.0, time).flatMap { circle ->
            projectSpaceCompassSkyReference(circle, orientation, 1920.0, 1080.0).map {
                SpaceCompassSunScenePoint((it.start.x + it.end.x) / 2, (it.start.y + it.end.y) / 2)
            }
        }
        assertTrue(samples.isNotEmpty())
        val reserved = listOf(samples[samples.size / 2])
        val labels = spaceCompassSkyReferenceDegreeAnchors(samples, 640.0, reserved, maximumLabels = 3)
        assertTrue(labels.isNotEmpty())
        assertTrue(labels.size <= 3)
        assertTrue(labels.all { it in samples && it.x in 0.0..1920.0 && it.y in 0.0..1080.0 })
        labels.forEach { point ->
            assertTrue(hypot(point.x - reserved.first().x, point.y - reserved.first().y) >= 640.0)
        }
    }

    @Test fun wrappedCopiesAndInvalidSamplesCannotCreateClusters() {
        val candidates = listOf(SpaceCompassSunScenePoint(10.0, 100.0),
            SpaceCompassSunScenePoint(4090.0, 100.0), SpaceCompassSunScenePoint(700.0, 100.0),
            SpaceCompassSunScenePoint(Double.NaN, 0.0), SpaceCompassSunScenePoint(700.0, 100.0))
        assertEquals(listOf(candidates[2]), spaceCompassSkyReferenceDegreeAnchors(candidates, 640.0,
            reserved = listOf(SpaceCompassSunScenePoint(0.0, 100.0)), wrapWidth = 4096.0))
        assertTrue(spaceCompassSkyReferenceDegreeAnchors(candidates, Double.NaN).isEmpty())
        assertTrue(spaceCompassSkyReferenceDegreeAnchors(candidates, 0.0).isEmpty())
    }
}
