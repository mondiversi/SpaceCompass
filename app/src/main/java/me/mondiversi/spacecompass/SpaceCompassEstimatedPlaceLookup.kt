package me.mondiversi.spacecompass

import android.content.Context
import android.location.Address
import android.location.Geocoder
import android.os.Build
import android.os.SystemClock
import androidx.annotation.RequiresApi
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.runInterruptible
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeoutOrNull
import java.util.Locale
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.coroutines.resume

internal data class SpaceCompassEstimatedPlaceReading(val text: String? = null, val loading: Boolean = false)

@RequiresApi(33)
private suspend fun asynchronousPlaceLookup(geocoder: Geocoder, key: SpaceCompassPlaceKey): List<Address> =
    suspendCancellableCoroutine { continuation ->
        val finished = AtomicBoolean(false)
        continuation.invokeOnCancellation { finished.set(true) }
        geocoder.getFromLocation(key.latitude, key.longitude, 1, object : Geocoder.GeocodeListener {
            override fun onGeocode(addresses: MutableList<Address>) {
                if (finished.compareAndSet(false, true)) continuation.resume(addresses)
            }
            override fun onError(errorMessage: String?) {
                // Provider errors may contain location data; neither log nor display their text.
                if (finished.compareAndSet(false, true)) continuation.resume(emptyList())
            }
        })
    }

@Suppress("DEPRECATION")
private suspend fun legacyPlaceLookup(geocoder: Geocoder, key: SpaceCompassPlaceKey): List<Address> =
    runInterruptible(Dispatchers.IO) { geocoder.getFromLocation(key.latitude, key.longitude, 1).orEmpty() }

/** Android 13+ callbacks, legacy worker-thread calls, and bounded waiting on every version. */
internal suspend fun lookupSpaceCompassEstimatedPlace(context: Context, key: SpaceCompassPlaceKey): String? =
    lookupSpaceCompassEstimatedPlaceParts(context, key)?.let(::formatSpaceCompassEstimatedPlace)

internal suspend fun lookupSpaceCompassEstimatedPlaceParts(context: Context, key: SpaceCompassPlaceKey): SpaceCompassPlaceParts? {
    if (!Geocoder.isPresent()) return null
    val locale = Locale.forLanguageTag(key.languageTag)
    val geocoder = Geocoder(context, locale)
    val address = withTimeoutOrNull(10_000L) {
        if (Build.VERSION.SDK_INT >= 33) asynchronousPlaceLookup(geocoder, key)
        else legacyPlaceLookup(geocoder, key)
    }?.firstOrNull() ?: return null
    val country = address.countryName?.takeIf { it.isNotBlank() }
        ?: address.countryCode?.takeIf { it.matches(Regex("[A-Za-z]{2}")) }
            ?.let { Locale.Builder().setRegion(it).build().getDisplayCountry(locale) }
    return SpaceCompassPlaceParts(address.locality, address.subLocality,
        address.subAdminArea, address.adminArea, country)
}

/** Requests only while environment details are visible and the activity is resumed. */
@Composable
internal fun rememberSpaceCompassEstimatedPlace(latitude: Double?, longitude: Double?, enabled: Boolean): SpaceCompassEstimatedPlaceReading {
    val context = LocalContext.current.applicationContext
    val languageTag = LocalConfiguration.current.locales[0].toLanguageTag()
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    val key = remember(latitude, longitude, languageTag) { spaceCompassPlaceKey(latitude, longitude, languageTag) }
    val cache = remember { SpaceCompassPlaceCache() }
    var reading by remember(key) {
        mutableStateOf(SpaceCompassEstimatedPlaceReading(key?.let { cache.get(it, SystemClock.elapsedRealtime())?.text }))
    }
    var lastRequestElapsed by remember { mutableLongStateOf(-10_000L) }
    LaunchedEffect(key, enabled, lifecycle) {
        if (key == null || !enabled) {
            reading = reading.copy(loading = false)
            return@LaunchedEffect
        }
        lifecycle.repeatOnLifecycle(Lifecycle.State.RESUMED) {
            while (true) {
                val cached = cache.get(key, SystemClock.elapsedRealtime())
                val entry = cached ?: run {
                    reading = SpaceCompassEstimatedPlaceReading(loading = true)
                    // Also throttle requests across rapidly changing location cells/languages.
                    val wait = lastRequestElapsed + 10_000L - SystemClock.elapsedRealtime()
                    if (wait > 0) delay(wait)
                    delay(250L)
                    lastRequestElapsed = SystemClock.elapsedRealtime()
                    val result = try {
                        lookupSpaceCompassEstimatedPlace(context, key)
                    } catch (cancelled: CancellationException) {
                        throw cancelled
                    } catch (_: Exception) {
                        null
                    }
                    cache.put(key, result, SystemClock.elapsedRealtime())
                }
                reading = SpaceCompassEstimatedPlaceReading(entry.text)
                delay((entry.expiresElapsed - SystemClock.elapsedRealtime()).coerceAtLeast(1L))
            }
        }
    }
    return reading
}
