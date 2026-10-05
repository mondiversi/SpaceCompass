package me.mondiversi.spacecompass

/** Standard psi is pound-force per square inch, not a mass-density unit.
 * Exact pound, standard gravity and inch definitions; see docs/ATMOSPHERIC_PRESSURE.md. */
internal const val SPACE_COMPASS_PASCALS_PER_PSI =
    SPACE_COMPASS_KG_PER_POUND * SPACE_COMPASS_STANDARD_GRAVITY_M_S2 / (0.0254 * 0.0254)
internal const val SPACE_COMPASS_PRESSURE_UNIT_KEY = "pressure_unit"

internal enum class SpaceCompassPressureUnit(val storedValue: String, val symbol: String, val pascals: Double) {
    BAR("bar", "bar", 100_000.0), PASCAL("pa", "Pa", 1.0), PSI("psi", "psi", SPACE_COMPASS_PASCALS_PER_PSI)
}

/** Automatic pressure is independent of app language and other unit overrides. */
internal fun spaceCompassResolvePressureUnit(region: String, preference: String?): SpaceCompassPressureUnit =
    SpaceCompassPressureUnit.entries.firstOrNull { it.storedValue == preference }
        ?: if (region.uppercase(java.util.Locale.ROOT) in setOf("US", "LR", "MM")) SpaceCompassPressureUnit.PSI else SpaceCompassPressureUnit.BAR

/** Very tenuous exospheres need negative exponents, rather than a rounded zero. */
internal fun formatSpaceCompassPressure(pascals: Double, numeric: SpaceCompassNumericFormat,
    unit: SpaceCompassPressureUnit): String? {
    val value = pascals / unit.pascals
    if (!value.isFinite() || value <= 0) return null
    val exponent = kotlin.math.floor(kotlin.math.log10(value)).toInt()
    val text = if (value < 0.001 || value >= 1e6) {
        val power = exponent.toString().map { if (it == '-') '⁻' else "⁰¹²³⁴⁵⁶⁷⁸⁹"[it.digitToInt()] }.joinToString("")
        val mantissa = value / Math.pow(10.0, exponent.toDouble())
        "${formatSpaceCompassNumber(mantissa, 2, numeric, minimumDigits = 0)} × 10$power"
    } else formatSpaceCompassNumber(value, (2 - exponent).coerceIn(0, 8), numeric, minimumDigits = 0)
    return "$text ${unit.symbol}"
}
