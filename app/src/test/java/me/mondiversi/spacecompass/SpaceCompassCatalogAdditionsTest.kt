package me.mondiversi.spacecompass

import io.github.cosinekitty.astronomy.Body
import io.github.cosinekitty.astronomy.helioState
import java.io.File
import java.net.URLDecoder
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import javax.xml.parsers.DocumentBuilderFactory
import org.junit.Assert.*
import org.junit.Test

/** New references preserve pointing, physical meaning and all existing catalogue preferences. */
class SpaceCompassCatalogAdditionsTest {
    private val now = Instant.parse("2026-10-08T12:00:00Z").toEpochMilli()
    private val titan = SpaceCompassCelestialBody.TITAN
    private val distant = listOf(SpaceCompassCelestialBody.SIRIUS, SpaceCompassCelestialBody.BETELGEUSE,
        SpaceCompassCelestialBody.ORION_NEBULA, SpaceCompassCelestialBody.PLEIADES)
    private fun fixture(name: String) = requireNotNull(javaClass.getResource("/catalog-additions/$name.txt")).readText()
    private fun remote() = SpaceCompassCelestialRemoteData(
        ephemerides = mapOf(titan to parseSpaceCompassHorizonsEphemeris(titan, fixture("titan-pointing"))),
        motions = mapOf(titan to parseSpaceCompassHorizonsMotion(titan, fixture("titan-motion"))))

    @Test fun actualJplTitanResponsesSupportPointingFullDailyPathsAndAnIlluminatedFace() {
        val data = remote()
        assertEquals(73, data.ephemerides.getValue(titan).samples.size)
        assertEquals(73, data.motions.getValue(titan).samples.size)
        for (latitude in listOf(-90.0, -33.0, 45.0, 90.0)) {
            val observation = requireNotNull(calculateSpaceCompassCelestialObservation(titan, now, latitude, 9.0, 50.0, data))
            assertTrue(observation.distanceKm / SPACE_COMPASS_AU_KM in 8.0..12.0)
            assertTrue(observation.position.azimuthDegrees in 0.0..360.0)
            assertTrue(observation.position.elevationDegrees in -90.0..90.0)
            val view = requireNotNull(calculateSpaceCompassCelestialViewGeometry(titan, now, latitude, 9.0, 50.0, data))
            assertEquals(observation.distanceKm, view.distanceKm!!, .001)
            assertEquals(1.0, view.bodyX.cross(view.bodyY).dot(view.bodyZ), 1e-9)
            assertTrue(view.illuminatedFraction!! in .9..1.0)
            val path = requireNotNull(calculateSpaceCompassCelestialPath(titan, LocalDate.parse("2026-10-08"),
                ZoneId.of("Europe/Rome"), now, latitude, 9.0, 50.0, data))
            assertTrue(path.samples.size > 400)
            assertEquals(24, path.markers.count { it.event == SpaceCompassSunPathEvent.HOUR })
            assertEquals(1, path.markers.count { SpaceCompassSunPathEvent.MINIMUM in it.events })
        }
    }

    @Test fun titanSpeedIsAboutSaturnRatherThanItsLargerHeliocentricMotion() {
        val data = remote()
        val speed = requireNotNull(calculateSpaceCompassCelestialSpeed(titan, now, data))
        assertTrue("Titan's orbital speed $speed", speed in 4.0..7.0)
        val sample = data.motions.getValue(titan).samples.single { kotlin.math.abs(it.timeMs-now) < 100 }
        val saturn = helioState(Body.Saturn, spaceCompassAstronomyTime(now))
        val expected = spaceCompassCelestialVelocityMagnitude(sample.vx-saturn.vx, sample.vy-saturn.vy,
            sample.vz-saturn.vz, SPACE_COMPASS_AU_DAY_TO_KM_SECOND)
        assertEquals(expected, speed, 1e-5)
        assertEquals(R.string.celestial_speed_orbit_saturn, spaceCompassCelestialSpeedLabel(titan))
        assertNull(calculateSpaceCompassCelestialSpeed(titan, now-5*86_400_000L, data))
        assertNull(calculateSpaceCompassCelestialSpeed(titan, now))
    }

