package me.mondiversi.spacecompass

import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp

/** Attention orange highlights active overrides; a crossed glyph marks all overrides off. */
@Composable
internal fun SpaceCompassScenarioButton(active: Boolean, color: Color, background: Color,
    onClick: () -> Unit) {
    // Match Uvir's attention orange in both light and dark themes.
    val tint = if (active) Color(0xFFF57C00) else color
    val label = stringResource(R.string.observer_title)
    val activeLabel = stringResource(R.string.observer_simulation_active)
    SpaceCompassFloatingActionButton(label, onClick, tint, background,
        Modifier.testTag("open-scenario"), selectedState = active,
        stateText = activeLabel.takeIf { active }) {
        SpaceCompassSettingsMenuIcon("observer", tint, Modifier.size(26.dp),
            iconStrokeWidth = SpaceCompassTitleActionIconStrokeWidth, crossed = !active)
    }
}
