package me.mondiversi.spacecompass

import androidx.compose.foundation.background
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
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
internal fun SpaceCompassSettingsIsland(title: String? = null, iconKey: String? = null,
    headerContent: (@Composable () -> Unit)? = null, showHeaderDivider: Boolean = true,
    content: @Composable ColumnScope.() -> Unit) {
    val foreground = MaterialTheme.colorScheme.onSurface
    Surface(Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp),
        color = spaceCompassSettingsCardColor(), contentColor = foreground) {
        Column(Modifier.fillMaxWidth().padding(spaceCompassIslandContentPadding),
            verticalArrangement = Arrangement.spacedBy(SpaceCompassSettingsGroupGap)) {
            if (headerContent != null) {
                headerContent()
                if (showHeaderDivider) HorizontalDivider(color = foreground.copy(alpha = .20f))
            } else if (title != null) {
                Row(verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    iconKey?.let { SpaceCompassSettingsTitleIcon(it, foreground) }
                    Text(title, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                }
                if (showHeaderDivider) HorizontalDivider(color = foreground.copy(alpha = .20f))
            }
            content()
        }
    }
}

/** UVIR centers its 20 dp heading glyph in a neutral 28 dp alignment slot. */
@Composable
private fun SpaceCompassSettingsTitleIcon(key: String, tint: Color) {
    Box(Modifier.size(28.dp), contentAlignment = Alignment.Center) {
        SpaceCompassSettingsGroupIcon(key, tint)
    }
}

/** The whole heading owns activation; the decorative checkbox never toggles twice. */
@Composable
internal fun SpaceCompassSettingsToggleIsland(title: String, iconKey: String, checked: Boolean,
    toggleDescription: String, onCheckedChange: (Boolean) -> Unit, modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit) {
    val foreground = MaterialTheme.colorScheme.onSurface
    SpaceCompassSettingsIsland(headerContent = {
        Column(Modifier.fillMaxWidth()) {
            Row(modifier.fillMaxWidth()
                .toggleable(checked, role = Role.Checkbox, onValueChange = onCheckedChange)
                .spaceCompassAccessibleAction(label = toggleDescription, role = Role.Checkbox,
                    checkedState = checked, onClick = { onCheckedChange(!checked) }),
                verticalAlignment = Alignment.CenterVertically) {
                SpaceCompassSettingsTitleIcon(iconKey, foreground)
                Spacer(Modifier.width(8.dp))
                // Match UVIR's natural heading height; the touch target expands into card padding.
                CompositionLocalProvider(LocalMinimumInteractiveComponentSize provides 0.dp) {
                    Checkbox(checked, onCheckedChange = null, modifier = Modifier.size(SpaceCompassSettingsControlSize),
                        colors = spaceCompassCheckboxColors(uncheckedColor = foreground.copy(alpha = .72f)))
                }
                Spacer(Modifier.width(6.dp))
                Text(title, Modifier.weight(1f), fontSize = 15.sp,
                    fontWeight = if (checked) FontWeight.Bold else FontWeight.Normal)
            }
            SpaceCompassSettingsIslandReveal(checked) {
                HorizontalDivider(color = foreground.copy(alpha = .20f))
                content()
            }
        }
    }, showHeaderDivider = false, content = {})
}

/** Shared compact row for settings and the searchable time-zone list. */
@Composable
internal fun SpaceCompassSettingsRadioRow(checked: Boolean, onClick: () -> Unit,
    modifier: Modifier = Modifier, enabled: Boolean = true,
    content: @Composable RowScope.() -> Unit) {
    val foreground = MaterialTheme.colorScheme.onSurface
    Row(modifier.spaceCompassSettingsChoiceSurface(checked)
        .selectable(checked, enabled = enabled, role = Role.RadioButton, onClick = onClick)
        .padding(horizontal = SpaceCompassSettingsChoiceHorizontalPadding,
            vertical = SpaceCompassSettingsChoiceVerticalPadding),
        verticalAlignment = Alignment.CenterVertically) {
        CompositionLocalProvider(LocalMinimumInteractiveComponentSize provides 0.dp) {
            RadioButton(checked, onClick = null, enabled = enabled,
                modifier = Modifier.size(SpaceCompassSettingsControlSize),
                colors = RadioButtonDefaults.colors(selectedColor = MaterialTheme.colorScheme.primary,
                    unselectedColor = foreground.copy(alpha = .72f),
                    disabledSelectedColor = foreground.copy(alpha = .72f * .62f),
                    disabledUnselectedColor = foreground.copy(alpha = .72f * .62f)))
        }
        Spacer(Modifier.width(10.dp))
        content()
    }
}

