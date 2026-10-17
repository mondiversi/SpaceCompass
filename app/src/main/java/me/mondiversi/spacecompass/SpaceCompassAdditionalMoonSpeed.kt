package me.mondiversi.spacecompass


/** Parent-relative moon speed. No absolute heliocentric speed is mislabeled as orbital speed. */
internal fun spaceCompassAdditionalMoonSpeed(body: SpaceCompassCelestialBody, timeMs: Long,
    remote: SpaceCompassCelestialRemoteData): Double? {
    val parent = body.horizonsMotionPrimary ?: return null
    val samples = remote.motions[body]?.takeIf { it.body == body && it.primary == parent }?.samples ?: return null
    if (timeMs !in samples.first().timeMs..samples.last().timeMs) return null
    val index = samples.indexOfFirst { it.timeMs >= timeMs }.coerceAtLeast(1)
    val a = samples[index - 1]; val b = samples[index]
    val f = (timeMs - a.timeMs).toDouble() / (b.timeMs - a.timeMs)
    val days = (b.timeMs - a.timeMs) / 86_400_000.0
    // Differentiate the Hermite position, rather than linearly damping fast moon velocities.
    // For Phobos, a half-hour midpoint would otherwise underestimate the speed by about 8%.
    fun velocity(p: Double, q: Double, v: Double, w: Double): Double {
        val f2 = f * f
        return ((6*f2-6*f)*p + (3*f2-4*f+1)*days*v +
            (-6*f2+6*f)*q + (3*f2-2*f)*days*w) / days
    }
    return spaceCompassCelestialVelocityMagnitude(velocity(a.x,b.x,a.vx,b.vx),
        velocity(a.y,b.y,a.vy,b.vy), velocity(a.z,b.z,a.vz,b.vz),
        SPACE_COMPASS_AU_DAY_TO_KM_SECOND)
}
