package me.mondiversi.spacecompass

import java.time.ZoneId
import java.util.Locale
import java.util.TimeZone

/** The displayed interval follows the same language, date, clock and number choices as its rows. */
internal fun formatSpaceCompassPathInterval(startMs: Long,endMs: Long,zone: ZoneId,
    time: SpaceCompassTimeFormat,date: SpaceCompassDateFormat,numeric: SpaceCompassNumericFormat,
    locale: Locale,deviceLocale: Locale): String {
    val timezone=TimeZone.getTimeZone(zone)
    fun moment(stamp: Long): String {
        val day=formatSpaceCompassDateOnly(stamp,date,
            if(date==SpaceCompassDateFormat.SYSTEM) deviceLocale else locale,timezone,numeric,deviceLocale)
        val clock=formatSpaceCompassCelestialMoment(stamp,stamp,zone,time,date,locale,deviceLocale,numeric)
        return "$day · $clock"
    }
    return "${moment(startMs)} → ${moment(endMs)}"
}
