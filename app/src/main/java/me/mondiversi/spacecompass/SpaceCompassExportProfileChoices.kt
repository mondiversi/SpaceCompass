package me.mondiversi.spacecompass

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

/** Shared typography, choices and semantics for scenario, image and table exports. */
@Composable
internal fun SpaceCompassExportProfileChoices(mode: SpaceCompassPanoramaExportMode,
    onSelect: (SpaceCompassPanoramaExportMode) -> Unit, enabled: Boolean = true) {
    Text(stringResource(R.string.panorama_export_profile),fontSize=15.sp,fontWeight=FontWeight.Bold)
    Column(Modifier.fillMaxWidth().selectableGroup().testTag("panorama-export-profile"),
        verticalArrangement=Arrangement.spacedBy(SpaceCompassSettingsChoiceSpacing)) {
        SpaceCompassPanoramaExportMode.entries.forEach { option ->
            SpaceCompassSettingsRadioRow(mode==option,{ onSelect(option) },
                Modifier.fillMaxWidth().testTag("panorama-export-${option.key}"),enabled=enabled) {
                Text(stringResource(if (option==SpaceCompassPanoramaExportMode.SELECTED)
                    R.string.panorama_export_selected else R.string.panorama_export_international),
                    Modifier.weight(1f),fontSize=14.sp,fontWeight=FontWeight.Medium)
            }
        }
    }
}
