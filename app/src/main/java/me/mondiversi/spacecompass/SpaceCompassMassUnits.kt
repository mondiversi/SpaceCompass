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

/** Display policy only: switch by mass, never by object category or selected kg/lb. */
internal const val SPACE_COMPASS_SOLAR_MASS_DISPLAY_THRESHOLD = 10.0

internal fun spaceCompassUsesSolarMass(facts: SpaceCompassCelestialFacts): Boolean {
    val solar = facts.massSolar
    return if (solar != null) solar.isFinite() && solar >= SPACE_COMPASS_SOLAR_MASS_DISPLAY_THRESHOLD
        else facts.massKg?.let { it.isFinite() && it >= SPACE_COMPASS_SOLAR_MASS_DISPLAY_THRESHOLD * SPACE_COMPASS_SOLAR_MASS_KG } == true
}

/** Prefer the published solar reference when present; no fallback from invalid data. */
internal fun spaceCompassCelestialMassKilograms(facts: SpaceCompassCelestialFacts): Double? =
    (facts.massSolar?.times(SPACE_COMPASS_SOLAR_MASS_KG) ?: facts.massKg)
        ?.takeIf { it.isFinite() && it > 0 }

/** A shared exponent keeps converted stellar uncertainties compact and unambiguous. */
private fun formatSpaceCompassMassWithUncertainty(value: Double, uncertainty: Double?,
    numeric: SpaceCompassNumericFormat): String {
    if (uncertainty == null) return formatSpaceCompassScientificNumber(value, numeric)
    if (value < 1e6) return formatSpaceCompassScientificNumber(value, numeric) + " ± " +
        formatSpaceCompassScientificNumber(uncertainty, numeric)
    val exponent = kotlin.math.floor(kotlin.math.log10(value)).toInt()
    val scale = Math.pow(10.0, exponent.toDouble())
    val error = uncertainty / scale
    val digits = (1 - kotlin.math.floor(kotlin.math.log10(error)).toInt()).coerceIn(3, 12)
    val amount = formatSpaceCompassNumber(value / scale, 3, numeric)
    val errorText = formatSpaceCompassNumber(error, digits, numeric, minimumDigits = 0)
    val power = exponent.toString().map { "⁰¹²³⁴⁵⁶⁷⁸⁹"[it.digitToInt()] }.joinToString("")
    return "($amount ± $errorText) × 10$power"
}

internal fun formatSpaceCompassCelestialMass(body: SpaceCompassCelestialBody,
    facts: SpaceCompassCelestialFacts, numeric: SpaceCompassNumericFormat, pounds: Boolean = false): String {
    val solar = facts.massSolar ?: facts.massKg?.div(SPACE_COMPASS_SOLAR_MASS_KG) ?: return "—"
    if (!solar.isFinite() || solar <= 0) return "—"
    val uncertainty = facts.massSolarError
    if (uncertainty != null && (!uncertainty.isFinite() || uncertainty <= 0)) return "—"
    val amount: String
    val unit: String
    if (spaceCompassUsesSolarMass(facts)) {
        val value = if (solar >= 1e6) formatSpaceCompassScientificNumber(solar, numeric)
            else formatSpaceCompassNumber(solar, 3, numeric, minimumDigits = 0)
        val error = uncertainty?.let { " ± " + formatSpaceCompassNumber(it, 3, numeric, minimumDigits = 0) } ?: ""
        amount = value + error
        unit = "M☉"
    } else {
        val kilograms = spaceCompassCelestialMassKilograms(facts) ?: return "—"
        val value = spaceCompassMassForDisplay(kilograms, pounds)
        val error = uncertainty?.let { spaceCompassMassForDisplay(it * SPACE_COMPASS_SOLAR_MASS_KG, pounds) }
        if (!value.isFinite() || value <= 0 || (error != null && (!error.isFinite() || error <= 0))) return "—"
        amount = formatSpaceCompassMassWithUncertainty(value, error, numeric)
        unit = if (pounds) "lb" else "kg"
    }
    val prefix = if (facts.massModelAssumption) "† " else if (facts.massEstimated) "≈ " else ""
    return prefix + amount + " " + unit + if (body == SpaceCompassCelestialBody.ALPHA_CENTAURI) " (A+B)" else ""
}
