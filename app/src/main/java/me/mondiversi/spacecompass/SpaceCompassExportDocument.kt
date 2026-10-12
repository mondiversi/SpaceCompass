package me.mondiversi.spacecompass

/** Fully formatted at opening time; neither native delivery nor profile changes query live data. */
internal data class SpaceCompassExportDocument(val title: String, val metadata: List<Pair<String,String>>,
    val sections: List<SpaceCompassCelestialDetailSection>, val notes: List<String>,
    val columns: List<String>, val notesTitle: String, val decimalComma: Boolean)

internal fun spaceCompassExportDocumentGrid(document: SpaceCompassExportDocument): List<List<String>> =
    listOf(document.columns) + document.metadata.map { listOf(document.title,it.first,it.second,"") } +
    document.sections.flatMap { section -> section.rows.map { row ->
        listOf(section.title,row.label,row.value,listOfNotNull(row.subtitle,section.note).joinToString("\n"))
    } } + document.notes.map { listOf(document.notesTitle,"","",it) }

internal fun renderSpaceCompassExportDocumentCsv(document: SpaceCompassExportDocument): String {
    val separator=if (document.decimalComma) ";" else ","
    return "\uFEFF" + spaceCompassExportDocumentGrid(document).joinToString("\r\n") { row ->
        row.joinToString(separator) { spaceCompassOrbitCsvCell(it) }
    } + "\r\n"
}

internal fun renderSpaceCompassExportDocumentTxt(document: SpaceCompassExportDocument): String = buildString {
    append(document.title).append('\n')
    document.metadata.forEach { (label,value) -> append(label).append(": ").append(value).append('\n') }
    document.sections.forEach { section ->
        if (section.rows.isNotEmpty()) {
            append('\n').append(section.title).append('\n').append("─".repeat(section.title.length.coerceAtMost(80))).append('\n')
            val width=section.rows.maxOf { row -> row.label.lines().maxOf { it.length } }.coerceAtMost(80)
            section.rows.forEach { row ->
                val labels=row.label.lines()
                val values=row.value.lines()+row.subtitle?.lines().orEmpty()
                repeat(maxOf(labels.size,values.size)) { index ->
                    append(labels.getOrElse(index) { "" }.padEnd(width)).append(" | ")
                        .append(values.getOrElse(index) { "" }).append('\n')
                }
            }
            section.note?.let { append(it).append('\n') }
        }
    }
    if (document.notes.isNotEmpty()) {
        append('\n').append(document.notesTitle).append('\n')
        document.notes.forEach { append(it).append('\n') }
    }
}
