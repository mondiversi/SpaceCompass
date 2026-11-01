package me.mondiversi.spacecompass

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.time.Instant
import kotlinx.coroutines.launch

/** Independent annual browsing uses the daily-path page chrome, without touching the Location. */
@Composable
internal fun SpaceCompassKeyEventsPage(body: SpaceCompassCelestialBody, timeMs: Long,
    latitude: Double?, longitude: Double?, altitude: Double, remote: SpaceCompassCelestialRemoteData,
    onBack: () -> Unit) {
    val foreground = MaterialTheme.colorScheme.onSurface
    val secondary = foreground.copy(alpha = .72f)
    val pageBackground = spaceCompassSurfacePalette(spaceCompassPageBackground().luminance() < .4f).island
    val visit = rememberSpaceCompassObjectPageScroll(body, SpaceCompassObjectPage.KEY_EVENTS)
    val scroll = visit.entry.scroll
    val zone = spaceCompassObservationZone()
    val locationYear = Instant.ofEpochMilli(timeMs).atZone(zone).year
    var customYear by rememberSaveable(body) { mutableStateOf<Int?>(null) }
    val year = customYear ?: locationYear
    var yearValid by remember { mutableStateOf(true) }
    var yearInteracting by remember { mutableStateOf(false) }
    val quickHeader = rememberSpaceCompassQuickReturnHeader(pinned = yearInteracting,
        initialVisible = visit.entry.headerVisible)
    SideEffect { visit.entry.headerVisible = quickHeader.visible }
    DisposableEffect(visit.entry, quickHeader) {
        onDispose { visit.entry.headerVisible = quickHeader.visible }
    }
    val scope = rememberCoroutineScope()
    val referenceTime = remember(timeMs, year, zone) {
        if (year in spaceCompassObservationYears) spaceCompassObservationYearReference(timeMs, year, zone) else timeMs
    }
    val computed = rememberSpaceCompassKeyEvents(SpaceCompassObservationCalendar(body, year, zone),
        latitude, longitude, altitude, remote)
    val result = computed.copy(complete = computed.complete && yearValid)
    val export = rememberSpaceCompassKeyEventsExport(body, referenceTime, result)
    BackHandler { if (export.visible) export.close() else if (!export.saving) onBack() }
    Box(Modifier.fillMaxSize()) {
        Surface(Modifier.fillMaxSize().layout { measurable, constraints ->
            val placeable = measurable.measure(constraints)
            layout(placeable.width, placeable.height) { if (!export.visible) placeable.placeRelative(0, 0) }
        }.testTag("page-key-events"), color = pageBackground, contentColor = foreground) {
            Column(Modifier.fillMaxSize()) {
                SpaceCompassPageToolbar(stringResource(R.string.key_events_title),
                    { if (!export.saving) onBack() }, modifier = Modifier.background(spaceCompassPageBackground())) {
                    SpaceCompassTitleActionButton(stringResource(R.string.panorama_export), export.open,
                        Modifier.testTag("key-events-export"),
                        enabled = result.complete && !result.events.isNullOrEmpty() && !export.saving) {
                        SpaceCompassPanoramaExportIcon(SpaceCompassTitleActionIconStrokeWidth)
                    }
                }
                SpaceCompassQuickReturnHeader(quickHeader, Modifier.testTag("key-events-header")) {
                    Row(Modifier.fillMaxWidth().padding(start = 18.dp, end = 8.dp, top = 8.dp, bottom = 8.dp),
                        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        SpaceCompassScrollingText(stringResource(body.nameResource), Modifier.weight(1f),
                            color = foreground, style = TextStyle(fontSize = 14.sp, lineHeight = 18.sp,
                                fontWeight = FontWeight.SemiBold))
                        SpaceCompassSkyHeightsYearField(year, { selected ->
                            if (selected != year) scope.launch { visit.entry.scrollTo(0) }
                            customYear = selected.takeUnless { value -> value == locationYear }
                        },
                            { yearValid = it }, { yearInteracting = it }, Modifier.width(128.dp))
                    }
                    HorizontalDivider(Modifier.padding(horizontal = 10.dp), thickness = .5.dp,
                        color = secondary.copy(alpha = .25f))
                }
                Column(Modifier.fillMaxWidth().weight(1f).nestedScroll(quickHeader.connection).clipToBounds()
                    .scrollbarOverlay(scroll, secondary.copy(alpha = .46f)).verticalScroll(scroll)
                    .padding(start = 18.dp, end = 8.dp, top = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    SpaceCompassKeyEventsRows(body, result, secondary)
                }
            }
        }
        if (export.visible) SpaceCompassPanoramaExportPage(export)
    }
}
