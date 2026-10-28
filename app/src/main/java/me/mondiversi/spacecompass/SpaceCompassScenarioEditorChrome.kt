package me.mondiversi.spacecompass

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.disabled
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp

/** One activation control in the child toolbar, using the shared day/night form accent. */
@Composable
internal fun SpaceCompassScenarioActivation(checked: Boolean, label: String, tag: String,
    onCheckedChange: (Boolean) -> Unit) {
    val foreground = MaterialTheme.colorScheme.onSurface
    Box(Modifier.size(SpaceCompassTitleActionButtonSize).testTag(tag)
        .toggleable(checked, role = Role.Checkbox, onValueChange = onCheckedChange)
        .spaceCompassAccessibleAction(label, role = Role.Checkbox, checkedState = checked,
            onClick = { onCheckedChange(!checked) }), contentAlignment = Alignment.Center) {
        CompositionLocalProvider(LocalMinimumInteractiveComponentSize provides 0.dp) {
            Checkbox(checked, onCheckedChange = null, modifier = Modifier.size(SpaceCompassSettingsControlSize),
                colors = spaceCompassCheckboxColors(uncheckedColor = foreground.copy(alpha = .72f)))
        }
    }
}

/** Keep the whole form visible. Its actual controls own disabled input/semantics, including keyboard edits. */
@Composable
internal fun SpaceCompassScenarioParameterIsland(checked: Boolean, content: @Composable ColumnScope.() -> Unit) {
    val scheme = MaterialTheme.colorScheme
    val opacity by animateFloatAsState(if (checked) 1f else .48f,
        tween(SPACE_COMPASS_ISLAND_REVEAL_DURATION_MS), label = "scenario-form-opacity")
    Surface(Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp),
        color = spaceCompassSettingsCardColor(), contentColor = scheme.onSurface) {
        // Disabled fields and their custom slider track share the neutral grey treatment.
        MaterialTheme(colorScheme = if (checked) scheme else scheme.copy(primary = scheme.onSurface,
            onPrimary = scheme.surface)) {
            Column(Modifier.fillMaxWidth().padding(spaceCompassIslandContentPadding).alpha(opacity)
                .semantics { if (!checked) disabled() },
                verticalArrangement = Arrangement.spacedBy(SpaceCompassSettingsGroupGap), content = content)
        }
    }
}
