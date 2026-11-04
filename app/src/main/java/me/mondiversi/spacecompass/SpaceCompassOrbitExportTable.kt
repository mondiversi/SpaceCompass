package me.mondiversi.spacecompass

import java.time.Instant
import java.time.ZoneId

internal enum class SpaceCompassOrbitExportRowKind { POINT, CURRENT, EVENT, SELECTED, PASS, ANTIPODE }

internal data class SpaceCompassOrbitExportCell(val point: SpaceCompassSunPathPoint, val selected: Boolean)

internal data class SpaceCompassOrbitExportRow(
    val kind: SpaceCompassOrbitExportRowKind,
    val index: Int = 0,
    val occurrence: Int = 0,
    val event: SpaceCompassSunPathEvent? = null,
    val cells: Map<SpaceCompassCelestialBody, SpaceCompassOrbitExportCell> = emptyMap(),
    val comparisonCells: Map<String, SpaceCompassOrbitExportCell> = emptyMap()
) {
    val allCells: List<SpaceCompassOrbitExportCell> get() = cells.values.toList() + comparisonCells.values
    fun cell(item: SpaceCompassPanoramaObject): SpaceCompassOrbitExportCell? =
        item.comparison?.let { comparisonCells[it.id] } ?: cells[item.body].takeIf { item.comparison == null }
}

/** Rows share an instant, so each object's populated cells follow its real timeline.
 * Empty sample slots keep their labels and follow the available data without inventing a time.
 */
internal fun spaceCompassChronologicalOrbitExportRows(rows: List<SpaceCompassOrbitExportRow>): List<SpaceCompassOrbitExportRow> =
    rows.flatMap { row ->
        val times = row.allCells.map { it.point.timeMs }.distinct()
        if (times.isEmpty()) listOf(row) else times.map { time -> row.copy(
            cells = row.cells.filterValues { it.point.timeMs == time },
            comparisonCells = row.comparisonCells.filterValues { it.point.timeMs == time }) }
    }.sortedWith(compareBy<SpaceCompassOrbitExportRow> { it.allCells.firstOrNull()?.point?.timeMs ?: Long.MAX_VALUE }
        .thenBy { it.kind.ordinal }.thenBy { it.index }.thenBy { it.occurrence })

/** Canonical positions and flags copied at the shutter, never from the later main-page state. */
internal data class SpaceCompassCapturedOrbitTable(
    val observationTimeMs: Long,
    val zoneId: String,
    val bodies: List<SpaceCompassCelestialBody>,
    val rows: List<SpaceCompassOrbitExportRow>
)

