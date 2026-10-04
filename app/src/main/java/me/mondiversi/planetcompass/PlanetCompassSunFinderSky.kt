package me.mondiversi.planetcompass

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.text.style.TextAlign
import java.time.ZoneId
import kotlin.math.*

/** Full-screen backdrop, separate from the pointing viewport so panels never get an opaque sky margin. */
@Composable
internal fun PlanetCompassSunSkyBackdrop(phase: PlanetCompassSunSkyPhase, weather: PlanetCompassSunWeatherSnapshot?, modifier: Modifier) {
    val palette = when (phase) {
        PlanetCompassSunSkyPhase.NIGHT -> Color(0xFF071327) to Color(0xFF203B5C)
        PlanetCompassSunSkyPhase.DAWN -> Color(0xFF5D71A2) to Color(0xFFF4C091)
        PlanetCompassSunSkyPhase.MORNING -> Color(0xFF267EBD) to Color(0xFFBEE9F1)
        PlanetCompassSunSkyPhase.AFTERNOON -> Color(0xFF176AAD) to Color(0xFF94D7ED)
        PlanetCompassSunSkyPhase.SUNSET -> Color(0xFF5C477B) to Color(0xFFFFB074)
        PlanetCompassSunSkyPhase.EVENING -> Color(0xFF182344) to Color(0xFF80607A)
    }
    val cover = planetCompassSunDisplayCloudCover(weather)
    val storm = weather?.kind == PlanetCompassSunWeatherKind.STORM
    val cloudy = cover * if (storm) 0.88f else 0.65f
    val night = phase == PlanetCompassSunSkyPhase.NIGHT || phase == PlanetCompassSunSkyPhase.EVENING
    val greyTop = if (night) Color(0xFF172231) else Color(0xFF536677)
    val greyBottom = if (night) Color(0xFF394555) else Color(0xFFB7C4CE)
    val top by animateColorAsState(lerp(palette.first, greyTop, cloudy), tween(2_000), label = "solar sky top")
    val bottom by animateColorAsState(lerp(palette.second, greyBottom, cloudy), tween(2_000), label = "solar sky horizon")
    Canvas(modifier.testTag("sun-finder-backdrop")) {
        drawRect(Brush.verticalGradient(listOf(top, bottom)))
        if (night && weather != null && cover < 0.65f && weather.kind != PlanetCompassSunWeatherKind.FOG) {
            repeat(45) { index ->
                val point = Offset(size.width * ((index * 0.618034f) % 1f),
                    size.height * (0.06f + ((index * 0.414214f) % 1f) * 0.67f))
                drawCircle(Color.White.copy(alpha = (1 - cover) * 0.65f),
                    (if (index % 4 == 0) 1.2.dp else 0.7.dp).toPx(), point)
            }
        }
        // Cloud positions are decorative, not a claim to locate actual individual clouds.
        val cloudCount = planetCompassSunDisplayCloudCount(weather)
        repeat(cloudCount) { index ->
            val cx = size.width * ((0.13f + index * 0.38197f) % 1f)
            val cy = size.height * (0.07f + (index % 4) * 0.11f)
            val r = size.minDimension * (0.055f + cover * 0.065f)
            val cloud = Path()
            repeat(4) { part ->
                val radius = r * if (part in 1..2) 1.3f else 0.9f
                val x = cx + (part - 1.5f) * r
                cloud.addOval(Rect(x - radius, cy - radius, x + radius, cy + radius))
            }
            val color = if (storm) Color(0xFF455263) else if (night) Color(0xFF7B8B9F) else Color.White
            drawPath(cloud, color.copy(alpha = 0.12f + cover * 0.30f))
        }
        when (weather?.kind) {
            PlanetCompassSunWeatherKind.FOG -> {
                repeat(5) { index ->
                    val y = size.height * (0.23f + index * 0.14f)
                    drawLine(Color(0xFFD8DFE2).copy(alpha = 0.18f), Offset(0f, y), Offset(size.width, y),
                        size.height * 0.09f, StrokeCap.Round)
                }
            }
            PlanetCompassSunWeatherKind.RAIN, PlanetCompassSunWeatherKind.DRIZZLE, PlanetCompassSunWeatherKind.STORM -> {
                repeat(if (weather.kind == PlanetCompassSunWeatherKind.DRIZZLE) 30 else 75) { index ->
                    val point = Offset(size.width * ((index * 0.618034f) % 1f),
                        size.height * ((index * 0.414214f) % 1f))
                    drawLine(Color(0xFFE3F2FF).copy(alpha = 0.30f), point,
                        point + Offset(-4.dp.toPx(), 13.dp.toPx()), 1.dp.toPx(), StrokeCap.Round)
                }
                if (storm) {
                    // Static lightning motif: no flashing effect.
                    val x = size.width * 0.78f; val y = size.height * 0.16f; val r = 25.dp.toPx()
                    val bolt = Path().apply {
                        moveTo(x, y); lineTo(x - r * 0.3f, y + r)
                        lineTo(x + r * 0.1f, y + r * 0.8f); lineTo(x - r * 0.1f, y + r * 1.6f)
                    }
                    drawPath(bolt, Color(0xFFFFDB87).copy(alpha = 0.6f), style = Stroke(2.dp.toPx(), cap = StrokeCap.Round))
                }
            }
            PlanetCompassSunWeatherKind.SNOW -> repeat(65) { index ->
                drawCircle(Color.White.copy(alpha = 0.65f), (1 + index % 3).dp.toPx(),
                    Offset(size.width * ((index * 0.618034f) % 1f), size.height * ((index * 0.414214f) % 1f)))
            }
            else -> Unit
        }
    }
}

