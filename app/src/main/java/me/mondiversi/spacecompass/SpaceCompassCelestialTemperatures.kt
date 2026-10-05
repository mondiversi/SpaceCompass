package me.mondiversi.spacecompass

/** Reference temperatures, never current telemetry or a function of the visible phase.
 * The quantity states the physical layer and statistic; see docs/CELESTIAL_VIEWER.md. */
internal enum class SpaceCompassCelestialTemperatureKind(val labelResource: Int) {
    SURFACE_MEAN(R.string.celestial_temperature_surface_mean),
    DAY_MAXIMUM(R.string.celestial_temperature_day_maximum),
    NIGHT_MINIMUM(R.string.celestial_temperature_night_minimum),
    SURFACE_RANGE(R.string.celestial_temperature_surface_range),
    ATMOSPHERE_ONE_BAR(R.string.celestial_temperature_atmosphere),
    PHOTOSPHERE(R.string.celestial_temperature_photosphere),
    STELLAR_EFFECTIVE(R.string.celestial_temperature_effective)
}

internal data class SpaceCompassCelestialTemperature(
    val kind: SpaceCompassCelestialTemperatureKind,
    val celsius: Double,
    val maximumCelsius: Double? = null
)

internal fun spaceCompassCelestialTemperatures(body: SpaceCompassCelestialBody): List<SpaceCompassCelestialTemperature> {
    fun value(kind: SpaceCompassCelestialTemperatureKind, celsius: Double) =
        SpaceCompassCelestialTemperature(kind, celsius)
    fun mean(celsius: Double) = listOf(value(SpaceCompassCelestialTemperatureKind.SURFACE_MEAN, celsius))
    fun atmosphere(celsius: Double) = listOf(value(SpaceCompassCelestialTemperatureKind.ATMOSPHERE_ONE_BAR, celsius))
    fun dayNight(day: Double, night: Double) = listOf(
        value(SpaceCompassCelestialTemperatureKind.DAY_MAXIMUM, day),
        value(SpaceCompassCelestialTemperatureKind.NIGHT_MINIMUM, night))
    return when (body) {
        SpaceCompassCelestialBody.EARTH_CENTER, SpaceCompassCelestialBody.TRAPPIST_1_E -> emptyList()
        SpaceCompassCelestialBody.PROXIMA_CENTAURI -> listOf(value(SpaceCompassCelestialTemperatureKind.STELLAR_EFFECTIVE, 2900.0 - 273.15))
        SpaceCompassCelestialBody.RIGEL -> listOf(value(SpaceCompassCelestialTemperatureKind.STELLAR_EFFECTIVE, 12100.0 - 273.15))
        SpaceCompassCelestialBody.SUN -> listOf(value(SpaceCompassCelestialTemperatureKind.PHOTOSPHERE, 5_500.0))
        SpaceCompassCelestialBody.MERCURY -> dayNight(430.0, -180.0)
        SpaceCompassCelestialBody.VENUS -> mean(464.0)
        SpaceCompassCelestialBody.MOON -> dayNight(127.0, -173.0)
        SpaceCompassCelestialBody.MARS -> mean(-65.0) +
            SpaceCompassCelestialTemperature(SpaceCompassCelestialTemperatureKind.SURFACE_RANGE, -153.0, 20.0)
        SpaceCompassCelestialBody.JUPITER -> atmosphere(-110.0)
        SpaceCompassCelestialBody.SATURN -> atmosphere(-140.0)
        SpaceCompassCelestialBody.URANUS -> atmosphere(-195.0)
        SpaceCompassCelestialBody.NEPTUNE -> atmosphere(-200.0)
        SpaceCompassCelestialBody.IO -> mean(-155.0)
        SpaceCompassCelestialBody.EUROPA -> listOf(
            SpaceCompassCelestialTemperature(SpaceCompassCelestialTemperatureKind.SURFACE_RANGE, -223.0, -133.0))
        SpaceCompassCelestialBody.PLUTO -> mean(-225.0)
        // Polaris Aa's mean effective temperature in the cited 2015 spectra, not the whole system.
        SpaceCompassCelestialBody.POLARIS -> listOf(value(SpaceCompassCelestialTemperatureKind.STELLAR_EFFECTIVE, 6_017.0 - 273.15))
        // No defensible single current hull temperature, nor a measured Sedna surface temperature.
        SpaceCompassCelestialBody.ALPHA_CENTAURI, SpaceCompassCelestialBody.SAGITTARIUS_A, SpaceCompassCelestialBody.STEPHENSON_2_18, SpaceCompassCelestialBody.RX_J1856,
    SpaceCompassCelestialBody.PSR_J0437, SpaceCompassCelestialBody.TON_618, SpaceCompassCelestialBody.ANDROMEDA_CORE,
        SpaceCompassCelestialBody.SEDNA, SpaceCompassCelestialBody.ISS, SpaceCompassCelestialBody.STARLINK_V3,
        SpaceCompassCelestialBody.VOYAGER_1, SpaceCompassCelestialBody.VOYAGER_2 -> emptyList()
    }
}

internal fun formatSpaceCompassCelestialTemperature(value: SpaceCompassCelestialTemperature,
    numeric: SpaceCompassNumericFormat, fahrenheit: Boolean = false): String {
    val maximum = value.maximumCelsius
    if (!value.celsius.isFinite() || value.celsius < -273.15 ||
        (maximum != null && (!maximum.isFinite() || maximum < value.celsius))) return "—"
    fun number(celsius: Double) = formatSpaceCompassNumber(celsius, 0, numeric)
    fun temperature(celsius: Double) = number(if (fahrenheit) celsius * 1.8 + 32 else celsius)
    val unit = if (fahrenheit) "°F" else "°C"
    val reference = if (value.kind == SpaceCompassCelestialTemperatureKind.STELLAR_EFFECTIVE)
        " (${number(value.celsius + 273.15)} K)" else ""
    return if (maximum == null) "≈${temperature(value.celsius)} $unit$reference"
        else "≈${temperature(value.celsius)} … ${temperature(maximum)} $unit"
}
