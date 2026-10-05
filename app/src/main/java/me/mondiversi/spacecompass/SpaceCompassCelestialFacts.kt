package me.mondiversi.spacecompass

/** BitmapFactory.Options starts at zero, which is unsuitable for sample-size arithmetic. */
internal fun spaceCompassCelestialTextureSampleSize(width: Int, maximumDecodeWidth: Int = 2048): Int {
    var sample = 1
    while (width / sample > maximumDecodeWidth.coerceAtLeast(1)) sample *= 2
    return sample
}

/** Reference values, not telemetry. Sources and qualifications: docs/CELESTIAL_VIEWER.md. */
internal data class SpaceCompassCelestialFacts(
    val diameterKm: Double? = null, val massKg: Double? = null, val gravity: Double? = null,
    val density: Double? = null, val rotationHours: Double? = null, val revolutionDays: Double? = null,
    val parent: SpaceCompassCelestialBody? = null, val parentIsEarth: Boolean = false,
    val minimumParentKm: Double? = null, val maximumParentKm: Double? = null,
    val dimensionMeters: Double? = null, val diameterErrorKm: Double? = null,
    val massSolar: Double? = null, val massSolarError: Double? = null,
    val massEstimated: Boolean = false, val massModelAssumption: Boolean = false,
    val rotationSeconds: Double? = null, val binaryPeriodDays: Double? = null,
    val diameterEstimated: Boolean = false, val diameterErrorMinusKm: Double? = null,
    val diameterErrorPlusKm: Double? = null, val spectralType: String? = null,
    val luminositySolar: Double? = null, val rotationErrorHours: Double? = null, val parentName: String? = null
)

