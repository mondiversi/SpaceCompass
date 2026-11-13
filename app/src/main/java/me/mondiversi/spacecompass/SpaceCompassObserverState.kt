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
private const val scenarioPreparedClockKey = "scenario_prepared_clock"

internal class SpaceCompassObserverState(private val preferences: SharedPreferences?,
    private val elapsedClock: () -> Long = { android.os.SystemClock.elapsedRealtime() },
    private val editorPreview: Boolean = false, initialPreviewInstant: Long? = null) : ViewModel() {
    var timeSettings by mutableStateOf(SpaceCompassScenarioTimeSettings()); private set
    private var clock: SpaceCompassScenarioClock? = null
    private var clockPlan: SpaceCompassObserverPlan? = null
    private var active = false
    private var localCustom = false
    private var applyingDeferred = false
    private var editor: SpaceCompassObserverState? = null
    private var previewSeed = initialPreviewInstant
    private var preparedClock: SpaceCompassScenarioCheckpoint? = null
    private var configuredPlan by mutableStateOf<SpaceCompassObserverPlan?>(null, referentialEqualityPolicy())
    private var navigationPlan by mutableStateOf<SpaceCompassObserverPlan?>(null, referentialEqualityPolicy())
    private var navigationPaused by mutableStateOf(false)
    private var navigationConfiguration: Triple<SpaceCompassObserverPlan?, SpaceCompassScenarioTimeSettings, Boolean>? = null
    private fun configuredCustomMode(): Boolean = if (preferences == null) localCustom else
        preferences.getString(SPACE_COMPASS_OBSERVER_MODE, null) == "custom"
    private fun resetNavigationPlayback() {
        navigationPlan = null; navigationPaused = false; navigationConfiguration = null
    }
    val plan: SpaceCompassObserverPlan? get() = navigationPlan ?: configuredPlan
    val playbackPaused: Boolean get() = navigationPaused ||
        (navigationPlan == null && configuredPlan?.scenarioClock?.settings?.multiplier == 0)

    /** Session-only playback never rewrites the user's Location or remembered time rate. */
    fun togglePlayback(wallNow: Long = System.currentTimeMillis(), elapsedNow: Long = elapsedClock()) {
        if (editorPreview || applyingDeferred) return
        val pause = !playbackPaused
        // Spatial overrides do not change the live clock. Resume those at the actual current instant.
        val temporalOverride = configuredPlan?.let { it.simulateDate || it.simulateTime } == true ||
            timeSettings.enabled && timeSettings.rate != 1
        if (!pause && !temporalOverride) {
            resetNavigationPlayback()
            clock?.let { previous ->
                clock = SpaceCompassScenarioClock(previous.settings, previous.signature, wallNow, wallNow,
                    elapsedNow, elapsed = elapsedClock).also { it.setActive(active, wallNow, elapsedNow) }
                configuredPlan = configuredPlan?.copy()?.also { it.scenarioClock = clock }
            }
            return
        }
        val existing = navigationPlan?.scenarioClock
        if (existing != null) {
            if (pause) existing.hold(wallNow, elapsedNow) else existing.release(wallNow, elapsedNow)
        } else {
            val base = configuredPlan ?: SpaceCompassObserverPlan(0.0, 0.0, 0.0, wallNow,
                ZoneId.systemDefault().id, simulatePosition = false, simulateTime = false,
                simulateAltitude = false, simulateZone = false, simulateDate = false)
            val previous = base.scenarioClock?.settings ?: timeSettings
            val settings = previous.copy(rate = previous.rate.takeUnless { it == 0 } ?: 1)
            val instant = base.scenarioClock?.at(wallNow, elapsedNow) ?: base.observationTimeMs(wallNow)
            val playback = SpaceCompassScenarioClock(settings, "navigation-playback", instant, wallNow,
                elapsedNow, elapsed = elapsedClock).also {
                it.setActive(active, wallNow, elapsedNow)
                if (pause) it.hold(wallNow, elapsedNow)
            }
            navigationConfiguration = Triple(savedPlan, timeSettings, configuredCustomMode())
            navigationPlan = base.copy().also { it.scenarioClock = playback }
        }
        navigationPaused = pause
    }
    /** Restore an exported hold without rewriting the recipient's configured Location or speed. */
    fun restoreScenePause(instant: Long, wallNow: Long = System.currentTimeMillis(), elapsedNow: Long = elapsedClock()) {
        if (editorPreview || applyingDeferred) return
        require(java.time.Instant.ofEpochMilli(instant).atZone(ZoneId.of("UTC")).year in 1900..2100)
        val base = configuredPlan ?: SpaceCompassObserverPlan(0.0, 0.0, 0.0, wallNow, ZoneId.systemDefault().id,
            simulatePosition = false, simulateTime = false, simulateAltitude = false, simulateZone = false, simulateDate = false)
        val previous = base.scenarioClock?.settings ?: timeSettings
        val settings = previous.copy(rate = previous.rate.takeUnless { it == 0 } ?: 1)
        val playback = SpaceCompassScenarioClock(settings, "navigation-playback", instant, wallNow, elapsedNow,
            elapsed = elapsedClock).also {
                it.setActive(active, wallNow, elapsedNow)
                it.hold(wallNow, elapsedNow)
            }
        navigationConfiguration = Triple(savedPlan, timeSettings, configuredCustomMode())
        navigationPlan = base.copy().also { it.scenarioClock = playback }
        navigationPaused = true
    }
    var savedPlan by mutableStateOf<SpaceCompassObserverPlan?>(null)
        private set
    var editorGeneration by mutableIntStateOf(0); private set
    var archiveName by mutableStateOf<String?>(null); private set
    var devicePlace by mutableStateOf<SpaceCompassDevicePlace?>(null)
    init { reload() }
    fun reload() {
        if (applyingDeferred) return
        val seed = previewSeed.also { previewSeed = null }
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
        // Reattaching a preference listener (for example after rotation) must not resume a held sky.
        if (navigationPlan != null && navigationConfiguration == Triple(savedPlan, timeSettings, configuredCustomMode())) return
        resetNavigationPlayback()
        val raw = savedPlan.takeIf { configuredCustomMode() }
            ?: if (timeSettings.enabled) SpaceCompassObserverPlan(0.0, 0.0, 0.0, now, ZoneId.systemDefault().id,
                simulatePosition = false, simulateTime = false, simulateAltitude = false, simulateZone = false,
                simulateDate = false) else null
        if (raw == null || !raw.hasCustomMoment && !timeSettings.enabled) {
            clock = null; clockPlan = null; configuredPlan = raw
            if (editorPreview) {
                preparedClock = null
                preferences?.edit()?.remove(scenarioPreparedClockKey)?.apply()
            }
            // Returning wholly to live time ends the temporal session, while retaining its settings.
            if (preferences?.getString(scenarioCheckpointKey,null) != null)
                preferences?.edit()?.remove(scenarioCheckpointKey)?.apply()
            return
        }
        val signature = spaceCompassScenarioClockSignature(raw, timeSettings, ZoneId.systemDefault())
        val prepared = (preparedClock ?: SpaceCompassScenarioCheckpoint.decode(
            preferences?.getString(scenarioPreparedClockKey, null)))?.takeIf { it.signature == signature }
        if (clock?.signature != signature || prepared != null && clock?.at(now, elapsedClock()) != prepared.instant) {
            val sameMoment = clockPlan?.let { spaceCompassScenarioClockSignature(it, SpaceCompassScenarioTimeSettings(), ZoneId.systemDefault()) ==
                spaceCompassScenarioClockSignature(raw, SpaceCompassScenarioTimeSettings(), ZoneId.systemDefault()) } == true
            val current = seed ?: clock?.takeIf { sameMoment }?.at(now)
            val checkpoint = SpaceCompassScenarioCheckpoint.decode(preferences?.getString(scenarioCheckpointKey, null))
            val configured = if (editorPreview || prepared != null || timeSettings.resume == SpaceCompassScenarioResume.RESTART)
                raw.copy(advanceTime = false, clockOffsetMs = null) else raw
            clock = SpaceCompassScenarioClock(timeSettings, signature, prepared?.instant ?: current ?: configured.observationTimeMs(now), now,
                elapsedClock(), checkpoint.takeUnless { editorPreview || prepared != null }, elapsedClock).also {
                    if (editorPreview || prepared != null) it.hold(now, elapsedClock())
                    it.setActive(active, now, elapsedClock())
                }
            clockPlan = raw
        }
        configuredPlan = raw.copy().also { it.scenarioClock = clock }
        if (editorPreview) {
            preparedClock = clock?.checkpoint(now, elapsedClock())
            preferences?.edit()?.putString(scenarioPreparedClockKey, preparedClock?.encode())?.apply()
        } else if (timeSettings.resume == SpaceCompassScenarioResume.ALWAYS) checkpoint()
    }

    /** Keep the running observer unchanged while the editor autosaves its prepared configuration. */
    fun beginEditing(): SpaceCompassObserverState {
        editor?.let { return it }
        checkpoint(sync = true)
        val now = System.currentTimeMillis()
        clock?.hold(now, elapsedClock())
        navigationPlan?.scenarioClock?.hold(now, elapsedClock())
        applyingDeferred = true
        return SpaceCompassObserverState(preferences, elapsedClock, editorPreview = true,
            initialPreviewInstant = plan?.observationTimeMs(now)).also { draft ->
            if (preferences == null) {
                draft.savedPlan = savedPlan; draft.timeSettings = timeSettings; draft.localCustom = localCustom
                draft.previewSeed = plan?.observationTimeMs(now); draft.reload()
            }
            draft.devicePlace = devicePlace; draft.archiveName = archiveName
            editor = draft
        }
    }

    /** Called only when the navigation route returns to the main sky, after the editor flushes. */
    fun finishEditing() {
        val draft = editor
        if (draft == null && preferences?.getString(scenarioPreparedClockKey, null) == null) return
        val now = System.currentTimeMillis()
        if (draft != null) {
            val instant = draft.clock?.at(now, elapsedClock())
            val saved = draft.savedPlan?.let { value ->
                if (value.advancesClock && instant != null) value.copy(clockOffsetMs = instant - now) else value
            }
            draft.savedPlan = saved
            preparedClock = if (saved != null && instant != null) SpaceCompassScenarioCheckpoint(
                spaceCompassScenarioClockSignature(saved, draft.timeSettings, ZoneId.systemDefault()), instant, now) else null
            preferences?.edit()?.apply {
                if (saved != null) putString(SPACE_COMPASS_OBSERVER_PLAN, saved.encode())
                putString(scenarioPreparedClockKey, preparedClock?.encode())
            }?.apply()
            if (preferences == null) {
                savedPlan = saved; timeSettings = draft.timeSettings; localCustom = draft.localCustom
            }
            archiveName = draft.archiveName ?: archiveName
        }
        applyingDeferred = false; editor = null
        resetNavigationPlayback()
        reload()
        clock?.release(now, elapsedClock())
        preparedClock = null
        preferences?.edit()?.remove(scenarioPreparedClockKey)?.apply()
        checkpoint(sync = true)
    }
    /** A deliberately imported speed is restored exactly; an archived file never carries elapsed runtime. */
    fun importArchive(archive: SpaceCompassScenarioArchive) {
        resetNavigationPlayback()
        val checked = SpaceCompassScenarioArchive.decode(archive.encode())
        val configured = checked.configurationAtImport(System.currentTimeMillis(), ZoneId.systemDefault())
        clock = null; clockPlan = null; preparedClock = null
        savedPlan = configured; localCustom = checked.custom
        // A place-only import returns to live time without replacing this device's resumption preference.
        timeSettings = if (checked.temporalSettingsIncluded) checked.timeSettings else timeSettings.copy(enabled = false)
        val editor = preferences?.edit()?.putString(SPACE_COMPASS_OBSERVER_PLAN, configured.encode())
            ?.putString(SPACE_COMPASS_OBSERVER_MODE, if (checked.custom) "custom" else "live")
            ?.putBoolean(scenarioEnabledKey, timeSettings.enabled)?.putInt(scenarioRateKey, timeSettings.rate)
            ?.putString(scenarioResumeKey, timeSettings.resume.name)?.remove(scenarioCheckpointKey)?.remove(scenarioPreparedClockKey)
        checked.celestialSelection?.let { editor?.putSpaceCompassCelestialSelection(it) }
        editor?.apply()
        reload()
        archiveName = checked.name
        editorGeneration++
    }

    fun configureTime(value: SpaceCompassScenarioTimeSettings) {
        // Activation changes only the switch; the last chosen speed (including pause/rewind) stays prepared.
        val next = value.normalized()
        if (timeSettings == next) return
        checkpoint()
        preferences?.edit()?.putBoolean(scenarioEnabledKey, next.enabled)?.putInt(scenarioRateKey, next.rate)
            ?.putString(scenarioResumeKey, next.resume.name)?.apply()
        if (preferences == null) timeSettings = next
        reload()
    }
    /** Select and freeze an event atomically while retaining configured spatial values. */
    fun observeAt(instant: Long, deviceZone: ZoneId = ZoneId.systemDefault()): Boolean {
        val selected = spaceCompassObserverPlanAtMoment(plan, savedPlan, instant, deviceZone) ?: return false
        val paused = timeSettings.copy(enabled = true, rate = 0)
        if (savedPlan == selected && timeSettings == paused && plan != null) return false
        resetNavigationPlayback()
        clock = null; clockPlan = null; preparedClock = null
        // Every preference becomes visible together, so listeners cannot resurrect a running checkpoint.
        preferences?.edit()?.putString(SPACE_COMPASS_OBSERVER_PLAN, selected.encode())
            ?.putString(SPACE_COMPASS_OBSERVER_MODE, "custom")?.putBoolean(scenarioEnabledKey, true)
            ?.putInt(scenarioRateKey, 0)?.putString(scenarioResumeKey, paused.resume.name)
            ?.remove(scenarioCheckpointKey)?.remove(scenarioPreparedClockKey)?.apply()
        savedPlan = selected; timeSettings = paused; localCustom = true
        reload()
        editorGeneration++
        return true
    }

    fun active(value: Boolean) {
        active = value
        val now = System.currentTimeMillis()
        clock?.setActive(value, now)
        navigationPlan?.scenarioClock?.setActive(value, now)
        if (!value) checkpoint(sync = true)
    }
    fun checkpoint(sync: Boolean = false) {
        if (editorPreview || applyingDeferred || navigationPlan != null) return
        if (clock?.settings?.resume == SpaceCompassScenarioResume.RESTART) return
        val value = clock?.checkpoint(System.currentTimeMillis()) ?: return
        val edit = preferences?.edit()?.putString(scenarioCheckpointKey, value.encode()) ?: return
        if (sync) edit.commit() else edit.apply()
    }
    override fun onCleared() { checkpoint(sync = true) }
    /** Keep prepared values while all overrides are off, without creating a live override. */
    fun storeEditorConfiguration(value: SpaceCompassObserverPlan?): Boolean {
        if (value == null || value.activeSections.isNotEmpty()) return apply(value)
        if (savedPlan == value && plan == null) return false
        checkpoint()
        preferences?.edit()?.putString(SPACE_COMPASS_OBSERVER_PLAN,value.encode())
            ?.putString(SPACE_COMPASS_OBSERVER_MODE,"live")?.apply()
        savedPlan = value
        localCustom = false
        reload()
        return true
    }

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
    LocalSpaceCompassObservationZoneOverride.current?.let { return it }
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
