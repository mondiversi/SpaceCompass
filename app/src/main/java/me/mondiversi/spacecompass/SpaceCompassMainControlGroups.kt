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

/** Weather becomes transparency in camera mode; both views keep zoom in the second slot. */
@Composable
internal fun SpaceCompassMainViewModeControls(vertical: Boolean, modifier: Modifier,
    maximumHeight: Dp, content: @Composable () -> Unit) {
    val target = modifier.testTag("celestial-bottom-center-controls").spaceCompassSceneInputShield()
    if (vertical) Column(target.heightIn(max = maximumHeight).verticalScroll(rememberScrollState()).padding(4.dp),
        horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        content()
    } else Row(target.padding(4.dp), verticalAlignment = Alignment.Bottom,
        horizontalArrangement = Arrangement.spacedBy(8.dp)) { content() }
}

/** Pin antipodes above the shutter in portrait and beside it in landscape. */
@Composable
internal fun SpaceCompassMainCaptureControls(vertical: Boolean, modifier: Modifier,
    antipodes: @Composable () -> Unit, landscape: Boolean,
    camera: @Composable () -> Unit, capture: @Composable () -> Unit) {
    val target = modifier.padding(4.dp).testTag("celestial-capture-controls")
    Row(target, verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        if (landscape && vertical) {
            antipodes()
            Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                camera(); capture()
            }
        } else {
            camera()
            if (landscape) { antipodes(); capture() }
            else Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                antipodes(); capture()
            }
        }
    }
}
