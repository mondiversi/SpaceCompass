package me.mondiversi.planetcompass

import androidx.compose.foundation.*
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp

internal data class PlanetCompassCelestialControlStyle(val background: Color, val border: Color)

/** Sky actions have the same dark-grey fill whether the trajectory is shown or hidden.
 * Only mutually exclusive viewer tabs use a selected fill. All outlines/dimensions match.
 */
internal fun planetCompassCelestialControlStyle(selected: Boolean, pressed: Boolean = false,
    role: Role = Role.Tab): PlanetCompassCelestialControlStyle {
    val base = Color(0xFF424242)
    val background = if (selected && role == Role.Tab) Color.White.copy(alpha = 0.16f).compositeOver(base) else base
    return PlanetCompassCelestialControlStyle(
        if (pressed) Color.White.copy(alpha = 0.12f).compositeOver(background) else background,
        Color.White.copy(alpha = 0.65f))
}

/** Shared sky/viewer geometry: 30 dp visible circle, independent 48 dp touch target. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun PlanetCompassCelestialIconControl(selected: Boolean, label: String, onClick: () -> Unit,
    modifier: Modifier = Modifier, role: Role = Role.Tab, icon: @Composable () -> Unit) {
    TooltipBox(positionProvider = TooltipDefaults.rememberPlainTooltipPositionProvider(),
        tooltip = { PlainTooltip { Text(label) } }, state = rememberTooltipState()) {
        val pressSource = remember { MutableInteractionSource() }
        val pressed by pressSource.collectIsPressedAsState()
        val style = planetCompassCelestialControlStyle(selected, pressed, role)
        val action = if (role == Role.Tab) Modifier.selectable(selected, interactionSource = pressSource,
            indication = null, role = role, onClick = onClick)
            else Modifier.clickable(interactionSource = pressSource, indication = null,
                role = role, onClickLabel = label, onClick = onClick)
        Box(modifier.size(48.dp).clip(CircleShape).then(action)
            .planetCompassAccessibleAction(label = label, role = role, selectedState = if (role == Role.Tab) selected else null,
                onClick = onClick), contentAlignment = Alignment.Center) {
            Box(Modifier.size(30.dp).clip(CircleShape).background(style.background)
                .border(1.dp, style.border, CircleShape),
                contentAlignment = Alignment.Center) { icon() }
        }
    }
}
