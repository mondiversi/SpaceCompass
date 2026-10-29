package me.mondiversi.spacecompass

import android.content.SharedPreferences

internal const val SPACE_COMPASS_CELESTIAL_FAVORITES_KEY = "celestial_favorites"

/** Stable catalogue IDs keep favorites independent of language, scene checks and the active object. */
internal fun restoreSpaceCompassCelestialFavorites(names: Set<String>?): Set<SpaceCompassCelestialBody> =
    spaceCompassAllCelestialOrder.filter { it.name in names.orEmpty() }.toSet()

internal fun readSpaceCompassCelestialFavorites(preferences: SharedPreferences?): Set<SpaceCompassCelestialBody> =
    restoreSpaceCompassCelestialFavorites(preferences?.getStringSet(SPACE_COMPASS_CELESTIAL_FAVORITES_KEY, null))

/** Read the latest set for every action, preserving other favorites and never mutating Android's set. */
internal fun toggleSpaceCompassCelestialFavorite(preferences: SharedPreferences?,
    body: SpaceCompassCelestialBody): Set<SpaceCompassCelestialBody> {
    val current = readSpaceCompassCelestialFavorites(preferences)
    if (body !in spaceCompassAllCelestialOrder) return current
    val updated = if (body in current) current - body else current + body
    preferences?.edit()?.putStringSet(SPACE_COMPASS_CELESTIAL_FAVORITES_KEY,
        updated.map { it.name }.toSet())?.apply()
    return updated
}
