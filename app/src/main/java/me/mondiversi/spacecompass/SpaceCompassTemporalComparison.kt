package me.mondiversi.spacecompass

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.util.UUID

internal enum class SpaceCompassComparisonSource { HEIGHT, EVENT, PERSONAL }

/** A separate scene instance, never a synthetic catalogue object or a running Location clock. */
internal data class SpaceCompassTemporalComparison(val id: String, val body: SpaceCompassCelestialBody,
    val timeMs: Long, val zoneId: String, val source: SpaceCompassComparisonSource, val origin: String,
    val annualExtreme: Boolean = false, val personalName: String? = null, val hasReference: Boolean = true,
    val personalDate: LocalDate? = null) : java.io.Serializable {
    init {
        require(origin.length in 1..160 && origin.none { it.isISOControl() || it == '|' })
        require(id == spaceCompassComparisonId(body, source, origin))
        require(body in spaceCompassAllCelestialOrder)
        require(!annualExtreme || source == SpaceCompassComparisonSource.HEIGHT &&
            origin.substringBefore(':') in setOf("MAXIMUM", "MINIMUM"))
        require((personalName != null && personalDate != null) == (source == SpaceCompassComparisonSource.PERSONAL))
        require(source == SpaceCompassComparisonSource.PERSONAL || personalName == null && personalDate == null && hasReference)
        val resolvedZone = ZoneId.of(zoneId)
        require((personalDate ?: Instant.ofEpochMilli(timeMs).atZone(resolvedZone).toLocalDate()).year in 1900..2100)
    }
    val zone: ZoneId get() = ZoneId.of(zoneId)
    val date: LocalDate get() = personalDate ?: Instant.ofEpochMilli(timeMs).atZone(zone).toLocalDate()
    companion object {
        fun create(body: SpaceCompassCelestialBody, timeMs: Long, zone: ZoneId,
            source: SpaceCompassComparisonSource, origin: String, annualExtreme: Boolean = false,
            personalName: String? = null, hasReference: Boolean = true, personalDate: LocalDate? = null) =
            SpaceCompassTemporalComparison(spaceCompassComparisonId(body, source, origin), body, timeMs, zone.id, source,
                origin, annualExtreme, personalName, hasReference, personalDate)
    }
}

internal fun spaceCompassComparisonId(body: SpaceCompassCelestialBody, source: SpaceCompassComparisonSource, origin: String) =
    UUID.nameUUIDFromBytes("${body.name}:${source.name}:$origin".toByteArray(Charsets.UTF_8)).toString()

/** Stable row provenance survives numeric refinements, language changes and different UTC offsets. */
internal fun spaceCompassComparisonOrigin(kind: String, timeMs: Long, zone: ZoneId): String =
    "$kind:${Instant.ofEpochMilli(timeMs).atZone(zone).toLocalDate()}"
