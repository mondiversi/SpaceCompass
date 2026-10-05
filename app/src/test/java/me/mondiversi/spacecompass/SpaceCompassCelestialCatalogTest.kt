package me.mondiversi.spacecompass

import org.junit.Assert.*
import org.junit.Test
import java.time.Instant
import java.util.Locale
import kotlin.math.sqrt

class SpaceCompassCelestialCatalogTest {
    private val now = Instant.parse("2026-10-04T12:00:00Z").toEpochMilli()
    @Test fun requestedPresentationOrderContainsEveryBodyExactlyOnce() {
        assertEquals(listOf("SUN","MERCURY","VENUS","EARTH_CENTER","ISS","STARLINK_V3","MOON","MARS","JUPITER","IO","EUROPA",
            "SATURN","URANUS","NEPTUNE","PLUTO","SEDNA","VOYAGER_1","VOYAGER_2","PROXIMA_CENTAURI","ALPHA_CENTAURI","TRAPPIST_1_E","RX_J1856","POLARIS","PSR_J0437","RIGEL","STEPHENSON_2_18","SAGITTARIUS_A","ANDROMEDA_CORE","TON_618"),
            spaceCompassCelestialCatalogOrder.map { it.name })
        assertEquals(SpaceCompassCelestialBody.entries.toSet(), spaceCompassCelestialCatalogOrder.toSet())
        assertEquals(SpaceCompassCelestialBody.entries.size,spaceCompassCelestialCatalogOrder.size)
    }
    @Test fun catalogueUsesSunDistanceRatherThanEarthObserverRange() {
        assertEquals(0.0,spaceCompassCelestialCatalogDistanceAu(SpaceCompassCelestialBody.SUN,now)!!,0.0)
        for (body in listOf(SpaceCompassCelestialBody.ISS,SpaceCompassCelestialBody.STARLINK_V3,SpaceCompassCelestialBody.MOON)) {
            val distance = spaceCompassCelestialCatalogDistanceAu(body,now)!!
            assertTrue(distance in 0.98..1.02)
        }
        for (body in listOf(SpaceCompassCelestialBody.JUPITER,SpaceCompassCelestialBody.IO,SpaceCompassCelestialBody.EUROPA))
            assertTrue(spaceCompassCelestialCatalogDistanceAu(body,now)!! in 4.8..5.5)
        assertTrue(spaceCompassCelestialCatalogDistanceAu(SpaceCompassCelestialBody.POLARIS,now)!! > 20_000_000)
    }
    @Test fun remoteDistancesUseValidatedInterpolatedHeliocentricVectorsWithoutInventedFallback() {
        val body = SpaceCompassCelestialBody.VOYAGER_1
        val samples = listOf(SpaceCompassHorizonsStateSample(now,100.0,30.0,0.0,0.01,0.0,0.0),
            SpaceCompassHorizonsStateSample(now+3_600_000,100.1,30.0,0.0,0.01,0.0,0.0))
        val remote = SpaceCompassCelestialRemoteData(motions = mapOf(body to SpaceCompassHorizonsMotion(body,samples)))
        assertEquals(sqrt(100.05*100.05+900),spaceCompassCelestialCatalogDistanceAu(body,now+1_800_000,remote)!!,1e-9)
        assertEquals(sqrt(100.1*100.1+900),spaceCompassCelestialCatalogDistanceAu(body,now+3_600_000,remote)!!,1e-9)
        assertNull(spaceCompassCelestialCatalogDistanceAu(body,now-1,remote))
        assertNull(spaceCompassCelestialCatalogDistanceAu(SpaceCompassCelestialBody.SEDNA,now))
    }
    @Test fun miniatureDistanceHasCoarseLocaleAwarePrecisionAndKeepsUnavailableAsDash() {
        val english=SpaceCompassNumericFormat.INTERNATIONAL
        val italian=SpaceCompassNumericFormat.EUROPEAN
        assertEquals("0.4 AU",formatSpaceCompassCelestialCatalogDistance(0.387,english))
        assertEquals("5,2 AU",formatSpaceCompassCelestialCatalogDistance(5.199,italian))
        for (invalid in listOf(null, -1.0, Double.NaN, Double.POSITIVE_INFINITY, Double.NEGATIVE_INFINITY))
            assertEquals("—",formatSpaceCompassCelestialCatalogDistance(invalid,english))
    }
    @Test fun catalogueSwitchesUnitsAtTenThousandAuWithoutChangingRawDistances() {
        val english=SpaceCompassNumericFormat.INTERNATIONAL
        val below=SPACE_COMPASS_CELESTIAL_CATALOG_LY_THRESHOLD_AU-0.001
        assertEquals("${formatSpaceCompassNumber(below,0,english)} AU",formatSpaceCompassCelestialCatalogDistance(below,english))
        assertEquals("0.2 ly",formatSpaceCompassCelestialCatalogDistance(10_000.0,english))
        assertEquals("0,2 ly",formatSpaceCompassCelestialCatalogDistance(10_001.0,SpaceCompassNumericFormat.EUROPEAN))
        assertEquals("1.0 ly",formatSpaceCompassCelestialCatalogDistance(SPACE_COMPASS_LIGHT_YEAR_KM/SPACE_COMPASS_AU_KM,english))
        assertEquals("0.0 AU",formatSpaceCompassCelestialCatalogDistance(0.0,english))
    }
    @Test fun polarisUsesLightYearsWhilePlanetsAndCurrentProbesKeepAu() {
        val raw=spaceCompassCelestialCatalogDistanceAu(SpaceCompassCelestialBody.POLARIS,now)!!
        assertTrue(raw>20_000_000)
        assertEquals("446.5 ly",formatSpaceCompassCelestialCatalogDistance(raw,SpaceCompassNumericFormat.INTERNATIONAL))
        assertEquals("446,5 ly",formatSpaceCompassCelestialCatalogDistance(raw,SpaceCompassNumericFormat.EUROPEAN))
        for (au in listOf(0.387,1.0,5.2,30.0,82.8,144.1,171.9,9999.0))
            assertTrue(formatSpaceCompassCelestialCatalogDistance(au,SpaceCompassNumericFormat.INTERNATIONAL).endsWith(" AU"))
    }
    @Test fun lightYearFormattingUsesSystemLocaleAndAvoidsConversionOverflow() {
        val previous=Locale.getDefault()
        try {
            Locale.setDefault(Locale.ITALY)
            assertEquals("446,5 ly",formatSpaceCompassCelestialCatalogDistance(
                spaceCompassCelestialCatalogDistanceAu(SpaceCompassCelestialBody.POLARIS,now),SpaceCompassNumericFormat.SYSTEM))
            Locale.setDefault(Locale.US)
            assertEquals("446.5 ly",formatSpaceCompassCelestialCatalogDistance(
                spaceCompassCelestialCatalogDistanceAu(SpaceCompassCelestialBody.POLARIS,now),SpaceCompassNumericFormat.SYSTEM))
        } finally { Locale.setDefault(previous) }
        val largest=formatSpaceCompassCelestialCatalogDistance(Double.MAX_VALUE,SpaceCompassNumericFormat.INTERNATIONAL)
        assertTrue(largest.endsWith(" ly"))
        assertFalse(largest.contains("Infinity"))
    }
    @Test fun mapsShareTheirDocumentedLongitudeSeamsInViewerAndMiniature() {
        assertEquals(0.5,SpaceCompassCelestialBody.IO.textureLongitudeOffset,0.0)
        assertEquals(0.0,SpaceCompassCelestialBody.EUROPA.textureLongitudeOffset,0.0)
        assertEquals(0.0,SpaceCompassCelestialBody.PLUTO.textureLongitudeOffset,0.0)
        assertTrue(SpaceCompassCelestialBody.EUROPA.textureHasUnmappedAreas)
        assertEquals("io.jpg",SpaceCompassCelestialBody.IO.viewerTexture)
        assertEquals("europa.jpg",SpaceCompassCelestialBody.EUROPA.viewerTexture)
    }
}
