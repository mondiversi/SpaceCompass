package me.mondiversi.spacecompass

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp

/** A narrower hit region keeps removal separate from the adjacent navigation arrow. */
@Composable
internal fun SpaceCompassRemoveObjectButton(name: String, onClick: () -> Unit, tint: Color) {
    val label = stringResource(R.string.celestial_remove_description, name)
    val interaction = remember { MutableInteractionSource() }
    SpaceCompassFloatingControlHitRegion {
        // Keep the glyph centered in its layout slot while shrinking only its hit bounds.
        Box(Modifier.size(48.dp), contentAlignment = Alignment.Center) {
            Box(Modifier.size(width = 24.dp, height = 32.dp).testTag("celestial-body-remove")
                .clickable(interactionSource = interaction, indication = null, onClick = onClick)
                .spaceCompassAccessibleAction(label, onClick = onClick), contentAlignment = Alignment.Center) {
                SpaceCompassObjectHeaderIcon(SpaceCompassObjectHeaderGlyph.REMOVE, tint = tint)
            }
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
