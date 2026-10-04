package me.mondiversi.planetcompass

import io.github.cosinekitty.astronomy.*
import kotlin.math.*

/** Astronomy Engine jovicentric EQJ states; no GPS or network required. */
internal fun planetCompassJovianMoonState(body: PlanetCompassCelestialBody, time: Time): StateVector {
    require(body.isJovianMoon)
    val moons = jupiterMoons(time)
    return if (body == PlanetCompassCelestialBody.IO) moons.io else moons.europa
}

/** Follow the engine's Jupiter-moons demo: moons are evaluated at light emission time. */
internal fun planetCompassJovianMoonGeoVector(body: PlanetCompassCelestialBody, time: Time, aberration: Aberration): Vector {
    val jupiter = geoVector(Body.Jupiter, time, aberration)
    val moon = planetCompassJovianMoonState(body, time.addDays(-jupiter.length() / C_AUDAY))
    return Vector(jupiter.x + moon.x, jupiter.y + moon.y, jupiter.z + moon.z, time)
}

internal fun planetCompassJovianMoonHelioVector(body: PlanetCompassCelestialBody, time: Time): Vector {
    val jupiter = helioVector(Body.Jupiter, time)
    val moon = planetCompassJovianMoonState(body, time)
    return Vector(jupiter.x + moon.x, jupiter.y + moon.y, jupiter.z + moon.z, time)
}

/** IAU orientation coefficients from NASA/NAIF pck00011.tpc, including J3..J7 nutation. */
internal fun planetCompassJovianMoonAxis(body: PlanetCompassCelestialBody, time: Time): AxisInfo {
    require(body.isJovianMoon)
    val d = time.tt; val t = d / 36525.0
    val j3 = Math.toRadians(283.90 + 4850.7*t)
    val j4 = Math.toRadians(355.80 + 1191.3*t)
    val j5 = Math.toRadians(119.90 + 262.1*t)
    val j6 = Math.toRadians(229.80 + 64.3*t)
    val j7 = Math.toRadians(352.25 + 2382.6*t)
    val ra: Double; val dec: Double; val spin: Double
    if (body == PlanetCompassCelestialBody.IO) {
        ra = 268.05 - 0.009*t + 0.094*sin(j3) + 0.024*sin(j4)
        dec = 64.50 + 0.003*t + 0.040*cos(j3) + 0.011*cos(j4)
        spin = 200.39 + 203.4889538*d - 0.085*sin(j3) - 0.022*sin(j4)
    } else {
        ra = 268.08 - 0.009*t + 1.086*sin(j4) + 0.060*sin(j5) + 0.015*sin(j6) + 0.009*sin(j7)
        dec = 64.51 + 0.003*t + 0.468*cos(j4) + 0.026*cos(j5) + 0.007*cos(j6) + 0.002*cos(j7)
        spin = 36.022 + 101.3747235*d - 0.980*sin(j4) - 0.054*sin(j5) - 0.014*sin(j6) - 0.008*sin(j7)
    }
    val r = Math.toRadians(ra); val c = Math.toRadians(dec)
    return AxisInfo(ra/15.0, dec, spin, Vector(cos(c)*cos(r), cos(c)*sin(r), sin(c), time))
}
