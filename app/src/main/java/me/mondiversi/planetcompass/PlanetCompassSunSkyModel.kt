package me.mondiversi.planetcompass

import java.util.Locale
import kotlin.math.roundToInt

internal enum class PlanetCompassSunSkyPhase { NIGHT, DAWN, MORNING, AFTERNOON, SUNSET, EVENING }
internal enum class PlanetCompassSunWeatherKind { CLEAR, MAINLY_CLEAR, PARTLY_CLOUDY, CLOUDY, FOG, DRIZZLE, RAIN, SNOW, STORM }

/** Solar phases, not the phone's timezone: dawn/sunset follow the actual local solar elevation. */
internal fun planetCompassSunSkyPhase(elevation: Double?, rising: Boolean): PlanetCompassSunSkyPhase = when {
    elevation == null || !elevation.isFinite() -> PlanetCompassSunSkyPhase.NIGHT
    elevation < -18 || (rising && elevation < -12) -> PlanetCompassSunSkyPhase.NIGHT
    elevation < -6 && !rising -> PlanetCompassSunSkyPhase.EVENING
    elevation <= 6 -> if (rising) PlanetCompassSunSkyPhase.DAWN else PlanetCompassSunSkyPhase.SUNSET
    rising -> PlanetCompassSunSkyPhase.MORNING
    else -> PlanetCompassSunSkyPhase.AFTERNOON
}

/** Open-Meteo WMO codes. Unknown codes must never be presented as clear weather. */
internal fun planetCompassSunWeatherKind(code: Int): PlanetCompassSunWeatherKind? = when (code) {
    0 -> PlanetCompassSunWeatherKind.CLEAR
    1 -> PlanetCompassSunWeatherKind.MAINLY_CLEAR
    2 -> PlanetCompassSunWeatherKind.PARTLY_CLOUDY
    3 -> PlanetCompassSunWeatherKind.CLOUDY
    45, 48 -> PlanetCompassSunWeatherKind.FOG
    51, 53, 55, 56, 57 -> PlanetCompassSunWeatherKind.DRIZZLE
    61, 63, 65, 66, 67, 80, 81, 82 -> PlanetCompassSunWeatherKind.RAIN
    71, 73, 75, 77, 85, 86 -> PlanetCompassSunWeatherKind.SNOW
    95, 96, 97, 99 -> PlanetCompassSunWeatherKind.STORM
    else -> null
}

internal data class PlanetCompassSunWeatherTile(val latitudeIndex: Int, val longitudeIndex: Int) {
    val latitude get() = latitudeIndex / 50.0
    val longitude get() = longitudeIndex / 50.0
    fun url(): String = String.format(Locale.ROOT,
        "https://api.open-meteo.com/v1/forecast?latitude=%.2f&longitude=%.2f" +
            "&current=weather_code,cloud_cover&timeformat=unixtime&timezone=GMT&forecast_days=1",
        latitude, longitude)
}

/** Only ~2 km rounded cells leave the phone; neither altitude nor precise coordinates are transmitted. */
internal fun planetCompassSunWeatherTile(latitude: Double, longitude: Double): PlanetCompassSunWeatherTile {
    require(latitude.isFinite() && latitude in -90.0..90.0)
    require(longitude.isFinite() && longitude in -180.0..180.0)
    return PlanetCompassSunWeatherTile((latitude * 50).roundToInt(), (longitude * 50).roundToInt())
}

internal data class PlanetCompassSunWeatherSnapshot(
    val kind: PlanetCompassSunWeatherKind, val cloudCover: Float, val modelTimeMs: Long
)

/** The visual must agree with the displayed WMO condition, even if model fields disagree. */
internal fun planetCompassSunDisplayCloudCover(weather: PlanetCompassSunWeatherSnapshot?): Float {
    val cover = weather?.cloudCover?.takeIf { it.isFinite() }?.coerceIn(0f, 1f) ?: 0f
    return when (weather?.kind) {
        PlanetCompassSunWeatherKind.CLEAR -> 0f
        PlanetCompassSunWeatherKind.MAINLY_CLEAR -> cover.coerceIn(0.05f, 0.18f)
        else -> cover
    }
}
internal fun planetCompassSunDisplayCloudCount(weather: PlanetCompassSunWeatherSnapshot?): Int {
    val cover = planetCompassSunDisplayCloudCover(weather)
    return if (weather == null || cover < 0.05f) 0
        else if (weather.kind == PlanetCompassSunWeatherKind.MAINLY_CLEAR) 1 else (2 + cover * 8).roundToInt()
}

internal const val PLANET_COMPASS_SUN_WEATHER_REFRESH_MS = 15 * 60_000L
internal const val PLANET_COMPASS_SUN_WEATHER_RETRY_MS = 5 * 60_000L
internal fun planetCompassSunWeatherTimeUsable(modelTimeMs: Long, nowMs: Long): Boolean =
    modelTimeMs > 0 && nowMs - modelTimeMs in -30 * 60_000L..90 * 60_000L

/** Eight compass marks clockwise from north; projection remains geographic in RTL layouts. */
internal val PlanetCompassSunCompassLabels = listOf("N", "NE", "E", "SE", "S", "SW", "W", "NW")