@Composable
internal fun SpaceCompassSettingsChoices(spec: SpaceCompassSettingSpec, languageCodes: Boolean = false) {
    val preferences = LocalSpaceCompassPreferences.current ?: return
    val selected = LocalSpaceCompassPresentationSettings.current[spec.key] ?: spec.default
    val foreground = MaterialTheme.colorScheme.onSurface
    SpaceCompassSettingsIsland(if (languageCodes) null else stringResource(spec.title),
        iconKey = if (languageCodes) null else spec.key) {
        Column(Modifier.fillMaxWidth().selectableGroup(),
            verticalArrangement = Arrangement.spacedBy(SpaceCompassSettingsChoiceSpacing)) {
            spec.description?.let {
                SpaceCompassSettingsDescription(it, pageDescription = languageCodes)
                Spacer(Modifier.height(4.dp))
            }
            spec.options.forEach { (value, label) ->
                val checked = selected == value
                SpaceCompassSettingsRadioRow(checked, enabled = spec.enabled,
                    modifier = Modifier.testTag("setting-${spec.key}-$value"), onClick = {
                        if (!checked) preferences.edit().putString(spec.key, value).apply()
                    }) {
                    SpaceCompassSettingsChoiceLabel(label, Modifier.weight(1f), enabled = spec.enabled)
                    spec.examples[value]?.let { example ->
                        Text(example, Modifier.widthIn(max = 132.dp).padding(start = 8.dp)
                            .testTag("example-${spec.key}-$value"),
                            style = TextStyle(textDirection = TextDirection.ContentOrLtr),
                            fontSize = 11.sp, lineHeight = 14.sp, fontWeight = FontWeight.Medium,
                            color = foreground.copy(alpha = if (spec.enabled) .72f else .72f * .62f),
                            textAlign = TextAlign.End)
                    }
                    if (languageCodes && value != "system") {
                        Text(value.substringBefore('-').uppercase(java.util.Locale.ROOT),
                            Modifier.width(with(LocalDensity.current) { 36.sp.toDp() }),
                            fontSize = 14.sp, lineHeight = 20.sp, fontWeight = FontWeight.Medium,
                            color = foreground.copy(alpha = .72f), textAlign = TextAlign.Center,
                            maxLines = 1, style = TextStyle(textDirection = TextDirection.Ltr))
                    }
                }
            }
        }
    }
}

@Composable
internal fun SpaceCompassSettingsCheckbox(label: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier) {
    val foreground = MaterialTheme.colorScheme.onSurface
    Row(modifier.fillMaxWidth().heightIn(min = 48.dp)
        .toggleable(checked, role = Role.Checkbox, onValueChange = onCheckedChange),
        verticalAlignment = Alignment.CenterVertically) {
        CompositionLocalProvider(LocalMinimumInteractiveComponentSize provides 0.dp) {
            Checkbox(checked, onCheckedChange = null, modifier = Modifier.size(SpaceCompassSettingsControlSize),
                colors = spaceCompassCheckboxColors(uncheckedColor = foreground.copy(alpha = .72f)))
        }
        Spacer(Modifier.width(6.dp))
        SpaceCompassSettingsChoiceLabel(label, Modifier.weight(1f), weight = FontWeight.Normal)
    }
}

@Composable
private fun Modifier.spaceCompassSettingsChoiceSurface(checked: Boolean): Modifier =
    fillMaxWidth().clip(RoundedCornerShape(10.dp))
        .background(if (checked) MaterialTheme.colorScheme.onSurface.copy(alpha = .08f) else Color.Transparent)

@Composable
private fun SpaceCompassSettingsChoiceLabel(label: String, modifier: Modifier, enabled: Boolean = true,
    weight: FontWeight = FontWeight.Medium) {
    Text(label, modifier, fontSize = 14.sp, fontWeight = weight,
        color = MaterialTheme.colorScheme.onSurface.copy(alpha = if (enabled) 1f else .72f * .62f))
}
