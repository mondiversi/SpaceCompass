package me.mondiversi.spacecompass

import java.time.Instant
import java.time.ZoneId
import java.util.Locale
import org.junit.Assert.*
import org.junit.Test

class SpaceCompassCelestialTimeTest {
    private val rome = ZoneId.of("Europe/Rome")
    private fun stamp(utc: String) = Instant.parse(utc).toEpochMilli()
    private fun text(utc: String, now: String = utc, zone: ZoneId = rome,
        clock: SpaceCompassTimeFormat = SpaceCompassTimeFormat.H24, date: SpaceCompassDateFormat = SpaceCompassDateFormat.INTERNATIONAL) =
        formatSpaceCompassCelestialMoment(stamp(utc), stamp(now), zone, clock, date, Locale.US)

    @Test fun pointTimeUsesTheLocalClockAndDoesNotChangeAsTheCurrentTimeAdvances() {
        assertEquals("14:30", text("2026-10-03T12:30:00Z"))
        assertEquals("14:30", text("2026-10-03T12:30:00Z", "2026-10-03T20:00:00Z"))
        assertEquals("2:30 PM", text("2026-10-03T12:30:00Z", clock = SpaceCompassTimeFormat.H12))
        assertEquals("12:30", text("2026-10-03T12:30:00Z", zone = ZoneId.of("UTC")))
    }

    @Test fun dateIsShownOnlyForAnActualDifferentLocalDayIncludingTheIssMidnightCrossing() {
        assertEquals("00:15 · 2026-10-04", text("2026-10-03T22:15:00Z", "2026-10-03T21:55:00Z"))
        assertEquals("23:55 · 03/10/2026", text("2026-10-03T21:55:00Z", "2026-10-03T22:15:00Z",
            date = SpaceCompassDateFormat.EUROPEAN))
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
        val anchor = SpaceCompassSunScenePoint(160.0, 100.0)
        val below = placeSpaceCompassCelestialTimeBadge(320.0, 300.0, 100.0, 30.0, anchor, 4.0, 18.0)!!
        assertEquals(SpaceCompassSunScenePoint(110.0, 118.0), below)
        val above = placeSpaceCompassCelestialTimeBadge(320.0, 300.0, 100.0, 30.0, anchor, 4.0, 18.0,
            listOf(SpaceCompassSunSceneFrame(0.0, 115.0, 320.0, 100.0)))!!
        assertEquals(SpaceCompassSunScenePoint(110.0, 52.0), above)
    }

    @Test fun allEdgeAndOffscreenLocationsAreClampedAndAvoidTheSelectionPanel() {
        val panel = SpaceCompassSunSceneFrame(0.0, 220.0, 320.0, 80.0)
        val menu = SpaceCompassSunSceneFrame(150.0, 0.0, 170.0, 56.0)
        for (x in listOf(0.0, 10.0, 160.0, 310.0, 320.0)) for (y in listOf(0.0, 20.0, 150.0, 290.0, 300.0)) {
            val point = placeSpaceCompassCelestialTimeBadge(320.0, 300.0, 140.0, 30.0,
                SpaceCompassSunScenePoint(x, y), 4.0, 18.0, listOf(menu, panel))!!
            assertTrue(point.x >= 4.0 && point.x + 140.0 <= 316.0)
            assertTrue(point.y >= 4.0 && point.y + 30.0 <= 220.0)
            assertFalse(point.x < 320.0 && point.x + 140.0 > 150.0 && point.y < 56.0)
        }
    }

    @Test fun missingOrientationHasAStableReadableCornerInsteadOfAPretendBearing() {
        assertEquals(SpaceCompassSunScenePoint(4.0, 4.0), placeSpaceCompassCelestialTimeBadge(
            320.0, 300.0, 140.0, 30.0, null, 4.0, 18.0))
    }

    @Test fun impossibleAndInvalidViewportsDoNotClipOrOverlapTheCaption() {
        assertNull(placeSpaceCompassCelestialTimeBadge(40.0, 20.0, 100.0, 30.0, null, 4.0, 18.0))
        assertNull(placeSpaceCompassCelestialTimeBadge(320.0, 300.0, 100.0, 30.0, null, 4.0, 18.0,
            listOf(SpaceCompassSunSceneFrame(0.0, 0.0, 320.0, 300.0))))
        assertNull(placeSpaceCompassCelestialTimeBadge(Double.NaN, 300.0, 100.0, 30.0, null, 4.0, 18.0))
    }

    @Test fun systemDateUsesDeviceRegionEvenWhenInterfaceLanguageChanges() {
        val moment = stamp("2026-10-04T12:00:00Z")
        val now = stamp("2026-10-03T12:00:00Z")
        for (ui in listOf(Locale.US, Locale.ITALY, Locale.JAPAN)) {
            val text = formatSpaceCompassCelestialMoment(moment, now, ZoneId.of("UTC"),
                SpaceCompassTimeFormat.H24, SpaceCompassDateFormat.SYSTEM, ui, Locale.US)
            assertTrue(text.endsWith(" · 10/4/26"))
        }
        val explicit = formatSpaceCompassCelestialMoment(moment, now, ZoneId.of("UTC"),
            SpaceCompassTimeFormat.H24, SpaceCompassDateFormat.EUROPEAN, Locale.US, Locale.JAPAN)
        assertTrue(explicit.endsWith(" · 04/10/2026"))
    }
}
