package me.mondiversi.spacecompass

/** Exact international avoirdupois pound: NIST SP 811, Appendix B, note 22. */
internal const val SPACE_COMPASS_KG_PER_POUND = 0.45359237
private const val SPACE_COMPASS_CUBIC_METERS_PER_CUBIC_FOOT = 0.028316846592

/** Presentation conversion only; celestial reference facts and physics stay in SI. */
internal fun spaceCompassMassForDisplay(kilograms: Double, pounds: Boolean): Double =
    if (pounds) kilograms / SPACE_COMPASS_KG_PER_POUND else kilograms

internal fun formatSpaceCompassCelestialDensity(kilogramsPerCubicMeter: Double?,
    numeric: SpaceCompassNumericFormat, units: SpaceCompassUnits, fractionDigits: Int = 0): String {
    val density = kilogramsPerCubicMeter?.takeIf { it.isFinite() && it > 0 } ?: return "—"
    val value = spaceCompassMassForDisplay(density, units.pounds) *
        if (units.feet) SPACE_COMPASS_CUBIC_METERS_PER_CUBIC_FOOT else 1.0
    if (!value.isFinite() || value <= 0) return "—"
    // Retain fractional densities, including the low-density giant Polaris.
    val digits = maxOf(fractionDigits, if (value < 1) 3 else if (units.feet) 1 else 0)
    val mass = if (units.pounds) "lb" else "kg"
    val volume = if (units.feet) "ft³" else "m³"
    return "${formatSpaceCompassNumber(value, digits, numeric)} $mass/$volume"
}
