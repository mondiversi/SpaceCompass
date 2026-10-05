package me.mondiversi.spacecompass

import android.content.SharedPreferences
import androidx.compose.runtime.*

/** Only presentation preferences participate in configuration and settings UI updates. */
internal val spaceCompassPresentationKeys = setOf(
    "theme", "language", "display", SPACE_COMPASS_DISTANCE_SPEED_UNIT_KEY, SPACE_COMPASS_MASS_UNIT_KEY, "temperature", "coordinates",
    SPACE_COMPASS_NUMERIC_FORMAT_KEY, SPACE_COMPASS_DATE_FORMAT_KEY, SPACE_COMPASS_TIME_FORMAT_KEY
)

internal fun spaceCompassReadPresentationSettings(read: (String) -> String?): Map<String, String?> =
    spaceCompassPresentationKeys.associateWith(read)

internal val LocalSpaceCompassPresentationSettings = staticCompositionLocalOf<Map<String, String?>> { emptyMap() }

/** One lifecycle-scoped observer shared by the provider and every settings row. */
@Composable
internal fun rememberSpaceCompassPresentationSettings(preferences: SharedPreferences): State<Map<String, String?>> {
    fun read() = spaceCompassReadPresentationSettings { preferences.getString(it, null) }
    val state = remember(preferences) { mutableStateOf(read()) }
    DisposableEffect(preferences) {
        val listener = SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
            if (key == null || key in spaceCompassPresentationKeys) state.value = read()
        }
        preferences.registerOnSharedPreferenceChangeListener(listener)
        // Capture writes between initial composition and listener registration.
        state.value = read()
        onDispose { preferences.unregisterOnSharedPreferenceChangeListener(listener) }
    }
    return state
}
