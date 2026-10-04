package me.mondiversi.planetcompass

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import kotlin.math.*
import org.junit.Assert.*
import org.junit.Test

class PlanetCompassIssPathTest {
    private val now = Instant.parse("2026-10-03T18:35:00Z").toEpochMilli()
    private val date = LocalDate.parse("2026-10-03")
    private val zone = ZoneId.of("Europe/Rome")
    private val orbit = PlanetCompassIssOrbit.parse("1 25544U 98067A   26276.04623379  .00005750  00000+0  11349-3 0  9994\n" +
        "2 25544  51.6314 126.3061 0006899 216.4733 143.5786 15.48722558588472")
    private val remote = PlanetCompassCelestialRemoteData(iss = orbit)
    private fun path(time: Long = now, latitude: Double = 45.0) = calculatePlanetCompassCelestialPath(
        PlanetCompassCelestialBody.ISS, Instant.ofEpochMilli(time).atZone(zone).toLocalDate(), zone,
        time, latitude, 9.0, 0.0, remote)
    private fun facing(p: PlanetCompassSunPosition): PlanetCompassSunOrientation {
        val a = Math.toRadians(p.azimuthDegrees); val e = Math.toRadians(p.elevationDegrees)
        return PlanetCompassSunOrientation(PlanetCompassSunVector(cos(a), -sin(a), 0.0),
            PlanetCompassSunVector(-sin(e) * sin(a), -sin(e) * cos(a), cos(e)),
            PlanetCompassSunVector(cos(e) * sin(a), cos(e) * cos(a), sin(e)))
    }

    @Test fun fullRevolutionUsesTlePeriodAndAlwaysContainsCurrentPosition() {
        assertEquals((86_400_000.0 / 15.48722558).roundToLong(), orbit.periodMs)
        val p = path()!!
        assertEquals(orbit.periodMs, p.samples.last().timeMs - p.samples.first().timeMs)
        assertTrue(now in p.samples.first().timeMs..p.samples.last().timeMs)
        assertTrue(p.samples.size in 500..650)
        assertTrue(p.samples.minOf { it.position.elevationDegrees } < -60.0)
        val current = orbit.observe(now, 45.0, 9.0, 0.0).position
        assertTrue(current.elevationDegrees < 0.0)
        assertEquals(current, p.samples.single { it.timeMs == now }.position)
        assertTrue(projectPlanetCompassSunDailyPath(p, facing(current), 400.0, 600.0).any { it.belowHorizon })
        // The observer moves with Earth: do not fabricate a closed circle between different endpoints.
        val a = p.samples.first().direction; val b = p.samples.last().direction
        assertTrue(sqrt((a.east - b.east).pow(2) + (a.north - b.north).pow(2) + (a.up - b.up).pow(2)) > 0.01)
    }

    @Test fun orbitIsStableWithinMinuteAndRefreshesWithoutDroppingCurrentTime() {
        val first = path()!!; val same = path(now + 30_000L)!!; val later = path(now + 60_000L)!!
        assertEquals(first.samples, same.samples)
        assertEquals(first.markers, same.markers)
        assertEquals(60_000L, later.samples.first().timeMs - first.samples.first().timeMs)
        assertTrue(now + 60_000L in later.samples.first().timeMs..later.samples.last().timeMs)
        val regular = first.markers.filter { it.event == PlanetCompassSunPathEvent.HOUR }
        assertEquals(24, regular.size)
        assertEquals(regular.size, regular.map { it.timeMs }.distinct().size)
        regular.forEachIndexed { index, marker ->
            assertEquals(first.samples.first().timeMs + orbit.periodMs * index / 24, marker.timeMs)
        }
        assertTrue(regular.any { it.position.elevationDegrees < 0 })
    }

    @Test fun nextPassIsSeparateAndContainsRefinedRisePeakAndSet() {
        val p = path()!!
        assertEquals(listOf(PlanetCompassSunPathEvent.SUNRISE, PlanetCompassSunPathEvent.CULMINATION, PlanetCompassSunPathEvent.SUNSET),
            p.issPass.map { it.event })
        val (rise, peak, set) = p.issPass
        assertTrue(rise.timeMs > p.samples.last().timeMs)
        assertTrue(rise.timeMs <= now + 24 * 3_600_000L)
        assertTrue(rise.timeMs < peak.timeMs && peak.timeMs < set.timeMs)
        assertEquals(0.0, rise.position.elevationDegrees, 0.02)
        assertEquals(0.0, set.position.elevationDegrees, 0.02)
        for (delta in listOf(-10_000L, 10_000L))
            assertTrue(peak.position.elevationDegrees > orbit.observe(peak.timeMs + delta, 45.0, 9.0, 0.0).position.elevationDegrees)
    }

    @Test fun currentPassRemainsCurrentRatherThanSkippingToTheFollowingPass() {
        val first = path()!!.issPass
        val during = path(first[1].timeMs)!!
        for ((a, b) in first.zip(during.issPass)) assertEquals(a.timeMs.toDouble(), b.timeMs.toDouble(), 250.0)
        assertTrue(during.samples.any { it.position.elevationDegrees > 0 })
        assertTrue(during.samples.any { it.position.elevationDegrees < -60 })
        assertTrue(during.markers.any { it.event == PlanetCompassSunPathEvent.SUNRISE })
        assertTrue(during.markers.any { it.event == PlanetCompassSunPathEvent.SUNSET })
        assertTrue(during.issPass[0].timeMs < first[1].timeMs && during.issPass[2].timeMs > first[1].timeMs)
    }

