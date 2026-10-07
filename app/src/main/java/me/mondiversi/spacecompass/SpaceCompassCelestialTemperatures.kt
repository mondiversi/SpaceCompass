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
    STELLAR_EFFECTIVE(R.string.celestial_temperature_effective),
    SURFACE_ESTIMATE(R.string.celestial_temperature_surface_estimate),
    EQUILIBRIUM_MODEL(R.string.celestial_temperature_equilibrium_model),
    HISTORICAL_SURFACE_RANGE(R.string.celestial_temperature_historical_range),
    THERMAL_MODEL(R.string.celestial_temperature_thermal_model)
}

internal data class SpaceCompassCelestialTemperature(
    val kind: SpaceCompassCelestialTemperatureKind,
    val celsius: Double,
    val maximumCelsius: Double? = null,
    val component: String? = null,
    val epochYear: Int? = null,
    val uncertaintyCelsius: Double? = null
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
        // Licensed ALIEN RPG reference: terraformed Hadley's Hope era, not present-day weather.
        SpaceCompassCelestialBody.LV_426 -> mean(10.0)
        SpaceCompassCelestialBody.EARTH_CENTER -> emptyList()
        // Gillon et al. 2017, Table 1: zero Bond albedo; not an observed surface temperature.
        SpaceCompassCelestialBody.TRAPPIST_1_E -> listOf(SpaceCompassCelestialTemperature(
            SpaceCompassCelestialTemperatureKind.EQUILIBRIUM_MODEL, 251.3 - 273.15,
            uncertaintyCelsius = 4.9))
        // Brown's discovery-era estimate at about 90 AU, not current surface telemetry.
        SpaceCompassCelestialBody.SEDNA -> listOf(SpaceCompassCelestialTemperature(
            SpaceCompassCelestialTemperatureKind.SURFACE_ESTIMATE, -240.0, epochYear = 2004))
        // Historical nucleus observations near each mission encounter; the epoch stays visible.
        SpaceCompassCelestialBody.HALLEY -> listOf(SpaceCompassCelestialTemperature(
            SpaceCompassCelestialTemperatureKind.HISTORICAL_SURFACE_RANGE,
            300.0 - 273.15, 400.0 - 273.15, epochYear = 1986))
        SpaceCompassCelestialBody.COMET_67P -> listOf(SpaceCompassCelestialTemperature(
            SpaceCompassCelestialTemperatureKind.HISTORICAL_SURFACE_RANGE,
            205.0 - 273.15, 230.0 - 273.15, epochYear = 2014))
        SpaceCompassCelestialBody.ALPHA_CENTAURI -> spaceCompassStellarComponents(body).map {
            SpaceCompassCelestialTemperature(SpaceCompassCelestialTemperatureKind.STELLAR_EFFECTIVE,
                spaceCompassStellarComponentTemperature(it), component = it.name)
        }
        // Siebert et al. 2026: adopted stellar temperature in a cluster-distance SED model.
        SpaceCompassCelestialBody.STEPHENSON_2_18 -> listOf(value(
            SpaceCompassCelestialTemperatureKind.STELLAR_EFFECTIVE, 3200.0 - 273.15))
        // Apparent (redshifted) two-blackbody components, not a uniform local surface temperature.
        SpaceCompassCelestialBody.RX_J1856 -> listOf(SpaceCompassCelestialTemperature(
            SpaceCompassCelestialTemperatureKind.THERMAL_MODEL,
            38.9 * 11604.51812155008 - 273.15, 62.4 * 11604.51812155008 - 273.15))
        // Durant et al. 2012: UV bulk-surface thermal-model range, excluding hotter polar caps.
        SpaceCompassCelestialBody.PSR_J0437 -> listOf(SpaceCompassCelestialTemperature(
            SpaceCompassCelestialTemperatureKind.THERMAL_MODEL,
            125000.0 - 273.15, 350000.0 - 273.15))
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
        // A black-hole horizon has no planetary surface; spacecraft hulls have no single reference T.
        SpaceCompassCelestialBody.SAGITTARIUS_A, SpaceCompassCelestialBody.TON_618,
        SpaceCompassCelestialBody.ANDROMEDA_CORE, SpaceCompassCelestialBody.ISS, SpaceCompassCelestialBody.STARLINK_V3,
        SpaceCompassCelestialBody.VOYAGER_1, SpaceCompassCelestialBody.VOYAGER_2 -> emptyList()
    }
}

internal fun formatSpaceCompassCelestialTemperature(value: SpaceCompassCelestialTemperature,
    numeric: SpaceCompassNumericFormat, fahrenheit: Boolean = false): String {
    val maximum = value.maximumCelsius
    if (!value.celsius.isFinite() || value.celsius < -273.15 ||
        (maximum != null && (!maximum.isFinite() || maximum < value.celsius)) ||
        value.uncertaintyCelsius?.let { !it.isFinite() || it < 0 } == true) return "—"
    fun number(celsius: Double) = formatSpaceCompassNumber(celsius, 0, numeric)
    fun temperature(celsius: Double) = number(if (fahrenheit) celsius * 1.8 + 32 else celsius)
    val unit = if (fahrenheit) "°F" else "°C"
    val reference = if (value.kind == SpaceCompassCelestialTemperatureKind.STELLAR_EFFECTIVE)
        " (${number(value.celsius + 273.15)} K)" else ""
    val uncertainty = value.uncertaintyCelsius?.let {
        " ±${number(if (fahrenheit) it * 1.8 else it)}"
    } ?: ""
    val epoch = value.epochYear?.let { " (${formatSpaceCompassNumber(it.toDouble(), 0, numeric, grouping = false)})" } ?: ""
    return (if (maximum == null) "≈${temperature(value.celsius)}$uncertainty $unit$reference"
        else "≈${temperature(value.celsius)} … ${temperature(maximum)} $unit") + epoch
}
