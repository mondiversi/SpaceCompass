package me.mondiversi.spacecompass

import android.content.SharedPreferences
import androidx.compose.runtime.*
import java.time.ZoneId

internal class SpaceCompassObserverState(private val preferences: SharedPreferences?) {
    var plan by mutableStateOf<SpaceCompassObserverPlan?>(null)
        private set
    var savedPlan by mutableStateOf<SpaceCompassObserverPlan?>(null)
        private set
    var devicePlace by mutableStateOf<SpaceCompassDevicePlace?>(null)
    init { reload() }
    fun reload() {
        savedPlan = decodeSpaceCompassObserverPlan(preferences?.getString(SPACE_COMPASS_OBSERVER_PLAN, null))
        plan = savedPlan.takeIf { preferences?.getString(SPACE_COMPASS_OBSERVER_MODE, null) == "custom" }
    }
    fun apply(value: SpaceCompassObserverPlan?): Boolean {
        if (plan == value) return false
        preferences?.edit()?.apply {
            if (value != null) putString(SPACE_COMPASS_OBSERVER_PLAN, value.encode())
            putString(SPACE_COMPASS_OBSERVER_MODE, if (value == null) "live" else "custom")
        }?.apply()
        if (value != null) savedPlan = value
        plan = value
        return true
    }
}

internal val LocalSpaceCompassObserver = staticCompositionLocalOf<SpaceCompassObserverState?> { null }
/** Live mode follows phone-zone changes instead of retaining a composition-time default. */
@Composable
internal fun spaceCompassObservationZone(): ZoneId {
    val deviceZone = ZoneId.systemDefault()
    return LocalSpaceCompassObserver.current?.plan?.observationZone(deviceZone) ?: deviceZone
}

@Composable
internal fun rememberSpaceCompassObserverState(): SpaceCompassObserverState {
    val preferences = LocalSpaceCompassPreferences.current
    val observer = remember(preferences) { SpaceCompassObserverState(preferences) }
    DisposableEffect(preferences, observer) {
        val listener = SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
            if (key == null || key == SPACE_COMPASS_OBSERVER_MODE || key == SPACE_COMPASS_OBSERVER_PLAN) observer.reload()
        }
        preferences?.registerOnSharedPreferenceChangeListener(listener)
        observer.reload()
        onDispose { preferences?.unregisterOnSharedPreferenceChangeListener(listener) }
    }
    return observer
}
