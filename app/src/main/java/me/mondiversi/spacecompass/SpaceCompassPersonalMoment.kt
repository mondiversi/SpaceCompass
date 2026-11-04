package me.mondiversi.spacecompass

import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.util.UUID

internal const val SPACE_COMPASS_PERSONAL_MOMENT_LIMIT = 512
internal const val SPACE_COMPASS_PERSONAL_MOMENT_NAME_LIMIT = 80

/** A civil date and optional local time travel with the moment, independent of the observer's zone. */
internal data class SpaceCompassPersonalMoment(val body: SpaceCompassCelestialBody, val name: String,
    val date: LocalDate, val time: LocalTime?, val enabled: Boolean = false) {
    init {
        require(body in spaceCompassAllCelestialOrder)
        require(name.isNotBlank() && name == name.trim() && name.length <= SPACE_COMPASS_PERSONAL_MOMENT_NAME_LIMIT &&
            name.none(Char::isISOControl))
        require(date.year in 1900..2100)
        require(time == null || time.second == 0 && time.nano == 0)
    }

    private val origin = UUID.nameUUIDFromBytes("$date\u0000$time\u0000$name".toByteArray(Charsets.UTF_8)).toString()
    val id = spaceCompassComparisonId(body, SpaceCompassComparisonSource.PERSONAL, origin)

    fun resolve(zone: ZoneId): SpaceCompassTemporalComparison {
        val local = date.atTime(time ?: LocalTime.NOON)
        val zoned = local.atZone(zone)
        // For a skipped hour, retain the day and let zone rules resolve the wall-clock time.
        return SpaceCompassTemporalComparison.create(body, zoned.toInstant().toEpochMilli(), zone,
            SpaceCompassComparisonSource.PERSONAL, origin, personalName = name, hasReference = time != null,
            personalDate = date)
    }
}

internal val spaceCompassPersonalMomentOrder = compareBy<SpaceCompassPersonalMoment>({ it.date }, { it.time }, { it.id })

/** Imports add missing moments while preserving the recipient’s existing definitions and checkboxes. */
internal fun mergeSpaceCompassPersonalMoments(local: List<SpaceCompassPersonalMoment>,
    imported: List<SpaceCompassPersonalMoment>?): List<SpaceCompassPersonalMoment> =
    (local + imported.orEmpty()).distinctBy { it.id }.also { require(it.size <= SPACE_COMPASS_PERSONAL_MOMENT_LIMIT) }
