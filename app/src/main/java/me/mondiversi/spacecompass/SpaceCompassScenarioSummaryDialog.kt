package me.mondiversi.spacecompass

import android.icu.text.TimeZoneNames
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.time.Instant
import java.time.ZoneId
import java.util.TimeZone
import kotlin.math.abs

/** Read-only effective observer: real defaults and committed overrides, including that place/time's weather. */
@Composable
internal fun SpaceCompassScenarioSummaryDialog(plan: SpaceCompassObserverPlan?, latitude: Double?, longitude: Double?,
    altitudeMeters: Double?, timeMs: Long, zone: ZoneId, weather: SpaceCompassSunWeatherReading,
    placeReading: SpaceCompassEstimatedPlaceReading = SpaceCompassEstimatedPlaceReading(),
    deviceTimeMs: Long = System.currentTimeMillis(), deviceZone: ZoneId = ZoneId.systemDefault(),
    onCustomize: () -> Unit, onDismiss: () -> Unit) {
    val numeric = LocalSpaceCompassNumericFormat.current
    val units = LocalSpaceCompassUnits.current
    val locale = LocalConfiguration.current.locales[0]
    val deviceLocale = LocalSpaceCompassDeviceLocale.current
    val dateFormat = LocalSpaceCompassDateFormat.current
    val dateLocale = if (dateFormat == SpaceCompassDateFormat.SYSTEM) deviceLocale else locale
    val timeFormat = resolveSpaceCompassTimeFormat(LocalContext.current, LocalSpaceCompassTimeFormat.current)
    val timezone = remember(zone) { TimeZone.getTimeZone(zone) }
    val names = remember(locale) { TimeZoneNames.getInstance(locale) }
    val place = placeReading
    val effects = spaceCompassScenarioSummaryEffects(plan, timeMs, zone, deviceTimeMs, deviceZone)
    val coordinates = formatSpaceCompassSelectedCoordinates(latitude, longitude, numeric, units.dms, deviceLocale) ?: "—"
    // An explicitly configured height remains meaningful even while real GPS is unavailable.
    val effectiveAltitude = when {
        plan?.simulateAltitude == true -> plan.altitudeMeters
        plan?.simulatePosition == true -> plan.automaticAltitudeMeters
        else -> altitudeMeters
    }
    val height = effectiveAltitude?.let {
        formatSpaceCompassNumber(if (units.feet) it / .3048 else it, 2, numeric,
            grouping = false, systemLocale = deviceLocale) + if (units.feet) " ft" else " m"
    } ?: "—"
    val zoneValue = (names.getExemplarLocationName(zone.id)
        ?: zone.id.substringAfterLast('/').replace('_', ' ')) + " · " +
        formatSpaceCompassObserverUtcOffset(zone.rules.getOffset(Instant.ofEpochMilli(timeMs)))
    val snapshot = weather.snapshot?.takeIf { latitude != null && longitude != null && spaceCompassSunWeatherSnapshotUsable(it, timeMs) }
    val weatherLabel = spaceCompassSunDataRow(stringResource(R.string.sun_weather_estimate,
        SPACE_COMPASS_SUN_DATA_MARKER), "", "").label
    // A disabled speed island follows the normal clock, regardless of its remembered slider value.
    val clock = plan?.scenarioClock
    val timeRate = clock?.settings?.multiplier ?: if (plan?.simulateTime == true && !plan.advancesClock) 0 else 1
    val rateValue = (if (timeRate < 0) "−" else "") + formatSpaceCompassNumber(abs(timeRate).toDouble(),
        0, numeric, grouping = false, systemLocale = deviceLocale)
    val rateDirection = stringResource(when {
        timeRate < 0 -> R.string.scenario_time_backward
        timeRate > 0 -> R.string.scenario_time_forward
        else -> R.string.scenario_time_paused
    })
    val speedValue = rateDirection + " · " + stringResource(R.string.scenario_speed_value, rateValue)

    SpaceCompassAlertDialog(onDismissRequest = onDismiss,
        modifier = Modifier.testTag("scenario-summary-dialog"),
        title = { Text(stringResource(R.string.observer_current_scenario)) },
        dismissButton = {
            SpaceCompassDialogActionButton(stringResource(R.string.close), onDismiss,
                Modifier.testTag("scenario-summary-close"))
        },
        confirmButton = {
            SpaceCompassDialogActionButton(stringResource(R.string.observer_customize), onCustomize,
                Modifier.testTag("scenario-summary-customize"))
        },
        confirmButtonFirst = true,
        text = {
            Column(Modifier.fillMaxWidth().testTag("observer-summary"), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                SpaceCompassScenarioSummaryRow(stringResource(R.string.observer_place), coordinates, "position",
                    place.text ?: stringResource(if (place.loading) R.string.environment_place_loading else R.string.environment_place_unavailable),
                    highlighted = effects.position, subtitleReady = place.text != null)
                SpaceCompassScenarioSummaryRow(stringResource(R.string.observer_altitude), height, "altitude",
                    highlighted = effects.altitude, automatic = effects.automaticAltitude)
                SpaceCompassScenarioSummaryRow(stringResource(R.string.observer_zone), zoneValue, "zone",
                    highlighted = effects.zone, automatic = effects.automaticZone)
                SpaceCompassScenarioSummaryRow(stringResource(R.string.observer_date),
                    formatSpaceCompassDateOnly(timeMs, dateFormat, dateLocale, timezone, numeric, deviceLocale, allowHistorical = true), "date",
                    highlighted = effects.date)
                SpaceCompassScenarioSummaryRow(stringResource(R.string.observer_time),
                    formatSpaceCompassTimeOnly(timeMs, timeFormat, locale, timezone, numeric, deviceLocale,
                        includeSeconds = false, allowHistorical = true), "time", highlighted = effects.time)
                SpaceCompassScenarioSummaryRow(stringResource(R.string.scenario_speed), speedValue, "speed",
                    highlighted = effects.speed)
                SpaceCompassScenarioSummaryRow(weatherLabel,
                    stringResource(spaceCompassWeatherValueResource(snapshot, weather.loading)), "weather", highlighted = effects.weather, automatic = true)
            }
        })
}

