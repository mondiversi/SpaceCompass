package me.mondiversi.spacecompass

import org.junit.Assert.*
import org.junit.Test
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.util.Locale

class SpaceCompassFictionalCatalogTest {
    private val now = Instant.parse("2026-10-27T12:00:00Z").toEpochMilli()
    @Test fun fictionHasAnExplicitIndependentFilterWithoutChangingDefaultChecks() {
        val rows = spaceCompassFilterCatalog(setOf(SpaceCompassCatalogType.SCIENCE_FICTION), SpaceCompassCatalogVisibility.ALL, emptyMap())
        assertEquals(11, rows.size)
        assertEquals(11, rows.distinct().size)
        assertTrue(SpaceCompassCelestialBody.LV_426 in rows)
        assertEquals(spaceCompassFictionalReferences.map { it.body }.toSet(), rows.toSet())
        assertEquals(setOf(SpaceCompassCelestialBody.SUN, SpaceCompassCelestialBody.MOON), SpaceCompassCelestialSelection().selected)
        assertEquals(setOf(SpaceCompassCelestialBody.LV_426, SpaceCompassCelestialBody.LV_223, SpaceCompassCelestialBody.PANDORA),
            spaceCompassFilterCatalog(setOf(SpaceCompassCatalogType.NATURAL_SATELLITE), SpaceCompassCatalogVisibility.ALL, emptyMap()).filter { it.isFictional }.toSet())
        assertEquals(11, spaceCompassCatalogTypeCounts(spaceCompassCelestialCatalogOrder, emptyMap(), Locale.ROOT).single { it.type == SpaceCompassCatalogType.SCIENCE_FICTION }.count)
    }
    @Test fun allReferencesPointOfflineAtMultipleDatesAndHemispheresWithoutInventingRange() {
        for (entry in spaceCompassFictionalReferences) for (latitude in listOf(-70.0, 0.0, 45.0, 80.0)) for (offset in listOf(0L, 200L*86_400_000)) {
            val actual = calculateSpaceCompassCelestialObservation(entry.body, now+offset, latitude, 9.0, 100.0)!!
            assertTrue(entry.body.name, actual.position.azimuthDegrees in 0.0..360.0)
            assertTrue(entry.body.name, actual.position.elevationDegrees in -90.0..90.0)
            assertTrue(actual.distanceKm.isNaN())
            assertNull(spaceCompassCelestialCatalogDistanceAu(entry.body, now+offset))
            assertNull(spaceCompassCelestialCatalogEarthDistanceKm(entry.body, now+offset))
            assertNull(spaceCompassCelestialCatalogReferenceDistanceAu(entry.body))
        }
    }
    @Test fun shipAndPlanetDirectionsFollowTheirReferencesRatherThanStaticScreenCoordinates() {
        for ((body, host) in listOf(SpaceCompassCelestialBody.ARRAKIS to SpaceCompassCelestialBody.CANOPUS,
            SpaceCompassCelestialBody.PANDORA to SpaceCompassCelestialBody.ALPHA_CENTAURI,
            SpaceCompassCelestialBody.DISCOVERY_ONE to SpaceCompassCelestialBody.JUPITER)) {
            for (offset in listOf(0L, 6*3_600_000L, 150*86_400_000L)) {
                val a=calculateSpaceCompassCelestialObservation(body, now+offset, 45.0, 9.0)!!
                val b=calculateSpaceCompassCelestialObservation(host, now+offset, 45.0, 9.0)!!
                assertEquals(0.0, spaceCompassAngularSeparation(a.position,b.position), 1e-9)
            }
        }
    }
    @Test fun dailyPathsExistAndUseTheSamePointingReference() {
        for (body in listOf(SpaceCompassCelestialBody.ARRAKIS, SpaceCompassCelestialBody.DISCOVERY_ONE)) {
            val path=calculateSpaceCompassCelestialPath(body, LocalDate.of(2026,10,27), ZoneId.of("Europe/Rome"), now, 45.0,9.0,0.0,SpaceCompassCelestialRemoteData())
            assertNotNull(path)
            assertTrue(path!!.samples.size > 24)
            assertTrue(path.markers.isNotEmpty())
            for (point in path.markers) {
                val exact=calculateSpaceCompassCelestialObservation(body,point.timeMs,45.0,9.0)!!
                assertEquals(0.0,spaceCompassAngularSeparation(point.position,exact.position),1e-8)
            }
        }
    }
    @Test fun fictionNeverCreatesFalseConjunctionsWithItsHostOrAnotherFictionalReference() {
        val direction=SpaceCompassSunPosition(100.0,30.0)
        val positions=(spaceCompassFictionalReferences.map { it.body }+SpaceCompassCelestialBody.JUPITER).associateWith { direction }
        assertTrue(spaceCompassApproachCandidates(positions).isEmpty())
        assertTrue(spaceCompassCurrentApproaches(now,positions).isEmpty())
    }
    @Test fun unknownPhysicalFactsStayAbsentAndSearchAcceptsInternationalAndLocalizedNames() {
        for (body in spaceCompassNewFictionalBodies) {
            assertEquals(SpaceCompassCelestialFacts(),spaceCompassCelestialFacts(body))
            assertNull(body.scientificDesignation)
            assertNotNull(body.scientificTypeResource)
            assertNotNull(SpaceCompassCelestialTexturePolicy.texture(body.viewerTexture))
            assertNotNull(SpaceCompassCelestialThumbnailPolicy.forImage(body.viewerTexture))
        }
        assertEquals(listOf(SpaceCompassCelestialBody.VULCAN),spaceCompassSearchCatalog(spaceCompassNewFictionalBodies,emptyMap(),"Vulcano"))
        assertEquals(listOf(SpaceCompassCelestialBody.ARRAKIS),spaceCompassSearchCatalog(spaceCompassNewFictionalBodies,emptyMap(),"Dune").filter { it==SpaceCompassCelestialBody.ARRAKIS })
        assertTrue(SpaceCompassCelestialBody.NOSTROMO.hasCatalogPhotograph)
        assertFalse(SpaceCompassCelestialBody.CALADAN.hasCatalogPhotograph)
    }

    @Test fun orbitalExportsDiscloseNarrativeReferencesWithoutChangingOrdinaryTables() {
        val zone=ZoneId.of("Europe/Rome")
        val date=LocalDate.of(2026,10,27)
        for (body in listOf(SpaceCompassCelestialBody.DISCOVERY_ONE,SpaceCompassCelestialBody.SUN)) {
            val point=SpaceCompassSunPathPoint(now,SpaceCompassSunPosition(100.0,30.0))
            val path=SpaceCompassSunDailyPath(date,zone,listOf(point),listOf(point),body)
            val snapshot=spaceCompassSingleOrbitExportSnapshot(path,null,emptyList(),now)
            val table=spaceCompassSingleOrbitExportTable(snapshot,path,null)
            val csv=renderSpaceCompassOrbitCsv(table,snapshot,now,"")
            val txt=renderSpaceCompassOrbitTxt(table,snapshot,now,"")
            if (body.isFictional) {
                assertTrue(csv.contains("not a measured orbit"))
                assertTrue(txt.contains("Jupiter · Solar System"))
                assertTrue(csv.contains("Direction reference"))
            } else {
                assertFalse(csv.contains("Fictional"))
                assertFalse(txt.contains("Fictional"))
            }
            assertFalse(txt.startsWith("["))
        }
    }
}
