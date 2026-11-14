package me.mondiversi.spacecompass

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalResources

/** Tone describes the action, independently of its localized caption. */
internal enum class SpaceCompassDialogActionTone { PRIMARY, NEUTRAL, DESTRUCTIVE }

/** Dialog actions share locale-aware capitals, normal text and the existing accent palettes. */
@Composable
internal fun SpaceCompassDialogActionButton(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    tone: SpaceCompassDialogActionTone = SpaceCompassDialogActionTone.PRIMARY,
    contentPadding: PaddingValues = ButtonDefaults.TextButtonContentPadding
) {
    val locale = LocalResources.current.configuration.locales[0]
    TextButton(
        onClick = onClick,
        modifier = modifier,
        enabled = enabled,
        contentPadding = contentPadding,
        colors = ButtonDefaults.textButtonColors(
            contentColor = when (tone) {
                SpaceCompassDialogActionTone.PRIMARY -> MaterialTheme.colorScheme.primary
                SpaceCompassDialogActionTone.NEUTRAL -> spaceCompassDialogContentColor()
                SpaceCompassDialogActionTone.DESTRUCTIVE -> SpaceCompassDestructiveActionColor
            }
        )
    ) {
        Text(label.uppercase(locale))
    }
}
