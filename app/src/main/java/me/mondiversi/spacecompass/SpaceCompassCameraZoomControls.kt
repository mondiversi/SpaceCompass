package me.mondiversi.spacecompass

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.*
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
    val value = label(actual)
    val accessibilityLabel = stringResource(R.string.camera_zoom_level, value)
    val open = { expanded = true }
    LaunchedEffect(canChoose) { if (!canChoose) expanded = false }
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        val less = enabled && range != null && range.step(requested, false) != range.snap(requested)
        val more = enabled && range != null && range.step(requested, true) != range.snap(requested)
        ZoomButton(stringResource(R.string.celestial_view_zoom_out), SpaceCompassPanoramaControl.ZOOM_OUT,
            less, { range?.let { onZoom(it.step(requested, false)) } }, color, background, "camera-zoom-out")
        Box {
            SpaceCompassFloatingControlHitRegion {
                Surface(onClick = open, enabled = canChoose,
                    modifier = Modifier.spaceCompassFloatingControlVisual().testTag("camera-zoom-value")
                        .spaceCompassAccessibleAction(accessibilityLabel, enabled = canChoose, onClick = open),
                    shape = CircleShape, color = background.copy(alpha = .94f),
                    border = BorderStroke(1.dp, color.copy(alpha = .35f))) {
                    Text(value, Modifier.widthIn(min = 42.dp).padding(horizontal = 8.dp, vertical = 7.dp),
                        color = color.copy(alpha = if (canChoose) 1f else .38f), fontSize = 12.sp,
                        textAlign = TextAlign.Center, maxLines = 1)
                }
            }
            SpaceCompassAdaptiveDropdownMenu(expanded, onDismissRequest = { expanded = false },
                modifier = Modifier.widthIn(min = 144.dp).selectableGroup().testTag("camera-zoom-menu"),
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
        ZoomButton(stringResource(R.string.celestial_view_zoom_in), SpaceCompassPanoramaControl.ZOOM_IN,
            more, { range?.let { onZoom(it.step(requested, true)) } }, color, background, "camera-zoom-in")
    }
}

@Composable
private fun ZoomButton(label: String, icon: SpaceCompassPanoramaControl, enabled: Boolean, click: () -> Unit,
    color: Color, background: Color, tag: String) {
    SpaceCompassFloatingControlHitRegion {
        Surface(onClick = click, enabled = enabled, modifier = Modifier.spaceCompassFloatingControlVisual().size(48.dp).testTag(tag)
            .spaceCompassAccessibleAction(label, enabled = enabled, onClick = click), shape = CircleShape,
            color = background.copy(alpha = .94f), contentColor = color.copy(alpha = if (enabled) 1f else .38f),
            border = BorderStroke(1.dp, color.copy(alpha = .35f)), shadowElevation = 3.dp) {
            Box(contentAlignment = Alignment.Center) { SpaceCompassPanoramaControlIcon(icon) }
        }
    }
}
