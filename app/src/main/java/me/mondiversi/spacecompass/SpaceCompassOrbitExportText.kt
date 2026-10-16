package me.mondiversi.spacecompass

import java.util.TimeZone

internal fun spaceCompassOrbitCsvCell(value: String): String {
    // Geographic names can come from external sources; keep spreadsheet formulas inert.
    val trimmed = value.trimStart()
    val dangerous = trimmed.firstOrNull() in listOf('=','+','-','@') &&
        !Regex("[+-]?\\d+(?:[.,]\\d+)?").matches(trimmed)
    val safe = if (dangerous) "'$value" else value
    return "\"${safe.replace("\"","\"\"")}\""
}

private fun spaceCompassOrbitExportRowName(row: SpaceCompassOrbitExportRow, snapshot: SpaceCompassPanoramaSnapshot): String {
    val vocabulary = snapshot.orbitExportVocabulary
    val number = formatSpaceCompassNumber(row.index.toDouble(),0,snapshot.formatting.numeric,
        grouping=false,systemLocale=snapshot.formatting.deviceLocale)
    val label = when (row.kind) {
        SpaceCompassOrbitExportRowKind.POINT -> String.format(snapshot.formatting.locale,vocabulary.point,number)
        SpaceCompassOrbitExportRowKind.CURRENT -> snapshot.currentPointName
        SpaceCompassOrbitExportRowKind.EVENT -> snapshot.pointEventNames[row.event] ?: row.event!!.name
        SpaceCompassOrbitExportRowKind.SELECTED -> "${vocabulary.selected} · ${String.format(snapshot.formatting.locale,vocabulary.point,number)}"
        SpaceCompassOrbitExportRowKind.PASS -> "${vocabulary.pass} · " +
            (snapshot.pointEventNames[row.event] ?: String.format(snapshot.formatting.locale,vocabulary.point,number))
    }
    val occurrence = formatSpaceCompassNumber((row.occurrence + 1).toDouble(),0,snapshot.formatting.numeric,
        grouping=false,systemLocale=snapshot.formatting.deviceLocale)
    return if (row.occurrence == 0) label else "$label ($occurrence)"
}

private fun spaceCompassOrbitExportCellValues(cell: SpaceCompassOrbitExportCell?, row: SpaceCompassOrbitExportRow,
    snapshot: SpaceCompassPanoramaSnapshot): List<String> {
    if (cell == null) return List(7) { "—" }
    val point = cell.point
    val formatting = snapshot.formatting
    val zone = snapshot.displayTimeZone
    val moment = formatSpaceCompassPanoramaExportTimestamp(point.timeMs,zone,formatting).removePrefix("Space Compass · ")
    val events = point.events.filter { it != SpaceCompassSunPathEvent.HOUR }.sortedBy { it.ordinal }
        .mapNotNull { event -> snapshot.pointEventNames[event]?.let {
            if (event == SpaceCompassSunPathEvent.APPROACH)
                spaceCompassApproachLabel(point,it,formatting,snapshot.bodyNames) else it
        } }.joinToString(" · ")
    val illumination = point.moonPhase?.illuminatedFraction?.let {
        formatSpaceCompassNumber(it*100,1,formatting.numeric,grouping=false,systemLocale=formatting.deviceLocale)
    } ?: "—"
    return listOf(moment,formatSpaceCompassPanoramaPointCountdown(point.timeMs,snapshot.timeMs,formatting),
        formatSpaceCompassPanoramaElevation(point.position.azimuthDegrees,formatting),
        formatSpaceCompassPanoramaElevation(point.position.elevationDegrees,formatting),
        events.ifBlank { if (row.kind == SpaceCompassOrbitExportRowKind.CURRENT) snapshot.currentPointName else "" },
        if (cell.selected) "* ${snapshot.orbitExportVocabulary.selected}" else "",illumination)
}

