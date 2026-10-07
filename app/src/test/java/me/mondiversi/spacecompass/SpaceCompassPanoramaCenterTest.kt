package me.mondiversi.spacecompass

import java.time.LocalDate
import java.time.ZoneId
import org.junit.Assert.*
import org.junit.Test
import kotlin.math.abs

class SpaceCompassPanoramaCenterTest {
    @Test fun missingChoiceUsesCaptureHemisphereAndManualChoiceWinsForLaterLocations() {
        assertEquals(SpaceCompassPanoramaCenter.SOUTH, SpaceCompassPanoramaCenter.fromStored(null, 45.0))
        assertEquals(SpaceCompassPanoramaCenter.NORTH, SpaceCompassPanoramaCenter.fromStored(null, -33.0))
        assertEquals(SpaceCompassPanoramaCenter.SOUTH, SpaceCompassPanoramaCenter.fromStored(null, 0.0))
        assertEquals(SpaceCompassPanoramaCenter.SOUTH, SpaceCompassPanoramaCenter.fromStored(null, null))
        assertEquals(SpaceCompassPanoramaCenter.NORTH, SpaceCompassPanoramaCenter.fromStored("invalid", -10.0))
        for (center in SpaceCompassPanoramaCenter.entries) for (latitude in listOf(-80.0, 0.0, 80.0))
            assertEquals(center, SpaceCompassPanoramaCenter.fromStored(center.key, latitude))
        assertEquals(setOf("north", "east", "south", "west"), SpaceCompassPanoramaCenter.entries.map { it.key }.toSet())
    }

    @Test fun chosenDirectionProjectsToTheCenterWithoutChangingElevation() {
        for (center in SpaceCompassPanoramaCenter.entries) for (elevation in listOf(-90.0, -20.0, 0.0, 35.0, 90.0)) {
            val p = spaceCompassPanoramaPoint(SpaceCompassSunPosition(center.azimuth, elevation), 360.0, 180.0, center.azimuth)!!
            assertEquals(180.0, p.x, 1e-9)
            assertEquals(90.0-elevation, p.y, 1e-9)
        }
    }

    @Test fun cardinalGridLabelsMatchGeometricLocationsInAllFourCenters() {
        for (center in SpaceCompassPanoramaCenter.entries) for (index in 0..8) {
            val source = spaceCompassPanoramaGridIndex(index, 8, center)
            val p = spaceCompassPanoramaPoint(SpaceCompassSunPosition(source*45.0, 0.0), 360.0, 180.0, center.azimuth)!!
            assertEquals((index%8)*45.0, p.x, 1e-9)
        }
    }

    @Test fun orbitSegmentsSplitAtTheChosenSeamRatherThanAcrossThePicture() {
        for (center in SpaceCompassPanoramaCenter.entries) for (reverse in listOf(false, true)) {
            val seam = wrapSpaceCompassSunDegrees(center.azimuth - 180.0)
            val positions = listOf(SpaceCompassSunPosition(seam-1, 20.0), SpaceCompassSunPosition(seam+1, 20.0))
                .let { if (reverse) it.reversed() else it }
            val path = SpaceCompassSunDailyPath(LocalDate.parse("2026-10-07"), ZoneId.of("UTC"),
                positions.mapIndexed { i, p -> SpaceCompassSunPathPoint(i*1000L, p) }, emptyList())
            val segments = spaceCompassPanoramaSegments(path, 360.0, 180.0, center.azimuth)
            assertEquals(2, segments.size)
            assertTrue(segments.all { abs(it.end.x-it.start.x) <= 1.00001 })
            assertEquals(2.0, segments.sumOf { abs(it.end.x-it.start.x) }, 1e-8)
        }
    }

    @Test fun projectedVectorsAndPositionsUseTheSameCenter() {
        for (center in SpaceCompassPanoramaCenter.entries) {
            val vector = SpaceCompassSunVector(1.0, 0.0, 0.0)
            assertEquals(spaceCompassPanoramaPoint(SpaceCompassSunPosition(90.0, 0.0), 360.0, 180.0, center.azimuth),
                spaceCompassPanoramaVectorPoint(vector, 360.0, 180.0, center.azimuth))
        }
        assertNull(spaceCompassPanoramaPoint(SpaceCompassSunPosition(0.0,0.0), 360.0,180.0, Double.NaN))
    }

    @Test fun everyCenterDisclosureLanguageAndLabelProfileHasASeparateExportIdentity() {
        val keys = SpaceCompassPanoramaCenter.entries.flatMap { center ->
            SpaceCompassPanoramaExportMode.entries.flatMap { mode -> SpaceCompassPanoramaPosition.entries.flatMap { position ->
                listOf(false,true).map { labels -> spaceCompassPanoramaVariantKey(position,labels,mode,center) }
            } }
        }
        assertEquals(48, keys.size)
        assertEquals(48, keys.toSet().size)
    }
}
