package me.mondiversi.spacecompass

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.util.Locale
import org.junit.Assert.*
import org.junit.Test

class SpaceCompassHundredCatalogTest {
    private val now = Instant.parse("2026-10-17T12:30:00Z").toEpochMilli()
    private val additions = SpaceCompassCelestialBody.entries.filter { it.additionalCatalogEntry != null && it.largeCatalogEntry == null }
    private fun fixture(name: String) = javaClass.classLoader!!.getResource("catalog-100/$name")!!.readText()
    private fun remote(body: SpaceCompassCelestialBody): SpaceCompassCelestialRemoteData {
        val name = body.name.lowercase(Locale.ROOT)
        return SpaceCompassCelestialRemoteData(
            ephemerides = mapOf(body to parseSpaceCompassHorizonsEphemeris(body, fixture("$name-pointing.txt"))),
            motions = mapOf(body to parseSpaceCompassHorizonsMotion(body, fixture("$name-motion.txt"))))
    }

    @Test fun catalogueExtensionsKeepUniqueOrdinaryObjectsAndStableSelectionIdentifiers() {
        assertEquals(51, additions.size)
        assertEquals(1009, spaceCompassCelestialCatalogOrder.size)
        assertEquals(1009, spaceCompassCelestialCatalogOrder.distinct().size)
        assertTrue(SpaceCompassCelestialBody.LV_426 in spaceCompassCelestialCatalogOrder)
        assertFalse(SpaceCompassCelestialBody.EARTH_CENTER in spaceCompassCelestialCatalogOrder)
        for (body in additions) {
            assertEquals(1, spaceCompassCelestialCatalogOrder.count { it == body })
            assertNotNull(body.scientificDesignation)
            assertNotNull(body.scientificTypeResource)
            assertTrue(body.supportsDailyPath)
            assertTrue(body.usesHorizons xor (body.deepSkyReference != null))
            assertEquals(1f, spaceCompassCelestialPathTint(body).alpha, 0f)
            spaceCompassCelestialFacts(body)
        }
        assertEquals(setOf(SpaceCompassCelestialBody.SUN, SpaceCompassCelestialBody.MOON),SpaceCompassCelestialSelection().selected)
    }

    @Test fun allTwentyTwoMovingTargetsMatchIndependentTopocentricJplObserverTables() {
        val expected = mapOf(
            SpaceCompassCelestialBody.GANYMEDE to listOf(271.960198, 19.017409, 5.71682404911527),
            SpaceCompassCelestialBody.CALLISTO to listOf(271.852607, 19.066934, 5.71378586068497),
            SpaceCompassCelestialBody.PHOBOS to listOf(282.592625, 14.034073, 1.5461724413224),
            SpaceCompassCelestialBody.DEIMOS to listOf(282.591409, 14.036306, 1.54623781427061),
            SpaceCompassCelestialBody.RHEA to listOf(40.585449, -35.428386, 8.45927813147101),
            SpaceCompassCelestialBody.IAPETUS to listOf(40.648108, -35.448519, 8.48071086753508),
            SpaceCompassCelestialBody.TITANIA to listOf(339.226924, -21.348048, 18.6743137233686),
            SpaceCompassCelestialBody.MIRANDA to listOf(339.234263, -21.340041, 18.6749990197978),
            SpaceCompassCelestialBody.TRITON to listOf(49.83524, -33.220123, 28.9505117435066),
            SpaceCompassCelestialBody.CHARON to listOf(111.139265, -12.401267, 35.4541494861228),
            SpaceCompassCelestialBody.ERIS to listOf(20.952709, -43.007692, 94.4741137777885),
            SpaceCompassCelestialBody.HAUMEA to listOf(183.520887, 58.346689, 50.6186046095476),
            SpaceCompassCelestialBody.MAKEMAKE to listOf(220.142441, 59.475247, 53.6054595919618),
            SpaceCompassCelestialBody.VESTA to listOf(29.757872, -44.76699, 1.48990547988695),
            SpaceCompassCelestialBody.BENNU to listOf(175.926339, 23.860369, 2.17850956975285),
            SpaceCompassCelestialBody.RYUGU to listOf(160.264508, 23.070969, 2.03017253485994),
            SpaceCompassCelestialBody.HALE_BOPP to listOf(172.932939, -45.869594, 51.3138281926984),
            SpaceCompassCelestialBody.ENCKE to listOf(39.120138, -17.459638, 1.01402741818537),
            SpaceCompassCelestialBody.BEPICOLOMBO to listOf(177.376573, 24.588927, 0.90986102693603),
            SpaceCompassCelestialBody.SOLAR_ORBITER to listOf(272.159823, 34.265877, 0.72539635624324),
            SpaceCompassCelestialBody.LUCY to listOf(185.105457, 27.578769, 6.07280167357999),
            SpaceCompassCelestialBody.PSYCHE to listOf(277.956662, 12.65223, 1.81020561674473))
        for ((body, value) in expected) {
            val data = remote(body)
            val actual = calculateSpaceCompassCelestialObservation(body, now, 45.0, 9.0, 100.0, data)!!
            assertEquals(body.name, value[0], actual.position.azimuthDegrees, .02)
            assertEquals(body.name, value[1], actual.position.elevationDegrees, .02)
            assertEquals(body.name, value[2] * SPACE_COMPASS_AU_KM, actual.distanceKm, 25.0)
            assertTrue(spaceCompassCelestialCatalogEarthDistanceKm(body, now, data)!! > 0)
            assertTrue(spaceCompassCelestialCatalogDistanceAu(body, now, data)!! > 0)
            assertNotNull(calculateSpaceCompassCelestialCoordinates(body, now, data))
            assertNotNull(calculateSpaceCompassCelestialSpeed(body, now, data))
        }
    }

