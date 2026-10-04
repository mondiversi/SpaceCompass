package me.mondiversi.planetcompass

import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URI
import kotlin.coroutines.coroutineContext

internal data class PlanetCompassSunWeatherReading(
    val snapshot: PlanetCompassSunWeatherSnapshot? = null, val loading: Boolean = false
)

internal fun parsePlanetCompassSunWeather(json: String, nowMs: Long): PlanetCompassSunWeatherSnapshot {
    val current = JSONObject(json).getJSONObject("current")
    val codeNumber = current.getDouble("weather_code")
    require(codeNumber.isFinite() && codeNumber == codeNumber.toInt().toDouble()) { "Invalid weather code" }
    val kind = requireNotNull(planetCompassSunWeatherKind(codeNumber.toInt())) { "Unknown weather code" }
    val cover = current.getDouble("cloud_cover")
    require(cover.isFinite() && cover in 0.0..100.0) { "Invalid cloud cover" }
    val seconds = current.getDouble("time")
    require(seconds.isFinite() && seconds > 0 && seconds < Long.MAX_VALUE / 1000.0) { "Invalid weather time" }
    val modelTimeMs = (seconds * 1000).toLong()
    require(planetCompassSunWeatherTimeUsable(modelTimeMs, nowMs)) { "Stale weather data" }
    return PlanetCompassSunWeatherSnapshot(kind, (cover / 100).toFloat(), modelTimeMs)
}

/** Bounded HTTPS request, no redirects, no location in error logs, no camera or device commands. */
internal suspend fun fetchPlanetCompassSunWeather(tile: PlanetCompassSunWeatherTile): PlanetCompassSunWeatherSnapshot = withContext(Dispatchers.IO) {
    val connection = URI(tile.url()).toURL().openConnection() as HttpURLConnection
    try {
        connection.connectTimeout = 8_000
        connection.readTimeout = 8_000
        connection.instanceFollowRedirects = false
        connection.setRequestProperty("User-Agent", "PlanetCompass/${BuildConfig.VERSION_NAME}")
        connection.setRequestProperty("Accept", "application/json")
        require(connection.responseCode == 200) { "Weather HTTP ${connection.responseCode}" }
        require(connection.contentLengthLong <= 32_768) { "Weather response too large" }
        val body = connection.inputStream.use { input ->
            val output = java.io.ByteArrayOutputStream()
            val buffer = ByteArray(4096)
            while (true) {
                coroutineContext.ensureActive()
                val count = input.read(buffer)
                if (count < 0) break
                require(output.size() + count <= 32_768) { "Weather response too large" }
                output.write(buffer, 0, count)
            }
            output.toString("UTF-8")
        }
        parsePlanetCompassSunWeather(body, System.currentTimeMillis())
    } finally { connection.disconnect() }
}

/** Foreground-only, memory-only caching. Changing location never shows the old area's weather. */
@Composable
internal fun rememberPlanetCompassSunWeather(tile: PlanetCompassSunWeatherTile?, resumed: Boolean): PlanetCompassSunWeatherReading {
    val context = LocalContext.current
    var reading by remember(tile) { mutableStateOf(PlanetCompassSunWeatherReading()) }
    var snapshot by remember(tile) { mutableStateOf<PlanetCompassSunWeatherSnapshot?>(null) }
    var nextRequestElapsed by remember(tile) { mutableLongStateOf(0L) }
    var lastRequestElapsed by remember { mutableLongStateOf(-60_000L) }
    var reportedFailure by remember { mutableStateOf(false) }
    LaunchedEffect(tile, resumed) {
        if (tile == null || !resumed) return@LaunchedEffect
        while (true) {
            val cached = snapshot?.takeIf { planetCompassSunWeatherTimeUsable(it.modelTimeMs, System.currentTimeMillis()) }
            reading = PlanetCompassSunWeatherReading(cached, loading = cached == null)
            val wait = maxOf(nextRequestElapsed, lastRequestElapsed + 60_000L) - android.os.SystemClock.elapsedRealtime()
            if (wait > 0) delay(wait)
            // Throttle even while rapidly switching nearby cells or cancelling the screen.
            lastRequestElapsed = android.os.SystemClock.elapsedRealtime()
            try {
                val result = fetchPlanetCompassSunWeather(tile)
                snapshot = result
                reading = PlanetCompassSunWeatherReading(result)
                reportedFailure = false
                nextRequestElapsed = android.os.SystemClock.elapsedRealtime() + PLANET_COMPASS_SUN_WEATHER_REFRESH_MS
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Exception) {
                reading = PlanetCompassSunWeatherReading(cached)
                if (!reportedFailure) {
                    // Exceptions from HTTP libraries may contain a URL with coordinates. Do not persist them.
                    PlanetCompassErrorLog.record(context, "sun_finder:weather",
                        IllegalStateException("Weather unavailable (${error.javaClass.simpleName})"))
                    reportedFailure = true
                }
                nextRequestElapsed = android.os.SystemClock.elapsedRealtime() + PLANET_COMPASS_SUN_WEATHER_RETRY_MS
            }
            delay((nextRequestElapsed - android.os.SystemClock.elapsedRealtime()).coerceAtLeast(1L))
        }
    }
    return reading
}
