package me.mondiversi.spacecompass

import java.util.Locale
import kotlin.math.roundToInt

internal enum class SpaceCompassSunSkyPhase { NIGHT, DAWN, MORNING, AFTERNOON, SUNSET, EVENING }
internal enum class SpaceCompassSunWeatherKind { CLEAR, MAINLY_CLEAR, PARTLY_CLOUDY, CLOUDY, FOG, DRIZZLE, RAIN, SNOW, STORM }

/** Solar phases, not the phone's timezone: dawn/sunset follow the actual local solar elevation. */
internal fun spaceCompassSunSkyPhase(elevation: Double?, rising: Boolean): SpaceCompassSunSkyPhase = when {
    elevation == null || !elevation.isFinite() -> SpaceCompassSunSkyPhase.NIGHT
    elevation < -18 || (rising && elevation < -12) -> SpaceCompassSunSkyPhase.NIGHT
    elevation < -6 && !rising -> SpaceCompassSunSkyPhase.EVENING
    elevation <= 6 -> if (rising) SpaceCompassSunSkyPhase.DAWN else SpaceCompassSunSkyPhase.SUNSET
    rising -> SpaceCompassSunSkyPhase.MORNING
    else -> SpaceCompassSunSkyPhase.AFTERNOON
}

/** Open-Meteo WMO codes. Unknown codes must never be presented as clear weather. */
internal fun spaceCompassSunWeatherKind(code: Int): SpaceCompassSunWeatherKind? = when (code) {
    0 -> SpaceCompassSunWeatherKind.CLEAR
    1 -> SpaceCompassSunWeatherKind.MAINLY_CLEAR
    2 -> SpaceCompassSunWeatherKind.PARTLY_CLOUDY
    3 -> SpaceCompassSunWeatherKind.CLOUDY
    45, 48 -> SpaceCompassSunWeatherKind.FOG
    51, 53, 55, 56, 57 -> SpaceCompassSunWeatherKind.DRIZZLE
    61, 63, 65, 66, 67, 80, 81, 82 -> SpaceCompassSunWeatherKind.RAIN
    71, 73, 75, 77, 85, 86 -> SpaceCompassSunWeatherKind.SNOW
    95, 96, 97, 99 -> SpaceCompassSunWeatherKind.STORM
    else -> null
}

internal data class SpaceCompassSunWeatherTile(val latitudeIndex: Int, val longitudeIndex: Int) {
    val latitude get() = latitudeIndex / 50.0
    val longitude get() = longitudeIndex / 50.0
    fun url(): String = String.format(Locale.ROOT,
        "https://api.open-meteo.com/v1/forecast?latitude=%.2f&longitude=%.2f" +
            "&current=weather_code,cloud_cover&timeformat=unixtime&timezone=GMT&forecast_days=1",
        latitude, longitude)
}

/** Only ~2 km rounded cells leave the phone; neither altitude nor precise coordinates are transmitted. */
internal fun spaceCompassSunWeatherTile(latitude: Double, longitude: Double): SpaceCompassSunWeatherTile {
    require(latitude.isFinite() && latitude in -90.0..90.0)
    require(longitude.isFinite() && longitude in -180.0..180.0)
    return SpaceCompassSunWeatherTile((latitude * 50).roundToInt(), (longitude * 50).roundToInt())
}

internal data class SpaceCompassSunWeatherSnapshot(
    val kind: SpaceCompassSunWeatherKind, val cloudCover: Float, val modelTimeMs: Long, val hourly: Boolean = false
)

/** The visual must agree with the displayed WMO condition, even if model fields disagree. */
internal fun spaceCompassSunDisplayCloudCover(weather: SpaceCompassSunWeatherSnapshot?): Float {
    val cover = weather?.cloudCover?.takeIf { it.isFinite() }?.coerceIn(0f, 1f) ?: 0f
    return when (weather?.kind) {
        SpaceCompassSunWeatherKind.CLEAR -> 0f
        SpaceCompassSunWeatherKind.MAINLY_CLEAR -> cover.coerceIn(0.05f, 0.18f)
        SpaceCompassSunWeatherKind.CLOUDY -> cover.coerceAtLeast(.75f)
        else -> cover
    }
}
internal fun spaceCompassSunDisplayCloudCount(weather: SpaceCompassSunWeatherSnapshot?): Int {
    val cover = spaceCompassSunDisplayCloudCover(weather)
    return if (weather == null || cover < 0.05f) 0
        else if (weather.kind == SpaceCompassSunWeatherKind.MAINLY_CLEAR) 1 else (2 + cover * 8).roundToInt()
}

internal const val SPACE_COMPASS_SUN_WEATHER_REFRESH_MS = 15 * 60_000L
internal const val SPACE_COMPASS_SUN_WEATHER_RETRY_MS = 5 * 60_000L
internal fun spaceCompassSunWeatherTimeUsable(modelTimeMs: Long, nowMs: Long): Boolean =
    modelTimeMs > 0 && nowMs - modelTimeMs in -30 * 60_000L..90 * 60_000L

/** Eight compass marks clockwise from north; projection remains geographic in RTL layouts. */
internal val SpaceCompassSunCompassLabels = listOf("N", "NE", "E", "SE", "S", "SW", "W", "NW")
