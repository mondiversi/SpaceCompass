package me.mondiversi.planetcompass

import kotlin.math.abs
import org.junit.Assert.*
import org.junit.Test

class PlanetCompassCelestialOffscreenGeometryTest {
    @Test fun onlyTheInspectedBodyGetsADirectionEvenWithTheEntireCatalogueAvailable() {
        val projections = planetCompassCelestialCatalogOrder.associateWith { PlanetCompassSunProjection(false, 400.0, 200.0, 90.0) }
        for (body in planetCompassCelestialCatalogOrder) {
            val selected = planetCompassCelestialDirectionalProjections(projections, body)
            assertEquals(setOf(body), selected.keys)
            assertEquals(projections.getValue(body), selected.getValue(body))
            assertEquals(setOf(body), placePlanetCompassCelestialOffscreenMarkers(selected, 340.0, 400.0, 56.0).keys)
            assertEquals(56.0, fitPlanetCompassCelestialOffscreenDiameter(selected, 340.0, 400.0, 56.0, emptyList()), 0.0)
        }
        assertEquals(planetCompassCelestialCatalogOrder.size, projections.size)
    }

    @Test fun noSelectionVisibleOrUnavailableBodiesNeverShowAnUnrelatedDirection() {
        val projections = mapOf(PlanetCompassCelestialBody.MOON to PlanetCompassSunProjection(false, 400.0, 200.0, 90.0),
            PlanetCompassCelestialBody.SUN to PlanetCompassSunProjection(true, 170.0, 200.0, 0.0))
        assertTrue(planetCompassCelestialDirectionalProjections(projections, null).isEmpty())
        assertTrue(planetCompassCelestialDirectionalProjections(projections, PlanetCompassCelestialBody.SUN).isEmpty())
        assertTrue(planetCompassCelestialDirectionalProjections(projections, PlanetCompassCelestialBody.SEDNA).isEmpty())
        assertTrue(planetCompassCelestialDirectionalProjections(mapOf(PlanetCompassCelestialBody.SUN to
            PlanetCompassSunProjection(false, Double.NaN, 200.0, 90.0)), PlanetCompassCelestialBody.SUN).isEmpty())
        assertTrue(planetCompassCelestialDirectionalProjections(mapOf(PlanetCompassCelestialBody.SUN to
            PlanetCompassSunProjection(false, 400.0, Double.POSITIVE_INFINITY, 90.0)), PlanetCompassCelestialBody.SUN).isEmpty())
    }

    @Test fun aShortPhoneViewportKeepsAllIndicatorsOnThePerimeterEvenWithAPointPanelOpen() {
        val width = 300.0; val height = 242.0
        for (panel in listOf(0.0, 60.0)) {
            val excluded = if (panel == 0.0) emptyList() else listOf(PlanetCompassSunSceneFrame(0.0, height - panel, width, panel))
            val projections = planetCompassCelestialCatalogOrder.associateWith { PlanetCompassSunProjection(false, width, height / 2, 90.0) }
            val diameter = fitPlanetCompassCelestialOffscreenDiameter(projections, width, height, 56.0, excluded)
            assertTrue(diameter in 56.0 * 0.64..56.0)
            val placements = placePlanetCompassCelestialOffscreenMarkers(projections, width, height, diameter, excluded)
            val centers = placements.values.map { it.center }
            centers.forEachIndexed { index, center ->
                assertTrue(abs(center.x - diameter / 2) < 1e-8 || abs(center.x - (width - diameter / 2)) < 1e-8 ||
                    abs(center.y - diameter / 2) < 1e-8 || abs(center.y - (height - panel - diameter / 2)) < 1e-8)
                centers.drop(index + 1).forEach { other -> assertTrue(abs(center.x - other.x) >= diameter - 1e-8 ||
                    abs(center.y - other.y) >= diameter - 1e-8) }
            }
        }
    }

    @Test fun theEntireCatalogueFitsWithoutOverlapsEvenWhenAllObjectsShareADirection() {
        for ((width, height) in listOf(340.0 to 400.0, 800.0 to 380.0, 1000.0 to 700.0))
            for (density in listOf(1.0, 2.0, 3.0)) {
                val projections = planetCompassCelestialCatalogOrder.associateWith {
                    PlanetCompassSunProjection(false, width * density * 2, height * density / 2, 90.0)
                }
                val diameter = 56.0 * density
                val placements = placePlanetCompassCelestialOffscreenMarkers(projections, width * density, height * density, diameter)
                assertEquals(projections.size, placements.size)
                val centers = placements.values.map { it.center }
                centers.forEachIndexed { index, center ->
                    assertTrue(center.x >= diameter / 2 && center.x <= width * density - diameter / 2)
                    assertTrue(center.y >= diameter / 2 && center.y <= height * density - diameter / 2)
                    assertEquals(0.0, placements.values.elementAt(index).bearingDegrees, 0.0)
                    centers.drop(index + 1).forEach { other ->
                        assertTrue("Overlapping catalogue locators", abs(center.x - other.x) >= diameter ||
                            abs(center.y - other.y) >= diameter)
                    }
                }
            }
    }

