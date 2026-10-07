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
import androidx.compose.ui.unit.dp

@Composable
internal fun SpaceCompassPanoramaCenterDialog(selected: SpaceCompassPanoramaCenter,
    onSelect: (SpaceCompassPanoramaCenter) -> Unit, onDismiss: () -> Unit) {
    val foreground = spaceCompassDialogContentColor()
    SpaceCompassAlertDialog(onDismissRequest = onDismiss, confirmButton = null,
        title = { Text(stringResource(R.string.panorama_center)) }, text = {
            Column(Modifier.fillMaxWidth().selectableGroup().testTag("panorama-center-dialog")) {
                SpaceCompassPanoramaCenter.entries.forEach { center ->
                    val checked = center == selected
                    Row(Modifier.fillMaxWidth().heightIn(min = 48.dp).clip(RoundedCornerShape(10.dp))
                        .background(if (checked) foreground.copy(alpha = .07f) else Color.Transparent)
                        .selectable(checked, role = Role.RadioButton, onClick = { onSelect(center) })
                        .testTag("panorama-center-${center.key}").padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically) {
                        RadioButton(checked, onClick = null)
                        Text(stringResource(center.labelResource), Modifier.weight(1f).padding(start = 8.dp))
                    }
                }
            }
        })
}
