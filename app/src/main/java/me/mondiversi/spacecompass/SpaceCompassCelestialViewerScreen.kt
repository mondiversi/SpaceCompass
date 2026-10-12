package me.mondiversi.spacecompass

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.layout
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private val rotationSaver = Saver<SpaceCompassCelestialRotation, List<Any>>(
    save = { listOf(it.yaw, it.pitch, it.automatic, it.manualOrientation.w,
        it.manualOrientation.x, it.manualOrientation.y, it.manualOrientation.z, it.returning) },
    restore = { SpaceCompassCelestialRotation(it[0] as Double, it[1] as Double, it[2] as Boolean,
        if (it.size >= 7) SpaceCompassViewQuaternion(it[3] as Double, it[4] as Double, it[5] as Double, it[6] as Double)
        else SpaceCompassViewQuaternion(), if (it.size >= 8) it[7] as Boolean else false) })

private val viewportSaver = Saver<SpaceCompassCelestialViewportState, List<Double>>(
    save = { listOf(it.zoom, it.panX, it.panY) },
    restore = { SpaceCompassCelestialViewportState(it[0], it[1], it[2]).transform(1.0) })

@Composable
internal fun SpaceCompassCelestialViewerScreen(body: SpaceCompassCelestialBody, timeMs: Long,
    latitude: Double?, longitude: Double?, altitude: Double, remote: SpaceCompassCelestialRemoteData,
    primaryText: Color, secondaryText: Color, backgroundColor: Color, onBack: () -> Unit) {
    val context = LocalContext.current
    var rotating by rememberSaveable(body) { mutableStateOf(false) }
    val rotationState = rememberSaveable(body, stateSaver = rotationSaver) {
        mutableStateOf(SpaceCompassCelestialRotation(pitch = if (body.isSpacecraft) 50.0 else 15.0))
    }
    val viewportState = rememberSaveable(body, stateSaver = viewportSaver) { mutableStateOf(SpaceCompassCelestialViewportState()) }
    val automatic by remember { derivedStateOf { rotationState.value.automatic } }
    val returning by remember { derivedStateOf { rotationState.value.returning } }
    var modelSize by remember(body) { mutableStateOf(IntSize.Zero) }
    val facts = remember(body) { spaceCompassCelestialFacts(body) }
    val temperatures = remember(body) { spaceCompassCelestialTemperatures(body) }
    val coreReference = remember(body) { spaceCompassCelestialCoreTemperature(body) }
    val numeric = LocalSpaceCompassNumericFormat.current
    val units = LocalSpaceCompassUnits.current
    val geometry = key(body) {
        // A fresh fix must not dispose/recreate the GL surface while the replacement is calculated.
        produceState<SpaceCompassCelestialViewGeometry?>(null, timeMs, latitude, longitude, altitude,
            remote.ephemerides[body].takeIf { body == SpaceCompassCelestialBody.TITAN }) {
            value = if (latitude == null || longitude == null) null else withContext(Dispatchers.Default) {
                runCatching { calculateSpaceCompassCelestialViewGeometry(body, timeMs, latitude, longitude, altitude, remote) }
                    .onFailure { SpaceCompassErrorLog.record(context, "celestial_viewer:geometry", it) }.getOrNull()
            }
        }.value
    }
    val remoteDistance = key(body) {
        produceState<Double?>(null, timeMs, remote, latitude, longitude, altitude) {
            value = if (body.hasPhysicalFace || latitude == null || longitude == null) null else withContext(Dispatchers.Default) {
                runCatching { calculateSpaceCompassCelestialObservation(body, timeMs, latitude, longitude, altitude, remote)?.distanceKm }.getOrNull()
            }
        }.value
    }
    val phase = remember(body, timeMs) { if (body == SpaceCompassCelestialBody.MOON) calculateSpaceCompassMoonPhase(timeMs) else null }
    val hasLocation = latitude != null && longitude != null
    val distance = if (!hasLocation) null else geometry?.distanceKm ?: remoteDistance
    val coordinateTimeMs = timeMs / 1000 * 1000
    val coordinates = key(body) {
        produceState<SpaceCompassCelestialCoordinates?>(null, coordinateTimeMs, remote) {
            value = withContext(Dispatchers.Default) {
                runCatching { calculateSpaceCompassCelestialCoordinates(body, coordinateTimeMs, remote) }
                    .onFailure { SpaceCompassErrorLog.record(context, "celestial_viewer:coordinates", it) }.getOrNull()
            }
        }.value
    }
    val export = rememberSpaceCompassObservatoryExport(body,timeMs,remote,distance,phase,geometry,coordinates,
        { rotationState.value },{ viewportState.value },{ modelSize })
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    var resumed by remember { mutableStateOf(lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)) }
    DisposableEffect(lifecycle) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) resumed = true
            if (event == Lifecycle.Event.ON_PAUSE || event == Lifecycle.Event.ON_STOP) resumed = false
        }
        lifecycle.addObserver(observer); onDispose { lifecycle.removeObserver(observer) }
    }
    LaunchedEffect(rotating, automatic, resumed, export.visible) {
        // Cap to 30 updates/s and stop entirely off-screen or in a manually held orientation.
        if (rotating && automatic && resumed && !export.visible) {
            var previous = 0L
            while (true) withFrameNanos { frame ->
                if (previous == 0L) previous = frame
                else if (frame - previous >= 32_000_000) {
                    rotationState.value = rotationState.value.advance((frame - previous) / 1e9, (facts.rotationHours ?: 0.0) < 0)
                    previous = frame
                }
            }
        }
    }
    LaunchedEffect(body, rotating, returning, resumed, export.visible) {
        if (rotating && returning && resumed && !export.visible) {
            if (!android.animation.ValueAnimator.areAnimatorsEnabled()) {
                rotationState.value = rotationState.value.resume()
            } else {
                val start = rotationState.value.manualOrientation
                var started = 0L
                var previous = 0L
                var completed = false
                while (!completed) withFrameNanos { frame ->
                    if (started == 0L) started = frame
                    val fraction = ((frame-started)/450_000_000.0).coerceIn(0.0, 1.0)
                    if (fraction >= 1) {
                        rotationState.value = rotationState.value.resume()
                        completed = true
                    } else if (frame-previous >= 32_000_000) {
                        val smooth = fraction*fraction*(3-2*fraction)
                        rotationState.value = rotationState.value.copy(manualOrientation = start.returning(smooth))
                        previous = frame
                    }
                }
            }
        }
    }
    BackHandler { if (export.visible) export.close() else if (!export.saving) onBack() }
    val referencePressure = formatSpaceCompassPressure(SpaceCompassPressureUnit.BAR.pascals, numeric, units.pressure)!!
    val actual = stringResource(R.string.celestial_view_current)
    val spin = stringResource(R.string.celestial_view_rotation)
    val resume = stringResource(R.string.celestial_view_resume)
    val note = stringResource(when {
        body.isFictional -> R.string.celestial_lv426_model_note
        body == SpaceCompassCelestialBody.EARTH_CENTER -> R.string.celestial_earth_center_note
        body == SpaceCompassCelestialBody.TITAN -> R.string.celestial_view_map_note
        body.deepSkyReference != null -> body.deepSkyNoteResource
        body == SpaceCompassCelestialBody.POLARIS -> R.string.celestial_polaris_model
        rotating -> R.string.celestial_free_rotation_hint
        body.isComet -> R.string.celestial_comet_note
        body.isSpacecraft -> R.string.celestial_view_craft_note
        body == SpaceCompassCelestialBody.SEDNA -> R.string.celestial_view_sedna_note
        else -> R.string.celestial_view_map_note
    })
    val model: @Composable (Modifier) -> Unit = { placement ->
            Box(placement.padding(horizontal = spaceCompassDetailScrollInset).testTag("celestial-view-model").onSizeChanged { modelSize = it }.clip(RoundedCornerShape(18.dp)).background(Color(0xff04060c)),
                contentAlignment = Alignment.Center) {
                // Only this restartable region reads the animation angle: facts do not recompose at 30 Hz.
                val viewGeometry = if (rotating || !body.hasPhysicalFace) rotationState.value.geometry() else geometry.takeIf { hasLocation }
                val symbolPlacement = Modifier.fillMaxSize().graphicsLayer {
                    val framing = viewportState.value
                    scaleX = framing.zoom.toFloat(); scaleY = framing.zoom.toFloat()
                    translationX = (framing.panX * size.width / 2).toFloat()
                    translationY = (-framing.panY * size.height / 2).toFloat()
                }
                if (body == SpaceCompassCelestialBody.EARTH_CENTER) SpaceCompassEarthCenterSymbol(symbolPlacement)
                else if (body.usesDeepSkySymbol) SpaceCompassDeepSkySymbol(body, symbolPlacement)
                else if (viewGeometry != null) SpaceCompassCelestialModelViewport(body, viewGeometry, rotating, rotationState.value,
                    { rotationState.value = it }, viewportState.value, { viewportState.value = it },
                    stringResource(body.nameResource), resume, Modifier.fillMaxSize())
                else if (body == SpaceCompassCelestialBody.TITAN && hasLocation) Text(
                    if (remote.loading) stringResource(R.string.celestial_loading)
                    else stringResource(R.string.celestial_unavailable, stringResource(body.nameResource)),
                    Modifier.padding(16.dp), color = Color.White, fontSize = 13.sp)
                else Text(stringResource(R.string.celestial_view_location_needed), Modifier.padding(16.dp), color = Color.White, fontSize = 13.sp)
                // Sibling overlays receive their own touches; zoom never starts a model drag or resets rotation.
                SpaceCompassCelestialZoomControls(viewportState.value,
                    onZoom = { factor -> viewportState.value = viewportState.value.transform(factor) },
                    modifier = Modifier.align(Alignment.TopStart).padding(4.dp),
                    enabled = viewGeometry != null || body.usesDeepSkySymbol || body == SpaceCompassCelestialBody.EARTH_CENTER)
                // Sibling overlays receive their own touches; the model's drag/pinch never intercepts tabs.
                if (!body.usesDeepSkySymbol && !body.hasCatalogPhotograph && body != SpaceCompassCelestialBody.EARTH_CENTER)
                    SpaceCompassCelestialViewModeSwitch(rotating, actual, spin, { rotating = it },
                        Modifier.align(Alignment.TopEnd).padding(4.dp))
                if (!body.hasCatalogPhotograph) Text(note, color = Color(0xffd7dae0), fontSize = 10.sp, lineHeight = 12.sp,
                    modifier = Modifier.align(Alignment.BottomCenter).padding(6.dp)
                        .background(Color.Black.copy(alpha = 0.72f), RoundedCornerShape(8.dp))
                        .padding(horizontal = 6.dp, vertical = 3.dp).testTag("celestial-view-model-note"))
            }
    }
    val info: @Composable (Modifier) -> Unit = { placement ->
        val scroll = rememberScrollState()
        Column(placement.testTag("celestial-view-information")
            .scrollbarOverlay(scroll, secondaryText.copy(alpha = 0.46f)).verticalScroll(scroll),
            verticalArrangement = Arrangement.spacedBy(8.dp)) {
            SpaceCompassCelestialDetailSections(body, facts, timeMs, remote, distance, phase,
                geometry?.illuminatedFraction, coordinates, primaryText, secondaryText)
            Column(Modifier.fillMaxWidth().padding(start = spaceCompassDetailScrollInset, top = 4.dp,
                end = spaceCompassDetailScrollInset)
                .testTag("celestial-view-notes"), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                if (body.isComet) Text(stringResource(R.string.celestial_comet_note), color = secondaryText, fontSize = 11.sp, lineHeight = 14.sp)
                if (body == SpaceCompassCelestialBody.STEPHENSON_2_18)
                    Text(stringResource(R.string.celestial_mass_unavailable_note), color = secondaryText, fontSize = 11.sp, lineHeight = 14.sp)
                if (body == SpaceCompassCelestialBody.PSR_J0437 || spaceCompassHorizonDiameterKm(body) != null)
                    Text(stringResource(R.string.celestial_reference_estimates_note), color = secondaryText, fontSize = 11.sp, lineHeight = 14.sp)
                if (!body.isFictional) Text(if (body == SpaceCompassCelestialBody.EARTH_CENTER) stringResource(R.string.celestial_earth_center_note)
                    else stringResource(R.string.celestial_view_facts_note, referencePressure), color = secondaryText, fontSize = 11.sp, lineHeight = 14.sp)
                if (spaceCompassAtmosphericPressure(body) != null) {
                    Text(stringResource(R.string.celestial_pressure_reference_note), color = secondaryText,
                        fontSize = 11.sp, lineHeight = 14.sp)
                    if (body.isJovianMoon) Text("Bagenal & Dols (2020)", color = secondaryText, fontSize = 10.sp, lineHeight = 13.sp)
                }
                if (temperatures.isNotEmpty() && !body.isFictional) Text(stringResource(R.string.celestial_temperature_note),
                    color = secondaryText, fontSize = 11.sp, lineHeight = 14.sp)
                if (temperatures.any { it.kind in setOf(SpaceCompassCelestialTemperatureKind.SURFACE_ESTIMATE,
                        SpaceCompassCelestialTemperatureKind.EQUILIBRIUM_MODEL,
                        SpaceCompassCelestialTemperatureKind.HISTORICAL_SURFACE_RANGE,
                        SpaceCompassCelestialTemperatureKind.THERMAL_MODEL) })
                    Text(stringResource(R.string.celestial_temperature_reference_note),
                        color = secondaryText, fontSize = 11.sp, lineHeight = 14.sp)
                coreReference?.let {
                    Text(stringResource(R.string.celestial_temperature_core_note), color = secondaryText,
                        fontSize = 11.sp, lineHeight = 14.sp)
                    if (it.temperature.kind == SpaceCompassCelestialTemperatureKind.CORE_BOUNDARY_ESTIMATE)
                        Text(stringResource(R.string.celestial_temperature_core_boundary_note), color = secondaryText,
                            fontSize = 11.sp, lineHeight = 14.sp)
                    if (it.adiabaticReference) Text(stringResource(R.string.celestial_temperature_core_adiabatic_note),
                        color = secondaryText, fontSize = 11.sp, lineHeight = 14.sp)
                    Text(it.citation, color = secondaryText, fontSize = 10.sp, lineHeight = 13.sp)
                }
                if (body == SpaceCompassCelestialBody.STEPHENSON_2_18)
                    Text(stringResource(R.string.celestial_stephenson_reference_note),
                        color = secondaryText, fontSize = 11.sp, lineHeight = 14.sp)
                if (spaceCompassUsesSolarMass(facts))
                    Text(stringResource(R.string.celestial_solar_mass_note), color = secondaryText, fontSize = 11.sp, lineHeight = 14.sp)
                else if (spaceCompassCelestialMassKilograms(facts) != null && (facts.massModelAssumption || facts.massEstimated))
                    Text(stringResource(R.string.celestial_mass_reference_note), color = secondaryText, fontSize = 11.sp, lineHeight = 14.sp)
                if (body == SpaceCompassCelestialBody.TITAN) Text(stringResource(R.string.celestial_titan_note),
                    color = secondaryText, fontSize = 11.sp, lineHeight = 14.sp)
                if (body == SpaceCompassCelestialBody.ANDROMEDA_CORE) {
                    Text(stringResource(R.string.celestial_andromeda_galaxy_title), color = primaryText,
                        fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
                    SpaceCompassCatalogPhotograph("andromeda_galaxy.webp", Modifier.fillMaxWidth().height(150.dp))
                    spaceCompassCatalogImageCredit("andromeda_galaxy.webp")?.let { credit ->
                        Text(credit, color = secondaryText, fontSize = 10.sp, lineHeight = 13.sp)
                    }
                    Text(stringResource(R.string.celestial_andromeda_galaxy_note), color = secondaryText,
                        fontSize = 11.sp, lineHeight = 14.sp)
                }
                spaceCompassCatalogImageCredit(body.viewerTexture)?.let { credit ->
                    Text(credit, color = secondaryText, fontSize = 10.sp, lineHeight = 13.sp)
                }
                if (body.hasCatalogPhotograph) Text(stringResource(R.string.celestial_archival_image),
                    color = secondaryText, fontSize = 11.sp, lineHeight = 14.sp)
                if (body.deepSkyReference != null) Text(stringResource(body.deepSkyNoteResource),
                    color = secondaryText, fontSize = 11.sp, lineHeight = 14.sp)
                if (body == SpaceCompassCelestialBody.POLARIS) Text(stringResource(R.string.celestial_polaris_derived),
                    color = secondaryText, fontSize = 11.sp, lineHeight = 14.sp)
                if (body == SpaceCompassCelestialBody.STARLINK_V3) Text(stringResource(R.string.celestial_starlink_note),
                    color = secondaryText, fontSize = 11.sp, lineHeight = 14.sp)
                if (body == SpaceCompassCelestialBody.SUN || body == SpaceCompassCelestialBody.SEDNA || body == SpaceCompassCelestialBody.ISS || body == SpaceCompassCelestialBody.POLARIS) {
                    Text(stringResource(when (body) {
                        SpaceCompassCelestialBody.SUN -> R.string.celestial_view_sun_note
                        SpaceCompassCelestialBody.SEDNA -> R.string.celestial_view_sedna_facts
                        SpaceCompassCelestialBody.POLARIS -> R.string.celestial_polaris_facts
                        else -> R.string.celestial_view_iss_note
                    }), color = secondaryText, fontSize = 11.sp, lineHeight = 14.sp)
                }
                Text(stringResource(if (body.isFictional) R.string.celestial_lv426_credits else R.string.celestial_view_credits), color = secondaryText, fontSize = 10.sp, lineHeight = 13.sp)
                if (body.isJovianMoon) Text(stringResource(R.string.celestial_view_jovian_credits), color = secondaryText,
                    fontSize = 10.sp, lineHeight = 13.sp)
            }
        }
    }

    Box(Modifier.fillMaxSize()) {
    Column(Modifier.fillMaxSize().layout { measurable,constraints ->
        val placeable=measurable.measure(constraints)
        layout(placeable.width,placeable.height) { if (!export.visible) placeable.placeRelative(0,0) }
    }.background(backgroundColor).testTag("celestial-viewer")) {
        SpaceCompassPageToolbar(stringResource(R.string.celestial_details_title), { if (!export.saving) onBack() },
            modifier = Modifier.testTag("celestial-view-toolbar"),
            titleModifier = Modifier.testTag("celestial-view-title"), titleColor = primaryText) {
            SpaceCompassTitleActionButton(stringResource(R.string.panorama_export),export.open,
                Modifier.testTag("observatory-export"),enabled=!export.saving) { SpaceCompassPanoramaExportIcon(2.dp) }
        }
        BoxWithConstraints(Modifier.fillMaxSize().padding(horizontal = 10.dp)) {
            // Start below the toolbar with side insets; system bars already own the bottom safe area.
            if (maxWidth > maxHeight && maxWidth >= 540.dp) Row(Modifier.fillMaxSize(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                model(Modifier.weight(1f).fillMaxHeight()); info(Modifier.weight(1f).fillMaxHeight())
            } else Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                model(Modifier.weight(1f).fillMaxWidth()); info(Modifier.weight(1f).fillMaxWidth())
            }
        }
    }
    if (export.visible) SpaceCompassPanoramaExportPage(export)
    }
}
