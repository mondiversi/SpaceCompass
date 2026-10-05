package me.mondiversi.spacecompass

import java.util.Locale
import kotlin.math.round

internal data class SpaceCompassPlaceParts(
    val locality: String? = null, val subLocality: String? = null,
    val subAdminArea: String? = null, val adminArea: String? = null,
    val country: String? = null
)

private fun String?.placeName(): String? = this?.replace(Regex("[\\s\u00a0\u202f]+"), " ")
    ?.trim()?.takeIf { it.isNotEmpty() }

/** Administrative names only: never synthesize a city or expose a street/house number. */
internal fun formatSpaceCompassEstimatedPlace(parts: SpaceCompassPlaceParts): String? {
    val city = parts.locality.placeName() ?: parts.subAdminArea.placeName() ?: parts.subLocality.placeName()
    return listOfNotNull(city, parts.adminArea.placeName(), parts.country.placeName())
        .distinctBy { it.lowercase(Locale.ROOT) }.takeIf { it.isNotEmpty() }?.joinToString("\n")
}

internal data class SpaceCompassPlaceKey(val latitude: Double, val longitude: Double, val languageTag: String)

/** Approximately 100 m cells prevent sensor jitter from triggering repeated place lookups. */
internal fun spaceCompassPlaceKey(latitude: Double?, longitude: Double?, languageTag: String): SpaceCompassPlaceKey? {
    if (latitude == null || longitude == null || !latitude.isFinite() || !longitude.isFinite() ||
        latitude !in -90.0..90.0 || longitude !in -180.0..180.0) return null
    fun cell(value: Double) = (round(value * 1_000.0) / 1_000.0).let { if (it == 0.0) 0.0 else it }
    return SpaceCompassPlaceKey(cell(latitude), cell(longitude), languageTag.ifBlank { "und" })
}

internal const val SPACE_COMPASS_PLACE_SUCCESS_TTL_MS = 30 * 60_000L
internal const val SPACE_COMPASS_PLACE_RETRY_MS = 60_000L
internal data class SpaceCompassPlaceCacheEntry(val text: String?, val expiresElapsed: Long)

/** Small memory-only cache; different locations/languages never share a label. */
internal class SpaceCompassPlaceCache(private val capacity: Int = 16) {
    init { require(capacity > 0) }
    private val entries = LinkedHashMap<SpaceCompassPlaceKey, SpaceCompassPlaceCacheEntry>(capacity, .75f, true)

    fun get(key: SpaceCompassPlaceKey, elapsed: Long): SpaceCompassPlaceCacheEntry? {
        val entry = entries[key] ?: return null
        if (elapsed >= entry.expiresElapsed) {
            entries.remove(key)
            return null
        }
        return entry
    }

    fun put(key: SpaceCompassPlaceKey, text: String?, elapsed: Long): SpaceCompassPlaceCacheEntry {
        val value = text?.takeIf { it.isNotBlank() }
        val entry = SpaceCompassPlaceCacheEntry(value,
            elapsed + if (value != null) SPACE_COMPASS_PLACE_SUCCESS_TTL_MS else SPACE_COMPASS_PLACE_RETRY_MS)
        entries[key] = entry
        while (entries.size > capacity) entries.remove(entries.keys.first())
        return entry
    }
}
