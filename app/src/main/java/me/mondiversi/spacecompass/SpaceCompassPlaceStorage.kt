package me.mondiversi.spacecompass

import android.content.Context
import android.os.SystemClock

private val placeRepositoryLock = Any()
@Volatile private var sharedPlaceRepository: SpaceCompassPlaceRepository? = null

/** Bounded private cache, separate from presentation preferences and scenario configuration. */
internal fun spaceCompassPlaceRepository(context: Context): SpaceCompassPlaceRepository =
    sharedPlaceRepository ?: synchronized(placeRepositoryLock) {
        sharedPlaceRepository ?: run {
            val application = context.applicationContext
            val preferences = application.getSharedPreferences("spaceCompass_place_names", Context.MODE_PRIVATE)
            SpaceCompassPlaceRepository(read = { key ->
                SpaceCompassSavedPlace.decode(preferences.getString(spaceCompassPlaceStorageKey(key), null))
            }, write = { key, record ->
                synchronized(placeRepositoryLock) {
                    val id = spaceCompassPlaceStorageKey(key)
                    val others = preferences.all.filterKeys { it.startsWith("v1|") && it != id }
                        .map { (name, value) -> name to (SpaceCompassSavedPlace.decode(value as? String)?.savedWallMs ?: 0L) }
                        .sortedByDescending { it.second }
                    preferences.edit().putString(id, record.encode()).apply {
                        others.drop(63).forEach { (name, _) -> remove(name) }
                    }.apply()
                }
            }, elapsed = SystemClock::elapsedRealtime,
                fetch = { key -> lookupSpaceCompassEstimatedPlace(application, key) })
                .also { sharedPlaceRepository = it }
        }
    }
