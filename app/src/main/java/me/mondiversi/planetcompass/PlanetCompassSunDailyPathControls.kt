package me.mondiversi.planetcompass

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.text.SimpleDateFormat
import java.time.Instant
import java.util.Date
import java.util.TimeZone

/** User interaction belongs to the screen, not to a replaceable GPS-derived curve. */
@Stable
internal class PlanetCompassSunDailyPathUiState {
    var menuPath by mutableStateOf<PlanetCompassSunDailyPath?>(null)
        private set
    private var selection by mutableStateOf<Pair<PlanetCompassSunDailyPath, PlanetCompassSunPathPoint>?>(null)
    private var currentSelection by mutableStateOf<Pair<PlanetCompassCelestialBody, PlanetCompassSunPathPoint>?>(null)
    val selectedBody: PlanetCompassCelestialBody? get() = currentSelection?.first ?: selection?.first?.body

    // Freeze the list being consulted: live refreshes must neither dismiss nor move it.
    fun openMenu(path: PlanetCompassSunDailyPath) { menuPath = path }
    fun closeMenu() { menuPath = null }
    fun select(path: PlanetCompassSunDailyPath, point: PlanetCompassSunPathPoint) {
        currentSelection = null
        selection = path to point
    }
    // Snapshot the tapped instant: later live updates must not silently change its time/angles.
    fun selectCurrent(body: PlanetCompassCelestialBody, point: PlanetCompassSunPathPoint) {
        selection = null
        currentSelection = body to point
    }
    fun selectedCurrentFor(body: PlanetCompassCelestialBody): PlanetCompassSunPathPoint? =
        currentSelection?.takeIf { it.first == body }?.second
    fun clearSelection() { selection = null; currentSelection = null }
    fun selectedIn(path: PlanetCompassSunDailyPath): PlanetCompassSunPathPoint? {
        selectedCurrentFor(path.body)?.let { return it }
        val (previousPath, point) = selection ?: return null
        if (previousPath.date != path.date || previousPath.zone != path.zone || previousPath.body != path.body) return null
        return path.markers.firstOrNull { it.timeMs == point.timeMs && it.event == point.event }
    }
}

@Composable
internal fun rememberPlanetCompassSunDailyPathUiState(): PlanetCompassSunDailyPathUiState = remember { PlanetCompassSunDailyPathUiState() }

internal data class PlanetCompassSunPathLabels(
    val date: String,
    val name: (PlanetCompassSunPathPoint) -> String,
    val point: (PlanetCompassSunPathPoint) -> String,
    val current: (PlanetCompassSunPathPoint) -> String,
    val angle: (Double) -> String
)

@Composable
internal fun rememberPlanetCompassSunPathLabels(path: PlanetCompassSunDailyPath): PlanetCompassSunPathLabels {
    val context = LocalContext.current
    val locale = LocalConfiguration.current.locales[0]
    val timeFormat = resolvePlanetCompassTimeFormat(context, LocalPlanetCompassTimeFormat.current)
    val dateFormat = LocalPlanetCompassDateFormat.current
    val numeric = LocalPlanetCompassNumericFormat.current
    val rise = stringResource(R.string.celestial_rise)
    val culmination = stringResource(R.string.sun_path_culmination)
    val set = stringResource(R.string.celestial_set)
    val minimum = stringResource(R.string.sun_path_minimum)
    val current = stringResource(R.string.celestial_current_position)
    val numbered = stringResource(R.string.celestial_point_number)
    val pointTime = stringResource(R.string.celestial_point_time)
    return remember(path, locale, timeFormat, dateFormat, numeric, rise, culmination, set, minimum, current, numbered, pointTime) {
        val names = PlanetCompassSunPathPointNames(path.markers, rise, culmination, set, minimum, current) {
            String.format(locale, numbered, it)
        }
        val zone = TimeZone.getTimeZone(path.zone)
        val firstDate = Instant.ofEpochMilli(path.samples.first().timeMs).atZone(path.zone).toLocalDate()
        val formatter = SimpleDateFormat(if (timeFormat == PlanetCompassTimeFormat.H12) "h:mm a" else "HH:mm", locale)
            .apply { timeZone = zone }
        val moment: (PlanetCompassSunPathPoint) -> String = { point ->
            val local = Instant.ofEpochMilli(point.timeMs).atZone(path.zone)
            val repeated = point.event == PlanetCompassSunPathEvent.HOUR && path.markers.any {
                it.event == PlanetCompassSunPathEvent.HOUR && it.timeMs != point.timeMs &&
                    Instant.ofEpochMilli(it.timeMs).atZone(path.zone).toLocalDateTime() == local.toLocalDateTime()
            }
            formatter.format(Date(point.timeMs)) +
                (if (repeated) " (UTC${local.offset.id})" else "") +
                (if (local.toLocalDate() != firstDate)
                    " · ${formatPlanetCompassDateOnly(point.timeMs, dateFormat, locale, zone)}" else "")
        }
        PlanetCompassSunPathLabels(formatPlanetCompassDateOnly(path.samples.first().timeMs, dateFormat, locale, zone),
            name = names::name,
            point = { String.format(locale, pointTime, names.name(it), moment(it)) },
            current = { String.format(locale, pointTime, names.current, moment(it)) },
            angle = { "${formatPlanetCompassNumber(it, 1, numeric)}°" })
    }
}

