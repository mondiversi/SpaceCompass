package me.mondiversi.spacecompass

internal const val SPACE_COMPASS_SUN_COMPASS_RECOVERY_NS = 500_000_000L
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
    if (magneticAccuracy < 1 || !fieldMicrotesla.isFinite() || fieldMicrotesla !in 10.0..100.0) return false
    if (expectedFieldMicrotesla != null && (!expectedFieldMicrotesla.isFinite() || expectedFieldMicrotesla <= 0 ||
            kotlin.math.abs(fieldMicrotesla / expectedFieldMicrotesla - 1) > 0.35)) return false
    return true
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
