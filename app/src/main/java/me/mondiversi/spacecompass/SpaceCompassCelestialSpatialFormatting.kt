package me.mondiversi.spacecompass

import java.util.Locale
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.pow
import kotlin.math.roundToLong

/** RA is an angle measured in hours, independent of the user's 12/24-hour clock. */
internal fun formatSpaceCompassRightAscension(hours: Double, numeric: SpaceCompassNumericFormat,
    sexagesimal: Boolean, locale: Locale = Locale.getDefault()): String {
    if (!hours.isFinite()) return "—"
    val wrapped = ((hours % 24) + 24) % 24
    if (!sexagesimal) {
        val rounded = (wrapped * 100000).roundToLong() % 2400000 / 100000.0
        return formatSpaceCompassNumber(rounded, 5, numeric, grouping = false, systemLocale = locale) + " h"
    }
    val ticks = (wrapped * 360000).roundToLong() % 8640000
    val h = ticks / 360000
    val m = ticks % 360000 / 6000
    val s = ticks % 6000 / 100.0
    fun number(value: Double, digits: Int = 0) = formatSpaceCompassNumber(value, digits, numeric,
        grouping = false, systemLocale = locale)
    return "${number(h.toDouble())}h ${number(m.toDouble())}m ${number(s, 2)}s"
}

internal fun formatSpaceCompassDeclination(degrees: Double, numeric: SpaceCompassNumericFormat,
    sexagesimal: Boolean, locale: Locale = Locale.getDefault()): String {
    if (!degrees.isFinite() || abs(degrees) > 90) return "—"
    val sign = if (degrees < 0) "−" else "+"
    fun number(value: Double, digits: Int = 0) = formatSpaceCompassNumber(value, digits, numeric,
        grouping = false, systemLocale = locale)
    if (!sexagesimal) return sign + number(abs(degrees), 5) + "°"
    val ticks = (abs(degrees) * 36000).roundToLong()
    val d = ticks / 36000
    val m = ticks % 36000 / 600
    val s = ticks % 600 / 10.0
    return "$sign${number(d.toDouble())}° ${number(m.toDouble())}′ ${number(s, 1)}″"
}

/** A spherical estimate, not a measured volume of an irregular body, system or horizon. */
internal fun spaceCompassCelestialSphericalVolumeKm3(body: SpaceCompassCelestialBody,
    facts: SpaceCompassCelestialFacts): Double? {
    if (body == SpaceCompassCelestialBody.EARTH_CENTER || body.isFictional || body.catalogType !in setOf(
            SpaceCompassCatalogType.PLANET, SpaceCompassCatalogType.DWARF_PLANET,
            SpaceCompassCatalogType.EXOPLANET, SpaceCompassCatalogType.NATURAL_SATELLITE,
            SpaceCompassCatalogType.STAR)) return null
    val diameter = facts.diameterKm?.takeIf { it.isFinite() && it > 0 } ?: return null
    return (PI / 6 * diameter.pow(3)).takeIf { it.isFinite() && it > 0 }
}

internal fun formatSpaceCompassCelestialVolume(volumeKm3: Double?, numeric: SpaceCompassNumericFormat,
    imperial: Boolean): String? {
    val volume = volumeKm3?.takeIf { it.isFinite() && it > 0 } ?: return null
    val converted = if (imperial) volume / 1.609344.pow(3) else volume
    return "≈ " + (if (converted < 1) formatSpaceCompassNumber(converted, 6, numeric, minimumDigits = 0)
        else formatSpaceCompassScientificNumber(converted, numeric)) + if (imperial) " mi³" else " km³"
}
