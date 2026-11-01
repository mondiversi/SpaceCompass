package me.mondiversi.spacecompass

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag

/** Independent corner hosts retain the same information/menu controls and exact hit regions. */
@Composable
internal fun SpaceCompassMainInfoControl(color: Color, background: Color, modifier: Modifier = Modifier) {
    SpaceCompassMainNavigationControl(modifier.testTag("celestial-main-info-navigation")) {
        SpaceCompassMainInfoButton(color, background)
    }
}

@Composable
internal fun SpaceCompassMainMenuControl(color: Color, background: Color, modifier: Modifier = Modifier) {
    SpaceCompassMainNavigationControl(modifier.testTag("celestial-main-menu-navigation")) {
        SpaceCompassSettingsButton(color, background)
    }
}

@Composable
private fun SpaceCompassMainNavigationControl(modifier: Modifier, content: @Composable () -> Unit) {
    Box(modifier.spaceCompassSceneInputShield().wrapContentWidth(unbounded = true)) { content() }
}
