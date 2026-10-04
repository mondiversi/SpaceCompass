package me.mondiversi.planetcompass

/** Split the existing localized label/value templates without duplicating twenty translations. */
internal const val PLANET_COMPASS_SUN_DATA_MARKER = "\uFFF0"
internal data class PlanetCompassSunDataRow(val label: String, val value: String, val announcement: String, val tag: String)

internal fun planetCompassSunDataRow(template: String, value: String, tag: String): PlanetCompassSunDataRow {
    val marker = template.indexOf(PLANET_COMPASS_SUN_DATA_MARKER)
    require(marker >= 0) { "Missing solar data placeholder" }
    val prefix = template.substring(0, marker).trimEnd()
    // Accuracy's ± belongs to the number, not to the label column.
    val sign = if (prefix.endsWith('±')) "±" else ""
    val label = prefix.removeSuffix(sign).trimEnd().trimEnd(':', '：').trimEnd()
    return PlanetCompassSunDataRow(label, sign + value + template.substring(marker + PLANET_COMPASS_SUN_DATA_MARKER.length),
        template.replace(PLANET_COMPASS_SUN_DATA_MARKER, value), tag)
}

/** Unknown quantities stay a bare dash, not a made-up zero or a dash with a unit suffix. */
internal fun planetCompassSunOptionalDataRow(template: String, value: String?, tag: String): PlanetCompassSunDataRow =
    if (value != null) planetCompassSunDataRow(template, value, tag)
    else planetCompassSunDataRow(template.substringBefore(PLANET_COMPASS_SUN_DATA_MARKER).removeSuffix("±") +
        PLANET_COMPASS_SUN_DATA_MARKER, "—", tag)
