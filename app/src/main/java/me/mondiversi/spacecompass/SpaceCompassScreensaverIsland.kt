package me.mondiversi.spacecompass

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import androidx.core.content.edit

@Composable
internal fun SpaceCompassScreensaverIsland() {
    val preferences = LocalSpaceCompassPreferences.current ?: return
    val settings = LocalSpaceCompassScreensaverSettings.current
    val context = LocalContext.current
    val saved = stringResource(R.string.settings_saved)
    val title = stringResource(R.string.screensaver_title)
    SpaceCompassSettingsToggleIsland(title, "screensaver", settings.enabled, title,
        onCheckedChange = { enabled ->
            if (enabled != settings.enabled) {
                preferences.edit { putBoolean(SPACE_COMPASS_SCREENSAVER_ENABLED_KEY, enabled) }
                showSpaceCompassBottomMessage(context, saved)
            }
        }, modifier = Modifier.testTag("screensaver-enabled")) {
        SpaceCompassSettingsDescription(stringResource(R.string.screensaver_description))
        Text(stringResource(R.string.screensaver_delay), fontSize = 13.sp, fontWeight = FontWeight.Medium)
        Column(Modifier.fillMaxWidth().selectableGroup(),
            verticalArrangement = Arrangement.spacedBy(SpaceCompassSettingsChoiceSpacing)) {
            spaceCompassScreensaverDelays.forEach { seconds ->
                val amount = if (seconds < 60) seconds else seconds / 60
                val label = stringResource(if (seconds < 60) R.string.screensaver_seconds else R.string.screensaver_minutes,
                    formatSpaceCompassNumber(amount.toDouble(), 0, LocalSpaceCompassNumericFormat.current,
                        systemLocale = LocalSpaceCompassDeviceLocale.current))
                val checked = settings.delaySeconds == seconds
                SpaceCompassSettingsRadioRow(checked, modifier = Modifier.testTag("screensaver-delay-$seconds"), onClick = {
                    if (!checked) {
                        preferences.edit { putInt(SPACE_COMPASS_SCREENSAVER_DELAY_KEY, seconds) }
                        showSpaceCompassBottomMessage(context, saved)
                    }
                }) { Text(label, fontSize = 14.sp, fontWeight = FontWeight.Medium) }
            }
        }
    }
}
