package me.mondiversi.spacecompass

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Surface
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.util.TimeZone

/** User interaction belongs to the screen, not to a replaceable GPS-derived curve. */
@Stable
internal class SpaceCompassSunDailyPathUiState {
    var menuPath by mutableStateOf<SpaceCompassSunDailyPath?>(null)
        private set
    var menuCurrentPoint by mutableStateOf<SpaceCompassSunPathPoint?>(null)
        private set
    private var selection by mutableStateOf<Pair<SpaceCompassSunDailyPath, SpaceCompassSunPathPoint>?>(null)
    private var currentSelection by mutableStateOf<Pair<SpaceCompassCelestialBody, SpaceCompassSunPathPoint>?>(null)
    val selectedBody: SpaceCompassCelestialBody? get() = currentSelection?.first ?: selection?.first?.body

    // Freeze the list being consulted: live refreshes must neither dismiss nor move it.
    fun openMenu(path: SpaceCompassSunDailyPath, currentPoint: SpaceCompassSunPathPoint? = null) {
        menuPath = path
        menuCurrentPoint = currentPoint
    }
    fun closeMenu() { menuPath = null; menuCurrentPoint = null }
    fun select(path: SpaceCompassSunDailyPath, point: SpaceCompassSunPathPoint) {
        currentSelection = null
        selection = path to point
    }
    // Snapshot the tapped instant: later live updates must not silently change its time/angles.
    fun selectCurrent(body: SpaceCompassCelestialBody, point: SpaceCompassSunPathPoint) {
        selection = null
        currentSelection = body to point
    }
    fun selectedCurrentFor(body: SpaceCompassCelestialBody): SpaceCompassSunPathPoint? =
        currentSelection?.takeIf { it.first == body }?.second
    fun clearSelection() { selection = null; currentSelection = null }
    fun selectedIn(path: SpaceCompassSunDailyPath): SpaceCompassSunPathPoint? {
        selectedCurrentFor(path.body)?.let { return it }
        val (previousPath, point) = selection ?: return null
        if (previousPath.date != path.date || previousPath.zone != path.zone || previousPath.body != path.body) return null
        return path.markers.firstOrNull { it.timeMs == point.timeMs && it.event == point.event }
    }
}

