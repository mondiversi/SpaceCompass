package me.mondiversi.spacecompass

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.clickable
import androidx.compose.foundation.ScrollState
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.abs
import kotlinx.coroutines.flow.collect

@Composable
internal fun SpaceCompassCameraZoomControls(range: SpaceCompassCameraZoomRange?, requested: Float,
    actual: Float, enabled: Boolean, onZoom: (Float) -> Unit, color: Color, background: Color, camera: Boolean = true) {
    var expanded by remember { mutableStateOf(false) }
    val tag = if (camera) "camera-zoom" else "virtual-zoom"
    val canChoose = enabled && range != null
    val presets = remember(range) { range?.presets().orEmpty() }
    val numeric = LocalSpaceCompassNumericFormat.current
    fun label(ratio: Float): String {
        val nominal = range?.displayRatio(ratio) ?: ratio
        return formatSpaceCompassNumber(nominal.toDouble(), spaceCompassCameraZoomDigits(nominal), numeric, grouping = false) + "×"
    }
    // The popup can cover the scene and information islands; its window bounds limit the height.
    val menuScroll = remember(expanded) { ScrollState(0) }
    var menuRowHeight by remember(expanded) { mutableIntStateOf(0) }
    LaunchedEffect(expanded, presets, requested, menuScroll) {
        if (expanded) snapshotFlow { menuRowHeight to menuScroll.viewportSize }.collect { (rowHeight, viewport) ->
            if (rowHeight > 0 && viewport > 0) {
                val index = presets.indices.minByOrNull { abs(presets[it] - requested) } ?: 0
                menuScroll.scrollTo((index * rowHeight + (rowHeight - viewport) / 2).coerceAtLeast(0))
            }
        }
    }
    val value = label(actual)
    val accessibilityLabel = stringResource(R.string.camera_zoom_level, value)
    val open = { expanded = true }
    LaunchedEffect(canChoose) { if (!canChoose) expanded = false }
    Box {
        SpaceCompassFloatingActionButton(accessibilityLabel, open, color, background,
            enabled = canChoose,
            modifier = Modifier
                .testTag("$tag-value").spaceCompassTutorialTarget(SpaceCompassTutorialFeature.ZOOM, enabled = canChoose)) {
            Text(value, Modifier.padding(2.dp), fontSize = 14.sp, fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center, maxLines = 1)
        }
        SpaceCompassAdaptiveDropdownMenu(expanded, onDismissRequest = { expanded = false },
            modifier = Modifier.widthIn(min = 48.dp).testTag("$tag-menu"),
            containerColor = spaceCompassSettingsCardColor(), scrollState = menuScroll, centerOnAnchor = true, centerVerticallyOnAnchor = true) {
            presets.forEach { preset ->
                val checked = abs(preset - requested) < .005f
                // Numeric-only options size the menu from their text, without Material's generic item minimum width.
                Row(Modifier.fillMaxWidth().heightIn(min = 48.dp)
                    .clickable(role = Role.Button) { expanded = false; onZoom(preset) }
                    .testTag("$tag-preset-$preset").semantics { selected = checked }
                    .onSizeChanged { if (preset == presets.firstOrNull()) menuRowHeight = it.height }
                    .padding(horizontal = 16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(label(preset), fontSize = 14.sp, maxLines = 1, style = MaterialTheme.typography.bodyLarge,
                        color = if (checked) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                        fontWeight = if (checked) FontWeight.SemiBold else FontWeight.Normal)
                }
            }
        }
    }
}
