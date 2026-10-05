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
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.layout
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
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
    val catalogOpen = remember { mutableStateOf(false) }
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    var permission by remember { mutableStateOf(hasSpaceCompassSunLocationPermission(context)) }
    var requested by rememberSaveable { mutableStateOf(false) }
    var retry by remember { mutableIntStateOf(0) }
    var time by remember { mutableLongStateOf(System.currentTimeMillis()) }
    val preferences = LocalSpaceCompassPreferences.current
        ?: remember(context) { context.getSharedPreferences(SPACE_COMPASS_PREFERENCES_NAME, android.content.Context.MODE_PRIVATE) }
    val initialSelection = remember(preferences) { restoreSpaceCompassCelestialSelection(
        preferences.getStringSet("celestial_selected", null), preferences.getString("celestial_active", null)) }
    var selectedBody by rememberSaveable { mutableStateOf(initialSelection.active ?: SpaceCompassCelestialBody.SUN) }
    var selectedNames by rememberSaveable { mutableStateOf(initialSelection.ordered.map { it.name }) }
    val selectedBodies = remember(selectedNames) { selectedNames.mapNotNull { name ->
        SpaceCompassCelestialBody.entries.firstOrNull { it.name == name }
    }.toSet() }
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
    LaunchedEffect(Unit) {
        if (!permission && !requested) {
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
    val visibleReadings = if (!fixFresh && readings.location != null)
        readings.copy(location = null, locationStatus = SpaceCompassSunLocationStatus.SEARCHING) else readings
    val tile = visibleReadings.location?.let { spaceCompassSunWeatherTile(it.latitude, it.longitude) }
    val weather = rememberSpaceCompassSunWeather(tile, resumed)
    // The catalog reads existing caches; only selected objects trigger remote downloads.
    val remote = rememberSpaceCompassCelestialRemote(selectedBodies, resumed, motionBodies = selectedBodies)
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
        time, primaryText, secondaryText, backgroundColor,
        weather = weather,
        remote = remote, body = selectedBody, onBodyChange = { applySelection(SpaceCompassCelestialSelection(selectedBodies, it)) },
        selectedBodies = selectedBodies, onSelectionChange = ::applySelection, onDismissRequest = onDismissRequest) {
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
        val selection = SpaceCompassCelestialSelection(selectedBodies, selectedBody.takeIf { it in selectedBodies })
        SpaceCompassCelestialCatalogPage(time, remote, selectedBodies, primaryText, backgroundColor,
            onBack = { catalogOpen.value = false },
            onToggleAll = { applySelection(selection.toggleVisible(it)) },
            onSelect = { applySelection(selection.toggle(it)) },
            latitude = visibleReadings.location?.latitude, longitude = visibleReadings.location?.longitude,
            altitude = visibleReadings.location?.takeIf { it.hasAltitude() }?.altitude ?: 0.0)
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
    onDismissRequest: (() -> Unit)? = null, onLocationAction: () -> Unit = {}
) {
    val dailyPathUiState = rememberSpaceCompassSunDailyPathUiState()
    var showEnvironment by rememberSaveable { mutableStateOf(false) }
    var showViewer by rememberSaveable { mutableStateOf(false) }
    val hasActiveBody = body in selectedBodies
    val bodyName = if (hasActiveBody) stringResource(body.nameResource) else "—"
    val fix = readings.location
    val altitude = fix?.takeIf { it.hasAltitude() && it.altitude.isFinite() && it.altitude in -500.0..20_000.0 }?.altitude
    val sun = remember(timeMs, fix?.latitude, fix?.longitude, altitude) {
        fix?.let { calculateSpaceCompassSunPosition(timeMs, it.latitude, it.longitude, altitude ?: 0.0) }
    }
    val rising = remember(timeMs, fix?.latitude, fix?.longitude, altitude) {
        fix != null && sun != null && calculateSpaceCompassSunPosition(timeMs + 60_000L,
            fix.latitude, fix.longitude, altitude ?: 0.0).elevationDegrees >= sun.elevationDegrees
    }
    val phase = spaceCompassSunSkyPhase(sun?.elevationDegrees, rising)
    val zone = ZoneId.systemDefault()
    val date = Instant.ofEpochMilli(timeMs).atZone(zone).toLocalDate()
    // Approximately 11 m location cells keep GPS jitter from rebuilding a whole day's curve.
    // All ephemeris work runs off the UI thread; orientation merely projects the cached vectors.
    val pathLatitude = fix?.latitude?.let { round(it * 10_000) / 10_000 }
    val pathLongitude = fix?.longitude?.let { round(it * 10_000) / 10_000 }
    val pathAltitude = round((altitude ?: 0.0) / 10) * 10
    val allOverlays = rememberSpaceCompassCelestialOverlays(spaceCompassCelestialCatalogOrder.toSet(), timeMs, date, zone,
        pathLatitude, pathLongitude, pathAltitude, remote, pathBodies = selectedBodies)
    // Data producers stay composed while a child page is visible: returning reuses their caches.
    if (showViewer && hasActiveBody) {
        val location = readings.location
        val height = location?.takeIf { it.hasAltitude() && it.altitude.isFinite() && it.altitude in -500.0..20_000.0 }?.altitude ?: 0.0
        SpaceCompassCelestialViewerScreen(body, timeMs, location?.latitude, location?.longitude, height, remote,
            primaryText, secondaryText, backgroundColor) { showViewer = false }
        return
    }
    if (dailyPathUiState.menuPath != null) {
        SpaceCompassSunDailyPathPage(dailyPathUiState, primaryText, secondaryText, backgroundColor)
        return
    }
    // Catalogue calculations are not visibility: unchecked live positions must not enter the scene.
    val selectedOverlays = spaceCompassSelectedCelestialEntries(allOverlays, selectedBodies)
    val activeOverlay = selectedOverlays[body]
    val target = if (!hasActiveBody) null else if (body == SpaceCompassCelestialBody.SUN) sun else activeOverlay?.observation?.position
    val speed = activeOverlay?.speedKmSecond
    val shownDistance = activeOverlay?.distanceKm
    val dailyPath = activeOverlay?.path
    val overlays = selectedOverlays - body
    val currentWeather = weather.snapshot?.takeIf { sun != null && spaceCompassSunWeatherTimeUsable(it.modelTimeMs, timeMs) }
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
        !readings.compassReliable -> if (readings.compassUsable) R.string.pc_compass_approximate else R.string.sun_finder_compass_accuracy
        hasActiveBody && target == null -> if (remote.loading) R.string.celestial_loading else R.string.celestial_unavailable
        body.isEarthSatellite && remote.satelliteOrbit(body)?.let { timeMs - it.epochMs > SPACE_COMPASS_ISS_WARNING_AGE_MS } == true -> R.string.celestial_satellite_old
        else -> null
    }
    val messageText = message?.let {
        if (it == R.string.celestial_unavailable || it == R.string.celestial_satellite_old) stringResource(it, bodyName)
        else stringResource(it)
    }
    val remoteMessage = when {
        hasActiveBody && target == null -> if (remote.loading) stringResource(R.string.celestial_loading)
            else stringResource(when {
                body == SpaceCompassCelestialBody.STARLINK_V3 && body in remote.timedOutBodies -> R.string.celestial_starlink_timeout
                body == SpaceCompassCelestialBody.STARLINK_V3 -> R.string.celestial_starlink_unavailable
                body.isEarthSatellite && body in remote.timedOutBodies -> R.string.celestial_satellite_timeout
                body.isEarthSatellite -> R.string.celestial_satellite_unavailable
                else -> R.string.celestial_unavailable
            }, bodyName)
        body.isEarthSatellite && remote.satelliteOrbit(body)?.let { timeMs - it.epochMs > SPACE_COMPASS_ISS_WARNING_AGE_MS } == true ->
            stringResource(R.string.celestial_satellite_old, bodyName)
        else -> null
    }
    val orientation = readings.orientation
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
    val locationInfoRows = listOf(
        spaceCompassSunOptionalDataRow(stringResource(R.string.celestial_gps_coordinates, SPACE_COMPASS_SUN_DATA_MARKER),
            formatSpaceCompassSelectedCoordinates(fix?.latitude, fix?.longitude, numeric, units.dms), "sun-info-coordinates"),
        spaceCompassSunOptionalDataRow(stringResource(R.string.celestial_gps_coordinate_accuracy, SPACE_COMPASS_SUN_DATA_MARKER),
            fix?.takeIf { it.hasAccuracy() && it.accuracy.isFinite() && it.accuracy >= 0f }
                ?.let { formatSpaceCompassPhysicalLength(it.accuracy.toDouble(), 0, numeric, units.feet) }, "sun-info-accuracy"),
        spaceCompassSunDataRow(stringResource(R.string.sun_finder_altitude, SPACE_COMPASS_SUN_DATA_MARKER), height, "sun-info-altitude")
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
    if (showEnvironment) {
        SpaceCompassSunFinderModelInfo(currentWeather != null, weatherRow, locationInfoRows,
            primaryText, secondaryText, backgroundColor) { showEnvironment = false }
        return
    }
    val locationActionLabel = if (sun != null) null else stringResource(when (readings.locationStatus) {
        SpaceCompassSunLocationStatus.PERMISSION -> R.string.sun_finder_allow_location
        SpaceCompassSunLocationStatus.DISABLED -> R.string.sun_finder_enable_location
        else -> R.string.sun_finder_retry
    })
    var sceneCoordinates by remember { mutableStateOf<LayoutCoordinates?>(null) }
    var pointingCoordinates by remember { mutableStateOf<LayoutCoordinates?>(null) }
    val selectorSize = with(LocalDensity.current) { 60.dp.toPx().toDouble() }
    val selectorRtl = androidx.compose.ui.platform.LocalLayoutDirection.current == androidx.compose.ui.unit.LayoutDirection.Rtl
    val timeBadgeExclusions = pointingCoordinates?.size?.width?.toDouble()?.let { width ->
        listOf(SpaceCompassSunSceneFrame(if (selectorRtl) 0.0 else width - selectorSize, 0.0, selectorSize, selectorSize))
    }.orEmpty()
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
    val skyDescription = stringResource(R.string.sun_finder_sky_description)
    val selection = SpaceCompassCelestialSelection(selectedBodies, body.takeIf { hasActiveBody })
    val navigation = if (selection.selected.size > 1) SpaceCompassCelestialBodyNavigation(
        previousLabel = stringResource(selection.step(-1).active!!.nameResource),
        nextLabel = stringResource(selection.step(1).active!!.nameResource),
        onPrevious = { dailyPathUiState.clearSelection(); onBodyChange(selection.step(-1).active!!) },
        onNext = { dailyPathUiState.clearSelection(); onBodyChange(selection.step(1).active!!) }) else null
    val pathActionTitle = stringResource(spaceCompassCelestialPathTitle(body))
    val details: @Composable (Modifier, Boolean, Boolean) -> Unit = { modifier, compact, landscape ->
        Column(modifier.testTag("celestial-details-column"), horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(5.dp)) {
            if (landscape) dailyPathUiState.selectedBody?.let { selected ->
                SpaceCompassSunPathSelectedPanel(selectedOverlays[selected]?.path, dailyPathUiState, primaryText, secondaryText,
                    backgroundColor, compact = compact, body = selected, nowMs = timeMs)
            }
            SpaceCompassSunFinderDataPanel(rows, locationRows, orientation, readings.compassUsable,
                remoteMessage ?: messageText.takeUnless { message == R.string.sun_finder_compass_accuracy || message == R.string.pc_compass_approximate }, weatherText,
                currentWeather != null, primaryText, secondaryText, backgroundColor, Modifier.fillMaxWidth().weight(1f, fill = false), compact,
                locationActionLabel, onLocationAction, bodyName = bodyName, orientationRows = orientationRows,
                bodyNavigation = navigation, locationInfoRows = locationInfoRows, onInfo = { showEnvironment = true }, bodyActions = {
                    if (hasActiveBody) SpaceCompassCelestialSkyActions(body, { showViewer = true },
                        dailyPath?.let { path -> { dailyPathUiState.openMenu(path) } }, pathActionTitle, dailyPath != null)
                })
        }
    }
    val navigate = LocalSpaceCompassNavigate.current
    val toolbar: @Composable () -> Unit = {
        if (onDismissRequest != null) Row(
            Modifier.fillMaxWidth().testTag("celestial-toolbar")
                .background(backgroundColor.copy(alpha = 0.72f))
                .padding(start = 16.dp, end = 8.dp), verticalAlignment = Alignment.CenterVertically
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
        Box(Modifier.fillMaxSize()) {
            SpaceCompassSunPointingViewport(target, pointingOrientation,
                Modifier.fillMaxSize().then(pointingPlacement), skyDescription, dailyPathUiState, rising,
                readings.compassUsable, dailyPath.takeIf { hasActiveBody }, primaryText, secondaryText, backgroundColor, body, timeMs,
                timeBadgeExclusions, showSelectedPanel = !landscape, onVisualize = { showViewer = true },
                compassWarning = messageText.takeIf { message == R.string.sun_finder_compass_accuracy || message == R.string.pc_compass_approximate },
                compassAccurate = readings.compassReliable,
                showActions = false, offscreenBody = body.takeIf { hasActiveBody }, overlays = overlays, onActivateBody = {
                    if (it in selectedBodies) onBodyChange(it) })
            SpaceCompassCelestialSelector(body, primaryText, backgroundColor, Modifier.align(Alignment.TopEnd).padding(4.dp), timeMs, remote,
                selectedBodies, onSelectionChange?.let { { dailyPathUiState.clearSelection(); it(selection.toggleAll()) } }) {
                dailyPathUiState.clearSelection()
                if (onSelectionChange != null) onSelectionChange(selection.toggle(it)) else onBodyChange(it)
            }
        }
    }
    BoxWithConstraints(Modifier.fillMaxSize().testTag("sun-finder-content").onGloballyPositioned {
        sceneCoordinates = it
        refreshGroundFrame()
    }) {
        val viewportWidth = maxWidth
        val viewportHeight = maxHeight
        SpaceCompassSunSkyBackdrop(phase, currentWeather, Modifier.fillMaxSize())
        SpaceCompassSunGroundBackdrop(orientation, groundFrame, phase, Modifier.fillMaxSize())
        val minimumPanelWidth = if (largeText) 280.dp else 220.dp
        val compactPanel = viewportHeight < 420.dp && !largeText
        if (viewportWidth > viewportHeight && viewportWidth / 2 - 20.dp >= minimumPanelWidth) {
            // The toolbar belongs to the sky column, not above the full-height data column.
            Row(Modifier.fillMaxSize().testTag("celestial-landscape-layout")) {
                Column(Modifier.weight(1f).fillMaxHeight()) {
                    toolbar()
                    Box(Modifier.weight(1f).fillMaxWidth().padding(10.dp).testTag("celestial-pointing-area")) {
                        pointing(true)
                    }
                }
                // Equal insets towards the sky/toolbar and the outer edge, including in RTL.
                details(Modifier.weight(1f).fillMaxHeight().padding(10.dp), compactPanel, true)
            }
        } else Column(Modifier.fillMaxSize()) {
            toolbar()
            Column(Modifier.weight(1f).fillMaxWidth().padding(10.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Box(Modifier.weight(1f).fillMaxWidth()) {
                    pointing(false)
                }
                details(Modifier.fillMaxWidth().heightIn(max = viewportHeight * 0.56f), compactPanel, false)
            }
        }
    }
}
