package me.mondiversi.spacecompass

/** Android getRotationMatrix's rows are east/north/up; columns are device screen axes.
 * The rear-camera axis is minus screen Z. Apply magnetic declination once to all three axes.
 */
internal fun spaceCompassSunOrientationFromScreenMatrix(
    matrix: FloatArray, declinationDegrees: Double
): SpaceCompassSunOrientation? {
    if (matrix.size != 9 || matrix.any { !it.isFinite() } || !declinationDegrees.isFinite()) return null
    fun axis(column: Int, sign: Double = 1.0) = SpaceCompassSunVector(
        matrix[column] * sign, matrix[3 + column] * sign, matrix[6 + column] * sign
    ).trueNorth(declinationDegrees)
    return SpaceCompassSunOrientation(axis(0), axis(1), axis(2, -1.0))
}

/** Keep tilt/horizon available, but do not manufacture a solar bearing from untrusted north. */
internal fun spaceCompassSunTrustedPointingOrientation(
    orientation: SpaceCompassSunOrientation?, reliable: Boolean
): SpaceCompassSunOrientation? = orientation.takeIf { reliable }
