package me.mondiversi.spacecompass

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.*
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.zIndex
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
    var catalogViewerBody by rememberSaveable { mutableStateOf<SpaceCompassCelestialBody?>(null) }
    val context = LocalContext.current
    val unlockedMessage = stringResource(R.string.celestial_lv426_unlocked)
    val lifecycleOwner = LocalLifecycleOwner.current
    var permission by remember { mutableStateOf(hasSpaceCompassSunLocationPermission(context)) }
    var requested by rememberSaveable { mutableStateOf(false) }
    var retry by remember { mutableIntStateOf(0) }
    var remoteRetry by remember { mutableIntStateOf(0) }
    var catalogRefresh by remember { mutableIntStateOf(0) }
    var time by remember { mutableLongStateOf(System.currentTimeMillis()) }
    val preferences = LocalSpaceCompassPreferences.current
        ?: remember(context) { context.getSharedPreferences(SPACE_COMPASS_PREFERENCES_NAME, android.content.Context.MODE_PRIVATE) }
    val initialSelection = remember(preferences) { restoreSpaceCompassCelestialSelection(
        preferences.getStringSet("celestial_selected", null), preferences.getString("celestial_active", null)) }
    var selectedBody by rememberSaveable { mutableStateOf(initialSelection.active ?: SpaceCompassCelestialBody.SUN) }
    var selectedNames by rememberSaveable { mutableStateOf(initialSelection.ordered.map { it.name }) }
    val selectedBodies = remember(selectedNames) { spaceCompassAllCelestialOrder.filter { it.name in selectedNames }.toSet() }
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
    val selectionResources = LocalResources.current
    fun applySelection(selection: SpaceCompassCelestialSelection) {
        val nextNames = selection.ordered.map { it.name }
        val activationNotice = spaceCompassActivationNotice(selectedNames.isNotEmpty(), nextNames.isNotEmpty())
        selectedNames = nextNames
        selection.active?.let { selectedBody = it }
        // Save in the selection event, before a subsequent frame or app closure.
        preferences.edit().putStringSet("celestial_selected", selection.ordered.map { it.name }.toSet())
            .putString("celestial_active", selection.active?.name).apply()
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
    LaunchedEffect(plan?.simulatePosition) {
        if (!permission && !requested && plan?.simulatePosition != true) {
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
            delay(if ((observer?.timeSettings?.multiplier ?: 1) > 1) 50L else 500L)
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
    val weather = rememberSpaceCompassSunWeather(tile, resumed, selectedTimeMs = requestTime)
    // Warm the whole online catalog in the foreground; unchecked objects never become scene selections.
    val remote = rememberSpaceCompassCelestialRemote(selectedBodies, resumed, motionBodies = selectedBodies,
        retryRevision = remoteRetry, prefetchCatalog = true, catalogRefreshRevision = catalogRefresh,
        observationTimeOverrideMs = requestTime)
    BackHandler(onBack = onDismissRequest)
    CompositionLocalProvider(LocalSpaceCompassCatalogOpen provides catalogOpen) {
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
        weather = weather,
        remote = remote, body = resolvedBody, onBodyChange = { applySelection(SpaceCompassCelestialSelection(selectedBodies, it)) },
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
                onSelect = { applySelection(selection.toggle(it)) },
                latitude = visibleReadings.location?.latitude, longitude = visibleReadings.location?.longitude,
                altitude = visibleReadings.location?.takeIf { it.hasAltitude() }?.altitude ?: 0.0,
                onRefresh = { catalogRefresh++ }, onObserve = { catalogViewerBody = it }, onRevealHiddenObject = {
                    val revealed = selection.revealHiddenObject()
                    if (revealed != selection) {
                        applySelection(revealed)
                        showSpaceCompassBottomMessage(context,
                            unlockedMessage)
                    }
                })
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

@Composable
internal fun SpaceCompassSunFinderContent(
    readings: SpaceCompassSunFinderReadings, timeMs: Long, primaryText: Color,
    secondaryText: Color, backgroundColor: Color, weather: SpaceCompassSunWeatherReading = SpaceCompassSunWeatherReading(),
    remote: SpaceCompassCelestialRemoteData = SpaceCompassCelestialRemoteData(), body: SpaceCompassCelestialBody = SpaceCompassCelestialBody.SUN,
    onBodyChange: (SpaceCompassCelestialBody) -> Unit = {},
    selectedBodies: Set<SpaceCompassCelestialBody> = setOf(body),
    onSelectionChange: ((SpaceCompassCelestialSelection) -> Unit)? = null,
    onDismissRequest: (() -> Unit)? = null, resumed: Boolean = true,
    onRemoteRetry: () -> Unit = {}, onLocationAction: () -> Unit = {}
) {
    val dailyPathUiState = rememberSpaceCompassSunDailyPathUiState()
    val cameraContext = LocalContext.current
    val controlResources = LocalResources.current
    fun notifyControl(id: Int, vararg arguments: Any) =
        showSpaceCompassBottomMessage(cameraContext, controlResources.getString(id, *arguments))
    // Camera is opt-in on every cold app start; never restore it from preferences or saved state.
    var cameraEnabled by remember { mutableStateOf(false) }
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
    var pointingTopEdge by remember(scenePreferences) {
        mutableStateOf(scenePreferences.getBoolean(SPACE_COMPASS_POINTING_TOP_EDGE_KEY, false))
    }
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
    val cameraAttitudes = remember { SpaceCompassCameraAttitudeHistory() }
    SideEffect { readings.cameraAttitude?.let(cameraAttitudes::add) }
    val cameraPermissionMessage = stringResource(R.string.camera_permission)
    val cameraUnavailableMessage = stringResource(R.string.camera_unavailable)
    val cameraPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        cameraEnabled = granted
        if (granted) notifyControl(R.string.scene_mode_camera)
        else showSpaceCompassBottomMessage(cameraContext, cameraPermissionMessage)
    }
    val mainVisible = LocalSpaceCompassMainVisible.current
    val displayRotation = deviceView.display?.rotation ?: 0
    val visualPose = rememberSpaceCompassSceneResumePose(readings.orientation, readings.compassUsable,
        displayRotation, resumed && mainVisible)
    LaunchedEffect(resumed) {
        if (resumed && cameraEnabled && !hasSpaceCompassCameraPermission(cameraContext)) cameraEnabled = false
    }
    val observerPlan = LocalSpaceCompassObserver.current?.plan
    LaunchedEffect(observerPlan) { dailyPathUiState.clearSelection() }
    var showEnvironment by rememberSaveable { mutableStateOf(false) }
    var showViewer by rememberSaveable { mutableStateOf(false) }
    var removeTargetName by rememberSaveable { mutableStateOf<String?>(null) }
    val hasActiveBody = body in selectedBodies
    val bodyName = if (hasActiveBody) stringResource(body.nameResource) else "—"
    val fix = readings.location
    val estimatedPlace = rememberSpaceCompassEstimatedPlace(fix?.latitude, fix?.longitude,
        enabled = showEnvironment && resumed)
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
    val solarObserver = remember(fix?.latitude, fix?.longitude, altitude, observerPlan) {
        fix?.let { SpaceCompassScreensaverSolarObserver(it.latitude, it.longitude, altitude ?: 0.0,
            observerPlan = observerPlan) }
    }
    SideEffect { publishSolarObserver(solarObserver, observerPlan) }
    val zone = spaceCompassObservationZone()
    val date = Instant.ofEpochMilli(timeMs).atZone(zone).toLocalDate()
    // Approximately 11 m location cells keep GPS jitter from rebuilding a whole day's curve.
    // All ephemeris work runs off the UI thread; orientation merely projects the cached vectors.
    val pathLatitude = fix?.latitude?.let { round(it * 10_000) / 10_000 }
    val pathLongitude = fix?.longitude?.let { round(it * 10_000) / 10_000 }
    val pathAltitude = round((altitude ?: 0.0) / 10) * 10
    val allOverlays = rememberSpaceCompassCelestialOverlays(selectedBodies, timeMs, date, zone,
        pathLatitude, pathLongitude, pathAltitude, remote, pathBodies = selectedBodies)
    val selectedOverlays = rememberSpaceCompassApproachOverlays(
        spaceCompassSelectedCelestialEntries(allOverlays, selectedBodies), pathLatitude, pathLongitude, pathAltitude, remote, timeMs)
    val currentWeather = weather.snapshot?.takeIf { sun != null && spaceCompassSunWeatherSnapshotUsable(it, timeMs) }
    val sceneWeather = currentWeather.takeIf { showWeather }
    // Data producers stay composed while a child page is visible: returning reuses their caches.
    if (showViewer && hasActiveBody) {
        val location = readings.location
        val height = location?.takeIf { it.hasAltitude() && it.altitude.isFinite() && it.altitude in -500.0..20_000.0 }?.altitude ?: 0.0
        SpaceCompassCelestialViewerScreen(body, timeMs, location?.latitude, location?.longitude, height, remote,
            primaryText, secondaryText, backgroundColor) { showViewer = false }
        return
    }
    if (dailyPathUiState.menuPath != null) {
        val displayedBody = dailyPathUiState.menuPath!!.body
        val displayed = selectedOverlays[displayedBody]
        SideEffect { dailyPathUiState.refreshMenu(displayed?.path,
            spaceCompassCurrentPathPoint(displayedBody, timeMs, displayed?.observation?.position)) }
        SpaceCompassSunDailyPathPage(dailyPathUiState, primaryText, secondaryText, backgroundColor, nowMs = timeMs,
            location = fix?.let { SpaceCompassExportLocation(it.latitude,it.longitude,altitude,
                it.accuracy.toDouble().takeIf { _ -> it.hasAccuracy() }) })
        return
    }
    // Catalogue calculations are not visibility: unchecked live positions must not enter the scene.
    val activeOverlay = selectedOverlays[body]
    val target = if (!hasActiveBody) null else if (body == SpaceCompassCelestialBody.SUN) sun else activeOverlay?.observation?.position
    val speed = activeOverlay?.speedKmSecond
    val shownDistance = activeOverlay?.distanceKm
    val dailyPath = activeOverlay?.path
    val overlays = selectedOverlays - body
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
        hasActiveBody && target == null -> if (remote.loading) R.string.celestial_loading else R.string.celestial_unavailable
        body.isEarthSatellite && remote.satelliteOrbit(body)?.let { timeMs - it.epochMs > SPACE_COMPASS_ISS_WARNING_AGE_MS } == true -> R.string.celestial_satellite_old
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
    val dateOutsideRemoteData = hasActiveBody && spaceCompassCelestialDataOutsideDate(body, timeMs, remote)
    val remoteMessageId = when {
        hasActiveBody && target == null && sun != null && !remote.loading && dateOutsideRemoteData ->
            R.string.celestial_date_unavailable
        hasActiveBody && target == null && sun != null -> if (remote.loading) R.string.celestial_loading
            else R.string.celestial_data_unavailable_compact
        body.isEarthSatellite && remote.satelliteOrbit(body)?.let { timeMs - it.epochMs > SPACE_COMPASS_ISS_WARNING_AGE_MS } == true ->
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
    val height = formatSpaceCompassCelestialAltitude(altitude,
        fix?.takeIf { it.hasVerticalAccuracy() }?.verticalAccuracyMeters?.toDouble(), numeric, units.feet) ?: "—"
    val orientationRows = listOf(
        spaceCompassSunDataRow(stringResource(R.string.sun_finder_heading, SPACE_COMPASS_SUN_DATA_MARKER),
            angle(spaceCompassSunPointingHeading(pointingOrientation)), "sun-data-heading"),
        spaceCompassSunDataRow(stringResource(R.string.sun_finder_tilt, SPACE_COMPASS_SUN_DATA_MARKER),
            angle(orientation?.tiltDegrees), "sun-data-tilt")
    )
    val speedTemplate = stringResource(if (body.isExtrasolar)
        R.string.celestial_table_speed else spaceCompassCelestialSpeedLabel(body), SPACE_COMPASS_SUN_DATA_MARKER)
    val rows = if (!hasActiveBody) emptyList() else listOf(
        spaceCompassSunOptionalDataRow(stringResource(R.string.celestial_table_distance, SPACE_COMPASS_SUN_DATA_MARKER),
            formatSpaceCompassSelectedDistance(body, shownDistance, numeric, units.distance, units.feet), "celestial-distance"),
        spaceCompassSunOptionalDataRow(if (units.miles) speedTemplate.replace("km/s", "mi/s") else speedTemplate,
            formatSpaceCompassSpeed(speed, numeric, units.miles, allowNegative = body.isVoyager),
            "celestial-speed"),
        spaceCompassSunDataRow(stringResource(R.string.celestial_point_azimuth, SPACE_COMPASS_SUN_DATA_MARKER),
            angle(target?.azimuthDegrees), "sun-data-azimuth"),
        spaceCompassSunDataRow(stringResource(R.string.celestial_point_elevation, SPACE_COMPASS_SUN_DATA_MARKER),
            angle(target?.elevationDegrees), "sun-data-elevation")
    )
    val locationRows = listOf(
        spaceCompassSunDataRow(stringResource(R.string.celestial_altitude, SPACE_COMPASS_SUN_DATA_MARKER), height, "sun-data-altitude")
    )
    val simulated = LocalSpaceCompassObserver.current?.plan != null
    val simulatedPosition = LocalSpaceCompassObserver.current?.plan?.simulatePosition == true
    val simulatedAltitude = simulatedPosition || LocalSpaceCompassObserver.current?.plan?.simulateAltitude == true
    val locationInfoRows = listOf(
        spaceCompassSunOptionalDataRow(stringResource(if (simulatedPosition) R.string.observer_coordinates_value else R.string.celestial_gps_coordinates, SPACE_COMPASS_SUN_DATA_MARKER),
            formatSpaceCompassSelectedCoordinates(fix?.latitude, fix?.longitude, numeric, units.dms), "sun-info-coordinates"),
        spaceCompassSunOptionalDataRow(stringResource(R.string.celestial_gps_coordinate_accuracy, SPACE_COMPASS_SUN_DATA_MARKER),
            fix?.takeIf { it.hasAccuracy() && it.accuracy.isFinite() && it.accuracy >= 0f }
                ?.let { formatSpaceCompassPhysicalLength(it.accuracy.toDouble(), 0, numeric, units.feet) }, "sun-info-accuracy"),
        spaceCompassSunDataRow(stringResource(if (simulatedAltitude) R.string.observer_altitude_value else R.string.sun_finder_altitude, SPACE_COMPASS_SUN_DATA_MARKER), height, "sun-info-altitude"),
        spaceCompassSunDataRow(stringResource(R.string.environment_estimated_place, SPACE_COMPASS_SUN_DATA_MARKER),
            estimatedPlace.text ?: stringResource(if (estimatedPlace.loading)
                R.string.environment_place_loading else R.string.environment_place_unavailable), "sun-info-estimated-place")
    )
    val weatherValue = if (currentWeather == null) stringResource(
        if (weather.loading && sun != null) R.string.sun_weather_loading else R.string.sun_weather_unavailable
    ) else stringResource(when (currentWeather.kind) {
        SpaceCompassSunWeatherKind.CLEAR -> R.string.sun_weather_clear
        SpaceCompassSunWeatherKind.MAINLY_CLEAR -> R.string.celestial_weather_mainly_clear
        SpaceCompassSunWeatherKind.PARTLY_CLOUDY -> R.string.sun_weather_partial
        SpaceCompassSunWeatherKind.CLOUDY -> R.string.sun_weather_cloudy
        SpaceCompassSunWeatherKind.FOG -> R.string.sun_weather_fog
        SpaceCompassSunWeatherKind.DRIZZLE -> R.string.sun_weather_drizzle
        SpaceCompassSunWeatherKind.RAIN -> R.string.sun_weather_rain
        SpaceCompassSunWeatherKind.SNOW -> R.string.sun_weather_snow
        SpaceCompassSunWeatherKind.STORM -> R.string.sun_weather_storm
    })
    val weatherRow = spaceCompassSunDataRow(stringResource(R.string.sun_weather_estimate,
        SPACE_COMPASS_SUN_DATA_MARKER), weatherValue, "celestial-environment-weather")
    val weatherText = if (currentWeather == null) weatherValue else weatherRow.announcement
    val panorama = rememberSpaceCompassPanoramaAction(timeMs, fix?.latitude, fix?.longitude, altitude,
        phase, currentWeather, selectedBodies, selectedOverlays, remote,
        gpsAccuracyMeters = fix?.takeIf { it.hasAccuracy() && it.accuracy.isFinite() && it.accuracy >= 0f }
            ?.accuracy?.toDouble(), cameraEnabled = cameraEnabled, cameraCapture = cameraCapture, showSkyReferences = showSkyReferences, showWeather = showWeather, solarLighting = solarLighting,
        selectedPoints = dailyPathUiState.selectedPoints, activeBody = body.takeIf { it in selectedBodies })
    panorama.preview?.let { preview ->
        SpaceCompassPanoramaPreview(preview, panorama.closePreview)
        return
    }
    if (showEnvironment) {
        SpaceCompassSunFinderModelInfo(currentWeather != null, weatherRow, locationInfoRows,
            primaryText, secondaryText, backgroundColor, latitude = fix?.latitude, longitude = fix?.longitude) { showEnvironment = false }
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
    var groundFrame by remember { mutableStateOf<SpaceCompassSunSceneFrame?>(null) }
    val density = LocalDensity.current
    var toolbarHeightPx by remember { mutableIntStateOf(0) }
    fun refreshGroundFrame() {
        val scene = sceneCoordinates ?: return
        val pointing = pointingCoordinates ?: return
        if (!scene.isAttached || !pointing.isAttached) return
        val bounds = scene.localBoundingBoxOf(pointing, clipBounds = false)
        groundFrame = spaceCompassSceneOpticalFrame(
            SpaceCompassSunSceneFrame(bounds.left.toDouble(), bounds.top.toDouble(),
                bounds.width.toDouble(), bounds.height.toDouble()), toolbarHeightPx.toDouble())
    }
    val pointingPlacement = Modifier.onGloballyPositioned {
        pointingCoordinates = it
        refreshGroundFrame()
    }
    val skyDescription = stringResource(if (cameraEnabled) R.string.camera_view_description else R.string.sun_finder_sky_description)
    val selection = SpaceCompassCelestialSelection(selectedBodies, body.takeIf { hasActiveBody })
    val navigation = if (selection.selected.size > 1) SpaceCompassCelestialBodyNavigation(
        previousLabel = stringResource(selection.step(-1).active!!.nameResource),
        nextLabel = stringResource(selection.step(1).active!!.nameResource),
        onPrevious = { onBodyChange(selection.step(-1).active!!) },
        onNext = { onBodyChange(selection.step(1).active!!) }) else null
    val removalTarget = removeTargetName?.let { name -> SpaceCompassCelestialBody.entries.firstOrNull { it.name == name } }
    LaunchedEffect(removalTarget, selectedBodies) {
        if (removeTargetName != null && removalTarget !in selectedBodies) removeTargetName = null
    }
    if (removalTarget != null && removalTarget in selectedBodies && onSelectionChange != null) {
        SpaceCompassRemoveObjectDialog(removalTarget, onDismiss = { removeTargetName = null }, onConfirm = {
            removeTargetName = null
            onSelectionChange(selection.deselect(removalTarget))
        })
    }
    val pathActionTitle = stringResource(spaceCompassCelestialPathTitle(body))
    val details: @Composable (Modifier, Boolean, Boolean) -> Unit = { modifier, compact, landscape ->
        Column(modifier.testTag("celestial-details-column"), horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)) {
            SpaceCompassSunFinderDataPanel(rows, locationRows, orientation, readings.compassUsable,
                weatherText,
                currentWeather != null, primaryText, secondaryText, backgroundColor, Modifier.fillMaxWidth().weight(1f, fill = false), compact,
                bodyName = bodyName, orientationRows = orientationRows,
                // Keep the object identity at its full above-horizon palette color.
                bodyNameColor = if (hasActiveBody) spaceCompassCelestialPathTint(body) else primaryText,
                onRemoveBody = if (hasActiveBody && onSelectionChange != null) ({ removeTargetName = body.name }) else null,
                bodyNavigation = navigation, locationInfoRows = locationInfoRows, onInfo = { showEnvironment = true }, bodyActions = {
                    if (hasActiveBody) SpaceCompassCelestialSkyActions(body, { showViewer = true },
                        dailyPath?.let { path -> { dailyPathUiState.openMenu(path, spaceCompassCurrentPathPoint(body, timeMs, target)) } }, pathActionTitle, dailyPath != null,
                        targetSpacing = SpaceCompassObjectDetailsActionTargetSpacing)
                })
        }
    }
    val navigate = LocalSpaceCompassNavigate.current
    val topControlsInset = with(density) { toolbarHeightPx.toDp() } + 10.dp
    val titleBar: @Composable (Modifier, Boolean) -> Unit = { modifier, floating ->
        if (onDismissRequest != null) Row(
            modifier.fillMaxWidth().zIndex(1f).spaceCompassSceneInputShield().testTag("celestial-toolbar")
                .onSizeChanged {
                    // Only a full-width header removes height from the optical viewport.
                    toolbarHeightPx = if (floating) 0 else it.height
                    refreshGroundFrame()
                }
                .background(backgroundColor.copy(alpha = SPACE_COMPASS_MAIN_OVERLAY_ALPHA))
                .padding(start = 16.dp, end = SpaceCompassTitleBarContentPadding.calculateEndPadding(
                    androidx.compose.ui.platform.LocalLayoutDirection.current)),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(Modifier.weight(1f).heightIn(min = 48.dp), contentAlignment = Alignment.CenterStart) {
                SpaceCompassMenuTitle(stringResource(R.string.app_name),
                    Modifier.testTag("celestial-title")
                        .clickable(interactionSource = remember { MutableInteractionSource() },
                            indication = null, role = androidx.compose.ui.semantics.Role.Button) { navigate("info") },
                    color = primaryText)
            }
            SpaceCompassSettingsButton()
        }

    }
    val toolbar: @Composable () -> Unit = { titleBar(Modifier, false) }
    val captureControls: @Composable (Modifier) -> Unit = { modifier ->
        Row(modifier.padding(4.dp).testTag("celestial-capture-controls"),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            SpaceCompassCaptureButton(panorama.capture, !panorama.busy && (!cameraEnabled || cameraCapture != null),
                cameraEnabled = cameraEnabled)
        }
    }
    var landscapeCaptureSize by remember { mutableStateOf(IntSize.Zero) }
    val pointing: @Composable (Boolean) -> Unit = { landscape ->
        val floatingToolbar: (@Composable (Modifier) -> Unit)? =
            if (landscape && !detailsExpanded && onDismissRequest != null) {
                { modifier -> titleBar(modifier, true) }
            } else null
        Box(Modifier.fillMaxSize().spaceCompassCameraPinchZoom(cameraZoomRange, cameraZoomRequest,
            cameraEnabled && !panorama.busy, cameraCapture != null, ::updateCameraZoom)) {
            SpaceCompassSunPointingViewport(target, scenePointingOrientation.takeIf { !cameraEnabled || cameraPerspective != null },
                Modifier.fillMaxSize().then(pointingPlacement), skyDescription, dailyPathUiState, rising,
                readings.compassUsable, dailyPath.takeIf { hasActiveBody }, primaryText, secondaryText, backgroundColor, body, timeMs,
                onVisualize = { showViewer = true },
                compassWarning = messageText.takeIf { isCompassMessage && resumed && mainVisible },
                compassAccurate = readings.compassReliable,
                statusMessage = remoteMessage ?: messageText.takeUnless { isCompassMessage } ?: weatherFailureMessage,
                compassWarningAttention = message == R.string.sun_finder_compass_accuracy ||
                    message == R.string.sun_finder_compass_calibrate,
                statusAttention = (remoteMessageId ?: message) == R.string.celestial_satellite_old,
                statusActionLabel = if (showWeatherFailure) stringResource(R.string.sun_finder_retry)
                    else if (hasActiveBody && target == null && sun != null && !remote.loading)
                    stringResource(R.string.sun_finder_retry) else locationActionLabel,
                onStatusAction = if (showWeatherFailure) weather.onRetry
                    else if (hasActiveBody && target == null && sun != null) onRemoteRetry else onLocationAction,
                showActions = false, offscreenBody = body.takeIf { hasActiveBody }, overlays = overlays,
                approachOverlays = selectedOverlays,
                perspective = cameraPerspective.takeIf { cameraEnabled }, noticesBelowReticle = true,
                observerLatitude = fix?.latitude, showSkyReferences = showSkyReferences,
                observerAltitude = fix?.takeIf { it.hasAltitude() }?.altitude ?: 0.0,
                topContentInset = if (floatingToolbar != null) 10.dp else topControlsInset,
                projectionTopInset = if (floatingToolbar != null) 0.dp else with(density) { toolbarHeightPx.toDp() },
                topCenterActions = floatingToolbar,
                drawScene = false,
                bottomStartActions = { modifier ->
                    Row(modifier.padding(4.dp), verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        SpaceCompassMainDetailsToggleButton(detailsExpanded, {
                            detailsExpanded = !detailsExpanded
                            scenePreferences.edit().putBoolean(SPACE_COMPASS_MAIN_DETAILS_KEY, detailsExpanded).apply()
                        }, primaryText, backgroundColor, landscape = landscape)
                        SpaceCompassPointingAxisButton(pointingTopEdge && !cameraEnabled, { topEdge ->
                            pointingTopEdge = topEdge
                            scenePreferences.edit().putBoolean(SPACE_COMPASS_POINTING_TOP_EDGE_KEY, topEdge).apply()
                            notifyControl(R.string.control_choice_message, controlResources.getString(
                                if (topEdge) R.string.pointing_axis_top_edge else R.string.pointing_axis_camera),
                                formatSpaceCompassNumber(if (topEdge) 0.0 else 90.0, 0, numeric,
                                    grouping = false, systemLocale = controlLocale) + "°")
                        }, primaryText, backgroundColor, enabled = !cameraEnabled)
                        val selectedPointCount = dailyPathUiState.selectedPoints.size
                        if (selectedPointCount > 0) SpaceCompassClearOrbitPointsButton({
                            dailyPathUiState.clearSelection()
                            notifyControl(R.string.scene_points_cleared)
                        }, MaterialTheme.colorScheme.primary, backgroundColor, selectedCount = selectedPointCount)
                    }
                },
                bottomActions = { modifier ->
                    if (!landscape) captureControls(modifier)
                    else if (!detailsExpanded) {
                        // Keep labels and edge locators clear of the capture overlay after expansion.
                        Spacer(modifier.size(with(density) { landscapeCaptureSize.width.toDp() },
                            with(density) { landscapeCaptureSize.height.toDp() }))
                    }
                },
                topStartActions = { modifier ->
                    Row(modifier.padding(4.dp).testTag("celestial-top-start-controls"),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        SpaceCompassCameraToggleButton(cameraEnabled, { enabled ->
                            if (!enabled) {
                                cameraEnabled = false
                                notifyControl(R.string.scene_mode_sky)
                            } else if (hasSpaceCompassCameraPermission(cameraContext)) {
                                cameraEnabled = true
                                notifyControl(R.string.scene_mode_camera)
                            } else cameraPermission.launch(Manifest.permission.CAMERA)
                        }, primaryText, backgroundColor)
                        if (cameraEnabled) SpaceCompassCameraZoomControls(cameraZoomRange, cameraZoomRequest, cameraZoomActual,
                            !panorama.busy && cameraCapture != null, ::updateCameraZoom, primaryText, backgroundColor)
                        else SpaceCompassWeatherToggleButton(showWeather, { enabled ->
                            showWeather = enabled
                            scenePreferences.edit().putBoolean(SPACE_COMPASS_WEATHER_VISIBLE_KEY, enabled).apply()
                            notifyControl(if (enabled) R.string.scene_weather_on else R.string.scene_weather_off)
                        }, primaryText, backgroundColor)
                    }
                },
                topEndActions = { modifier ->
                    Row(modifier.padding(4.dp).testTag("celestial-top-end-controls"),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        SpaceCompassSkyReferenceButton(showSkyReferences, { enabled ->
                            showSkyReferences = enabled
                            scenePreferences.edit().putBoolean(SPACE_COMPASS_SKY_REFERENCES_KEY, enabled).apply()
                            notifyControl(if (enabled) R.string.scene_references_on else R.string.scene_references_off)
                        }, primaryText, backgroundColor)
                        SpaceCompassScenarioButton(LocalSpaceCompassObserver.current?.plan.activeSectionCount, primaryText, backgroundColor) {
                            navigate("observer")
                        }
                        SpaceCompassCelestialSelector(body, primaryText, backgroundColor, Modifier, timeMs, remote,
                            selectedBodies, onSelectionChange?.let { { it(selection.toggleAll()) } }) {
                            if (onSelectionChange != null) onSelectionChange(selection.toggle(it)) else onBodyChange(it)
                        }
                    }
                })
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
                    cameraEnabled = false
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
                    sun?.elevationDegrees, sceneWeather, resumed && mainVisible, Modifier.fillMaxSize())
            }
            SpaceCompassSunGroundBackdrop(sceneOrientation, groundFrame, phase, Modifier.fillMaxSize(), solarLighting)
        }
        SpaceCompassCelestialSceneBackdrop(groundFrame, cameraPerspective.takeIf { cameraEnabled },
            scenePointingOrientation.takeIf { !cameraEnabled || cameraPerspective != null },
            overlays, body.takeIf { hasActiveBody }, dailyPath.takeIf { hasActiveBody }, target, timeMs,
            dailyPathUiState, primaryText, secondaryText, backgroundColor,
            fix?.latitude, fix?.takeIf { it.hasAltitude() }?.altitude ?: 0.0, showSkyReferences,
            toolbarHeightPx.toDouble(), Modifier.fillMaxSize(), longitude = fix?.longitude)
        val minimumPanelWidth = if (largeText) 280.dp else 220.dp
        val detailsWidth = viewportWidth * 0.45f
        val compactPanel = viewportHeight < 420.dp && !largeText
        if (viewportWidth > viewportHeight && detailsWidth - 20.dp >= minimumPanelWidth) {
            // The toolbar belongs to the sky column, not above the full-height data column.
            Box(Modifier.fillMaxSize().testTag("celestial-landscape-layout")) {
            Row(Modifier.fillMaxSize()) {
                Box(Modifier.weight(1f).fillMaxHeight().clipToBounds()) {
                    // One optical frame for sky, paths and camera, including the translucent header.
                    Box(Modifier.fillMaxSize().padding(start = 10.dp, end = 10.dp, bottom = 10.dp)
                        .testTag("celestial-pointing-area")) {
                        pointing(true)
                    }
                    if (detailsExpanded) toolbar()
                }
                // Equal insets towards the sky/toolbar and the outer edge, including in RTL.
                SpaceCompassMainDetailsVisibility(detailsExpanded, landscape = true) {
                    Column(Modifier.width(detailsWidth).fillMaxHeight().padding(10.dp)) {
                        details(Modifier.fillMaxWidth().weight(1f), compactPanel, true)
                        Spacer(Modifier.height(with(density) { landscapeCaptureSize.height.toDp() }))
                    }
                }
            }
            // One stable overlay stays outside the information panels' transition and changing sky width.
            captureControls(Modifier.align(Alignment.BottomEnd).padding(10.dp)
                .onSizeChanged { landscapeCaptureSize = it })
            }
        } else Box(Modifier.fillMaxSize().clipToBounds()) {
            Column(Modifier.fillMaxSize().padding(start = 10.dp, end = 10.dp, bottom = 10.dp)) {
                Box(Modifier.weight(1f).fillMaxWidth().testTag("celestial-pointing-area")) {
                    pointing(false)
                }
                SpaceCompassMainDetailsVisibility(detailsExpanded, landscape = false) {
                    Column {
                        Spacer(Modifier.height(10.dp))
                        details(Modifier.fillMaxWidth().heightIn(max = viewportHeight * 0.56f), compactPanel, false)
                    }
                }
            }
            toolbar()
        }
    }
}
