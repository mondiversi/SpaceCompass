package me.mondiversi.planetcompass

/** BitmapFactory.Options starts at zero, which is unsuitable for sample-size arithmetic. */
internal fun planetCompassCelestialTextureSampleSize(width: Int, maximumDecodeWidth: Int = 2048): Int {
    var sample = 1
    while (width / sample > maximumDecodeWidth.coerceAtLeast(1)) sample *= 2
    return sample
}

/** Reference values, not telemetry. Sources and qualifications: docs/CELESTIAL_VIEWER.md. */
internal data class PlanetCompassCelestialFacts(
    val diameterKm: Double? = null, val massKg: Double? = null, val gravity: Double? = null,
    val density: Double? = null, val rotationHours: Double? = null, val revolutionDays: Double? = null,
    val parent: PlanetCompassCelestialBody? = null, val parentIsEarth: Boolean = false,
    val minimumParentKm: Double? = null, val maximumParentKm: Double? = null,
    val dimensionMeters: Double? = null, val diameterErrorKm: Double? = null
)

internal fun planetCompassCelestialFacts(body: PlanetCompassCelestialBody): PlanetCompassCelestialFacts = when (body) {
    PlanetCompassCelestialBody.SUN -> PlanetCompassCelestialFacts(1_391_400.0, 1.9884e30, 274.0, 1408.0, 609.12)
    PlanetCompassCelestialBody.MOON -> PlanetCompassCelestialFacts(3475.0, 7.3e22, 1.6, 3340.0, 655.7, 27.3,
        parentIsEarth = true, minimumParentKm = 363_000.0, maximumParentKm = 406_000.0)
    // Polaris Aa's measured mean radius/mass, not the sum of the triple system.
    PlanetCompassCelestialBody.POLARIS -> polarisFacts()
    PlanetCompassCelestialBody.IO -> jovianFacts(1821.49, 5959.91547, 3527.6, 203.4889538, 421800.0, 0.004)
    PlanetCompassCelestialBody.EUROPA -> jovianFacts(1560.80, 3202.71210, 3013.0, 101.3747235, 671100.0, 0.009)
    PlanetCompassCelestialBody.MERCURY -> planet(4879.0, 3.30e23, 3.7, 5429.0, 1407.6, 88.0, 46.0, 69.8)
    PlanetCompassCelestialBody.VENUS -> planet(12104.0, 4.87e24, 8.9, 5243.0, -5832.5, 224.7, 107.5, 108.9)
    PlanetCompassCelestialBody.MARS -> planet(6792.0, 6.42e23, 3.7, 3934.0, 24.6, 687.0, 206.7, 249.3)
    PlanetCompassCelestialBody.JUPITER -> planet(142984.0, 1.898e27, 23.1, 1326.0, 9.9, 4331.0, 740.6, 816.4)
    PlanetCompassCelestialBody.SATURN -> planet(120536.0, 5.68e26, 9.0, 687.0, 10.7, 10747.0, 1357.6, 1506.5)
    PlanetCompassCelestialBody.URANUS -> planet(51118.0, 8.68e25, 8.7, 1270.0, -17.2, 30589.0, 2732.7, 3001.4)
    PlanetCompassCelestialBody.NEPTUNE -> planet(49528.0, 1.02e26, 11.0, 1638.0, 16.1, 59800.0, 4471.1, 4558.9)
    PlanetCompassCelestialBody.PLUTO -> planet(2376.0, 1.30e22, 0.7, 1850.0, -153.3, 90560.0, 4436.8, 7375.9)
    PlanetCompassCelestialBody.SEDNA -> PlanetCompassCelestialFacts(diameterKm = 995.0, diameterErrorKm = 80.0,
        rotationHours = 10.273, revolutionDays = 4_630_000.0, parent = PlanetCompassCelestialBody.SUN,
        minimumParentKm = 76.2 * PLANET_COMPASS_AU_KM, maximumParentKm = 1010 * PLANET_COMPASS_AU_KM)
    // There is no planetary surface gravity or axial rotation period for these spacecraft.
    // ISS orbit bounds/period change with manoeuvres, so static planetary rows remain unavailable.
    PlanetCompassCelestialBody.ISS -> PlanetCompassCelestialFacts(massKg = 419_725.0, dimensionMeters = 109.0, parentIsEarth = true)
    // No authoritative current mass, dimensions or attitude for this specific Starlink spacecraft.
    PlanetCompassCelestialBody.STARLINK_V3 -> PlanetCompassCelestialFacts(parentIsEarth = true)
    // Do not present launch mass as the current mass after decades of propellant use.
    PlanetCompassCelestialBody.VOYAGER_1, PlanetCompassCelestialBody.VOYAGER_2 -> PlanetCompassCelestialFacts(dimensionMeters = 3.7)
}

