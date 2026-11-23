package me.mondiversi.spacecompass

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/** Expanded/portrait keep their original rows; compact landscape uses bounded side columns. */
@Composable
internal fun SpaceCompassMainNavigationControls(vertical: Boolean, modifier: Modifier,
    spacing: Dp, maximumHeight: Dp, objects: @Composable () -> Unit,
    location: @Composable () -> Unit, guides: @Composable () -> Unit, interval: @Composable () -> Unit) {
    val target = modifier.testTag("celestial-top-center-controls").spaceCompassSceneInputShield()
    if (vertical) Column(target.heightIn(max = maximumHeight).verticalScroll(rememberScrollState()).padding(4.dp),
        horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        objects(); location(); guides(); interval()
    } else FlowRow(target.padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(spacing, Alignment.CenterHorizontally),
        verticalArrangement = Arrangement.spacedBy(spacing)) {
        interval(); guides(); location(); objects()
    }
}

/** Keep Zoom before the mode selector and shutter; compact landscape stacks all three. */
@Composable
internal fun SpaceCompassMainCaptureControls(vertical: Boolean, modifier: Modifier,
    landscape: Boolean,
    zoom: @Composable () -> Unit, camera: @Composable () -> Unit, capture: @Composable () -> Unit) {
    val target = modifier.padding(4.dp).testTag("celestial-capture-controls")
    Row(target, verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        if (landscape && vertical) {
            Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                zoom(); camera(); capture()
            }
        } else {
            zoom(); camera()
            capture()
        }
    }
}