internal fun spaceCompassCelestialFacts(body: SpaceCompassCelestialBody): SpaceCompassCelestialFacts = when (body) {
    // Agol et al. 2021: reference Earth radius 6371 km; gravity is derived.
    SpaceCompassCelestialBody.TRAPPIST_1_E -> SpaceCompassCelestialFacts(
        diameterKm = 2 * .920 * 6371, diameterEstimated = true,
        diameterErrorMinusKm = 2 * .012 * 6371, diameterErrorPlusKm = 2 * .013 * 6371,
        massKg = .692 * 5.9722e24, massEstimated = true, density = 4900.0,
        gravity = 6.67430e-11 * (.692 * 5.9722e24) / Math.pow(.920 * 6371e3, 2.0),
        revolutionDays = 6.101013, parentName = "TRAPPIST-1")
    SpaceCompassCelestialBody.EARTH_CENTER -> SpaceCompassCelestialFacts()
    SpaceCompassCelestialBody.PROXIMA_CENTAURI -> proximaFacts()
    SpaceCompassCelestialBody.RIGEL -> SpaceCompassCelestialFacts(
        diameterKm = 2 * 78.9 * 695700, diameterErrorKm = 2 * 7.4 * 695700,
        diameterEstimated = true, massSolar = 21.0, massSolarError = 3.0, massEstimated = true,
        gravity = Math.pow(10.0, 1.75) / 100, spectralType = "B8 Ia", luminositySolar = Math.pow(10.0, 5.08))
    SpaceCompassCelestialBody.SUN -> SpaceCompassCelestialFacts(1_391_400.0, 1.9884e30, 274.0, 1408.0, 609.12)
    SpaceCompassCelestialBody.MOON -> SpaceCompassCelestialFacts(3475.0, 7.3e22, 1.6, 3340.0, 655.7, 27.3,
        parentIsEarth = true, minimumParentKm = 363_000.0, maximumParentKm = 406_000.0)
    // Polaris Aa's measured mean radius/mass, not the sum of the triple system.
    SpaceCompassCelestialBody.POLARIS -> polarisFacts()
    // No single stellar surface for a binary system or a galactic nucleus.
    // Total of the two resolved stellar components (A+B), not Proxima.
    SpaceCompassCelestialBody.ALPHA_CENTAURI -> SpaceCompassCelestialFacts(massSolar = 1.0788 + 0.9092, binaryPeriodDays = 79.762 * 365.25, spectralType = "A: G2 V; B: K1 V")
    // No reliable present-day mass found; initial mass or mass-loss rate is not current mass.
    SpaceCompassCelestialBody.STEPHENSON_2_18 -> SpaceCompassCelestialFacts(spectralType = "M6 I")
    SpaceCompassCelestialBody.SAGITTARIUS_A -> SpaceCompassCelestialFacts(massSolar = 4.297e6, massEstimated = true)
    SpaceCompassCelestialBody.ANDROMEDA_CORE -> SpaceCompassCelestialFacts(massSolar = 1.4e8, massEstimated = true)
    SpaceCompassCelestialBody.TON_618 -> SpaceCompassCelestialFacts(massSolar = 6.6e10, massEstimated = true)
    SpaceCompassCelestialBody.PSR_J0437 -> SpaceCompassCelestialFacts(massSolar = 1.418, massSolarError = 0.044,
        diameterKm = 22.72, diameterEstimated = true, diameterErrorMinusKm = 1.26, diameterErrorPlusKm = 1.90,
        rotationSeconds = 0.0057574519367, binaryPeriodDays = 5.741048)
    // This is a cited modelling assumption, not a measured mass of RX J1856.
    SpaceCompassCelestialBody.RX_J1856 -> SpaceCompassCelestialFacts(massSolar = 1.4, massModelAssumption = true, rotationSeconds = 7.055)
    SpaceCompassCelestialBody.IO -> jovianFacts(1821.49, 5959.91547, 3527.6, 203.4889538, 421800.0, 0.004)
    SpaceCompassCelestialBody.EUROPA -> jovianFacts(1560.80, 3202.71210, 3013.0, 101.3747235, 671100.0, 0.009)
    SpaceCompassCelestialBody.MERCURY -> planet(4879.0, 3.30e23, 3.7, 5429.0, 1407.6, 88.0, 46.0, 69.8)
    SpaceCompassCelestialBody.VENUS -> planet(12104.0, 4.87e24, 8.9, 5243.0, -5832.5, 224.7, 107.5, 108.9)
    SpaceCompassCelestialBody.MARS -> planet(6792.0, 6.42e23, 3.7, 3934.0, 24.6, 687.0, 206.7, 249.3)
    SpaceCompassCelestialBody.JUPITER -> planet(142984.0, 1.898e27, 23.1, 1326.0, 9.9, 4331.0, 740.6, 816.4)
    SpaceCompassCelestialBody.SATURN -> planet(120536.0, 5.68e26, 9.0, 687.0, 10.7, 10747.0, 1357.6, 1506.5)
    SpaceCompassCelestialBody.URANUS -> planet(51118.0, 8.68e25, 8.7, 1270.0, -17.2, 30589.0, 2732.7, 3001.4)
    SpaceCompassCelestialBody.NEPTUNE -> planet(49528.0, 1.02e26, 11.0, 1638.0, 16.1, 59800.0, 4471.1, 4558.9)
    SpaceCompassCelestialBody.PLUTO -> planet(2376.0, 1.30e22, 0.7, 1850.0, -153.3, 90560.0, 4436.8, 7375.9)
    SpaceCompassCelestialBody.SEDNA -> SpaceCompassCelestialFacts(diameterKm = 995.0, diameterErrorKm = 80.0,
        rotationHours = 10.273, revolutionDays = 4_630_000.0, parent = SpaceCompassCelestialBody.SUN,
        minimumParentKm = 76.2 * SPACE_COMPASS_AU_KM, maximumParentKm = 1010 * SPACE_COMPASS_AU_KM)
    // There is no planetary surface gravity or axial rotation period for these spacecraft.
    // ISS orbit bounds/period change with manoeuvres, so static planetary rows remain unavailable.
    SpaceCompassCelestialBody.ISS -> SpaceCompassCelestialFacts(massKg = 419_725.0, dimensionMeters = 109.0, parentIsEarth = true)
    // No authoritative current mass, dimensions or attitude for this specific Starlink spacecraft.
    SpaceCompassCelestialBody.STARLINK_V3 -> SpaceCompassCelestialFacts(parentIsEarth = true)
    // Do not present launch mass as the current mass after decades of propellant use.
    SpaceCompassCelestialBody.VOYAGER_1 -> SpaceCompassCelestialFacts(dimensionMeters = 3.7, massKg = 733.0, massEstimated = true)
    SpaceCompassCelestialBody.VOYAGER_2 -> SpaceCompassCelestialFacts(dimensionMeters = 3.7, massKg = 735.0, massEstimated = true)
}

