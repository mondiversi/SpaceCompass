package me.mondiversi.spacecompass

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.layout
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import kotlinx.coroutines.delay
import java.time.Instant
import java.time.ZoneId
import kotlin.math.round

@Composable
internal fun SpaceCompassSunFinderScreen(
    backgroundColor: Color, primaryText: Color, secondaryText: Color, onDismissRequest: () -> Unit
) {
    val observer = LocalSpaceCompassObserver.current
    val plan = observer?.plan
    val catalogOpen = rememberSaveable { mutableStateOf(false) }
    var catalogViewerBody by rememberSaveable(stateSaver = spaceCompassOptionalCelestialBodySaver) { mutableStateOf<SpaceCompassCelestialBody?>(null) }
    val context = LocalContext.current
    val tutorialState = LocalSpaceCompassTutorial.current
    val tutorialActive = tutorialState?.active == true
    val tutorialBlocking = tutorialActive || tutorialState?.welcome == true
    LaunchedEffect(tutorialActive) {
        if (tutorialActive) { catalogOpen.value = false; catalogViewerBody = null }
    }

    val lifecycleOwner = LocalLifecycleOwner.current
    var permission by remember { mutableStateOf(hasSpaceCompassSunLocationPermission(context)) }
    var requested by rememberSaveable { mutableStateOf(false) }
    var retry by remember { mutableIntStateOf(0) }
    var remoteRetry by remember { mutableIntStateOf(0) }
    var time by remember { mutableLongStateOf(System.currentTimeMillis()) }
    val preferences = LocalSpaceCompassPreferences.current
        ?: remember(context) { context.getSharedPreferences(SPACE_COMPASS_PREFERENCES_NAME, android.content.Context.MODE_PRIVATE) }
    val initialSelection = remember(preferences) { readSpaceCompassCelestialSelection(preferences) }
    var selectedBody by rememberSaveable(stateSaver = spaceCompassCelestialBodySaver) { mutableStateOf(initialSelection.active ?: SpaceCompassCelestialBody.SUN) }
    var selectedNames by rememberSaveable { mutableStateOf(initialSelection.ordered.map { it.name }) }
    val selectedNameSet = remember(selectedNames) { selectedNames.toHashSet() }
    val selectedBodies = remember(selectedNameSet) { spaceCompassAllCelestialOrder.filter { it.name in selectedNameSet }.toSet() }
    val resolvedBody = selectedBody.takeIf { it in selectedBodies }
        ?: spaceCompassAllCelestialOrder.firstOrNull { it in selectedBodies } ?: SpaceCompassCelestialBody.SUN
    LaunchedEffect(preferences) {
        val stored = preferences.getStringSet("celestial_selected", null)
        if (stored?.contains("EARTH_CENTER") == true) {
            val migrated = restoreSpaceCompassCelestialSelection(stored, preferences.getString("celestial_active", null))
            preferences.edit().putStringSet("celestial_selected", migrated.ordered.map { it.name }.toSet())
                .putString("celestial_active", migrated.active?.name).apply()
        }
    }
    DisposableEffect(preferences) {
        // Scene imports can replace scene checks while this screen is retained behind another page.
        fun syncSelection() {
            val stored = readSpaceCompassCelestialSelection(preferences)
            selectedNames = stored.ordered.map { it.name }
            selectedBody = stored.active ?: SpaceCompassCelestialBody.SUN
        }
        val listener = android.content.SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
            if (key == null || key == "celestial_selected" || key == "celestial_active") syncSelection()
        }
        preferences.registerOnSharedPreferenceChangeListener(listener)
        syncSelection()
        onDispose { preferences.unregisterOnSharedPreferenceChangeListener(listener) }
    }
    val selectionResources = LocalResources.current
    val comparisons = LocalSpaceCompassComparisons.current
    val comparisonZone = spaceCompassObservationZone()
    val comparisonEntries = remember(comparisons?.entries, comparisons?.personalMoments, comparisonZone) {
        comparisons?.sceneEntries(comparisonZone).orEmpty()
    }
    fun applySelection(selection: SpaceCompassCelestialSelection) {
        val nextNames = selection.ordered.map { it.name }
        val activationNotice = spaceCompassActivationNotice(
            selectedNames.isNotEmpty() || comparisonEntries.isNotEmpty(),
            nextNames.isNotEmpty() || comparisonEntries.isNotEmpty())
        selectedNames = nextNames
        selection.active?.let { selectedBody = it }
        // Save in the selection event, before a subsequent frame or app closure.
        preferences.edit().putSpaceCompassCelestialSelection(selection).apply()
        activationNotice?.let { active ->
            showSpaceCompassBottomMessage(context, selectionResources.getString(
                if (active) R.string.scene_objects_on else R.string.scene_objects_off))
        }
    }
    var resumed by remember { mutableStateOf(lifecycleOwner.lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)) }
    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {
        permission = hasSpaceCompassSunLocationPermission(context)
        retry++
    }
    LaunchedEffect(plan?.simulatePosition, tutorialBlocking) {
        // First-run platform permissions follow the welcome/tutorial, never cover the guide.
        if (!tutorialBlocking && !permission && !requested && plan?.simulatePosition != true) {
            requested = true
            permissionLauncher.launch(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION))
        }
    }
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                resumed = true
                permission = hasSpaceCompassSunLocationPermission(context)
                time = System.currentTimeMillis()
            }
            if (event == Lifecycle.Event.ON_PAUSE || event == Lifecycle.Event.ON_STOP) resumed = false
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }
    LaunchedEffect(resumed) {
        // ISS's directional marker remains live even when its trajectory is unchecked.
        if (resumed) while (true) {
            time = System.currentTimeMillis()
            delay(if (observer?.timeSettings?.accelerated == true) 50L else 500L)
        }
    }
    val pauseOrientation = catalogOpen.value || !LocalSpaceCompassMainVisible.current
    val readings = key(retry) { rememberSpaceCompassSunFinderReadings(permission, paused = pauseOrientation) }
    val fixFresh = readings.location?.let {
        spaceCompassSunLocationUsable(it, android.os.SystemClock.elapsedRealtimeNanos())
    } == true
    val deviceReadings = if (!fixFresh && readings.location != null)
        readings.copy(location = null, locationStatus = SpaceCompassSunLocationStatus.SEARCHING) else readings
    SideEffect { observer?.devicePlace = deviceReadings.location?.let {
        SpaceCompassDevicePlace(it.latitude, it.longitude, it.altitude.takeIf { _ -> it.hasAltitude() })
    } }
    val resolvedFix = remember(plan, deviceReadings.location) {
        val real = deviceReadings.location
        val place = plan?.resolvePlace(real?.let {
            SpaceCompassDevicePlace(it.latitude, it.longitude, it.altitude.takeIf { _ -> it.hasAltitude() })
        })
        when {
            plan?.simulatePosition == true -> place?.let { android.location.Location("SpaceCompass scenario").apply {
                latitude = it.latitude; longitude = it.longitude; altitude = it.altitude ?: 0.0
                // No GPS precision is claimed for a point selected by the user.
            } }
            plan?.simulateAltitude == true && real != null && place != null -> android.location.Location(real).apply {
                altitude = requireNotNull(place.altitude)
                androidx.core.location.LocationCompat.removeVerticalAccuracy(this) // Coordinate accuracy remains real; simulated height has no GPS accuracy.
            }
            else -> real
        }
    }
    val visibleReadings = deviceReadings.copy(location = resolvedFix,
        locationStatus = if (plan?.simulatePosition == true) SpaceCompassSunLocationStatus.READY else deviceReadings.locationStatus)
    val observationTime = plan?.observationTimeMs(time) ?: time
    val requestTime = rememberSpaceCompassScenarioRequestTime(plan, time)
    val tile = visibleReadings.location?.let { spaceCompassSunWeatherTile(it.latitude, it.longitude) }
    val deviceTile = deviceReadings.location?.let { spaceCompassSunWeatherTile(it.latitude, it.longitude) }
    val deviceWeather = rememberSpaceCompassSunWeather(deviceTile, resumed)
    // Share one live request when possible; scenario weather never replaces the device environment.
    val weather = if (tile == deviceTile && requestTime == null) deviceWeather
        else rememberSpaceCompassSunWeather(tile, resumed, selectedTimeMs = requestTime)
    // Warm viewed catalogue rows, with selected objects/Observatory first. Never flood JPL
    // with thousands of unseen targets; each downloaded model keeps its own offline cache.
    var visibleCatalogBodies by remember { mutableStateOf<Set<SpaceCompassCelestialBody>>(emptySet()) }
    val observedBodies = remember(selectedBodies, comparisonEntries, catalogViewerBody) {
        selectedBodies + comparisonEntries.map { it.body } + listOfNotNull(catalogViewerBody)
    }
    val remote = rememberSpaceCompassCelestialRemote(observedBodies, resumed, motionBodies = observedBodies,
        retryRevision = remoteRetry, prefetchCatalog = true,
        observationTimeOverrideMs = requestTime, catalogBodies = visibleCatalogBodies)
    BackHandler(onBack = onDismissRequest)
    val comparisonOverlays = rememberSpaceCompassComparisonOverlays(comparisonEntries,
        visibleReadings.location?.latitude?.let { round(it * 10_000) / 10_000 },
        visibleReadings.location?.longitude?.let { round(it * 10_000) / 10_000 },
        round((visibleReadings.location?.takeIf { it.hasAltitude() }?.altitude ?: 0.0) / 10) * 10, remote)
    CompositionLocalProvider(LocalSpaceCompassCatalogOpen provides catalogOpen,
        LocalSpaceCompassComparisonOverlays provides comparisonOverlays) {
    Box(Modifier.fillMaxSize()) {
    // Retain data owners without drawing or exposing the hidden compass to accessibility.
    Box(Modifier.fillMaxSize().layout { measurable, constraints ->
        val placeable = measurable.measure(constraints)
        layout(placeable.width, placeable.height) {
            if (!catalogOpen.value) placeable.placeRelative(0, 0)
        }
    }) {
    SpaceCompassSunFinderContent(visibleReadings,
        observationTime, primaryText, secondaryText, backgroundColor,
        weather = weather, deviceReadings = deviceReadings, deviceTimeMs = time, deviceWeather = deviceWeather,
        remote = remote, body = resolvedBody, onBodyChange = { comparisons?.activeId = null; applySelection(SpaceCompassCelestialSelection(selectedBodies, it)) },
        selectedBodies = selectedBodies, onSelectionChange = ::applySelection, onDismissRequest = onDismissRequest,
        resumed = resumed && !catalogOpen.value, onRemoteRetry = { remoteRetry++ }) {
        val intent = when (visibleReadings.locationStatus) {
            SpaceCompassSunLocationStatus.PERMISSION -> Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                Uri.parse("package:${context.packageName}"))
            SpaceCompassSunLocationStatus.DISABLED -> Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)
            else -> null
        }
        if (intent == null) retry++ else runCatching { context.startActivity(intent) }
            .onFailure { SpaceCompassErrorLog.record(context, "sun_finder:settings", it) }
    }
    }
    if (catalogOpen.value) {
        val selection = SpaceCompassCelestialSelection(selectedBodies, resolvedBody.takeIf { it in selectedBodies })
        // Keep the catalog, search and scroll position alive underneath its independent viewer.
        Box(Modifier.fillMaxSize().layout { measurable, constraints ->
            val placeable = measurable.measure(constraints)
            layout(placeable.width, placeable.height) {
                if (catalogViewerBody == null) placeable.placeRelative(0, 0)
            }
        }) {
            SpaceCompassCelestialCatalogPage(observationTime, remote, selectedBodies, primaryText, backgroundColor,
                onBack = { catalogOpen.value = false },
                onToggleAll = { applySelection(selection.toggleVisible(it)) },
                onSelect = {
                    if (it !in selectedBodies) comparisons?.activeId = null
                    applySelection(selection.toggle(it))
                },
                latitude = visibleReadings.location?.latitude, longitude = visibleReadings.location?.longitude,
                altitude = visibleReadings.location?.takeIf { it.hasAltitude() }?.altitude ?: 0.0,
                onObserve = { catalogViewerBody = it }, onDataNeeded = { visibleCatalogBodies = it })
        }
        catalogViewerBody?.let { observedBody ->
            key(observedBody) {
                SpaceCompassCelestialViewerScreen(observedBody, observationTime,
                    visibleReadings.location?.latitude, visibleReadings.location?.longitude,
                    visibleReadings.location?.takeIf { it.hasAltitude() && it.altitude.isFinite() &&
                        it.altitude in -500.0..20_000.0 }?.altitude ?: 0.0,
                    remote, primaryText, secondaryText, backgroundColor) { catalogViewerBody = null }
            }
        }
    }
    }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun SpaceCompassSunFinderContent(
    readings: SpaceCompassSunFinderReadings, timeMs: Long, primaryText: Color,
    secondaryText: Color, backgroundColor: Color, weather: SpaceCompassSunWeatherReading = SpaceCompassSunWeatherReading(),
    remote: SpaceCompassCelestialRemoteData = SpaceCompassCelestialRemoteData(), body: SpaceCompassCelestialBody = SpaceCompassCelestialBody.SUN,
    onBodyChange: (SpaceCompassCelestialBody) -> Unit = {},
    selectedBodies: Set<SpaceCompassCelestialBody> = setOf(body),
    onSelectionChange: ((SpaceCompassCelestialSelection) -> Unit)? = null,
    onDismissRequest: (() -> Unit)? = null, resumed: Boolean = true,
    deviceReadings: SpaceCompassSunFinderReadings = readings, deviceTimeMs: Long = timeMs,
    deviceWeather: SpaceCompassSunWeatherReading = weather,
    onRemoteRetry: () -> Unit = {}, onLocationAction: () -> Unit = {}
) {
    val comparisons = LocalSpaceCompassComparisons.current
    val comparisonOverlays = LocalSpaceCompassComparisonOverlays.current
    val activeComparison = comparisonOverlays.activeSpaceCompassComparison(comparisons?.activeId)
        ?: comparisonOverlays.firstOrNull()?.comparison?.takeIf { selectedBodies.isEmpty() }
    val sceneBody = activeComparison?.body ?: body
    val entryTimeMs = activeComparison?.timeMs ?: timeMs
    val dailyPathUiState = rememberSpaceCompassSunDailyPathUiState()
    val cameraContext = LocalContext.current
    val controlResources = LocalResources.current
    fun notifyControl(id: Int, vararg arguments: Any) =
        showSpaceCompassBottomMessage(cameraContext, controlResources.getString(id, *arguments))
    val tutorial = LocalSpaceCompassTutorial.current
    val tutorialActive = tutorial?.active == true
    // Camera is opt-in on every cold app start; never restore it from preferences or saved state.
    var requestedCameraEnabled by remember { mutableStateOf(false) }
    val cameraEnabled = spaceCompassTutorialCameraEnabled(requestedCameraEnabled, tutorialActive)
    val cameraControlsMode = if (tutorialActive) tutorial?.cameraPreview == true else cameraEnabled
    val scenePreferences = LocalSpaceCompassPreferences.current ?: remember(cameraContext) {
        cameraContext.getSharedPreferences(SPACE_COMPASS_PREFERENCES_NAME, android.content.Context.MODE_PRIVATE)
    }
    var detailsExpanded by remember(scenePreferences) {
        mutableStateOf(scenePreferences.getBoolean(SPACE_COMPASS_MAIN_DETAILS_KEY, true))
    }
    var showSkyReferences by remember(scenePreferences) {
        mutableStateOf(scenePreferences.getBoolean(SPACE_COMPASS_SKY_REFERENCES_KEY, SPACE_COMPASS_SKY_REFERENCES_DEFAULT))
    }
    var showWeather by remember(scenePreferences) {
        mutableStateOf(scenePreferences.getBoolean(SPACE_COMPASS_WEATHER_VISIBLE_KEY, SPACE_COMPASS_WEATHER_VISIBLE_DEFAULT))
    }
    var forward24Hours by remember(scenePreferences) {
        mutableStateOf(scenePreferences.getBoolean(SPACE_COMPASS_FORWARD_PATH_KEY,false))
    }
    val showAntipodes = dailyPathUiState.antipodesVisible
    var skyControlsVisible by remember(scenePreferences) {
        mutableStateOf(scenePreferences.getBoolean(SPACE_COMPASS_SKY_CONTROLS_VISIBLE_KEY, true))
    }
    val showReticle = skyControlsVisible || tutorialActive
    val showSkyControls = skyControlsVisible || tutorialActive
    var pointingTopEdge by remember(scenePreferences) {
        mutableStateOf(scenePreferences.getBoolean(SPACE_COMPASS_POINTING_TOP_EDGE_KEY, SPACE_COMPASS_POINTING_TOP_EDGE_DEFAULT))
    }
    var translucentCameraObjects by remember(scenePreferences) {
        mutableStateOf(scenePreferences.getBoolean(SPACE_COMPASS_CAMERA_OBJECT_TRANSLUCENCY_KEY,false))
    }
    DisposableEffect(scenePreferences) {
        val listener = android.content.SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
            when (key) {
                SPACE_COMPASS_SKY_REFERENCES_KEY -> showSkyReferences = scenePreferences.getBoolean(key,SPACE_COMPASS_SKY_REFERENCES_DEFAULT)
                SPACE_COMPASS_FORWARD_PATH_KEY -> forward24Hours = scenePreferences.getBoolean(key,false)
                SPACE_COMPASS_WEATHER_VISIBLE_KEY -> showWeather = scenePreferences.getBoolean(key,SPACE_COMPASS_WEATHER_VISIBLE_DEFAULT)
                SPACE_COMPASS_CAMERA_OBJECT_TRANSLUCENCY_KEY -> translucentCameraObjects = scenePreferences.getBoolean(key,false)
            }
        }
        scenePreferences.registerOnSharedPreferenceChangeListener(listener)
        onDispose { scenePreferences.unregisterOnSharedPreferenceChangeListener(listener) }
    }
    val cameraObjectOpacity = spaceCompassCameraObjectOpacity(cameraEnabled,translucentCameraObjects)
    val deviceView = LocalView.current
    var cameraPerspective by remember { mutableStateOf<SpaceCompassPerspective?>(null) }
    var cameraCapture by remember { mutableStateOf<SpaceCompassCameraCapture?>(null) }
    val savedCameraZoom = remember(scenePreferences) {
        val stored = try { scenePreferences.getFloat(SPACE_COMPASS_CAMERA_ZOOM_KEY, SPACE_COMPASS_CAMERA_ZOOM_DEFAULT) }
            catch (_: ClassCastException) { SPACE_COMPASS_CAMERA_ZOOM_DEFAULT }
        stored.takeIf { it.isFinite() && it > 0f } ?: SPACE_COMPASS_CAMERA_ZOOM_DEFAULT
    }
    var cameraZoomRequest by remember(scenePreferences) { mutableFloatStateOf(savedCameraZoom) }
    var cameraZoomActual by remember(scenePreferences) { mutableFloatStateOf(savedCameraZoom) }
    var cameraZoomRange by remember { mutableStateOf<SpaceCompassCameraZoomRange?>(null) }
    fun updateCameraZoom(value: Float) {
        val next = cameraZoomRange?.snap(value) ?: value
        if (!next.isFinite() || next <= 0f || next == cameraZoomRequest) return
        cameraZoomRequest = next
        // Persist user detents, never transient capture-result zoom on every camera frame.
        scenePreferences.edit().putFloat(SPACE_COMPASS_CAMERA_ZOOM_KEY, next).apply()
    }
    var virtualZoom by remember(scenePreferences) {
        val stored = try { scenePreferences.getFloat(SPACE_COMPASS_VIRTUAL_ZOOM_KEY, SPACE_COMPASS_VIRTUAL_ZOOM_DEFAULT) }
            catch (_: ClassCastException) { SPACE_COMPASS_VIRTUAL_ZOOM_DEFAULT }
        mutableFloatStateOf(spaceCompassVirtualZoom(stored))
    }
    fun updateVirtualZoom(value: Float) {
        val next = spaceCompassVirtualZoom(value)
        if (next == virtualZoom) return
        virtualZoom = next
        scenePreferences.edit().putFloat(SPACE_COMPASS_VIRTUAL_ZOOM_KEY, next).apply()
    }
    val cameraAttitudes = remember { SpaceCompassCameraAttitudeHistory() }
    SideEffect { readings.cameraAttitude?.let(cameraAttitudes::add) }
    val cameraPermissionMessage = stringResource(R.string.camera_permission)
    val cameraUnavailableMessage = stringResource(R.string.camera_unavailable)
    val cameraPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        requestedCameraEnabled = granted
        if (granted) notifyControl(R.string.scene_mode_camera)
        else showSpaceCompassBottomMessage(cameraContext, cameraPermissionMessage)
    }
    val mainVisible = LocalSpaceCompassMainVisible.current
    val displayRotation = deviceView.display?.rotation ?: 0
    val visualPose = rememberSpaceCompassSceneResumePose(readings.orientation, readings.compassUsable,
        displayRotation, resumed && mainVisible)
    LaunchedEffect(resumed, cameraEnabled) {
        if (resumed && cameraEnabled && !hasSpaceCompassCameraPermission(cameraContext)) requestedCameraEnabled = false
    }
    val skyObserver = LocalSpaceCompassObserver.current
    val observerPlan = skyObserver?.plan
    // Pins are reprojected from refreshed curves for the effective observer; navigating Location must not erase comparisons.
    var showEnvironment by rememberSaveable { mutableStateOf(false) }
    var showViewer by rememberSaveable { mutableStateOf(false) }
    var viewerBodyName by rememberSaveable { mutableStateOf(body.name) }
    var viewerComparison by rememberSaveable { mutableStateOf<SpaceCompassTemporalComparison?>(null) }
    fun openViewer() {
        viewerBodyName = sceneBody.name
        viewerComparison = activeComparison
        showViewer = true
    }
    LaunchedEffect(tutorialActive) {
        if (tutorialActive) { showViewer = false; showEnvironment = false; dailyPathUiState.closeMenu() }
    }
    val hasActiveBody = activeComparison != null || sceneBody in selectedBodies
    val bodyName = if (activeComparison != null) spaceCompassComparisonDisplayName(activeComparison) else if (hasActiveBody) stringResource(sceneBody.nameResource) else "—"
    val fix = readings.location
    // Device details are independent of the effective observer used for celestial calculations.
    val deviceFix = deviceReadings.location
    val deviceAltitude = deviceFix?.takeIf { it.hasAltitude() && it.altitude.isFinite() && it.altitude in -500.0..20_000.0 }?.altitude
    val estimatedPlace = rememberSpaceCompassEstimatedPlace(deviceFix?.latitude, deviceFix?.longitude,
        enabled = resumed && (showEnvironment || observerPlan?.simulatePosition != true))
    val altitude = fix?.takeIf { it.hasAltitude() && it.altitude.isFinite() && it.altitude in -500.0..20_000.0 }?.altitude
    val sun = remember(timeMs, fix?.latitude, fix?.longitude, altitude) {
        fix?.let { calculateSpaceCompassSunPosition(timeMs, it.latitude, it.longitude, altitude ?: 0.0) }
    }
    val rising = remember(timeMs, fix?.latitude, fix?.longitude, altitude) {
        fix != null && sun != null && calculateSpaceCompassSunPosition(timeMs + 60_000L,
            fix.latitude, fix.longitude, altitude ?: 0.0).elevationDegrees >= sun.elevationDegrees
    }
    val phase = spaceCompassSunSkyPhase(sun?.elevationDegrees, rising)
    val solarLighting = remember(sun?.elevationDegrees) { spaceCompassSolarLighting(sun?.elevationDegrees) }
    val publishSolarObserver = LocalSpaceCompassScreensaverSolarObserver.current
    val solarObserver = remember(deviceFix?.latitude, deviceFix?.longitude, deviceAltitude) {
        deviceFix?.let { SpaceCompassScreensaverSolarObserver(it.latitude, it.longitude, deviceAltitude ?: 0.0) }
    }
    SideEffect { publishSolarObserver(solarObserver) }
    val zone = spaceCompassObservationZone()
    val scenarioMomentLabel = spaceCompassScenarioMomentLabel(observerPlan, timeMs, deviceTimeMs, zone, skyObserver?.playbackPaused == true)
    val date = Instant.ofEpochMilli(timeMs).atZone(zone).toLocalDate()
    // Use the Location clock/zone, including accelerated forward/rewind, rather than device midnight.
    SideEffect { dailyPathUiState.refreshObservationDate(date) }
    // Approximately 11 m location cells keep GPS jitter from rebuilding a whole day's curve.
    // All ephemeris work runs off the UI thread; orientation merely projects the cached vectors.
    val pathLatitude = fix?.latitude?.let { round(it * 10_000) / 10_000 }
    val pathLongitude = fix?.longitude?.let { round(it * 10_000) / 10_000 }
    val pathAltitude = round((altitude ?: 0.0) / 10) * 10
    val allOverlays = rememberSpaceCompassCelestialOverlays(selectedBodies, timeMs, date, zone,
        pathLatitude, pathLongitude, pathAltitude, remote, pathBodies = selectedBodies, forward24Hours = forward24Hours)
    val approachOverlays = rememberSpaceCompassApproachOverlays(
        spaceCompassSelectedCelestialEntries(allOverlays, selectedBodies), pathLatitude, pathLongitude, pathAltitude, remote, timeMs)
    val selectedOverlays = rememberSpaceCompassForwardOverlays(approachOverlays,forward24Hours,timeMs,
        pathLatitude,pathLongitude,pathAltitude,remote)
    val currentWeather = weather.snapshot?.takeIf { sun != null && spaceCompassSunWeatherSnapshotUsable(it, timeMs) }
    val sceneWeather = currentWeather.takeIf { showWeather }
    // Data producers stay composed while a child page is visible: returning reuses their caches.
    if (showViewer || dailyPathUiState.menuPath != null) {
        val displayedBody = dailyPathUiState.menuPath?.body ?: SpaceCompassCelestialBody.valueOf(viewerBodyName)
        val shownComparison = dailyPathUiState.menuPath?.comparison ?: viewerComparison
        CompositionLocalProvider(LocalSpaceCompassObservationZoneOverride provides shownComparison?.zone) {
        SpaceCompassCelestialViewerScreen(displayedBody, shownComparison?.timeMs ?: timeMs, fix?.latitude, fix?.longitude,
            altitude ?: 0.0, remote, primaryText, secondaryText, backgroundColor,
            sharedPathState = dailyPathUiState, overlay = if (shownComparison == null) selectedOverlays[displayedBody]
                else comparisonOverlays.firstOrNull { it.comparison.id == shownComparison.id }?.overlay,
            startWithPath = !showViewer,
            dailyObservation = SpaceCompassLocationDailyObservation(timeMs, zone, selectedOverlays[displayedBody]),
            viewComparisonId = shownComparison?.id,
            exportLocation = fix?.let { SpaceCompassExportLocation(it.latitude, it.longitude, altitude,
                it.accuracy.toDouble().takeIf { _ -> it.hasAccuracy() }) }) {
            showViewer = false
            dailyPathUiState.closeMenu()
        }
        }
        return
    }
    // Catalogue calculations are not visibility: unchecked live positions must not enter the scene.
    val activeOverlay = if (activeComparison == null) selectedOverlays[sceneBody] else
        comparisonOverlays.firstOrNull { it.comparison.id == activeComparison.id }?.overlay
    val target = if (!hasActiveBody) null else if (activeComparison == null && sceneBody == SpaceCompassCelestialBody.SUN) sun else activeOverlay?.observation?.position
    val speed = activeOverlay?.speedKmSecond
    val shownDistance = remember(sceneBody, activeOverlay?.distanceKm, entryTimeMs, remote, fix?.latitude, fix?.longitude, altitude) {
        spaceCompassDisplayObserverDistanceKm(sceneBody, activeOverlay?.distanceKm, entryTimeMs, remote,
            fix?.latitude, fix?.longitude, altitude ?: 0.0)
    }
    val dailyPath = activeOverlay?.path
    val liveTarget = target.takeIf { activeComparison == null }
    val livePath = dailyPath.takeIf { activeComparison == null }
    val pointingUnavailable = hasActiveBody && target == null && (activeComparison?.hasReference != false || dailyPath == null)
    val overlays = if (activeComparison == null) selectedOverlays - sceneBody else selectedOverlays
    val numeric = LocalSpaceCompassNumericFormat.current
    val controlLocale = LocalSpaceCompassDeviceLocale.current
    val units = LocalSpaceCompassUnits.current
    val largeText = LocalDensity.current.fontScale > 1.4f
    fun angle(value: Double?) = value?.let { "${formatSpaceCompassNumber(it, 1, numeric)}°" } ?: "—"
    val message = when {
        readings.locationStatus == SpaceCompassSunLocationStatus.PERMISSION -> R.string.sun_finder_location_permission
        readings.locationStatus == SpaceCompassSunLocationStatus.DISABLED -> R.string.sun_finder_location_disabled
        readings.locationStatus == SpaceCompassSunLocationStatus.ERROR -> R.string.sun_finder_location_error
        sun == null -> R.string.sun_finder_location_searching
        !readings.compassAvailable -> R.string.sun_finder_compass_missing
        readings.orientation == null -> R.string.sun_finder_orientation_searching
        !readings.compassReliable -> when (readings.compassIssue) {
            SpaceCompassSunCompassIssue.MAGNETIC_INTERFERENCE -> R.string.sun_finder_compass_accuracy
            SpaceCompassSunCompassIssue.CALIBRATION,
            SpaceCompassSunCompassIssue.REDUCED_ACCURACY -> R.string.sun_finder_compass_calibrate
            else -> R.string.sun_finder_orientation_searching
        }
        pointingUnavailable -> if (remote.loading) R.string.celestial_loading else R.string.celestial_unavailable
        sceneBody.isEarthSatellite && remote.satelliteOrbit(sceneBody)?.let { timeMs - it.epochMs > SPACE_COMPASS_ISS_WARNING_AGE_MS } == true -> R.string.celestial_satellite_old
        else -> null
    }
    val isCompassMessage = when (message) {
        R.string.sun_finder_compass_accuracy, R.string.sun_finder_compass_calibrate,
        R.string.sun_finder_orientation_searching -> true
        else -> false
    }
    val messageText = message?.let {
        if (it == R.string.celestial_unavailable) stringResource(it, bodyName)
        else stringResource(it)
    }
    val dateOutsideRemoteData = hasActiveBody && spaceCompassCelestialDataOutsideDate(sceneBody, entryTimeMs, remote)
    val remoteMessageId = when {
        pointingUnavailable && sun != null && !remote.loading && dateOutsideRemoteData ->
            R.string.celestial_date_unavailable
        hasActiveBody && target == null && sun != null -> if (remote.loading) R.string.celestial_loading
            else R.string.celestial_data_unavailable_compact
        sceneBody.isEarthSatellite && remote.satelliteOrbit(sceneBody)?.let { timeMs - it.epochMs > SPACE_COMPASS_ISS_WARNING_AGE_MS } == true ->
            R.string.celestial_satellite_old
        else -> null
    }
    val remoteMessage = remoteMessageId?.let { stringResource(it) }
    val orientation = spaceCompassSunViewOrientation(readings.orientation, pointingTopEdge, cameraEnabled,
        displayRotation)
    val pointingOrientation = spaceCompassSunTrustedPointingOrientation(orientation, readings.compassUsable)
    // Keep the task snapshot and resume frames intact; live readouts and hit targets still use sensor trust.
    val sceneOrientation = if (cameraEnabled) orientation else
        spaceCompassSunViewOrientation(visualPose.orientation, pointingTopEdge, false, displayRotation)
    val scenePointingOrientation = if (cameraEnabled) pointingOrientation else
        spaceCompassSunViewOrientation(visualPose.pointing, pointingTopEdge, false, displayRotation)
    val deviceOrientation = spaceCompassSunViewOrientation(deviceReadings.orientation, pointingTopEdge, cameraEnabled, displayRotation)
    val devicePointingOrientation = spaceCompassSunTrustedPointingOrientation(deviceOrientation, deviceReadings.compassUsable)
    val deviceVerticalAccuracy = deviceFix?.takeIf { it.hasVerticalAccuracy() }?.verticalAccuracyMeters?.toDouble()
    val deviceHeight = formatSpaceCompassCelestialAltitude(deviceAltitude, deviceVerticalAccuracy, numeric, units.feet) ?: "—"
    val deviceHeightSummary = formatSpaceCompassCelestialAltitude(deviceAltitude, null, numeric, units.feet) ?: "—"
    val orientationRows = listOf(
        spaceCompassSunDataRow(stringResource(R.string.sun_finder_heading, SPACE_COMPASS_SUN_DATA_MARKER),
            angle(spaceCompassSunPointingHeading(devicePointingOrientation)), "sun-data-heading"),
        spaceCompassSunDataRow(stringResource(R.string.sun_finder_tilt, SPACE_COMPASS_SUN_DATA_MARKER),
            angle(deviceOrientation?.tiltDegrees), "sun-data-tilt")
    )
    val speedTemplate = stringResource(if (sceneBody.isExtrasolar)
        R.string.celestial_table_speed else spaceCompassCelestialSpeedLabel(sceneBody), SPACE_COMPASS_SUN_DATA_MARKER)
    val rows = if (!hasActiveBody) emptyList() else listOf(
        spaceCompassSunOptionalDataRow(stringResource(R.string.celestial_table_distance, SPACE_COMPASS_SUN_DATA_MARKER),
            formatSpaceCompassSelectedDistance(sceneBody, shownDistance, numeric, units.distance, units.feet), "celestial-distance"),
        spaceCompassSunOptionalDataRow(if (units.miles) speedTemplate.replace("km/s", "mi/s") else speedTemplate,
            formatSpaceCompassSpeed(speed, numeric, units.miles, allowNegative = sceneBody.isVoyager),
            "celestial-speed"),
        spaceCompassSunDataRow(stringResource(R.string.celestial_point_azimuth, SPACE_COMPASS_SUN_DATA_MARKER),
            angle(target?.azimuthDegrees), "sun-data-azimuth"),
        spaceCompassSunDataRow(stringResource(R.string.celestial_point_elevation, SPACE_COMPASS_SUN_DATA_MARKER),
            angle(target?.elevationDegrees), "sun-data-elevation")
    )
    val locationRows = listOf(
        spaceCompassSunDataRow(stringResource(R.string.sun_finder_altitude, SPACE_COMPASS_SUN_DATA_MARKER), deviceHeightSummary, "sun-data-altitude")
    )
    val locationInfoRows = listOf(
        spaceCompassSunOptionalDataRow(stringResource(R.string.celestial_gps_coordinates, SPACE_COMPASS_SUN_DATA_MARKER),
            formatSpaceCompassSelectedCoordinates(deviceFix?.latitude, deviceFix?.longitude, numeric, units.dms), "sun-info-coordinates"),
        spaceCompassSunOptionalDataRow(stringResource(R.string.celestial_gps_coordinate_accuracy, SPACE_COMPASS_SUN_DATA_MARKER),
            deviceFix?.takeIf { it.hasAccuracy() && it.accuracy.isFinite() && it.accuracy >= 0f }
                ?.let { formatSpaceCompassPhysicalLength(it.accuracy.toDouble(), 0, numeric, units.feet) }, "sun-info-accuracy"),
        spaceCompassSunDataRow(stringResource(R.string.sun_finder_altitude, SPACE_COMPASS_SUN_DATA_MARKER), deviceHeight, "sun-info-altitude"),
        spaceCompassSunDataRow(stringResource(R.string.environment_estimated_place, SPACE_COMPASS_SUN_DATA_MARKER),
            estimatedPlace.text ?: stringResource(if (estimatedPlace.loading)
                R.string.environment_place_loading else R.string.environment_place_unavailable), "sun-info-estimated-place")
    )
    val currentDeviceWeather = deviceWeather.snapshot?.takeIf { deviceFix != null && spaceCompassSunWeatherSnapshotUsable(it, deviceTimeMs) }
    val weatherValue = stringResource(spaceCompassWeatherValueResource(currentDeviceWeather, deviceWeather.loading))
    val weatherRow = spaceCompassSunDataRow(stringResource(R.string.sun_weather_estimate,
        SPACE_COMPASS_SUN_DATA_MARKER), weatherValue, "celestial-environment-weather")
    val weatherText = if (currentDeviceWeather == null) weatherValue else weatherRow.announcement
    val panorama = rememberSpaceCompassPanoramaAction(timeMs, fix?.latitude, fix?.longitude, altitude,
        phase, currentWeather, selectedBodies, selectedOverlays, remote,
        gpsAccuracyMeters = fix?.takeIf { it.hasAccuracy() && it.accuracy.isFinite() && it.accuracy >= 0f }
            ?.accuracy?.toDouble(), cameraEnabled = cameraEnabled, cameraCapture = cameraCapture, cameraObjectOpacity = cameraObjectOpacity, showSkyReferences = showSkyReferences, showWeather = showWeather, solarLighting = solarLighting,
        selectedPoints = dailyPathUiState.selectedPoints, activeBody = sceneBody.takeIf { it in selectedBodies },
        activeComparisonId = activeComparison?.id, forward24Hours = forward24Hours, showAntipodes = showAntipodes)
    panorama.preview?.let { preview ->
        SpaceCompassPanoramaPreview(preview, panorama.closePreview)
        return
    }
    if (showEnvironment) {
        SpaceCompassSunFinderModelInfo(currentDeviceWeather != null, weatherRow, locationInfoRows,
            primaryText, secondaryText, backgroundColor, latitude = deviceFix?.latitude, longitude = deviceFix?.longitude) { showEnvironment = false }
        return
    }
    val locationActionLabel = (if (sun != null) null else when (readings.locationStatus) {
        SpaceCompassSunLocationStatus.PERMISSION -> R.string.sun_finder_allow_location
        SpaceCompassSunLocationStatus.DISABLED -> R.string.sun_finder_enable_location
        SpaceCompassSunLocationStatus.ERROR -> R.string.sun_finder_retry
        SpaceCompassSunLocationStatus.SEARCHING, SpaceCompassSunLocationStatus.READY -> null
    })?.let { stringResource(it) }
    val showWeatherFailure = spaceCompassWeatherFailureVisible(weather, showWeather, cameraEnabled,
        remoteMessage != null || messageText != null && !isCompassMessage)
    val weatherFailureMessage = if (showWeatherFailure) stringResource(R.string.sun_finder_weather_unavailable) else null
    var sceneCoordinates by remember { mutableStateOf<LayoutCoordinates?>(null) }
    var pointingCoordinates by remember { mutableStateOf<LayoutCoordinates?>(null) }
    var pointingFrame by remember { mutableStateOf<SpaceCompassSunSceneFrame?>(null) }
    var groundFrame by remember { mutableStateOf<SpaceCompassSunSceneFrame?>(null) }
    val density = LocalDensity.current
    fun refreshGroundFrame() {
        val scene = sceneCoordinates ?: return
        val pointing = pointingCoordinates ?: return
        if (!scene.isAttached || !pointing.isAttached) return
        val bounds = scene.localBoundingBoxOf(pointing, clipBounds = false)
        val viewport = SpaceCompassSunSceneFrame(bounds.left.toDouble(), bounds.top.toDouble(),
            bounds.width.toDouble(), bounds.height.toDouble())
        pointingFrame = viewport
        groundFrame = spaceCompassMainReticleOpticalFrame(viewport, scene.size.height.toDouble(),
            landscape = scene.size.width > scene.size.height, detailsExpanded = detailsExpanded,
            detailsGap = with(density) { 10.dp.toPx().toDouble() })
    }
    val scenePerspective = if (cameraEnabled) cameraPerspective else groundFrame?.let {
        spaceCompassVirtualPerspective(it.width, it.height, virtualZoom)
    }
    val pointingPlacement = Modifier.onGloballyPositioned {
        pointingCoordinates = it
        refreshGroundFrame()
    }
    val skyDescription = stringResource(if (cameraEnabled) R.string.camera_view_description else R.string.sun_finder_sky_description)
    val selection = remember(selectedBodies, sceneBody, hasActiveBody) {
        SpaceCompassCelestialSelection(selectedBodies, body.takeIf { it in selectedBodies } ?: selectedBodies.firstOrNull())
    }
    val navigationEntries = remember(selectedBodies, comparisonOverlays) {
        val dates = comparisonOverlays.map { it.comparison }
        val bodies = spaceCompassOrderedCelestialSubset(selectedBodies + dates.map { it.body })
        spaceCompassCatalogDisplayRows(bodies, dates, selectedBodies::contains)
    }
    val activeIndex = navigationEntries.indexOfFirst { it.body == sceneBody && it.comparison?.id == activeComparison?.id }.coerceAtLeast(0)
    fun navigateEntry(delta: Int) {
        if (navigationEntries.isEmpty()) return
        val entry = navigationEntries[Math.floorMod(activeIndex + delta, navigationEntries.size)]
        comparisons?.activeId = entry.comparison?.id
        if (entry.comparison == null) onBodyChange(entry.body)
    }
    val navigation = if (navigationEntries.size > 1) SpaceCompassCelestialBodyNavigation(
        previousLabel = stringResource(navigationEntries[Math.floorMod(activeIndex - 1, navigationEntries.size)].body.nameResource),
        nextLabel = stringResource(navigationEntries[(activeIndex + 1) % navigationEntries.size].body.nameResource),
        onPrevious = { navigateEntry(-1) }, onNext = { navigateEntry(1) }) else null
    val details: @Composable (Modifier, Boolean) -> Unit = { modifier, compact ->
        Column(modifier.testTag("celestial-details-column"), horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)) {
            SpaceCompassSunFinderDataPanel(rows, locationRows, deviceOrientation, deviceReadings.compassUsable,
                weatherText,
                currentDeviceWeather != null, primaryText, secondaryText, backgroundColor, Modifier.fillMaxWidth().weight(1f, fill = false), compact,
                bodyName = bodyName, orientationRows = orientationRows,
                onBodyNameClick = if (hasActiveBody) (::openViewer) else null,
                // Keep the object identity at its full above-horizon palette color.
                bodyNameColor = if (hasActiveBody) activeComparison?.let(::spaceCompassComparisonTint) ?: spaceCompassCelestialPathTint(sceneBody) else primaryText,
                bodyNavigation = navigation, locationInfoRows = locationInfoRows, onInfo = { showEnvironment = true })
        }
    }
    val navigate = LocalSpaceCompassNavigate.current
    val antipodesControl: @Composable () -> Unit = {
        val selectedPointCount = dailyPathUiState.selectedPoints.size
        if (selectedPointCount > 0 || tutorialActive) SpaceCompassAntipodesButton(
            showAntipodes, {
                if (tutorialActive) tutorial?.explore(SpaceCompassTutorialFeature.ANTIPODES)
                else {
                    dailyPathUiState.toggleAntipodes()
                    notifyControl(if (dailyPathUiState.antipodesVisible) R.string.scene_antipodes_on else R.string.scene_antipodes_off)
                }
            }, primaryText, backgroundColor, selectedCount = selectedPointCount.coerceAtLeast(1), onClear = {
                if (!tutorialActive && dailyPathUiState.selectedPoints.isNotEmpty()) {
                    dailyPathUiState.clearSelection()
                    notifyControl(R.string.scene_points_cleared)
                }
            })
    }
    val pointingAxisControl: @Composable () -> Unit = {
        SpaceCompassPointingAxisButton(pointingTopEdge, { topEdge ->
                pointingTopEdge = topEdge
                scenePreferences.edit().putBoolean(SPACE_COMPASS_POINTING_TOP_EDGE_KEY, topEdge).apply()
                notifyControl(R.string.control_choice_message, controlResources.getString(
                    if (topEdge) R.string.pointing_axis_top_edge else R.string.pointing_axis_camera),
                    formatSpaceCompassNumber(if (topEdge) 0.0 else 90.0, 0, numeric,
                        grouping = false, systemLocale = controlLocale) + "°")
            }, primaryText, backgroundColor, enabled = !cameraControlsMode)
    }
    val captureControls: @Composable (Modifier, Boolean, Boolean) -> Unit = { modifier, vertical, landscape ->
        CompositionLocalProvider(LocalSpaceCompassFloatingControlMultiplier provides SPACE_COMPASS_COMPACT_CONTROL_MULTIPLIER) {
            SpaceCompassMainCaptureControls(vertical, modifier,
                antipodes = { if (!landscape || vertical) antipodesControl() }, landscape = landscape, camera = {
                    SpaceCompassCameraToggleButton(cameraControlsMode, { enabled ->
                        if (tutorialActive) tutorial?.explore(SpaceCompassTutorialFeature.CAMERA)
                        else if (!enabled) {
                            requestedCameraEnabled = false
                            notifyControl(R.string.scene_mode_sky)
                        } else if (hasSpaceCompassCameraPermission(cameraContext)) {
                            requestedCameraEnabled = true
                            notifyControl(R.string.scene_mode_camera)
                        } else cameraPermission.launch(Manifest.permission.CAMERA)
                    }, primaryText, backgroundColor)
                },
                capture = {
                    SpaceCompassCaptureButton(if (tutorialActive) ({}) else panorama.capture,
                        tutorialActive || (!panorama.busy && (!cameraEnabled || cameraCapture != null)),
                        cameraEnabled = cameraEnabled, animateBorder = resumed && mainVisible)
                })
        }
    }

    var landscapeCaptureSize by remember(showSkyControls) { mutableStateOf(IntSize.Zero) }
    val pointing: @Composable (Boolean) -> Unit = { landscape ->
        BoxWithConstraints(Modifier.fillMaxSize().spaceCompassCameraPinchZoom(
            if (cameraEnabled) cameraZoomRange else spaceCompassVirtualZoomRange,
            if (cameraEnabled) cameraZoomRequest else virtualZoom,
            !tutorialActive && !panorama.busy, !cameraEnabled || cameraCapture != null,
            if (cameraEnabled) ::updateCameraZoom else ::updateVirtualZoom)) {
            val sideControls = landscape && !detailsExpanded
            val sideControlsMaxHeight = (maxHeight -
                (spaceCompassFloatingControlBaseSize * SPACE_COMPASS_COMPACT_CONTROL_MULTIPLIER + 16.dp) * 2)
                .coerceAtLeast(48.dp)
            val regularTopRowWidth = spaceCompassFloatingControlBaseSize * (6 * SPACE_COMPASS_COMPACT_CONTROL_MULTIPLIER) + 56.dp
            val topControlSpacing = if (maxWidth >= regularTopRowWidth) 8.dp else 4.dp
            CompositionLocalProvider(LocalSpaceCompassFloatingControlMultiplier provides SPACE_COMPASS_COMPACT_CONTROL_MULTIPLIER) {
                SpaceCompassSunPointingViewport(liveTarget, scenePointingOrientation.takeIf { !cameraEnabled || cameraPerspective != null },
                    Modifier.fillMaxSize().then(pointingPlacement), skyDescription, dailyPathUiState, rising,
                    readings.compassUsable, livePath, primaryText, secondaryText, backgroundColor, sceneBody, timeMs,
                    onVisualize = ::openViewer,
                    compassWarning = messageText.takeIf { isCompassMessage && resumed && mainVisible },
                    compassAccurate = readings.compassReliable,
                    statusMessage = remoteMessage ?: messageText.takeUnless { isCompassMessage } ?: weatherFailureMessage,
                    compassWarningAttention = message == R.string.sun_finder_compass_accuracy ||
                        message == R.string.sun_finder_compass_calibrate,
                    statusAttention = (remoteMessageId ?: message) == R.string.celestial_satellite_old,
                    statusActionLabel = if (showWeatherFailure) stringResource(R.string.sun_finder_retry)
                        else if (pointingUnavailable && sun != null && !remote.loading)
                        stringResource(R.string.sun_finder_retry) else locationActionLabel,
                    onStatusAction = if (showWeatherFailure) weather.onRetry
                        else if (hasActiveBody && target == null && sun != null) onRemoteRetry else onLocationAction,
                    showActions = false, offscreenBody = sceneBody.takeIf { hasActiveBody }, overlays = overlays,
                    approachOverlays = selectedOverlays, activeComparisonId = activeComparison?.id,
                    perspective = scenePerspective, noticesBelowReticle = true,
                    observerLatitude = fix?.latitude, showSkyReferences = showSkyReferences,
                    observerAltitude = fix?.takeIf { it.hasAltitude() }?.altitude ?: 0.0,
                    topContentInset = 10.dp, topControlSpacing = topControlSpacing, sideControls = sideControls,
                    projectionTopInset = 0.dp,
                    opticalFrame = pointingFrame?.let { viewport -> groundFrame?.let { optical ->
                        optical.copy(left = optical.left - viewport.left, top = optical.top - viewport.top)
                    } },
                    drawScene = false, objectOpacity = cameraObjectOpacity, showReticle = showReticle,
                    showFloatingControls = showSkyControls, showAntipodes = showAntipodes,
                    scenarioMomentLabel = scenarioMomentLabel,
                    timePaused = skyObserver?.playbackPaused == true,
                    onToggleTime = if (tutorialActive) null else ({ skyObserver?.togglePlayback() }),
                    onToggleFloatingControls = if (tutorialActive) null else ({
                        skyControlsVisible = !skyControlsVisible
                        scenePreferences.edit().putBoolean(SPACE_COMPASS_SKY_CONTROLS_VISIBLE_KEY, skyControlsVisible).apply()
                    }),
                    bottomStartActions = { modifier ->
                        // Keep panel and point-selection actions together at the bottom edge.
                        FlowRow(modifier.padding(4.dp).testTag("celestial-bottom-start-controls"),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            SpaceCompassMainDetailsToggleButton(detailsExpanded, {
                                detailsExpanded = !detailsExpanded
                                scenePreferences.edit().putBoolean(SPACE_COMPASS_MAIN_DETAILS_KEY, detailsExpanded).apply()
                            }, primaryText, backgroundColor, landscape = landscape)
                            if (!landscape) pointingAxisControl()
                        }
                    },
                    bottomCenterActions = { modifier ->
                        SpaceCompassMainViewModeControls(sideControls, modifier, sideControlsMaxHeight) {
                            if (cameraControlsMode) {
                                SpaceCompassCameraObjectTransparencyButton(translucentCameraObjects, { enabled ->
                                    if (!tutorialActive && translucentCameraObjects != enabled) {
                                        translucentCameraObjects = enabled
                                        scenePreferences.edit().putBoolean(SPACE_COMPASS_CAMERA_OBJECT_TRANSLUCENCY_KEY, enabled).apply()
                                        notifyControl(R.string.control_choice_message,
                                            controlResources.getString(R.string.camera_object_transparency),
                                            controlResources.getString(if (enabled) R.string.camera_objects_translucent else R.string.camera_objects_opaque))
                                    }
                                }, primaryText, backgroundColor, enabled = !panorama.busy)
                            } else SpaceCompassWeatherToggleButton(showWeather, { enabled ->
                                showWeather = enabled
                                scenePreferences.edit().putBoolean(SPACE_COMPASS_WEATHER_VISIBLE_KEY, enabled).apply()
                                notifyControl(if (enabled) R.string.scene_weather_on else R.string.scene_weather_off)
                            }, primaryText, backgroundColor)
                            if (tutorialActive) SpaceCompassTutorialCameraZoom(primaryText, backgroundColor)
                            else if (cameraEnabled) SpaceCompassCameraZoomControls(cameraZoomRange, cameraZoomRequest, cameraZoomActual,
                                !panorama.busy && cameraCapture != null, ::updateCameraZoom, primaryText, backgroundColor)
                            else SpaceCompassCameraZoomControls(spaceCompassVirtualZoomRange, virtualZoom, virtualZoom,
                                !panorama.busy, ::updateVirtualZoom, primaryText, backgroundColor, camera = false)
                        }
                    },
                    bottomActions = { modifier ->
                        if (!landscape) captureControls(modifier, false, false)
                        else if (!detailsExpanded) {
                            // Keep labels and edge locators clear of the capture overlay after expansion.
                            Spacer(modifier.size(with(density) { landscapeCaptureSize.width.toDp() },
                                with(density) { landscapeCaptureSize.height.toDp() }))
                        } else if (dailyPathUiState.selectedPoints.isNotEmpty() || tutorialActive) {
                            Box(modifier.padding(4.dp).testTag("celestial-expanded-landscape-points")) {
                                antipodesControl()
                            }
                        }
                    },
                    topStartActions = { modifier ->
                        Box(modifier.padding(if (onDismissRequest != null) 4.dp else 0.dp)
                            .testTag("celestial-top-start-controls")) {
                            if (onDismissRequest != null)
                                SpaceCompassMainInfoControl(primaryText, backgroundColor)
                        }
                    },
                    topEndActions = { modifier ->
                        Column(modifier.padding(if (onDismissRequest != null) 4.dp else 0.dp)
                            .testTag("celestial-top-end-controls"), horizontalAlignment = Alignment.End,
                            verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            if (onDismissRequest != null)
                                SpaceCompassMainMenuControl(primaryText, backgroundColor)
                            if (landscape) pointingAxisControl()
                        }
                    },
                    topCenterActions = { modifier ->
                        SpaceCompassMainNavigationControls(sideControls, modifier, topControlSpacing,
                            sideControlsMaxHeight,
                            objects = {
                                SpaceCompassCelestialSelector(sceneBody, primaryText, backgroundColor, Modifier, timeMs, remote,
                                    selectedBodies, onSelectionChange?.let { { it(selection.toggleAll()) } }) {
                                    if (onSelectionChange != null) onSelectionChange(selection.toggle(it)) else onBodyChange(it)
                                }
                            },
                            location = {
                                SpaceCompassScenarioButton(LocalSpaceCompassObserver.current?.plan.activeSectionCount, primaryText, backgroundColor) {
                                    navigate("observer")
                                }
                            },
                            guides = {
                                SpaceCompassSkyReferenceButton(showSkyReferences, { enabled ->
                                    showSkyReferences = enabled
                                    scenePreferences.edit().putBoolean(SPACE_COMPASS_SKY_REFERENCES_KEY, enabled).apply()
                                    notifyControl(if (enabled) R.string.scene_references_on else R.string.scene_references_off)
                                }, primaryText, backgroundColor)
                            },
                            interval = {
                                SpaceCompassForwardPathButton(forward24Hours, { enabled ->
                                    forward24Hours = enabled
                                    scenePreferences.edit().putBoolean(SPACE_COMPASS_FORWARD_PATH_KEY,enabled).apply()
                                    notifyControl(if (enabled) R.string.scene_forward_path_on else R.string.scene_forward_path_off)
                                },primaryText,backgroundColor)
                            }
                        )
                    })
            }
        }
    }
    BoxWithConstraints(Modifier.fillMaxSize().testTag("sun-finder-content").onGloballyPositioned {
        sceneCoordinates = it
        refreshGroundFrame()
    }) {
        val viewportWidth = maxWidth
        val viewportHeight = maxHeight
        if (cameraEnabled) {
            Box(Modifier.fillMaxSize().background(Color.Black))
            if (resumed && mainVisible) groundFrame?.let { frame ->
                SpaceCompassCameraPreview(frame, Modifier.fillMaxSize(), onPerspective = { cameraPerspective = it },
                    onCaptureReady = { cameraCapture = it }, attitudeAt = { stamp, rotation -> cameraAttitudes.at(stamp, rotation)?.let { it.copy(usable = it.usable && readings.compassUsable) } }, onError = {
                    requestedCameraEnabled = false
                    showSpaceCompassBottomMessage(cameraContext, cameraUnavailableMessage)
                }, zoom = cameraZoomRequest, onZoomRange = { range ->
                    cameraZoomRange = range
                    if (range != null) updateCameraZoom(cameraZoomRequest)
                },
                    onZoomActual = { cameraZoomActual = it })
            }
            SpaceCompassCameraHorizon(orientation, groundFrame, cameraPerspective, Modifier.fillMaxSize())
        } else {
            SpaceCompassSunSkyBackdrop(phase, sceneWeather, Modifier.fillMaxSize(), solarLighting) {
                SpaceCompassStarField(timeMs, fix?.latitude, fix?.longitude, altitude ?: 0.0, sceneOrientation, groundFrame,
                    sun?.elevationDegrees, sceneWeather, resumed && mainVisible, Modifier.fillMaxSize(), zoom = virtualZoom)
            }
            SpaceCompassSunGroundBackdrop(sceneOrientation, groundFrame, phase, Modifier.fillMaxSize(), solarLighting, scenePerspective)
        }
        SpaceCompassCelestialSceneBackdrop(groundFrame, scenePerspective,
            scenePointingOrientation.takeIf { !cameraEnabled || cameraPerspective != null },
            overlays, sceneBody.takeIf { hasActiveBody && activeComparison == null }, livePath, liveTarget, timeMs,
            dailyPathUiState, primaryText, secondaryText, backgroundColor,
            fix?.latitude, fix?.takeIf { it.hasAltitude() }?.altitude ?: 0.0, showSkyReferences,
            0.0, Modifier.fillMaxSize(), longitude = fix?.longitude, objectOpacity = cameraObjectOpacity,
            showReticle = showReticle, showAntipodes = showAntipodes)
        // Retain the readability fallback when the 45% information pane is too narrow.
        val minimumPanelWidth = if (largeText) 280.dp else 210.dp
        val detailsWidth = viewportWidth * 0.45f
        // Keep the islands at their regular size when the available height changes.
        val compactPanel = false
        if (viewportWidth > viewportHeight && detailsWidth - 20.dp >= minimumPanelWidth) {
            // Floating navigation stays in the sky column beside the full-height data column.
            Box(Modifier.fillMaxSize().testTag("celestial-landscape-layout")) {
            Row(Modifier.fillMaxSize()) {
                Box(Modifier.weight(1f).fillMaxHeight().clipToBounds()) {
                    // One full-height optical frame for sky, paths and camera.
                    Box(Modifier.fillMaxSize().padding(start = 10.dp, end = 10.dp, bottom = 10.dp)
                        .testTag("celestial-pointing-area")) {
                        pointing(true)
                    }
                }
                // Equal insets towards the sky and the outer edge, including in RTL.
                SpaceCompassMainDetailsVisibility(detailsExpanded, landscape = true) {
                    Column(Modifier.width(detailsWidth).fillMaxHeight().padding(10.dp)) {
                        details(Modifier.fillMaxWidth().weight(1f), compactPanel)
                        Spacer(Modifier.height(with(density) { landscapeCaptureSize.height.toDp() }))
                    }
                }
            }
            // One stable overlay stays outside the information panels' transition and changing sky width.
            if (showSkyControls) captureControls(Modifier.align(Alignment.BottomEnd).padding(10.dp)
                .onSizeChanged { landscapeCaptureSize = it }, !detailsExpanded, true)
            }
        } else Box(Modifier.fillMaxSize().clipToBounds()) {
            Column(Modifier.fillMaxSize().padding(start = 10.dp, end = 10.dp, bottom = 10.dp)) {
                Box(Modifier.weight(1f).fillMaxWidth().testTag("celestial-pointing-area")) {
                    pointing(false)
                }
                SpaceCompassMainDetailsVisibility(detailsExpanded, landscape = false) {
                    Column {
                        Spacer(Modifier.height(10.dp))
                        details(Modifier.fillMaxWidth().heightIn(max = viewportHeight * 0.56f), compactPanel)
                    }
                }
            }
        }
    }
}
