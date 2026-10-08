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
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
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
import kotlin.math.abs

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
    val facts = remember(body) { spaceCompassCelestialFacts(body) }
    val temperatures = remember(body) { spaceCompassCelestialTemperatures(body) }
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
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    var resumed by remember { mutableStateOf(lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)) }
    DisposableEffect(lifecycle) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) resumed = true
            if (event == Lifecycle.Event.ON_PAUSE || event == Lifecycle.Event.ON_STOP) resumed = false
        }
        lifecycle.addObserver(observer); onDispose { lifecycle.removeObserver(observer) }
    }
    LaunchedEffect(rotating, automatic, resumed) {
        // Cap to 30 updates/s and stop entirely off-screen or in a manually held orientation.
        if (rotating && automatic && resumed) {
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
    LaunchedEffect(body, rotating, returning, resumed) {
        if (rotating && returning && resumed) {
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
    BackHandler(onBack = onBack)
    fun number(value: Double?, decimals: Int, unit: String) = value?.takeIf { it.isFinite() }
        ?.let { "${formatSpaceCompassNumber(it, decimals, numeric)} $unit" } ?: "—"
    val referencePressure = formatSpaceCompassPressure(SpaceCompassPressureUnit.BAR.pascals, numeric, units.pressure)!!
    val temperatureRows = if (temperatures.isEmpty()) listOf(
        stringResource(R.string.celestial_temperature) to "—"
    ) else temperatures.map {
        val label = if (it.kind == SpaceCompassCelestialTemperatureKind.ATMOSPHERE_ONE_BAR)
            stringResource(it.kind.labelResource, referencePressure) else stringResource(it.kind.labelResource)
        (label + (it.component?.let { name -> " ($name)" } ?: "")) to formatSpaceCompassCelestialTemperature(it, numeric, units.fahrenheit)
    }
    val pressure = spaceCompassAtmosphericPressure(body)
    val pressureRows = pressure?.let { reference ->
        formatSpaceCompassAtmosphericPressure(reference, numeric, units.pressure)?.let {
            listOf(stringResource(reference.kind.labelResource) to it)
        }
    } ?: emptyList()
    val rows = listOf(
        stringResource(R.string.celestial_view_diameter) to formatSpaceCompassCelestialDiameter(facts, numeric, units.feet),
        stringResource(if (body.isVoyager) R.string.celestial_view_antenna else R.string.celestial_view_size) to formatSpaceCompassPhysicalLength(facts.dimensionMeters, 1, numeric, units.feet),
        stringResource(R.string.celestial_view_mass) to formatSpaceCompassCelestialMass(body, facts, numeric, units.pounds),
        stringResource(R.string.celestial_view_gravity) to formatSpaceCompassCelestialGravity(facts.gravity, numeric,
            fractionDigits = if (body == SpaceCompassCelestialBody.POLARIS) 2 else 1, feet = units.feet),
        stringResource(R.string.celestial_view_density) to formatSpaceCompassCelestialDensity(facts.density, numeric, units,
            fractionDigits = if (body == SpaceCompassCelestialBody.POLARIS) 3 else 0),
    ) + pressureRows + temperatureRows + listOf(
        stringResource(R.string.celestial_view_parent) to when {
            facts.parentName != null -> facts.parentName
            facts.parentIsEarth -> stringResource(R.string.celestial_view_earth)
            facts.parent != null -> stringResource(facts.parent.nameResource)
            else -> "—"
        },
        stringResource(R.string.celestial_view_rotation_period) to formatSpaceCompassRotationPeriod(facts, numeric),
        stringResource(R.string.celestial_view_retrograde) to if (facts.rotationHours == null) "—" else
            stringResource(if (facts.rotationHours < 0) R.string.celestial_view_yes else R.string.celestial_view_no),
        stringResource(R.string.celestial_view_revolution_period) to number(facts.revolutionDays ?: remote.satelliteOrbit(body)?.takeIf { it.usable(timeMs) }?.periodMs?.div(86_400_000.0), 4, "d"),
        stringResource(R.string.celestial_view_min_distance) to (formatSpaceCompassSelectedDistance(body, facts.minimumParentKm, numeric, units.distance, units.feet) ?: "—"),
        stringResource(R.string.celestial_view_max_distance) to (formatSpaceCompassSelectedDistance(body, facts.maximumParentKm, numeric, units.distance, units.feet) ?: "—"),
        stringResource(R.string.celestial_view_observer_distance) to (formatSpaceCompassSelectedDistance(body, distance, numeric, units.distance, units.feet) ?: "—"),
        stringResource(R.string.celestial_view_phase) to (phase?.let { stringResource(it.kind.nameResource) } ?: "—"),
        stringResource(R.string.celestial_view_illuminated) to number((geometry?.illuminatedFraction ?: phase?.illuminatedFraction)?.times(100), 1, "%")
    )
    val supplementalRows = buildList {
        facts.apparentMagnitude?.let { add(stringResource(R.string.celestial_apparent_magnitude) to
            formatSpaceCompassNumber(it, 2, numeric, minimumDigits = 0)) }
        facts.spectralType?.let { add(stringResource(R.string.celestial_spectral_type) to it) }
        facts.luminositySolar?.let { add(stringResource(R.string.celestial_luminosity) to
            "≈ " + formatSpaceCompassNumber(it, if (it < 1) 4 else 0, numeric, minimumDigits = 0) + " L☉") }
        facts.binaryPeriodDays?.let { add(stringResource(R.string.celestial_view_revolution_period) + " (" +
            (if (body == SpaceCompassCelestialBody.POLARIS) "Aa–Ab" else if (body == SpaceCompassCelestialBody.ALPHA_CENTAURI || body == SpaceCompassCelestialBody.SIRIUS) "A–B" else "★–WD") + ")" to number(it, 3, "d")) }
        spaceCompassHorizonDiameterKm(body)?.let { add(stringResource(R.string.celestial_horizon_diameter) to
            "≈ " + formatSpaceCompassPhysicalLength(it * 1000, 0, numeric, units.feet, large = true)) }
        for (component in spaceCompassStellarComponents(body)) {
            add(stringResource(R.string.celestial_luminosity) + " (${component.name})" to
                formatSpaceCompassNumber(component.luminositySolar, 4, numeric, minimumDigits = 0) + " L☉")
            add(stringResource(R.string.celestial_view_mass) + " (${component.name})" to
                formatSpaceCompassCelestialMass(SpaceCompassCelestialBody.SUN, SpaceCompassCelestialFacts(
                    massSolar = component.massSolar, massSolarError = component.massError), numeric, units.pounds))
            add(stringResource(R.string.celestial_view_diameter) + " (${component.name})" to
                formatSpaceCompassPhysicalLength(2 * component.radiusSolar * 695700000, 0, numeric, units.feet, large = true) +
                " (±" + formatSpaceCompassPhysicalLength(2 * component.radiusError * 695700000, 0, numeric, units.feet, large = true) + ")")
        }
    }
    val informationRows = if (body == SpaceCompassCelestialBody.EARTH_CENTER) listOf(
        stringResource(R.string.celestial_view_observer_distance) to
            (formatSpaceCompassSelectedDistance(body, distance, numeric, units.distance, units.feet) ?: "—")
    ) else if (body.isExtendedSkyObject) listOf(
        stringResource(R.string.celestial_view_observer_distance) to
            (formatSpaceCompassSelectedDistance(body, distance, numeric, units.distance, units.feet) ?: "—")
    ) + supplementalRows else rows + supplementalRows
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
            Box(placement.testTag("celestial-view-model").clip(RoundedCornerShape(18.dp)).background(Color(0xff04060c)),
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
            Column(Modifier.fillMaxWidth().padding(horizontal = spaceCompassDetailScrollInset)
                .clip(RoundedCornerShape(18.dp))
                .background(primaryText.copy(alpha = 0.05f)).padding(12.dp)
                .testTag("celestial-view-information-island"), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                informationRows.forEach { (label, value) -> Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(label, Modifier.weight(0.52f), color = secondaryText, fontSize = 12.sp, lineHeight = 16.sp)
                    Text(value, Modifier.weight(0.48f), color = primaryText, fontSize = 12.sp, lineHeight = 16.sp,
                        style = TextStyle(textDirection = TextDirection.ContentOrLtr))
                } }
            }
            Column(Modifier.fillMaxWidth().padding(horizontal = spaceCompassDetailScrollInset, vertical = 4.dp)
                .testTag("celestial-view-notes"), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                if (body.isComet) Text(stringResource(R.string.celestial_comet_note), color = secondaryText, fontSize = 11.sp, lineHeight = 14.sp)
                if (body == SpaceCompassCelestialBody.STEPHENSON_2_18)
                    Text(stringResource(R.string.celestial_mass_unavailable_note), color = secondaryText, fontSize = 11.sp, lineHeight = 14.sp)
                if (body == SpaceCompassCelestialBody.PSR_J0437 || spaceCompassHorizonDiameterKm(body) != null)
                    Text(stringResource(R.string.celestial_reference_estimates_note), color = secondaryText, fontSize = 11.sp, lineHeight = 14.sp)
                if (!body.isFictional) Text(if (body == SpaceCompassCelestialBody.EARTH_CENTER) stringResource(R.string.celestial_earth_center_note)
                    else stringResource(R.string.celestial_view_facts_note, referencePressure), color = secondaryText, fontSize = 11.sp, lineHeight = 14.sp)
                if (pressureRows.isNotEmpty()) {
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

    Column(Modifier.fillMaxSize().background(backgroundColor).testTag("celestial-viewer")) {
        SpaceCompassPageToolbar(stringResource(body.nameResource), onBack,
            modifier = Modifier.testTag("celestial-view-toolbar"), titleColor = primaryText)
        BoxWithConstraints(Modifier.fillMaxSize().padding(start = 10.dp, end = 10.dp, bottom = 6.dp)) {
            // Both layouts start directly below the toolbar; only side/bottom insets remain.
            if (maxWidth > maxHeight && maxWidth >= 540.dp) Row(Modifier.fillMaxSize(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                model(Modifier.weight(1f).fillMaxHeight()); info(Modifier.weight(1f).fillMaxHeight())
            } else Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                model(Modifier.weight(1f).fillMaxWidth()); info(Modifier.weight(1f).fillMaxWidth())
            }
        }
    }
}
