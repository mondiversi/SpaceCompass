package me.mondiversi.spacecompass

import android.content.Context
import android.location.Geocoder
import android.os.Build
import kotlinx.coroutines.*
import org.json.JSONObject
import java.util.Locale
import kotlin.coroutines.resume

internal data class SpaceCompassObserverMetadata(val altitude: Double, val zoneId: String)

/** Explicit place selection only; bounded HTTPS, fixed provider, no coordinate-bearing error logs. */
internal suspend fun lookupSpaceCompassObserverMetadata(latitude: Double, longitude: Double): SpaceCompassObserverMetadata =
    withTimeout(12_000L) {
        require(latitude.isFinite() && latitude in -90.0..90.0 && longitude.isFinite() && longitude in -180.0..180.0)
        val json = JSONObject(fetchSpaceCompassCelestialText(
            "https://api.open-meteo.com/v1/forecast?latitude=$latitude&longitude=$longitude&timezone=auto&forecast_days=0", 8192))
        val altitude = json.getDouble("elevation")
        val zone = java.time.ZoneId.of(json.getString("timezone"))
        require(altitude.isFinite() && altitude in -500.0..20_000.0)
        SpaceCompassObserverMetadata(altitude, zone.id)
    }

@Suppress("DEPRECATION")
internal suspend fun searchSpaceCompassObserverPlace(context: Context, query: String): SpaceCompassDevicePlace? =
    withTimeoutOrNull(10_000L) {
        require(query.isNotBlank() && query.length <= 200)
        if (!Geocoder.isPresent()) return@withTimeoutOrNull null
        val geocoder = Geocoder(context, Locale.getDefault())
        val addresses = if (Build.VERSION.SDK_INT >= 33) suspendCancellableCoroutine { continuation ->
            geocoder.getFromLocationName(query, 1, object : Geocoder.GeocodeListener {
                override fun onGeocode(addresses: MutableList<android.location.Address>) {
                    if (continuation.isActive) continuation.resume(addresses)
                }
                override fun onError(errorMessage: String?) {
                    if (continuation.isActive) continuation.resume(emptyList<android.location.Address>())
                }
            })
        } else runInterruptible(Dispatchers.IO) { geocoder.getFromLocationName(query, 1).orEmpty() }
        addresses.firstOrNull()?.takeIf { it.hasLatitude() && it.hasLongitude() }?.let {
            SpaceCompassDevicePlace(it.latitude, it.longitude, null)
        }
    }
