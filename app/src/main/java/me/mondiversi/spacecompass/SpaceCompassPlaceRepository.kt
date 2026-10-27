package me.mondiversi.spacecompass

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

internal const val SPACE_COMPASS_SAVED_PLACE_TTL_MS = 30L * 24 * 60 * 60_000

/** A locale-specific administrative label; exact street addresses never enter this cache. */
internal data class SpaceCompassSavedPlace(val text: String, val savedWallMs: Long) {
    fun fresh(now: Long) = now >= savedWallMs && now - savedWallMs < SPACE_COMPASS_SAVED_PLACE_TTL_MS
    fun encode() = "$savedWallMs\n$text"
    companion object {
        fun decode(value: String?): SpaceCompassSavedPlace? = runCatching {
            val content = requireNotNull(value)
            val timestamp = content.substringBefore('\n').toLong()
            val text = content.substringAfter('\n', "")
            require(timestamp > 0 && text.isNotBlank() && text.length <= 512 && text.none(Char::isISOControl))
            SpaceCompassSavedPlace(text, timestamp)
        }.getOrNull()
    }
}

internal fun spaceCompassPlaceStorageKey(key: SpaceCompassPlaceKey) =
    "v1|${key.languageTag}|${key.latitude}|${key.longitude}"

/** Shared successful labels survive page/app recreation. Same-key lookups are coalesced before geocoding. */
internal class SpaceCompassPlaceRepository(private val read: (SpaceCompassPlaceKey) -> SpaceCompassSavedPlace?,
    private val write: (SpaceCompassPlaceKey, SpaceCompassSavedPlace) -> Unit,
    private val wall: () -> Long = System::currentTimeMillis,
    private val elapsed: () -> Long = { System.nanoTime() / 1_000_000 },
    private val fetch: suspend (SpaceCompassPlaceKey) -> String?) {
    private val memory = SpaceCompassPlaceCache(64)
    private val request = Mutex()
    private fun available(key: SpaceCompassPlaceKey): SpaceCompassPlaceCacheEntry? =
        memory.get(key, elapsed()) ?: read(key)?.takeIf { it.fresh(wall()) }?.let { memory.put(key, it.text, elapsed()) }

    fun cached(key: SpaceCompassPlaceKey): String? = available(key)?.text ?: read(key)?.text
    fun needsLookup(key: SpaceCompassPlaceKey): Boolean = available(key) == null

    fun seed(key: SpaceCompassPlaceKey, text: String) {
        val clean = text.replace(Regex("[\\s\u00a0\u202f]+"), " ").trim().take(512)
        if (clean.isBlank()) return
        write(key, SpaceCompassSavedPlace(clean, wall()))
        memory.put(key, clean, elapsed())
    }

    suspend fun resolve(key: SpaceCompassPlaceKey): SpaceCompassPlaceCacheEntry {
        available(key)?.let { return it }
        return request.withLock {
            available(key) ?: run {
                val previous = read(key)?.text
                val found = try { fetch(key)?.takeIf { it.isNotBlank() }
                } catch (cancelled: CancellationException) { throw cancelled
                } catch (_: Exception) { null }
                available(key) ?: if (found != null) {
                    seed(key, found)
                    requireNotNull(memory.get(key, elapsed()))
                } else {
                    // A failed refresh does not erase a previously known name or replace it with loading text.
                    memory.put(key, previous, elapsed())
                }
            }
        }
    }
}
