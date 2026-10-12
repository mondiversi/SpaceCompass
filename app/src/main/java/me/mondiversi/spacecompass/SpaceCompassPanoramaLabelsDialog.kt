package me.mondiversi.spacecompass

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource

/** A configuration picker, rather than an enabled/disabled image-overlay switch. */
@Composable
internal fun SpaceCompassPanoramaLabelsDialog(selected: SpaceCompassPanoramaLabelMode,
    onSelect: (SpaceCompassPanoramaLabelMode) -> Unit, onDismiss: () -> Unit) {
    SpaceCompassAlertDialog(onDismissRequest = onDismiss, confirmButton = null,
        title = { Text(stringResource(R.string.panorama_point_labels)) }, text = {
            Column(Modifier.fillMaxWidth().selectableGroup().testTag("panorama-labels-dialog")) {
                SpaceCompassPanoramaLabelMode.entries.forEach { mode ->
                    SpaceCompassSettingsRadioRow(mode == selected, { onSelect(mode) },
                        Modifier.fillMaxWidth().testTag("panorama-labels-${mode.key}")) {
                        Text(stringResource(mode.labelResource), Modifier.weight(1f))
                    }
                }
            }
        })
}
