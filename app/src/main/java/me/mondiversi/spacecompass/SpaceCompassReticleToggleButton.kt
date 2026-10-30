package me.mondiversi.spacecompass

import android.content.SharedPreferences
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp

internal const val SPACE_COMPASS_RETICLE_VISIBLE_KEY = "sky_reticle_visible"
internal const val SPACE_COMPASS_RETICLE_VISIBLE_DEFAULT = true

internal fun readSpaceCompassReticleVisible(preferences: SharedPreferences?): Boolean =
    preferences?.getBoolean(SPACE_COMPASS_RETICLE_VISIBLE_KEY, SPACE_COMPASS_RETICLE_VISIBLE_DEFAULT)
        ?: SPACE_COMPASS_RETICLE_VISIBLE_DEFAULT

internal fun saveSpaceCompassReticleVisible(preferences: SharedPreferences?, visible: Boolean) {
    preferences?.edit()?.putBoolean(SPACE_COMPASS_RETICLE_VISIBLE_KEY, visible)?.apply()
}

/** Live viewing only: capture label settings and selected orbit points remain independent. */
@Composable
internal fun SpaceCompassReticleToggleButton(checked: Boolean, onCheckedChange: (Boolean) -> Unit,
    color: Color, background: Color) {
    val tint = spaceCompassFloatingControlTint(color, checked)
    SpaceCompassFloatingActionButton(
        stringResource(if (checked) R.string.reticle_disable else R.string.reticle_enable),
        { onCheckedChange(!checked) }, tint, background,
        Modifier.testTag("toggle-reticle"), selectedState = checked,
        glyphInk = spaceCompassFloatingVectorInk(if (checked) R.drawable.ic_reticle else R.drawable.ic_reticle_off),
        stateText = stringResource(if (checked) R.string.scene_reticle_on else R.string.scene_reticle_off)) {
        SpaceCompassFloatingVectorIcon(if (checked) R.drawable.ic_reticle else R.drawable.ic_reticle_off)
    }
}
