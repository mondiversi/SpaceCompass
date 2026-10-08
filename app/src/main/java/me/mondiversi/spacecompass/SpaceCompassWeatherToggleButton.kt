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

internal const val SPACE_COMPASS_WEATHER_VISIBLE_KEY = "sky_weather_visible"
internal const val SPACE_COMPASS_WEATHER_VISIBLE_DEFAULT = false

/** Disabling the visual weather leaves physical solar phases and estimated position data intact. */
@Composable
internal fun SpaceCompassWeatherToggleButton(checked: Boolean, onCheckedChange: (Boolean) -> Unit,
    color: Color, background: Color, enabled: Boolean = true) {
    val tint = spaceCompassFloatingControlTint(color, checked, enabled)
    SpaceCompassFloatingControlHitRegion {
        Surface(Modifier.spaceCompassFloatingControlVisual().size(48.dp), shape = CircleShape,
            color = background.copy(alpha = .94f), contentColor = tint.copy(alpha = if (enabled) 1f else .38f),
            border = BorderStroke(1.dp, tint.copy(alpha = if (enabled) .35f else .13f)), shadowElevation = 3.dp) {
            Box(contentAlignment = Alignment.Center) {
                IconToggleButton(checked, onCheckedChange, enabled = enabled, modifier = Modifier.fillMaxSize().testTag("toggle-weather"),
                    colors = IconButtonDefaults.iconToggleButtonColors(contentColor = color, checkedContentColor = tint,
                        disabledContentColor = color.copy(alpha = .38f))) {
                    Icon(painterResource(if (checked) R.drawable.ic_weather else R.drawable.ic_weather_off),
                        stringResource(if (checked) R.string.sky_weather_disable else R.string.sky_weather_enable),
                        Modifier.size(26.dp))
                }
            }
        }
    }
}
