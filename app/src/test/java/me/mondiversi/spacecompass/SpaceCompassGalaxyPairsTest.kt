package me.mondiversi.spacecompass

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import org.junit.Assert.*
import org.junit.Test

class SpaceCompassGalaxyPairsTest {
    private val now = Instant.parse("2026-10-17T12:30:00Z").toEpochMilli()
    private val pairs = listOf(
        SpaceCompassCelestialBody.ANDROMEDA_GALAXY to SpaceCompassCelestialBody.ANDROMEDA_CORE,
        SpaceCompassCelestialBody.M87_GALAXY to SpaceCompassCelestialBody.M87,
        SpaceCompassCelestialBody.MILKY_WAY_CENTER to SpaceCompassCelestialBody.SAGITTARIUS_A,
        SpaceCompassCelestialBody.M51 to SpaceCompassCelestialBody.M51_CORE)

    @Test fun galaxyAndNucleusHaveSeparateIdentitiesAndNoCopiedBlackHoleProperties() {
        for ((galaxy,nucleus) in pairs) {
            assertNotEquals(galaxy.nameResource,nucleus.nameResource)
            assertEquals(SpaceCompassCatalogType.GALAXY,galaxy.catalogType)
            assertEquals(SpaceCompassCatalogType.BLACK_HOLE,nucleus.catalogType)
            assertEquals(spaceCompassCelestialMembership(galaxy).galaxy,spaceCompassCelestialMembership(nucleus).galaxy)
            assertNull(spaceCompassCelestialFacts(galaxy).massSolar)
            assertNull(spaceCompassHorizonDiameterKm(galaxy))
            assertNotNull(spaceCompassCelestialFacts(nucleus).massSolar)
        }
    }

    @Test fun adoptedNuclearDirectionsTrackTogetherWithoutNetworkInBothHemispheres() {
        for ((galaxy,nucleus) in pairs) for (lat in listOf(45.0,-35.0)) for (hour in listOf(0,6,12)) {
            val time = now + hour * 3_600_000L
            val a = calculateSpaceCompassCelestialObservation(galaxy,time,lat,9.0)!!
            val b = calculateSpaceCompassCelestialObservation(nucleus,time,lat,9.0)!!
            assertTrue(spaceCompassAngularSeparation(a.position,b.position) < .001)
            assertEquals(a.distanceKm,b.distanceKm,a.distanceKm * 1e-12)
        }
    }

    @Test fun m51UsesPublishedNuclearAstrometryAndQualifiedMassInsteadOfAnInventedHorizonPhoto() {
        val body = SpaceCompassCelestialBody.M51_CORE
        val reference = body.deepSkyReference!!
        assertEquals(13.497975,reference.raHours,1e-9)
        assertEquals(47.19521944444444,reference.decDegrees,1e-9)
        assertEquals(31_000_000.0,reference.distanceLy,0.0)
        val facts = spaceCompassCelestialFacts(body)
        assertEquals(4e6,facts.massSolar!!,0.0)
        assertTrue(facts.massEstimated)
        assertNull(facts.massSolarError) // No misleading symmetric error for a logarithmic estimate.
        assertNull(body.viewerTexture)
        assertFalse(body.hasCatalogPhotograph)
        assertNull(spaceCompassAtmosphericPressure(body))
        assertTrue(spaceCompassCelestialTemperatures(body).isEmpty())
    }

    @Test fun newPhotographsAreExternalPinnedResourcesAndM31DoesNotDuplicateAnAsset() {
        for (body in listOf(SpaceCompassCelestialBody.ANDROMEDA_GALAXY,SpaceCompassCelestialBody.M87_GALAXY,
            SpaceCompassCelestialBody.MILKY_WAY_CENTER)) {
            assertTrue(body.hasCatalogPhotograph)
            val texture = SpaceCompassCelestialTexturePolicy.texture(body.viewerTexture)!!
            assertTrue(texture.name.endsWith(".webp"))
            assertEquals(2048,texture.width)
            assertEquals(1024,texture.height)
            assertNotNull(spaceCompassCatalogImageCredit(texture.name))
        }
        assertEquals("andromeda_galaxy.webp",SpaceCompassCelestialBody.ANDROMEDA_GALAXY.viewerTexture)
        assertNull(SpaceCompassCelestialBody.ANDROMEDA_CORE.viewerTexture)
        assertEquals("m87.webp",SpaceCompassCelestialBody.M87.viewerTexture)
        assertEquals("m87_galaxy.webp",SpaceCompassCelestialBody.M87_GALAXY.viewerTexture)
    }

    @Test fun coLocatedGalaxiesAndTheirOwnNucleiDoNotCreateFalseConjunctionEvents() {
        val day = LocalDate.parse("2026-10-17")
        for ((galaxy,nucleus) in pairs) {
            val paths = listOf(galaxy,nucleus).associateWith { body ->
                calculateSpaceCompassCelestialPath(body,day,ZoneId.of("UTC"),now,45.0,9.0,0.0,
                    SpaceCompassCelestialRemoteData())!!
            }
            val result = spaceCompassApproachPaths(paths, { body,time ->
                calculateSpaceCompassCelestialObservation(body,time,45.0,9.0)?.position })
            assertTrue(result.values.all { path -> path.markers.none { it.approach != null } })
            val positions = listOf(galaxy,nucleus).associateWith {
                calculateSpaceCompassCelestialObservation(it,now,45.0,9.0)!!.position }
            assertTrue(spaceCompassCurrentApproaches(now,positions).isEmpty())
            assertTrue(spaceCompassSharesGalacticNucleus(nucleus,galaxy))
        }
        val position = SpaceCompassSunPosition(120.0,30.0)
        assertEquals(1,spaceCompassCurrentApproaches(now,mapOf(
            SpaceCompassCelestialBody.SUN to position,SpaceCompassCelestialBody.MOON to position)).size)
    }

    @Test fun searchesFindGalaxiesSeparatelyFromTheirNucleiAndPreserveTheirScientificDesignations() {
        for ((query,body) in listOf("NGC224" to SpaceCompassCelestialBody.ANDROMEDA_GALAXY,
            "Galactic Centre" to SpaceCompassCelestialBody.MILKY_WAY_CENTER,
            "M51 buco nero" to SpaceCompassCelestialBody.M51_CORE,
            "M87 Galaxy" to SpaceCompassCelestialBody.M87_GALAXY)) {
            assertTrue(spaceCompassSearchCatalog(spaceCompassCelestialCatalogOrder,emptyMap(),query).contains(body))
            assertNotNull(body.scientificDesignation)
        }
    }
}
