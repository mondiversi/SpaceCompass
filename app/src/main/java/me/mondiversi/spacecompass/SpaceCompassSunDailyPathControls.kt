package me.mondiversi.spacecompass

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Surface
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.zIndex
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.util.TimeZone
import java.time.Instant
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.layout
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.material3.MaterialTheme
import kotlin.math.roundToInt

internal data class SpaceCompassSunPathLabels(
    val date: String,
    val name: (SpaceCompassSunPathPoint) -> String,
    val point: (SpaceCompassSunPathPoint) -> String,
    val current: (SpaceCompassSunPathPoint) -> String,
    val angle: (Double) -> String,
    val moment: (SpaceCompassSunPathPoint) -> String
)

@Composable
internal fun rememberSpaceCompassSunPathLabels(path: SpaceCompassSunDailyPath): SpaceCompassSunPathLabels {
    val context = LocalContext.current
    val resources = LocalResources.current
    val locale = LocalConfiguration.current.locales[0]
    val timeFormat = resolveSpaceCompassTimeFormat(context, LocalSpaceCompassTimeFormat.current)
    val dateFormat = LocalSpaceCompassDateFormat.current
    val deviceLocale = LocalSpaceCompassDeviceLocale.current
    val dateLocale = if (dateFormat == SpaceCompassDateFormat.SYSTEM) deviceLocale else locale
    val numeric = LocalSpaceCompassNumericFormat.current
    val rise = stringResource(R.string.celestial_rise)
    val culmination = stringResource(R.string.sun_path_culmination)
    val set = stringResource(R.string.celestial_set)
    val minimum = stringResource(R.string.sun_path_minimum)
    val current = stringResource(R.string.celestial_current_position)
    val numbered = stringResource(R.string.celestial_point_number)
    val pointTime = stringResource(R.string.celestial_point_time)
    val approach = stringResource(R.string.celestial_approach)
    return remember(path, locale, deviceLocale, dateLocale, timeFormat, dateFormat, numeric, rise, culmination, set, minimum, current, numbered, pointTime, approach, resources) {
        val names = SpaceCompassSunPathPointNames(path.markers, rise, culmination, set, minimum, current,
            hourlyZone = path.zone.takeUnless { path.body.isEarthSatellite }, approach = { point ->
                point.approach?.let { "$approach · ${resources.getString(it.other.nameResource)} · " +
                    formatSpaceCompassNumber(it.separationDegrees, 2, numeric, grouping = false, systemLocale = deviceLocale) + "°" } ?: approach
            }) {
            String.format(locale, numbered,
                formatSpaceCompassNumber(it.toDouble(), 0, numeric, grouping = false))
        }
        val zone = TimeZone.getTimeZone(path.zone)
        val moment: (SpaceCompassSunPathPoint) -> String = { point ->
            formatSpaceCompassCelestialMoment(point.timeMs, path.samples.first().timeMs, path.zone,
                timeFormat, dateFormat, locale, deviceLocale, numeric)
        }
        SpaceCompassSunPathLabels(formatSpaceCompassDateOnly(path.samples.first().timeMs, dateFormat, dateLocale, zone, numeric, deviceLocale),
            name = names::name,
            point = { String.format(locale, pointTime, names.name(it), moment(it)) },
            current = { String.format(locale, pointTime, names.current, moment(it)) },
            angle = { "${formatSpaceCompassNumber(it, 1, numeric)}°" }, moment = moment)
    }
}

