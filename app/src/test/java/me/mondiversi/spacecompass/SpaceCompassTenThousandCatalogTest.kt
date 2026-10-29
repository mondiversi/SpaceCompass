package me.mondiversi.spacecompass

import org.junit.Assert.*
import org.junit.Test
import kotlinx.coroutines.runBlocking
import java.io.*
import java.time.Instant

class SpaceCompassTenThousandCatalogTest {
    private val moment = Instant.parse("2026-10-29T12:00:00Z").toEpochMilli()
    private val added get() = spaceCompassLargeCatalogEntries.filter { it.body.ordinal >= 1010 }

    @Test fun releasedIdentitiesAndOrdinalsRemainUnchangedAndAllNewNamesResolveUniquely() {
        val lines = javaClass.classLoader!!.getResourceAsStream("catalog-9999/released-identities.tsv")!!
            .bufferedReader().readLines()
        assertEquals(1010,lines.size)
        lines.forEach { line ->
            val (index,name) = line.split('\t')
            val body = SpaceCompassCelestialBody.valueOf(name)
            assertEquals(index.toInt(),body.ordinal)
            assertSame(body,SpaceCompassCelestialBody.entries[index.toInt()])
        }
        assertEquals(10000,SpaceCompassCelestialBody.entries.size)
        assertEquals(9999,spaceCompassCelestialCatalogOrder.size)
        assertEquals(8990,added.size)
        SpaceCompassCelestialBody.entries.forEach { assertSame(it,SpaceCompassCelestialBody.valueOf(it.name)) }
        assertFalse(SpaceCompassCelestialBody.EARTH_CENTER in spaceCompassCelestialCatalogOrder)
        assertEquals(setOf(SpaceCompassCelestialBody.SUN,SpaceCompassCelestialBody.MOON),SpaceCompassCelestialSelection().selected)
    }

    @Test fun savedIdentitySerializationRestoresTheRegistrySingleton() {
        for (body in listOf(SpaceCompassCelestialBody.SUN,SpaceCompassCelestialBody.LV_426,added.last().body)) {
            val output = ByteArrayOutputStream()
            ObjectOutputStream(output).use { it.writeObject(body) }
            val restored = ObjectInputStream(ByteArrayInputStream(output.toByteArray())).use { it.readObject() }
            assertSame(body,restored)
        }
    }

    @Test fun missingAstrometrySentinelsAreRejectedAndResolvedNucleusUsesItsRealHostDirection() {
        assertThrows(IllegalArgumentException::class.java) { SpaceCompassDeepSkyReference(-82.0,-1234.0,1000.0) }
        assertThrows(IllegalArgumentException::class.java) { SpaceCompassDeepSkyReference(12.0,-1234.0,1000.0) }
        assertThrows(IllegalArgumentException::class.java) { SpaceCompassDeepSkyReference(12.0,20.0,Double.NaN) }
        assertFalse(SpaceCompassDeepSkyReference(12.0,20.0,Double.NaN,hasMeasuredDistance=false).hasMeasuredDistance)
        val reference = SpaceCompassCelestialBody.valueOf("NUCLEUS_NGC_4395").deepSkyReference!!
        assertTrue(reference.raHours in 12.42..12.45)
        assertTrue(reference.decDegrees in 33.0..34.0)
    }

    @Test fun locationWithEveryObjectAndItsActiveObjectFitsAndRoundTrips() {
        val all = spaceCompassCelestialCatalogOrder.toSet()
        val active = added.last().body
        val selection = SpaceCompassCelestialSelection(all,active)
        val location = SpaceCompassScenarioArchive("Complete catalogue",SpaceCompassObserverPlan(
            28.0,-14.0,100.0,moment,"Atlantic/Canary"),SpaceCompassScenarioTimeSettings(),selection)
        val encoded = location.encode()
        assertTrue(encoded.toByteArray().size > 131072)
        assertTrue(encoded.toByteArray().size < SPACE_COMPASS_SCENARIO_FILE_LIMIT)
        assertEquals(selection,SpaceCompassScenarioArchive.decode(encoded).celestialSelection)
    }

    @Test fun batchPointingPreservesPatternsParallaxAndObserverChanges() = runBlocking {
        val bodies = (added.filter { it.entry.reference != null }.filterIndexed { i,_ -> i%100==0 }.map { it.body } +
            spaceCompassCelestialCatalogOrder.filter { it.constellationPattern != null } +
            listOf(SpaceCompassCelestialBody.SUN,SpaceCompassCelestialBody.MOON,SpaceCompassCelestialBody.PROXIMA_CENTAURI)).distinct()
        for (latitude in listOf(-45.0,0.0,70.0)) {
            val batch = spaceCompassCatalogObservations(bodies,moment,latitude,9.0,100.0,SpaceCompassCelestialRemoteData())
            for (body in bodies) {
                val expected = calculateSpaceCompassCelestialObservation(body,moment,latitude,9.0,100.0)!!
                val actual = batch.getValue(body)!!
                assertEquals(body.name,expected.position.azimuthDegrees,actual.position.azimuthDegrees,1e-10)
                assertEquals(body.name,expected.position.elevationDegrees,actual.position.elevationDegrees,1e-10)
                if (expected.distanceKm.isNaN()) assertTrue(actual.distanceKm.isNaN())
                else assertEquals(body.name,expected.distanceKm,actual.distanceKm,1e-6)
            }
        }
    }

