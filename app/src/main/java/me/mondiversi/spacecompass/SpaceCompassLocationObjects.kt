package me.mondiversi.spacecompass

import android.content.SharedPreferences

/** Portable scene checks remain separate from favorites and from the observer's location overrides. */
internal fun readSpaceCompassCelestialSelection(preferences: SharedPreferences?): SpaceCompassCelestialSelection =
    restoreSpaceCompassCelestialSelection(preferences?.getStringSet("celestial_selected", null),
        preferences?.getString("celestial_active", null))

internal fun SharedPreferences.Editor.putSpaceCompassCelestialSelection(selection: SpaceCompassCelestialSelection): SharedPreferences.Editor =
    putStringSet("celestial_selected", selection.ordered.map { it.name }.toSet())
        .putString("celestial_active", selection.active?.name)
