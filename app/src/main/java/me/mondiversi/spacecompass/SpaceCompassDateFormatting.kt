package me.mondiversi.spacecompass

import android.content.Context
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.core.content.edit
import java.text.DateFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

internal const val SPACE_COMPASS_DATE_FORMAT_KEY = "date_format"
internal const val SPACE_COMPASS_TIME_FORMAT_KEY = "time_format"

enum class SpaceCompassDateFormat(
    val storedValue: String
) {
    SYSTEM("system"),
    INTERNATIONAL("international"),
    EUROPEAN("european"),
    AMERICAN("american");

    companion object {
        fun fromStoredValue(value: String?): SpaceCompassDateFormat =
            entries.firstOrNull {
                it.storedValue == value
            } ?: SYSTEM
    }
}

enum class SpaceCompassTimeFormat(
    val storedValue: String
) {
    SYSTEM("system"),
    H24("24_hour"),
    H12("12_hour");

    companion object {
        fun fromStoredValue(value: String?): SpaceCompassTimeFormat =
            entries.firstOrNull {
                it.storedValue == value
            } ?: SYSTEM
    }
}

internal val LocalSpaceCompassDateFormat =
    staticCompositionLocalOf {
        SpaceCompassDateFormat.SYSTEM
    }

internal val LocalSpaceCompassTimeFormat =
    staticCompositionLocalOf {
        SpaceCompassTimeFormat.H24
    }

internal fun loadSpaceCompassDateFormat(
    context: Context
): SpaceCompassDateFormat =
    SpaceCompassDateFormat.fromStoredValue(
        context.getSharedPreferences(
            SPACE_COMPASS_PREFERENCES_NAME,
            Context.MODE_PRIVATE
        ).getString(
            SPACE_COMPASS_DATE_FORMAT_KEY,
            SpaceCompassDateFormat.SYSTEM.storedValue
        )
    )

internal fun saveSpaceCompassDateFormat(
    context: Context,
    format: SpaceCompassDateFormat
) {
    context.getSharedPreferences(
        SPACE_COMPASS_PREFERENCES_NAME,
        Context.MODE_PRIVATE
    ).edit {
        putString(
            SPACE_COMPASS_DATE_FORMAT_KEY,
            format.storedValue
        )
    }
}

internal fun loadSpaceCompassTimeFormat(
    context: Context
): SpaceCompassTimeFormat =
    SpaceCompassTimeFormat.fromStoredValue(
        context.getSharedPreferences(
            SPACE_COMPASS_PREFERENCES_NAME,
            Context.MODE_PRIVATE
        ).getString(
            SPACE_COMPASS_TIME_FORMAT_KEY,
            SpaceCompassTimeFormat.SYSTEM.storedValue
        )
    )

internal fun saveSpaceCompassTimeFormat(
    context: Context,
    format: SpaceCompassTimeFormat
) {
    context.getSharedPreferences(
        SPACE_COMPASS_PREFERENCES_NAME,
        Context.MODE_PRIVATE
    ).edit {
        putString(
            SPACE_COMPASS_TIME_FORMAT_KEY,
            format.storedValue
        )
    }
}

internal fun resolveSpaceCompassTimeFormat(
    context: Context,
    format: SpaceCompassTimeFormat
): SpaceCompassTimeFormat =
    if (format == SpaceCompassTimeFormat.SYSTEM) {
        if (android.text.format.DateFormat.is24HourFormat(context)) {
            SpaceCompassTimeFormat.H24
        } else {
            SpaceCompassTimeFormat.H12
        }
    } else {
        format
    }

private fun spaceCompassDateFormatter(
    format: SpaceCompassDateFormat,
    locale: Locale,
    timeZone: TimeZone
): DateFormat =
    when (format) {
        SpaceCompassDateFormat.SYSTEM ->
            DateFormat.getDateInstance(
                DateFormat.SHORT,
                locale
            )

        SpaceCompassDateFormat.INTERNATIONAL ->
            SimpleDateFormat(
                "yyyy-MM-dd",
                Locale.US
            )

        SpaceCompassDateFormat.EUROPEAN ->
            SimpleDateFormat(
                "dd/MM/yyyy",
                Locale.US
            )

        SpaceCompassDateFormat.AMERICAN ->
            SimpleDateFormat(
                "MM/dd/yyyy",
                Locale.US
            )
    }.apply {
        this.timeZone = timeZone
    }

