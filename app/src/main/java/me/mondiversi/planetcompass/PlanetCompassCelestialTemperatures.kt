package me.mondiversi.planetcompass

/** Reference temperatures, never current telemetry or a function of the visible phase.
 * The quantity states the physical layer and statistic; see docs/CELESTIAL_VIEWER.md. */
internal enum class PlanetCompassCelestialTemperatureKind(val labelResource: Int) {
    SURFACE_MEAN(R.string.celestial_temperature_surface_mean),
    DAY_MAXIMUM(R.string.celestial_temperature_day_maximum),
    NIGHT_MINIMUM(R.string.celestial_temperature_night_minimum),
    SURFACE_RANGE(R.string.celestial_temperature_surface_range),
    ATMOSPHERE_ONE_BAR(R.string.celestial_temperature_atmosphere),
    PHOTOSPHERE(R.string.celestial_temperature_photosphere),
    STELLAR_EFFECTIVE(R.string.celestial_temperature_effective)
}

internal data class PlanetCompassCelestialTemperature(
    val kind: PlanetCompassCelestialTemperatureKind,
    val celsius: Double,
    val maximumCelsius: Double? = null
)

internal fun planetCompassCelestialTemperatures(body: PlanetCompassCelestialBody): List<PlanetCompassCelestialTemperature> {
    fun value(kind: PlanetCompassCelestialTemperatureKind, celsius: Double) =
        PlanetCompassCelestialTemperature(kind, celsius)
    fun mean(celsius: Double) = listOf(value(PlanetCompassCelestialTemperatureKind.SURFACE_MEAN, celsius))
    fun atmosphere(celsius: Double) = listOf(value(PlanetCompassCelestialTemperatureKind.ATMOSPHERE_ONE_BAR, celsius))
    fun dayNight(day: Double, night: Double) = listOf(
        value(PlanetCompassCelestialTemperatureKind.DAY_MAXIMUM, day),
        value(PlanetCompassCelestialTemperatureKind.NIGHT_MINIMUM, night))
    return when (body) {
        PlanetCompassCelestialBody.SUN -> listOf(value(PlanetCompassCelestialTemperatureKind.PHOTOSPHERE, 5_500.0))
        PlanetCompassCelestialBody.MERCURY -> dayNight(430.0, -180.0)
        PlanetCompassCelestialBody.VENUS -> mean(464.0)
        PlanetCompassCelestialBody.MOON -> dayNight(127.0, -173.0)
        PlanetCompassCelestialBody.MARS -> mean(-65.0) +
            PlanetCompassCelestialTemperature(PlanetCompassCelestialTemperatureKind.SURFACE_RANGE, -153.0, 20.0)
        PlanetCompassCelestialBody.JUPITER -> atmosphere(-110.0)
        PlanetCompassCelestialBody.SATURN -> atmosphere(-140.0)
        PlanetCompassCelestialBody.URANUS -> atmosphere(-195.0)
        PlanetCompassCelestialBody.NEPTUNE -> atmosphere(-200.0)
        PlanetCompassCelestialBody.IO -> mean(-155.0)
        PlanetCompassCelestialBody.EUROPA -> listOf(
            PlanetCompassCelestialTemperature(PlanetCompassCelestialTemperatureKind.SURFACE_RANGE, -223.0, -133.0))
        PlanetCompassCelestialBody.PLUTO -> mean(-225.0)
        // Polaris Aa's mean effective temperature in the cited 2015 spectra, not the whole system.
        PlanetCompassCelestialBody.POLARIS -> listOf(value(PlanetCompassCelestialTemperatureKind.STELLAR_EFFECTIVE, 6_017.0 - 273.15))
        // No defensible single current hull temperature, nor a measured Sedna surface temperature.
        PlanetCompassCelestialBody.SEDNA, PlanetCompassCelestialBody.ISS, PlanetCompassCelestialBody.STARLINK_V3,
        PlanetCompassCelestialBody.VOYAGER_1, PlanetCompassCelestialBody.VOYAGER_2 -> emptyList()
    }
}

internal fun formatPlanetCompassCelestialTemperature(value: PlanetCompassCelestialTemperature,
    numeric: PlanetCompassNumericFormat): String {
    val maximum = value.maximumCelsius
    if (!value.celsius.isFinite() || value.celsius < -273.15 ||
        (maximum != null && (!maximum.isFinite() || maximum < value.celsius))) return "—"
    fun number(celsius: Double) = formatPlanetCompassNumber(celsius, 0, numeric)
    // Effective stellar temperatures are conventionally reported in kelvin; no degree symbol.
    if (value.kind == PlanetCompassCelestialTemperatureKind.STELLAR_EFFECTIVE)
        return "≈${number(value.celsius + 273.15)} K"
    return if (maximum == null) "≈${number(value.celsius)} °C"
        else "≈${number(value.celsius)} … ${number(maximum)} °C"
}
