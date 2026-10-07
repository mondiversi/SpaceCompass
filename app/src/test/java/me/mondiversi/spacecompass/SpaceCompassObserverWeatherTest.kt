package me.mondiversi.spacecompass

import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.util.Locale
import org.junit.Assert.*
import org.junit.Test

class SpaceCompassObserverWeatherTest {
    private val now = Instant.parse("2026-10-06T12:00:00Z").toEpochMilli()
    private val tile = spaceCompassSunWeatherTile(28.379590849, -14.0625)
    private fun request(iso: String) = spaceCompassObserverWeatherRequest(tile, Instant.parse(iso).toEpochMilli(), now)

    @Test fun selectedLocalTimeUsesTheCorrectUtcDateAndHour() {
        val selected = LocalDateTime.parse("2026-10-07T00:15:00").atZone(ZoneId.of("Pacific/Kiritimati")).toInstant().toEpochMilli()
        val result = requireNotNull(spaceCompassObserverWeatherRequest(tile, selected, now))
        assertEquals(Instant.parse("2026-10-06T10:00:00Z").toEpochMilli(), result.hourMs)
        assertTrue(result.url().contains("start_date=2026-10-06&end_date=2026-10-06"))
        assertTrue(result.url().contains("timeformat=unixtime&timezone=GMT"))
    }

    @Test fun fractionalTimezoneAndDstUseTheInstantRatherThanWallClock() {
        val nepal = LocalDateTime.parse("2026-10-07T00:15:00").atZone(ZoneId.of("Asia/Kathmandu")).toInstant().toEpochMilli()
        assertEquals(Instant.parse("2026-10-06T18:00:00Z").toEpochMilli(),
            requireNotNull(spaceCompassObserverWeatherRequest(tile, nepal, now)).hourMs)
        val before = LocalDateTime.parse("2026-03-29T01:30:00").atZone(ZoneId.of("Europe/Rome")).toInstant().toEpochMilli()
        val after = LocalDateTime.parse("2026-03-29T03:30:00").atZone(ZoneId.of("Europe/Rome")).toInstant().toEpochMilli()
        assertEquals(SPACE_COMPASS_WEATHER_HOUR_MS, requireNotNull(spaceCompassObserverWeatherRequest(tile, after, now)).hourMs -
            requireNotNull(spaceCompassObserverWeatherRequest(tile, before, now)).hourMs)
    }

    @Test fun unsupportedFutureOrPreArchiveDatesNeverRequestCurrentWeather() {
        assertNotNull(request("2026-10-21T23:59:59Z"))
        assertNull(request("2026-10-22T00:00:00Z"))
        assertNull(request("2027-04-06T20:00:00Z"))
        assertNull(request("1939-12-31T23:59:59Z"))
        assertNotNull(request("1940-01-01T00:00:00Z"))
    }

    @Test fun recentPastUsesForecastAndOlderDatesUseHistoricalReanalysis() {
        assertFalse(requireNotNull(request("2026-10-01T00:00:00Z")).historical)
        val archive = requireNotNull(request("2026-09-30T23:59:00Z"))
        assertTrue(archive.historical)
        assertTrue(archive.url().startsWith("https://archive-api.open-meteo.com/v1/archive?"))
        assertTrue(requireNotNull(request("2026-10-07T00:00:00Z")).url().startsWith("https://api.open-meteo.com/v1/forecast?"))
    }

    @Test fun urlsOnlySendRoundedLocationAndRequestedDayRegardlessOfNumberLocale() {
        val previous = Locale.getDefault()
        try {
            Locale.setDefault(Locale.ITALIAN)
            val url = requireNotNull(request("2026-10-07T20:30:00Z")).url()
            assertTrue(url.contains("latitude=28.38&longitude=-14.06"))
            assertTrue(url.contains("hourly=weather_code,cloud_cover"))
            assertFalse(url.contains("379590"))
            assertFalse(url.contains("altitude"))
            assertFalse(url.contains("device"))
        } finally { Locale.setDefault(previous) }
    }