@Composable
private fun SpaceCompassScenarioSummaryRow(label: String, value: String, key: String, subtitle: String? = null,
    highlighted: Boolean = false, subtitleReady: Boolean = true, automatic: Boolean = false) {
    val neutral = spaceCompassDialogContentColor()
    val foreground = if (highlighted) spaceCompassAttentionColor(isSystemInDarkTheme()) else neutral
    val secondary = neutral.copy(alpha = .72f)
    Row(Modifier.fillMaxWidth().testTag("observer-summary-$key"), verticalAlignment = Alignment.Top,
        horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(Modifier.weight(.40f), verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Box(Modifier.size(20.dp).testTag("observer-summary-$key-icon")) {
                SpaceCompassSettingsGroupIcon(when (key) {
                    "position" -> "coordinates"
                    "zone" -> "time_zone"
                    "date" -> "date_format"
                    "time" -> "time_format"
                    "speed" -> "scenario_speed"
                    else -> key
                }, secondary)
            }
            Text(label, Modifier.weight(1f).testTag("observer-summary-$key-label"), fontSize = 13.sp, lineHeight = 17.sp, color = secondary)
        }
        Column(Modifier.weight(.60f), horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Text(value, Modifier.fillMaxWidth().testTag("observer-summary-$key-value"), fontSize = 13.sp, lineHeight = 17.sp,
                color = foreground, textAlign = TextAlign.End, style = TextStyle(textDirection = TextDirection.ContentOrLtr))
            if (automatic) Text(stringResource(R.string.scenario_summary_automatic),
                Modifier.testTag("observer-summary-$key-automatic"), fontSize = 12.sp, lineHeight = 16.sp, color = secondary)
            if (subtitle != null) SpaceCompassScenarioPlaceName(subtitle, subtitleReady, if (highlighted) foreground else secondary)
        }
    }
}

/** Keep a late first lookup from changing the dialog height. Cached names reserve only their actual lines. */
@Composable
private fun SpaceCompassScenarioPlaceName(text: String, ready: Boolean, color: androidx.compose.ui.graphics.Color) {
    BoxWithConstraints(Modifier.fillMaxWidth()) {
        val firstName = remember { text.takeIf { ready } }
        val density = LocalDensity.current
        val measurer = rememberTextMeasurer()
        val style = TextStyle(fontSize = 12.sp, lineHeight = 16.sp)
        val lines = remember(firstName, maxWidth, density) {
            firstName?.let { measurer.measure(it, style,
                constraints = Constraints(maxWidth = with(density) { maxWidth.roundToPx() })).lineCount.coerceIn(1, 3) } ?: 3
        }
        Text(text, Modifier.fillMaxWidth().testTag("observer-summary-place"), style = style,
            textAlign = TextAlign.End, color = color, minLines = lines, maxLines = lines, overflow = TextOverflow.Ellipsis)
    }
}
