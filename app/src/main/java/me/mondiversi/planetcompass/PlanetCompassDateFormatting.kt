package me.mondiversi.planetcompass

import android.content.Context
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.core.content.edit
import java.text.DateFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

internal const val PLANET_COMPASS_DATE_FORMAT_KEY = "date_format"
internal const val PLANET_COMPASS_TIME_FORMAT_KEY = "time_format"

enum class PlanetCompassDateFormat(
    val storedValue: String
) {
    SYSTEM("system"),
    INTERNATIONAL("international"),
    EUROPEAN("european"),
    AMERICAN("american");

    companion object {
        fun fromStoredValue(value: String?): PlanetCompassDateFormat =
            entries.firstOrNull {
                it.storedValue == value
            } ?: SYSTEM
    }
}

enum class PlanetCompassTimeFormat(
    val storedValue: String
) {
    SYSTEM("system"),
    H24("24_hour"),
    H12("12_hour");

    companion object {
        fun fromStoredValue(value: String?): PlanetCompassTimeFormat =
            entries.firstOrNull {
                it.storedValue == value
            } ?: SYSTEM
    }
}

internal val LocalPlanetCompassDateFormat =
    staticCompositionLocalOf {
        PlanetCompassDateFormat.SYSTEM
    }

internal val LocalPlanetCompassTimeFormat =
    staticCompositionLocalOf {
        PlanetCompassTimeFormat.H24
    }

internal fun loadPlanetCompassDateFormat(
    context: Context
): PlanetCompassDateFormat =
    PlanetCompassDateFormat.fromStoredValue(
        context.getSharedPreferences(
            PLANET_COMPASS_PREFERENCES_NAME,
            Context.MODE_PRIVATE
        ).getString(
            PLANET_COMPASS_DATE_FORMAT_KEY,
            PlanetCompassDateFormat.SYSTEM.storedValue
        )
    )

internal fun savePlanetCompassDateFormat(
    context: Context,
    format: PlanetCompassDateFormat
) {
    context.getSharedPreferences(
        PLANET_COMPASS_PREFERENCES_NAME,
        Context.MODE_PRIVATE
    ).edit {
        putString(
            PLANET_COMPASS_DATE_FORMAT_KEY,
            format.storedValue
        )
    }
}

internal fun loadPlanetCompassTimeFormat(
    context: Context
): PlanetCompassTimeFormat =
    PlanetCompassTimeFormat.fromStoredValue(
        context.getSharedPreferences(
            PLANET_COMPASS_PREFERENCES_NAME,
            Context.MODE_PRIVATE
        ).getString(
            PLANET_COMPASS_TIME_FORMAT_KEY,
            PlanetCompassTimeFormat.SYSTEM.storedValue
        )
    )

internal fun savePlanetCompassTimeFormat(
    context: Context,
    format: PlanetCompassTimeFormat
) {
    context.getSharedPreferences(
        PLANET_COMPASS_PREFERENCES_NAME,
        Context.MODE_PRIVATE
    ).edit {
        putString(
            PLANET_COMPASS_TIME_FORMAT_KEY,
            format.storedValue
        )
    }
}

internal fun resolvePlanetCompassTimeFormat(
    context: Context,
    format: PlanetCompassTimeFormat
): PlanetCompassTimeFormat =
    if (format == PlanetCompassTimeFormat.SYSTEM) {
        if (android.text.format.DateFormat.is24HourFormat(context)) {
            PlanetCompassTimeFormat.H24
        } else {
            PlanetCompassTimeFormat.H12
        }
    } else {
        format
    }

private fun planetCompassDateFormatter(
    format: PlanetCompassDateFormat,
    locale: Locale,
    timeZone: TimeZone
): DateFormat =
    when (format) {
        PlanetCompassDateFormat.SYSTEM ->
            DateFormat.getDateInstance(
                DateFormat.SHORT,
                locale
            )

        PlanetCompassDateFormat.INTERNATIONAL ->
            SimpleDateFormat(
                "yyyy-MM-dd",
                Locale.US
            )

        PlanetCompassDateFormat.EUROPEAN ->
            SimpleDateFormat(
                "dd/MM/yyyy",
                Locale.US
            )

        PlanetCompassDateFormat.AMERICAN ->
            SimpleDateFormat(
                "MM/dd/yyyy",
                Locale.US
            )
    }.apply {
        this.timeZone = timeZone
    }

