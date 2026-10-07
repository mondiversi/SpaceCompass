package me.mondiversi.spacecompass

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.IconToggleButton
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp

/** Camera opt-in uses the same neutral circular control as the other sky actions. */
@Composable
internal fun SpaceCompassCameraToggleButton(checked: Boolean, onCheckedChange: (Boolean) -> Unit,
    color: Color, background: Color) {
    Surface(Modifier.size(48.dp), shape = CircleShape, color = background.copy(alpha = .94f),
        contentColor = color, border = BorderStroke(1.dp, color.copy(alpha = .35f)), shadowElevation = 3.dp) {
        Box(contentAlignment = Alignment.Center) {
            IconToggleButton(checked = checked, onCheckedChange = onCheckedChange,
                modifier = Modifier.fillMaxSize().testTag("toggle-camera"),
                colors = IconButtonDefaults.iconToggleButtonColors(contentColor = color, checkedContentColor = color)) {
                Icon(painterResource(if (checked) R.drawable.ic_camera else R.drawable.ic_camera_off),
                    stringResource(if (checked) R.string.camera_disable else R.string.camera_enable),
                    Modifier.size(26.dp))
            }
        }
    }
}
