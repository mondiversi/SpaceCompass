package me.mondiversi.spacecompass

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

/** Closed settings selectors share the compact text-field insets; menu rows keep their targets. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun SpaceCompassCompactSelectionField(onClick: () -> Unit, modifier: Modifier = Modifier,
    enabled: Boolean = true, content: @Composable RowScope.() -> Unit) {
    CompositionLocalProvider(LocalMinimumInteractiveComponentSize provides SpaceCompassCompactFieldIconSize) {
        Surface(onClick, modifier.fillMaxWidth().heightIn(min = SpaceCompassCompactFieldMinHeight),
            enabled = enabled, shape = MaterialTheme.shapes.small, color = Color.Transparent,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = if (enabled) 1f else .4f))) {
            Row(Modifier.padding(SpaceCompassCompactFieldContentPadding),
                verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp),
                content = content)
        }
    }
}
