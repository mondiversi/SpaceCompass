package me.mondiversi.spacecompass

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import java.time.LocalDateTime
import java.time.ZoneId
import java.util.TimeZone

/** Default previews resolve the phone's settings independently of explicit row selections. */
@Composable
internal fun spaceCompassUnitSettingExamples(): Map<String, Map<String, String>> {
    val context = LocalContext.current
    val deviceLocale = LocalSpaceCompassDeviceLocale.current
    val locale = LocalConfiguration.current.locales[0]
    val numeric = LocalSpaceCompassNumericFormat.current
    val feet = LocalSpaceCompassUnits.current.feet
    val phoneTime = resolveSpaceCompassTimeFormat(context, SpaceCompassTimeFormat.SYSTEM)
    val zone = TimeZone.getDefault()
    return remember(deviceLocale, locale, numeric, feet, phoneTime, zone.id) {
        val defaults = spaceCompassResolveUnits(deviceLocale.country) { null }
        val sample = LocalDateTime.of(2026, 10, 4, 17, 30).atZone(ZoneId.of(zone.id)).toInstant().toEpochMilli()
        fun distances(imperial: Boolean) = if (imperial) "ft, mi, mi/s" else "m, km, km/s"
        fun mass(pounds: Boolean): String {
            val unit = if (pounds) "lb" else "kg"
            return "$unit, $unit/${if (feet) "ft" else "m"}³"
        }
        fun temperature(fahrenheit: Boolean) = if (fahrenheit) "°F" else "°C"
        fun coordinates(dms: Boolean) = formatSpaceCompassSelectedCoordinates(12.3456, 0.0, numeric, dms)!!
            .substringBefore('\n')
        mapOf(
            SPACE_COMPASS_DISTANCE_SPEED_UNIT_KEY to mapOf("system" to distances(defaults.miles),
                "metric" to distances(false), "imperial" to distances(true)),
            SPACE_COMPASS_MASS_UNIT_KEY to mapOf("system" to mass(defaults.pounds), "kg" to mass(false), "lb" to mass(true)),
            SPACE_COMPASS_PRESSURE_UNIT_KEY to mapOf("system" to defaults.pressure.symbol,
                "bar" to "bar", "pa" to "Pa", "psi" to "psi"),
            "temperature" to mapOf("system" to temperature(defaults.fahrenheit), "c" to "°C", "f" to "°F"),
            "coordinates" to mapOf("system" to coordinates(defaults.dms), "decimal" to coordinates(false), "dms" to coordinates(true)),
            SPACE_COMPASS_NUMERIC_FORMAT_KEY to SpaceCompassNumericFormat.entries.associate {
                it.storedValue to formatSpaceCompassNumber(1000.23, 2, it)
            },
            SPACE_COMPASS_DATE_FORMAT_KEY to SpaceCompassDateFormat.entries.associate {
                it.storedValue to formatSpaceCompassDateOnly(sample, it,
                    if (it == SpaceCompassDateFormat.SYSTEM) deviceLocale else locale, zone, numeric, deviceLocale)
            },
            SPACE_COMPASS_TIME_FORMAT_KEY to SpaceCompassTimeFormat.entries.associate {
                it.storedValue to formatSpaceCompassTimeOnly(sample,
                    if (it == SpaceCompassTimeFormat.SYSTEM) phoneTime else it,
                    locale, zone, numeric, deviceLocale, includeSeconds = false)
            }
        )
    }
}
