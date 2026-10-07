package me.mondiversi.spacecompass

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource

/** Auxiliary lookup links remain inside the corresponding island, below its input. */
@Composable
internal fun SpaceCompassObserverMetadataActions(key: String, selectedEnabled: Boolean,
    currentEnabled: Boolean, busy: Boolean, notice: Int,
    onSelected: () -> Unit, onCurrent: () -> Unit) {
    Column(Modifier.fillMaxWidth()) {
        TextButton(onClick = onSelected, enabled = selectedEnabled,
            modifier = Modifier.testTag("observer-detect-selected-$key")) {
            Text(stringResource(R.string.observer_detect_selected))
        }
        TextButton(onClick = onCurrent, enabled = currentEnabled,
            modifier = Modifier.testTag("observer-detect-current-$key")) {
            Text(stringResource(R.string.observer_detect_current))
        }
    }
    if (busy) LinearProgressIndicator(Modifier.fillMaxWidth())
    if (notice != 0) Text(stringResource(notice), style = MaterialTheme.typography.bodySmall)
}
