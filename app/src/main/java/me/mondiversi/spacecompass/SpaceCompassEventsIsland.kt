package me.mondiversi.spacecompass

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource

@Composable
internal fun SpaceCompassEventsIsland() {
    val preferences = LocalSpaceCompassPreferences.current ?: return
    val visible = spaceCompassMainReminderButtonVisible(LocalSpaceCompassPresentationSettings.current)
    SpaceCompassSettingsIsland(stringResource(R.string.settings_events_title), "events") {
        SpaceCompassSettingsDescription(stringResource(R.string.settings_events_description))
        SpaceCompassSettingsCheckbox(stringResource(R.string.settings_events_show_alarm), visible,
            onCheckedChange = { checked ->
                if (checked != visible) preferences.edit()
                    .putString(SPACE_COMPASS_MAIN_REMINDER_BUTTON_KEY, checked.toString()).apply()
            }, modifier = Modifier.testTag("main-reminder-button-visible"))
    }
}
