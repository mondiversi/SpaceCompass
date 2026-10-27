package me.mondiversi.spacecompass

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import java.time.Instant
import java.time.ZoneId
import java.util.TimeZone
import kotlin.math.abs

/** Uses the already cached administrative label; opening Export never starts another geocoder request. */
@Composable
internal fun spaceCompassScenarioNameSuggestion(): (SpaceCompassScenarioArchive) -> String {
    val context = LocalContext.current
    val locale = LocalConfiguration.current.locales[0]
    val deviceLocale = LocalSpaceCompassDeviceLocale.current
    val numeric = LocalSpaceCompassNumericFormat.current
    val units = LocalSpaceCompassUnits.current
    val dateFormat = LocalSpaceCompassDateFormat.current
    val dateLocale = if (dateFormat == SpaceCompassDateFormat.SYSTEM) deviceLocale else locale
    val timeFormat = resolveSpaceCompassTimeFormat(context, LocalSpaceCompassTimeFormat.current)
    val fallback = stringResource(R.string.observer_current_scenario)
    val repository = spaceCompassPlaceRepository(context)
    return { archive ->
        val c = archive.configuration
        fun number(value: Double, decimals: Int) = formatSpaceCompassNumber(value, decimals, numeric,
            grouping = false, minimumDigits = 0, systemLocale = deviceLocale)
        val place = if (c.simulatePosition) spaceCompassPlaceKey(c.latitude, c.longitude, locale.toLanguageTag())
            ?.let(repository::cached) else null
        val coordinates = "${number(abs(c.latitude), 2)}°${if (c.latitude < 0) "S" else "N"} " +
            "${number(abs(c.longitude), 2)}°${if (c.longitude < 0) "W" else "E"}"
        val height = number(if (units.feet) c.altitudeMeters / .3048 else c.altitudeMeters, 1) +
            if (units.feet) " ft" else " m"
        val zoneMoment = if (c.simulateDate || c.simulateTime) c.timeMs else System.currentTimeMillis()
        val offset = formatSpaceCompassObserverUtcOffset(ZoneId.of(c.zoneId).rules.getOffset(Instant.ofEpochMilli(zoneMoment)))
            .removeSuffix(":00").replace(':', '.')
        val timezone = TimeZone.getTimeZone(c.momentZoneId)
        val date = formatSpaceCompassDateOnly(c.timeMs, dateFormat, dateLocale, timezone, numeric, deviceLocale, allowHistorical = true)
            .replace('/', '-')
        val time = formatSpaceCompassTimeOnly(c.timeMs, timeFormat, locale, timezone, numeric, deviceLocale,
            includeSeconds = false, allowHistorical = true).replace(':', '.')
        val speed = number(archive.timeSettings.rate.toDouble(), 0) + "×"
        spaceCompassScenarioSuggestedName(archive,
            SpaceCompassScenarioNameParts(place?.substringBefore(',') ?: coordinates, height, offset, date, time, speed), fallback)
    }
}
