package me.mondiversi.spacecompass

import io.github.cosinekitty.astronomy.*
import org.junit.Assert.*
import org.junit.Test
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import kotlin.math.*

class SpaceCompassDeepSkyTest {
    private val now = Instant.parse("2026-10-04T12:00:00Z").toEpochMilli()
    private val bodies = listOf(SpaceCompassCelestialBody.ALPHA_CENTAURI,
        SpaceCompassCelestialBody.SAGITTARIUS_A, SpaceCompassCelestialBody.ANDROMEDA_CORE, SpaceCompassCelestialBody.TON_618, SpaceCompassCelestialBody.STEPHENSON_2_18,
        SpaceCompassCelestialBody.RX_J1856, SpaceCompassCelestialBody.PSR_J0437, SpaceCompassCelestialBody.PROXIMA_CENTAURI, SpaceCompassCelestialBody.RIGEL)

    @Test fun referencesRemainExtrasolarAcrossCatalogCompassAndInformationFormats() {
        for (body in bodies) {
            val reference = body.deepSkyReference!!
            val au = spaceCompassCelestialCatalogDistanceAu(body, now)!!
            assertEquals(reference.distanceLy, au * SPACE_COMPASS_AU_KM / SPACE_COMPASS_LIGHT_YEAR_KM, 1e-8)
            for (latitude in listOf(-70.0, 0.0, 45.0, 89.0)) {
                val observation = calculateSpaceCompassCelestialObservation(body, now, latitude, 12.5)!!
                assertTrue(observation.position.azimuthDegrees in 0.0..360.0)
                assertTrue(observation.position.elevationDegrees in -90.0..90.0)
                assertEquals(reference.distanceLy, observation.distanceKm / SPACE_COMPASS_LIGHT_YEAR_KM, 0.0001)
                for (unit in listOf("default", "mkm", "mmi")) {
                    val expected = formatSpaceCompassNumber(reference.distanceLy, 1, SpaceCompassNumericFormat.EUROPEAN) + " ly"
                    assertEquals(expected, formatSpaceCompassSelectedDistance(body, observation.distanceKm, SpaceCompassNumericFormat.EUROPEAN, unit))
                    assertEquals(expected, formatSpaceCompassCelestialCatalogDistance(au, SpaceCompassNumericFormat.EUROPEAN, unit))
                }
            }
            assertNull(calculateSpaceCompassCelestialSpeed(body, now))
            assertNull(body.viewerTexture)
            if (body == SpaceCompassCelestialBody.PROXIMA_CENTAURI || body == SpaceCompassCelestialBody.RIGEL) {
                assertTrue(spaceCompassCelestialFacts(body).massSolar!! > 0)
                assertTrue(spaceCompassCelestialFacts(body).diameterKm!! > 0)
                assertEquals(1, spaceCompassCelestialTemperatures(body).size)
            } else {
                assertNull(spaceCompassCelestialFacts(body).gravity)
                assertTrue(spaceCompassCelestialTemperatures(body).isEmpty())
            }
            assertFalse(body.usesHorizons)
        }
        assertTrue(SpaceCompassCelestialBody.ALPHA_CENTAURI.deepSkyReference!!.distanceLy in 4.3..4.4)
        assertEquals(26_400.0, SpaceCompassCelestialBody.SAGITTARIUS_A.deepSkyReference!!.distanceLy, 0.0)
        assertEquals(2_500_000.0, SpaceCompassCelestialBody.ANDROMEDA_CORE.deepSkyReference!!.distanceLy, 0.0)
    }

    @Test fun pointingMatchesIndependentOfDateEquatorialHorizonConversion() {
        val epoch = Instant.parse("2000-01-01T12:00:00Z").toEpochMilli()
        val time = spaceCompassAstronomyTime(epoch)
        for (body in bodies) for (latitude in listOf(-45.0, 0.0, 45.0)) {
            val reference = body.deepSkyReference!!
            val ra = Math.toRadians(reference.raHours * 15)
            val dec = Math.toRadians(reference.decDegrees)
            val ofDate = rotationEqjEqd(time).rotate(Vector(cos(dec)*cos(ra), cos(dec)*sin(ra), sin(dec), time))
            val ofDateRa = (Math.toDegrees(atan2(ofDate.y, ofDate.x)) + 360) % 360 / 15
            val ofDateDec = Math.toDegrees(atan2(ofDate.z, hypot(ofDate.x, ofDate.y)))
            val expected = horizon(time, Observer(latitude, 12.5, 0.0), ofDateRa, ofDateDec, Refraction.None)
            val actual = calculateSpaceCompassCelestialObservation(body, epoch, latitude, 12.5)!!.position
            val difference = ((actual.azimuthDegrees-expected.azimuth+540) % 360)-180
            // Near the zenith, azimuth amplifies tiny aberration shifts: compare sky directions.
            val expectedAltitude = Math.toRadians(expected.altitude)
            val actualAltitude = Math.toRadians(actual.elevationDegrees)
            val separation = Math.toDegrees(acos((sin(expectedAltitude) * sin(actualAltitude) +
                cos(expectedAltitude) * cos(actualAltitude) * cos(Math.toRadians(difference))).coerceIn(-1.0, 1.0)))
            assertEquals(body.name, 0.0, separation, 0.02)
        }
    }

    @Test fun dailyTracksWorkOfflineAndRespectSouthernVisibility() {
        for (body in bodies) {
            val path = calculateSpaceCompassCelestialPath(body, LocalDate.parse("2026-10-04"),
                ZoneId.of("Europe/Rome"), now, 45.0, 12.5, 0.0, SpaceCompassCelestialRemoteData())!!
            assertEquals(24, path.markers.count { it.event == SpaceCompassSunPathEvent.HOUR })
            assertTrue(path.issPass.isEmpty())
            if (body == SpaceCompassCelestialBody.ALPHA_CENTAURI || body == SpaceCompassCelestialBody.PSR_J0437 || body == SpaceCompassCelestialBody.PROXIMA_CENTAURI)
                assertTrue(path.samples.all { it.position.elevationDegrees < 0 })
            else assertTrue(path.samples.any { it.position.elevationDegrees > 0 })
        }
    }
}
