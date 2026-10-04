package me.mondiversi.planetcompass

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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.LayoutCoordinates
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
internal fun PlanetCompassSunFinderScreen(
    backgroundColor: Color, primaryText: Color, secondaryText: Color, onDismissRequest: () -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    var permission by remember { mutableStateOf(hasPlanetCompassSunLocationPermission(context)) }
    var requested by rememberSaveable { mutableStateOf(false) }
    var retry by remember { mutableIntStateOf(0) }
    var time by remember { mutableLongStateOf(System.currentTimeMillis()) }
    var selectedBody by rememberSaveable { mutableStateOf(PlanetCompassCelestialBody.SUN) }
    var selectedNames by rememberSaveable {
        mutableStateOf(PlanetCompassCelestialSelection().ordered.map { it.name })
    }
    val selectedBodies = remember(selectedNames) { selectedNames.mapNotNull { name ->
        PlanetCompassCelestialBody.entries.firstOrNull { it.name == name }
    }.toSet() }
    fun applySelection(selection: PlanetCompassCelestialSelection) {
        selectedNames = selection.ordered.map { it.name }
        selection.active?.let { selectedBody = it }
    }
    var resumed by remember { mutableStateOf(lifecycleOwner.lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)) }
    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {
        permission = hasPlanetCompassSunLocationPermission(context)
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
                permission = hasPlanetCompassSunLocationPermission(context)
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
    val readings = key(retry) { rememberPlanetCompassSunFinderReadings(permission) }
    val fixFresh = readings.location?.let {
        planetCompassSunLocationUsable(it, android.os.SystemClock.elapsedRealtimeNanos())
    } == true
    val visibleReadings = if (!fixFresh && readings.location != null)
        readings.copy(location = null, locationStatus = PlanetCompassSunLocationStatus.SEARCHING) else readings
    val tile = visibleReadings.location?.let { planetCompassSunWeatherTile(it.latitude, it.longitude) }
    val weather = rememberPlanetCompassSunWeather(tile, resumed)
    val remote = rememberPlanetCompassCelestialRemote(planetCompassCelestialCatalogOrder.toSet(), resumed, motionBodies = selectedBodies)
    BackHandler(onBack = onDismissRequest)
    PlanetCompassSunFinderContent(visibleReadings, time, primaryText, secondaryText, backgroundColor,
        weather = weather, remote = remote, body = selectedBody, onBodyChange = { selectedBody = it },
        selectedBodies = selectedBodies, onSelectionChange = ::applySelection, onDismissRequest = onDismissRequest) {
        val intent = when (visibleReadings.locationStatus) {
            PlanetCompassSunLocationStatus.PERMISSION -> Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                Uri.parse("package:${context.packageName}"))
            PlanetCompassSunLocationStatus.DISABLED -> Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)
            else -> null
        }
        if (intent == null) retry++ else runCatching { context.startActivity(intent) }
            .onFailure { PlanetCompassErrorLog.record(context, "sun_finder:settings", it) }
    }
}

