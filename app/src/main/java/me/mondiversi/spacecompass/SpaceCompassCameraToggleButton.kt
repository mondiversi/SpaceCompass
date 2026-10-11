package me.mondiversi.spacecompass

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
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
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp

/** Show the current scene mode; the shutter remains a separate capture action. */
@Composable
internal fun SpaceCompassCameraToggleButton(checked: Boolean, onCheckedChange: (Boolean) -> Unit,
    color: Color, background: Color) {
    val label = stringResource(if (checked) R.string.camera_disable else R.string.camera_enable)
    SpaceCompassFloatingControlHitRegion {
        Surface(Modifier.spaceCompassFloatingControlVisual().size(48.dp), shape = CircleShape, color = background.copy(alpha = .94f),
            contentColor = color, border = BorderStroke(1.dp, color.copy(alpha = .35f)), shadowElevation = 3.dp) {
            Box(contentAlignment = Alignment.Center) {
                IconToggleButton(checked = checked, onCheckedChange = onCheckedChange,
                    modifier = Modifier.fillMaxSize().testTag("toggle-camera")
                        .semantics { contentDescription = label },
                    colors = IconButtonDefaults.iconToggleButtonColors(contentColor = color, checkedContentColor = color)) {
                    if (checked) {
                        Icon(painterResource(R.drawable.ic_camera), null, Modifier.size(26.dp))
                    } else {
                        SpaceCompassSettingsMenuIcon("panorama", LocalContentColor.current, Modifier.size(26.dp))
                    }
                }
            }
        }
    }
}
