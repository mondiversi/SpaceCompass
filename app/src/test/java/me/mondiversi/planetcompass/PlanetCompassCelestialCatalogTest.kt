package me.mondiversi.planetcompass

import org.junit.Assert.*
import org.junit.Test
import java.time.Instant
import java.util.Locale
import kotlin.math.sqrt

class PlanetCompassCelestialCatalogTest {
    private val now = Instant.parse("2026-10-04T12:00:00Z").toEpochMilli()
    @Test fun requestedPresentationOrderContainsEveryBodyExactlyOnce() {
        assertEquals(listOf("SUN","MERCURY","VENUS","ISS","STARLINK_V3","MOON","MARS","JUPITER","IO","EUROPA",
            "SATURN","URANUS","NEPTUNE","PLUTO","SEDNA","VOYAGER_1","VOYAGER_2","POLARIS"),
            planetCompassCelestialCatalogOrder.map { it.name })
        assertEquals(PlanetCompassCelestialBody.entries.toSet(), planetCompassCelestialCatalogOrder.toSet())
        assertEquals(PlanetCompassCelestialBody.entries.size,planetCompassCelestialCatalogOrder.size)
    }
    @Test fun catalogueUsesSunDistanceRatherThanEarthObserverRange() {
        assertEquals(0.0,planetCompassCelestialCatalogDistanceAu(PlanetCompassCelestialBody.SUN,now)!!,0.0)
        for (body in listOf(PlanetCompassCelestialBody.ISS,PlanetCompassCelestialBody.STARLINK_V3,PlanetCompassCelestialBody.MOON)) {
            val distance = planetCompassCelestialCatalogDistanceAu(body,now)!!
            assertTrue(distance in 0.98..1.02)
        }
        for (body in listOf(PlanetCompassCelestialBody.JUPITER,PlanetCompassCelestialBody.IO,PlanetCompassCelestialBody.EUROPA))
            assertTrue(planetCompassCelestialCatalogDistanceAu(body,now)!! in 4.8..5.5)
        assertTrue(planetCompassCelestialCatalogDistanceAu(PlanetCompassCelestialBody.POLARIS,now)!! > 20_000_000)
    }
    @Test fun remoteDistancesUseValidatedInterpolatedHeliocentricVectorsWithoutInventedFallback() {
        val body = PlanetCompassCelestialBody.VOYAGER_1
        val samples = listOf(PlanetCompassHorizonsStateSample(now,100.0,30.0,0.0,0.01,0.0,0.0),
            PlanetCompassHorizonsStateSample(now+3_600_000,100.1,30.0,0.0,0.01,0.0,0.0))
        val remote = PlanetCompassCelestialRemoteData(motions = mapOf(body to PlanetCompassHorizonsMotion(body,samples)))
        assertEquals(sqrt(100.05*100.05+900),planetCompassCelestialCatalogDistanceAu(body,now+1_800_000,remote)!!,1e-9)
        assertEquals(sqrt(100.1*100.1+900),planetCompassCelestialCatalogDistanceAu(body,now+3_600_000,remote)!!,1e-9)
        assertNull(planetCompassCelestialCatalogDistanceAu(body,now-1,remote))
        assertNull(planetCompassCelestialCatalogDistanceAu(PlanetCompassCelestialBody.SEDNA,now))
    }
    @Test fun miniatureDistanceHasCoarseLocaleAwarePrecisionAndKeepsUnavailableAsDash() {
        val english=PlanetCompassNumericFormat.INTERNATIONAL
        val italian=PlanetCompassNumericFormat.EUROPEAN
        assertEquals("0.4 AU",formatPlanetCompassCelestialCatalogDistance(0.387,english))
        assertEquals("5,2 AU",formatPlanetCompassCelestialCatalogDistance(5.199,italian))
        for (invalid in listOf(null, -1.0, Double.NaN, Double.POSITIVE_INFINITY, Double.NEGATIVE_INFINITY))
            assertEquals("—",formatPlanetCompassCelestialCatalogDistance(invalid,english))
    }
    @Test fun catalogueSwitchesUnitsAtTenThousandAuWithoutChangingRawDistances() {
        val english=PlanetCompassNumericFormat.INTERNATIONAL
        val below=PLANET_COMPASS_CELESTIAL_CATALOG_LY_THRESHOLD_AU-0.001
        assertEquals("${formatPlanetCompassNumber(below,0,english)} AU",formatPlanetCompassCelestialCatalogDistance(below,english))
        assertEquals("0.2 ly",formatPlanetCompassCelestialCatalogDistance(10_000.0,english))
        assertEquals("0,2 ly",formatPlanetCompassCelestialCatalogDistance(10_001.0,PlanetCompassNumericFormat.EUROPEAN))
        assertEquals("1.0 ly",formatPlanetCompassCelestialCatalogDistance(PLANET_COMPASS_LIGHT_YEAR_KM/PLANET_COMPASS_AU_KM,english))
        assertEquals("0.0 AU",formatPlanetCompassCelestialCatalogDistance(0.0,english))
    }
    @Test fun polarisUsesLightYearsWhilePlanetsAndCurrentProbesKeepAu() {
        val raw=planetCompassCelestialCatalogDistanceAu(PlanetCompassCelestialBody.POLARIS,now)!!
        assertTrue(raw>20_000_000)
        assertEquals("446.5 ly",formatPlanetCompassCelestialCatalogDistance(raw,PlanetCompassNumericFormat.INTERNATIONAL))
        assertEquals("446,5 ly",formatPlanetCompassCelestialCatalogDistance(raw,PlanetCompassNumericFormat.EUROPEAN))
        for (au in listOf(0.387,1.0,5.2,30.0,82.8,144.1,171.9,9999.0))
            assertTrue(formatPlanetCompassCelestialCatalogDistance(au,PlanetCompassNumericFormat.INTERNATIONAL).endsWith(" AU"))
    }
    @Test fun lightYearFormattingUsesSystemLocaleAndAvoidsConversionOverflow() {
        val previous=Locale.getDefault()
        try {
            Locale.setDefault(Locale.ITALY)
            assertEquals("446,5 ly",formatPlanetCompassCelestialCatalogDistance(
                planetCompassCelestialCatalogDistanceAu(PlanetCompassCelestialBody.POLARIS,now),PlanetCompassNumericFormat.SYSTEM))
            Locale.setDefault(Locale.US)
            assertEquals("446.5 ly",formatPlanetCompassCelestialCatalogDistance(
                planetCompassCelestialCatalogDistanceAu(PlanetCompassCelestialBody.POLARIS,now),PlanetCompassNumericFormat.SYSTEM))
        } finally { Locale.setDefault(previous) }
        val largest=formatPlanetCompassCelestialCatalogDistance(Double.MAX_VALUE,PlanetCompassNumericFormat.INTERNATIONAL)
        assertTrue(largest.endsWith(" ly"))
        assertFalse(largest.contains("Infinity"))
    }
    @Test fun mapsShareTheirDocumentedLongitudeSeamsInViewerAndMiniature() {
        assertEquals(0.5,PlanetCompassCelestialBody.IO.textureLongitudeOffset,0.0)
        assertEquals(0.0,PlanetCompassCelestialBody.EUROPA.textureLongitudeOffset,0.0)
        assertEquals(0.0,PlanetCompassCelestialBody.PLUTO.textureLongitudeOffset,0.0)
        assertTrue(PlanetCompassCelestialBody.EUROPA.textureHasUnmappedAreas)
        assertEquals("io.jpg",PlanetCompassCelestialBody.IO.viewerTexture)
        assertEquals("europa.jpg",PlanetCompassCelestialBody.EUROPA.viewerTexture)
    }
}