    @Test fun missingMalformedOrUnknownValuesDoNotInventClearConditions() {
        val r = requireNotNull(request("2026-10-07T20:30:00Z"))
        val seconds = r.hourMs / 1000.0
        assertNull(spaceCompassObserverWeatherSnapshot(r, null, 0.0, 0.0))
        assertNull(spaceCompassObserverWeatherSnapshot(r, seconds, null, 0.0))
        assertNull(spaceCompassObserverWeatherSnapshot(r, seconds, 0.0, null))
        for (code in listOf(-1.0, 100.0, 2.5, Double.NaN))
            assertNull(spaceCompassObserverWeatherSnapshot(r, seconds, code, 20.0))
        for (cover in listOf(-.01, 100.01, Double.NaN, Double.POSITIVE_INFINITY))
            assertNull(spaceCompassObserverWeatherSnapshot(r, seconds, 2.0, cover))
    }

    @Test fun anotherHourIsRejectedAndTheChosenHourExpiresAtItsBoundary() {
        val r = requireNotNull(request("2026-10-07T20:30:00Z"))
        assertNull(spaceCompassObserverWeatherSnapshot(r, r.hourMs / 1000.0 - 3600, 61.0, 83.0))
        assertNull(spaceCompassObserverWeatherSnapshot(r, r.hourMs / 1000.0 + 3600, 61.0, 83.0))
        val weather = requireNotNull(spaceCompassObserverWeatherSnapshot(r, r.hourMs / 1000.0, 61.0, 83.0))
        assertEquals(SpaceCompassSunWeatherKind.RAIN, weather.kind)
        assertEquals(.83f, weather.cloudCover, .0001f)
        assertTrue(spaceCompassSunWeatherSnapshotUsable(weather, r.hourMs))
        assertTrue(spaceCompassSunWeatherSnapshotUsable(weather, r.hourMs + SPACE_COMPASS_WEATHER_HOUR_MS - 1))
        assertFalse(spaceCompassSunWeatherSnapshotUsable(weather, r.hourMs - 1))
        assertFalse(spaceCompassSunWeatherSnapshotUsable(weather, r.hourMs + SPACE_COMPASS_WEATHER_HOUR_MS))
    }

    @Test fun historicalNegativeAndZeroEpochTimesAreUsableForTheirSelectedHour() {
        for (iso in listOf("1940-01-01T00:20:00Z", "1969-12-31T23:50:00Z", "1970-01-01T00:15:00Z")) {
            val r = requireNotNull(request(iso))
            val weather = requireNotNull(spaceCompassObserverWeatherSnapshot(r, r.hourMs / 1000.0, 71.0, 90.0))
            assertEquals(SpaceCompassSunWeatherKind.SNOW, weather.kind)
            assertTrue(spaceCompassSunWeatherSnapshotUsable(weather, Instant.parse(iso).toEpochMilli()))
        }
    }

    @Test fun liveSnapshotsStillRejectStaleConditionsWhileSimulationUsesItsOwnHour() {
        val live = SpaceCompassSunWeatherSnapshot(SpaceCompassSunWeatherKind.CLEAR, 0f, now)
        assertTrue(spaceCompassSunWeatherSnapshotUsable(live, now))
        assertFalse(spaceCompassSunWeatherSnapshotUsable(live, now + 91 * 60_000L))
        val r = requireNotNull(request("2020-01-01T12:30:00Z"))
        val historical = requireNotNull(spaceCompassObserverWeatherSnapshot(r, r.hourMs / 1000.0, 3.0, 95.0))
        assertFalse(spaceCompassSunWeatherSnapshotUsable(historical, now))
        assertTrue(spaceCompassSunWeatherSnapshotUsable(historical, Instant.parse("2020-01-01T12:30:00Z").toEpochMilli()))
    }

    @Test fun missingWeatherDrawsClearBaseWithoutCloudsAndAllowsNightStars() {
        assertEquals(0f, spaceCompassSunDisplayCloudCover(null), 0f)
        assertEquals(0, spaceCompassSunDisplayCloudCount(null))
        assertTrue(spaceCompassSunDisplayStars(null))
        assertFalse(spaceCompassSunDisplayStars(SpaceCompassSunWeatherSnapshot(SpaceCompassSunWeatherKind.FOG, .1f, now)))
        assertFalse(spaceCompassSunDisplayStars(SpaceCompassSunWeatherSnapshot(SpaceCompassSunWeatherKind.CLOUDY, .9f, now)))
        assertEquals(0, spaceCompassSunDisplayCloudCount(SpaceCompassSunWeatherSnapshot(SpaceCompassSunWeatherKind.CLEAR, .9f, now)))
    }
}
