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
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
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
    val catalogOpen = remember { mutableStateOf(false) }
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
    fun applySelection(selection: SpaceCompassCelestialSelection) {
        selectedNames = selection.ordered.map { it.name }
        selection.active?.let { selectedBody = it }
        // Save in the selection event, before a subsequent frame or app closure.
        preferences.edit().putStringSet("celestial_selected", selection.ordered.map { it.name }.toSet())
            .putString("celestial_active", selection.active?.name).apply()
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
        if (resumed) while (true) { time = System.currentTimeMillis(); delay(500L) }
    }
    val readings = key(retry) { rememberSpaceCompassSunFinderReadings(permission, paused = catalogOpen.value) }
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
    val observationTime = plan?.timeOverrideMs ?: time
    val tile = visibleReadings.location?.let { spaceCompassSunWeatherTile(it.latitude, it.longitude) }
    val weather = rememberSpaceCompassSunWeather(tile, resumed, selectedTimeMs = plan?.timeOverrideMs)
    // Warm the whole online catalog in the foreground; unchecked objects never become scene selections.
    val remote = rememberSpaceCompassCelestialRemote(selectedBodies, resumed, motionBodies = selectedBodies,
        retryRevision = remoteRetry, prefetchCatalog = true, catalogRefreshRevision = catalogRefresh,
        observationTimeOverrideMs = plan?.timeOverrideMs)
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
        SpaceCompassCelestialCatalogPage(observationTime, remote, selectedBodies, primaryText, backgroundColor,
            onBack = { catalogOpen.value = false },
            onToggleAll = { applySelection(selection.toggleVisible(it)) },
            onSelect = { applySelection(selection.toggle(it)) },
            latitude = visibleReadings.location?.latitude, longitude = visibleReadings.location?.longitude,
            altitude = visibleReadings.location?.takeIf { it.hasAltitude() }?.altitude ?: 0.0,
            onRefresh = { catalogRefresh++ }, onRevealHiddenObject = {
                val revealed = selection.revealHiddenObject()
                if (revealed != selection) {
                    applySelection(revealed)
                    showSpaceCompassBottomMessage(context,
                        unlockedMessage)
                }
            })
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
        if (!granted) showSpaceCompassBottomMessage(cameraContext, cameraPermissionMessage)
    }
    val mainVisible = LocalSpaceCompassMainVisible.current
    LaunchedEffect(resumed) {
        if (resumed && cameraEnabled && !hasSpaceCompassCameraPermission(cameraContext)) cameraEnabled = false
    }
    val observerPlan = LocalSpaceCompassObserver.current?.plan
    LaunchedEffect(observerPlan) { dailyPathUiState.clearSelection() }
    var showEnvironment by rememberSaveable { mutableStateOf(false) }
    var showViewer by rememberSaveable { mutableStateOf(false) }
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
    val publishSolarHeight = LocalSpaceCompassScreensaverSunElevation.current
    SideEffect { publishSolarHeight(sun?.elevationDegrees) }
    val zone = spaceCompassObservationZone()
    val date = Instant.ofEpochMilli(timeMs).atZone(zone).toLocalDate()
    // Approximately 11 m location cells keep GPS jitter from rebuilding a whole day's curve.
    // All ephemeris work runs off the UI thread; orientation merely projects the cached vectors.
    val pathLatitude = fix?.latitude?.let { round(it * 10_000) / 10_000 }
    val pathLongitude = fix?.longitude?.let { round(it * 10_000) / 10_000 }
    val pathAltitude = round((altitude ?: 0.0) / 10) * 10
    val allOverlays = rememberSpaceCompassCelestialOverlays(spaceCompassAvailableCelestialCatalog(selectedBodies).toSet(), timeMs, date, zone,
        pathLatitude, pathLongitude, pathAltitude, remote, pathBodies = selectedBodies)
    val selectedOverlays = spaceCompassSelectedCelestialEntries(allOverlays, selectedBodies)
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
        SpaceCompassSunDailyPathPage(dailyPathUiState, primaryText, secondaryText, backgroundColor, nowMs = timeMs)
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
            SpaceCompassSunCompassIssue.CALIBRATION -> R.string.sun_finder_compass_calibrate
            SpaceCompassSunCompassIssue.REDUCED_ACCURACY -> R.string.pc_compass_approximate
            else -> R.string.sun_finder_orientation_searching
        }
        hasActiveBody && target == null -> if (remote.loading) R.string.celestial_loading else R.string.celestial_unavailable
        body.isEarthSatellite && remote.satelliteOrbit(body)?.let { timeMs - it.epochMs > SPACE_COMPASS_ISS_WARNING_AGE_MS } == true -> R.string.celestial_satellite_old
        else -> null
    }
    val isCompassMessage = when (message) {
        R.string.sun_finder_compass_accuracy, R.string.sun_finder_compass_calibrate,
        R.string.pc_compass_approximate, R.string.sun_finder_orientation_searching -> true
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
        deviceView.display?.rotation ?: 0)
    val pointingOrientation = spaceCompassSunTrustedPointingOrientation(orientation, readings.compassUsable)
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
            ?.accuracy?.toDouble(), cameraEnabled = cameraEnabled, cameraCapture = cameraCapture, showSkyReferences = showSkyReferences, showWeather = showWeather, solarLighting = solarLighting)
    panorama.preview?.let { preview ->
        SpaceCompassPanoramaPreview(preview, panorama.closePreview)
        return
    }
    if (showEnvironment) {
        SpaceCompassSunFinderModelInfo(currentWeather != null, weatherRow, locationInfoRows,
            primaryText, secondaryText, backgroundColor, latitude = fix?.latitude, longitude = fix?.longitude) { showEnvironment = false }
        return
    }
    val locationActionLabel = if (sun != null) null else stringResource(when (readings.locationStatus) {
        SpaceCompassSunLocationStatus.PERMISSION -> R.string.sun_finder_allow_location
        SpaceCompassSunLocationStatus.DISABLED -> R.string.sun_finder_enable_location
        else -> R.string.sun_finder_retry
    })
    var sceneCoordinates by remember { mutableStateOf<LayoutCoordinates?>(null) }
    var pointingCoordinates by remember { mutableStateOf<LayoutCoordinates?>(null) }
    var groundFrame by remember { mutableStateOf<SpaceCompassSunSceneFrame?>(null) }
    fun refreshGroundFrame() {
        val scene = sceneCoordinates ?: return
        val pointing = pointingCoordinates ?: return
        if (!scene.isAttached || !pointing.isAttached) return
        val bounds = scene.localBoundingBoxOf(pointing, clipBounds = false)
        groundFrame = SpaceCompassSunSceneFrame(bounds.left.toDouble(), bounds.top.toDouble(),
            bounds.width.toDouble(), bounds.height.toDouble())
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
        onPrevious = { dailyPathUiState.clearSelection(); onBodyChange(selection.step(-1).active!!) },
        onNext = { dailyPathUiState.clearSelection(); onBodyChange(selection.step(1).active!!) }) else null
    val pathActionTitle = stringResource(spaceCompassCelestialPathTitle(body))
    val details: @Composable (Modifier, Boolean, Boolean) -> Unit = { modifier, compact, landscape ->
        Column(modifier.testTag("celestial-details-column"), horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)) {
            if (landscape) dailyPathUiState.selectedBody?.let { selected ->
                SpaceCompassSunPathSelectedPanel(selectedOverlays[selected]?.path, dailyPathUiState, primaryText, secondaryText,
                    backgroundColor, compact = compact, body = selected, nowMs = timeMs)
            }
            SpaceCompassSunFinderDataPanel(rows, locationRows, orientation, readings.compassUsable,
                weatherText,
                currentWeather != null, primaryText, secondaryText, backgroundColor, Modifier.fillMaxWidth().weight(1f, fill = false), compact,
                bodyName = bodyName, orientationRows = orientationRows,
                bodyNavigation = navigation, locationInfoRows = locationInfoRows, onInfo = { showEnvironment = true }, bodyActions = {
                    if (hasActiveBody) SpaceCompassCelestialSkyActions(body, { showViewer = true },
                        dailyPath?.let { path -> { dailyPathUiState.openMenu(path, spaceCompassCurrentPathPoint(body, timeMs, target)) } }, pathActionTitle, dailyPath != null)
                })
        }
    }
    val navigate = LocalSpaceCompassNavigate.current
    val density = LocalDensity.current
    var toolbarHeightPx by remember { mutableIntStateOf(0) }
    val topControlsInset = with(density) { toolbarHeightPx.toDp() } + 10.dp
    val toolbar: @Composable () -> Unit = {
        if (onDismissRequest != null) Row(
            Modifier.fillMaxWidth().zIndex(1f).testTag("celestial-toolbar")
                .onSizeChanged { toolbarHeightPx = it.height }
                .background(backgroundColor.copy(alpha = 0.72f))
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
    val pointing: @Composable (Boolean) -> Unit = { landscape ->
        Box(Modifier.fillMaxSize().spaceCompassCameraPinchZoom(cameraZoomRange, cameraZoomRequest,
            cameraEnabled && !panorama.busy, cameraCapture != null, ::updateCameraZoom)) {
            SpaceCompassSunPointingViewport(target, pointingOrientation.takeIf { !cameraEnabled || cameraPerspective != null },
                Modifier.fillMaxSize().then(pointingPlacement), skyDescription, dailyPathUiState, rising,
                readings.compassUsable, dailyPath.takeIf { hasActiveBody }, primaryText, secondaryText, backgroundColor, body, timeMs,
                showSelectedPanel = !landscape, selectedPanelExpanded = detailsExpanded, onVisualize = { showViewer = true },
                compassWarning = messageText.takeIf { isCompassMessage },
                compassAccurate = readings.compassReliable,
                statusMessage = remoteMessage ?: messageText.takeUnless { isCompassMessage },
                compassWarningAttention = message == R.string.sun_finder_compass_accuracy ||
                    message == R.string.sun_finder_compass_calibrate,
                statusAttention = (remoteMessageId ?: message) == R.string.celestial_satellite_old,
                statusActionLabel = if (hasActiveBody && target == null && sun != null && !remote.loading)
                    stringResource(R.string.sun_finder_retry) else locationActionLabel,
                onStatusAction = if (hasActiveBody && target == null && sun != null) onRemoteRetry else onLocationAction,
                showActions = false, offscreenBody = body.takeIf { hasActiveBody }, overlays = overlays, onActivateBody = {
                    if (it in selectedBodies) onBodyChange(it) },
                perspective = cameraPerspective.takeIf { cameraEnabled }, noticesBelowReticle = true,
                observerLatitude = fix?.latitude, showSkyReferences = showSkyReferences,
                observerAltitude = fix?.takeIf { it.hasAltitude() }?.altitude ?: 0.0,
                topContentInset = topControlsInset,
                bottomStartActions = { modifier ->
                    Column(modifier.padding(4.dp), horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        SpaceCompassMainDetailsToggleButton(detailsExpanded, {
                            detailsExpanded = !detailsExpanded
                            scenePreferences.edit().putBoolean(SPACE_COMPASS_MAIN_DETAILS_KEY, detailsExpanded).apply()
                        }, primaryText, backgroundColor, landscape = landscape)
                    }
                },
                bottomActions = { modifier ->
                    Row(modifier.padding(4.dp).testTag("celestial-capture-controls"),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        if (cameraEnabled) SpaceCompassCameraZoomControls(cameraZoomRange, cameraZoomRequest, cameraZoomActual,
                            !panorama.busy && cameraCapture != null, ::updateCameraZoom, primaryText, backgroundColor)
                        SpaceCompassCaptureButton(panorama.capture, !panorama.busy && (!cameraEnabled || cameraCapture != null),
                            primaryText, backgroundColor, cameraEnabled = cameraEnabled)
                    }
                },
                topStartActions = { modifier ->
                    Column(modifier.padding(4.dp).testTag("celestial-top-start-controls"),
                        horizontalAlignment = Alignment.Start,
                        verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            SpaceCompassCameraToggleButton(cameraEnabled, { enabled ->
                                if (!enabled) cameraEnabled = false
                                else if (hasSpaceCompassCameraPermission(cameraContext)) cameraEnabled = true
                                else cameraPermission.launch(Manifest.permission.CAMERA)
                            }, primaryText, backgroundColor)
                            SpaceCompassPointingAxisButton(pointingTopEdge && !cameraEnabled, { topEdge ->
                                pointingTopEdge = topEdge
                                scenePreferences.edit().putBoolean(SPACE_COMPASS_POINTING_TOP_EDGE_KEY, topEdge).apply()
                            }, primaryText, backgroundColor, enabled = !cameraEnabled)
                        }
                        SpaceCompassSkyReferenceButton(showSkyReferences, { enabled ->
                            showSkyReferences = enabled
                            scenePreferences.edit().putBoolean(SPACE_COMPASS_SKY_REFERENCES_KEY, enabled).apply()
                        }, primaryText, backgroundColor)
                    }
                },
                topEndActions = { modifier ->
                    Column(modifier.padding(4.dp).testTag("celestial-top-end-controls"),
                        horizontalAlignment = Alignment.End,
                        verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            SpaceCompassScenarioButton(simulated, primaryText, backgroundColor) { navigate("observer") }
                            SpaceCompassCelestialSelector(body, primaryText, backgroundColor, Modifier, timeMs, remote,
                                selectedBodies, onSelectionChange?.let { { dailyPathUiState.clearSelection(); it(selection.toggleAll()) } }) {
                                dailyPathUiState.clearSelection()
                                if (onSelectionChange != null) onSelectionChange(selection.toggle(it)) else onBodyChange(it)
                            }
                        }
                        SpaceCompassWeatherToggleButton(showWeather, { enabled ->
                            showWeather = enabled
                            scenePreferences.edit().putBoolean(SPACE_COMPASS_WEATHER_VISIBLE_KEY, enabled).apply()
                        }, primaryText, backgroundColor, enabled = !cameraEnabled)
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
                SpaceCompassStarField(timeMs, fix?.latitude, fix?.longitude, altitude ?: 0.0, orientation, groundFrame,
                    sun?.elevationDegrees, sceneWeather, resumed && mainVisible, Modifier.fillMaxSize())
            }
            SpaceCompassSunGroundBackdrop(orientation, groundFrame, phase, Modifier.fillMaxSize(), solarLighting)
        }
        val minimumPanelWidth = if (largeText) 280.dp else 220.dp
        val detailsWidth = viewportWidth * 0.45f
        val compactPanel = viewportHeight < 420.dp && !largeText
        if (viewportWidth > viewportHeight && detailsWidth - 20.dp >= minimumPanelWidth) {
            // The toolbar belongs to the sky column, not above the full-height data column.
            Row(Modifier.fillMaxSize().testTag("celestial-landscape-layout")) {
                Box(Modifier.weight(1f).fillMaxHeight().clipToBounds()) {
                    // One optical frame for sky, paths and camera, including the translucent header.
                    Box(Modifier.fillMaxSize().padding(start = 10.dp, end = 10.dp, bottom = 10.dp)
                        .testTag("celestial-pointing-area")) {
                        pointing(true)
                    }
                    toolbar()
                }
                // Equal insets towards the sky/toolbar and the outer edge, including in RTL.
                SpaceCompassMainDetailsVisibility(detailsExpanded, landscape = true) {
                    details(Modifier.width(detailsWidth).fillMaxHeight().padding(10.dp), compactPanel, true)
                }
            }
        } else Box(Modifier.fillMaxSize().clipToBounds()) {
            Column(Modifier.fillMaxSize().padding(start = 10.dp, end = 10.dp, bottom = 10.dp)) {
                Box(Modifier.weight(1f).fillMaxWidth().testTag("celestial-pointing-area")) {
                    pointing(false)
                }
                SpaceCompassMainDetailsVisibility(detailsExpanded, landscape = false) {
                    Column {
                        Spacer(Modifier.height(8.dp))
                        details(Modifier.fillMaxWidth().heightIn(max = viewportHeight * 0.56f), compactPanel, false)
                    }
                }
            }
            toolbar()
        }
    }
}
