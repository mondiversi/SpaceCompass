package me.mondiversi.planetcompass

/** Android getRotationMatrix's rows are east/north/up; columns are device screen axes.
 * The rear-camera axis is minus screen Z. Apply magnetic declination once to all three axes.
 */
internal fun planetCompassSunOrientationFromScreenMatrix(
    matrix: FloatArray, declinationDegrees: Double
): PlanetCompassSunOrientation? {
    if (matrix.size != 9 || matrix.any { !it.isFinite() } || !declinationDegrees.isFinite()) return null
    fun axis(column: Int, sign: Double = 1.0) = PlanetCompassSunVector(
        matrix[column] * sign, matrix[3 + column] * sign, matrix[6 + column] * sign
    ).trueNorth(declinationDegrees)
    return PlanetCompassSunOrientation(axis(0), axis(1), axis(2, -1.0))
}

/** Keep tilt/horizon available, but do not manufacture a solar bearing from untrusted north. */
internal fun planetCompassSunTrustedPointingOrientation(
    orientation: PlanetCompassSunOrientation?, reliable: Boolean
): PlanetCompassSunOrientation? = orientation.takeIf { reliable }
