package me.mondiversi.spacecompass

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/** Disclosure belongs to this export opening; it never writes preview preferences. */
@Composable
internal fun SpaceCompassExportPositionChoices(selected: SpaceCompassPanoramaPosition,
    onSelect: (SpaceCompassPanoramaPosition) -> Unit, enabled: Boolean) {
    Text(stringResource(R.string.observer_place),fontSize=15.sp,fontWeight=FontWeight.Bold)
    Column(Modifier.fillMaxWidth().selectableGroup().testTag("capture-export-position"),
        verticalArrangement=Arrangement.spacedBy(SpaceCompassSettingsChoiceSpacing)) {
        SpaceCompassPanoramaPosition.entries.forEach { option ->
            SpaceCompassSettingsRadioRow(selected==option,{ onSelect(option) },
                Modifier.fillMaxWidth().testTag("capture-export-position-${option.key}"),enabled=enabled) {
                Column(Modifier.weight(1f)) {
                    Text(stringResource(option.labelResource),fontSize=14.sp,fontWeight=FontWeight.Medium)
                    Text(stringResource(option.detailResource),fontSize=11.sp,lineHeight=15.sp,
                        color=MaterialTheme.colorScheme.onSurface.copy(alpha=.65f))
                }
            }
        }
    }
}
