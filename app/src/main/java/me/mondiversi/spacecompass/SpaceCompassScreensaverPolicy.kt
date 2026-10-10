package me.mondiversi.spacecompass

internal const val SPACE_COMPASS_SCREENSAVER_ENABLED_KEY = "screensaver_enabled"
internal const val SPACE_COMPASS_SCREENSAVER_DELAY_KEY = "screensaver_delay_seconds"
internal const val SPACE_COMPASS_SCREENSAVER_DEFAULT_DELAY = 300
internal val spaceCompassScreensaverDelays = listOf(30, 60, 120, 300, 600)

internal data class SpaceCompassScreensaverSettings(val enabled: Boolean = true,
    val delaySeconds: Int = SPACE_COMPASS_SCREENSAVER_DEFAULT_DELAY) {
    fun normalized() = copy(delaySeconds = delaySeconds.takeIf { it in spaceCompassScreensaverDelays }
        ?: SPACE_COMPASS_SCREENSAVER_DEFAULT_DELAY)
}

/** Monotonic inactivity: updates and sensor readings do not count as user input. */
internal class SpaceCompassScreensaverPolicy {
    var settings = SpaceCompassScreensaverSettings(); private set
    var eligible = false; private set
    var active = false; private set
    private var touching = false
    private var lastInteraction = 0L

    fun updateSettings(value: SpaceCompassScreensaverSettings, now: Long) {
        val normalized = value.normalized()
        if (settings == normalized) return
        settings = normalized
        if (!settings.enabled) active = false
        interaction(now)
    }

    fun setEligible(value: Boolean, now: Long) {
        if (eligible == value) return
        eligible = value
        if (!value) touching = false
        interaction(now)
    }

    fun interaction(now: Long) { lastInteraction = now }
    fun setTouching(value: Boolean, now: Long) { touching = value; interaction(now) }
    fun remainingMillis(now: Long): Long? {
        if (!eligible || !settings.enabled || active || touching) return null
        val elapsed = (now - lastInteraction).coerceAtLeast(0L)
        return (settings.delaySeconds * 1_000L - elapsed).coerceAtLeast(0L)
    }

    fun tick(now: Long): Boolean {
        if (remainingMillis(now) == 0L) active = true
        return active
    }

    // Input alone must not remove the cover during ACTION_DOWN and click the underlying page.
    fun dismiss(now: Long) { active = false; touching = false; interaction(now) }
    fun reset(now: Long) { dismiss(now) }
}
