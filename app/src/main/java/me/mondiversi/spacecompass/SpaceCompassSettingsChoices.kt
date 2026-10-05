package me.mondiversi.spacecompass

import androidx.compose.foundation.*
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
internal fun SpaceCompassSettingsIsland(title: String? = null, iconKey: String? = null, content: @Composable ColumnScope.() -> Unit) {
    val foreground = MaterialTheme.colorScheme.onSurface
    Surface(Modifier.fillMaxWidth(), shape = RoundedCornerShape(18.dp), color = spaceCompassSettingsCardColor(), contentColor = foreground,
        border = BorderStroke(1.dp, foreground.copy(alpha = .12f))) {
        Column(Modifier.fillMaxWidth().padding(spaceCompassIslandContentPadding), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            if (title != null) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    iconKey?.let { SpaceCompassSettingsGroupIcon(it, foreground) }
                    Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                }
                HorizontalDivider(color = foreground.copy(alpha = .12f))
            }
            content()
        }
    }
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
                Row(Modifier.fillMaxWidth().heightIn(min = 48.dp).clip(RoundedCornerShape(10.dp))
                    .background(if (checked) foreground.copy(alpha = .07f) else Color.Transparent)
                    .selectable(checked, enabled = spec.enabled, role = Role.RadioButton, onClick = {
                        preferences.edit().putString(spec.key, value).apply()
                    }).testTag("setting-${spec.key}-$value").padding(horizontal = 8.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically) {
                    RadioButton(checked, onClick = null, enabled = spec.enabled,
                        colors = RadioButtonDefaults.colors(selectedColor = MaterialTheme.colorScheme.primary,
                            unselectedColor = foreground.copy(alpha = .6f)))
                    Text(label, Modifier.weight(1f).padding(horizontal = 8.dp), fontSize = 14.sp,
                        color = foreground.copy(alpha = if (spec.enabled) 1f else .4f))
                    spec.examples[value]?.let { example ->
                        Text(example, Modifier.padding(start = 8.dp).testTag("example-${spec.key}-$value"),
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
