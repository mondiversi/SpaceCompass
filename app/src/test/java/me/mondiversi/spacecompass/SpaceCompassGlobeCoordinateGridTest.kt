package me.mondiversi.spacecompass

import org.junit.Assert.*
import org.junit.Test
import kotlin.math.*

class SpaceCompassGlobeCoordinateGridTest {
    @Test fun independentGridUsesRealFifteenDegreeLatitudeAndLongitudeCircles() {
        assertEquals(35,spaceCompassGeographicGrid.size)
        for(curve in spaceCompassGeographicGrid) {
            assertEquals(0,curve.degrees%15)
            curve.vertices.forEach { assertEquals(1.0,it.length,1e-12) }
            if(curve.latitude) curve.vertices.forEach { assertEquals(sin(Math.toRadians(curve.degrees.toDouble())),it.y,1e-12) }
            else assertEquals(curve.degrees.toDouble(),Math.toDegrees(atan2(curve.vertices[30].x,curve.vertices[30].z)),1e-9)
        }
        val equator=spaceCompassGeographicGrid.single { it.latitude&&it.degrees==0 }
        assertEquals(equator.vertices.first().x,equator.vertices.last().x,1e-12)
        assertEquals(equator.vertices.first().z,equator.vertices.last().z,1e-12)
        assertFalse(spaceCompassGeographicGrid.any { it.degrees==23||it.degrees==66 })
    }
    @Test fun coordinatesIdentifyLatitudeAndLongitudeWithoutInventingAzimuthOrElevation() {
        val formatting=spaceCompassPanoramaInternationalFormatting
        assertEquals("30° N",spaceCompassGeographicGridLabel(30,true,formatting))
        assertEquals("30° W",spaceCompassGeographicGridLabel(-30,false,formatting))
        assertEquals("0° N",spaceCompassGeographicGridLabel(0,true,formatting))
        assertEquals("0° E",spaceCompassGeographicGridLabel(0,false,formatting))
        assertEquals("45° S",spaceCompassGeographicGridLabel(-45,true,formatting))
        assertEquals("60° E",spaceCompassGeographicGridLabel(60,false,formatting))
        assertEquals("180° W",spaceCompassGeographicGridLabel(-180,false,formatting))
        assertEquals("180° E",spaceCompassGeographicGridLabel(180,false,formatting))
        assertEquals("90° O",spaceCompassGeographicGridLabel(-90,false,formatting,listOf("N","E","S","O")))
    }
}
