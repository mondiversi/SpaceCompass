package me.mondiversi.spacecompass

import org.junit.Assert.*
import org.junit.Test
import java.time.Instant
import java.util.Random

class SpaceCompassLargeCatalogTest {
    private val now = Instant.parse("2026-10-26T12:00:00Z").toEpochMilli()
    private fun fixture(name: String) = javaClass.classLoader!!.getResource("catalog-999/$name")!!.readText()
    @Test fun all999IdentitiesAreUniqueAndEveryAdditionHasARealPointingModel() {
        assertEquals(9999, spaceCompassCelestialCatalogOrder.size)
        assertEquals(9999, spaceCompassCelestialCatalogOrder.distinct().size)
        assertEquals(9884, spaceCompassLargeCatalogEntries.size)
        spaceCompassLargeCatalogEntries.forEach { row ->
            val body = row.body
            assertNotNull(body.additionalCatalogEntry)
            assertTrue(body.name, body.deepSkyReference != null || body.usesHorizons || body.catalogSatellite != null)
            assertFalse(body.sourceName().isBlank())
            assertTrue(row.sourceUrl.startsWith("https://"))
        }
        val expected = mapOf(SpaceCompassCatalogType.PLANET to 7, SpaceCompassCatalogType.DWARF_PLANET to 100,
            SpaceCompassCatalogType.NATURAL_SATELLITE to 200, SpaceCompassCatalogType.ASTEROID to 1000,
            SpaceCompassCatalogType.COMET to 300, SpaceCompassCatalogType.STAR to 3061,
            SpaceCompassCatalogType.EXOPLANET to 1500, SpaceCompassCatalogType.BLACK_HOLE to 120,
            SpaceCompassCatalogType.NEUTRON_STAR to 250, SpaceCompassCatalogType.GALAXY to 1600,
            SpaceCompassCatalogType.NEBULA to 900, SpaceCompassCatalogType.STAR_CLUSTER to 700,
            SpaceCompassCatalogType.CONSTELLATION to 88, SpaceCompassCatalogType.SPACE_PROBE to 60,
            SpaceCompassCatalogType.ARTIFICIAL_SATELLITE to 100, SpaceCompassCatalogType.SPACE_STATION to 2, SpaceCompassCatalogType.SCIENCE_FICTION to 11)
        assertEquals(expected, spaceCompassCelestialCatalogOrder.groupingBy { it.catalogType }.eachCount())
    }
    private fun SpaceCompassCelestialBody.sourceName() = largeCatalogEntry!!.source
    @Test fun everyNewDirectionIsUsableOfflineAtBothHemispheresAndTheEquator() {
        for (row in spaceCompassLargeCatalogEntries.filter { it.entry.reference != null }) {
            val body = row.body
            for (latitude in listOf(-80.0, 0.0, 45.0, 80.0)) {
                val observation = calculateSpaceCompassCelestialObservation(body, now, latitude, 9.0, 100.0)!!
                assertTrue(body.name, observation.position.azimuthDegrees in 0.0..360.0)
                assertTrue(body.name, observation.position.elevationDegrees in -90.0..90.0)
                if (row.entry.reference!!.hasMeasuredDistance && body.constellationPattern == null)
                    assertTrue(body.name, observation.distanceKm.isFinite() && observation.distanceKm > 0)
                else {
                    assertTrue(body.name, observation.distanceKm.isNaN())
                    assertNull(spaceCompassCelestialCatalogDistanceAu(body, now))
                    assertNull(spaceCompassCelestialCatalogEarthDistanceKm(body, now))
                }
            }
        }
    }
    @Test fun everyJplTargetParsesVerifiedLiveStatesAndRefusesToExtrapolate() {
        for (row in spaceCompassLargeCatalogEntries.filter { it.entry.target != null && it.body.ordinal < 1010 }) {
            val states = parseSpaceCompassHorizonsEphemeris(row.body, fixture(row.body.name.lowercase()+".txt"))
            val time = states.samples[12].timeMs
            val remote = SpaceCompassCelestialRemoteData(ephemerides = mapOf(row.body to states))
            assertNotNull(row.body.name, calculateSpaceCompassCelestialObservation(row.body,time,45.0,9.0,100.0,remote))
            assertNull(states.at(states.samples.first().timeMs-1))
            assertNull(states.at(states.samples.last().timeMs+1))
            if (row.entry.type == SpaceCompassCatalogType.NATURAL_SATELLITE)
                assertEquals(row.facts.parent, row.body.horizonsMotionPrimary)
        }
    }
    @Test fun everySatellitePassesIdentityChecksumAndItsOwnOrbitalEnvelope() {
        for (row in spaceCompassLargeCatalogEntries.filter { it.satellite != null && it.body.ordinal < 1010 }) {
            val orbit = SpaceCompassCatalogSatelliteOrbit.parse(row.body,fixture(row.body.name.lowercase()+".tle"))
            assertTrue(orbit.usable(orbit.epochMs))
            assertFalse(orbit.usable(orbit.epochMs+40*86400000L))
            val observation=orbit.observe(orbit.epochMs,45.0,9.0,100.0)
            assertTrue(observation.position.elevationDegrees in -90.0..90.0)
            assertTrue(orbit.speedKmPerSecond(orbit.epochMs) in row.satellite!!.speedKmSecond)
        }
    }
    @Test fun all88PatternsHaveRealStarsValidEdgesAndRotateWithTheSky() {
        val bodies = spaceCompassCelestialCatalogOrder.filter { it.constellationPattern != null }
        assertEquals(88,bodies.size)
        for (body in bodies) {
            val pattern=body.constellationPattern!!
            assertTrue(pattern.stars.size>=2 && pattern.edges.isNotEmpty())
            assertTrue(pattern.edges.all { it.first in pattern.stars.indices && it.second in pattern.stars.indices && it.first!=it.second })
            val symbol=spaceCompassConstellationSymbolPoints(body)
            assertEquals(pattern.stars.size,symbol.size)
            assertTrue(symbol.all { it.x.isFinite() && it.y.isFinite() && it.x in -1.001..1.001 && it.y in -1.001..1.001 })
            val first=calculateSpaceCompassConstellation(body,now,45.0,9.0)
            val next=calculateSpaceCompassConstellation(body,now+6*3600000,45.0,9.0)
            assertTrue(body.name, first.zip(next).any { (a,b)->spaceCompassAngularSeparation(a,b)>1 })
            assertTrue(body.name, spaceCompassAngularSeparation(first[0],first[1])-
                spaceCompassAngularSeparation(next[0],next[1])<.01)
        }
    }
    @Test fun sparsePairSearchMatchesExhaustiveAngularSearchIncludingSeamsAndPoles() {
        val random=Random(42)
        val positions=spaceCompassCelestialCatalogOrder.take(1009).associateWith {
            SpaceCompassSunPosition(random.nextDouble()*360,random.nextDouble()*180-90)
        }.toMutableMap()
        positions[SpaceCompassCelestialBody.SUN]=SpaceCompassSunPosition(359.8,0.0)
        positions[SpaceCompassCelestialBody.MOON]=SpaceCompassSunPosition(.2,0.0)
        val bodies=positions.keys.toList()
        val expected=buildSet {
            for(i in bodies.indices) for(j in i+1 until bodies.size) {
                val a=bodies[i];val b=bodies[j]
                if (!a.isFictional && !b.isFictional && !spaceCompassSharesUnresolvedTarget(a,b) && spaceCompassAngularSeparation(positions.getValue(a),positions.getValue(b))<=SPACE_COMPASS_APPROACH_LIMIT_DEGREES)
                    add(setOf(a,b))
            }
        }
        val actual=spaceCompassCurrentApproaches(now,positions).map { setOf(it.first,it.second) }.toSet()
        assertEquals(expected,actual)
        assertTrue(spaceCompassApproachCandidates(positions).size<10000)
    }
    @Test fun addedGalaxiesAndTheirOwnNucleiDoNotBecomeConjunctions() {
        var checked=0
        for(nucleus in spaceCompassLargeCatalogEntries.filter { it.entry.type==SpaceCompassCatalogType.BLACK_HOLE && it.galaxy.startsWith("NGC ") }) {
            val galaxy=spaceCompassLargeCatalogEntries.firstOrNull { it.entry.type==SpaceCompassCatalogType.GALAXY && it.entry.designation==nucleus.galaxy } ?: continue
            assertTrue(spaceCompassSharesGalacticNucleus(galaxy.body,nucleus.body))
            assertTrue(spaceCompassSharesGalacticNucleus(nucleus.body,galaxy.body));checked++
        }
        assertTrue(checked>0)
    }
    @Test fun everyNewSelectionRoundTripsAndUnknownScienceRemainsAbsent() {
        val bodies=spaceCompassCelestialCatalogOrder.toSet()
        val state=SpaceCompassCelestialSelection(bodies,SpaceCompassCelestialBody.LV_426)
        assertEquals(state,restoreSpaceCompassCelestialSelection(bodies.map { it.name }.toSet(),state.active!!.name))
        val noDistance=spaceCompassLargeCatalogEntries.first { it.entry.reference?.hasMeasuredDistance==false && it.pattern==null }
        assertNull(spaceCompassCelestialCatalogReferenceDistanceAu(noDistance.body))
        assertNull(spaceCompassAtmosphericPressure(noDistance.body))
        assertNull(spaceCompassCelestialFacts(noDistance.body).massKg)
    }
}