/** Keep this host outside conditional viewport/layout branches and asynchronous path keys. */
@Composable
internal fun SpaceCompassSunDailyPathPage(
    state: SpaceCompassSunDailyPathUiState, primaryText: Color, secondaryText: Color, backgroundColor: Color,
    nowMs: Long = System.currentTimeMillis(), location: SpaceCompassExportLocation? = null,
    onBack: () -> Unit = { state.closeMenu() }
) {
    val path = state.menuPath ?: return
    val labels = rememberSpaceCompassSunPathLabels(path)
    val currentName = stringResource(R.string.celestial_current_position)
    val pageBackground = spaceCompassSurfacePalette(backgroundColor.luminance() < .4f).island
    val highlightColor = MaterialTheme.colorScheme.primary
    val openingPointId = remember(path) { state.menuFocusPointId }
    val scroll = rememberScrollState()
    val quickHeader = rememberSpaceCompassQuickReturnHeader()
    var openingPointCentered by remember(path, openingPointId) { mutableStateOf(false) }
    var viewport by remember { mutableStateOf<Rect?>(null) }
    var pointCenter by remember(path) { mutableStateOf<Int?>(null) }
    val viewportHeight = viewport?.height?.roundToInt() ?: 0
    LaunchedEffect(path, openingPointId, viewportHeight, pointCenter, scroll.maxValue) {
        if (!openingPointCentered && openingPointId != null && viewportHeight > 0) pointCenter?.let {
            openingPointCentered = true
            scroll.scrollTo(spaceCompassDailyPathCenterScroll(it, viewportHeight, scroll.maxValue))
        }
    }
    val countdownLocale = if (LocalSpaceCompassNumericFormat.current == SpaceCompassNumericFormat.SYSTEM)
        LocalSpaceCompassDeviceLocale.current else java.util.Locale.ROOT
    val selectedPoints = state.selectedPoints
    val frozenLocation = remember(path) { location }
    val export = rememberSpaceCompassDailyPathExport(path,state.menuCurrentPoint,selectedPoints,nowMs,frozenLocation)
    val realNowMs = rememberSpaceCompassPointReminderNow(active = !export.visible)
    val entries = remember(path, state.menuCurrentPoint, selectedPoints) {
        spaceCompassSelectedDailyPathEntries(path, state.menuCurrentPoint, selectedPoints)
    }
    val azimuthLabel = spaceCompassSunDataRow(stringResource(R.string.celestial_point_azimuth,
        SPACE_COMPASS_SUN_DATA_MARKER), "", "").label
    val elevationLabel = spaceCompassSunDataRow(stringResource(R.string.celestial_point_elevation,
        SPACE_COMPASS_SUN_DATA_MARKER), "", "").label
    val angleValues: @Composable (SpaceCompassSunPathPoint, Color, Boolean) -> Unit = { point, valueColor, emphasized ->
        val weight = if (emphasized) FontWeight.Bold else FontWeight.Normal
        Row(Modifier.clearAndSetSemantics {
            contentDescription = "$azimuthLabel: ${labels.angle(point.position.azimuthDegrees)}, $elevationLabel: ${labels.angle(point.position.elevationDegrees)}"
        }, verticalAlignment = Alignment.CenterVertically) {
            Text(labels.angle(point.position.azimuthDegrees), Modifier.width(50.dp),
                color = valueColor, fontSize = 12.sp, fontWeight = weight, style = TextStyle(textDirection = TextDirection.Ltr),
                textAlign = androidx.compose.ui.text.style.TextAlign.End)
            Spacer(Modifier.width(8.dp))
            Text(labels.angle(point.position.elevationDegrees), Modifier.width(50.dp),
                color = valueColor, fontSize = 12.sp, fontWeight = weight, style = TextStyle(textDirection = TextDirection.Ltr),
                textAlign = androidx.compose.ui.text.style.TextAlign.End)
        }
    }
    BackHandler { if (export.visible) export.close() else if (!export.saving) onBack() }
    Box(Modifier.fillMaxSize()) {
    Surface(color = pageBackground, contentColor = primaryText,
        modifier = Modifier.fillMaxSize().layout { measurable,constraints ->
            val placeable=measurable.measure(constraints)
            layout(placeable.width,placeable.height) { if (!export.visible) placeable.placeRelative(0,0) }
        }.testTag("sun-path-page")) {
        Column(Modifier.fillMaxSize()) {
            SpaceCompassPageToolbar(stringResource(R.string.celestial_daily_path),
                { if (!export.saving) onBack() },
                modifier = Modifier.background(spaceCompassPageBackground()),
                titleModifier = Modifier.testTag("sun-path-title"), titleColor = primaryText) {
                SpaceCompassTitleActionButton(stringResource(R.string.panorama_export),export.open,
                    Modifier.testTag("daily-path-export"),enabled=!export.saving) { SpaceCompassPanoramaExportIcon(2.dp) }
            }
            SpaceCompassQuickReturnHeader(quickHeader, Modifier.testTag("sun-path-quick-return-header")) {
            Row(Modifier.fillMaxWidth().padding(start = 18.dp, end = 8.dp, top = 8.dp, bottom = 8.dp).zIndex(1f),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Column(Modifier.weight(1f)) {
                    SpaceCompassScrollingText(stringResource(path.body.nameResource),
                        Modifier.fillMaxWidth().semantics(mergeDescendants = true) { heading() }
                            .testTag("sun-path-object-name"), color = primaryText,
                        style = TextStyle(fontSize = 14.sp, lineHeight = 18.sp, fontWeight = FontWeight.SemiBold))
                    SpaceCompassScrollingText("${labels.date} · ${path.zone.id}",
                        Modifier.fillMaxWidth().semantics(mergeDescendants = true) { }
                            .testTag("sun-path-date"), color = secondaryText,
                        style = TextStyle(fontSize = 10.sp, lineHeight = 13.sp,
                            textDirection = TextDirection.ContentOrLtr))
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(azimuthLabel, Modifier.width(50.dp).testTag("sun-path-angle-azimuth"),
                        color = secondaryText, fontSize = 12.sp,
                        textAlign = androidx.compose.ui.text.style.TextAlign.End)
                    Spacer(Modifier.width(8.dp))
                    Text(elevationLabel, Modifier.width(50.dp).testTag("sun-path-angle-elevation"),
                        color = secondaryText, fontSize = 12.sp,
                        textAlign = androidx.compose.ui.text.style.TextAlign.End)
                    Spacer(Modifier.width(SpaceCompassTableReminderWidth + 8.dp))
                }
            }
            HorizontalDivider(Modifier.padding(horizontal = 10.dp).testTag("sun-path-header-divider"),
                thickness = 0.5.dp, color = secondaryText.copy(alpha = 0.25f))
            }
            Box(Modifier.weight(1f).fillMaxWidth().nestedScroll(quickHeader.connection).clipToBounds().onGloballyPositioned { viewport = it.boundsInRoot() }
                .testTag("sun-path-list-viewport")) {
            Column(Modifier.fillMaxSize().scrollbarOverlay(scroll, secondaryText.copy(alpha = .46f))
                .verticalScroll(scroll)) {
                if (path.body.isEarthSatellite) {
                    Text(stringResource(R.string.celestial_iss_note), color = secondaryText, fontSize = 12.sp,
                        modifier = Modifier.padding(start = 10.dp, end = 10.dp, bottom = 8.dp))
                }
                if (path.body.isEarthSatellite) {
                    Text(stringResource(R.string.celestial_pass), color = primaryText, fontSize = 12.sp,
                        fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 10.dp).testTag("iss-pass-heading"))
                    if (path.issPass.isEmpty()) Text(stringResource(R.string.celestial_iss_no_pass),
                        color = secondaryText, fontSize = 12.sp, modifier = Modifier.padding(horizontal = 10.dp).testTag("iss-no-pass"))
                    path.issPass.forEach { point ->
                        val emphasized = point.events.any { it != SpaceCompassSunPathEvent.HOUR }
                        // Informational rows, not points on the current orbit (the pass may be tomorrow).
                        Row(Modifier.fillMaxWidth().clipToBounds()
                            .padding(start = 18.dp, end = 8.dp, top = 6.dp, bottom = 6.dp).testTag("iss-pass-${point.event.name}"),
                            verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            SpaceCompassDailyPathPointCaption(labels.name(point), labels.moment(point),
                                formatSpaceCompassPointCountdown(point.timeMs, nowMs, countdownLocale),
                                primaryText, secondaryText, emphasized, Modifier.weight(1f), point = point,
                                trend = spaceCompassElevationTrend(point, path.issPass))
                            angleValues(point, secondaryText, emphasized)
                            SpaceCompassPointReminderButton(path.body, point, nowMs, realNowMs, false,
                                labels.name(point), primaryText, tag = "iss-pass-reminder-${point.event.name}")
                        }
                    }
                    HorizontalDivider(Modifier.padding(horizontal = 10.dp, vertical = 8.dp), color = secondaryText.copy(alpha = 0.25f))
                    Text(stringResource(R.string.celestial_orbit), color = primaryText, fontSize = 12.sp,
                        fontWeight = FontWeight.Bold, modifier = Modifier.padding(start = 10.dp, end = 10.dp, bottom = 4.dp))
                }
                entries.forEach { entry ->
                    val point = entry.point
                    val pointId = entry.pointId
                    val emphasized = entry.isCurrent || point.events.any { it != SpaceCompassSunPathEvent.HOUR }
                    val highlighted = state.isSelected(path, point, entry.isCurrent)
                    val nameColor = if (highlighted) highlightColor else primaryText
                    val valueColor = if (highlighted) highlightColor else secondaryText
                    Row(Modifier.fillMaxWidth().heightIn(min = 48.dp).clipToBounds()
                        // Current is always visually highlighted; point selection remains explicit.
                        .background(if (highlighted || entry.isCurrent) highlightColor.copy(alpha = .16f)
                            else Color.Transparent)
                        .clickable {
                            if (entry.isCurrent) state.toggleCurrent(path.body, point, path)
                            else state.togglePoint(entry.sourcePath ?: path, point)
                        }
                        .then(if (pointId != openingPointId) Modifier else Modifier.onGloballyPositioned { coordinates ->
                            viewport?.let { frame -> pointCenter =
                                (coordinates.positionInRoot().y + coordinates.size.height / 2f - frame.top + scroll.value).roundToInt() }
                        })
                        .semantics { selected = highlighted }
                        .testTag(if (entry.isCurrent) "sun-path-current" else "sun-path-point-$pointId")
                        .padding(start = 18.dp, end = 8.dp, top = 6.dp, bottom = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        if (path.body == SpaceCompassCelestialBody.MOON) point.moonPhase?.let { phase ->
                            SpaceCompassMoonPhaseIcon(phase, primaryText, Modifier.testTag("moon-phase-$pointId"))
                        }
                        val pointLabels = entry.sourcePath?.let { rememberSpaceCompassSunPathLabels(it) } ?: labels
                        SpaceCompassDailyPathPointCaption(if (entry.isCurrent) currentName else pointLabels.name(point),
                            labels.moment(point), formatSpaceCompassPointCountdown(point.timeMs, nowMs, countdownLocale),
                            nameColor, valueColor, emphasized,
                            Modifier.weight(1f), point = point, isCurrent = entry.isCurrent,
                            trend = spaceCompassElevationTrend(point, (entry.sourcePath ?: path).samples,
                                (entry.sourcePath ?: path).markers))
                        angleValues(point, valueColor, emphasized)
                        SpaceCompassPointReminderButton(path.body, point, nowMs, realNowMs, entry.isCurrent,
                            if (entry.isCurrent) currentName else pointLabels.name(point), primaryText,
                            tag = if (entry.isCurrent) "sun-path-reminder-current" else "sun-path-reminder-$pointId")
                    }
                }
            }
            }
        }
    }
    if (export.visible) SpaceCompassPanoramaExportPage(export)
    }
}
