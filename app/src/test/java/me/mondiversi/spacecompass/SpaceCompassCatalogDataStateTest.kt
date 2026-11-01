package me.mondiversi.spacecompass

import java.time.Instant
import java.util.Locale
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test

class SpaceCompassCatalogDataStateTest {
    private val body = SpaceCompassCelestialBody.valueOf("SATELLITE_NORAD_25994")
    private val now = Instant.parse("2026-11-01T12:00:00Z").toEpochMilli()
    private val position = SpaceCompassCelestialRequest(body)
    private val earth = SpaceCompassCatalogSortField.EARTH_DISTANCE
    private val solar = SpaceCompassCatalogSortField.DISTANCE
    private val raw = javaClass.classLoader!!.getResource("catalog-repair/terra-2026-11-01.json")!!.readText()
    private val orbit = SpaceCompassCatalogSatelliteOrbit.parse(body, spaceCompassSatnogsTle(raw))
    private val cached = SpaceCompassCelestialRemoteData(satellites = mapOf(body to orbit))
    private fun state(remote: SpaceCompassCelestialRemoteData, observation: Long = now, wall: Long = now) =
        spaceCompassCatalogMissingValueState(body, earth, observation, wall, remote)

    @Test fun realCurrentAlternativeHasValidatedIdentityDistanceAndPointing() = runBlocking {
        val urls = mutableListOf<String>()
        val text = SpaceCompassCatalogSatelliteSources(body).load(now) { url, _ -> urls += url; raw }
        val downloaded = SpaceCompassCatalogSatelliteOrbit.parse(body, text)
        assertEquals(listOf("https://db.satnogs.org/api/tle/?norad_cat_id=25994"), urls)
        assertTrue(downloaded.usable(now))
        assertTrue(spaceCompassCelestialCatalogEarthDistanceKm(body, now, cached)!! in 6400.0..9000.0)
        assertNotNull(calculateSpaceCompassCelestialObservation(body, now, 0.0, 0.0, 0.0, cached))
    }

    @Test fun expiredAndMissingRowsDistinguishQueuedActiveFailedAndUncoveredDates() {
        val later = now + 7 * 86_400_000L
        assertEquals(SpaceCompassCatalogDataState.STALE, state(cached, later, later))
        assertEquals(SpaceCompassCatalogDataState.LOADING,
            state(cached.copy(requestedBodies = setOf(body)), later, later))
        assertEquals(SpaceCompassCatalogDataState.STALE,
            state(cached.copy(requestedBodies = setOf(body), failedRequests = setOf(position)), later, later))
        assertEquals(SpaceCompassCatalogDataState.LOADING,
            state(cached.copy(activeRequest = position, failedRequests = setOf(position)), later, later))
        assertEquals(SpaceCompassCatalogDataState.OUTSIDE_DATE,
            state(cached.copy(activeRequest = position), later, now))
        assertEquals(SpaceCompassCatalogDataState.UNAVAILABLE,
            state(SpaceCompassCelestialRemoteData(failedRequests = setOf(position))))
        assertEquals(SpaceCompassCatalogDataState.LOADING,
            state(SpaceCompassCelestialRemoteData(requestedBodies = setOf(body))))
        assertNull(spaceCompassCelestialCatalogEarthDistanceKm(body, later, cached))
        assertNull(calculateSpaceCompassCelestialObservation(body, later, 0.0, 0.0, 0.0, cached))
    }

    @Test fun aFailureOnOneObjectDoesNotMarkAnotherQueuedObjectUnavailable() {
        val remote = SpaceCompassCelestialRemoteData(requestedBodies = setOf(body),
            failedRequests = setOf(SpaceCompassCelestialRequest(SpaceCompassCelestialBody.HUBBLE)))
        assertEquals(SpaceCompassCatalogDataState.LOADING, state(remote))
        assertEquals(SpaceCompassCatalogDataState.UNAVAILABLE, spaceCompassCatalogMissingValueState(
            body, SpaceCompassCatalogSortField.MASS, now, now, remote))
        assertEquals(SpaceCompassCatalogDataState.UNAVAILABLE, spaceCompassCatalogMissingValueState(
            SpaceCompassCelestialBody.SOUTHERN_CROSS, solar, now, now, remote))
    }

    @Test fun solarMotionCannotPretendToProvideEarthDistanceOrPointing() {
        val comet = SpaceCompassCelestialBody.SEDNA
        val sample = SpaceCompassHorizonsStateSample(now, 83.0, 0.0, 0.0, 0.0, 0.01, 0.0)
        val motion = SpaceCompassHorizonsMotion(comet, listOf(sample.copy(timeMs = now - 3_600_000L),
            sample.copy(timeMs = now + 3_600_000L)))
        val remote = SpaceCompassCelestialRemoteData(motions = mapOf(comet to motion),
            requestedBodies = setOf(comet), failedRequests = setOf(SpaceCompassCelestialRequest(comet)))
        assertNotNull(spaceCompassCelestialCatalogDistanceAu(comet, now, remote))
        assertNull(spaceCompassCelestialCatalogEarthDistanceKm(comet, now, remote))
        assertEquals(SpaceCompassCatalogDataState.LOADING,
            spaceCompassCatalogMissingValueState(comet, solar, now, now, remote))
        assertEquals(SpaceCompassCatalogDataState.UNAVAILABLE,
            spaceCompassCatalogMissingValueState(comet, earth, now, now, remote))
    }

    @Test fun defaultUsesSolarValuesInBothGroupsAndFollowsArrivingDistanceModels() = runBlocking {
        val sun = SpaceCompassCelestialBody.SUN
        val moon = SpaceCompassCelestialBody.MOON
        val mars = SpaceCompassCelestialBody.MARS
        val bodies = listOf(mars, moon, sun)
        val calculator = SpaceCompassCatalogPresenter.create(bodies, Locale.ENGLISH, { it.name }, { it.name })
        fun query(solarValues: Map<SpaceCompassCelestialBody, Double?>) = SpaceCompassCatalogQuery(
            SpaceCompassCatalogPreferences(), "", setOf(mars), setOf(mars), emptySet(), emptyMap(),
            SpaceCompassCatalogDistances(mapOf(moon to 384000.0), solarValues,
                mapOf(moon to 384000.0, sun to SPACE_COMPASS_AU_KM, mars to SPACE_COMPASS_AU_KM)))
        val first = calculator.present(query(mapOf(sun to 0.0, moon to 1.0, mars to 1.5)), false)
        assertEquals(listOf(mars, sun, moon), first.visible)
        assertEquals(1.0, first.solarDistances[moon]!!, 0.0)
        val changed = calculator.present(query(mapOf(sun to 1.1, moon to 0.9, mars to 1.5)), false)
        assertEquals(listOf(mars, moon, sun), changed.visible)
        val ordered = spaceCompassSortCatalog(bodies, SpaceCompassCatalogSort.CATALOG, emptyMap(),
            mapOf(sun to 0.0, moon to 1.0), Locale.ROOT)
        assertEquals(listOf(sun, moon, mars), ordered)
        val estimate = formatSpaceCompassCatalogDisplayDistance(body, 1.0, SpaceCompassNumericFormat.INTERNATIONAL, "mkm")
        assertTrue(estimate.startsWith("≈ "))
    }
}
