package me.mondiversi.spacecompass

import kotlin.math.abs
import kotlin.math.floor
import kotlin.math.log10
import kotlin.math.pow
import kotlin.math.round

internal data class SpaceCompassCelestialCoreReference(
    val temperature: SpaceCompassCelestialTemperature,
    val citation: String,
    val sourceUrl: String,
    val adiabaticReference: Boolean = false
)

/** Curated interior estimates. Separate from surface/atmosphere references so a core
 * never becomes the fallback for day/night catalogue temperatures or their sort order. */
internal fun spaceCompassCelestialCoreTemperature(body: SpaceCompassCelestialBody): SpaceCompassCelestialCoreReference? {
    fun core(celsius: Double, maximum: Double? = null, digits: Int = 2) = SpaceCompassCelestialTemperature(
        SpaceCompassCelestialTemperatureKind.CORE_ESTIMATE, celsius, maximum, significantDigits = digits)
    fun boundary(kelvin: Double, maximumKelvin: Double? = null, digits: Int = 2) = SpaceCompassCelestialTemperature(
        SpaceCompassCelestialTemperatureKind.CORE_BOUNDARY_ESTIMATE, kelvin - 273.15,
        maximumKelvin?.minus(273.15), significantDigits = digits)
    return when (body) {
        SpaceCompassCelestialBody.SUN -> SpaceCompassCelestialCoreReference(core(15_000_000.0),
            "NASA · Marshall Solar Physics", "https://solarscience.msfc.nasa.gov/interior.shtml")
        SpaceCompassCelestialBody.JUPITER -> SpaceCompassCelestialCoreReference(core(24_000.0),
            "NASA · Space Place", "https://spaceplace.nasa.gov/jupiter/en/")
        SpaceCompassCelestialBody.SATURN -> SpaceCompassCelestialCoreReference(core(10_000.0),
            "NASA · Cassini (2004)", "https://www.nasa.gov/wp-content/uploads/2015/01/59910main_cassini.pdf")
        // Current core-mantle-boundary estimate, not the central temperature or a lava flow.
        SpaceCompassCelestialBody.MOON -> SpaceCompassCelestialCoreReference(boundary(1650.0, digits = 3),
            "Righter et al. (2014)", "https://ntrs.nasa.gov/citations/20140007368")
        SpaceCompassCelestialBody.MERCURY -> SpaceCompassCelestialCoreReference(boundary(1800.0, 2000.0),
            "Edmund et al. (2022)", "https://doi.org/10.1038/s41467-022-27991-9")
        SpaceCompassCelestialBody.MARS -> SpaceCompassCelestialCoreReference(core(2000.0 - 273.15, 2400.0 - 273.15),
            "Hsieh et al. (2024)", "https://doi.org/10.1126/sciadv.adk1087")
        // A standard adiabatic reference, not a unique determination of an ice giant's core.
        SpaceCompassCelestialBody.URANUS, SpaceCompassCelestialBody.NEPTUNE -> SpaceCompassCelestialCoreReference(
            core(6000.0 - 273.15).copy(kind = SpaceCompassCelestialTemperatureKind.CORE_ADIABATIC_REFERENCE),
            "Morf & Helled (2025) · Nettelmann et al. (2013)",
            "https://doi.org/10.1051/0004-6361/202556911", adiabaticReference = true)
        // Do not infer a core from mass/surface temperature, a photosphere, corona, accretion
        // disk, comet nucleus surface, or a fictional world's appearance.
        else -> null
    }
}

internal val SpaceCompassCelestialTemperatureKind.isCore: Boolean
    get() = this == SpaceCompassCelestialTemperatureKind.CORE_ESTIMATE ||
        this == SpaceCompassCelestialTemperatureKind.CORE_BOUNDARY_ESTIMATE ||
        this == SpaceCompassCelestialTemperatureKind.CORE_ADIABATIC_REFERENCE

/** Conversion precedes rounding. Uncertain interiors must not gain spurious digits from
 * the Celsius/Fahrenheit offsets; solar millions use a compact scientific notation. */
internal fun formatSpaceCompassCoreTemperatureNumber(value: Double, digits: Int,
    numeric: SpaceCompassNumericFormat): String {
    if (!value.isFinite() || digits !in 1..6) return "—"
    if (value == 0.0) return formatSpaceCompassNumber(0.0, 0, numeric)
    val scale = 10.0.pow(floor(log10(abs(value))) - digits + 1)
    val rounded = round(value / scale) * scale
    if (abs(rounded) < 1e6) return formatSpaceCompassNumber(rounded, 0, numeric)
    val exponent = floor(log10(abs(rounded))).toInt()
    val power = exponent.toString().map { "⁰¹²³⁴⁵⁶⁷⁸⁹"[it.digitToInt()] }.joinToString("")
    return formatSpaceCompassNumber(rounded / 10.0.pow(exponent), digits - 1, numeric,
        grouping = false, minimumDigits = 0) + " × 10$power"
}