@Composable
internal fun PlanetCompassSunFinderContent(
    readings: PlanetCompassSunFinderReadings, timeMs: Long, primaryText: Color,
    secondaryText: Color, backgroundColor: Color, weather: PlanetCompassSunWeatherReading = PlanetCompassSunWeatherReading(),
    remote: PlanetCompassCelestialRemoteData = PlanetCompassCelestialRemoteData(), body: PlanetCompassCelestialBody = PlanetCompassCelestialBody.SUN,
    onBodyChange: (PlanetCompassCelestialBody) -> Unit = {},
    selectedBodies: Set<PlanetCompassCelestialBody> = setOf(body),
    onSelectionChange: ((PlanetCompassCelestialSelection) -> Unit)? = null,
    onDismissRequest: (() -> Unit)? = null, onLocationAction: () -> Unit = {}
) {
    val dailyPathUiState = rememberPlanetCompassSunDailyPathUiState()
    var showViewer by rememberSaveable { mutableStateOf(false) }
    val hasActiveBody = body in selectedBodies
    if (showViewer && hasActiveBody) {
        val location = readings.location
        val height = location?.takeIf { it.hasAltitude() && it.altitude.isFinite() && it.altitude in -500.0..20_000.0 }?.altitude ?: 0.0
        PlanetCompassCelestialViewerScreen(body, timeMs, location?.latitude, location?.longitude, height, remote,
            primaryText, secondaryText, backgroundColor) { showViewer = false }
        return
    }
    val bodyName = if (hasActiveBody) stringResource(body.nameResource) else "—"
    val fix = readings.location
    val altitude = fix?.takeIf { it.hasAltitude() && it.altitude.isFinite() && it.altitude in -500.0..20_000.0 }?.altitude
    val sun = remember(timeMs, fix?.latitude, fix?.longitude, altitude) {
        fix?.let { calculatePlanetCompassSunPosition(timeMs, it.latitude, it.longitude, altitude ?: 0.0) }
    }
    val rising = remember(timeMs, fix?.latitude, fix?.longitude, altitude) {
        fix != null && sun != null && calculatePlanetCompassSunPosition(timeMs + 60_000L,
            fix.latitude, fix.longitude, altitude ?: 0.0).elevationDegrees >= sun.elevationDegrees
    }
    val phase = planetCompassSunSkyPhase(sun?.elevationDegrees, rising)
    val zone = ZoneId.systemDefault()
    val date = Instant.ofEpochMilli(timeMs).atZone(zone).toLocalDate()
    // Approximately 11 m location cells keep GPS jitter from rebuilding a whole day's curve.
    // All ephemeris work runs off the UI thread; orientation merely projects the cached vectors.
    val pathLatitude = fix?.latitude?.let { round(it * 10_000) / 10_000 }
    val pathLongitude = fix?.longitude?.let { round(it * 10_000) / 10_000 }
    val pathAltitude = round((altitude ?: 0.0) / 10) * 10
    val allOverlays = rememberPlanetCompassCelestialOverlays(planetCompassCelestialCatalogOrder.toSet(), timeMs, date, zone,
        pathLatitude, pathLongitude, pathAltitude, remote, pathBodies = selectedBodies)
    // Catalogue calculations are not visibility: unchecked live positions must not enter the scene.
    val selectedOverlays = planetCompassSelectedCelestialEntries(allOverlays, selectedBodies)
    val activeOverlay = selectedOverlays[body]
    val target = if (!hasActiveBody) null else if (body == PlanetCompassCelestialBody.SUN) sun else activeOverlay?.observation?.position
    val speed = activeOverlay?.speedKmSecond
    val shownDistance = activeOverlay?.distanceKm
    val dailyPath = activeOverlay?.path
    val overlays = selectedOverlays - body
    val currentWeather = weather.snapshot?.takeIf { sun != null && planetCompassSunWeatherTimeUsable(it.modelTimeMs, timeMs) }
    val numeric = LocalPlanetCompassNumericFormat.current
    val largeText = LocalDensity.current.fontScale > 1.4f
    fun angle(value: Double?) = value?.let { "${formatPlanetCompassNumber(it, 1, numeric)}°" } ?: "—"
    val message = when {
        readings.locationStatus == PlanetCompassSunLocationStatus.PERMISSION -> R.string.sun_finder_location_permission
        readings.locationStatus == PlanetCompassSunLocationStatus.DISABLED -> R.string.sun_finder_location_disabled
        readings.locationStatus == PlanetCompassSunLocationStatus.ERROR -> R.string.sun_finder_location_error
        sun == null -> R.string.sun_finder_location_searching
        !readings.compassAvailable -> R.string.sun_finder_compass_missing
        readings.orientation == null -> R.string.sun_finder_orientation_searching
        !readings.compassReliable -> R.string.sun_finder_compass_accuracy
        hasActiveBody && target == null -> if (remote.loading) R.string.celestial_loading else R.string.celestial_unavailable
        body.isEarthSatellite && remote.satelliteOrbit(body)?.let { timeMs - it.epochMs > PLANET_COMPASS_ISS_WARNING_AGE_MS } == true -> R.string.celestial_satellite_old
        else -> null
    }
    val messageText = message?.let {
        if (it == R.string.celestial_unavailable || it == R.string.celestial_satellite_old) stringResource(it, bodyName)
        else stringResource(it)
    }
    val orientation = readings.orientation
    val pointingOrientation = planetCompassSunTrustedPointingOrientation(orientation, readings.compassReliable)
    val height = formatPlanetCompassCelestialAltitude(altitude,
        fix?.takeIf { it.hasVerticalAccuracy() }?.verticalAccuracyMeters?.toDouble(), numeric) ?: "—"
    val orientationRows = listOf(
        planetCompassSunDataRow(stringResource(R.string.sun_finder_heading, PLANET_COMPASS_SUN_DATA_MARKER),
            angle(planetCompassSunPointingHeading(pointingOrientation)), "sun-data-heading"),
        planetCompassSunDataRow(stringResource(R.string.sun_finder_tilt, PLANET_COMPASS_SUN_DATA_MARKER),
            angle(orientation?.tiltDegrees), "sun-data-tilt")
    )
    val speedTemplate = stringResource(if (body == PlanetCompassCelestialBody.POLARIS)
        R.string.celestial_table_speed else planetCompassCelestialSpeedLabel(body), PLANET_COMPASS_SUN_DATA_MARKER)
    val rows = if (!hasActiveBody) emptyList() else listOf(
        planetCompassSunOptionalDataRow(stringResource(R.string.celestial_table_distance, PLANET_COMPASS_SUN_DATA_MARKER),
            formatPlanetCompassCelestialTableDistance(body, shownDistance, numeric), "celestial-distance"),
        planetCompassSunOptionalDataRow(speedTemplate,
            speed?.takeIf { it.isFinite() && (it >= 0 || body.isVoyager) }?.let { formatPlanetCompassNumber(it, 2, numeric) },
            "celestial-speed"),
        planetCompassSunDataRow(stringResource(R.string.celestial_point_azimuth, PLANET_COMPASS_SUN_DATA_MARKER),
            angle(target?.azimuthDegrees), "sun-data-azimuth"),
        planetCompassSunDataRow(stringResource(R.string.celestial_point_elevation, PLANET_COMPASS_SUN_DATA_MARKER),
            angle(target?.elevationDegrees), "sun-data-elevation")
    )
    val locationRows = listOf(
        planetCompassSunDataRow(stringResource(R.string.celestial_altitude, PLANET_COMPASS_SUN_DATA_MARKER), height, "sun-data-altitude")
    )
    val locationInfoRows = listOf(
        planetCompassSunOptionalDataRow(stringResource(R.string.celestial_gps_coordinates, PLANET_COMPASS_SUN_DATA_MARKER),
            formatPlanetCompassCelestialGpsCoordinates(fix?.latitude, fix?.longitude, numeric), "sun-info-coordinates"),
        planetCompassSunOptionalDataRow(stringResource(R.string.celestial_gps_coordinate_accuracy, PLANET_COMPASS_SUN_DATA_MARKER),
            fix?.takeIf { it.hasAccuracy() && it.accuracy.isFinite() && it.accuracy >= 0f }
                ?.let { "${formatPlanetCompassNumber(it.accuracy.toDouble(), 0, numeric)} m" }, "sun-info-accuracy"),
        planetCompassSunDataRow(stringResource(R.string.sun_finder_altitude, PLANET_COMPASS_SUN_DATA_MARKER), height, "sun-info-altitude")
    )
    val weatherText = if (currentWeather == null) stringResource(
        if (weather.loading && sun != null) R.string.sun_weather_loading else R.string.sun_weather_unavailable
    ) else stringResource(R.string.sun_weather_estimate, stringResource(when (currentWeather.kind) {
        PlanetCompassSunWeatherKind.CLEAR -> R.string.sun_weather_clear
        PlanetCompassSunWeatherKind.MAINLY_CLEAR -> R.string.celestial_weather_mainly_clear
        PlanetCompassSunWeatherKind.PARTLY_CLOUDY -> R.string.sun_weather_partial
        PlanetCompassSunWeatherKind.CLOUDY -> R.string.sun_weather_cloudy
        PlanetCompassSunWeatherKind.FOG -> R.string.sun_weather_fog
        PlanetCompassSunWeatherKind.DRIZZLE -> R.string.sun_weather_drizzle
        PlanetCompassSunWeatherKind.RAIN -> R.string.sun_weather_rain
        PlanetCompassSunWeatherKind.SNOW -> R.string.sun_weather_snow
        PlanetCompassSunWeatherKind.STORM -> R.string.sun_weather_storm
    }))
    val locationActionLabel = if (sun != null) null else stringResource(when (readings.locationStatus) {
        PlanetCompassSunLocationStatus.PERMISSION -> R.string.sun_finder_allow_location
        PlanetCompassSunLocationStatus.DISABLED -> R.string.sun_finder_enable_location
        else -> R.string.sun_finder_retry
    })
    var sceneCoordinates by remember { mutableStateOf<LayoutCoordinates?>(null) }
    var pointingCoordinates by remember { mutableStateOf<LayoutCoordinates?>(null) }
    val timeBadgeExclusions = emptyList<PlanetCompassSunSceneFrame>()
    var groundFrame by remember { mutableStateOf<PlanetCompassSunSceneFrame?>(null) }
    fun refreshGroundFrame() {
        val scene = sceneCoordinates ?: return
        val pointing = pointingCoordinates ?: return
        if (!scene.isAttached || !pointing.isAttached) return
        val bounds = scene.localBoundingBoxOf(pointing, clipBounds = false)
        groundFrame = PlanetCompassSunSceneFrame(bounds.left.toDouble(), bounds.top.toDouble(),
            bounds.width.toDouble(), bounds.height.toDouble())
    }
    val pointingPlacement = Modifier.onGloballyPositioned {
        pointingCoordinates = it
        refreshGroundFrame()
    }
    val skyDescription = stringResource(R.string.sun_finder_sky_description)
    val selection = PlanetCompassCelestialSelection(selectedBodies, body.takeIf { hasActiveBody })
    val navigation = if (selection.selected.size > 1) PlanetCompassCelestialBodyNavigation(
        previousLabel = stringResource(selection.step(-1).active!!.nameResource),
        nextLabel = stringResource(selection.step(1).active!!.nameResource),
        onPrevious = { dailyPathUiState.clearSelection(); onBodyChange(selection.step(-1).active!!) },
        onNext = { dailyPathUiState.clearSelection(); onBodyChange(selection.step(1).active!!) }) else null
    val pathActionTitle = stringResource(planetCompassCelestialPathTitle(body))
    val details: @Composable (Modifier, Boolean, Boolean) -> Unit = { modifier, compact, landscape ->
        Column(modifier.testTag("celestial-details-column"), horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(5.dp)) {
            if (landscape) dailyPathUiState.selectedBody?.let { selected ->
                PlanetCompassSunPathSelectedPanel(selectedOverlays[selected]?.path, dailyPathUiState, primaryText, secondaryText,
                    backgroundColor, compact = compact, body = selected)
            }
            PlanetCompassSunFinderDataPanel(rows, locationRows, orientation, readings.compassReliable,
                messageText.takeUnless { message == R.string.sun_finder_compass_accuracy }, weatherText,
                currentWeather != null, primaryText, secondaryText, backgroundColor, Modifier.fillMaxWidth().weight(1f, fill = false), compact,
                locationActionLabel, onLocationAction, bodyName = bodyName, orientationRows = orientationRows,
                bodyNavigation = navigation, locationInfoRows = locationInfoRows, bodyActions = {
                    if (hasActiveBody) PlanetCompassCelestialSkyActions(body, { showViewer = true },
                        dailyPath?.let { path -> { dailyPathUiState.openMenu(path) } }, pathActionTitle, dailyPath != null)
                })
        }
    }
    val toolbar: @Composable () -> Unit = {
        if (onDismissRequest != null) Row(
            Modifier.fillMaxWidth().testTag("celestial-toolbar")
                .background(backgroundColor.copy(alpha = 0.72f))
                .padding(horizontal = 5.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically
        ) {
            PlanetCompassBackButton(onClick = onDismissRequest)
            PlanetCompassMenuTitle(stringResource(R.string.celestial_orbits_title), Modifier.weight(1f).testTag("celestial-title"),
                color = primaryText)
            PlanetCompassCelestialSelector(body, primaryText, backgroundColor, Modifier.wrapContentSize(Alignment.CenterEnd), timeMs, remote,
                selectedBodies, onSelectionChange?.let { { dailyPathUiState.clearSelection(); it(selection.toggleAll()) } }) {
                dailyPathUiState.clearSelection()
                if (onSelectionChange != null) onSelectionChange(selection.toggle(it)) else onBodyChange(it)
            }
        }
    }
    val pointing: @Composable (Boolean) -> Unit = { landscape ->
        PlanetCompassSunPointingViewport(target, pointingOrientation,
            Modifier.fillMaxSize().then(pointingPlacement), skyDescription, dailyPathUiState, rising,
            readings.compassReliable, dailyPath.takeIf { hasActiveBody }, primaryText, secondaryText, backgroundColor, body, timeMs,
            timeBadgeExclusions, showSelectedPanel = !landscape, onVisualize = { showViewer = true },
            compassWarning = messageText.takeIf { message == R.string.sun_finder_compass_accuracy },
            showActions = false, offscreenBody = body.takeIf { hasActiveBody }, overlays = overlays, onActivateBody = {
                if (it in selectedBodies) onBodyChange(it) })
    }
    BoxWithConstraints(Modifier.fillMaxSize().testTag("sun-finder-content").onGloballyPositioned {
        sceneCoordinates = it
        refreshGroundFrame()
    }) {
        val viewportWidth = maxWidth
        val viewportHeight = maxHeight
        PlanetCompassSunSkyBackdrop(phase, currentWeather, Modifier.fillMaxSize())
        PlanetCompassSunGroundBackdrop(orientation, groundFrame, phase, Modifier.fillMaxSize())
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
    PlanetCompassSunDailyPathDialog(dailyPathUiState, primaryText, secondaryText, backgroundColor)
}
