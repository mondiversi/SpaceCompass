package me.mondiversi.spacecompass

import java.time.LocalDate
import kotlin.math.roundToLong

internal enum class SpaceCompassKeyEventKind(val title: Int) {
    POLAR_DAY_START(R.string.key_event_polar_day_start), POLAR_DAY_END(R.string.key_event_polar_day_end),
    POLAR_NIGHT_START(R.string.key_event_polar_night_start), POLAR_NIGHT_END(R.string.key_event_polar_night_end),
    LONGEST_DAY(R.string.key_event_longest_day), SHORTEST_DAY(R.string.key_event_shortest_day),
    EARLIEST_SUNRISE(R.string.key_event_earliest_sunrise), LATEST_SUNRISE(R.string.key_event_latest_sunrise),
    EARLIEST_SUNSET(R.string.key_event_earliest_sunset), LATEST_SUNSET(R.string.key_event_latest_sunset),
    NEW_MOON(R.string.key_event_new_moon), FIRST_QUARTER(R.string.key_event_first_quarter),
    FULL_MOON(R.string.key_event_full_moon), LAST_QUARTER(R.string.key_event_last_quarter),
    PERIGEE(R.string.key_event_perigee), APOGEE(R.string.key_event_apogee),
    OPPOSITION(R.string.key_event_opposition), MORNING_ELONGATION(R.string.key_event_morning_elongation),
    EVENING_ELONGATION(R.string.key_event_evening_elongation), PERIHELION(R.string.key_event_perihelion),
    APHELION(R.string.key_event_aphelion), NEAREST_EARTH(R.string.key_event_nearest_earth),
    FARTHEST_EARTH(R.string.key_event_farthest_earth)
}

/** Instants are universal; elevation is always topocentric at the accepted Location/altitude.
 * Distances for apsides/approaches are centre-to-centre, not GPS altitude or slant range. */
internal data class SpaceCompassKeyEvent(val kind: SpaceCompassKeyEventKind, val timeMs: Long,
    val elevation: Double, val distanceKm: Double? = null, val durationMs: Long? = null,
    val angle: Double? = null) {
    init {
        require(elevation.isFinite() && elevation in -90.0..90.0)
        require(distanceKm == null || distanceKm.isFinite() && distanceKm >= 0)
        require(durationMs == null || durationMs >= 0)
        require(angle == null || angle.isFinite() && angle in 0.0..180.0)
        require(listOfNotNull(distanceKm, durationMs, angle).size <= 1)
    }
}

internal fun spaceCompassKeyEventYearBounds(calendar: SpaceCompassObservationCalendar): LongRange {
    require(calendar.year in spaceCompassObservationYears)
    val start = LocalDate.of(calendar.year, 1, 1).atStartOfDay(calendar.zone).toInstant().toEpochMilli()
    val end = LocalDate.of(calendar.year + 1, 1, 1).atStartOfDay(calendar.zone).toInstant().toEpochMilli()
    return start until end
}

/** Values remain SI until formatting/export. The existing distance-unit preference is shared. */
internal fun spaceCompassKeyEventValue(event: SpaceCompassKeyEvent, body: SpaceCompassCelestialBody,
    profile: SpaceCompassDataExportProfile): String {
    val f = profile.formatting
    fun number(value: Long) = formatSpaceCompassNumber(value.toDouble(), 0, f.numeric,
        grouping = false, systemLocale = f.deviceLocale)
    event.durationMs?.let {
        val minutes = (it / 60_000.0).roundToLong()
        return buildList {
            if (minutes >= 1440) add("${number(minutes / 1440)} d")
            if (minutes / 60 % 24 > 0) add("${number(minutes / 60 % 24)} h")
            if (minutes % 60 > 0 || isEmpty()) add("${number(minutes % 60)} min")
        }.joinToString(" ")
    }
    event.distanceKm?.let {
        return formatSpaceCompassSelectedDistance(body, it, f.numeric, profile.units.distance, profile.units.feet) ?: "—"
    }
    return event.angle?.let { formatSpaceCompassPanoramaElevation(it, f) + "°" }.orEmpty()
}