    @Test fun allMovingPathsHaveRealMotionAndNeverReuseStaleOrMissingEphemerides() {
        for (body in additions.filter { it.usesHorizons }) {
            val data = remote(body)
            val path = calculateSpaceCompassCelestialPath(body,LocalDate.parse("2026-10-17"),ZoneId.of("UTC"),
                now,45.0,9.0,100.0,data)!!
            assertEquals(body.name,24,path.markers.count { it.event == SpaceCompassSunPathEvent.HOUR })
            assertTrue(body.name,path.samples.map { it.position }.distinct().size > 50)
            assertNull(calculateSpaceCompassCelestialObservation(body,now,45.0,9.0))
            val outside = data.ephemerides.getValue(body).samples.last().timeMs + 1
            assertNull(calculateSpaceCompassCelestialObservation(body,outside,45.0,9.0,100.0,data))
            assertNull(spaceCompassCelestialCatalogEarthDistanceKm(body,outside,data))
            assertTrue(body in spaceCompassCelestialRefreshBodies(emptySet(),emptySet(),true))
        }
    }

    @Test fun distantDirectionsWorkOfflineAtMultipleTimesAndBothHemispheres() {
        for (body in additions.filter { it.deepSkyReference != null }) {
            assertTrue(body.isExtrasolar)
            assertFalse(body.usesHorizons)
            assertTrue(body.deepSkyReference!!.distanceLy > 0)
            for (lat in listOf(45.0,-35.0)) {
                val a = calculateSpaceCompassCelestialObservation(body,now,lat,9.0)!!
                val b = calculateSpaceCompassCelestialObservation(body,now+6*3600000,lat,9.0)!!
                assertTrue(a.position.azimuthDegrees in 0.0..360.0)
                assertTrue(a.position.elevationDegrees in -90.0..90.0)
                assertNotEquals(body.name,a.position,b.position)
                assertTrue(a.distanceKm > SPACE_COMPASS_LIGHT_YEAR_KM)
                val path = calculateSpaceCompassCelestialPath(body,LocalDate.parse("2026-10-17"),ZoneId.of("UTC"),
                    now,lat,9.0,100.0,SpaceCompassCelestialRemoteData())!!
                assertEquals(24,path.markers.count { it.event == SpaceCompassSunPathEvent.HOUR })
            }
        }
    }

    @Test fun moonsHavePrimaryRelativeSpeedsAndTheCorrectParents() {
        val ranges = mapOf(
            SpaceCompassCelestialBody.GANYMEDE to 10.0..12.0, SpaceCompassCelestialBody.CALLISTO to 7.0..9.0,
            SpaceCompassCelestialBody.PHOBOS to 1.9..2.3, SpaceCompassCelestialBody.DEIMOS to 1.2..1.5,
            SpaceCompassCelestialBody.RHEA to 8.0..9.0, SpaceCompassCelestialBody.IAPETUS to 3.0..4.0,
            SpaceCompassCelestialBody.TITANIA to 3.0..4.0, SpaceCompassCelestialBody.MIRANDA to 6.0..7.0,
            SpaceCompassCelestialBody.TRITON to 4.0..5.0, SpaceCompassCelestialBody.CHARON to .1..0.4)
        for ((body,range) in ranges) {
            assertNotNull(spaceCompassCelestialFacts(body).parent)
            assertTrue(body.name,calculateSpaceCompassCelestialSpeed(body,now,remote(body))!! in range)
            assertNotEquals(R.string.celestial_speed_orbit_sun,spaceCompassCelestialSpeedLabel(body))
        }
    }