private fun polarisFacts(): SpaceCompassCelestialFacts {
    val radiusMeters = 46.27 * 695_700 * 1_000
    val massKg = 5.13 * 1.9884e30
    // Spherical Newtonian estimates from the measured mean radius/mass, not live stellar telemetry.
    return SpaceCompassCelestialFacts(diameterKm = 2 * radiusMeters / 1_000,
        diameterErrorKm = 2 * 0.42 * 695_700, massKg = massKg,
        gravity = 6.67430e-11 * massKg / (radiusMeters * radiusMeters),
        density = 3 * massKg / (4 * Math.PI * radiusMeters * radiusMeters * radiusMeters),
        massSolarError = 0.28, binaryPeriodDays = 29.416 * 365.25, spectralType = "F7 Ib")
}

private fun planet(diameter: Double, mass: Double, gravity: Double, density: Double,
    rotation: Double, revolution: Double, minimum: Double, maximum: Double) =
    SpaceCompassCelestialFacts(diameter, mass, gravity, density, rotation, revolution, SpaceCompassCelestialBody.SUN,
        minimumParentKm = minimum * 1e6, maximumParentKm = maximum * 1e6)

private fun jovianFacts(radiusKm: Double, gmKm3S2: Double, density: Double, spinDegreesDay: Double,
    semimajorKm: Double, eccentricity: Double) = SpaceCompassCelestialFacts(diameterKm = radiusKm*2,
    diameterErrorKm = if (radiusKm > 1800) 1.0 else 0.6,
    massKg = gmKm3S2*1e9/6.67430e-11, gravity = gmKm3S2/(radiusKm*radiusKm)*1000,
    density = density, rotationHours = 360/spinDegreesDay*24, revolutionDays = 360/spinDegreesDay,
    parent = SpaceCompassCelestialBody.JUPITER, minimumParentKm = semimajorKm*(1-eccentricity),
    maximumParentKm = semimajorKm*(1+eccentricity))

/** Standard Earth gravity, not a live local acceleration or a new reference measurement. */
internal const val SPACE_COMPASS_STANDARD_GRAVITY_M_S2 = 9.80665

internal fun formatSpaceCompassCelestialGravity(value: Double?, numeric: SpaceCompassNumericFormat,
    fractionDigits: Int = 1, feet: Boolean = false): String {
    if (value == null || !value.isFinite() || value <= 0) return "—"
    val acceleration = formatSpaceCompassNumber(if (feet) value / 0.3048 else value, fractionDigits, numeric)
    val earthG = formatSpaceCompassNumber(value / SPACE_COMPASS_STANDARD_GRAVITY_M_S2, 3, numeric, minimumDigits = 0)
    return "$acceleration ${if (feet) "ft/s²" else "m/s²"} ($earthG g)"
}

internal val SpaceCompassCelestialBody.viewerTexture: String?
    get() = when (this) {
        SpaceCompassCelestialBody.EARTH_CENTER, SpaceCompassCelestialBody.TRAPPIST_1_E -> null
        SpaceCompassCelestialBody.PROXIMA_CENTAURI, SpaceCompassCelestialBody.RIGEL,
        SpaceCompassCelestialBody.ALPHA_CENTAURI, SpaceCompassCelestialBody.SAGITTARIUS_A,
        SpaceCompassCelestialBody.STEPHENSON_2_18, SpaceCompassCelestialBody.RX_J1856,
    SpaceCompassCelestialBody.PSR_J0437, SpaceCompassCelestialBody.TON_618, SpaceCompassCelestialBody.ANDROMEDA_CORE, SpaceCompassCelestialBody.POLARIS, SpaceCompassCelestialBody.SEDNA, SpaceCompassCelestialBody.ISS, SpaceCompassCelestialBody.STARLINK_V3, SpaceCompassCelestialBody.VOYAGER_1, SpaceCompassCelestialBody.VOYAGER_2 -> null
        SpaceCompassCelestialBody.VENUS -> "venus_atmosphere.jpg"
        else -> "${name.lowercase(java.util.Locale.ROOT)}.jpg"
    }
internal val SpaceCompassCelestialBody.isSpacecraft: Boolean get() = isEarthSatellite || isVoyager

/** Raster X increases eastward even for ISIS PositiveWest coordinate labels.
 * Europa/Pluto span 0..360; the other reference maps are centred on longitude zero. */
internal val SpaceCompassCelestialBody.textureLongitudeOffset: Double
    get() = if (this == SpaceCompassCelestialBody.PLUTO || this == SpaceCompassCelestialBody.EUROPA) 0.0 else 0.5
