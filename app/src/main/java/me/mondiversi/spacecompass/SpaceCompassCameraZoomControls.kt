package me.mondiversi.spacecompass

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.abs

@Composable
internal fun SpaceCompassCameraZoomControls(range: SpaceCompassCameraZoomRange?, requested: Float,
    actual: Float, enabled: Boolean, onZoom: (Float) -> Unit, color: Color, background: Color) {
    var expanded by remember { mutableStateOf(false) }
    val canChoose = enabled && range != null
    val presets = remember(range) { range?.presets().orEmpty() }
    val numeric = LocalSpaceCompassNumericFormat.current
    fun label(ratio: Float): String {
        val nominal = range?.displayRatio(ratio) ?: ratio
        return formatSpaceCompassNumber(nominal.toDouble(), spaceCompassCameraZoomDigits(nominal), numeric, grouping = false) + "×"
    }
    val density = LocalDensity.current
    val configuration = LocalConfiguration.current
    var anchorTop by remember { mutableFloatStateOf(0f) }
    var anchorBottom by remember { mutableFloatStateOf(0f) }
    // A bottom-mounted selector must also leave room for a scrollable menu above it.
    val menuHeight = maxOf(with(density) { anchorTop.toDp() } - 16.dp,
        configuration.screenHeightDp.dp - with(density) { anchorBottom.toDp() } - 16.dp)
        .coerceAtLeast(48.dp)
    val value = label(actual)
    val accessibilityLabel = stringResource(R.string.camera_zoom_level, value)
    val open = { expanded = true }
    LaunchedEffect(canChoose) { if (!canChoose) expanded = false }
    Box {
        SpaceCompassFloatingActionButton(accessibilityLabel, open, color, background,
            enabled = canChoose,
            modifier = Modifier.onGloballyPositioned {
                val anchor = it.boundsInWindow()
                anchorTop = anchor.top
                anchorBottom = anchor.bottom
            }
                .testTag("camera-zoom-value")) {
            Text(value, Modifier.padding(2.dp), fontSize = 14.sp, fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center, maxLines = 1)
        }
        SpaceCompassAdaptiveDropdownMenu(expanded, onDismissRequest = { expanded = false },
            modifier = Modifier.widthIn(min = 144.dp).heightIn(max = menuHeight).selectableGroup().testTag("camera-zoom-menu"),
            containerColor = spaceCompassSettingsCardColor()) {
            presets.forEach { preset ->
                val checked = abs(preset - requested) < .005f
                DropdownMenuItem(text = { Text(label(preset), fontSize = 14.sp, maxLines = 1) },
                    trailingIcon = { RadioButton(checked, onClick = null,
                        modifier = Modifier.clearAndSetSemantics {}) },
                    modifier = Modifier.testTag("camera-zoom-preset-$preset").semantics {
                        role = Role.RadioButton
                        selected = checked
                    },
                    onClick = { expanded = false; onZoom(preset) })
            }
        }
    }
}