@Composable
internal fun rememberSpaceCompassSunDailyPathUiState(): SpaceCompassSunDailyPathUiState = remember { SpaceCompassSunDailyPathUiState() }

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
    return remember(path, locale, deviceLocale, dateLocale, timeFormat, dateFormat, numeric, rise, culmination, set, minimum, current, numbered, pointTime) {
        val names = SpaceCompassSunPathPointNames(path.markers, rise, culmination, set, minimum, current) {
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
    nowMs: Long = System.currentTimeMillis()
) {
    val path = state.menuPath ?: return
    val labels = rememberSpaceCompassSunPathLabels(path)
    val currentName = stringResource(R.string.celestial_current_position)
    val eventBackground = spaceCompassSettingsCardColor()
    val countdownLocale = if (LocalSpaceCompassNumericFormat.current == SpaceCompassNumericFormat.SYSTEM)
        LocalSpaceCompassDeviceLocale.current else java.util.Locale.ROOT
    val entries = remember(path, state.menuCurrentPoint) { spaceCompassDailyPathListEntries(path, state.menuCurrentPoint) }
    val azimuthLabel = spaceCompassSunDataRow(stringResource(R.string.celestial_point_azimuth,
        SPACE_COMPASS_SUN_DATA_MARKER), "", "").label
    val elevationLabel = spaceCompassSunDataRow(stringResource(R.string.celestial_point_elevation,
        SPACE_COMPASS_SUN_DATA_MARKER), "", "").label
    val angleHeading: @Composable (Boolean) -> Unit = { showDate ->
        Row(Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically) {
            if (showDate) Text("${labels.date} · ${path.zone.id}", Modifier.weight(1f).padding(end = 8.dp),
                color = secondaryText, fontSize = 10.sp, lineHeight = 13.sp,
                textAlign = androidx.compose.ui.text.style.TextAlign.Start)
            else Spacer(Modifier.weight(1f))
            Text(azimuthLabel, Modifier.width(64.dp), color = secondaryText, fontSize = 12.sp,
                textAlign = androidx.compose.ui.text.style.TextAlign.End)
            Spacer(Modifier.width(12.dp))
            Text(elevationLabel, Modifier.width(64.dp), color = secondaryText, fontSize = 12.sp,
                textAlign = androidx.compose.ui.text.style.TextAlign.End)
        }
    }
    val angleValues: @Composable (SpaceCompassSunPathPoint) -> Unit = { point ->
        Row(Modifier.clearAndSetSemantics {
            contentDescription = "$azimuthLabel: ${labels.angle(point.position.azimuthDegrees)}, $elevationLabel: ${labels.angle(point.position.elevationDegrees)}"
        }, verticalAlignment = Alignment.CenterVertically) {
            Text(labels.angle(point.position.azimuthDegrees), Modifier.width(64.dp),
                color = secondaryText, fontSize = 12.sp, style = TextStyle(textDirection = TextDirection.Ltr),
                textAlign = androidx.compose.ui.text.style.TextAlign.End)
            Spacer(Modifier.width(12.dp))
            Text(labels.angle(point.position.elevationDegrees), Modifier.width(64.dp),
                color = secondaryText, fontSize = 12.sp, style = TextStyle(textDirection = TextDirection.Ltr),
                textAlign = androidx.compose.ui.text.style.TextAlign.End)
        }
    }
    BackHandler(onBack = state::closeMenu)
    Surface(color = backgroundColor, contentColor = primaryText,
        modifier = Modifier.fillMaxSize().testTag("sun-path-page")) {
        Column(Modifier.fillMaxSize()) {
            SpaceCompassPageToolbar(stringResource(if (path.body.isEarthSatellite)
                R.string.celestial_orbit else R.string.celestial_daily_path), state::closeMenu,
                titleModifier = Modifier.testTag("sun-path-title"), titleColor = primaryText)
            Column(Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState()).padding(spaceCompassPageContentPadding)) {
                if (path.body.isEarthSatellite) {
                    Text(stringResource(R.string.celestial_iss_note), color = secondaryText, fontSize = 12.sp,
                        modifier = Modifier.padding(bottom = 8.dp))
                }
                if (path.body.isEarthSatellite) {
                    Text(stringResource(R.string.celestial_pass), color = primaryText, fontSize = 12.sp,
                        fontWeight = FontWeight.Bold, modifier = Modifier.testTag("iss-pass-heading"))
                    if (path.issPass.isEmpty()) Text(stringResource(R.string.celestial_iss_no_pass),
                        color = secondaryText, fontSize = 12.sp, modifier = Modifier.testTag("iss-no-pass"))
                    angleHeading(true)
                    path.issPass.forEach { point ->
                        // Informational rows, not points on the current orbit (the pass may be tomorrow).
                        Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp))
                            .background(if (point.events.any { it != SpaceCompassSunPathEvent.HOUR }) eventBackground else Color.Transparent)
                            .padding(horizontal = 8.dp, vertical = 6.dp).testTag("iss-pass-${point.event.name}"),
                            verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            SpaceCompassDailyPathPointCaption(labels.name(point), labels.moment(point),
                                formatSpaceCompassPointCountdown(point.timeMs, nowMs, countdownLocale),
                                primaryText, secondaryText, true, Modifier.weight(1f))
                            angleValues(point)
                        }
                    }
                    HorizontalDivider(Modifier.padding(vertical = 8.dp), color = secondaryText.copy(alpha = 0.25f))
                    Text(stringResource(R.string.celestial_orbit), color = primaryText, fontSize = 12.sp,
                        fontWeight = FontWeight.Bold, modifier = Modifier.padding(bottom = 4.dp))
                }
                angleHeading(!path.body.isEarthSatellite)
                entries.forEach { entry ->
                    val point = entry.point
                    val pointId = entry.markerIndex?.toString() ?: "current"
                    Row(Modifier.fillMaxWidth().heightIn(min = 48.dp).clip(RoundedCornerShape(16.dp))
                        .background(if (entry.isCurrent || point.events.any { it != SpaceCompassSunPathEvent.HOUR })
                            eventBackground else Color.Transparent)
                        .clickable {
                            if (entry.isCurrent) state.selectCurrent(path.body, point) else state.select(path, point)
                            state.closeMenu()
                        }
                        .testTag(if (entry.isCurrent) "sun-path-current" else "sun-path-point-$pointId")
                        .padding(horizontal = 8.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        if (path.body == SpaceCompassCelestialBody.MOON) point.moonPhase?.let { phase ->
                            SpaceCompassMoonPhaseIcon(phase, primaryText, Modifier.testTag("moon-phase-$pointId"))
                        }
                        SpaceCompassDailyPathPointCaption(if (entry.isCurrent) currentName else labels.name(point),
                            labels.moment(point), formatSpaceCompassPointCountdown(point.timeMs, nowMs, countdownLocale),
                            primaryText, secondaryText, entry.isCurrent || point.event != SpaceCompassSunPathEvent.HOUR,
                            Modifier.weight(1f))
                        angleValues(point)
                    }
                }
            }
        }
    }
}
