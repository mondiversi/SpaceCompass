package me.mondiversi.spacecompass

import androidx.compose.foundation.layout.size
import androidx.compose.ui.Alignment
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp

/** Attention orange marks active overrides; a muted gray glyph marks automatic values. */
@Composable
internal fun SpaceCompassScenarioButton(activeCount: Int, color: Color, background: Color,
    onClick: () -> Unit) {
    val active = activeCount > 0
    val tint = if (active) spaceCompassScenarioAccentColor(background.luminance() < .4f)
        else spaceCompassFloatingControlTint(color, active = false)
    val label = stringResource(R.string.observer_title)
    val activeLabel = stringResource(R.string.observer_simulation_active)
    val count = formatSpaceCompassNumber(activeCount.toDouble(), 0,
        LocalSpaceCompassNumericFormat.current, grouping = false)
    SpaceCompassFloatingActionButton(label, onClick, tint, background,
        Modifier.testTag("open-scenario").spaceCompassTutorialTarget(SpaceCompassTutorialFeature.LOCATION), selectedState = active,
        glyphInk = spaceCompassFloatingInk(26f, .10f, .1075f, .93f, .90f),
        stateText = if (active) "$activeLabel · $count" else null,
        overlay = {
            if (active) SpaceCompassFloatingCountBadge(count,
                Modifier.align(Alignment.TopEnd).testTag("scenario-active-count"), accent = tint)
        }) {
        SpaceCompassSettingsMenuIcon("observer", tint, Modifier.size(26.dp),
            iconStrokeWidth = SpaceCompassFloatingIconStrokeWidth)
    }
}
