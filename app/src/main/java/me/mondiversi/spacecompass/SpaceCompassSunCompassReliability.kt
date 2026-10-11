package me.mondiversi.spacecompass

internal const val SPACE_COMPASS_SUN_COMPASS_RECOVERY_NS = 500_000_000L
internal const val SPACE_COMPASS_SUN_SENSOR_MAX_AGE_NS = 500_000_000L
internal const val SPACE_COMPASS_SUN_MAGNETIC_ANOMALY_CONFIRM_NS = 2_000_000_000L
internal enum class SpaceCompassSunCompassIssue {
    NONE, REDUCED_ACCURACY, CALIBRATION, MAGNETIC_INTERFERENCE, WAITING
}

/** A magnetic anomaly is distinct from calibration and incomplete or recovering samples. */
internal fun spaceCompassSunCompassIssue(
    magneticAccuracy: Int, fieldMicrotesla: Double, expectedFieldMicrotesla: Double?,
    readingsFresh: Boolean, recovered: Boolean
): SpaceCompassSunCompassIssue {
    if (!readingsFresh || !fieldMicrotesla.isFinite() ||
        expectedFieldMicrotesla != null && (!expectedFieldMicrotesla.isFinite() || expectedFieldMicrotesla <= 0))
        return SpaceCompassSunCompassIssue.WAITING
    if (!spaceCompassSunMagneticFieldPlausible(fieldMicrotesla, expectedFieldMicrotesla))
        return SpaceCompassSunCompassIssue.MAGNETIC_INTERFERENCE
    if (magneticAccuracy < 1) return SpaceCompassSunCompassIssue.CALIBRATION
    if (!recovered) return SpaceCompassSunCompassIssue.WAITING
    return if (magneticAccuracy < 2) SpaceCompassSunCompassIssue.REDUCED_ACCURACY else SpaceCompassSunCompassIssue.NONE
}

private fun spaceCompassSunMagneticFieldPlausible(fieldMicrotesla: Double, expectedFieldMicrotesla: Double?): Boolean =
    fieldMicrotesla.isFinite() && fieldMicrotesla in 10.0..100.0 &&
        (expectedFieldMicrotesla == null || expectedFieldMicrotesla.isFinite() && expectedFieldMicrotesla > 0 &&
            kotlin.math.abs(fieldMicrotesla / expectedFieldMicrotesla - 1) <= 0.35)

/** North is established by calibrated magnetic readings, never by an unrelated fused yaw. */
internal fun spaceCompassSunMagneticReferenceReliable(
    magneticAccuracy: Int, fieldMicrotesla: Double, expectedFieldMicrotesla: Double?
): Boolean {
    return magneticAccuracy >= 2 && spaceCompassSunMagneticReferenceUsable(magneticAccuracy, fieldMicrotesla, expectedFieldMicrotesla)
}

/** Low calibration accuracy permits approximate pointing only when the field is plausible. */
internal fun spaceCompassSunMagneticReferenceUsable(
    magneticAccuracy: Int, fieldMicrotesla: Double, expectedFieldMicrotesla: Double?
): Boolean {
    return magneticAccuracy >= 1 && spaceCompassSunMagneticFieldPlausible(fieldMicrotesla, expectedFieldMicrotesla)
}

/** Fail immediately; recover only after sustained agreement to avoid flickering during calibration. */
internal class SpaceCompassSunCompassRecovery {
    private var since: Long? = null
    fun reset() { since = null }
    fun update(consistent: Boolean, timestampNanos: Long): Boolean {
        if (!consistent || timestampNanos <= 0) { reset(); return false }
        val previous = since
        if (previous == null || timestampNanos < previous) { since = timestampNanos; return false }
        return timestampNanos - previous >= SPACE_COMPASS_SUN_COMPASS_RECOVERY_NS
    }
}

/** Debounce the warning only; an implausible field invalidates the bearing immediately. */
internal class SpaceCompassSunCompassIssueStabilizer {
    private var anomalySince: Long? = null
    private var lastSample: Long? = null

    fun reset() { anomalySince = null; lastSample = null }

    fun update(issue: SpaceCompassSunCompassIssue, magneticTimestampNanos: Long): SpaceCompassSunCompassIssue {
        if (issue != SpaceCompassSunCompassIssue.MAGNETIC_INTERFERENCE) {
            reset()
            return issue
        }
        if (magneticTimestampNanos <= 0) {
            reset()
            return SpaceCompassSunCompassIssue.WAITING
        }
        val previous = lastSample
        if (previous == null || magneticTimestampNanos < previous ||
            magneticTimestampNanos - previous > SPACE_COMPASS_SUN_SENSOR_MAX_AGE_NS)
            anomalySince = magneticTimestampNanos
        lastSample = magneticTimestampNanos
        // Gravity events and accuracy callbacks cannot advance an unchanged magnetic sample.
        return if (magneticTimestampNanos - requireNotNull(anomalySince) >= SPACE_COMPASS_SUN_MAGNETIC_ANOMALY_CONFIRM_NS)
            SpaceCompassSunCompassIssue.MAGNETIC_INTERFERENCE else SpaceCompassSunCompassIssue.WAITING
    }
}
