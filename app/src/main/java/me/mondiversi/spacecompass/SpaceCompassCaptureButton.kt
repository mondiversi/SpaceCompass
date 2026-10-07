package me.mondiversi.spacecompass

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp

@Composable
internal fun SpaceCompassCaptureButton(onCapture: () -> Unit, enabled: Boolean, color: Color, background: Color,
    modifier: Modifier = Modifier) {
    val label=stringResource(R.string.panorama_capture)
    Surface(onClick=onCapture,enabled=enabled,modifier=modifier.size(48.dp).testTag("capture-panorama")
        .spaceCompassAccessibleAction(label,enabled=enabled,onClick=onCapture),
        shape=CircleShape,color=background.copy(alpha=.94f),contentColor=color,
        border=BorderStroke(1.dp,color.copy(alpha=.35f)),shadowElevation=3.dp) {
        Box(contentAlignment=Alignment.Center) { SpaceCompassSettingsMenuIcon("capture",color.copy(alpha=if(enabled) 1f else .38f)) }
    }
}
