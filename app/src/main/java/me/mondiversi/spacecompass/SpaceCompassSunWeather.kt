package me.mondiversi.spacecompass

import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URI
import kotlin.coroutines.coroutineContext

internal data class SpaceCompassSunWeatherReading(
    val snapshot: SpaceCompassSunWeatherSnapshot? = null, val loading: Boolean = false,
    val failed: Boolean = false, val onRetry: () -> Unit = {}
)

internal fun parseSpaceCompassSunWeather(json: String, nowMs: Long): SpaceCompassSunWeatherSnapshot {
    val current = JSONObject(json).getJSONObject("current")
    val codeNumber = current.getDouble("weather_code")
    require(codeNumber.isFinite() && codeNumber == codeNumber.toInt().toDouble()) { "Invalid weather code" }
    val kind = requireNotNull(spaceCompassSunWeatherKind(codeNumber.toInt())) { "Unknown weather code" }
    val cover = current.getDouble("cloud_cover")
    require(cover.isFinite() && cover in 0.0..100.0) { "Invalid cloud cover" }
    val seconds = current.getDouble("time")
    require(seconds.isFinite() && seconds > 0 && seconds < Long.MAX_VALUE / 1000.0) { "Invalid weather time" }
    val modelTimeMs = (seconds * 1000).toLong()
    require(spaceCompassSunWeatherTimeUsable(modelTimeMs, nowMs)) { "Stale weather data" }
    return SpaceCompassSunWeatherSnapshot(kind, (cover / 100).toFloat(), modelTimeMs)
}

/** Reject missing/misaligned hourly arrays rather than using another date or nearby hour. */
internal fun parseSpaceCompassObserverWeather(json: String, request: SpaceCompassObserverWeatherRequest): SpaceCompassSunWeatherSnapshot {
    val hourly = JSONObject(json).getJSONObject("hourly")
    val times = hourly.getJSONArray("time")
    val codes = hourly.getJSONArray("weather_code")
    val covers = hourly.getJSONArray("cloud_cover")
    require(times.length() in 1..24 && codes.length() == times.length() && covers.length() == times.length()) {
        "Invalid hourly weather arrays"
    }
    val index = (0 until times.length()).firstOrNull { !times.isNull(it) && times.getDouble(it) == request.hourMs / 1000.0 }
    require(index != null) { "Requested weather hour unavailable" }
    return requireNotNull(spaceCompassObserverWeatherSnapshot(request, times.getDouble(index),
        codes.getDouble(index), covers.getDouble(index))) { "Invalid hourly weather values" }
}

/** Bounded HTTPS request, no redirects, no location in error logs, no camera or device commands. */
internal suspend fun fetchSpaceCompassSunWeather(tile: SpaceCompassSunWeatherTile, request: SpaceCompassObserverWeatherRequest? = null): SpaceCompassSunWeatherSnapshot = withContext(Dispatchers.IO) { coroutineScope {
    val connection = URI(request?.url() ?: tile.url()).toURL().openConnection() as HttpURLConnection
    // A total deadline also stops slow-drip responses that never hit the per-read timeout.
    val deadline = System.nanoTime() + 18_000_000_000L
    val watchdog = launch {
        delay(18_000L)
        connection.disconnect()
    }
    try {
        connection.connectTimeout = 8_000
        connection.readTimeout = 8_000
        connection.instanceFollowRedirects = false
        connection.setRequestProperty("User-Agent", "SpaceCompass/${BuildConfig.VERSION_NAME}")
        connection.setRequestProperty("Accept", "application/json")
        require(connection.responseCode == 200) { "Weather HTTP ${connection.responseCode}" }
        require(connection.contentLengthLong <= 32_768) { "Weather response too large" }
        val body = connection.inputStream.use { input ->
            val output = java.io.ByteArrayOutputStream()
            val buffer = ByteArray(4096)
            while (true) {
                coroutineContext.ensureActive()
                val remaining = (deadline - System.nanoTime()) / 1_000_000L
                if (remaining <= 0) throw java.net.SocketTimeoutException("Weather request timed out")
                connection.readTimeout = remaining.coerceIn(1L, 8_000L).toInt()
                val count = input.read(buffer)
                if (count < 0) break
                require(output.size() + count <= 32_768) { "Weather response too large" }
                output.write(buffer, 0, count)
            }
            output.toString("UTF-8")
        }
        if (request == null) parseSpaceCompassSunWeather(body, System.currentTimeMillis())
        else parseSpaceCompassObserverWeather(body, request)
    } finally { watchdog.cancel(); connection.disconnect() }
} }

