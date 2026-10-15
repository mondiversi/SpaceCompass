package me.mondiversi.spacecompass

import androidx.compose.foundation.layout.size
import androidx.compose.ui.Alignment
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp

/** Attention orange highlights active overrides; a crossed glyph marks all overrides off. */
@Composable
internal fun SpaceCompassScenarioButton(activeCount: Int, color: Color, background: Color,
    onClick: () -> Unit) {
    val active = activeCount > 0
    // Match Uvir's attention orange in both light and dark themes.
    val tint = if (active) Color(0xFFF57C00) else color
    val label = stringResource(R.string.observer_title)
    val activeLabel = stringResource(R.string.observer_simulation_active)
    val count = formatSpaceCompassNumber(activeCount.toDouble(), 0,
        LocalSpaceCompassNumericFormat.current, grouping = false)
    SpaceCompassFloatingActionButton(label, onClick, tint, background,
        Modifier.testTag("open-scenario"), selectedState = active,
        stateText = if (active) "$activeLabel · $count" else null,
        overlay = {
            if (active) SpaceCompassFloatingCountBadge(count, tint, background,
                Modifier.align(Alignment.TopEnd).testTag("scenario-active-count"))
        }) {
        SpaceCompassSettingsMenuIcon("observer", tint, Modifier.size(26.dp),
            iconStrokeWidth = SpaceCompassTitleActionIconStrokeWidth, crossed = !active)
    }
}
