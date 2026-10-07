package me.mondiversi.spacecompass

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import java.util.Locale

internal const val SPACE_COMPASS_WEATHER_HOUR_MS = 3_600_000L
internal const val SPACE_COMPASS_OBSERVER_ARCHIVE_WEATHER_REFRESH_MS = 24 * 60 * 60_000L

/** Hourly estimates for the selected instant, independent of the phone or observer timezone. */
internal data class SpaceCompassObserverWeatherRequest(
    val tile: SpaceCompassSunWeatherTile, val hourMs: Long, val historical: Boolean
) {
    fun url(): String {
        val date = Instant.ofEpochMilli(hourMs).atZone(ZoneOffset.UTC).toLocalDate()
        val endpoint = if (historical) "https://archive-api.open-meteo.com/v1/archive"
            else "https://api.open-meteo.com/v1/forecast"
        return String.format(Locale.ROOT,
            "%s?latitude=%.2f&longitude=%.2f&hourly=weather_code,cloud_cover" +
                "&start_date=%s&end_date=%s&timeformat=unixtime&timezone=GMT",
            endpoint, tile.latitude, tile.longitude, date, date)
    }
}

/** Never substitute today's conditions for a date outside the provider's coverage. */
internal fun spaceCompassObserverWeatherRequest(tile: SpaceCompassSunWeatherTile, selectedMs: Long,
    nowMs: Long): SpaceCompassObserverWeatherRequest? {
    val today = Instant.ofEpochMilli(nowMs).atZone(ZoneOffset.UTC).toLocalDate()
    val day = Instant.ofEpochMilli(selectedMs).atZone(ZoneOffset.UTC).toLocalDate()
    if (day < LocalDate.of(1940, 1, 1) || day > today.plusDays(15)) return null
    val hourMs = Math.floorDiv(selectedMs, SPACE_COMPASS_WEATHER_HOUR_MS) * SPACE_COMPASS_WEATHER_HOUR_MS
    // Recent dates use the forecast endpoint; older dates use hourly reanalysis.
    return SpaceCompassObserverWeatherRequest(tile, hourMs, historical = day < today.minusDays(5))
}

/** Strictly accept the requested hour; absent/unknown values remain unavailable. */
internal fun spaceCompassObserverWeatherSnapshot(request: SpaceCompassObserverWeatherRequest,
    seconds: Double?, weatherCode: Double?, cloudCover: Double?): SpaceCompassSunWeatherSnapshot? {
    if (seconds == null || !seconds.isFinite() || seconds != request.hourMs / 1000.0) return null
    if (weatherCode == null || !weatherCode.isFinite() || weatherCode != weatherCode.toInt().toDouble()) return null
    val kind = spaceCompassSunWeatherKind(weatherCode.toInt()) ?: return null
    if (cloudCover == null || !cloudCover.isFinite() || cloudCover !in 0.0..100.0) return null
    return SpaceCompassSunWeatherSnapshot(kind, (cloudCover / 100.0).toFloat(), request.hourMs, hourly = true)
}

/** Live freshness and simulated-hour validity are separate, including dates before Unix epoch. */
internal fun spaceCompassSunWeatherSnapshotUsable(weather: SpaceCompassSunWeatherSnapshot, selectedMs: Long): Boolean =
    if (weather.hourly) weather.modelTimeMs ==
        Math.floorDiv(selectedMs, SPACE_COMPASS_WEATHER_HOUR_MS) * SPACE_COMPASS_WEATHER_HOUR_MS
    else spaceCompassSunWeatherTimeUsable(weather.modelTimeMs, selectedMs)

/** Missing estimates draw the same clear base sky in the live viewport and exported panorama. */
internal fun spaceCompassSunDisplayStars(weather: SpaceCompassSunWeatherSnapshot?): Boolean =
    spaceCompassSunDisplayCloudCover(weather) < .65f && weather?.kind != SpaceCompassSunWeatherKind.FOG
