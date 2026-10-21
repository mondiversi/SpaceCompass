package me.mondiversi.spacecompass

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier

/** Match UVIR's soft Material press indication, tinted by the unchanged action foreground.
 * Keep the resting surface transparent and the disabled palette independent of press feedback.
 */
@Composable
internal fun SpaceCompassSettingsActionButton(label: String, onClick: () -> Unit,
    modifier: Modifier = Modifier, enabled: Boolean = true, primary: Boolean = true,
    icon: @Composable () -> Unit) {
    CompositionLocalProvider(LocalSpaceCompassSettingsActionButtons provides true) {
        val foreground = MaterialTheme.colorScheme.onSurface
        val resting = if (primary) spaceCompassPrimaryOutlinedButtonColors()
            else spaceCompassOutlinedActionColors(foreground)
        OutlinedButton(onClick, modifier.fillMaxWidth(), enabled = enabled,
            colors = resting,
            border = if (primary) spaceCompassPrimaryOutlinedButtonBorder(enabled, foreground)
                else spaceCompassOutlinedActionBorder(enabled, foreground)) {
            SpaceCompassLabeledButtonContent(label, icon)
        }
    }
}
