package me.mondiversi.planetcompass

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.selection.selectable
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.abs

private val rotationSaver = Saver<PlanetCompassCelestialRotation, List<Any>>(
    save = { listOf(it.yaw, it.pitch, it.automatic, it.manualOrientation.w,
        it.manualOrientation.x, it.manualOrientation.y, it.manualOrientation.z, it.returning) },
    restore = { PlanetCompassCelestialRotation(it[0] as Double, it[1] as Double, it[2] as Boolean,
        if (it.size >= 7) PlanetCompassViewQuaternion(it[3] as Double, it[4] as Double, it[5] as Double, it[6] as Double)
        else PlanetCompassViewQuaternion(), if (it.size >= 8) it[7] as Boolean else false) })

private val viewportSaver = Saver<PlanetCompassCelestialViewportState, List<Double>>(
    save = { listOf(it.zoom, it.panX, it.panY) },
    restore = { PlanetCompassCelestialViewportState(it[0], it[1], it[2]) })

@Composable
internal fun PlanetCompassCelestialViewerScreen(body: PlanetCompassCelestialBody, timeMs: Long,
    latitude: Double?, longitude: Double?, altitude: Double, remote: PlanetCompassCelestialRemoteData,
    primaryText: Color, secondaryText: Color, backgroundColor: Color, onBack: () -> Unit) {
    val context = LocalContext.current
    var rotating by rememberSaveable(body) { mutableStateOf(false) }
    val rotationState = rememberSaveable(body, stateSaver = rotationSaver) {
        mutableStateOf(PlanetCompassCelestialRotation(pitch = if (body.isSpacecraft) 50.0 else 15.0))
    }
    val viewportState = rememberSaveable(body, stateSaver = viewportSaver) { mutableStateOf(PlanetCompassCelestialViewportState()) }
    val automatic by remember { derivedStateOf { rotationState.value.automatic } }
    val returning by remember { derivedStateOf { rotationState.value.returning } }
    val facts = remember(body) { planetCompassCelestialFacts(body) }
    val temperatures = remember(body) { planetCompassCelestialTemperatures(body) }
    val numeric = LocalPlanetCompassNumericFormat.current
    val geometry = key(body) {
        // A fresh fix must not dispose/recreate the GL surface while the replacement is calculated.
        produceState<PlanetCompassCelestialViewGeometry?>(null, timeMs, latitude, longitude, altitude) {
            value = if (latitude == null || longitude == null) null else withContext(Dispatchers.Default) {
                runCatching { calculatePlanetCompassCelestialViewGeometry(body, timeMs, latitude, longitude, altitude) }
                    .onFailure { PlanetCompassErrorLog.record(context, "celestial_viewer:geometry", it) }.getOrNull()
            }
        }.value
    }
    val remoteDistance = key(body) {
        produceState<Double?>(null, timeMs, remote, latitude, longitude, altitude) {
            value = if (body.hasPhysicalFace || latitude == null || longitude == null) null else withContext(Dispatchers.Default) {
                runCatching { calculatePlanetCompassCelestialObservation(body, timeMs, latitude, longitude, altitude, remote)?.distanceKm }.getOrNull()
            }
        }.value
    }
    val phase = remember(body, timeMs) { if (body == PlanetCompassCelestialBody.MOON) calculatePlanetCompassMoonPhase(timeMs) else null }
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
    fun number(value: Double?, decimals: Int, unit: String) = value?.let { "${formatPlanetCompassNumber(it, decimals, numeric)} $unit" } ?: "—"
    val temperatureRows = if (temperatures.isEmpty()) listOf(
        stringResource(R.string.celestial_temperature) to "—"
    ) else temperatures.map { stringResource(it.kind.labelResource) to formatPlanetCompassCelestialTemperature(it, numeric) }
    val rows = listOf(
        stringResource(R.string.celestial_view_diameter) to (number(facts.diameterKm, 0, "km") +
            (facts.diameterErrorKm?.let { " (±${formatPlanetCompassNumber(it, 0, numeric)} km)" } ?: "")),
        stringResource(if (body.isVoyager) R.string.celestial_view_antenna else R.string.celestial_view_size) to number(facts.dimensionMeters, 1, "m"),
        stringResource(R.string.celestial_view_mass) to facts.massKg?.let { formatPlanetCompassScientificNumber(it, numeric) + " kg" }.orEmpty().ifEmpty { "—" },
        stringResource(R.string.celestial_view_gravity) to formatPlanetCompassCelestialGravity(facts.gravity, numeric,
            siFractionDigits = if (body == PlanetCompassCelestialBody.POLARIS) 2 else 1),
        stringResource(R.string.celestial_view_density) to number(facts.density, if (body == PlanetCompassCelestialBody.POLARIS) 3 else 0, "kg/m³"),
    ) + temperatureRows + listOf(
        stringResource(R.string.celestial_view_parent) to when {
            facts.parentIsEarth -> stringResource(R.string.celestial_view_earth)
            facts.parent != null -> stringResource(facts.parent.nameResource)
            else -> "—"
        },
        stringResource(R.string.celestial_view_rotation_period) to number(facts.rotationHours?.let(::abs), 2, "h"),
        stringResource(R.string.celestial_view_retrograde) to if (facts.rotationHours == null) "—" else
            stringResource(if (facts.rotationHours < 0) R.string.celestial_view_yes else R.string.celestial_view_no),
        stringResource(R.string.celestial_view_revolution_period) to number(facts.revolutionDays, 1, "d"),
        stringResource(R.string.celestial_view_min_distance) to number(facts.minimumParentKm?.div(1e6), 3, "10⁶ km"),
        stringResource(R.string.celestial_view_max_distance) to number(facts.maximumParentKm?.div(1e6), 3, "10⁶ km"),
        stringResource(R.string.celestial_view_observer_distance) to formatPlanetCompassCelestialObserverDistance(distance, numeric),
        stringResource(R.string.celestial_view_phase) to (phase?.let { stringResource(it.kind.nameResource) } ?: "—"),
        stringResource(R.string.celestial_view_illuminated) to number((geometry?.illuminatedFraction ?: phase?.illuminatedFraction)?.times(100), 1, "%")
    )
    val actual = stringResource(R.string.celestial_view_current)
    val spin = stringResource(R.string.celestial_view_rotation)
    val resume = stringResource(R.string.celestial_view_resume)
    val note = stringResource(when {
        body == PlanetCompassCelestialBody.POLARIS -> R.string.celestial_polaris_model
        rotating -> R.string.celestial_free_rotation_hint
        body.isSpacecraft -> R.string.celestial_view_craft_note
        body == PlanetCompassCelestialBody.SEDNA -> R.string.celestial_view_sedna_note
        else -> R.string.celestial_view_map_note
    })
    val model: @Composable (Modifier) -> Unit = { placement ->
            Box(placement.testTag("celestial-view-model").clip(RoundedCornerShape(18.dp)).background(Color(0xff04060c)),
                contentAlignment = Alignment.Center) {
                // Only this restartable region reads the animation angle: facts do not recompose at 30 Hz.
                val viewGeometry = if (rotating || !body.hasPhysicalFace) rotationState.value.geometry() else geometry.takeIf { hasLocation }
                if (viewGeometry != null) PlanetCompassCelestialModelViewport(body, viewGeometry, rotating, rotationState.value,
                    { rotationState.value = it }, viewportState.value, { viewportState.value = it },
                    stringResource(body.nameResource), resume, Modifier.fillMaxSize())
                else Text(stringResource(R.string.celestial_view_location_needed), Modifier.padding(16.dp), color = Color.White, fontSize = 13.sp)
                // Sibling overlays receive their own touches; the model's drag/pinch never intercepts tabs.
                Row(Modifier.align(Alignment.TopEnd).padding(4.dp)) {
                    PlanetCompassCelestialIconControl(!rotating, actual, { rotating = false },
                        Modifier.testTag("celestial-view-current")) {
                        PlanetCompassPasswordVisibilityIcon(true, Modifier.size(18.dp), Color.White)
                    }
                    PlanetCompassCelestialIconControl(rotating, spin, { rotating = true },
                        Modifier.testTag("celestial-view-rotation")) {
                        Canvas(Modifier.size(18.dp)) {
                            val inset = 2.dp.toPx()
                            drawArc(Color.White, -45f, 285f, false, Offset(inset, inset),
                                Size(size.width - 2 * inset, size.height - 2 * inset),
                                style = Stroke(1.6.dp.toPx(), cap = StrokeCap.Round))
                            val tip = Offset(size.width - inset, size.height * 0.30f)
                            drawLine(Color.White, tip - Offset(4.dp.toPx(), 0f), tip,
                                1.6.dp.toPx(), StrokeCap.Round)
                            drawLine(Color.White, tip + Offset(0f, 4.dp.toPx()), tip,
                                1.6.dp.toPx(), StrokeCap.Round)
                        }
                    }
                }
                Text(note, color = Color(0xffd7dae0), fontSize = 10.sp, lineHeight = 12.sp,
                    modifier = Modifier.align(Alignment.BottomCenter).padding(6.dp)
                        .background(Color.Black.copy(alpha = 0.72f), RoundedCornerShape(8.dp))
                        .padding(horizontal = 6.dp, vertical = 3.dp).testTag("celestial-view-model-note"))
            }
    }
    val info: @Composable (Modifier) -> Unit = { placement ->
        val scroll = rememberScrollState()
        Column(placement.testTag("celestial-view-information-island").clip(RoundedCornerShape(18.dp)).background(primaryText.copy(alpha = 0.05f))
            .scrollbarOverlay(scroll, secondaryText.copy(alpha = 0.46f)).verticalScroll(scroll)
            .padding(12.dp).testTag("celestial-view-information"), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            rows.forEach { (label, value) -> Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(label, Modifier.weight(0.52f), color = secondaryText, fontSize = 12.sp, lineHeight = 16.sp)
                Text(value, Modifier.weight(0.48f), color = primaryText, fontSize = 12.sp, lineHeight = 16.sp)
            } }
            HorizontalDivider(color = secondaryText.copy(alpha = 0.22f))
            Text(stringResource(R.string.celestial_view_facts_note), color = secondaryText, fontSize = 11.sp, lineHeight = 14.sp)
            if (temperatures.isNotEmpty()) Text(stringResource(R.string.celestial_temperature_note),
                color = secondaryText, fontSize = 11.sp, lineHeight = 14.sp)
            if (body == PlanetCompassCelestialBody.POLARIS) Text(stringResource(R.string.celestial_polaris_derived),
                color = secondaryText, fontSize = 11.sp, lineHeight = 14.sp)
            if (body == PlanetCompassCelestialBody.STARLINK_V3) Text(stringResource(R.string.celestial_starlink_note),
                color = secondaryText, fontSize = 11.sp, lineHeight = 14.sp)
            if (body == PlanetCompassCelestialBody.SUN || body == PlanetCompassCelestialBody.SEDNA || body == PlanetCompassCelestialBody.ISS || body == PlanetCompassCelestialBody.POLARIS) {
                Text(stringResource(when (body) {
                    PlanetCompassCelestialBody.SUN -> R.string.celestial_view_sun_note
                    PlanetCompassCelestialBody.SEDNA -> R.string.celestial_view_sedna_facts
                    PlanetCompassCelestialBody.POLARIS -> R.string.celestial_polaris_facts
                    else -> R.string.celestial_view_iss_note
                }), color = secondaryText, fontSize = 11.sp, lineHeight = 14.sp)
            }
            Text(stringResource(R.string.celestial_view_credits), color = secondaryText, fontSize = 10.sp, lineHeight = 13.sp)
            if (body.isJovianMoon) Text(stringResource(R.string.celestial_view_jovian_credits), color = secondaryText,
                fontSize = 10.sp, lineHeight = 13.sp)
        }
    }
    Column(Modifier.fillMaxSize().background(backgroundColor).testTag("celestial-viewer")) {
        Row(Modifier.fillMaxWidth().testTag("celestial-view-toolbar").padding(horizontal = 5.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
            PlanetCompassBackButton(onBack)
            PlanetCompassMenuTitle(stringResource(body.nameResource), Modifier.weight(1f), color = primaryText)
        }
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
