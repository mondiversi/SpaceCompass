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

/** Both pointing references share a neutral color; the real camera disables this configuration. */
@Composable
internal fun SpaceCompassPointingAxisButton(checked: Boolean, onCheckedChange: (Boolean) -> Unit,
    color: Color, background: Color, enabled: Boolean = true) {
    SpaceCompassFloatingControlHitRegion {
        Surface(Modifier.spaceCompassFloatingControlVisual().size(48.dp), shape = CircleShape,
            color = background.copy(alpha = .94f), contentColor = color.copy(alpha = if (enabled) 1f else .38f),
            border = BorderStroke(1.dp, color.copy(alpha = if (enabled) .35f else .13f)), shadowElevation = 3.dp) {
            Box(contentAlignment = Alignment.Center) {
                IconToggleButton(checked, onCheckedChange, enabled = enabled, modifier = Modifier.fillMaxSize().testTag("toggle-pointing-axis"),
                    colors = IconButtonDefaults.iconToggleButtonColors(contentColor = color, checkedContentColor = color,
                        disabledContentColor = color.copy(alpha = .38f))) {
                    Icon(painterResource(if (checked) R.drawable.ic_phone_horizontal else R.drawable.ic_phone_vertical),
                        stringResource(if (checked) R.string.pointing_axis_top_edge else R.string.pointing_axis_camera),
                        Modifier.size(26.dp))
                }
            }
        }
    }
}