/** Only selected objects enter this viewport; the inspected body alone gets an edge locator. */
@Composable
internal fun PlanetCompassSunPointingViewport(
    sun: PlanetCompassSunPosition?, orientation: PlanetCompassSunOrientation?, modifier: Modifier, description: String,
    dailyPathUiState: PlanetCompassSunDailyPathUiState,
    rising: Boolean = true, compassReliable: Boolean = true, dailyPath: PlanetCompassSunDailyPath? = null,
    primaryText: Color = Color.White, secondaryText: Color = Color.LightGray, backgroundColor: Color = Color.Black,
    body: PlanetCompassCelestialBody = PlanetCompassCelestialBody.SUN, timeMs: Long? = null,
    timeBadgeExclusions: List<PlanetCompassSunSceneFrame> = emptyList(),
    showSelectedPanel: Boolean = true, onVisualize: (() -> Unit)? = null,
    compassWarning: String? = null, showActions: Boolean = true,
    offscreenBody: PlanetCompassCelestialBody? = body,
    overlays: Map<PlanetCompassCelestialBody, PlanetCompassCelestialOverlay> = emptyMap(),
    onActivateBody: ((PlanetCompassCelestialBody) -> Unit)? = null
) {
    BoxWithConstraints(modifier.testTag("sun-finder-sky").semantics { contentDescription = description }) {
        val density = LocalDensity.current
        val rtl = LocalLayoutDirection.current == LayoutDirection.Rtl
        val width = with(density) { maxWidth.toPx().toDouble() }
        val height = with(density) { maxHeight.toPx().toDouble() }
        var menuSize by remember { mutableStateOf(IntSize.Zero) }
        var panelSize by remember { mutableStateOf(IntSize.Zero) }
        val paths = overlays.mapNotNull { (candidate, overlay) -> overlay.path?.let { candidate to it } }.toMap() +
            (dailyPath?.let { mapOf(body to it) } ?: emptyMap())
        val positions = overlays.mapNotNull { (candidate, overlay) -> overlay.observation?.position?.let { candidate to it } }.toMap() +
            (sun?.let { mapOf(body to it) } ?: emptyMap())
        val projections = remember(positions, orientation, maxWidth, maxHeight) {
            if (orientation == null) emptyMap() else positions.mapValues { (_, position) ->
                val projected = projectPlanetCompassSun(position, orientation, maxWidth.value.toDouble(), maxHeight.value.toDouble())
                val radius = PLANET_COMPASS_CELESTIAL_LIVE_PREVIEW_SIZE_DP / 2
                // A partly clipped miniature becomes a full directional locator until it fits inside the sky.
                projected.copy(visible = projected.visible && projected.x >= radius && projected.y >= radius &&
                    projected.x <= maxWidth.value - radius && projected.y <= maxHeight.value - radius)
            }
        }
        val current = timeMs?.let { moment -> positions.mapValues { PlanetCompassSunPathPoint(moment, it.value) } }.orEmpty()
        val moonPhase = rememberPlanetCompassMoonMarkerPhase(timeMs.takeIf { PlanetCompassCelestialBody.MOON in positions })
        val targets = remember(paths, current, orientation, compassReliable, width, height) {
            projectPlanetCompassCelestialSceneTargets(paths, current, orientation.takeIf { compassReliable }, width, height)
        }
        val focused = focusedPlanetCompassCelestialSceneTarget(targets, width, height,
            with(density) { PLANET_COMPASS_CELESTIAL_RETICLE_RADIUS_DP.dp.toPx().toDouble() })
        val padding = with(density) { 4.dp.toPx().toDouble() }
        val exclusions = buildList {
            addAll(timeBadgeExclusions)
            if (menuSize.width > 0) add(PlanetCompassSunSceneFrame(
                if (rtl) 0.0 else width - menuSize.width - padding, 0.0,
                menuSize.width + padding, menuSize.height + padding))
            if (showSelectedPanel && dailyPathUiState.selectedBody != null && panelSize.height > 0)
                add(PlanetCompassSunSceneFrame(0.0, height - panelSize.height - padding, width, panelSize.height + padding))
        }
        val pixelProjections = projections.mapValues { it.value.copy(x = it.value.x * density.density, y = it.value.y * density.density) }
        val liveDiameter = with(density) { PLANET_COMPASS_CELESTIAL_LIVE_PREVIEW_SIZE_DP.dp.toPx().toDouble() }
        val markerExclusions = exclusions + pixelProjections.values.filter { it.visible }.map {
            PlanetCompassSunSceneFrame(it.x - liveDiameter / 2, it.y - liveDiameter / 2, liveDiameter, liveDiameter)
        }
        val directionalProjections = planetCompassCelestialDirectionalProjections(pixelProjections, offscreenBody)
        val markerDiameter = fitPlanetCompassCelestialOffscreenDiameter(directionalProjections, width, height,
            with(density) { PLANET_COMPASS_CELESTIAL_OFFSCREEN_DIAMETER_DP.dp.toPx().toDouble() }, markerExclusions)
        val offscreen = placePlanetCompassCelestialOffscreenMarkers(directionalProjections, width, height, markerDiameter, markerExclusions)
        paths.forEach { (candidate, path) -> key(candidate) {
            PlanetCompassSunDailyPathLayer(path, orientation.takeIf { compassReliable }, dailyPathUiState,
                primaryText, secondaryText, backgroundColor, showSelectedPanel = false, showActions = false,
                interactive = false, pathTint = planetCompassCelestialPathTint(candidate),
                highlightedPoint = focused?.takeIf { it.body == candidate && !it.isCurrent }?.point)
        } }
        val projection = projections[body]
        val appearance = sun?.let { planetCompassSunAppearance(it.elevationDegrees, rising) }
        val aligned = compassReliable && projection?.visible == true && projection.separationDegrees <= 3.0
        Canvas(Modifier.fillMaxSize()) {
            val sunTint = appearance?.let { Color(it.edgeArgb) } ?: Color(0xFFFFD84D)
            val reticle = if (aligned) sunTint else Color.White
            drawCircle(Color.Black.copy(alpha = 0.30f), PLANET_COMPASS_CELESTIAL_RETICLE_RADIUS_DP.dp.toPx(), center, style = Stroke(3.dp.toPx()))
            drawCircle(reticle, PLANET_COMPASS_CELESTIAL_RETICLE_RADIUS_DP.dp.toPx(), center, style = Stroke(1.5.dp.toPx()))
            val inner = 24.dp.toPx(); val outer = 32.dp.toPx()
            listOf(Offset(1f, 0f), Offset(-1f, 0f), Offset(0f, 1f), Offset(0f, -1f)).forEach {
                drawLine(Color.Black.copy(alpha = 0.30f), center + it * inner, center + it * outer, 3.dp.toPx(), StrokeCap.Round)
                drawLine(reticle, center + it * inner, center + it * outer, 1.5.dp.toPx(), StrokeCap.Round)
            }
        }
        PlanetCompassCelestialSceneInteraction(targets, focused, dailyPathUiState, timeMs, orientation,
            primaryText, secondaryText, backgroundColor, exclusions + offscreen.values.map {
                PlanetCompassSunSceneFrame(it.center.x - markerDiameter / 2, it.center.y - markerDiameter / 2, markerDiameter, markerDiameter)
            }, onActivateBody)
        // Only thumbnails have small local targets; they never intercept another path's whole viewport.
        planetCompassCelestialCatalogOrder.forEach { candidate -> key(candidate) { projections[candidate]?.let { projected ->
            val below = positions.getValue(candidate).elevationDegrees < 0
            if (projected.visible) PlanetCompassCelestialLiveMarker(candidate, projected, below, current[candidate], dailyPathUiState,
                onActivate = { onActivateBody?.invoke(candidate) }, moonPhase = moonPhase)
            else offscreen[candidate]?.let { placement ->
                PlanetCompassCelestialOffscreenMarker(candidate, projected, below,
                    Color(planetCompassSunAppearance(positions.getValue(candidate).elevationDegrees, true).edgeArgb),
                    menuItems = 0, forcedPlacement = placement, diameter = (markerDiameter / density.density).toFloat().dp,
                    moonPhase = moonPhase)
            }
        } } }
        if (showActions) PlanetCompassCelestialSkyActions(body, onVisualize,
            dailyPath?.let { { dailyPathUiState.openMenu(it) } },
            stringResource(planetCompassCelestialPathTitle(body)),
            pathSelected = dailyPath != null,
            modifier = Modifier.align(Alignment.TopEnd).padding(4.dp).onSizeChanged { menuSize = it })
        if (showSelectedPanel) dailyPathUiState.selectedBody?.let { selected ->
            PlanetCompassSunPathSelectedPanel(paths[selected], dailyPathUiState, primaryText, secondaryText, backgroundColor,
                Modifier.align(Alignment.BottomCenter).onSizeChanged { panelSize = it }, body = selected)
        }
        if (compassWarning != null) Text(compassWarning, color = primaryText, fontSize = 12.sp,
            lineHeight = 16.sp, textAlign = TextAlign.Center,
            modifier = Modifier.align(Alignment.Center).padding(horizontal = 12.dp).widthIn(max = 280.dp)
                .background(backgroundColor.copy(alpha = 0.90f), RoundedCornerShape(14.dp))
                .padding(horizontal = 12.dp, vertical = 8.dp).testTag("celestial-compass-warning")
                .semantics { liveRegion = LiveRegionMode.Polite })
    }
}
