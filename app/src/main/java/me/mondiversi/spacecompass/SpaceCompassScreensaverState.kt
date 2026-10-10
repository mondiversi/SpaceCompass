package me.mondiversi.spacecompass

import android.content.SharedPreferences
import android.os.SystemClock
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/** Retains an active saver through rotation, but owns no activity or rendering view. */
internal class SpaceCompassScreensaverState : ViewModel() {
    private val policy = SpaceCompassScreensaverPolicy()
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var timer: Job? = null
    private var preferences: SharedPreferences? = null
    private var resumed = false
    private var focused = false
    private var presented = false
    private var keyboard = false
    var settings by mutableStateOf(SpaceCompassScreensaverSettings()); private set
    var visible by mutableStateOf(false); private set
    var sceneStartedAt = 0L; private set
    // Latest height from the retained sky; sampled only by the visible saver, without extra sensors.
    var sunElevationDegrees: Double? = null; private set
    fun sunElevation(value: Double?) { sunElevationDegrees = value?.takeIf { it.isFinite() } }

    private val listener = SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
        if (key == null || key == SPACE_COMPASS_SCREENSAVER_ENABLED_KEY || key == SPACE_COMPASS_SCREENSAVER_DELAY_KEY) readSettings()
    }

    fun bind(value: SharedPreferences) {
        if (preferences !== value) {
            preferences?.unregisterOnSharedPreferenceChangeListener(listener)
            preferences = value
            value.registerOnSharedPreferenceChangeListener(listener)
        }
        readSettings()
    }

    private fun readSettings() {
        val saved = preferences ?: return
        val defaults = SpaceCompassScreensaverSettings()
        settings = SpaceCompassScreensaverSettings(
            runCatching { saved.getBoolean(SPACE_COMPASS_SCREENSAVER_ENABLED_KEY, defaults.enabled) }.getOrDefault(defaults.enabled),
            runCatching { saved.getInt(SPACE_COMPASS_SCREENSAVER_DELAY_KEY, defaults.delaySeconds) }
                .getOrDefault(defaults.delaySeconds)).normalized()
        policy.updateSettings(settings, SystemClock.elapsedRealtime())
        schedule()
    }

    fun resumed(value: Boolean) { resumed = value; eligibility() }
    fun focused(value: Boolean) { focused = value; eligibility() }
    fun presented(value: Boolean) { presented = value; eligibility() }
    fun keyboard(value: Boolean) {
        if (keyboard != value) { keyboard = value; eligibility() }
    }

    private fun eligibility() {
        policy.setEligible(resumed && focused && presented && !keyboard, SystemClock.elapsedRealtime())
        schedule()
    }

    fun interaction() { policy.interaction(SystemClock.elapsedRealtime()); schedule() }
    fun touching(value: Boolean) { policy.setTouching(value, SystemClock.elapsedRealtime()); schedule() }
    fun dismiss() { policy.dismiss(SystemClock.elapsedRealtime()); schedule() }
    fun reset() { policy.reset(SystemClock.elapsedRealtime()); schedule() }

    private fun schedule() {
        timer?.cancel()
        visible = policy.active && policy.eligible
        val remaining = policy.remainingMillis(SystemClock.elapsedRealtime()) ?: return
        timer = scope.launch {
            var waitMillis = remaining
            while (true) {
                delay(waitMillis)
                val now = SystemClock.elapsedRealtime()
                if (policy.tick(now)) {
                    sceneStartedAt = now
                    visible = policy.eligible
                    break
                }
                // A timer may resume just before the elapsed-clock deadline.
                waitMillis = policy.remainingMillis(now) ?: break
            }
        }
    }

    override fun onCleared() {
        preferences?.unregisterOnSharedPreferenceChangeListener(listener)
        scope.cancel()
    }
}
