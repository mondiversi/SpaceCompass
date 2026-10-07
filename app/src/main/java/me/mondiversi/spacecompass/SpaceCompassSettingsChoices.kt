package me.mondiversi.spacecompass

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
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

@Composable
internal fun SpaceCompassSettingsIsland(title: String? = null, iconKey: String? = null,
    headerContent: (@Composable () -> Unit)? = null, showHeaderDivider: Boolean = true,
    content: @Composable ColumnScope.() -> Unit) {
    val foreground = MaterialTheme.colorScheme.onSurface
    Surface(Modifier.fillMaxWidth(), shape = RoundedCornerShape(18.dp), color = spaceCompassSettingsCardColor(), contentColor = foreground,
        border = BorderStroke(1.dp, foreground.copy(alpha = .12f))) {
        Column(Modifier.fillMaxWidth().padding(spaceCompassIslandContentPadding), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            if (headerContent != null) {
                headerContent()
                if (showHeaderDivider) HorizontalDivider(color = foreground.copy(alpha = .12f))
            } else if (title != null) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    iconKey?.let { SpaceCompassSettingsGroupIcon(it, foreground) }
                    Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                }
                if (showHeaderDivider) HorizontalDivider(color = foreground.copy(alpha = .12f))
            }
            content()
        }
    }
}

/** Compact UVIR-style heading: decorative icon, checkbox and section name in one row. */
@Composable
internal fun SpaceCompassSettingsToggleIsland(title: String, iconKey: String, checked: Boolean,
    toggleDescription: String, onCheckedChange: (Boolean) -> Unit, modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit) {
    val foreground = MaterialTheme.colorScheme.onSurface
    SpaceCompassSettingsIsland(headerContent = {
        Row(modifier.fillMaxWidth()
            .toggleable(checked, role = Role.Checkbox, onValueChange = onCheckedChange)
            .spaceCompassAccessibleAction(label = toggleDescription, role = Role.Checkbox,
                checkedState = checked, onClick = { onCheckedChange(!checked) }),
            verticalAlignment = Alignment.CenterVertically) {
            SpaceCompassSettingsGroupIcon(iconKey, foreground)
            Spacer(Modifier.width(8.dp))
            // Keep the title's natural height; Compose expands the touch target into the card padding.
            // The whole heading owns activation, so the decorative checkbox must not toggle twice.
            CompositionLocalProvider(LocalMinimumInteractiveComponentSize provides 0.dp) {
                Checkbox(checked, onCheckedChange = null, modifier = Modifier.size(24.dp),
                    colors = spaceCompassCheckboxColors(uncheckedColor = foreground.copy(alpha = .6f)))
            }
            Spacer(Modifier.width(6.dp))
            Text(title, Modifier.weight(1f), style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold)
        }
    }, showHeaderDivider = checked, content = content)
}

@Composable
internal fun SpaceCompassSettingsChoices(spec: SpaceCompassSettingSpec, languageCodes: Boolean = false) {
    val preferences = LocalSpaceCompassPreferences.current ?: return
    val selected = LocalSpaceCompassPresentationSettings.current[spec.key] ?: spec.default
    val foreground = MaterialTheme.colorScheme.onSurface
    SpaceCompassSettingsIsland(if (languageCodes) null else stringResource(spec.title),
        iconKey = if (languageCodes) null else spec.key) {
        spec.description?.let { Text(it, color = foreground.copy(alpha = .65f), fontSize = 12.sp, lineHeight = 17.sp) }
        Column(Modifier.fillMaxWidth().selectableGroup()) {
            spec.options.forEach { (value, label) ->
                val checked = selected == value
                Row(Modifier.spaceCompassSettingsChoiceSurface(checked)
                    .selectable(checked, enabled = spec.enabled, role = Role.RadioButton, onClick = {
                        if (!checked) preferences.edit().putString(spec.key, value).apply()
                    }).testTag("setting-${spec.key}-$value").padding(horizontal = 8.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically) {
                    RadioButton(checked, onClick = null, enabled = spec.enabled,
                        colors = RadioButtonDefaults.colors(selectedColor = MaterialTheme.colorScheme.primary,
                            unselectedColor = foreground.copy(alpha = .6f)))
                    SpaceCompassSettingsChoiceLabel(label, Modifier.weight(1f), enabled = spec.enabled)
                    spec.examples[value]?.let { example ->
                        Text(example, Modifier.widthIn(max = 132.dp).padding(start = 8.dp).testTag("example-${spec.key}-$value"),
                            style = androidx.compose.ui.text.TextStyle(textDirection = androidx.compose.ui.text.style.TextDirection.ContentOrLtr),
                            fontSize = 11.sp, lineHeight = 14.sp,
                            color = foreground.copy(alpha = .65f),
                            textAlign = androidx.compose.ui.text.style.TextAlign.End)
                    }
                    if (languageCodes && value != "system") Surface(shape = RoundedCornerShape(6.dp), color = foreground.copy(alpha = .09f)) {
                        Box(Modifier.height(24.dp).widthIn(min = 32.dp).padding(horizontal = 6.dp),
                            contentAlignment = Alignment.Center) {
                            Text(value.substringBefore('-').uppercase(java.util.Locale.ROOT),
                                fontSize = 10.sp, lineHeight = 12.sp,
                                style = MaterialTheme.typography.labelSmall.copy(
                                    platformStyle = androidx.compose.ui.text.PlatformTextStyle(includeFontPadding = false)))
                        }
                    }
                }
            }
        }
    }
}

/** Checkbox settings use the same option row as appearance, language and units. */
@Composable
internal fun SpaceCompassSettingsCheckbox(label: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier) {
    val foreground = MaterialTheme.colorScheme.onSurface
    Row(modifier.spaceCompassSettingsChoiceSurface(checked)
        .toggleable(checked, role = Role.Checkbox, onValueChange = onCheckedChange)
        .padding(horizontal = 8.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
        Checkbox(checked, onCheckedChange = null,
            colors = spaceCompassCheckboxColors(uncheckedColor = foreground.copy(alpha = .6f)))
        SpaceCompassSettingsChoiceLabel(label, Modifier.weight(1f))
    }
}

@Composable
private fun Modifier.spaceCompassSettingsChoiceSurface(checked: Boolean): Modifier =
    fillMaxWidth().heightIn(min = 48.dp).clip(RoundedCornerShape(10.dp))
        .background(if (checked) MaterialTheme.colorScheme.onSurface.copy(alpha = .07f) else Color.Transparent)

@Composable
private fun SpaceCompassSettingsChoiceLabel(label: String, modifier: Modifier, enabled: Boolean = true) {
    Text(label, modifier.padding(horizontal = 8.dp), fontSize = 14.sp,
        color = MaterialTheme.colorScheme.onSurface.copy(alpha = if (enabled) 1f else .4f))
}
