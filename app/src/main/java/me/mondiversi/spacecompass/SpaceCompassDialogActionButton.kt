package me.mondiversi.spacecompass

import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalResources

/** Dialog actions share locale-aware capitals and the existing destructive palette. */
@Composable
internal fun SpaceCompassDialogActionButton(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    destructive: Boolean = false
) {
    val locale = LocalResources.current.configuration.locales[0]
    TextButton(
        onClick = onClick,
        modifier = modifier,
        enabled = enabled,
        colors = ButtonDefaults.textButtonColors(
            contentColor = if (destructive) SpaceCompassDestructiveActionColor
                else MaterialTheme.colorScheme.primary
        )
    ) {
        Text(label.uppercase(locale))
    }
}
