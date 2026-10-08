package me.mondiversi.spacecompass

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
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
    val atmosphere = spaceCompassWeatherPalette(phase, storm)
    val greyTop = Color(atmosphere.overcastTopArgb)
    val greyBottom = Color(atmosphere.overcastBottomArgb)
    return lerp(palette.first, greyTop, cloudy) to lerp(palette.second, greyBottom, cloudy)
}

/** Full-screen backdrop, separate from the pointing viewport so panels never get an opaque sky margin. */
@Composable
internal fun SpaceCompassSunSkyBackdrop(phase: SpaceCompassSunSkyPhase, weather: SpaceCompassSunWeatherSnapshot?, modifier: Modifier, stars: @Composable () -> Unit = {}) {
    val palette = spaceCompassSunSkyColors(phase, weather)
    val top by animateColorAsState(palette.first, tween(2_000), label = "solar sky top")
    val bottom by animateColorAsState(palette.second, tween(2_000), label = "solar sky horizon")
    Box(modifier.testTag("sun-finder-backdrop")) {
        Canvas(Modifier.fillMaxSize()) { drawRect(Brush.verticalGradient(listOf(top, bottom))) }
        stars()
        Box(Modifier.fillMaxSize().drawWithCache {
            val weatherDrawing = if (size.width > 0 && size.height > 0)
                SpaceCompassWeatherRenderer(size.width, size.height, phase, weather) else null
            onDrawBehind { drawIntoCanvas { weatherDrawing?.draw(it.nativeCanvas) } }
        })
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
    observerAltitude: Double = 0.0, compassCalibrationRequired: Boolean = false,
    bottomStartActions: @Composable (Modifier) -> Unit = {},
    bottomActions: @Composable (Modifier) -> Unit = {},
    noticesAtBottom: Boolean = false,
    topStartActions: @Composable (Modifier) -> Unit = {},
    topEndActions: @Composable (Modifier) -> Unit = {},
    selectedPanelExpanded: Boolean = true
) {
    BoxWithConstraints(modifier.testTag("sun-finder-sky").semantics { contentDescription = description }) {
        val density = LocalDensity.current
        val rtl = LocalLayoutDirection.current == LayoutDirection.Rtl
        val width = with(density) { maxWidth.toPx().toDouble() }
        val height = with(density) { maxHeight.toPx().toDouble() }
        var menuSize by remember { mutableStateOf(IntSize.Zero) }
        var noticeSize by remember { mutableStateOf(IntSize.Zero) }
        var panelSize by remember { mutableStateOf(IntSize.Zero) }
        var bottomActionsSize by remember { mutableStateOf(IntSize.Zero) }
        var bottomStartActionsSize by remember { mutableStateOf(IntSize.Zero) }
        var topStartActionsSize by remember { mutableStateOf(IntSize.Zero) }
        var topEndActionsSize by remember { mutableStateOf(IntSize.Zero) }
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
            val selectedPanelHeight = if (showSelectedPanel && dailyPathUiState.selectedBody != null) panelSize.height else 0
            if (noticeSize.width > 0) {
                val noticeLeft = if (noticesAtBottom) {
                    val leading = if (rtl) bottomActionsSize.width else bottomStartActionsSize.width
                    leading + (width - bottomStartActionsSize.width - bottomActionsSize.width - noticeSize.width) / 2
                } else if (rtl) width - noticeSize.width - padding else padding
                val noticeTop = if (noticesAtBottom) {
                    // Match the centered notice's real position, including its four-dp outer insets.
                    val noticeInsets = with(density) { 4.dp.roundToPx() } * 2
                    val rowHeight = maxOf(bottomStartActionsSize.height, bottomActionsSize.height,
                        noticeSize.height + noticeInsets)
                    height - selectedPanelHeight - rowHeight + (rowHeight - noticeSize.height) / 2.0
                } else padding
                add(SpaceCompassSunSceneFrame(noticeLeft, noticeTop,
                    noticeSize.width.toDouble(), noticeSize.height.toDouble()))
            }
            if (topStartActionsSize.width > 0) add(SpaceCompassSunSceneFrame(
                if (rtl) width - topStartActionsSize.width else 0.0, 0.0,
                topStartActionsSize.width.toDouble(), topStartActionsSize.height.toDouble()))
            if (topEndActionsSize.width > 0) add(SpaceCompassSunSceneFrame(
                if (rtl) 0.0 else width - topEndActionsSize.width, 0.0,
                topEndActionsSize.width.toDouble(), topEndActionsSize.height.toDouble()))
            if (menuSize.width > 0) add(SpaceCompassSunSceneFrame(
                if (rtl) 0.0 else width - menuSize.width - padding, 0.0,
                menuSize.width + padding, menuSize.height + padding))
            if (selectedPanelHeight > 0)
                add(SpaceCompassSunSceneFrame(0.0, height - selectedPanelHeight - padding, width, selectedPanelHeight + padding))
            // Include the measured shutter and leading control above the point panel.
            if (bottomActionsSize.width > 0 && bottomActionsSize.height > 0)
                add(SpaceCompassSunSceneFrame(if (rtl) 0.0 else width - bottomActionsSize.width,
                    height - selectedPanelHeight - bottomActionsSize.height,
                    bottomActionsSize.width.toDouble(), bottomActionsSize.height.toDouble()))
            if (bottomStartActionsSize.width > 0 && bottomStartActionsSize.height > 0)
                add(SpaceCompassSunSceneFrame(if (rtl) width - bottomStartActionsSize.width else 0.0,
                    height - selectedPanelHeight - bottomStartActionsSize.height,
                    bottomStartActionsSize.width.toDouble(), bottomStartActionsSize.height.toDouble()))
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
                dailyPath?.let { { dailyPathUiState.openMenu(it, current[body]?.let { point -> spaceCompassCurrentPathPoint(body, point.timeMs, point.position) }) } },
                stringResource(spaceCompassCelestialPathTitle(body)),
                pathSelected = dailyPath != null,
                modifier = Modifier.align(Alignment.TopEnd).padding(4.dp).onSizeChanged { menuSize = it })
            topStartActions(Modifier.align(Alignment.TopStart).onSizeChanged { topStartActionsSize = it })
            topEndActions(Modifier.align(Alignment.TopEnd).onSizeChanged { topEndActionsSize = it })
            // Keep the optical viewport fixed: controls stack over the scene instead of resizing it.
            Column(Modifier.align(Alignment.BottomCenter).fillMaxWidth(), horizontalAlignment = Alignment.End) {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    // Short notices share the button centers; taller stacks expand only upwards.
                    Box(Modifier.align(Alignment.Bottom), contentAlignment = Alignment.CenterStart) {
                        bottomStartActions(Modifier.onSizeChanged { bottomStartActionsSize = it })
                    }
                    Box(Modifier.weight(1f).padding(horizontal = 4.dp,
                        vertical = if (noticesAtBottom) 4.dp else 0.dp), contentAlignment = Alignment.Center) {
                        if (noticesAtBottom) SpaceCompassSunStatusNotices(compassWarning, statusMessage, statusActionLabel, onStatusAction,
                            primaryText, secondaryText, backgroundColor,
                            Modifier.onSizeChanged { noticeSize = it }, simulationLabel, onSimulation,
                            compassCalibrationRequired = compassCalibrationRequired)
                    }
                    Box(Modifier.align(Alignment.Bottom)) {
                        bottomActions(Modifier.onSizeChanged { bottomActionsSize = it })
                    }
                }
                if (showSelectedPanel) dailyPathUiState.selectedBody?.let { selected ->
                    // Keep selection alive while the island collapses; exclusions track its animated height.
                    SpaceCompassMainDetailsVisibility(selectedPanelExpanded, landscape = false,
                        modifier = Modifier.onSizeChanged { panelSize = it }) {
                        SpaceCompassSunPathSelectedPanel(paths[selected], dailyPathUiState, primaryText, secondaryText, backgroundColor,
                            body = selected, nowMs = timeMs)
                    }
                }
            }
            if (!noticesAtBottom) SpaceCompassSunStatusNotices(compassWarning, statusMessage, statusActionLabel, onStatusAction,
                primaryText, secondaryText, backgroundColor,
                Modifier.align(Alignment.TopStart).padding(start = 4.dp, end = topActionsWidth, top = 4.dp)
                    .onSizeChanged { noticeSize = it }, simulationLabel, onSimulation,
                compassCalibrationRequired = compassCalibrationRequired)
        }
    }
}
