package me.mondiversi.spacecompass

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource

/** Metadata lookups use the same app action buttons as the other Location editors. */
@Composable
internal fun SpaceCompassObserverMetadataActions(key: String, selectedEnabled: Boolean,
    currentEnabled: Boolean, busy: Boolean, notice: Int,
    onSelected: () -> Unit, onCurrent: () -> Unit) {
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(SpaceCompassSettingsActionGap)) {
        SpaceCompassObserverAction(stringResource(R.string.observer_detect_selected), onSelected, selectedEnabled,
            modifier = Modifier.testTag("observer-detect-selected-$key"), icon = R.drawable.ic_location_pin)
        SpaceCompassObserverAction(stringResource(R.string.observer_detect_current), onCurrent, currentEnabled,
            modifier = Modifier.testTag("observer-detect-current-$key"), groupIcon = "coordinates")
    }
    if (busy) LinearProgressIndicator(Modifier.fillMaxWidth())
    if (notice != 0) Text(stringResource(notice), style = MaterialTheme.typography.bodySmall)
}
