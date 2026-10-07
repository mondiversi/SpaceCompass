package me.mondiversi.spacecompass

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp

private val viewModeTargetSize = 48.dp
private val viewModeTrackHeight = 30.dp
private val viewModeChoiceSpacing = 30.dp
private val viewModeIconInset = (viewModeTargetSize - viewModeChoiceSpacing) / 2
private val viewModeTrackInset = 2.dp
private val viewModeThumbSize = viewModeTrackHeight - viewModeTrackInset * 2

/** Original 30 dp visible height with independent 48 dp targets; relative offsets mirror in RTL. */
@Composable
internal fun SpaceCompassCelestialViewModeSwitch(rotating: Boolean, currentLabel: String,
    rotationLabel: String, onModeChange: (Boolean) -> Unit, modifier: Modifier = Modifier) {
    val inactive = spaceCompassCelestialControlStyle(false)
    val active = spaceCompassCelestialControlStyle(true)
    val offset by animateDpAsState(if (rotating) viewModeChoiceSpacing else 0.dp,
        animationSpec = tween(180), label = "celestialViewMode")
    Box(modifier.size(viewModeTargetSize * 2, viewModeTargetSize).testTag("celestial-view-mode-switch")) {
        // Glyphs and thumb centres align; adjacent touch targets extend beyond the compact capsule.
        Box(Modifier.align(Alignment.Center).size(viewModeChoiceSpacing + viewModeTrackHeight, viewModeTrackHeight)
            .clip(CircleShape).background(inactive.background).border(1.dp, inactive.border, CircleShape)
            .padding(viewModeTrackInset).testTag("celestial-view-mode-track")) {
            Box(Modifier.offset { IntOffset(offset.roundToPx(), 0) }.size(viewModeThumbSize).clip(CircleShape)
                .background(active.background).border(1.dp, active.border, CircleShape))
        }
        Row(Modifier.selectableGroup()) {
            ModeChoice(!rotating, currentLabel, { onModeChange(false) },
                Modifier.testTag("celestial-view-current"), iconOffset = viewModeIconInset) { color ->
                SpaceCompassPasswordVisibilityIcon(true, Modifier.size(18.dp), color)
            }
            ModeChoice(rotating, rotationLabel, { onModeChange(true) },
                Modifier.testTag("celestial-view-rotation"), iconOffset = -viewModeIconInset) { color ->
                Canvas(Modifier.size(18.dp)) {
                    val inset = 2.dp.toPx()
                    drawArc(color, -45f, 285f, false, Offset(inset, inset),
                        Size(size.width - 2 * inset, size.height - 2 * inset),
                        style = Stroke(1.6.dp.toPx(), cap = StrokeCap.Round))
                    val tip = Offset(size.width - inset, size.height * .30f)
                    drawLine(color, tip - Offset(4.dp.toPx(), 0f), tip,
                        1.6.dp.toPx(), StrokeCap.Round)
                    drawLine(color, tip + Offset(0f, 4.dp.toPx()), tip,
                        1.6.dp.toPx(), StrokeCap.Round)
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ModeChoice(selected: Boolean, label: String, click: () -> Unit,
    modifier: Modifier, iconOffset: Dp, icon: @Composable (Color) -> Unit) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val style = spaceCompassCelestialControlStyle(selected)
    TooltipBox(positionProvider = TooltipDefaults.rememberPlainTooltipPositionProvider(),
        tooltip = { PlainTooltip { Text(label) } }, state = rememberTooltipState()) {
        Box(modifier.size(viewModeTargetSize)
            .selectable(selected, interactionSource = interaction, indication = null,
                role = Role.Tab, onClick = click)
            .spaceCompassAccessibleAction(label, role = Role.Tab, selectedState = selected, onClick = click),
            contentAlignment = Alignment.Center) {
            Box(Modifier.offset(x = iconOffset).size(viewModeThumbSize).clip(CircleShape)
                .background(if (pressed) style.content.copy(alpha = .12f) else Color.Transparent),
                contentAlignment = Alignment.Center) { icon(style.content) }
        }
    }
}
