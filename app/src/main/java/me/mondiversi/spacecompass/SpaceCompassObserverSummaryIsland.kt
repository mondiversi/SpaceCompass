package me.mondiversi.spacecompass

import android.icu.text.TimeZoneNames
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import kotlinx.coroutines.delay
import java.time.Instant
import java.time.ZoneId
import java.util.TimeZone

/** Read the committed plan; unfinished/invalid typing never appears as an applied scenario. */
@Composable
internal fun SpaceCompassObserverSummaryIsland(plan: SpaceCompassObserverPlan?) {
    val numeric = LocalSpaceCompassNumericFormat.current
    val units = LocalSpaceCompassUnits.current
    val locale = LocalConfiguration.current.locales[0]
    val deviceLocale = LocalSpaceCompassDeviceLocale.current
    val dateFormat = LocalSpaceCompassDateFormat.current
    val timeFormat = resolveSpaceCompassTimeFormat(LocalContext.current, LocalSpaceCompassTimeFormat.current)
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    val screensaver = LocalSpaceCompassScreensaverActive.current
    val positionPlan = plan?.takeIf { it.simulatePosition }
    val place = rememberSpaceCompassEstimatedPlace(positionPlan?.latitude, positionPlan?.longitude,
        enabled = positionPlan != null && !screensaver)
    val now by produceState(System.currentTimeMillis(), plan, lifecycle, screensaver) {
        lifecycle.repeatOnLifecycle(Lifecycle.State.RESUMED) {
            value = System.currentTimeMillis()
            if (!screensaver && plan != null) while (true) {
                delay(60_000L - System.currentTimeMillis() % 60_000L)
                value = System.currentTimeMillis()
            }
        }
    }
    SpaceCompassSettingsIsland(stringResource(R.string.observer_current_scenario), "observer") {
        if (plan == null) {
            SpaceCompassSettingsDescription(stringResource(R.string.scene_scenario_off))
        } else {
            val instant = plan.observationTimeMs(now, ZoneId.systemDefault())
            val zone = plan.observationZone(ZoneId.systemDefault())
            val timezone = TimeZone.getTimeZone(zone)
            val names = remember(locale) { TimeZoneNames.getInstance(locale) }
            Column(Modifier.fillMaxWidth().testTag("observer-summary"),
                verticalArrangement = Arrangement.spacedBy(8.dp)) {
                plan.activeSections.forEach { section ->
                    val label = when (section) {
                        SpaceCompassObserverSection.POSITION -> R.string.observer_place
                        SpaceCompassObserverSection.ALTITUDE -> R.string.observer_altitude
                        SpaceCompassObserverSection.ZONE -> R.string.observer_zone
                        SpaceCompassObserverSection.DATE -> R.string.observer_date
                        SpaceCompassObserverSection.TIME -> R.string.observer_time
                    }
                    val value = when (section) {
                        SpaceCompassObserverSection.POSITION -> formatSpaceCompassSelectedCoordinates(
                            plan.latitude, plan.longitude, numeric, units.dms, deviceLocale) ?: "—"
                        SpaceCompassObserverSection.ALTITUDE -> formatSpaceCompassNumber(
                            if (units.feet) plan.altitudeMeters / .3048 else plan.altitudeMeters, 2, numeric,
                            grouping = false, systemLocale = deviceLocale) + if (units.feet) " ft" else " m"
                        SpaceCompassObserverSection.ZONE -> (names.getExemplarLocationName(zone.id)
                            ?: zone.id.substringAfterLast('/').replace('_', ' ')) + " · " +
                            formatSpaceCompassObserverUtcOffset(zone.rules.getOffset(Instant.ofEpochMilli(instant)))
                        SpaceCompassObserverSection.DATE -> formatSpaceCompassDateOnly(instant, dateFormat,
                            locale, timezone, numeric, deviceLocale, allowHistorical = true)
                        SpaceCompassObserverSection.TIME -> formatSpaceCompassTimeOnly(instant, timeFormat,
                            locale, timezone, numeric, deviceLocale, includeSeconds = false, allowHistorical = true) +
                            if (plan.advanceTime) "\n" + stringResource(R.string.observer_advance_time) else ""
                    }
                    Row(Modifier.fillMaxWidth().testTag("observer-summary-${section.name.lowercase(java.util.Locale.ROOT)}"),
                        verticalAlignment = Alignment.Top, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text(stringResource(label), Modifier.weight(.35f), fontSize = 13.sp, lineHeight = 17.sp,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = .72f))
                        Column(Modifier.weight(.65f), horizontalAlignment = Alignment.End,
                            verticalArrangement = Arrangement.spacedBy(3.dp)) {
                            Text(value, Modifier.fillMaxWidth(), fontSize = 13.sp, lineHeight = 17.sp,
                                textAlign = TextAlign.End, style = TextStyle(textDirection = TextDirection.ContentOrLtr))
                            if (section == SpaceCompassObserverSection.POSITION) {
                                Text(place.text ?: stringResource(if (place.loading) R.string.environment_place_loading
                                    else R.string.environment_place_unavailable),
                                    Modifier.fillMaxWidth().testTag("observer-summary-place"),
                                    fontSize = 12.sp, lineHeight = 16.sp, textAlign = TextAlign.End,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = .72f))
                            }
                        }
                    }
                }
            }
        }
    }
}
