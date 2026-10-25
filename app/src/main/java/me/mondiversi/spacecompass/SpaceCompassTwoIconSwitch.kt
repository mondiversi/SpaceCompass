package me.mondiversi.spacecompass

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp

/** Shared capsule geometry for model modes and object-page navigation, including RTL. */
@Composable
internal fun SpaceCompassTwoIconSwitch(secondSelected: Boolean, firstLabel: String, secondLabel: String,
    onSelectionChange: (Boolean) -> Unit, modifier: Modifier = Modifier, prefix: String,
    firstTag: String, secondTag: String, targetSize: Dp = 48.dp,
    enabled: Boolean = true, secondEnabled: Boolean = true,
    firstIcon: @Composable (Color) -> Unit, secondIcon: @Composable (Color) -> Unit) {
    val trackHeight = 30.dp
    val choiceSpacing = 30.dp
    val trackInset = 2.dp
    val thumbSize = trackHeight - trackInset * 2
    val iconInset = (targetSize - choiceSpacing) / 2
    val inactive = spaceCompassCelestialControlStyle(false)
    val active = spaceCompassCelestialControlStyle(true)
    val offset by animateDpAsState(if (secondSelected) choiceSpacing else 0.dp,
        animationSpec = tween(180), label = prefix)
    Box(modifier.size(targetSize * 2, targetSize).testTag("$prefix-mode-switch")) {
        Box(Modifier.align(Alignment.Center).size(choiceSpacing + trackHeight, trackHeight)
            .clip(CircleShape).background(inactive.background).border(1.dp, inactive.border, CircleShape)
            .padding(trackInset).testTag("$prefix-mode-track")) {
            Box(Modifier.offset { IntOffset(offset.roundToPx(), 0) }.size(thumbSize).clip(CircleShape)
                .background(active.background).border(1.dp, active.border, CircleShape))
        }
        Row(Modifier.selectableGroup()) {
            SwitchChoice(!secondSelected, firstLabel, { onSelectionChange(false) }, enabled,
                Modifier.testTag(firstTag), targetSize, thumbSize, iconInset, firstIcon)
            SwitchChoice(secondSelected, secondLabel, { onSelectionChange(true) }, enabled && secondEnabled,
                Modifier.testTag(secondTag), targetSize, thumbSize, -iconInset, secondIcon)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SwitchChoice(selected: Boolean, label: String, click: () -> Unit, enabled: Boolean,
    modifier: Modifier, targetSize: Dp, thumbSize: Dp, iconOffset: Dp,
    icon: @Composable (Color) -> Unit) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val style = spaceCompassCelestialControlStyle(selected)
    val tint = if (enabled) style.content else style.content.copy(alpha = .38f)
    TooltipBox(positionProvider = TooltipDefaults.rememberPlainTooltipPositionProvider(),
        tooltip = { PlainTooltip { Text(label) } }, state = rememberTooltipState()) {
        Box(modifier.size(targetSize)
            .selectable(selected, enabled = enabled, interactionSource = interaction, indication = null,
                role = Role.Tab, onClick = click)
            .spaceCompassAccessibleAction(label, enabled = enabled, role = Role.Tab,
                selectedState = selected, onClick = click), contentAlignment = Alignment.Center) {
            Box(Modifier.offset(x = iconOffset).size(thumbSize).clip(CircleShape)
                .background(if (pressed && enabled) tint.copy(alpha = .12f) else Color.Transparent),
                contentAlignment = Alignment.Center) { icon(tint) }
        }
    }
}
