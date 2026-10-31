package me.mondiversi.spacecompass

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.time.Instant
import java.time.ZoneId
import java.util.TimeZone

/** One selected object/year is evaluated off the UI thread, independently of sensor/live ticks. */
@Composable
internal fun SpaceCompassObservationMomentsRows(result: SpaceCompassObservationMomentsUiState,
    secondaryText: Color) {
    val context = LocalContext.current
    val observer = LocalSpaceCompassObserver.current
    val zone = spaceCompassObservationZone()
    val profile = rememberSpaceCompassDataExportProfiles().selected
    val formatting = profile.formatting
    val timeZone = remember(zone) { TimeZone.getTimeZone(zone) }
    val appliedMessage = stringResource(R.string.observation_moment_applied)
    Text(stringResource(R.string.observation_moments_year), color = secondaryText, fontSize = 10.sp, lineHeight = 13.sp)
    if (result.complete && result.moments == null) Text(stringResource(if (!result.hasLocation)
        R.string.celestial_view_location_needed else R.string.observation_moments_unavailable),
        color = secondaryText, fontSize = 10.sp, lineHeight = 13.sp)
    val prepared = remember(result.moments) { result.moments?.let(::spaceCompassObservationMomentRows) }
    val rows = prepared ?: SpaceCompassObservationMomentKind.entries.map {
        SpaceCompassObservationMomentRow(it, null, annualExtreme = it == SpaceCompassObservationMomentKind.MAXIMUM ||
            it == SpaceCompassObservationMomentKind.MINIMUM)
    }
    for ((index, entry) in rows.withIndex()) {
        val kind = entry.kind
        val point = entry.point
        val label = spaceCompassObservationMomentTitle(entry, profile)
        val enabled = result.complete && point != null && observer != null &&
            Instant.ofEpochMilli(point.timeMs).atZone(zone).year in 1900..2100
        val elevation = remember(point, formatting) { point?.let {
            formatSpaceCompassPanoramaElevation(it.position.elevationDegrees, formatting) + "°"
        } ?: "—" }
        val elevationDescription = stringResource(R.string.celestial_point_elevation, elevation)
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(Modifier.weight(.40f).testTag("observation-moment-label-$index"),
                verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                SpaceCompassObservationMomentIcon(kind, secondaryText,
                    Modifier.testTag("observation-moment-icon-${kind.name.lowercase()}-$index"))
                Text(label, Modifier.weight(1f), color = secondaryText, fontSize = 12.sp, lineHeight = 16.sp)
            }
            Text(elevation, Modifier.weight(.15f).testTag("observation-moment-elevation-$index")
                .semantics { contentDescription = elevationDescription }, color = secondaryText,
                fontSize = 12.sp, lineHeight = 16.sp, textAlign = TextAlign.Center,
                style = TextStyle(textDirection = TextDirection.ContentOrLtr))
            Column(Modifier.weight(.45f).defaultMinSize(minHeight = 48.dp)
                .testTag("observation-moment-${kind.name.lowercase()}-$index")
                .clickable(enabled = enabled, onClickLabel = label, role = Role.Button) {
                    if (point != null && observer?.observeAt(point.timeMs, ZoneId.systemDefault()) == true)
                        showSpaceCompassBottomMessage(context, appliedMessage)
                }, verticalArrangement = Arrangement.Center) {
                val valueText = remember(point, formatting, zone) { point?.let {
                    val dateText = formatSpaceCompassDateOnly(it.timeMs, formatting.date,
                        formatting.deviceLocale, timeZone, formatting.numeric, formatting.deviceLocale, allowHistorical = true)
                    val clockText = formatSpaceCompassTimeOnly(it.timeMs, formatting.time,
                        formatting.locale, timeZone, formatting.numeric, formatting.deviceLocale, includeSeconds = false,
                        allowHistorical = true)
                    val offset = formatSpaceCompassObserverUtcOffset(zone.rules.getOffset(Instant.ofEpochMilli(it.timeMs)))
                    buildAnnotatedString {
                        append(dateText); append("\n"); append(clockText)
                        withStyle(SpanStyle(fontSize = 10.sp)) { append(" · $offset") }
                    }
                } }
                Text(valueText ?: AnnotatedString(if (!result.complete) stringResource(R.string.pc_loading) else "—"),
                    color = if (enabled) MaterialTheme.colorScheme.primary else secondaryText,
                    fontSize = 12.sp, lineHeight = 16.sp, style = TextStyle(textDirection = TextDirection.ContentOrLtr))
            }
        }
    }
    if (result.moments != null) Text(stringResource(R.string.observation_moments_midpoint_note),
        color = secondaryText, fontSize = 10.sp, lineHeight = 13.sp)
}
