package me.mondiversi.spacecompass

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.time.Instant
import java.util.TimeZone

/** One selected object/year is evaluated off the UI thread, independently of sensor/live ticks. */
@Composable
internal fun SpaceCompassObservationMomentsRows(body: SpaceCompassCelestialBody, result: SpaceCompassObservationMomentsUiState,
    secondaryText: Color, modifier: Modifier = Modifier,
    scroll: LazyListState = rememberLazyListState()) {
    val comparisons = LocalSpaceCompassComparisons.current
    val priority = rememberSpaceCompassComparisonPriority(body, SpaceCompassComparisonSource.HEIGHT)
    val zone = spaceCompassObservationZone()
    val profile = rememberSpaceCompassDataExportProfiles().selected
    val formatting = profile.formatting
    val timeZone = remember(zone) { TimeZone.getTimeZone(zone) }
    val rows = remember(result.moments, zone, priority) {
        val ordered = result.moments?.let(::spaceCompassObservationMomentRows)
            ?: SpaceCompassObservationMomentKind.entries.filter { it != SpaceCompassObservationMomentKind.MONTHLY }.map {
                SpaceCompassObservationMomentRow(it, null, annualExtreme = it == SpaceCompassObservationMomentKind.MAXIMUM ||
                    it == SpaceCompassObservationMomentKind.MINIMUM)
            }
        ordered.withIndex().sortedByDescending { (_, row) ->
            row.point?.let { spaceCompassComparisonOrigin(row.kind.name, it.timeMs, zone) in priority } == true
        }
    }
    LazyColumn(modifier, state = scroll, contentPadding = SpaceCompassObservationContentPadding,
        verticalArrangement = Arrangement.spacedBy(8.dp)) {
        item(key = "selection-note") {
            SpaceCompassSettingsDescription(stringResource(R.string.comparison_selection_note),
                Modifier.padding(horizontal = SpaceCompassObservationInkInset), compact = true)
        }
        item(key = "description") {
            SpaceCompassSettingsDescription(stringResource(R.string.observation_moments_year),
                Modifier.padding(horizontal = SpaceCompassObservationInkInset), compact = true)
        }
        if (result.complete && result.moments == null) item(key = "unavailable") {
            Text(stringResource(if (!result.hasLocation)
                R.string.celestial_view_location_needed else R.string.observation_moments_unavailable),
                color = secondaryText, fontSize = 10.sp, lineHeight = 13.sp)
        }
        item(key = "table-header") { SpaceCompassObservationTableHeader(secondaryText) }
        items(rows, key = { row -> "${row.value.kind.name}:${row.value.point?.timeMs}" }, contentType = { "observation-row" }) { (index, entry) ->
            val kind = entry.kind
            val point = entry.point
            val label = remember(entry, profile) { spaceCompassObservationMomentTitle(entry, profile) }
            val enabled = result.complete && point != null && comparisons != null &&
                Instant.ofEpochMilli(point.timeMs).atZone(zone).year in 1900..2100
            val origin = point?.let { spaceCompassComparisonOrigin(kind.name, it.timeMs, zone) }
            val checked = origin != null && comparisons?.selected(body, SpaceCompassComparisonSource.HEIGHT, origin) == true
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                androidx.compose.material3.Checkbox(checked, { if (point != null && origin != null)
                    comparisons?.toggle(body, point.timeMs, zone, SpaceCompassComparisonSource.HEIGHT, origin, entry.annualExtreme) },
                    Modifier.size(32.dp).testTag("observation-comparison-$index"), enabled = enabled, colors = spaceCompassCheckboxColors(uncheckedColor = secondaryText))
                Row(Modifier.weight(SpaceCompassObservationLabelWeight).testTag("observation-moment-label-$index"),
                    verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    SpaceCompassObservationMomentIcon(kind, secondaryText,
                        Modifier.testTag("observation-moment-icon-${kind.name.lowercase()}-$index"))
                    Text(label, Modifier.weight(1f), color = secondaryText, fontSize = 12.sp, lineHeight = 16.sp)
                }
                SpaceCompassObservationAngles(point?.position?.azimuthDegrees, point?.position?.elevationDegrees,
                    formatting, secondaryText, "observation-moment-$index", Modifier.weight(SpaceCompassObservationAnglesWeight))
                Column(Modifier.weight(SpaceCompassObservationDateWeight).padding(end = SpaceCompassObservationInkInset).defaultMinSize(minHeight = 48.dp)
                    .testTag("observation-moment-${kind.name.lowercase()}-$index"), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.End) {
                    val valueText = remember(point, formatting, zone) { point?.let {
                        val dateText = formatSpaceCompassDateOnly(it.timeMs, formatting.date,
                            formatting.deviceLocale, timeZone, formatting.numeric, formatting.deviceLocale, allowHistorical = true)
                        val clockText = formatSpaceCompassTimeOnly(it.timeMs, formatting.time,
                            formatting.locale, timeZone, formatting.numeric, formatting.deviceLocale, includeSeconds = false,
                            allowHistorical = true)
                        val offset = formatSpaceCompassObserverUtcOffset(zone.rules.getOffset(Instant.ofEpochMilli(it.timeMs)))
                        buildAnnotatedString {
                            append(dateText); append("\n"); append(clockText)
                            withStyle(SpanStyle(fontSize = 10.sp)) { append("\n$offset") }
                        }
                    } }
                    Text(valueText ?: AnnotatedString(if (!result.complete) stringResource(R.string.pc_loading) else "—"),
                        color = secondaryText,
                        fontSize = 12.sp, lineHeight = 16.sp, style = TextStyle(textDirection = TextDirection.ContentOrLtr))
                }
            }
        }
        if (result.moments != null) item(key = "footer") {
            SpaceCompassSettingsDescription(stringResource(R.string.observation_moments_midpoint_note),
                Modifier.fillMaxWidth().padding(horizontal = SpaceCompassObservationInkInset), compact = true)
        }
    }
}
