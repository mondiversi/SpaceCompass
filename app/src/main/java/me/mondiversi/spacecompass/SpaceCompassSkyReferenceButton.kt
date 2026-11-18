package me.mondiversi.spacecompass

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import kotlin.math.roundToInt

internal const val SPACE_COMPASS_SKY_REFERENCES_KEY = "sky_references_visible"
internal const val SPACE_COMPASS_SKY_REFERENCES_DEFAULT = false

/** The overlays are independent; opening or cancelling never edits the live scene. */
@Composable
internal fun SpaceCompassSkyReferenceButton(grid: Boolean, references: Boolean, numbers: Boolean, opacity: Int,
    onApply: (Boolean, Boolean, Boolean, Int) -> Unit, color: Color, background: Color) {
    var expanded by remember { mutableStateOf(false) }
    val tint = spaceCompassFloatingControlTint(color, grid || references)
    SpaceCompassFloatingActionButton(stringResource(R.string.main_grid_references), { expanded = true }, tint, background,
        Modifier.testTag("toggle-sky-references").spaceCompassTutorialTarget(SpaceCompassTutorialFeature.GUIDES),
        border = BorderStroke(1.dp, tint.copy(alpha = .35f)),
        glyphInk = spaceCompassFloatingInk(26f, .12f, .12f, .88f, .88f)) {
        SpaceCompassAngularGridIcon(LocalContentColor.current)
    }
    SpaceCompassGridReferencesDialog(expanded, grid, references, numbers, opacity, { expanded = false }) { newGrid, newReferences, newNumbers, newOpacity ->
        expanded = false
        onApply(newGrid, newReferences, newNumbers, newOpacity)
    }
}

@Composable
internal fun SpaceCompassGridReferencesDialog(expanded: Boolean, grid: Boolean, references: Boolean, numbers: Boolean, opacity: Int,
    onDismiss: () -> Unit, onApply: (Boolean, Boolean, Boolean, Int) -> Unit) {
    if (!expanded) return
    var draftGrid by rememberSaveable(grid, references, numbers) { mutableStateOf(grid) }
    var draftReferences by rememberSaveable(grid, references, numbers) { mutableStateOf(references) }
    var draftNumbers by rememberSaveable(grid, references, numbers) { mutableStateOf(numbers) }
    var draftOpacity by rememberSaveable(grid, references, numbers, opacity) { mutableIntStateOf(spaceCompassGridOpacity(opacity)) }
    val opacityTitle = stringResource(R.string.main_grid_opacity)
    val opacityValue = stringResource(R.string.music_volume_value,
        formatSpaceCompassNumber(draftOpacity.toDouble(), 0, LocalSpaceCompassNumericFormat.current,
            systemLocale = LocalSpaceCompassDeviceLocale.current))
    SpaceCompassAlertDialog(onDismissRequest = onDismiss,
        modifier = Modifier.testTag("grid-references-dialog"),
        title = { Text(stringResource(R.string.main_grid_references)) },
        confirmButton = { SpaceCompassDialogActionButton(stringResource(R.string.view_mode_apply),
            { onApply(draftGrid, draftReferences, draftNumbers, draftOpacity) }, Modifier.testTag("grid-references-apply")) },
        dismissButton = { SpaceCompassDialogActionButton(stringResource(R.string.close),
            onDismiss, Modifier.testTag("grid-references-cancel"), tone = SpaceCompassDialogActionTone.NEUTRAL) },
        text = {
            // The shared dialog owns scrolling, including short landscape windows.
            Column(Modifier.fillMaxWidth()) {
                SpaceCompassSettingsCheckbox(stringResource(R.string.main_grid_enable), draftGrid, { draftGrid = it },
                    Modifier.testTag("grid-references-grid"), compact = true, leadingIcon = {
                        SpaceCompassAngularGridIcon(LocalContentColor.current, SpaceCompassMenuIconSize, SpaceCompassMenuIconStrokeWidth)
                    })
                Column(Modifier.fillMaxWidth()) {
                    val foreground = MaterialTheme.colorScheme.onSurface.copy(alpha = if(draftGrid) 1f else .38f)
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(opacityTitle, Modifier.weight(1f).padding(end = 8.dp), fontSize = 13.sp, fontWeight = FontWeight.Medium, color = foreground)
                        Text(opacityValue, Modifier.testTag("grid-opacity-value"), fontSize = 11.sp,
                            color = foreground.copy(alpha = if(draftGrid) .72f else .38f),
                            style = TextStyle(textDirection = TextDirection.ContentOrLtr))
                    }
                    Slider(draftOpacity.toFloat(), { draftOpacity = spaceCompassGridOpacity(it.roundToInt()) },
                        enabled = draftGrid, valueRange = 0f..100f, steps = 9,
                        colors = spaceCompassSliderColors(), modifier = Modifier.fillMaxWidth()
                            .testTag("grid-opacity").semantics { contentDescription = opacityTitle })
                }
                SpaceCompassSettingsCheckbox(stringResource(R.string.main_grid_numbers), draftNumbers, { draftNumbers = it },
                    Modifier.testTag("grid-references-numbers"), enabled = draftGrid, compact = true, leadingIcon = {
                        SpaceCompassGridNumbersIcon()
                    })
                SpaceCompassSettingsCheckbox(stringResource(R.string.main_references_enable), draftReferences, { draftReferences = it },
                    Modifier.testTag("grid-references-lines"), compact = true, leadingIcon = {
                        Icon(painterResource(R.drawable.ic_sky_references), null, Modifier.size(SpaceCompassMenuIconSize))
                    })
            }
        })
}
