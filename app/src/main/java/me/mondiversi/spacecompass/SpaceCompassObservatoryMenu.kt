package me.mondiversi.spacecompass

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp

@Composable
internal fun SpaceCompassObservatoryMenu(enabled: Boolean, onDailyPath: (() -> Unit)?,
    onHeights: () -> Unit, onEvents: () -> Unit, onPersonalMoments: () -> Unit, onExport: () -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    val dividerColor = MaterialTheme.colorScheme.onSurface.copy(alpha = .10f)
    Box {
        SpaceCompassTitleActionButton(stringResource(R.string.observatory_menu), { expanded = true },
            Modifier.testTag("observatory-menu"), enabled = enabled) {
            SpaceCompassIconCanvas(LocalContentColor.current, Modifier.size(SpaceCompassTitleActionIconSize)) { ink ->
                drawSpaceCompassMenuGlyph(ink, SpaceCompassTitleActionIconStrokeWidth.toPx())
            }
        }
        SpaceCompassAdaptiveDropdownMenu(expanded, { expanded = false },
            Modifier.testTag("observatory-menu-items"), containerColor = spaceCompassSettingsCardColor(),
            shape = RoundedCornerShape(16.dp)) {
            SpaceCompassNavigationMenuItem(stringResource(R.string.celestial_daily_path),
                { expanded = false; onDailyPath?.invoke() }, Modifier.testTag("observatory-path"),
                enabled = onDailyPath != null) { tint ->
                CompositionLocalProvider(LocalContentColor provides tint) {
                    SpaceCompassDailyPathIcon(SpaceCompassMenuIconSize, SpaceCompassMenuIconStrokeWidth)
                }
            }
            HorizontalDivider(color = dividerColor)
            SpaceCompassNavigationMenuItem(stringResource(R.string.observation_heights_title),
                { expanded = false; onHeights() }, Modifier.testTag("observatory-heights")) { tint ->
                SpaceCompassSettingsGroupIcon("sky_heights", tint)
            }
            HorizontalDivider(color = dividerColor)
            SpaceCompassNavigationMenuItem(stringResource(R.string.key_events_title),
                { expanded = false; onEvents() }, Modifier.testTag("observatory-events")) { tint ->
                SpaceCompassSettingsGroupIcon("events", tint)
            }
            HorizontalDivider(color = dividerColor)
            SpaceCompassNavigationMenuItem(stringResource(R.string.personal_moments_title),
                { expanded = false; onPersonalMoments() }, Modifier.testTag("observatory-personal-moments")) { tint ->
                SpaceCompassSettingsGroupIcon("personal_moments", tint)
            }
            HorizontalDivider(color = dividerColor)
            SpaceCompassNavigationMenuItem(stringResource(R.string.panorama_export),
                { expanded = false; onExport() }, Modifier.testTag("observatory-export")) { tint ->
                CompositionLocalProvider(LocalContentColor provides tint) {
                    SpaceCompassPanoramaExportIcon(SpaceCompassMenuIconStrokeWidth, SpaceCompassMenuIconSize)
                }
            }
        }
    }
}