    @Test fun extendedObjectsAndExoplanetsDoNotInventPhysicalOrResolvedOrbitalData() {
        assertEquals(100,spaceCompassFilterCatalog(setOf(SpaceCompassCatalogType.GALAXY),SpaceCompassCatalogVisibility.ALL,emptyMap()).size)
        assertEquals(SpaceCompassCelestialGalaxy.LMC,spaceCompassCelestialMembership(SpaceCompassCelestialBody.LARGE_MAGELLANIC_CLOUD).galaxy)
        assertEquals(SpaceCompassCelestialGalaxy.SMC,spaceCompassCelestialMembership(SpaceCompassCelestialBody.SMALL_MAGELLANIC_CLOUD).galaxy)
        assertEquals(SpaceCompassCelestialGalaxy.M51,spaceCompassCelestialMembership(SpaceCompassCelestialBody.M51).galaxy)
        for (body in additions.filter { it.isExtendedSkyObject }) {
            assertNull(spaceCompassCelestialFacts(body).massKg)
            assertNull(spaceCompassCelestialFacts(body).diameterKm)
            assertNull(spaceCompassAtmosphericPressure(body))
            assertTrue(spaceCompassCelestialTemperatures(body).isEmpty())
        }
        for (body in listOf(SpaceCompassCelestialBody.TOI_700_D,SpaceCompassCelestialBody.KEPLER_186_F)) {
            assertEquals(SpaceCompassCatalogType.EXOPLANET,body.catalogType)
            assertEquals(R.string.celestial_unresolved_exoplanet_note,body.deepSkyNoteResource)
            assertNull(spaceCompassCelestialFacts(body).massKg)
            assertNotNull(spaceCompassCelestialFacts(body).diameterKm)
            assertNull(spaceCompassAtmosphericPressure(body))
        }
    }

    @Test fun fastMoonSpeedsMatchIndependentParentCenteredGeometricJplStatesAtAHalfHour() {
        val expected = mapOf(
            SpaceCompassCelestialBody.GANYMEDE to 10.852696487108037,
            SpaceCompassCelestialBody.CALLISTO to 8.150878783361149,
            SpaceCompassCelestialBody.PHOBOS to 2.1064256744263616,
            SpaceCompassCelestialBody.DEIMOS to 1.3513796781308516,
            SpaceCompassCelestialBody.RHEA to 8.487904466216039,
            SpaceCompassCelestialBody.IAPETUS to 3.3508923941494126,
            SpaceCompassCelestialBody.TITANIA to 3.6510371604213288,
            SpaceCompassCelestialBody.MIRANDA to 6.688086248757091,
            SpaceCompassCelestialBody.TRITON to 4.390359970331317,
            SpaceCompassCelestialBody.CHARON to 0.22307379519087875)
        for ((body,value) in expected)
            assertEquals(body.name,value,calculateSpaceCompassCelestialSpeed(body,now,remote(body))!!,.005)
    }

    @Test fun newNamesAndScientificDesignationsAreSearchableAcrossLanguages() {
        for ((query,body) in listOf("Ganimede" to SpaceCompassCelestialBody.GANYMEDE,
            "NGC6205" to SpaceCompassCelestialBody.M13,"NebulosaAnello" to SpaceCompassCelestialBody.M57,
            "南极座σ" to SpaceCompassCelestialBody.SIGMA_OCTANTIS))
            assertEquals(query,listOf(body),spaceCompassSearchCatalog(spaceCompassCelestialCatalogOrder,emptyMap(),query))
    }

    @Test fun parentStatesCannotBeMislabeledAsSunCenteredMotionOrHeliocentricDistance() {
        val body=SpaceCompassCelestialBody.CHARON
        val text=fixture("charon-motion.txt")
        val motion=parseSpaceCompassHorizonsMotion(body,text)
        assertEquals(SpaceCompassCelestialBody.PLUTO,motion.primary)
        assertThrows(IllegalArgumentException::class.java) {
            parseSpaceCompassHorizonsMotion(body,text.replace("Center body name: Pluto (999)","Center body name: Sun (10)"))
        }
        assertThrows(IllegalArgumentException::class.java) { motion.copy(primary=null) }
        assertNull(spaceCompassCelestialCatalogDistanceAu(body,now,SpaceCompassCelestialRemoteData(motions=mapOf(body to motion))))
    }

    @Test fun publicMoonRequestsUseCorrectOriginsWithoutObserverCoordinates() {
        val body=SpaceCompassCelestialBody.GANYMEDE
        val pointing=java.net.URLDecoder.decode(spaceCompassHorizonsUrl(body,now),"UTF-8")
        val motion=java.net.URLDecoder.decode(spaceCompassHorizonsUrl(body,now,true),"UTF-8")
        assertTrue(pointing.contains("CENTER='500@399'"))
        assertTrue(motion.contains("CENTER='500@599'"))
        assertFalse(pointing.contains("SITE_COORD"))
        assertFalse(motion.contains("SITE_COORD"))
    }

    @Test fun riseAndSetShareMirroredSemicirclesWithAFlatHorizonDiameter() {
        val up=spaceCompassHorizonEventGlyph(SpaceCompassSunPathEvent.SUNRISE,10f,20f,6f)
        val down=spaceCompassHorizonEventGlyph(SpaceCompassSunPathEvent.SUNSET,10f,20f,6f)
        assertEquals(25,up.size)
        assertEquals(up.first().second,up.last().second,.0001f)
        assertEquals(23f,up.first().second,.0001f)
        assertEquals(17f,up.minOf { it.second },.0001f)
        assertEquals(23f,down.maxOf { it.second },.0001f)
        up.zip(down).forEach { (a,b) ->
            assertEquals(a.first,b.first,.0001f)
            assertEquals(40f,a.second+b.second,.0001f)
        }
    }
}
