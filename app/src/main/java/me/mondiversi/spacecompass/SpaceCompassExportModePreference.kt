package me.mondiversi.spacecompass

import android.content.SharedPreferences
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable

internal data class SpaceCompassExportModePreference(val mode: SpaceCompassPanoramaExportMode,
    val change: (SpaceCompassPanoramaExportMode) -> Unit)

/** One existing preference, observed by retained pages as well as newly opened exports. */
@Composable
internal fun rememberSpaceCompassExportMode(): SpaceCompassExportModePreference {
    val preferences = LocalSpaceCompassPreferences.current
    var key by rememberSaveable(preferences) { mutableStateOf(
        SpaceCompassPanoramaExportMode.fromStored(preferences?.getString(SPACE_COMPASS_PANORAMA_EXPORT_MODE_KEY,null)).key) }
    DisposableEffect(preferences) {
        val listener = SharedPreferences.OnSharedPreferenceChangeListener { saved, changed ->
            if (changed == null || changed == SPACE_COMPASS_PANORAMA_EXPORT_MODE_KEY)
                key = SpaceCompassPanoramaExportMode.fromStored(saved.getString(SPACE_COMPASS_PANORAMA_EXPORT_MODE_KEY,null)).key
        }
        preferences?.registerOnSharedPreferenceChangeListener(listener)
        // A restored composition must use the latest shared preference, not its old saved page state.
        if (preferences != null) key = SpaceCompassPanoramaExportMode.fromStored(
            preferences.getString(SPACE_COMPASS_PANORAMA_EXPORT_MODE_KEY,null)).key
        onDispose { preferences?.unregisterOnSharedPreferenceChangeListener(listener) }
    }
    return SpaceCompassExportModePreference(SpaceCompassPanoramaExportMode.fromStored(key)) { option ->
        key = option.key
        preferences?.edit()?.putString(SPACE_COMPASS_PANORAMA_EXPORT_MODE_KEY,option.key)?.apply()
    }
}
