package me.mondiversi.spacecompass

import android.content.SharedPreferences
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource

internal val LocalSpaceCompassSettingsExitFailure = staticCompositionLocalOf<() -> Unit> { {} }

private fun settingsNoticeValues(preferences: SharedPreferences?): Map<String, String> {
    val keys = spaceCompassPresentationKeys + setOf(
        SPACE_COMPASS_MUSIC_ENABLED_KEY, SPACE_COMPASS_MUSIC_VOLUME_KEY,
        SPACE_COMPASS_SCREENSAVER_ENABLED_KEY, SPACE_COMPASS_SCREENSAVER_DELAY_KEY,
        SPACE_COMPASS_OBSERVER_PLAN, SPACE_COMPASS_OBSERVER_MODE,
        "scenario_speed_enabled", "scenario_speed_rate", "scenario_resume"
    )
    return preferences?.all.orEmpty().filterKeys { it in keys }.mapValues { it.value.toString() }
}

private val settingsExitSaver = listSaver<SpaceCompassSettingsExitConfirmation, Any>(
    save = { listOf(it.page.orEmpty(), HashMap(it.baseline)) },
    restore = {
        @Suppress("UNCHECKED_CAST")
        SpaceCompassSettingsExitConfirmation((it[0] as String).ifEmpty { null }, it[1] as Map<String, String>)
    }
)

/** No confirmation on rotation, backgrounding, subdialogs, or merely opening a settings page. */
@Composable
internal fun rememberSpaceCompassSettingsExitNotice(route: String?): SpaceCompassSettingsExitConfirmation {
    val context = LocalContext.current
    val preferences = LocalSpaceCompassPreferences.current
    val message = stringResource(R.string.settings_saved)
    val confirmation = rememberSaveable(saver = settingsExitSaver) { SpaceCompassSettingsExitConfirmation() }
    val page = route?.takeIf { it in setOf("appearance", "language", "units", "observer") }
    // Side effects run after child disposal: the last complete scenario draft is included.
    SideEffect {
        if (confirmation.moveTo(page, settingsNoticeValues(preferences)))
            showSpaceCompassBottomMessage(context, message)
    }
    return confirmation
}
