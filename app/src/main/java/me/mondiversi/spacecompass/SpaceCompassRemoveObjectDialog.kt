package me.mondiversi.spacecompass

import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp

/** Match the adjacent observer/path actions, including their neutral fill and press feedback. */
@Composable
internal fun SpaceCompassRemoveObjectButton(name: String, onClick: () -> Unit) {
    val label = stringResource(R.string.celestial_remove_description, name)
    SpaceCompassCelestialIconControl(false, label, onClick,
        Modifier.testTag("celestial-body-remove"), Role.Button) {
        SpaceCompassIconCanvas(Color.White, Modifier.size(18.dp)) { color ->
            drawLine(color, Offset(size.width*.22f, size.height*.22f),
                Offset(size.width*.78f, size.height*.78f), 1.8.dp.toPx(), StrokeCap.Round)
            drawLine(color, Offset(size.width*.78f, size.height*.22f),
                Offset(size.width*.22f, size.height*.78f), 1.8.dp.toPx(), StrokeCap.Round)
        }
    }
}

@Composable
internal fun SpaceCompassRemoveObjectDialog(body: SpaceCompassCelestialBody,
    onDismiss: () -> Unit, onConfirm: () -> Unit) {
    val name = stringResource(body.nameResource)
    SpaceCompassAlertDialog(onDismissRequest = onDismiss,
        modifier = Modifier.testTag("celestial-remove-dialog"),
        confirmButtonFirst = true,
        title = { Text(stringResource(R.string.celestial_remove_title)) },
        text = { Text(stringResource(R.string.celestial_remove_message, name)) },
        confirmButton = {
            SpaceCompassDialogActionButton(stringResource(R.string.celestial_remove_action),
                onClick = onConfirm, modifier = Modifier.testTag("celestial-remove-confirm"), destructive = true)
        }, dismissButton = {
            SpaceCompassDialogActionButton(stringResource(R.string.celestial_remove_cancel),
                onClick = onDismiss, modifier = Modifier.testTag("celestial-remove-cancel"))
        })
}