    @Test fun batchedDistancesKeepDirectionOnlyObjectsUnknownAndSeparateEarthFromSun() = runBlocking {
        val bodies = added.filter { it.entry.reference != null }.filterIndexed { index,_ -> index%50==0 }.map { it.body }
        val snapshot = spaceCompassCatalogDistances(bodies,moment,SpaceCompassCelestialRemoteData())
        for (body in bodies) {
            val solar = spaceCompassCatalogDisplaySolarDistanceAu(body,moment)
            val earth = spaceCompassCatalogDisplayEarthDistanceKm(body,moment)
            if (solar == null) assertNull(snapshot.solar[body]) else assertEquals(solar,snapshot.solar.getValue(body)!!,1e-8)
            if (earth == null) assertNull(snapshot.earth[body]) else assertEquals(earth,snapshot.earth.getValue(body)!!,maxOf(.001,earth*1e-14))
        }
    }

    @Test fun automaticDownloadsAreLimitedToSelectedObservedAndVisibleObjects() {
        val selected = setOf(SpaceCompassCelestialBody.SUN,SpaceCompassCelestialBody.MOON,SpaceCompassCelestialBody.SEDNA)
        val visible = added.filter { it.entry.target != null }.take(5).map { it.body }.toSet()
        val queue = spaceCompassCelestialRefreshBodies(selected,selected,true,visible)
        assertEquals(visible + SpaceCompassCelestialBody.SEDNA,queue.toSet())
        assertEquals(SpaceCompassCelestialBody.SEDNA,queue.first())
        assertEquals(listOf(SpaceCompassCelestialBody.SEDNA),spaceCompassCelestialRefreshBodies(selected,selected,true,emptySet()))
    }

    @Test fun allNewSatellitesPassChecksumIdentityEnvelopeAndStaleDataChecks() {
        for (row in added.filter { it.satellite != null }) {
            val text = javaClass.classLoader!!.getResourceAsStream("catalog-9999/${row.body.name.lowercase()}.tle")!!
                .bufferedReader().readText()
            val orbit = SpaceCompassCatalogSatelliteOrbit.parse(row.body,text)
            assertTrue(orbit.usable(orbit.epochMs))
            assertFalse(orbit.usable(orbit.epochMs+40*86400000L))
            val position = orbit.observe(orbit.epochMs,45.0,9.0,100.0)
            assertTrue(row.body.name,position.position.elevationDegrees in -90.0..90.0)
            assertTrue(row.body.name,orbit.speedKmPerSecond(orbit.epochMs) in row.satellite!!.speedKmSecond)
        }
    }

    @Test fun newPhotographsAndTheirPreviewsUseThePublishedExternalPack() {
        val pending = SpaceCompassCelestialTexturePolicy.images.filter { it.downloadPack == "celestial-textures-v1.8" }
        assertEquals(5,pending.size)
        for (image in pending) {
            assertEquals("https://github.com/mondiversi/SpaceCompass/releases/download/celestial-textures-v1.8/"+image.name,image.url)
            val thumbnail = SpaceCompassCelestialThumbnailPolicy.forImage(image.name)!!
            assertEquals("celestial-textures-v1.8",thumbnail.downloadPack)
            assertNotNull(thumbnail.url)
            assertEquals("thumb_"+image.name,thumbnail.name)
            assertEquals(128,thumbnail.width);assertEquals(128,thumbnail.height)
        }
    }

    @Test fun everyNewHorizonsTargetParsesItsActualVerifiedVectorsAndRefusesExtrapolation() {
        val expected = added.filter { it.entry.target != null }.map { it.body }.toSet()
        val seen = mutableSetOf<SpaceCompassCelestialBody>()
        val stream = javaClass.classLoader!!.getResourceAsStream("catalog-9999/horizons-vectors.tsv.gz")!!
        java.util.zip.GZIPInputStream(stream).bufferedReader().useLines { lines ->
            lines.forEach { line ->
                val columns = line.split('\t',limit=2)
                val body = SpaceCompassCelestialBody.valueOf(columns[0])
                val text = String(java.util.Base64.getDecoder().decode(columns[1]),Charsets.UTF_8)
                assertTrue(seen.add(body))
                val ephemeris = parseSpaceCompassHorizonsEphemeris(body,text)
                assertEquals(body.name,49,ephemeris.samples.size)
                val time = ephemeris.samples[24].timeMs
                val remote = SpaceCompassCelestialRemoteData(ephemerides=mapOf(body to ephemeris))
                for (latitude in listOf(-45.0,45.0)) {
                    val observation = calculateSpaceCompassCelestialObservation(body,time,latitude,9.0,100.0,remote)!!
                    assertTrue(body.name,observation.position.azimuthDegrees in 0.0..360.0)
                    assertTrue(body.name,observation.position.elevationDegrees in -90.0..90.0)
                    assertTrue(body.name,observation.distanceKm.isFinite() && observation.distanceKm>0)
                }
                assertNull(ephemeris.at(ephemeris.samples.first().timeMs-1))
                assertNull(ephemeris.at(ephemeris.samples.last().timeMs+1))
                if (body.catalogType==SpaceCompassCatalogType.NATURAL_SATELLITE)
                    assertEquals(spaceCompassCelestialFacts(body).parent,body.horizonsMotionPrimary)
            }
        }
        assertEquals(1414,seen.size)
        assertEquals(expected,seen)
    }
}
