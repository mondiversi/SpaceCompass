package me.mondiversi.spacecompass

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.net.URLDecoder
import org.junit.Assert.*
import org.junit.Test

/** Real NASA/JPL responses; exercise corrected pointing separately from heliocentric motion. */
class SpaceCompassCometsTest {
    private val now = Instant.parse("2026-10-05T12:00:00Z").toEpochMilli()
    private fun fixture(name: String) = requireNotNull(javaClass.getResource("/comets/$name.txt")).readText()

    @Test fun bothCometsHavePositionsDailyPathsVelocitiesAndTheCometFilter() {
        for ((body, prefix) in listOf(SpaceCompassCelestialBody.HALLEY to "1p", SpaceCompassCelestialBody.COMET_67P to "67p")) {
            val position = parseSpaceCompassHorizonsEphemeris(body, fixture("$prefix-pointing"))
            val motion = parseSpaceCompassHorizonsMotion(body, fixture("$prefix-motion"))
            val remote = SpaceCompassCelestialRemoteData(ephemerides = mapOf(body to position), motions = mapOf(body to motion))
            val observation = requireNotNull(calculateSpaceCompassCelestialObservation(body, now, 41.9028, 12.4964, remote = remote))
            assertTrue(observation.distanceKm > SPACE_COMPASS_AU_KM)
            assertTrue(observation.position.azimuthDegrees in 0.0..360.0)
            assertTrue(observation.position.elevationDegrees in -90.0..90.0)
            assertTrue(requireNotNull(motion.speedAt(now)).isFinite())
            assertTrue(requireNotNull(spaceCompassCelestialCatalogDistanceAu(body, now, remote)) > 1)
            val path = requireNotNull(calculateSpaceCompassCelestialPath(body, LocalDate.parse("2026-10-05"),
                ZoneId.of("Europe/Rome"), now, 41.9028, 12.4964, 0.0, remote))
            assertTrue(path.samples.size > 400)
            assertTrue(path.samples.maxOf { it.position.elevationDegrees } - path.samples.minOf { it.position.elevationDegrees } > 30)
            assertEquals(SpaceCompassCatalogType.COMET, body.catalogType)
            assertTrue(body.supportsDailyPath && body.usesLiveDistance)
            val decoded = URLDecoder.decode(spaceCompassHorizonsUrl(body, now), "UTF-8")
            assertTrue(decoded.contains("NOFRAG;CAP;"))
        }
        assertEquals(listOf(SpaceCompassCelestialBody.HALLEY, SpaceCompassCelestialBody.COMET_67P),
            spaceCompassFilterCatalog(setOf(SpaceCompassCatalogType.COMET), SpaceCompassCatalogVisibility.ALL, emptyMap()))
    }

    @Test fun apparentAndGeometricStatesAndCometIdentitiesCannotBeMixed() {
        for ((body, prefix) in listOf(SpaceCompassCelestialBody.HALLEY to "1p", SpaceCompassCelestialBody.COMET_67P to "67p")) {
            assertTrue(runCatching { parseSpaceCompassHorizonsMotion(body, fixture("$prefix-pointing")) }.isFailure)
            assertTrue(runCatching { parseSpaceCompassHorizonsEphemeris(body, fixture("$prefix-motion")) }.isFailure)
        }
        assertTrue(runCatching { parseSpaceCompassHorizonsEphemeris(SpaceCompassCelestialBody.COMET_67P, fixture("1p-pointing")) }.isFailure)
        assertNull(spaceCompassAtmosphericPressure(SpaceCompassCelestialBody.COMET_67P))
        val halleyTemperature = spaceCompassCelestialTemperatures(SpaceCompassCelestialBody.HALLEY).single()
        assertEquals(SpaceCompassCelestialTemperatureKind.HISTORICAL_SURFACE_RANGE, halleyTemperature.kind)
        assertEquals(1986, halleyTemperature.epochYear)
        assertNull(spaceCompassCelestialFacts(SpaceCompassCelestialBody.HALLEY).diameterKm)
    }
}