/** Keep this host outside conditional viewport/layout branches and asynchronous path keys. */
@Composable
internal fun PlanetCompassSunDailyPathDialog(
    state: PlanetCompassSunDailyPathUiState, primaryText: Color, secondaryText: Color, backgroundColor: Color
) {
    val path = state.menuPath ?: return
    val labels = rememberPlanetCompassSunPathLabels(path)
    PlanetCompassAlertDialog(onDismissRequest = state::closeMenu, confirmButton = null,
        modifier = Modifier.testTag("sun-path-dialog"), containerColor = backgroundColor,
        titleContentColor = primaryText, textContentColor = primaryText,
        title = { Text(stringResource(if (path.body.isEarthSatellite) R.string.celestial_orbit else R.string.celestial_daily_path),
            Modifier.testTag("sun-path-title")) }, text = {
            Column {
                Text("${labels.date} · ${path.zone.id}", color = secondaryText, fontSize = 12.sp)
                Text(stringResource(if (path.body.isEarthSatellite) R.string.celestial_iss_note else R.string.sun_path_note), color = secondaryText, fontSize = 12.sp,
                    modifier = Modifier.padding(bottom = 8.dp))
                if (path.body.isEarthSatellite) {
                    Text(stringResource(R.string.celestial_pass), color = primaryText, fontSize = 12.sp,
                        fontWeight = FontWeight.Bold, modifier = Modifier.testTag("iss-pass-heading"))
                    if (path.issPass.isEmpty()) Text(stringResource(R.string.celestial_iss_no_pass),
                        color = secondaryText, fontSize = 12.sp, modifier = Modifier.testTag("iss-no-pass"))
                    path.issPass.forEach { point ->
                        // Informational rows, not points on the current orbit (the pass may be tomorrow).
                        Row(Modifier.fillMaxWidth().padding(vertical = 3.dp).testTag("iss-pass-${point.event.name}"),
                            verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(labels.point(point), Modifier.weight(1f), color = primaryText, fontSize = 12.sp)
                            Text(labels.angle(point.position.elevationDegrees), color = secondaryText, fontSize = 12.sp)
                        }
                    }
                    HorizontalDivider(Modifier.padding(vertical = 8.dp), color = secondaryText.copy(alpha = 0.25f))
                    Text(stringResource(R.string.celestial_orbit), color = primaryText, fontSize = 12.sp,
                        fontWeight = FontWeight.Bold, modifier = Modifier.padding(bottom = 4.dp))
                }
                path.markers.forEachIndexed { index, point ->
                    Row(Modifier.fillMaxWidth().heightIn(min = 48.dp).clip(RoundedCornerShape(16.dp))
                        .clickable { state.select(path, point); state.closeMenu() }
                        .testTag("sun-path-point-$index").padding(horizontal = 8.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        if (path.body == PlanetCompassCelestialBody.MOON) point.moonPhase?.let { phase ->
                            PlanetCompassMoonPhaseIcon(phase, primaryText, Modifier.testTag("moon-phase-$index"))
                        }
                        Text(labels.point(point), Modifier.weight(1f), color = primaryText,
                            fontWeight = if (point.event == PlanetCompassSunPathEvent.HOUR) FontWeight.Normal else FontWeight.Bold)
                        Text(labels.angle(point.position.elevationDegrees), color = secondaryText)
                    }
                }
            }
        })
}