    @Test fun orbitStillExistsWhereNoPassCanRiseAboveHorizon() {
        val p = path(latitude = 89.0)!!
        assertTrue(p.issPass.isEmpty())
        assertTrue(p.samples.all { it.position.elevationDegrees < 0 })
        assertEquals(orbit.periodMs, p.samples.last().timeMs - p.samples.first().timeMs)
        val current = orbit.observe(now, 89.0, 9.0, 0.0).position
        assertTrue(projectPlanetCompassSunDailyPath(p, facing(current), 400.0, 600.0).any { it.belowHorizon })
    }

    @Test fun eachCompleteOrbitHasARefinedMinimumWhileItsPassSummaryStaysRisePeakSet() {
        for (latitude in listOf(0.0, 45.0, 89.0, -89.0)) {
            val p = path(latitude = latitude)!!
            val minimum = p.markers.single { PlanetCompassSunPathEvent.MINIMUM in it.events }
            assertTrue(minimum.timeMs in p.samples.first().timeMs..p.samples.last().timeMs)
            assertTrue(minimum.position.elevationDegrees <= p.samples.minOf { it.position.elevationDegrees } + 1e-6)
            assertEquals(24, p.markers.count { it.event == PlanetCompassSunPathEvent.HOUR })
            assertFalse(p.issPass.any { PlanetCompassSunPathEvent.MINIMUM in it.events })
        }
    }

    @Test fun validityEdgesShiftTheFullOrbitInsteadOfRemovingOrExtrapolatingIt() {
        val earliest = orbit.epochMs - PLANET_COMPASS_ISS_FUTURE_EPOCH_ALLOWANCE_MS
        val latest = orbit.epochMs + PLANET_COMPASS_ISS_MAX_AGE_MS
        for (time in listOf(earliest, earliest + 60_000L, latest - 60_000L, latest)) {
            val p = path(time)!!
            assertEquals(orbit.periodMs, p.samples.last().timeMs - p.samples.first().timeMs)
            assertTrue(time in p.samples.first().timeMs..p.samples.last().timeMs)
            assertTrue(p.samples.all { orbit.usable(it.timeMs) })
            assertEquals(24, p.markers.count { it.event == PlanetCompassSunPathEvent.HOUR })
            assertTrue(p.issPass.all { orbit.usable(it.timeMs) })
        }
        assertNull(path(earliest - 1))
        assertNull(path(latest + 1))
        assertNull(calculatePlanetCompassCelestialPath(PlanetCompassCelestialBody.ISS, date, zone, now, 45.0, 9.0, 0.0, PlanetCompassCelestialRemoteData()))
    }

    @Test fun orbitUsesUtcAndDoesNotDependOnLocalMidnightOrDst() {
        val a = path()!!
        for (z in listOf("UTC", "Pacific/Auckland", "America/New_York")) {
            val otherZone = ZoneId.of(z)
            val b = calculatePlanetCompassCelestialPath(PlanetCompassCelestialBody.ISS,
                Instant.ofEpochMilli(now).atZone(otherZone).toLocalDate(), otherZone, now, 45.0, 9.0, 0.0, remote)!!
            assertEquals(a.samples, b.samples); assertEquals(a.markers, b.markers); assertEquals(a.issPass, b.issPass)
        }
    }

    @Test fun everyOtherBodyKeepsItsDailyOrNoPathPolicyIncludingUndergroundProjection() {
        val begin = Instant.parse("2026-10-02T00:00:00Z").toEpochMilli()
        val sedna = PlanetCompassHorizonsEphemeris(PlanetCompassCelestialBody.SEDNA,
            (0..72).map { PlanetCompassHorizonsSample(begin + it * 3_600_000L, 38.0, 72.0, 12.0) })
        val data = remote.copy(ephemerides = mapOf(PlanetCompassCelestialBody.SEDNA to sedna))
        for (body in PlanetCompassCelestialBody.entries.filter { !it.isEarthSatellite }) {
            val p = calculatePlanetCompassCelestialPath(body, date, zone, now, 45.0, 9.0, 0.0, data)
            if (body.isVoyager) { assertNull(p); continue }
            assertNotNull(body.name, p)
            assertEquals(86_400_000L, p!!.samples.last().timeMs - p.samples.first().timeMs)
            assertTrue(p.issPass.isEmpty())
            if (body == PlanetCompassCelestialBody.POLARIS) {
                // Circumpolar at 45N: the star must never falsely cross below the horizon.
                assertTrue(p.samples.all { it.position.elevationDegrees > 0 })
                assertFalse(projectPlanetCompassSunDailyPath(p, facing(p.samples.first().position),400.0,600.0).any { it.belowHorizon })
                continue
            }
            val below = p.samples.first { it.position.elevationDegrees < -1 }
            assertTrue(body.name, projectPlanetCompassSunDailyPath(p, facing(below.position), 400.0, 600.0).any { it.belowHorizon })
        }
    }
}
