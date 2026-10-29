package me.mondiversi.spacecompass

import android.content.SharedPreferences
import androidx.compose.runtime.*
import java.time.ZoneId
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStoreOwner
import kotlinx.coroutines.delay
import androidx.activity.compose.LocalActivity
import androidx.lifecycle.repeatOnLifecycle

private const val scenarioEnabledKey = "scenario_speed_enabled"
private const val scenarioRateKey = "scenario_speed_rate"
private const val scenarioResumeKey = "scenario_resume"
private const val scenarioCheckpointKey = "scenario_clock_checkpoint"

internal class SpaceCompassObserverState(private val preferences: SharedPreferences?,
    private val elapsedClock: () -> Long = { android.os.SystemClock.elapsedRealtime() }) : ViewModel() {
    var timeSettings by mutableStateOf(SpaceCompassScenarioTimeSettings()); private set
    private var clock: SpaceCompassScenarioClock? = null
    private var clockPlan: SpaceCompassObserverPlan? = null
    private var active = false
    private var localCustom = false
    var plan by mutableStateOf<SpaceCompassObserverPlan?>(null, referentialEqualityPolicy())
        private set
    var savedPlan by mutableStateOf<SpaceCompassObserverPlan?>(null)
        private set
    var editorGeneration by mutableIntStateOf(0); private set
    var archiveName by mutableStateOf<String?>(null); private set
    var devicePlace by mutableStateOf<SpaceCompassDevicePlace?>(null)
    init { reload() }
    fun reload() {
        if (preferences != null) {
        savedPlan = decodeSpaceCompassObserverPlan(preferences.getString(SPACE_COMPASS_OBSERVER_PLAN, null),
            allowInactive = preferences.getString(SPACE_COMPASS_OBSERVER_MODE, null) == "live")
        timeSettings = SpaceCompassScenarioTimeSettings(
            preferences?.getBoolean(scenarioEnabledKey, false) ?: false,
            preferences?.getInt(scenarioRateKey, 1) ?: 1,
            runCatching { SpaceCompassScenarioResume.valueOf(preferences?.getString(scenarioResumeKey, null).orEmpty()) }
                .getOrDefault(if (savedPlan?.advancesClock == true) SpaceCompassScenarioResume.ALWAYS else SpaceCompassScenarioResume.DEFAULT))
            .normalized()
        }
        val now = System.currentTimeMillis()
        val raw = savedPlan.takeIf { if (preferences == null) localCustom else preferences.getString(SPACE_COMPASS_OBSERVER_MODE, null) == "custom" }
            ?: if (timeSettings.enabled) SpaceCompassObserverPlan(0.0, 0.0, 0.0, now, ZoneId.systemDefault().id,
                simulatePosition = false, simulateTime = false, simulateAltitude = false, simulateZone = false,
                simulateDate = false) else null
        if (raw == null || !raw.hasCustomMoment && !timeSettings.enabled) {
            clock = null; clockPlan = null; plan = raw
            // Returning wholly to live time ends the temporal session, while retaining its settings.
            if (preferences?.getString(scenarioCheckpointKey,null) != null)
                preferences?.edit()?.remove(scenarioCheckpointKey)?.apply()
            return
        }
        val signature = spaceCompassScenarioClockSignature(raw, timeSettings, ZoneId.systemDefault())
        if (clock?.signature != signature) {
            val sameMoment = clockPlan?.let { spaceCompassScenarioClockSignature(it, SpaceCompassScenarioTimeSettings(), ZoneId.systemDefault()) ==
                spaceCompassScenarioClockSignature(raw, SpaceCompassScenarioTimeSettings(), ZoneId.systemDefault()) } == true
            val current = clock?.takeIf { sameMoment }?.at(now)
            val checkpoint = SpaceCompassScenarioCheckpoint.decode(preferences?.getString(scenarioCheckpointKey, null))
            val configured = if (timeSettings.resume == SpaceCompassScenarioResume.RESTART)
                raw.copy(advanceTime = false, clockOffsetMs = null) else raw
            clock = SpaceCompassScenarioClock(timeSettings, signature, current ?: configured.observationTimeMs(now), now,
                elapsedClock(), checkpoint, elapsedClock).also { it.setActive(active, now) }
            clockPlan = raw
        }
        plan = raw.copy().also { it.scenarioClock = clock }
        if (timeSettings.resume == SpaceCompassScenarioResume.ALWAYS) checkpoint()
    }
    /** A deliberately imported speed is restored exactly; an archived file never carries elapsed runtime. */
    fun importArchive(archive: SpaceCompassScenarioArchive) {
        val checked = SpaceCompassScenarioArchive.decode(archive.encode())
        val configured = checked.configurationAtImport(System.currentTimeMillis(), ZoneId.systemDefault())
        clock = null; clockPlan = null
        savedPlan = configured; localCustom = checked.custom
        // A place-only import returns to live time without replacing this device's resumption preference.
        timeSettings = if (checked.temporalSettingsIncluded) checked.timeSettings else timeSettings.copy(enabled = false)
        val editor = preferences?.edit()?.putString(SPACE_COMPASS_OBSERVER_PLAN, configured.encode())
            ?.putString(SPACE_COMPASS_OBSERVER_MODE, if (checked.custom) "custom" else "live")
            ?.putBoolean(scenarioEnabledKey, timeSettings.enabled)?.putInt(scenarioRateKey, timeSettings.rate)
            ?.putString(scenarioResumeKey, timeSettings.resume.name)?.remove(scenarioCheckpointKey)
        checked.celestialSelection?.let { editor?.putSpaceCompassCelestialSelection(it) }
        editor?.apply()
        reload()
        archiveName = checked.name
        editorGeneration++
    }

    fun configureTime(value: SpaceCompassScenarioTimeSettings) {
        // Re-enabling acceleration always requires a fresh, deliberate speed choice.
        val next = (if (!timeSettings.enabled && value.enabled) value.copy(rate = 1) else value).normalized()
        if (timeSettings == next) return
        checkpoint()
        preferences?.edit()?.putBoolean(scenarioEnabledKey, next.enabled)?.putInt(scenarioRateKey, next.rate)
            ?.putString(scenarioResumeKey, next.resume.name)?.apply()
        if (preferences == null) timeSettings = next
        reload()
    }
    fun active(value: Boolean) {
        active = value; clock?.setActive(value, System.currentTimeMillis())
        if (!value) checkpoint(sync = true)
    }
    fun checkpoint(sync: Boolean = false) {
        if (clock?.settings?.resume == SpaceCompassScenarioResume.RESTART) return
        val value = clock?.checkpoint(System.currentTimeMillis()) ?: return
        val edit = preferences?.edit()?.putString(scenarioCheckpointKey, value.encode()) ?: return
        if (sync) edit.commit() else edit.apply()
    }
    override fun onCleared() { checkpoint(sync = true) }
    fun apply(value: SpaceCompassObserverPlan?): Boolean {
        val custom = if (preferences == null) localCustom else preferences.getString(SPACE_COMPASS_OBSERVER_MODE, null) == "custom"
        if (value == null && !custom) return false
        if (plan == value) return false
        checkpoint()
        preferences?.edit()?.apply {
            if (value != null) putString(SPACE_COMPASS_OBSERVER_PLAN, value.encode())
            putString(SPACE_COMPASS_OBSERVER_MODE, if (value == null) "live" else "custom")
        }?.apply()
        if (value != null) savedPlan = value
        localCustom = value != null
        reload()
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
    // The saver supplies a lifecycle-only wrapper, so retain state in the actual activity store.
    val owner = LocalActivity.current as? ViewModelStoreOwner
    val observer = remember(owner, preferences) {
        if (owner == null) SpaceCompassObserverState(preferences) else ViewModelProvider(owner, object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T = SpaceCompassObserverState(preferences) as T
        })[SpaceCompassObserverState::class.java]
    }
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    val saver = LocalSpaceCompassScreensaverActive.current
    DisposableEffect(observer, lifecycle, saver) {
        val listener = LifecycleEventObserver { _, _ ->
            observer.active(!saver && lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED))
        }
        lifecycle.addObserver(listener)
        observer.active(!saver && lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED))
        onDispose { lifecycle.removeObserver(listener); observer.active(false) }
    }
    LaunchedEffect(observer, saver, observer.plan) {
        lifecycle.repeatOnLifecycle(Lifecycle.State.RESUMED) {
            while (!saver && observer.plan?.scenarioClock?.settings?.resume == SpaceCompassScenarioResume.ACTIVE) {
                delay(1_000)
                observer.checkpoint()
            }
        }
    }
    DisposableEffect(preferences, observer) {
        val listener = SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
            if (key == null || key == SPACE_COMPASS_OBSERVER_MODE || key == SPACE_COMPASS_OBSERVER_PLAN ||
                key in setOf(scenarioEnabledKey, scenarioRateKey, scenarioResumeKey)) observer.reload()
        }
        preferences?.registerOnSharedPreferenceChangeListener(listener)
        observer.reload()
        onDispose { preferences?.unregisterOnSharedPreferenceChangeListener(listener) }
    }
    return observer
}
