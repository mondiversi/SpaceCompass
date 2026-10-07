package me.mondiversi.spacecompass

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
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
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import java.time.ZoneId
import kotlin.math.*

internal fun spaceCompassSunSkyColors(phase: SpaceCompassSunSkyPhase,
    weather: SpaceCompassSunWeatherSnapshot?): Pair<Color, Color> {
    val palette = when (phase) {
        SpaceCompassSunSkyPhase.NIGHT -> Color(0xFF071327) to Color(0xFF203B5C)
        SpaceCompassSunSkyPhase.DAWN -> Color(0xFF5D71A2) to Color(0xFFF4C091)
        SpaceCompassSunSkyPhase.MORNING -> Color(0xFF267EBD) to Color(0xFFBEE9F1)
        SpaceCompassSunSkyPhase.AFTERNOON -> Color(0xFF176AAD) to Color(0xFF94D7ED)
        SpaceCompassSunSkyPhase.SUNSET -> Color(0xFF5C477B) to Color(0xFFFFB074)
        SpaceCompassSunSkyPhase.EVENING -> Color(0xFF182344) to Color(0xFF80607A)
    }
    val cover = spaceCompassSunDisplayCloudCover(weather)
    val storm = weather?.kind == SpaceCompassSunWeatherKind.STORM
    val cloudy = cover * if (storm) 0.88f else 0.65f
    val night = phase == SpaceCompassSunSkyPhase.NIGHT || phase == SpaceCompassSunSkyPhase.EVENING
    val greyTop = if (night) Color(0xFF172231) else Color(0xFF536677)
    val greyBottom = if (night) Color(0xFF394555) else Color(0xFFB7C4CE)
    return lerp(palette.first, greyTop, cloudy) to lerp(palette.second, greyBottom, cloudy)
}