internal fun formatSpaceCompassDateOnly(
    timestamp: Long,
    format: SpaceCompassDateFormat,
    locale: Locale = Locale.getDefault(),
    timeZone: TimeZone = TimeZone.getDefault()
): String =
    if (timestamp <= 0L) {
        "—"
    } else {
        spaceCompassDateFormatter(
            format,
            locale,
            timeZone
        ).format(Date(timestamp))
    }

internal fun formatSpaceCompassTimeOnly(
    timestamp: Long,
    format: SpaceCompassTimeFormat,
    locale: Locale = Locale.getDefault(),
    timeZone: TimeZone = TimeZone.getDefault()
): String =
    if (timestamp <= 0L) {
        "—"
    } else when (format) {
        SpaceCompassTimeFormat.SYSTEM ->
            DateFormat.getTimeInstance(
                DateFormat.MEDIUM,
                locale
            )

        SpaceCompassTimeFormat.H24 ->
            SimpleDateFormat(
                "HH:mm:ss",
                locale
            )

        SpaceCompassTimeFormat.H12 ->
            SimpleDateFormat(
                "h:mm:ss a",
                locale
            )
    }.apply {
        this.timeZone = timeZone
    }.format(Date(timestamp))

internal fun formatSpaceCompassDateTime(
    timestamp: Long,
    format: SpaceCompassDateFormat,
    locale: Locale = Locale.getDefault(),
    timeZone: TimeZone = TimeZone.getDefault(),
    separator: String = "  ",
    timeFormat: SpaceCompassTimeFormat = SpaceCompassTimeFormat.H24
): String {
    if (timestamp <= 0L) return "—"
    val date =
        formatSpaceCompassDateOnly(
            timestamp,
            format,
            locale,
            timeZone
        )
    val time =
        formatSpaceCompassTimeOnly(
            timestamp = timestamp,
            format = timeFormat,
            locale = locale,
            timeZone = timeZone
        )
    return "$date$separator$time"
}

internal fun formatSpaceCompassInternationalDateTime(
    timestamp: Long
): String =
    formatSpaceCompassDateTime(
        timestamp = timestamp,
        format = SpaceCompassDateFormat.INTERNATIONAL,
        locale = Locale.US,
        separator = " ",
        timeFormat = SpaceCompassTimeFormat.H24
    )

fun formatDateTime(
    timestamp: Long,
    format: SpaceCompassDateFormat = SpaceCompassDateFormat.SYSTEM,
    timeFormat: SpaceCompassTimeFormat = SpaceCompassTimeFormat.H24
): String =
    formatSpaceCompassDateTime(
        timestamp,
        format,
        timeFormat = timeFormat
    )

fun formatDetailDateTime(
    timestamp: Long,
    format: SpaceCompassDateFormat = SpaceCompassDateFormat.SYSTEM,
    timeFormat: SpaceCompassTimeFormat = SpaceCompassTimeFormat.H24
): String =
    formatSpaceCompassDateTime(
        timestamp,
        format,
        timeFormat = timeFormat
    )

internal fun formatSensorListLastActivity(
    timestamp: Long,
    format: SpaceCompassDateFormat = SpaceCompassDateFormat.SYSTEM,
    timeFormat: SpaceCompassTimeFormat = SpaceCompassTimeFormat.H24
): String =
    if (timestamp > 0L) {
        formatDetailDateTime(timestamp, format, timeFormat)
            .replace("  ", "\n")
    } else {
        "—"
    }

fun formatAutomaticSessionDateTime(
    timestamp: Long,
    format: SpaceCompassDateFormat = SpaceCompassDateFormat.SYSTEM,
    timeFormat: SpaceCompassTimeFormat = SpaceCompassTimeFormat.H24
): String =
    formatSpaceCompassDateTime(
        timestamp,
        format,
        timeFormat = timeFormat
    )
