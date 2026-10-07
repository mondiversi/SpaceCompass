package me.mondiversi.spacecompass

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/** One confirmation/repository treatment: quiet outline at rest, primary fill while pressed.
 * Press feedback never changes layout or leaves a command looking selected after release.
 */
@Composable
internal fun SpaceCompassSettingsActionButton(label: String, onClick: () -> Unit,
    modifier: Modifier = Modifier, enabled: Boolean = true, primary: Boolean = true,
    icon: @Composable () -> Unit) {
    CompositionLocalProvider(LocalSpaceCompassSettingsActionButtons provides true) {
        val interaction = remember { MutableInteractionSource() }
        val pressed by interaction.collectIsPressedAsState()
        val active = enabled && pressed
        val foreground = MaterialTheme.colorScheme.onSurface
        val resting = if (primary) spaceCompassPrimaryOutlinedButtonColors()
            else spaceCompassOutlinedActionColors(foreground)
        val container by animateColorAsState(
            if (active) MaterialTheme.colorScheme.primary else resting.containerColor,
            tween(if (active) 90 else 140), label = "settingsActionPressedContainer")
        val content by animateColorAsState(
            if (active) MaterialTheme.colorScheme.onPrimary else resting.contentColor,
            tween(if (active) 90 else 140), label = "settingsActionPressedContent")
        OutlinedButton(onClick, modifier.fillMaxWidth().heightIn(min = 48.dp), enabled = enabled,
            interactionSource = interaction,
            colors = ButtonDefaults.outlinedButtonColors(containerColor = container, contentColor = content,
                disabledContainerColor = resting.disabledContainerColor,
                disabledContentColor = resting.disabledContentColor),
            border = if (primary || active) spaceCompassPrimaryOutlinedButtonBorder(enabled, foreground)
                else spaceCompassOutlinedActionBorder(enabled, foreground)) {
            SpaceCompassLabeledButtonContent(label, icon)
        }
    }
}
