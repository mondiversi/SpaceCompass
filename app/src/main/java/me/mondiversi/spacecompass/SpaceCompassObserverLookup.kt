package me.mondiversi.spacecompass

import kotlinx.coroutines.withTimeout
import org.json.JSONObject

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