internal fun spaceCompassOrbitExportGrid(table: SpaceCompassCapturedOrbitTable, snapshot: SpaceCompassPanoramaSnapshot,
    captureTimeMs: Long, caption: String): List<List<String>> {
    require(table.observationTimeMs == snapshot.timeMs && table.zoneId == snapshot.zoneId)
    require(table.bodies == snapshot.objects.map { it.body })
    val vocabulary = snapshot.orbitExportVocabulary
    val columns = listOf(vocabulary.time,"T±","${vocabulary.azimuth} (°)","${vocabulary.elevation} (°)",
        vocabulary.events,vocabulary.selected,"${vocabulary.illuminated} (%)")
    val pointHeading = vocabulary.point.replace("%s", "").replace("%1\$s", "").trim()
    val headers = listOf(pointHeading) + snapshot.objects.flatMap { item -> columns.map { "${item.name} — $it" } }
    fun metadata(name: String,value: String) = listOf(name,value) + List((headers.size-2).coerceAtLeast(0)) { "" }
    val zone = snapshot.displayTimeZone
    return listOf(headers,
        metadata(vocabulary.capturedAt,formatSpaceCompassPanoramaExportTimestamp(captureTimeMs,zone,snapshot.formatting)),
        metadata(vocabulary.observedAt,formatSpaceCompassPanoramaExportTimestamp(table.observationTimeMs,zone,snapshot.formatting)),
        metadata(vocabulary.context,caption),metadata(vocabulary.events,vocabulary.legend)) + table.rows.map { row ->
        val special = row.kind == SpaceCompassOrbitExportRowKind.CURRENT || row.kind == SpaceCompassOrbitExportRowKind.EVENT ||
            row.cells.values.any { cell -> cell.point.events.any { it != SpaceCompassSunPathEvent.HOUR } }
        val prefix = (if (row.cells.values.any { it.selected }) "* " else "") + (if (special) "! " else "")
        listOf(prefix + spaceCompassOrbitExportRowName(row,snapshot)) + table.bodies.flatMap {
            spaceCompassOrbitExportCellValues(row.cells[it],row,snapshot)
        }
    }
}

internal fun renderSpaceCompassOrbitCsv(table: SpaceCompassCapturedOrbitTable, snapshot: SpaceCompassPanoramaSnapshot,
    captureTimeMs: Long, caption: String): String {
    val separator = if (formatSpaceCompassNumber(1.5,1,snapshot.formatting.numeric,grouping=false,
        systemLocale=snapshot.formatting.deviceLocale).contains(',')) ';' else ','
    return "\uFEFF" + spaceCompassOrbitExportGrid(table,snapshot,captureTimeMs,caption).joinToString("\r\n") {
        it.joinToString(separator.toString()) { value -> spaceCompassOrbitCsvCell(value) }
    } + "\r\n"
}

/** Each object's column stays together; marker flags are visible even in plain-text viewers. */
internal fun renderSpaceCompassOrbitTxt(table: SpaceCompassCapturedOrbitTable, snapshot: SpaceCompassPanoramaSnapshot,
    captureTimeMs: Long, caption: String): String {
    require(table.observationTimeMs == snapshot.timeMs && table.zoneId == snapshot.zoneId)
    require(table.bodies == snapshot.objects.map { it.body })
    val vocabulary = snapshot.orbitExportVocabulary
    val zone = snapshot.displayTimeZone
    val header = listOf("Space Compass", "${vocabulary.capturedAt}: ${formatSpaceCompassPanoramaExportTimestamp(captureTimeMs,zone,snapshot.formatting)}",
        "${vocabulary.observedAt}: ${formatSpaceCompassPanoramaExportTimestamp(snapshot.timeMs,zone,snapshot.formatting)}",
        caption,vocabulary.legend).joinToString("\n")
    val names = listOf(vocabulary.point.replace("%s", "").replace("%1\$s", "").trim()) + snapshot.objects.map { it.name }
    val rows = table.rows.map { row ->
        val special = row.kind == SpaceCompassOrbitExportRowKind.CURRENT || row.kind == SpaceCompassOrbitExportRowKind.EVENT ||
            row.cells.values.any { cell -> cell.point.events.any { it != SpaceCompassSunPathEvent.HOUR } }
        val name = (if (row.cells.values.any { it.selected }) "* " else "") + (if (special) "! " else "") +
            spaceCompassOrbitExportRowName(row,snapshot)
        listOf(listOf(name)) + table.bodies.map { body ->
            if (row.cells[body] == null) listOf("—") else {
                val values = spaceCompassOrbitExportCellValues(row.cells[body],row,snapshot)
                listOf(values[0],"${values[1]} · ${vocabulary.azimuth}: ${values[2]}° · ${vocabulary.elevation}: ${values[3]}°") +
                    listOfNotNull(values[4].takeIf { it.isNotBlank() },values[5].takeIf { it.isNotBlank() },
                        values[6].takeIf { it != "—" }?.let { "${vocabulary.illuminated}: $it%" })
            }
        }
    }
    val widths = names.indices.map { column -> maxOf(names[column].length,rows.maxOfOrNull { row ->
        row[column].maxOfOrNull { it.length } ?: 1 } ?: 1) }
    fun line(values: List<String>) = values.mapIndexed { column,value -> value.padEnd(widths[column]) }.joinToString(" | ").trimEnd()
    val divider = widths.joinToString("-+-") { "-".repeat(it) }
    return buildString {
        append(header).append("\n\n").append(line(names)).append('\n').append(divider).append('\n')
        rows.forEach { row ->
            repeat(row.maxOf { it.size }) { index -> append(line(row.map { it.getOrElse(index) { "" } })).append('\n') }
            append(divider).append('\n')
        }
    }
}
