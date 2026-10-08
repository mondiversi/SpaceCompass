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

internal const val SPACE_COMPASS_SKY_REFERENCES_KEY = "sky_references_visible"
internal const val SPACE_COMPASS_SKY_REFERENCES_DEFAULT = false

/** The slash marks hidden guides; visible guides use the theme accent. */
@Composable
internal fun SpaceCompassSkyReferenceButton(checked: Boolean, onCheckedChange: (Boolean) -> Unit,
    color: Color, background: Color) {
    val tint = spaceCompassFloatingControlTint(color, checked)
    SpaceCompassFloatingControlHitRegion {
        Surface(Modifier.spaceCompassFloatingControlVisual().size(48.dp), shape = CircleShape, color = background.copy(alpha = .94f),
            contentColor = tint, border = BorderStroke(1.dp, tint.copy(alpha = .35f)), shadowElevation = 3.dp) {
            Box(contentAlignment = Alignment.Center) {
                IconToggleButton(checked = checked, onCheckedChange = onCheckedChange,
                    modifier = Modifier.fillMaxSize().testTag("toggle-sky-references"),
                    colors = IconButtonDefaults.iconToggleButtonColors(contentColor = color, checkedContentColor = tint)) {
                    Icon(painterResource(if (checked) R.drawable.ic_sky_references else R.drawable.ic_sky_references_off),
                        stringResource(if (checked) R.string.sky_references_hide else R.string.sky_references_show),
                        Modifier.size(26.dp))
                }
            }
        }
    }
}
