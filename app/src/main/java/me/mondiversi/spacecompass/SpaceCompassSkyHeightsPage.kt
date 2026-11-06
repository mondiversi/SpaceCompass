package me.mondiversi.spacecompass

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
import androidx.compose.foundation.background
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.rememberLazyListState
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

/** Independent annual browsing uses the daily-path page chrome, without touching the Location. */
@Composable
internal fun SpaceCompassSkyHeightsPage(body: SpaceCompassCelestialBody, timeMs: Long,
    latitude: Double?, longitude: Double?, altitude: Double, remote: SpaceCompassCelestialRemoteData,
    defaultYearResult: SpaceCompassObservationMomentsUiState, onBack: () -> Unit) {
    val foreground = MaterialTheme.colorScheme.onSurface
    val secondary = foreground.copy(alpha = .72f)
    val pageBackground = spaceCompassSurfacePalette(spaceCompassPageBackground().luminance() < .4f).island
    val entry = rememberSpaceCompassObjectPageVisit(body, SpaceCompassObjectPage.SKY_HEIGHTS).entry
    val loadingScroll = rememberLazyListState()
    val zone = spaceCompassObservationZone()
    val locationYear = Instant.ofEpochMilli(timeMs).atZone(zone).year
    var customYear by rememberSaveable(body) { mutableStateOf<Int?>(null) }
    val year = customYear ?: locationYear
    var yearValid by remember { mutableStateOf(true) }
    var yearInteracting by remember { mutableStateOf(false) }
    val quickHeader = rememberSpaceCompassQuickReturnHeader(pinned = yearInteracting,
        initialVisible = entry.headerVisible)
    SideEffect { entry.headerVisible = quickHeader.visible }
    DisposableEffect(entry, quickHeader) {
        onDispose { entry.headerVisible = quickHeader.visible }
    }
    val referenceTime = remember(timeMs, year, zone) {
        if (year in spaceCompassObservationYears) spaceCompassObservationYearReference(timeMs, year, zone) else timeMs
    }
    val computed = if (year == locationYear) defaultYearResult else
        rememberSpaceCompassObservationMoments(SpaceCompassObservationCalendar(body, year, zone),
            referenceTime, latitude, longitude, altitude, remote)
    val result = computed.copy(complete = computed.complete && yearValid)
    // Placeholder rows must not clamp the saved position of the loaded list.
    val scroll = if (result.moments.isNullOrEmpty()) loadingScroll else entry.lazyScroll
    val export = rememberSpaceCompassSkyHeightsExport(body, referenceTime, result)
    val comparisons = LocalSpaceCompassComparisons.current
    val choices = remember(body, result, zone) { spaceCompassHeightComparisonChoices(body, result, zone) }
    BackHandler { if (export.visible) export.close() else if (!export.saving) onBack() }
    Box(Modifier.fillMaxSize()) {
        Surface(Modifier.fillMaxSize().layout { measurable, constraints ->
            val placeable = measurable.measure(constraints)
            layout(placeable.width, placeable.height) { if (!export.visible) placeable.placeRelative(0, 0) }
        }.testTag("page-sky-heights"), color = pageBackground, contentColor = foreground) {
            Column(Modifier.fillMaxSize()) {
                val totalRows = remember(result) { result.totalRows }
                SpaceCompassPageToolbar(spaceCompassObservationCountLabel(stringResource(R.string.observation_heights_title), totalRows, countFirst = true),
                    { if (!export.saving) onBack() }, modifier = Modifier.background(spaceCompassPageBackground()),
                    titleModifier = Modifier.testTag("sky-heights-toggle-all-title").spaceCompassTitleClick(
                        enabled = !export.saving && comparisons?.canToggleAll(choices) == true) { comparisons?.toggleAll(choices) }) {
                    SpaceCompassTitleActionButton(stringResource(R.string.panorama_export), export.open,
                        Modifier.testTag("sky-heights-export"),
                        enabled = result.complete && !result.moments.isNullOrEmpty() && !export.saving) {
                        SpaceCompassPanoramaExportIcon(SpaceCompassTitleActionIconStrokeWidth)
                    }
                }
                SpaceCompassQuickReturnHeader(quickHeader, Modifier.testTag("sky-heights-header")) {
                    Row(Modifier.fillMaxWidth().padding(SpaceCompassObservationHeaderPadding),
                        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        SpaceCompassScrollingText(stringResource(body.nameResource), Modifier.weight(1f),
                            color = foreground, style = TextStyle(fontSize = 14.sp, lineHeight = 18.sp,
                                fontWeight = FontWeight.SemiBold))
                        SpaceCompassSkyHeightsYearField(year, { selected ->
                            if (selected != year) {
                                entry.lazyScroll.requestScrollToItem(0)
                                loadingScroll.requestScrollToItem(0)
                            }
                            customYear = selected.takeUnless { value -> value == locationYear }
                        },
                            { yearValid = it }, { yearInteracting = it }, Modifier.width(128.dp))
                    }
                    HorizontalDivider(Modifier.padding(horizontal = 10.dp), thickness = .5.dp,
                        color = secondary.copy(alpha = .25f))
                }
                SpaceCompassObservationMomentsRows(body, result, secondary,
                    Modifier.fillMaxWidth().weight(1f).nestedScroll(quickHeader.connection).clipToBounds()
                        .lazyScrollbarOverlay(scroll, secondary.copy(alpha = .46f)), scroll)
            }
        }
        if (export.visible) SpaceCompassPanoramaExportPage(export)
    }
}