    @Test fun titanCannotReuseGeometricMotionAsPointingOrAnUnrelatedTarget() {
        assertThrows(IllegalArgumentException::class.java) { parseSpaceCompassHorizonsEphemeris(titan, fixture("titan-motion")) }
        assertThrows(IllegalArgumentException::class.java) { parseSpaceCompassHorizonsMotion(titan, fixture("titan-pointing")) }
        assertThrows(IllegalArgumentException::class.java) {
            parseSpaceCompassHorizonsEphemeris(SpaceCompassCelestialBody.SEDNA, fixture("titan-pointing"))
        }
        for (geometric in listOf(false, true)) {
            val url = URLDecoder.decode(spaceCompassHorizonsUrl(titan, now, geometric), "UTF-8")
            assertTrue(url.contains("COMMAND='606'"))
            assertTrue(url.contains(if (geometric) "CENTER='500@10'" else "CENTER='500@399'"))
            assertFalse(url.contains("SITE_COORD"))
        }
        assertNull(calculateSpaceCompassCelestialObservation(titan, now, 45.0, 9.0))
        assertNull(calculateSpaceCompassCelestialViewGeometry(titan, now, 45.0, 9.0))
    }

    @Test fun titanSurfaceFactsAndAxisUseTheReviewedReferenceData() {
        val facts = spaceCompassCelestialFacts(titan)
        assertEquals(5149.52, facts.diameterKm!!, 1e-9)
        assertEquals(1881.4, facts.density!!, 1e-9)
        assertEquals(SpaceCompassCelestialBody.SATURN, facts.parent)
        assertEquals(15.945448, facts.revolutionDays!!, 1e-9)
        assertEquals(146700.0, spaceCompassAtmosphericPressure(titan)!!.pascals, 1e-9)
        assertEquals(93.7, spaceCompassCelestialTemperatures(titan).single().celsius+273.15, 1e-9)
        assertEquals(2005, spaceCompassCelestialTemperatures(titan).single().epochYear)
        val axis = spaceCompassTitanAxis(spaceCompassAstronomyTime(now))
        assertEquals(39.4827/15, axis.ra, 1e-10)
        assertEquals(83.4279, axis.dec, 1e-10)
        assertEquals(0.0, titan.textureLongitudeOffset, 0.0)
    }

    @Test fun allNewDistantObjectsHaveOfflineDistancesAndActualDailySkyMotion() {
        for (body in distant) {
            assertTrue(body.isExtrasolar && body.hasCatalogPhotograph && body.supportsDailyPath)
            val distance = requireNotNull(spaceCompassCelestialCatalogDistanceAu(body, now))
            assertTrue(distance * SPACE_COMPASS_AU_KM / SPACE_COMPASS_LIGHT_YEAR_KM > 8)
            val path = requireNotNull(calculateSpaceCompassCelestialPath(body, LocalDate.parse("2026-10-08"),
                ZoneId.of("Europe/Rome"), now, 45.0, 9.0, 0.0, SpaceCompassCelestialRemoteData()))
            assertEquals(24, path.markers.count { it.event == SpaceCompassSunPathEvent.HOUR })
            assertTrue(path.samples.maxOf { it.position.elevationDegrees }-path.samples.minOf { it.position.elevationDegrees } > 30)
            assertNull(calculateSpaceCompassCelestialSpeed(body, now))
        }
    }

    @Test fun extendedObjectsAreFilterableWithoutInventedPlanetarySurfaceData() {
        for ((body, type) in listOf(SpaceCompassCelestialBody.ORION_NEBULA to SpaceCompassCatalogType.NEBULA,
            SpaceCompassCelestialBody.PLEIADES to SpaceCompassCatalogType.STAR_CLUSTER)) {
            assertEquals(listOf(body), spaceCompassFilterCatalog(setOf(type), SpaceCompassCatalogVisibility.ALL, emptyMap()))
            val facts = spaceCompassCelestialFacts(body)
            assertNotNull(facts.apparentMagnitude)
            assertNull(facts.massKg); assertNull(facts.massSolar); assertNull(facts.diameterKm)
            assertNull(facts.gravity); assertNull(facts.rotationHours)
            assertTrue(spaceCompassCelestialTemperatures(body).isEmpty())
            assertNull(spaceCompassAtmosphericPressure(body))
            assertNull(spaceCompassCatalogPhysicalSortValue(body, SpaceCompassCatalogSortField.DAY_TEMPERATURE))
        }
    }