internal val SpaceCompassCelestialBody.textureHasUnmappedAreas: Boolean
    get() = this == SpaceCompassCelestialBody.PLUTO || this == SpaceCompassCelestialBody.EUROPA

internal fun formatSpaceCompassScientificNumber(value: Double, numeric: SpaceCompassNumericFormat): String {
    if (!value.isFinite() || value <= 0) return "—"
    if (value < 1e6) return formatSpaceCompassNumber(value, 0, numeric)
    val exponent = kotlin.math.floor(kotlin.math.log10(value)).toInt()
    val power = exponent.toString().map { "⁰¹²³⁴⁵⁶⁷⁸⁹"[it.digitToInt()] }.joinToString("")
    return "${formatSpaceCompassNumber(value / Math.pow(10.0, exponent.toDouble()), 3, numeric)} × 10$power"
}

internal const val SPACE_COMPASS_SOLAR_MASS_KG = 1.9884e30
/** Pulse periods use seconds/ms so fast rotators never round to zero hours. */
internal fun formatSpaceCompassRotationPeriod(facts: SpaceCompassCelestialFacts, numeric: SpaceCompassNumericFormat): String {
    facts.rotationSeconds?.let {
        if (!it.isFinite() || it <= 0) return "—"
        return if (it < 1) "${formatSpaceCompassNumber(it * 1000, 6, numeric, minimumDigits = 0)} ms"
            else "${formatSpaceCompassNumber(it, 3, numeric, minimumDigits = 0)} s"
    }
    return facts.rotationHours?.let { "${formatSpaceCompassNumber(kotlin.math.abs(it), 2, numeric)}" +
        (facts.rotationErrorHours?.let { error -> " ± ${formatSpaceCompassNumber(error, 2, numeric)}" } ?: "") + " h" } ?: "—"
}

/** Ordinary stellar components are kept separate instead of inventing one binary diameter. */
internal data class SpaceCompassStellarComponent(val name: String, val massSolar: Double,
    val massError: Double, val radiusSolar: Double, val radiusError: Double, val luminositySolar: Double)
internal fun spaceCompassStellarComponents(body: SpaceCompassCelestialBody): List<SpaceCompassStellarComponent> =
    if (body == SpaceCompassCelestialBody.ALPHA_CENTAURI) listOf(
        SpaceCompassStellarComponent("A", 1.0788, 0.0029, 1.2175, 0.0055, 1.5059),
        SpaceCompassStellarComponent("B", 0.9092, 0.0025, 0.8591, 0.0036, 0.4981)) else emptyList()

/** Schwarzschild-equivalent horizon scale: a nonrotating model, never a measured luminous diameter. */
internal fun spaceCompassHorizonDiameterKm(body: SpaceCompassCelestialBody): Double? =
    if (body in setOf(SpaceCompassCelestialBody.SAGITTARIUS_A, SpaceCompassCelestialBody.ANDROMEDA_CORE,
        SpaceCompassCelestialBody.TON_618)) spaceCompassCelestialFacts(body).massSolar?.let {
        4 * 6.67430e-11 * it * SPACE_COMPASS_SOLAR_MASS_KG / (299792458.0 * 299792458.0) / 1000
    } else null

/** Stefan-Boltzmann estimate from published luminosity/radius and IAU nominal solar Teff. */
internal fun spaceCompassStellarComponentTemperature(component: SpaceCompassStellarComponent): Double =
    5772.0 * Math.pow(component.luminositySolar / (component.radiusSolar * component.radiusSolar), 0.25) - 273.15

private fun proximaFacts(): SpaceCompassCelestialFacts {
    // Faria et al. 2022 reference values; gravity/density are spherical Newtonian estimates.
    val radius = 0.141 * 695700000
    val mass = 0.1221 * SPACE_COMPASS_SOLAR_MASS_KG
    return SpaceCompassCelestialFacts(diameterKm = 2 * radius / 1000, diameterErrorKm = 2 * 0.021 * 695700,
        massSolar = 0.1221, massSolarError = 0.0022, massEstimated = true, diameterEstimated = true,
        gravity = 6.67430e-11 * mass / (radius * radius), density = 3 * mass / (4 * Math.PI * radius * radius * radius),
        rotationHours = 90.0 * 24, rotationErrorHours = 4.0 * 24, spectralType = "M5.5 V", luminositySolar = 0.0016)
}
