package me.mondiversi.spacecompass

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/** Uvir's profile radio rows and full-width icon actions, using the Space Compass theme. */
@Composable
internal fun SpaceCompassPanoramaExportDialog(mode: SpaceCompassPanoramaExportMode,
    onModeChange: (SpaceCompassPanoramaExportMode) -> Unit, enabled: Boolean,
    onGallery: () -> Unit, onDocument: () -> Unit, onShare: () -> Unit, onDismiss: () -> Unit) {
    val foreground = spaceCompassDialogContentColor()
    SpaceCompassAlertDialog(onDismissRequest = onDismiss, confirmButton = null,
        title = { Text(stringResource(R.string.panorama_export_title)) }, text = {
            Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(stringResource(R.string.panorama_export_profile), color = foreground,
                    fontSize = 15.sp, fontWeight = FontWeight.Bold)
                Column(Modifier.fillMaxWidth().selectableGroup()) {
                    SpaceCompassPanoramaExportMode.entries.forEach { option ->
                        val selected = mode == option
                        Row(Modifier.fillMaxWidth().heightIn(min = 48.dp).clip(RoundedCornerShape(10.dp))
                            .background(if (selected) foreground.copy(alpha = .07f) else Color.Transparent)
                            .selectable(selected, role = Role.RadioButton, onClick = { onModeChange(option) })
                            .testTag("panorama-export-${option.key}").padding(horizontal = 8.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically) {
                            RadioButton(selected, onClick = null)
                            Text(stringResource(if (option == SpaceCompassPanoramaExportMode.SELECTED)
                                R.string.panorama_export_selected else R.string.panorama_export_international),
                                Modifier.weight(1f).padding(start = 8.dp), fontSize = 14.sp, color = foreground)
                        }
                    }
                }
                HorizontalDivider(color = foreground.copy(alpha = .22f))
                Spacer(Modifier.height(2.dp))
                SpaceCompassSettingsActionButton(stringResource(R.string.panorama_save_gallery), onGallery,
                    Modifier.testTag("panorama-export-gallery"), enabled) { SpaceCompassPanoramaGalleryIcon() }
                SpaceCompassSettingsActionButton(stringResource(R.string.panorama_save_phone), onDocument,
                    Modifier.testTag("panorama-export-document"), enabled) { SpaceCompassPanoramaActionIcon(false) }
                SpaceCompassSettingsActionButton(stringResource(R.string.panorama_share), onShare,
                    Modifier.testTag("panorama-export-share"), enabled) { SpaceCompassPanoramaActionIcon(true) }
            }
        })
}
