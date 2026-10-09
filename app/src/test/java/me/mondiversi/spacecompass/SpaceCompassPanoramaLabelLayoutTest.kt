package me.mondiversi.spacecompass

import org.junit.Assert.*
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneId

class SpaceCompassPanoramaLabelLayoutTest {
    private fun place(point: SpaceCompassSunScenePoint, occupied: List<SpaceCompassSunSceneFrame> = emptyList(),
        labelWidth: Double = 260.0, width: Double = 4096.0, height: Double = 2048.0, scale: Double = 1.0,
        radius: Double = 17.0) = spaceCompassPanoramaLabelBounds(point, labelWidth, 34*scale,
            width, height, scale, occupied, radius)
    private fun assertSeparated(a: SpaceCompassSunSceneFrame, b: SpaceCompassSunSceneFrame, gap: Double = 8.0) {
        assertTrue("Overlapping or touching labels: $a / $b",
            a.left+a.width+gap <= b.left+1e-7 || b.left+b.width+gap <= a.left+1e-7 ||
                a.top+a.height+gap <= b.top+1e-7 || b.top+b.height+gap <= a.top+1e-7)
    }

    @Test fun nearbyWideCulminationLabelsAreSeparatedWithTheirActualWidths() {
        val first = place(SpaceCompassSunScenePoint(2048.0, 550.0))!!
        val second = place(SpaceCompassSunScenePoint(2053.0, 580.0), listOf(first), labelWidth = 440.0)!!
        assertSeparated(first, second)
    }

    @Test fun aBlockedLowerSideUsesTheUpperSideInsteadOfCoveringAnotherPill() {
        val point = SpaceCompassSunScenePoint(1000.0, 550.0)
        val lower = SpaceCompassSunSceneFrame(0.0, 550.0, 4096.0, 1498.0)
        val label = place(point, listOf(lower))!!
        assertTrue(label.top+label.height+8 <= lower.top)
    }

    @Test fun severalEventsAtTheSamePositionKeepAllTheirNamesInSeparatePills() {
        val placed = mutableListOf<SpaceCompassSunSceneFrame>()
        for (size in listOf(260.0, 320.0, 400.0, 240.0, 460.0, 280.0)) {
            val label = place(SpaceCompassSunScenePoint(1800.0, 650.0), placed, labelWidth = size)!!
            placed.forEach { assertSeparated(it, label) }
            placed += label
        }
    }

    @Test fun riseAndSetStayClearOfCardinalCaptionsAndCurrentObjectImages() {
        val occupied = mutableListOf(SpaceCompassSunSceneFrame(960.0, 966.0, 160.0, 110.0),
            SpaceCompassSunSceneFrame(1080.0, 1040.0, 100.0, 100.0))
        for (point in listOf(SpaceCompassSunScenePoint(1030.0, 1024.0), SpaceCompassSunScenePoint(1060.0, 1030.0))) {
            val label = place(point, occupied)!!
            occupied.forEach { assertSeparated(it, label) }
            occupied += label
        }
    }

    @Test fun minimumLabelsAtThePanoramaSeamRemainInsideTheImageWithoutOverlap() {
        val occupied = mutableListOf<SpaceCompassSunSceneFrame>()
        for (x in listOf(0.0, 10.0, 4096.0, 4086.0)) {
            val label = place(SpaceCompassSunScenePoint(x, 1650.0), occupied)!!
            assertTrue(label.left >= 4 && label.left+label.width <= 4092)
            occupied.forEach { assertSeparated(it, label) }
            occupied += label
        }
    }

    @Test fun fullyOccupiedAndTooNarrowImagesNeverFallBackToAnOverlappingLabel() {
        assertNull(place(SpaceCompassSunScenePoint(500.0, 500.0),
            listOf(SpaceCompassSunSceneFrame(0.0, 0.0, 4096.0, 2048.0))))
        assertNull(place(SpaceCompassSunScenePoint(50.0, 200.0), labelWidth = 260.0, width = 250.0))
        assertNull(place(SpaceCompassSunScenePoint(Double.NaN, 200.0)))
    }

    @Test fun spacingScalesWithPreviewAndExportResolution() {
        val point = SpaceCompassSunScenePoint(1800.0, 650.0)
        val reference = place(point)!!
        for (scale in listOf(.125, .5, 1.0, 2.0)) {
            val actual = place(SpaceCompassSunScenePoint(point.x*scale, point.y*scale), labelWidth = 260*scale,
                width = 4096*scale, height = 2048*scale, scale = scale)!!
            assertEquals(reference.left, actual.left/scale, 1e-8)
            assertEquals(reference.top, actual.top/scale, 1e-8)
            assertEquals(reference.width, actual.width/scale, 1e-8)
        }
    }

    @Test fun sunAndMoonPrincipalEventsFitBeforeTheRegularHourlyLabels() {
        val date = LocalDate.of(2026, 10, 9); val zone = ZoneId.of("Europe/Rome")
        val paths = listOf(calculateSpaceCompassSunDailyPath(date, zone, 45.0, 9.0),
            calculateSpaceCompassCelestialPath(SpaceCompassCelestialBody.MOON, date, zone,
                date.atStartOfDay(zone).toInstant().toEpochMilli(), 45.0, 9.0, 0.0, SpaceCompassCelestialRemoteData())!!)
        val points = paths.flatMap { it.markers }.sortedBy { it.event == SpaceCompassSunPathEvent.HOUR }
        val occupied = mutableListOf<SpaceCompassSunSceneFrame>()
        var majorCount = 0
        for (point in points) {
            val p = spaceCompassPanoramaPoint(point.position, 4096.0, 2048.0)!!
            val major = point.events.any { it != SpaceCompassSunPathEvent.HOUR }
            val label = place(p, occupied, labelWidth = if (major) 300.0 else 170.0)
            if (major) { assertNotNull("Missing ${point.event}", label); majorCount++ }
            if (label != null) {
                occupied.forEach { assertSeparated(it, label) }
                occupied += label
            }
        }
        assertEquals(8, majorCount)
    }
}
