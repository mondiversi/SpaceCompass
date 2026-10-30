package me.mondiversi.spacecompass

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/** Editors stay fully visible and usable; activation belongs only to the management menu. */
@Composable
internal fun SpaceCompassScenarioParameterIsland(
    contentPadding: PaddingValues = spaceCompassIslandContentPadding,
    content: @Composable ColumnScope.() -> Unit) {
    Surface(Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp),
        color = spaceCompassSettingsCardColor(), contentColor = MaterialTheme.colorScheme.onSurface) {
        Column(Modifier.fillMaxWidth().padding(contentPadding),
            verticalArrangement = Arrangement.spacedBy(SpaceCompassSettingsGroupGap), content = content)
    }
}