internal fun captureSpaceCompassOrbitTable(snapshot: SpaceCompassPanoramaSnapshot,
    frozenCurrent: Map<SpaceCompassCelestialBody,SpaceCompassSunPosition>? = null): SpaceCompassCapturedOrbitTable {
    val zone = ZoneId.of(snapshot.zoneId)
    val current = frozenCurrent ?: if (!snapshot.observerPositionKnown) emptyMap() else snapshot.objects.filter { it.comparison == null }.mapNotNull { item ->
        runCatching { calculateSpaceCompassCelestialObservation(item.body, snapshot.timeMs, snapshot.latitude,
            snapshot.longitude, snapshot.altitude, snapshot.remote)?.position }.getOrNull()?.let { item.body to it }
    }.toMap()
    val resolved = resolveSpaceCompassPanoramaSelections(snapshot, current)
    val pins = resolved.filterNot { it.isAntipode }
    val rows = linkedMapOf<Triple<SpaceCompassOrbitExportRowKind, Int, Int>, SpaceCompassOrbitExportRow>()
    fun add(kind: SpaceCompassOrbitExportRowKind, index: Int, occurrence: Int,
        body: SpaceCompassCelestialBody, point: SpaceCompassSunPathPoint, event: SpaceCompassSunPathEvent? = null,
        comparisonId: String? = null,
        selected: Boolean = pins.any { it.body == body && it.comparison?.id == comparisonId && it.point == point && it.isCurrent == (kind == SpaceCompassOrbitExportRowKind.CURRENT) }) {
        if (!point.position.azimuthDegrees.isFinite() || !point.position.elevationDegrees.isFinite() ||
            point.position.elevationDegrees !in -90.0..90.0) return
        val key = Triple(kind, index, occurrence)
        val row = rows[key] ?: SpaceCompassOrbitExportRow(kind, index, occurrence, event)
        val cell = SpaceCompassOrbitExportCell(point.copy(coincidentEvents = point.coincidentEvents.toSet()), selected)
        rows[key] = if (comparisonId == null) row.copy(cells = row.cells + (body to cell))
            else row.copy(comparisonCells = row.comparisonCells + (comparisonId to cell))
    }
    // Preserve all 0..23 sample labels even when an object's data is unavailable.
    if (!snapshot.forward24Hours) repeat(24) { rows[Triple(SpaceCompassOrbitExportRowKind.POINT,it,0)] =
        SpaceCompassOrbitExportRow(SpaceCompassOrbitExportRowKind.POINT,it) }
    snapshot.objects.forEach { item ->
        val referenceTime = item.referenceTime(snapshot.timeMs)
        val itemZone = item.comparison?.zone ?: zone
        val comparisonId = item.comparison?.id
        val path = item.path ?: if (snapshot.observerPositionKnown) runCatching {
            calculateSpaceCompassCelestialPath(item.body, Instant.ofEpochMilli(referenceTime).atZone(itemZone).toLocalDate(),
                itemZone, referenceTime, snapshot.latitude, snapshot.longitude, snapshot.altitude, snapshot.remote,forward24Hours=snapshot.forward24Hours && item.comparison == null)?.copy(comparison = item.comparison)
        }.getOrNull() else null
        val hourly = path?.markers.orEmpty().filter { SpaceCompassSunPathEvent.HOUR in it.events }
            .distinctBy { it.timeMs }.sortedBy { it.timeMs }
        val occurrences = mutableMapOf<Int,Int>()
        hourly.forEachIndexed { index, point ->
            val number = if (item.body.isEarthSatellite && path?.forward24Hours != true) index else Instant.ofEpochMilli(point.timeMs)
                .atZone(path!!.zone).hour
            val occurrence = occurrences.getOrDefault(number,0)
            occurrences[number] = occurrence + 1
            add(SpaceCompassOrbitExportRowKind.POINT,number,occurrence,item.body,point,comparisonId=comparisonId)
        }
        val eventOccurrences = mutableMapOf<SpaceCompassSunPathEvent,Int>()
        path?.markers.orEmpty().sortedBy { it.timeMs }.forEach { point ->
            point.events.filter { it != SpaceCompassSunPathEvent.HOUR }.sortedBy { it.ordinal }.forEach { event ->
                val occurrence = eventOccurrences.getOrDefault(event,0)
                eventOccurrences[event] = occurrence + 1
                add(SpaceCompassOrbitExportRowKind.EVENT,event.ordinal,occurrence,item.body,point,event,comparisonId=comparisonId)
            }
        }
        (if (item.comparison == null) current[item.body] else if (snapshot.observerPositionKnown)
            item.currentPosition(snapshot) else item.currentPoint?.position)?.let { position ->
            spaceCompassCurrentPathPoint(item.body,referenceTime,position)?.let {
                add(SpaceCompassOrbitExportRowKind.CURRENT,0,0,item.body,it,comparisonId=comparisonId)
            }
        }
        // Pinned points from an older satellite window must not disappear from the export.
        pins.filter { it.body == item.body && it.comparison?.id == comparisonId && !it.isCurrent }.filter { pin ->
            rows.values.none { it.cell(item)?.point == pin.point }
        }.sortedBy { it.point.timeMs }.forEachIndexed { index,pin ->
            add(SpaceCompassOrbitExportRowKind.SELECTED,index,0,item.body,pin.point,comparisonId=comparisonId,selected=true)
        }
    }
    resolved.filter { it.isAntipode }.groupBy { it.body to it.comparison?.id }.forEach { (identity, mirrors) ->
        mirrors.sortedBy { it.point.timeMs }.forEachIndexed { index, mirror ->
            add(SpaceCompassOrbitExportRowKind.ANTIPODE, index, 0, identity.first, mirror.point, comparisonId = identity.second, selected = true)
        }
    }
    val sorted = spaceCompassChronologicalOrbitExportRows(rows.values.toList())
    return SpaceCompassCapturedOrbitTable(snapshot.timeMs,snapshot.zoneId,snapshot.objects.map { it.body }.toList(),sorted)
}

/** Text exports carry this vocabulary alongside the already frozen profile and object names. */
internal data class SpaceCompassOrbitExportVocabulary(
    val point: String = "Point %s", val time: String = "Time", val azimuth: String = "Azimuth",
    val elevation: String = "Elevation", val events: String = "Events", val selected: String = "Selected",
    val capturedAt: String = "Capture time", val observedAt: String = "Observation time",
    val context: String = "Capture details", val legend: String = "* = selected; ! = key point; — = unavailable",
    val illuminated: String = "Illuminated disc", val pass: String = "Next pass",
    val fictionNote: String = "Fictional illustration. Pointing follows the real reference, not a measured orbit.",
    val referenceLabel: String = "Direction reference", val antipode: String = "Antipode"
)
