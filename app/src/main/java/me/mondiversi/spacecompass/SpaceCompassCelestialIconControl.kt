package me.mondiversi.spacecompass

import androidx.compose.foundation.*
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp

internal data class SpaceCompassCelestialControlStyle(val background: Color, val border: Color, val content: Color)

/** Viewer tabs invert their fill and glyph when selected; sky action buttons retain their dark fill. */
internal fun spaceCompassCelestialControlStyle(selected: Boolean, pressed: Boolean = false,
    role: Role = Role.Tab): SpaceCompassCelestialControlStyle {
    val active = selected && role == Role.Tab
    val base = if (active) Color(0xFFE0F4FA) else Color(0xFF424242)
    val feedback = if (active) Color.Black else Color.White
    return SpaceCompassCelestialControlStyle(
        if (pressed) feedback.copy(alpha = 0.12f).compositeOver(base) else base,
        if (active) Color(0xFF4DD8F0) else Color.White.copy(alpha = 0.65f),
        if (active) Color(0xFF123743) else Color.White)
}

/** Shared sky/viewer geometry: 30 dp visible circle, independent 48 dp touch target. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun SpaceCompassCelestialIconControl(selected: Boolean, label: String, onClick: () -> Unit,
    modifier: Modifier = Modifier, role: Role = Role.Tab, icon: @Composable () -> Unit) {
    TooltipBox(positionProvider = TooltipDefaults.rememberPlainTooltipPositionProvider(),
        tooltip = { PlainTooltip { Text(label) } }, state = rememberTooltipState()) {
        val pressSource = remember { MutableInteractionSource() }
        val pressed by pressSource.collectIsPressedAsState()
        val style = spaceCompassCelestialControlStyle(selected, pressed, role)
        val action = if (role == Role.Tab) Modifier.selectable(selected, interactionSource = pressSource,
            indication = null, role = role, onClick = onClick)
            else Modifier.clickable(interactionSource = pressSource, indication = null,
                role = role, onClickLabel = label, onClick = onClick)
        Box(modifier.size(48.dp).clip(CircleShape).then(action)
            .spaceCompassAccessibleAction(label = label, role = role, selectedState = if (role == Role.Tab) selected else null,
                onClick = onClick), contentAlignment = Alignment.Center) {
            Box(Modifier.size(30.dp).clip(CircleShape).background(style.background)
                .border(1.dp, style.border, CircleShape),
                contentAlignment = Alignment.Center) {
                CompositionLocalProvider(LocalContentColor provides style.content) { icon() }
            }
        }
    }
}
