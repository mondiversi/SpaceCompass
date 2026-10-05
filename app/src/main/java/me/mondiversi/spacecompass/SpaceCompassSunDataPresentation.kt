package me.mondiversi.spacecompass

/** Split the existing localized label/value templates without duplicating twenty translations. */
internal const val SPACE_COMPASS_SUN_DATA_MARKER = "\uFFF0"
internal data class SpaceCompassSunDataRow(val label: String, val value: String, val announcement: String, val tag: String)

internal fun spaceCompassSunDataRow(template: String, value: String, tag: String): SpaceCompassSunDataRow {
    val marker = template.indexOf(SPACE_COMPASS_SUN_DATA_MARKER)
    require(marker >= 0) { "Missing solar data placeholder" }
    val prefix = template.substring(0, marker).trimEnd()
    // Accuracy's ± belongs to the number, not to the label column.
    val sign = if (prefix.endsWith('±')) "±" else ""
    val label = prefix.removeSuffix(sign).trimEnd().trimEnd(':', '：').trimEnd()
    return SpaceCompassSunDataRow(label, sign + value + template.substring(marker + SPACE_COMPASS_SUN_DATA_MARKER.length),
        template.replace(SPACE_COMPASS_SUN_DATA_MARKER, value), tag)
}

/** Unknown quantities stay a bare dash, not a made-up zero or a dash with a unit suffix. */
internal fun spaceCompassSunOptionalDataRow(template: String, value: String?, tag: String): SpaceCompassSunDataRow =
    if (value != null) spaceCompassSunDataRow(template, value, tag)
    else spaceCompassSunDataRow(template.substringBefore(SPACE_COMPASS_SUN_DATA_MARKER).removeSuffix("±") +
        SPACE_COMPASS_SUN_DATA_MARKER, "—", tag)
