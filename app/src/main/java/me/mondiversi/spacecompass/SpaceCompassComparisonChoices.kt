package me.mondiversi.spacecompass

import java.time.Instant
import java.time.ZoneId

/** The exact enabled rows form the title shortcut's scope, never other objects, pages or years. */
internal fun spaceCompassHeightComparisonChoices(body: SpaceCompassCelestialBody,
    result: SpaceCompassObservationMomentsUiState, zone: ZoneId): List<SpaceCompassTemporalComparison> =
    if (!result.complete) emptyList() else spaceCompassObservationMomentRows(result.moments.orEmpty()).mapNotNull { row ->
        row.point?.takeIf { Instant.ofEpochMilli(it.timeMs).atZone(zone).year in 1900..2100 }?.let {
            SpaceCompassTemporalComparison.create(body, it.timeMs, zone, SpaceCompassComparisonSource.HEIGHT,
                spaceCompassComparisonOrigin(row.kind.name, it.timeMs, zone), row.annualExtreme)
        }
    }

internal fun spaceCompassEventComparisonChoices(body: SpaceCompassCelestialBody,
    result: SpaceCompassKeyEventsUiState, zone: ZoneId): List<SpaceCompassTemporalComparison> =
    if (!result.complete) emptyList() else result.events.orEmpty().filter {
        Instant.ofEpochMilli(it.timeMs).atZone(zone).year in 1900..2100
    }.map { SpaceCompassTemporalComparison.create(body, it.timeMs, zone, SpaceCompassComparisonSource.EVENT,
        spaceCompassComparisonOrigin(it.kind.name, it.timeMs, zone)) }
