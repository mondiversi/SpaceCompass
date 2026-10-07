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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

internal val SpaceCompassPanoramaPosition.labelResource: Int get() = when (this) {
    SpaceCompassPanoramaPosition.COMPLETE -> R.string.panorama_position_complete
    SpaceCompassPanoramaPosition.AREA -> R.string.panorama_position_area
    SpaceCompassPanoramaPosition.HIDDEN -> R.string.panorama_position_hidden
}

@Composable
internal fun SpaceCompassPanoramaPositionDialog(selected: SpaceCompassPanoramaPosition, rotateContent: Boolean,
    onSelect: (SpaceCompassPanoramaPosition) -> Unit, onDismiss: () -> Unit) {
    val foreground = spaceCompassDialogContentColor()
    SpaceCompassAlertDialog(onDismissRequest = onDismiss, confirmButton = null, rotateContent = rotateContent,
        title = { Text(stringResource(R.string.panorama_position)) }, text = {
            Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(stringResource(R.string.panorama_position_description), color = foreground.copy(alpha = .72f),
                    fontSize = 12.sp, lineHeight = 17.sp)
                Column(Modifier.fillMaxWidth().selectableGroup()) {
                    SpaceCompassPanoramaPosition.entries.forEach { position ->
                        val checked = position == selected
                        val detail = when (position) {
                            SpaceCompassPanoramaPosition.COMPLETE -> R.string.panorama_position_complete_detail
                            SpaceCompassPanoramaPosition.AREA -> R.string.panorama_position_area_detail
                            SpaceCompassPanoramaPosition.HIDDEN -> R.string.panorama_position_hidden_detail
                        }
                        Row(Modifier.fillMaxWidth().heightIn(min = 48.dp).clip(RoundedCornerShape(10.dp))
                            .background(if (checked) foreground.copy(alpha = .07f) else androidx.compose.ui.graphics.Color.Transparent)
                            .selectable(checked, role = Role.RadioButton, onClick = { onSelect(position) })
                            .testTag("panorama-position-${position.key}").padding(horizontal = 8.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically) {
                            RadioButton(checked, onClick = null)
                            Column(Modifier.weight(1f).padding(start = 8.dp)) {
                                Text(stringResource(position.labelResource), fontSize = 14.sp, color = foreground)
                                Text(stringResource(detail), fontSize = 11.sp, lineHeight = 15.sp,
                                    color = foreground.copy(alpha = .65f))
                            }
                        }
                    }
                }
            }
        })
}