/** Foreground-only, memory-only caching. Changing location never shows the old area's weather. */
@Composable
internal fun rememberSpaceCompassSunWeather(tile: SpaceCompassSunWeatherTile?, resumed: Boolean,
    selectedTimeMs: Long? = null): SpaceCompassSunWeatherReading {
    val context = LocalContext.current
    val today = java.time.LocalDate.now(java.time.ZoneOffset.UTC)
    val request = remember(tile, selectedTimeMs, today) {
        if (tile == null || selectedTimeMs == null) null
        else spaceCompassObserverWeatherRequest(tile, selectedTimeMs, System.currentTimeMillis())
    }
    val canRequest = tile != null && resumed && (selectedTimeMs == null || request != null)
    var reading by remember(tile, selectedTimeMs, request) {
        mutableStateOf(SpaceCompassSunWeatherReading(loading = canRequest))
    }
    var snapshot by remember(tile, selectedTimeMs, request) { mutableStateOf<SpaceCompassSunWeatherSnapshot?>(null) }
    var nextRequestElapsed by remember(tile, selectedTimeMs, request) { mutableLongStateOf(0L) }
    var lastRequestElapsed by remember { mutableLongStateOf(-60_000L) }
    var reportedFailure by remember(tile, selectedTimeMs, request) { mutableStateOf(false) }
    var retryRevision by remember { mutableIntStateOf(0) }
    val onRetry: () -> Unit = {
        nextRequestElapsed = 0L
        lastRequestElapsed = android.os.SystemClock.elapsedRealtime() - 60_000L
        retryRevision++
    }
    LaunchedEffect(tile, selectedTimeMs, request, resumed, retryRevision) {
        if (tile == null || !resumed || (selectedTimeMs != null && request == null)) return@LaunchedEffect
        while (true) {
            val cached = snapshot?.takeIf { spaceCompassSunWeatherSnapshotUsable(it, selectedTimeMs ?: System.currentTimeMillis()) }
            val wait = maxOf(nextRequestElapsed, lastRequestElapsed + 60_000L) - android.os.SystemClock.elapsedRealtime()
            if (wait > 0) {
                // A queued first request is still loading; a failed request waiting for retry is not.
                reading = SpaceCompassSunWeatherReading(cached, loading = cached == null && !reportedFailure,
                    failed = reportedFailure && cached == null)
                delay(wait)
            }
            reading = SpaceCompassSunWeatherReading(cached, loading = cached == null)
            // Throttle even while rapidly switching nearby cells or cancelling the screen.
            lastRequestElapsed = android.os.SystemClock.elapsedRealtime()
            try {
                val result = fetchSpaceCompassSunWeather(tile, request)
                coroutineContext.ensureActive()
                snapshot = result
                reading = SpaceCompassSunWeatherReading(result)
                reportedFailure = false
                nextRequestElapsed = android.os.SystemClock.elapsedRealtime() +
                    if (request?.historical == true) SPACE_COMPASS_OBSERVER_ARCHIVE_WEATHER_REFRESH_MS else SPACE_COMPASS_SUN_WEATHER_REFRESH_MS
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Exception) {
                reading = SpaceCompassSunWeatherReading(cached, failed = cached == null)
                if (!reportedFailure) {
                    // Exceptions from HTTP libraries may contain a URL with coordinates. Do not persist them.
                    SpaceCompassErrorLog.record(context, "sun_finder:weather",
                        IllegalStateException("Weather unavailable (${error.javaClass.simpleName})"))
                    reportedFailure = true
                }
                nextRequestElapsed = android.os.SystemClock.elapsedRealtime() + SPACE_COMPASS_SUN_WEATHER_RETRY_MS
            }
            delay((nextRequestElapsed - android.os.SystemClock.elapsedRealtime()).coerceAtLeast(1L))
        }
    }
    return reading.copy(loading = canRequest && reading.loading, onRetry = onRetry)
}