internal fun formatPlanetCompassDateOnly(
    timestamp: Long,
    format: PlanetCompassDateFormat,
    locale: Locale = Locale.getDefault(),
    timeZone: TimeZone = TimeZone.getDefault()
): String =
    if (timestamp <= 0L) {
        "—"
    } else {
        planetCompassDateFormatter(
            format,
            locale,
            timeZone
        ).format(Date(timestamp))
    }

internal fun formatPlanetCompassTimeOnly(
    timestamp: Long,
    format: PlanetCompassTimeFormat,
    locale: Locale = Locale.getDefault(),
    timeZone: TimeZone = TimeZone.getDefault()
): String =
    if (timestamp <= 0L) {
        "—"
    } else when (format) {
        PlanetCompassTimeFormat.SYSTEM ->
            DateFormat.getTimeInstance(
                DateFormat.MEDIUM,
                locale
            )

        PlanetCompassTimeFormat.H24 ->
            SimpleDateFormat(
                "HH:mm:ss",
                locale
            )

        PlanetCompassTimeFormat.H12 ->
            SimpleDateFormat(
                "h:mm:ss a",
                locale
            )
    }.apply {
        this.timeZone = timeZone
    }.format(Date(timestamp))

internal fun formatPlanetCompassDateTime(
    timestamp: Long,
    format: PlanetCompassDateFormat,
    locale: Locale = Locale.getDefault(),
    timeZone: TimeZone = TimeZone.getDefault(),
    separator: String = "  ",
    timeFormat: PlanetCompassTimeFormat = PlanetCompassTimeFormat.H24
): String {
    if (timestamp <= 0L) return "—"
    val date =
        formatPlanetCompassDateOnly(
            timestamp,
            format,
            locale,
            timeZone
        )
    val time =
        formatPlanetCompassTimeOnly(
            timestamp = timestamp,
            format = timeFormat,
            locale = locale,
            timeZone = timeZone
        )
    return "$date$separator$time"
}

internal fun formatPlanetCompassInternationalDateTime(
    timestamp: Long
): String =
    formatPlanetCompassDateTime(
        timestamp = timestamp,
        format = PlanetCompassDateFormat.INTERNATIONAL,
        locale = Locale.US,
        separator = " ",
        timeFormat = PlanetCompassTimeFormat.H24
    )

fun formatDateTime(
    timestamp: Long,
    format: PlanetCompassDateFormat = PlanetCompassDateFormat.SYSTEM,
    timeFormat: PlanetCompassTimeFormat = PlanetCompassTimeFormat.H24
): String =
    formatPlanetCompassDateTime(
        timestamp,
        format,
        timeFormat = timeFormat
    )

fun formatDetailDateTime(
    timestamp: Long,
    format: PlanetCompassDateFormat = PlanetCompassDateFormat.SYSTEM,
    timeFormat: PlanetCompassTimeFormat = PlanetCompassTimeFormat.H24
): String =
    formatPlanetCompassDateTime(
        timestamp,
        format,
        timeFormat = timeFormat
    )

internal fun formatSensorListLastActivity(
    timestamp: Long,
    format: PlanetCompassDateFormat = PlanetCompassDateFormat.SYSTEM,
    timeFormat: PlanetCompassTimeFormat = PlanetCompassTimeFormat.H24
): String =
    if (timestamp > 0L) {
        formatDetailDateTime(timestamp, format, timeFormat)
            .replace("  ", "\n")
    } else {
        "—"
    }

fun formatAutomaticSessionDateTime(
    timestamp: Long,
    format: PlanetCompassDateFormat = PlanetCompassDateFormat.SYSTEM,
    timeFormat: PlanetCompassTimeFormat = PlanetCompassTimeFormat.H24
): String =
    formatPlanetCompassDateTime(
        timestamp,
        format,
        timeFormat = timeFormat
    )