private fun polarisFacts(): PlanetCompassCelestialFacts {
    val radiusMeters = 46.27 * 695_700 * 1_000
    val massKg = 5.13 * 1.9884e30
    // Spherical Newtonian estimates from the measured mean radius/mass, not live stellar telemetry.
    return PlanetCompassCelestialFacts(diameterKm = 2 * radiusMeters / 1_000,
        diameterErrorKm = 2 * 0.42 * 695_700, massKg = massKg,
        gravity = 6.67430e-11 * massKg / (radiusMeters * radiusMeters),
        density = 3 * massKg / (4 * Math.PI * radiusMeters * radiusMeters * radiusMeters))
}

private fun planet(diameter: Double, mass: Double, gravity: Double, density: Double,
    rotation: Double, revolution: Double, minimum: Double, maximum: Double) =
    PlanetCompassCelestialFacts(diameter, mass, gravity, density, rotation, revolution, PlanetCompassCelestialBody.SUN,
        minimumParentKm = minimum * 1e6, maximumParentKm = maximum * 1e6)

private fun jovianFacts(radiusKm: Double, gmKm3S2: Double, density: Double, spinDegreesDay: Double,
    semimajorKm: Double, eccentricity: Double) = PlanetCompassCelestialFacts(diameterKm = radiusKm*2,
    massKg = gmKm3S2*1e9/6.67430e-11, gravity = gmKm3S2/(radiusKm*radiusKm)*1000,
    density = density, rotationHours = 360/spinDegreesDay*24, revolutionDays = 360/spinDegreesDay,
    parent = PlanetCompassCelestialBody.JUPITER, minimumParentKm = semimajorKm*(1-eccentricity),
    maximumParentKm = semimajorKm*(1+eccentricity))

/** Standard Earth gravity, not a live local acceleration or a new reference measurement. */
internal const val PLANET_COMPASS_STANDARD_GRAVITY_M_S2 = 9.80665

internal fun formatPlanetCompassCelestialGravity(value: Double?, numeric: PlanetCompassNumericFormat,
    siFractionDigits: Int = 1): String {
    if (value == null || !value.isFinite() || value <= 0) return "—"
    val si = formatPlanetCompassNumber(value, siFractionDigits, numeric)
    val earthG = formatPlanetCompassNumber(value / PLANET_COMPASS_STANDARD_GRAVITY_M_S2, 3, numeric, minimumDigits = 0)
    return "$si m/s² ($earthG g)"
}

internal val PlanetCompassCelestialBody.viewerTexture: String?
    get() = when (this) {
        PlanetCompassCelestialBody.POLARIS, PlanetCompassCelestialBody.SEDNA, PlanetCompassCelestialBody.ISS, PlanetCompassCelestialBody.STARLINK_V3, PlanetCompassCelestialBody.VOYAGER_1, PlanetCompassCelestialBody.VOYAGER_2 -> null
        PlanetCompassCelestialBody.VENUS -> "venus_atmosphere.jpg"
        else -> "${name.lowercase(java.util.Locale.ROOT)}.jpg"
    }
internal val PlanetCompassCelestialBody.isSpacecraft: Boolean get() = isEarthSatellite || isVoyager

/** Raster X increases eastward even for ISIS PositiveWest coordinate labels.
 * Europa/Pluto span 0..360; the other reference maps are centred on longitude zero. */
internal val PlanetCompassCelestialBody.textureLongitudeOffset: Double
    get() = if (this == PlanetCompassCelestialBody.PLUTO || this == PlanetCompassCelestialBody.EUROPA) 0.0 else 0.5
internal val PlanetCompassCelestialBody.textureHasUnmappedAreas: Boolean
    get() = this == PlanetCompassCelestialBody.PLUTO || this == PlanetCompassCelestialBody.EUROPA

internal fun formatPlanetCompassScientificNumber(value: Double, numeric: PlanetCompassNumericFormat): String {
    if (!value.isFinite() || value <= 0) return "—"
    if (value < 1e6) return formatPlanetCompassNumber(value, 0, numeric)
    val exponent = kotlin.math.floor(kotlin.math.log10(value)).toInt()
    val power = exponent.toString().map { "⁰¹²³⁴⁵⁶⁷⁸⁹"[it.digitToInt()] }.joinToString("")
    return "${formatPlanetCompassNumber(value / Math.pow(10.0, exponent.toDouble()), 3, numeric)} × 10$power"
}
