package me.mondiversi.spacecompass

/** Reference atmosphere/exosphere only; no core, photosphere or spacecraft cabin pressure. */
internal enum class SpaceCompassAtmosphericPressureKind(val labelResource: Int) {
    SURFACE(R.string.celestial_pressure_surface),
    EXOSPHERE(R.string.celestial_pressure_exosphere),
    NIGHT_EXOSPHERE(R.string.celestial_pressure_night_exosphere)
}

internal data class SpaceCompassAtmosphericPressure(val pascals: Double,
    val kind: SpaceCompassAtmosphericPressureKind = SpaceCompassAtmosphericPressureKind.SURFACE,
    val upperLimit: Boolean = false, val constituent: String? = null)

/** NASA NSSDCA fact sheets; Io/Europa: Bagenal & Dols 2020, Table 1.
 * Approximate published reference values, not current telemetry. Sources/qualifications
 * and the absence of a defined gas-giant surface are documented separately. */
internal fun spaceCompassAtmosphericPressure(body: SpaceCompassCelestialBody): SpaceCompassAtmosphericPressure? = when (body) {
    SpaceCompassCelestialBody.TITAN -> SpaceCompassAtmosphericPressure(146_700.0)
    SpaceCompassCelestialBody.VENUS -> SpaceCompassAtmosphericPressure(9_200_000.0)
    SpaceCompassCelestialBody.MARS -> SpaceCompassAtmosphericPressure(636.0)
    SpaceCompassCelestialBody.PLUTO -> SpaceCompassAtmosphericPressure(1.3)
    SpaceCompassCelestialBody.MERCURY -> SpaceCompassAtmosphericPressure(5e-10,
        SpaceCompassAtmosphericPressureKind.EXOSPHERE, upperLimit = true)
    SpaceCompassCelestialBody.MOON -> SpaceCompassAtmosphericPressure(3e-10,
        SpaceCompassAtmosphericPressureKind.NIGHT_EXOSPHERE)
    SpaceCompassCelestialBody.IO -> SpaceCompassAtmosphericPressure(2e-4, constituent = "SO₂")
    SpaceCompassCelestialBody.EUROPA -> SpaceCompassAtmosphericPressure(2e-5,
        SpaceCompassAtmosphericPressureKind.EXOSPHERE, constituent = "O₂")
    else -> null
}

internal fun formatSpaceCompassAtmosphericPressure(pressure: SpaceCompassAtmosphericPressure,
    numeric: SpaceCompassNumericFormat, unit: SpaceCompassPressureUnit): String? {
    val value = formatSpaceCompassPressure(pressure.pascals, numeric, unit) ?: return null
    val prefix = if (pressure.upperLimit) "< ≈ " else "≈ "
    return prefix + value + (pressure.constituent?.let { " ($it)" } ?: "")
}
