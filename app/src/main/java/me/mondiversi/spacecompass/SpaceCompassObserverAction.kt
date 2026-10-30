package me.mondiversi.spacecompass

import androidx.compose.material3.LocalContentColor
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

/** Location actions share the app's outlined surface, leading icon and centered uppercase caption. */
@Composable
internal fun SpaceCompassObserverAction(label: String, onClick: () -> Unit, enabled: Boolean = true,
    modifier: Modifier = Modifier, icon: Int = R.drawable.ic_check, groupIcon: String? = null) {
    SpaceCompassSettingsActionButton(label, onClick, modifier, enabled) {
        if (groupIcon != null) SpaceCompassSettingsGroupIcon(groupIcon, LocalContentColor.current)
        else SpaceCompassActionIcon(icon)
    }
}
