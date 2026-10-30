package me.mondiversi.spacecompass

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp

/** Equal-size information and menu controls share one centred anchor between the corner groups. */
@Composable
internal fun SpaceCompassMainNavigationControls(color: Color, background: Color, modifier: Modifier = Modifier) {
    Box(modifier.testTag("celestial-main-navigation").spaceCompassSceneInputShield()
        .wrapContentWidth(unbounded = true)) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            SpaceCompassMainInfoButton(color, background)
            SpaceCompassSettingsButton(color, background)
        }
    }
}
