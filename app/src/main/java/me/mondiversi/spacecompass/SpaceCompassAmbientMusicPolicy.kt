package me.mondiversi.spacecompass

internal const val SPACE_COMPASS_MUSIC_ENABLED_KEY = "ambient_music_enabled"
internal const val SPACE_COMPASS_MUSIC_VOLUME_KEY = "ambient_music_volume"
internal const val SPACE_COMPASS_MUSIC_DEFAULT_ENABLED = true
internal const val SPACE_COMPASS_MUSIC_DEFAULT_VOLUME = 50

internal data class SpaceCompassAmbientMusicSettings(val enabled: Boolean = SPACE_COMPASS_MUSIC_DEFAULT_ENABLED,
    val volumePercent: Int = SPACE_COMPASS_MUSIC_DEFAULT_VOLUME) {
    val gain: Float get() = volumePercent.coerceIn(0, 100) / 100f
}

internal enum class SpaceCompassMusicFocus { NONE, GRANTED, TRANSIENT_LOSS, LOST }

/** Foreground ownership and audio focus are independent of the saved on/off choice. */
internal class SpaceCompassAmbientMusicPolicy {
    var settings = SpaceCompassAmbientMusicSettings(); private set
    var foreground = false; private set
    var focus = SpaceCompassMusicFocus.NONE; private set
    val wantsAudio: Boolean get() = foreground && settings.enabled && settings.volumePercent > 0
    val canRequestFocus: Boolean get() = wantsAudio && focus == SpaceCompassMusicFocus.NONE
    val canPlay: Boolean get() = wantsAudio && focus == SpaceCompassMusicFocus.GRANTED

    fun update(value: SpaceCompassAmbientMusicSettings) {
        val wasAudible = settings.enabled && settings.volumePercent > 0
        settings = value.copy(volumePercent = value.volumePercent.coerceIn(0, 100))
        if (!wantsAudio || !wasAudible) focus = SpaceCompassMusicFocus.NONE
    }

    fun setForeground(value: Boolean) {
        if (foreground != value) focus = SpaceCompassMusicFocus.NONE
        foreground = value
    }

    fun onFocus(value: SpaceCompassMusicFocus) {
        focus = if (wantsAudio) value else SpaceCompassMusicFocus.NONE
    }
}
