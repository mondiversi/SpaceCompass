package me.mondiversi.spacecompass

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.edit

@Composable
internal fun SpaceCompassScreensaverIsland() {
    val preferences = LocalSpaceCompassPreferences.current ?: return
    val settings = LocalSpaceCompassScreensaverSettings.current
    val context = LocalContext.current
    val saved = stringResource(R.string.settings_saved)
    val title = stringResource(R.string.screensaver_title)
    SpaceCompassSettingsToggleIsland(title, "screensaver", settings.enabled, title,
        onCheckedChange = { enabled ->
            if (enabled != settings.enabled) {
                preferences.edit { putBoolean(SPACE_COMPASS_SCREENSAVER_ENABLED_KEY, enabled) }
                showSpaceCompassBottomMessage(context, saved)
            }
        }, modifier = Modifier.testTag("screensaver-enabled")) {
        SpaceCompassSettingsDescription(stringResource(R.string.screensaver_description))
        val delayTitle = stringResource(R.string.screensaver_delay)
        val foreground = MaterialTheme.colorScheme.onSurface
        var expanded by remember { mutableStateOf(false) }
        val options = spaceCompassScreensaverDelays.map { seconds ->
            val amount = if (seconds < 60) seconds else seconds / 60
            seconds to stringResource(if (seconds < 60) R.string.screensaver_seconds else R.string.screensaver_minutes,
                formatSpaceCompassNumber(amount.toDouble(), 0, LocalSpaceCompassNumericFormat.current,
                    systemLocale = LocalSpaceCompassDeviceLocale.current))
        }
        Text(delayTitle, fontSize = 13.sp, fontWeight = FontWeight.Medium)
        Box(Modifier.fillMaxWidth()) {
            Surface(onClick = { expanded = true }, color = Color.Transparent,
                shape = MaterialTheme.shapes.small, border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp).testTag("screensaver-delay-field")
                    .semantics { contentDescription = delayTitle }) {
                Row(Modifier.padding(horizontal = 12.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(options.first { it.first == settings.delaySeconds }.second, Modifier.weight(1f),
                        fontSize = 14.sp, lineHeight = 18.sp)
                    Canvas(Modifier.size(16.dp)) {
                        val stroke = 1.7.dp.toPx()
                        drawLine(foreground, Offset(size.width * .2f, size.height * .35f),
                            Offset(size.width * .5f, size.height * .65f), stroke, StrokeCap.Round)
                        drawLine(foreground, Offset(size.width * .5f, size.height * .65f),
                            Offset(size.width * .8f, size.height * .35f), stroke, StrokeCap.Round)
                    }
                }
            }
            SpaceCompassAdaptiveDropdownMenu(expanded, { expanded = false },
                modifier = Modifier.widthIn(min = 180.dp).selectableGroup().testTag("screensaver-delay-menu"),
                containerColor = spaceCompassSettingsCardColor()) {
                options.forEach { (seconds, label) ->
                    val checked = settings.delaySeconds == seconds
                    DropdownMenuItem(text = {
                        Text(label, fontSize = 14.sp, fontWeight = if (checked) FontWeight.SemiBold else FontWeight.Medium,
                            color = if (checked) MaterialTheme.colorScheme.primary else foreground)
                    }, modifier = Modifier.testTag("screensaver-delay-$seconds").semantics { selected = checked },
                        onClick = {
                            expanded = false
                            if (!checked) {
                                preferences.edit { putInt(SPACE_COMPASS_SCREENSAVER_DELAY_KEY, seconds) }
                                showSpaceCompassBottomMessage(context, saved)
                            }
                        })
                }
            }
        }
    }
}