    @Test fun cataloguePackingClearsControlsAndLeavesVisibleOrUnknownObjectsAlone() {
        val menu = PlanetCompassSunSceneFrame(240.0, 0.0, 100.0, 70.0)
        val projections = mapOf(PlanetCompassCelestialBody.SUN to PlanetCompassSunProjection(false, 400.0, 0.0, 90.0),
            PlanetCompassCelestialBody.MOON to PlanetCompassSunProjection(false, 400.0, 0.0, 90.0),
            PlanetCompassCelestialBody.MARS to PlanetCompassSunProjection(true, 170.0, 200.0, 0.0))
        val placements = placePlanetCompassCelestialOffscreenMarkers(projections, 340.0, 400.0, 56.0, listOf(menu))
        assertEquals(setOf(PlanetCompassCelestialBody.SUN, PlanetCompassCelestialBody.MOON), placements.keys)
        placements.values.forEach { assertTrue(it.center.x + 28 <= menu.left || it.center.y - 28 >= menu.top + menu.height) }
        assertTrue(placePlanetCompassCelestialOffscreenMarkers(projections, Double.NaN, 400.0, 56.0).isEmpty())
    }

    @Test fun thePointerKeepsTheRealBearingInAllFourDirections() {
        for ((point, angle) in listOf(PlanetCompassSunScenePoint(372.0, 300.0) to 0.0,
            PlanetCompassSunScenePoint(200.0, 572.0) to 90.0, PlanetCompassSunScenePoint(28.0, 300.0) to 180.0,
            PlanetCompassSunScenePoint(200.0, 28.0) to -90.0)) {
            val geometry = placePlanetCompassCelestialOffscreenMarker(PlanetCompassSunProjection(false, point.x, point.y, 90.0),
                400.0, 600.0, 56.0)!!
            assertEquals(angle, geometry.bearingDegrees, 1e-9)
            assertEquals(point, geometry.center)
        }
    }

    @Test fun theWholeThumbnailAndTipStayInsideEveryViewportAndDensity() {
        for ((width, height) in listOf(340.0 to 400.0, 800.0 to 380.0, 20.0 to 20.0))
            for (density in listOf(1.0, 2.0, 3.0)) for ((x, y) in listOf(0.0 to 0.0, width to height,
                -100.0 to height / 2, width * 2 to height / 2)) {
                val geometry = placePlanetCompassCelestialOffscreenMarker(PlanetCompassSunProjection(false, x * density, y * density, 90.0),
                    width * density, height * density, 56.0 * density)!!
                val half = minOf(56.0, width, height) * density / 2
                assertTrue(geometry.center.x - half >= 0 && geometry.center.x + half <= width * density)
                assertTrue(geometry.center.y - half >= 0 && geometry.center.y + half <= height * density)
                assertTrue(geometry.bearingDegrees.isFinite())
            }
    }

    @Test fun clearingMenuButtonsDoesNotRotateOrMirrorTheTargetBearingInLtrOrRtl() {
        for (rtl in listOf(false, true)) {
            val projection = PlanetCompassSunProjection(false, if (rtl) 28.0 else 372.0, 28.0, 90.0)
            val menu = PlanetCompassSunSceneFrame(if (rtl) 0.0 else 296.0, 0.0, 104.0, 56.0)
            val normal = placePlanetCompassCelestialOffscreenMarker(projection, 400.0, 600.0, 56.0)!!
            val clear = placePlanetCompassCelestialOffscreenMarker(projection, 400.0, 600.0, 56.0, listOf(menu))!!
            assertEquals(normal.bearingDegrees, clear.bearingDegrees, 0.0)
            assertTrue(clear.center.y - 28 >= menu.top + menu.height ||
                clear.center.x + 28 <= menu.left || clear.center.x - 28 >= menu.left + menu.width)
            assertTrue(abs(clear.center.x - normal.center.x) + abs(clear.center.y - normal.center.y) > 0)
        }
    }

    @Test fun visibleTargetsAndInvalidGeometryDoNotProduceAnOffscreenLocator() {
        val offscreen = PlanetCompassSunProjection(false, 28.0, 200.0, 90.0)
        assertNull(placePlanetCompassCelestialOffscreenMarker(offscreen.copy(visible = true), 340.0, 400.0, 56.0))
        assertNull(placePlanetCompassCelestialOffscreenMarker(offscreen.copy(x = Double.NaN), 340.0, 400.0, 56.0))
        for (invalid in listOf(0.0, -1.0, Double.NaN, Double.POSITIVE_INFINITY)) {
            assertNull(placePlanetCompassCelestialOffscreenMarker(offscreen, invalid, 400.0, 56.0))
            assertNull(placePlanetCompassCelestialOffscreenMarker(offscreen, 340.0, invalid, 56.0))
            assertNull(placePlanetCompassCelestialOffscreenMarker(offscreen, 340.0, 400.0, invalid))
        }
    }
}
