package me.mondiversi.planetcompass

import java.time.Instant
import java.time.ZoneId
import java.util.Locale
import org.junit.Assert.*
import org.junit.Test

class PlanetCompassCelestialTimeTest {
    private val rome = ZoneId.of("Europe/Rome")
    private fun stamp(utc: String) = Instant.parse(utc).toEpochMilli()
    private fun text(utc: String, now: String = utc, zone: ZoneId = rome,
        clock: PlanetCompassTimeFormat = PlanetCompassTimeFormat.H24, date: PlanetCompassDateFormat = PlanetCompassDateFormat.INTERNATIONAL) =
        formatPlanetCompassCelestialMoment(stamp(utc), stamp(now), zone, clock, date, Locale.US)

    @Test fun pointTimeUsesTheLocalClockAndDoesNotChangeAsTheCurrentTimeAdvances() {
        assertEquals("14:30", text("2026-10-03T12:30:00Z"))
        assertEquals("14:30", text("2026-10-03T12:30:00Z", "2026-10-03T20:00:00Z"))
        assertEquals("2:30 PM", text("2026-10-03T12:30:00Z", clock = PlanetCompassTimeFormat.H12))
        assertEquals("12:30", text("2026-10-03T12:30:00Z", zone = ZoneId.of("UTC")))
    }

    @Test fun dateIsShownOnlyForAnActualDifferentLocalDayIncludingTheIssMidnightCrossing() {
        assertEquals("00:15 · 2026-10-04", text("2026-10-03T22:15:00Z", "2026-10-03T21:55:00Z"))
        assertEquals("23:55 · 03/10/2026", text("2026-10-03T21:55:00Z", "2026-10-03T22:15:00Z",
            date = PlanetCompassDateFormat.EUROPEAN))
        assertEquals("00:15", text("2026-10-03T22:15:00Z", "2026-10-04T05:00:00Z"))
    }

    @Test fun repeatedAutumnHourIsExplicitAndSpringJumpIsCorrect() {
        assertEquals("02:30 (UTC+02:00)", text("2026-10-25T00:30:00Z"))
        assertEquals("02:30 (UTC+01:00)", text("2026-10-25T01:30:00Z"))
        assertEquals("01:59", text("2026-03-29T00:59:00Z"))
        assertEquals("03:01", text("2026-03-29T01:01:00Z"))
    }

    @Test fun fractionalTimeZonesAndDateComparisonsUseTheExplicitZone() {
        assertEquals("00:15 · 2026-10-04", text("2026-10-03T18:30:00Z", "2026-10-03T17:20:00Z",
            zone = ZoneId.of("Asia/Kathmandu")))
        assertEquals("18:30", text("2026-10-03T18:30:00Z", "2026-10-03T18:20:00Z", zone = ZoneId.of("UTC")))
    }

    @Test fun captionStaysNearThePointWithoutCoveringItOrControls() {
        val anchor = PlanetCompassSunScenePoint(160.0, 100.0)
        val below = placePlanetCompassCelestialTimeBadge(320.0, 300.0, 100.0, 30.0, anchor, 4.0, 18.0)!!
        assertEquals(PlanetCompassSunScenePoint(110.0, 118.0), below)
        val above = placePlanetCompassCelestialTimeBadge(320.0, 300.0, 100.0, 30.0, anchor, 4.0, 18.0,
            listOf(PlanetCompassSunSceneFrame(0.0, 115.0, 320.0, 100.0)))!!
        assertEquals(PlanetCompassSunScenePoint(110.0, 52.0), above)
    }

    @Test fun allEdgeAndOffscreenLocationsAreClampedAndAvoidTheSelectionPanel() {
        val panel = PlanetCompassSunSceneFrame(0.0, 220.0, 320.0, 80.0)
        val menu = PlanetCompassSunSceneFrame(150.0, 0.0, 170.0, 56.0)
        for (x in listOf(0.0, 10.0, 160.0, 310.0, 320.0)) for (y in listOf(0.0, 20.0, 150.0, 290.0, 300.0)) {
            val point = placePlanetCompassCelestialTimeBadge(320.0, 300.0, 140.0, 30.0,
                PlanetCompassSunScenePoint(x, y), 4.0, 18.0, listOf(menu, panel))!!
            assertTrue(point.x >= 4.0 && point.x + 140.0 <= 316.0)
            assertTrue(point.y >= 4.0 && point.y + 30.0 <= 220.0)
            assertFalse(point.x < 320.0 && point.x + 140.0 > 150.0 && point.y < 56.0)
        }
    }

    @Test fun missingOrientationHasAStableReadableCornerInsteadOfAPretendBearing() {
        assertEquals(PlanetCompassSunScenePoint(4.0, 4.0), placePlanetCompassCelestialTimeBadge(
            320.0, 300.0, 140.0, 30.0, null, 4.0, 18.0))
    }

    @Test fun impossibleAndInvalidViewportsDoNotClipOrOverlapTheCaption() {
        assertNull(placePlanetCompassCelestialTimeBadge(40.0, 20.0, 100.0, 30.0, null, 4.0, 18.0))
        assertNull(placePlanetCompassCelestialTimeBadge(320.0, 300.0, 100.0, 30.0, null, 4.0, 18.0,
            listOf(PlanetCompassSunSceneFrame(0.0, 0.0, 320.0, 300.0))))
        assertNull(placePlanetCompassCelestialTimeBadge(Double.NaN, 300.0, 100.0, 30.0, null, 4.0, 18.0))
    }
}
