package me.mondiversi.spacecompass

import android.content.SharedPreferences
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner

internal val LocalSpaceCompassAmbientMusic = staticCompositionLocalOf { SpaceCompassAmbientMusicSettings() }
internal val LocalSpaceCompassMusicVolumePreview = staticCompositionLocalOf<(Int?) -> Unit> { {} }

/** Music changes do not invalidate celestial calculations or presentation preferences. */
@Composable
internal fun SpaceCompassAmbientMusic(content: @Composable () -> Unit) {
    val preferences = LocalSpaceCompassPreferences.current
    if (preferences == null) { content(); return }
    fun read() = SpaceCompassAmbientMusicSettings(
        runCatching { preferences.getBoolean(SPACE_COMPASS_MUSIC_ENABLED_KEY, SPACE_COMPASS_MUSIC_DEFAULT_ENABLED) }
            .getOrDefault(SPACE_COMPASS_MUSIC_DEFAULT_ENABLED),
        runCatching { preferences.getInt(SPACE_COMPASS_MUSIC_VOLUME_KEY, SPACE_COMPASS_MUSIC_DEFAULT_VOLUME) }
            .getOrDefault(SPACE_COMPASS_MUSIC_DEFAULT_VOLUME).coerceIn(0, 100))
    var settings by remember(preferences) { mutableStateOf(read()) }
    var preview by remember(preferences) { mutableStateOf<Int?>(null) }
    DisposableEffect(preferences) {
        val listener = SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
            if (key == null || key == SPACE_COMPASS_MUSIC_ENABLED_KEY || key == SPACE_COMPASS_MUSIC_VOLUME_KEY) settings = read()
        }
        preferences.registerOnSharedPreferenceChangeListener(listener)
        settings = read()
        onDispose { preferences.unregisterOnSharedPreferenceChangeListener(listener) }
    }
    val context = LocalContext.current
    val player = remember(context) { SpaceCompassAmbientMusicPlayer(context).also { it.update(settings) } }
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    SideEffect { player.update(settings.copy(volumePercent = preview ?: settings.volumePercent)) }
    DisposableEffect(player) { onDispose { player.release() } }
    DisposableEffect(player, lifecycle) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> player.foreground(true)
                Lifecycle.Event.ON_PAUSE, Lifecycle.Event.ON_STOP -> player.foreground(false)
                else -> Unit
            }
        }
        lifecycle.addObserver(observer)
        player.foreground(lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED))
        onDispose { lifecycle.removeObserver(observer); player.foreground(false) }
    }
    CompositionLocalProvider(LocalSpaceCompassAmbientMusic provides settings,
        LocalSpaceCompassMusicVolumePreview provides { preview = it?.coerceIn(0, 100) }, content = content)
}