/** Full-screen backdrop, separate from the pointing viewport so panels never get an opaque sky margin. */
@Composable
internal fun SpaceCompassSunSkyBackdrop(phase: SpaceCompassSunSkyPhase, weather: SpaceCompassSunWeatherSnapshot?, modifier: Modifier) {
    val palette = spaceCompassSunSkyColors(phase, weather)
    val cover = spaceCompassSunDisplayCloudCover(weather)
    val storm = weather?.kind == SpaceCompassSunWeatherKind.STORM
    val night = phase == SpaceCompassSunSkyPhase.NIGHT || phase == SpaceCompassSunSkyPhase.EVENING
    val top by animateColorAsState(palette.first, tween(2_000), label = "solar sky top")
    val bottom by animateColorAsState(palette.second, tween(2_000), label = "solar sky horizon")
    Canvas(modifier.testTag("sun-finder-backdrop")) {
        drawRect(Brush.verticalGradient(listOf(top, bottom)))
        if (night && spaceCompassSunDisplayStars(weather)) {
            repeat(45) { index ->
                val point = Offset(size.width * ((index * 0.618034f) % 1f),
                    size.height * (0.06f + ((index * 0.414214f) % 1f) * 0.67f))
                drawCircle(Color.White.copy(alpha = (1 - cover) * 0.65f),
                    (if (index % 4 == 0) 1.2.dp else 0.7.dp).toPx(), point)
            }
        }
        // Cloud positions are decorative, not a claim to locate actual individual clouds.
        val cloudCount = spaceCompassSunDisplayCloudCount(weather)
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
            SpaceCompassSunWeatherKind.FOG -> {
                repeat(5) { index ->
                    val y = size.height * (0.23f + index * 0.14f)
                    drawLine(Color(0xFFD8DFE2).copy(alpha = 0.18f), Offset(0f, y), Offset(size.width, y),
                        size.height * 0.09f, StrokeCap.Round)
                }
            }
            SpaceCompassSunWeatherKind.RAIN, SpaceCompassSunWeatherKind.DRIZZLE, SpaceCompassSunWeatherKind.STORM -> {
                repeat(if (weather.kind == SpaceCompassSunWeatherKind.DRIZZLE) 30 else 75) { index ->
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
            SpaceCompassSunWeatherKind.SNOW -> repeat(65) { index ->
                drawCircle(Color.White.copy(alpha = 0.65f), (1 + index % 3).dp.toPx(),
                    Offset(size.width * ((index * 0.618034f) % 1f), size.height * ((index * 0.414214f) % 1f)))
            }
            else -> Unit
        }
    }
}

/** Only selected objects enter this viewport; the inspected body alone gets an edge locator. */
@Composable
internal fun SpaceCompassSunPointingViewport(
    sun: SpaceCompassSunPosition?, orientation: SpaceCompassSunOrientation?, modifier: Modifier, description: String,
    dailyPathUiState: SpaceCompassSunDailyPathUiState,
    rising: Boolean = true, compassReliable: Boolean = true, dailyPath: SpaceCompassSunDailyPath? = null,
    primaryText: Color = Color.White, secondaryText: Color = Color.LightGray, backgroundColor: Color = Color.Black,
    body: SpaceCompassCelestialBody = SpaceCompassCelestialBody.SUN, timeMs: Long? = null,
    timeBadgeExclusions: List<SpaceCompassSunSceneFrame> = emptyList(),
    showSelectedPanel: Boolean = true, onVisualize: (() -> Unit)? = null,
    compassWarning: String? = null, showActions: Boolean = true,
    offscreenBody: SpaceCompassCelestialBody? = body,
    overlays: Map<SpaceCompassCelestialBody, SpaceCompassCelestialOverlay> = emptyMap(),
    onActivateBody: ((SpaceCompassCelestialBody) -> Unit)? = null, compassAccurate: Boolean = true,
    statusMessage: String? = null, statusActionLabel: String? = null, onStatusAction: () -> Unit = {},
    simulationLabel: String? = null, onSimulation: () -> Unit = {},
    perspective: SpaceCompassPerspective? = null, topActionsWidth: androidx.compose.ui.unit.Dp = 62.dp,
    observerLatitude: Double? = null, showSkyReferences: Boolean = SPACE_COMPASS_SKY_REFERENCES_DEFAULT,
    bottomActionsHeight: androidx.compose.ui.unit.Dp = 0.dp, observerAltitude: Double = 0.0
) {
    BoxWithConstraints(modifier.testTag("sun-finder-sky").semantics { contentDescription = description }) {
        val density = LocalDensity.current
        val rtl = LocalLayoutDirection.current == LayoutDirection.Rtl
        val width = with(density) { maxWidth.toPx().toDouble() }
        val height = with(density) { maxHeight.toPx().toDouble() }
        var menuSize by remember { mutableStateOf(IntSize.Zero) }
        var noticeSize by remember { mutableStateOf(IntSize.Zero) }
        var panelSize by remember { mutableStateOf(IntSize.Zero) }
        val paths = overlays.mapNotNull { (candidate, overlay) -> overlay.path?.let { candidate to it } }.toMap() +
            (dailyPath?.let { mapOf(body to it) } ?: emptyMap())
        val positions = overlays.mapNotNull { (candidate, overlay) -> overlay.observation?.position?.let { candidate to it } }.toMap() +
            (sun?.let { mapOf(body to it) } ?: emptyMap())
        val projections = remember(positions, orientation, maxWidth, maxHeight, perspective) {
            if (orientation == null) emptyMap() else positions.mapValues { (_, position) ->
                val projected = projectSpaceCompassSun(position, orientation, maxWidth.value.toDouble(), maxHeight.value.toDouble(), perspective)
                val radius = SPACE_COMPASS_CELESTIAL_LIVE_PREVIEW_SIZE_DP / 2
                // A partly clipped miniature becomes a full directional locator until it fits inside the sky.
                projected.copy(visible = projected.visible && projected.x >= radius && projected.y >= radius &&
                    projected.x <= maxWidth.value - radius && projected.y <= maxHeight.value - radius)
            }
        }
        val current = timeMs?.let { moment -> positions.mapValues { SpaceCompassSunPathPoint(moment, it.value) } }.orEmpty()
        val moonPhase = rememberSpaceCompassMoonMarkerPhase(timeMs.takeIf { SpaceCompassCelestialBody.MOON in positions })
        val targets = remember(paths, current, orientation, compassReliable, width, height, perspective) {
            projectSpaceCompassCelestialSceneTargets(paths, current, orientation.takeIf { compassReliable }, width, height, perspective)
        }
        val focused = focusedSpaceCompassCelestialSceneTarget(targets, width, height,
            with(density) { SPACE_COMPASS_CELESTIAL_RETICLE_RADIUS_DP.dp.toPx().toDouble() })
        val padding = with(density) { 4.dp.toPx().toDouble() }
        val exclusions = buildList {
            addAll(timeBadgeExclusions)
            if (noticeSize.width > 0) add(SpaceCompassSunSceneFrame(
                if (rtl) width - noticeSize.width - padding else padding, padding,
                noticeSize.width.toDouble(), noticeSize.height.toDouble()))
            if (menuSize.width > 0) add(SpaceCompassSunSceneFrame(
                if (rtl) 0.0 else width - menuSize.width - padding, 0.0,
                menuSize.width + padding, menuSize.height + padding))
            if (showSelectedPanel && dailyPathUiState.selectedBody != null && panelSize.height > 0)
                add(SpaceCompassSunSceneFrame(0.0, height - panelSize.height - padding, width, panelSize.height + padding))
        }
        val pixelProjections = projections.mapValues { it.value.copy(x = it.value.x * density.density, y = it.value.y * density.density) }
        val liveDiameter = with(density) { SPACE_COMPASS_CELESTIAL_LIVE_PREVIEW_SIZE_DP.dp.toPx().toDouble() }
        val markerExclusions = exclusions + pixelProjections.values.filter { it.visible }.map {
            SpaceCompassSunSceneFrame(it.x - liveDiameter / 2, it.y - liveDiameter / 2, liveDiameter, liveDiameter)
        }
        val directionalProjections = spaceCompassCelestialDirectionalProjections(pixelProjections, offscreenBody)
        val markerDiameter = fitSpaceCompassCelestialOffscreenDiameter(directionalProjections, width, height,
            with(density) { SPACE_COMPASS_CELESTIAL_OFFSCREEN_DIAMETER_DP.dp.toPx().toDouble() }, markerExclusions)
        val offscreen = placeSpaceCompassCelestialOffscreenMarkers(directionalProjections, width, height, markerDiameter, markerExclusions)
        val guideExclusions = markerExclusions + offscreen.values.map {
            SpaceCompassSunSceneFrame(it.center.x - markerDiameter / 2, it.center.y - markerDiameter / 2, markerDiameter, markerDiameter)
        } + targets.filterNot { it.isCurrent }.map {
            val radius = with(density) { 9.dp.toPx().toDouble() }
            SpaceCompassSunSceneFrame(it.projection.x - radius, it.projection.y - radius, radius * 2, radius * 2)
        } + with(density) {
            val radius = 34.dp.toPx().toDouble()
            listOf(SpaceCompassSunSceneFrame(width / 2 - radius, height / 2 - radius, radius * 2, radius * 2))
        }
        SpaceCompassSkyReferenceLayer(observerLatitude, timeMs, orientation.takeIf { compassReliable }, perspective,
            guideExclusions, enabled = showSkyReferences, observerAltitude = observerAltitude) {
            paths.forEach { (candidate, path) -> key(candidate) {
                SpaceCompassSunDailyPathLayer(path, orientation.takeIf { compassReliable }, dailyPathUiState,
                    primaryText, secondaryText, backgroundColor, showSelectedPanel = false, showActions = false,
                    interactive = false, pathTint = spaceCompassCelestialPathTint(candidate),
                    highlightedPoint = focused?.takeIf { it.body == candidate && !it.isCurrent }?.point, perspective = perspective)
            } }
            val projection = projections[body]
            val appearance = sun?.let { spaceCompassSunAppearance(it.elevationDegrees, rising) }
            val aligned = compassAccurate && compassReliable && projection?.visible == true && projection.separationDegrees <= 3.0
            Canvas(Modifier.fillMaxSize()) {
                val sunTint = appearance?.let { Color(it.edgeArgb) } ?: Color(0xFFFFD84D)
                val reticle = if (aligned) sunTint else Color.White
                drawCircle(Color.Black.copy(alpha = 0.30f), SPACE_COMPASS_CELESTIAL_RETICLE_RADIUS_DP.dp.toPx(), center, style = Stroke(3.dp.toPx()))
                drawCircle(reticle, SPACE_COMPASS_CELESTIAL_RETICLE_RADIUS_DP.dp.toPx(), center, style = Stroke(1.5.dp.toPx()))
                val inner = 24.dp.toPx(); val outer = 32.dp.toPx()
                listOf(Offset(1f, 0f), Offset(-1f, 0f), Offset(0f, 1f), Offset(0f, -1f)).forEach {
                    drawLine(Color.Black.copy(alpha = 0.30f), center + it * inner, center + it * outer, 3.dp.toPx(), StrokeCap.Round)
                    drawLine(reticle, center + it * inner, center + it * outer, 1.5.dp.toPx(), StrokeCap.Round)
                }
            }
            SpaceCompassCelestialSceneInteraction(targets, focused, dailyPathUiState, timeMs, orientation,
                primaryText, secondaryText, backgroundColor, exclusions + offscreen.values.map {
                    SpaceCompassSunSceneFrame(it.center.x - markerDiameter / 2, it.center.y - markerDiameter / 2, markerDiameter, markerDiameter)
                }, onActivateBody, perspective)
            // Only thumbnails have small local targets; they never intercept another path's whole viewport.
            spaceCompassAllCelestialOrder.forEach { candidate -> key(candidate) { projections[candidate]?.let { projected ->
                val below = positions.getValue(candidate).elevationDegrees < 0
                if (projected.visible) SpaceCompassCelestialLiveMarker(candidate, projected, below, current[candidate], dailyPathUiState,
                    onActivate = { onActivateBody?.invoke(candidate) }, moonPhase = moonPhase)
                else offscreen[candidate]?.let { placement ->
                    SpaceCompassCelestialOffscreenMarker(candidate, projected, below,
                        spaceCompassCelestialPathLineTint(spaceCompassCelestialPathTint(candidate), below),
                        menuItems = 0, forcedPlacement = placement, diameter = (markerDiameter / density.density).toFloat().dp,
                        moonPhase = moonPhase)
                }
            } } }
            if (showActions) SpaceCompassCelestialSkyActions(body, onVisualize,
                dailyPath?.let { { dailyPathUiState.openMenu(it) } },
                stringResource(spaceCompassCelestialPathTitle(body)),
                pathSelected = dailyPath != null,
                modifier = Modifier.align(Alignment.TopEnd).padding(4.dp).onSizeChanged { menuSize = it })
            if (showSelectedPanel) dailyPathUiState.selectedBody?.let { selected ->
                SpaceCompassSunPathSelectedPanel(paths[selected], dailyPathUiState, primaryText, secondaryText, backgroundColor,
                    Modifier.align(Alignment.BottomCenter).onSizeChanged { panelSize = it }
                        .padding(bottom = bottomActionsHeight), body = selected, nowMs = timeMs)
            }
            SpaceCompassSunStatusNotices(compassWarning, statusMessage, statusActionLabel, onStatusAction,
                primaryText, secondaryText, backgroundColor,
                Modifier.align(Alignment.TopStart).padding(start = 4.dp, end = topActionsWidth, top = 4.dp)
                    .onSizeChanged { noticeSize = it }, simulationLabel, onSimulation)
        }
    }
}
