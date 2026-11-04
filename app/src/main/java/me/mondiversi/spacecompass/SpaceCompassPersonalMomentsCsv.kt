package me.mondiversi.spacecompass

import java.time.LocalTime
import java.time.ZoneOffset
import java.util.TimeZone

/** Personal moments are civil dates and times. UTC is used here only to format them without a conversion. */
internal fun renderSpaceCompassPersonalMomentsCsv(moments: List<SpaceCompassPersonalMoment>,
    profile: SpaceCompassDataExportProfile): String {
    val strings = profile.strings
    val format = profile.formatting
    val zone = TimeZone.getTimeZone("UTC")
    val dateLocale = if (format.date == SpaceCompassDateFormat.SYSTEM) format.deviceLocale else format.locale
    val decimal = formatSpaceCompassNumber(1.5, 1, format.numeric, grouping = false, systemLocale = format.deviceLocale)
    val separator = if (',' in decimal) ";" else ","
    val header = listOf(R.string.personal_moments_object_column, R.string.celestial_view_name,
        R.string.observer_date, R.string.observer_time, R.string.capture_table_selected).map { strings.getString(it) }
    return buildString {
        fun appendRow(cells: List<String>) {
            append(cells.joinToString(separator, transform = ::spaceCompassOrbitCsvCell)).append("\r\n")
        }
        append('\uFEFF')
        appendRow(header)
        for (moment in moments) {
            val instant = moment.date.atTime(moment.time ?: LocalTime.NOON).toInstant(ZoneOffset.UTC).toEpochMilli()
            val date = formatSpaceCompassDateOnly(instant, format.date, dateLocale, zone,
                format.numeric, format.deviceLocale, allowHistorical = true)
            val time = moment.time?.let {
                formatSpaceCompassTimeOnly(instant, format.time, format.locale, zone,
                    format.numeric, format.deviceLocale, includeSeconds = false, allowHistorical = true)
            }.orEmpty()
            appendRow(listOf(strings.getString(moment.body.nameResource), moment.name, date, time,
                strings.getString(if (moment.enabled) R.string.celestial_view_yes else R.string.celestial_view_no)))
        }
    }
}