    @Test fun stellarMassesKeepSelectedUnitsAndAnExplicitModelRangeWithoutFakeErrors() {
        val sirius = SpaceCompassCelestialBody.SIRIUS
        assertEquals(2.063, spaceCompassCelestialFacts(sirius).massSolar!!, 1e-9)
        for (pounds in listOf(false, true)) {
            val result = formatSpaceCompassCelestialMass(sirius, spaceCompassCelestialFacts(sirius),
                SpaceCompassNumericFormat.INTERNATIONAL, pounds)
            assertTrue(result.endsWith(if (pounds) " lb" else " kg"))
            assertTrue(result.contains("±")); assertFalse(result.contains("M☉"))
            val betel = SpaceCompassCelestialBody.BETELGEUSE
            val range = formatSpaceCompassCelestialMass(betel, spaceCompassCelestialFacts(betel),
                SpaceCompassNumericFormat.INTERNATIONAL, pounds)
            assertEquals("≈ 16.5–19 M☉", range)
            assertFalse(range.contains("±"))
        }
        for (upper in listOf(Double.NaN, Double.POSITIVE_INFINITY, 15.0))
            assertEquals("—", formatSpaceCompassCelestialMass(SpaceCompassCelestialBody.BETELGEUSE,
                SpaceCompassCelestialFacts(massSolar=16.5, maximumMassSolar=upper), SpaceCompassNumericFormat.INTERNATIONAL))
    }

    @Test fun catalogueAliasesAndPreferencesIncludeNewEntriesWithoutEnablingThemByDefault() {
        val defaults = SpaceCompassCelestialSelection()
        assertEquals(setOf(SpaceCompassCelestialBody.SUN, SpaceCompassCelestialBody.MOON), defaults.selected)
        val selected = defaults.selected + distant + titan
        val restored = restoreSpaceCompassCelestialSelection(selected.map { it.name }.toSet(), titan.name)
        assertEquals(selected, restored.selected); assertEquals(titan, restored.active)
        for ((query, body) in listOf("M42" to SpaceCompassCelestialBody.ORION_NEBULA,
            "NGC1976" to SpaceCompassCelestialBody.ORION_NEBULA, "M45" to SpaceCompassCelestialBody.PLEIADES,
            "Sirio" to SpaceCompassCelestialBody.SIRIUS, "Saturn VI" to titan))
            assertEquals(listOf(body), spaceCompassSearchCatalog(spaceCompassCelestialCatalogOrder, emptyMap(), query))
        assertEquals(35, spaceCompassCelestialCatalogOrder.size)
        assertEquals(1, spaceCompassCelestialCatalogOrder.count { it == SpaceCompassCelestialBody.ANDROMEDA_CORE })
    }

    @Test fun aSmallTitanDiameterUncertaintyIsNonzeroInTheDetailAndSortedCatalogue() {
        val facts=spaceCompassCelestialFacts(titan)
        for (format in SpaceCompassNumericFormat.entries) for (feet in listOf(false,true)) {
            val detail=formatSpaceCompassCelestialDiameter(facts,format,feet)
            assertFalse(detail.contains("±0 "))
            assertTrue(detail.endsWith(if (feet) "mi)" else "km)"))
            assertEquals(detail,formatSpaceCompassCatalogPhysicalValue(titan,SpaceCompassCatalogSortField.DIAMETER,
                format,SpaceCompassUnits(feet=feet)) { "reference" })
        }
        assertEquals("5\u202F149.52 km (±0.04 km)",formatSpaceCompassCelestialDiameter(facts,SpaceCompassNumericFormat.INTERNATIONAL,false))
    }

    @Test fun allTwentyLocalesContainTheSameEighteenNonemptyStringsAndPlaceholders() {
        val files = File("src/main/res").listFiles()!!.map { File(it, "catalog_additions.xml") }.filter { it.isFile }
        assertEquals(20, files.size)
        var reference: Map<String, List<String>>? = null
        for (file in files) {
            val nodes = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(file).getElementsByTagName("string")
            assertEquals(18, nodes.length)
            val keys = (0 until nodes.length).associate { index ->
                val node = nodes.item(index)
                assertTrue(node.textContent.isNotBlank())
                node.attributes.getNamedItem("name").nodeValue to Regex("%[0-9]+\\$[a-z]").findAll(node.textContent).map { it.value }.toList()
            }
            assertEquals(18, keys.size)
            if (reference == null) reference = keys else assertEquals(reference, keys)
        }
    }
}
