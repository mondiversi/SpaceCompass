package me.mondiversi.spacecompass

import androidx.test.ext.junit.runners.AndroidJUnit4
import java.time.Instant
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class SpaceCompassObserverWeatherAndroidTest {
    private val tile = spaceCompassSunWeatherTile(28.38, -14.06)
    private val hour = Instant.parse("2026-10-07T20:00:00Z").toEpochMilli()
    private val request = SpaceCompassObserverWeatherRequest(tile, hour, historical = false)
    private fun body(times: String, codes: String, covers: String) =
        """{"hourly":{"time":[$times],"weather_code":[$codes],"cloud_cover":[$covers]}}"""

    @Test fun selectsTheRequestedHourInsteadOfFirstCurrentOrNeighboringConditions() {
        val seconds = hour / 1000
        val result = parseSpaceCompassObserverWeather(body("${seconds - 3600},$seconds,${seconds + 3600}",
            "61,2,71", "83,53,90"), request)
        assertEquals(SpaceCompassSunWeatherKind.PARTLY_CLOUDY, result.kind)
        assertEquals(.53f, result.cloudCover, .0001f)
        assertEquals(hour, result.modelTimeMs)
        assertTrue(result.hourly)
    }

    @Test fun missingNullUnknownAndMisalignedDataStayUnavailable() {
        val seconds = hour / 1000
        val invalid = listOf(
            body("${seconds - 3600}", "0", "0"),
            body("$seconds", "null", "40"), body("$seconds", "61", "null"),
            body("$seconds", "100", "40"), body("$seconds", "2.5", "40"),
            body("$seconds", "61", "101"), body("$seconds", "61,71", "40"),
            """{"current":{"time":$seconds,"weather_code":0,"cloud_cover":0}}"""
        )
        for (json in invalid) assertThrows(Exception::class.java) { parseSpaceCompassObserverWeather(json, request) }
    }

    @Test fun historicalDatesBeforeAndAtUnixEpochRemainValid() {
        for (iso in listOf("1940-01-01T00:00:00Z", "1970-01-01T00:00:00Z")) {
            val model = Instant.parse(iso).toEpochMilli()
            val historic = SpaceCompassObserverWeatherRequest(tile, model, historical = true)
            val result = parseSpaceCompassObserverWeather(body("${model / 1000}", "71", "90"), historic)
            assertEquals(model, result.modelTimeMs)
            assertEquals(SpaceCompassSunWeatherKind.SNOW, result.kind)
            assertTrue(spaceCompassSunWeatherSnapshotUsable(result, model + 30 * 60_000L))
        }
    }
}
