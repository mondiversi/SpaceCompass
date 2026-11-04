package me.mondiversi.spacecompass

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.time.Instant
import java.util.TimeZone

/** Match the shared checkbox/table layout and typography of Key elevations. */
@Composable
internal fun SpaceCompassKeyEventsRows(body: SpaceCompassCelestialBody, result: SpaceCompassKeyEventsUiState,
    secondaryText: Color) {
    val comparisons = LocalSpaceCompassComparisons.current
    val priority = rememberSpaceCompassComparisonPriority(body, SpaceCompassComparisonSource.EVENT)
    val zone = spaceCompassObservationZone()
    val profile = rememberSpaceCompassDataExportProfiles().selected
    val formatting = profile.formatting
    val timeZone = remember(zone) { TimeZone.getTimeZone(zone) }
    SpaceCompassSettingsDescription(stringResource(R.string.comparison_selection_note),
        Modifier.padding(horizontal = SpaceCompassObservationInkInset), compact = true)
    SpaceCompassSettingsDescription(stringResource(R.string.key_events_description),
        Modifier.padding(horizontal = SpaceCompassObservationInkInset), compact = true)
    if (!result.complete) Text(stringResource(R.string.pc_loading), Modifier.testTag("key-events-loading"),
        color = secondaryText, fontSize = 12.sp)
    if (result.complete && result.events.isNullOrEmpty()) Text(stringResource(when {
        !result.hasLocation -> R.string.celestial_view_location_needed
        result.events == null -> R.string.observation_moments_unavailable
        else -> R.string.key_events_empty
    }), Modifier.testTag("key-events-empty"), color = secondaryText, fontSize = 10.sp, lineHeight = 13.sp)
    if (!result.events.isNullOrEmpty()) SpaceCompassObservationTableHeader(secondaryText)
    val rows = remember(result.events, zone, priority) {
        result.events.orEmpty().withIndex().sortedByDescending { (_, event) ->
            spaceCompassComparisonOrigin(event.kind.name, event.timeMs, zone) in priority
        }
    }
    for ((index, event) in rows) {
        val label = stringResource(event.kind.title)
        val enabled = result.complete && comparisons != null && Instant.ofEpochMilli(event.timeMs).atZone(zone).year in 1900..2100
        val origin = spaceCompassComparisonOrigin(event.kind.name, event.timeMs, zone)
        val checked = comparisons?.selected(body, SpaceCompassComparisonSource.EVENT, origin) == true
        val data = remember(event, profile) { spaceCompassKeyEventValue(event, body, profile) }
        Row(Modifier.fillMaxWidth().testTag("key-event-row-$index"), verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            androidx.compose.material3.Checkbox(checked, {
                comparisons?.toggle(body, event.timeMs, zone, SpaceCompassComparisonSource.EVENT, origin)
            }, Modifier.size(32.dp).testTag("key-event-comparison-$index"), enabled = enabled, colors = spaceCompassCheckboxColors(uncheckedColor = secondaryText))
            Row(Modifier.weight(SpaceCompassObservationLabelWeight), verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                SpaceCompassKeyEventIcon(event.kind, secondaryText)
                Column(Modifier.weight(1f)) {
                    Text(label, color = secondaryText, fontSize = 12.sp, lineHeight = 16.sp)
                    if (data.isNotEmpty()) Text(data, color = secondaryText, fontSize = 10.sp, lineHeight = 13.sp,
                        style = TextStyle(textDirection = TextDirection.ContentOrLtr))
                }
            }
            SpaceCompassObservationAngles(event.azimuth, event.elevation, formatting, secondaryText,
                "key-event-$index", Modifier.weight(SpaceCompassObservationAnglesWeight))
            Column(Modifier.weight(SpaceCompassObservationDateWeight).padding(end = SpaceCompassObservationInkInset).defaultMinSize(minHeight = 48.dp)
                .testTag("key-event-date-$index"), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.End) {
                val stamp = remember(event.timeMs, formatting, zone) {
                    buildAnnotatedString {
                        append(formatSpaceCompassDateOnly(event.timeMs, formatting.date, formatting.deviceLocale,
                            timeZone, formatting.numeric, formatting.deviceLocale, allowHistorical = true))
                        append("\n")
                        append(formatSpaceCompassTimeOnly(event.timeMs, formatting.time, formatting.locale,
                            timeZone, formatting.numeric, formatting.deviceLocale, includeSeconds = false, allowHistorical = true))
                        withStyle(SpanStyle(fontSize = 10.sp)) {
                            append("\n" + formatSpaceCompassObserverUtcOffset(zone.rules.getOffset(Instant.ofEpochMilli(event.timeMs))))
                        }
                    }
                }
                Text(stamp, color = secondaryText,
                    fontSize = 12.sp, lineHeight = 16.sp, style = TextStyle(textDirection = TextDirection.ContentOrLtr))
            }
        }
    }
    if (!result.events.isNullOrEmpty()) SpaceCompassSettingsDescription(stringResource(if (body == SpaceCompassCelestialBody.SUN)
        R.string.key_events_horizon_note else R.string.key_events_global_note),
        Modifier.fillMaxWidth().padding(horizontal = SpaceCompassObservationInkInset), compact = true)
}

@Composable
private fun SpaceCompassKeyEventIcon(kind: SpaceCompassKeyEventKind, color: Color) {
    Canvas(Modifier.size(12.dp)) {
        val r = size.minDimension * .32f
        val stroke = Stroke(1.4.dp.toPx())
        drawCircle(color, r, center, style = stroke)
        when (kind) {
            SpaceCompassKeyEventKind.NEW_MOON, SpaceCompassKeyEventKind.POLAR_NIGHT_START,
            SpaceCompassKeyEventKind.POLAR_NIGHT_END -> Unit
            SpaceCompassKeyEventKind.FIRST_QUARTER -> drawArc(color, -90f, 180f, true,
                center-Offset(r,r), androidx.compose.ui.geometry.Size(2*r,2*r))
            SpaceCompassKeyEventKind.LAST_QUARTER -> drawArc(color, 90f, 180f, true,
                center-Offset(r,r), androidx.compose.ui.geometry.Size(2*r,2*r))
            SpaceCompassKeyEventKind.FULL_MOON, SpaceCompassKeyEventKind.POLAR_DAY_START,
            SpaceCompassKeyEventKind.POLAR_DAY_END -> drawCircle(color, r*.68f, center)
            else -> drawLine(color, center-Offset(r*.7f,0f), center+Offset(r*.7f,0f), 1.4.dp.toPx())
        }
    }
}
