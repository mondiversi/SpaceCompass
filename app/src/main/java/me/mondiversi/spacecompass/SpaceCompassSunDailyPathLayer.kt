package me.mondiversi.spacecompass

import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.hypot

/** Overlay only: never changes the live sun, orientation, location or user archive. */
@Composable
internal fun SpaceCompassSunDailyPathLayer(
    path: SpaceCompassSunDailyPath, orientation: SpaceCompassSunOrientation?, state: SpaceCompassSunDailyPathUiState,
    primaryText: Color, secondaryText: Color, backgroundColor: Color,
    liveMarker: @Composable () -> Unit = {}, timeMs: Long? = null,
    timeBadgeExclusions: List<SpaceCompassSunSceneFrame> = emptyList(),
    showSelectedPanel: Boolean = true,
    onVisualize: (() -> Unit)? = null, currentPoint: SpaceCompassSunPathPoint? = null,
    showActions: Boolean = true, interactive: Boolean = true, pathTint: Color? = null,
    highlightedPoint: SpaceCompassSunPathPoint? = null, perspective: SpaceCompassPerspective? = null
) {
    val selected = state.selectedIn(path)
    val labels = rememberSpaceCompassSunPathLabels(path)
    var viewport by remember { mutableStateOf(IntSize.Zero) }
    var menuSize by remember { mutableStateOf(IntSize.Zero) }
    var selectedPanelSize by remember { mutableStateOf(IntSize.Zero) }
    val density = LocalDensity.current
    val layoutDirection = LocalLayoutDirection.current
    val radius = with(density) { SPACE_COMPASS_SUN_PATH_TOUCH_RADIUS_DP.dp.toPx() }
    val segments = remember(path, orientation, viewport, perspective) {
        if (orientation == null) emptyList() else projectSpaceCompassSunDailyPath(path, orientation,
            viewport.width.toDouble(), viewport.height.toDouble(), perspective)
    }
    val points = remember(path, orientation, viewport, perspective) {
        if (orientation == null || viewport.width == 0 || viewport.height == 0) emptyList() else
            path.markers.mapNotNull { marker ->
                projectSpaceCompassSun(marker.position, orientation, viewport.width.toDouble(), viewport.height.toDouble(), perspective)
                    .takeIf { it.visible }?.let { marker to SpaceCompassSunScenePoint(it.x, it.y) }
            }
    }
    val directions = remember(path) { spaceCompassSunPathDirections(path) }
    val arrows = remember(directions, orientation, viewport, density, perspective) {
        if (orientation == null) emptyList() else directions.mapNotNull {
            projectSpaceCompassSunPathArrow(it, orientation, viewport.width.toDouble(), viewport.height.toDouble(),
                with(density) { 5.dp.toPx().toDouble() }, perspective)
        }
    }
    val projectedCurrent = if (orientation == null || currentPoint == null || viewport.width <= 0 || viewport.height <= 0) null else
        projectSpaceCompassSun(currentPoint.position, orientation, viewport.width.toDouble(), viewport.height.toDouble(), perspective)
            .takeIf { it.visible }?.let { SpaceCompassSunScenePoint(it.x, it.y) }
    val currentPoints by rememberUpdatedState(selectableSpaceCompassCelestialPathPoints(
        points, currentPoint, projectedCurrent))
    val currentLivePoint by rememberUpdatedState(currentPoint)
    val currentPath by rememberUpdatedState(path)
    val focused = if (!interactive) null else focusedSpaceCompassCelestialPoint(points, currentPoint, projectedCurrent,
        viewport.width.toDouble(), viewport.height.toDouble(),
        with(density) { SPACE_COMPASS_CELESTIAL_RETICLE_RADIUS_DP.dp.toPx().toDouble() })
    val title = stringResource(spaceCompassCelestialPathTitle(path.body))
    val above = pathTint ?: spaceCompassCelestialPathTint(path.body)
    // The same subdued treatment preserves each object's identity below the horizon.
    val below = spaceCompassCelestialPathVisibilityTint(above, true)
    val objectName = stringResource(path.body.nameResource)
    val orbitText = remember(density) { spaceCompassOrbitTextPaint(with(density) { 13.sp.toPx() }) }
    Box(Modifier.fillMaxSize().onSizeChanged { viewport = it }) {
        if (orientation != null) Canvas(Modifier.fillMaxSize().testTag("sun-daily-path")
            // Orientation updates must not cancel a finger already touching the trajectory.
            .then(if (!interactive) Modifier else Modifier.pointerInput(radius) {
                detectTapGestures { tap ->
                    tappedSpaceCompassSunPathPoint(currentPoints, SpaceCompassSunScenePoint(tap.x.toDouble(), tap.y.toDouble()),
                        radius.toDouble())?.let {
                            if (it == currentLivePoint) state.selectCurrent(currentPath.body, it)
                            else state.select(currentPath, it)
                        }
                }
            })) {
            for (underground in listOf(false, true)) {
                val curve = Path()
                var end: SpaceCompassSunScenePoint? = null
                segments.filter { it.belowHorizon == underground }.forEach { segment ->
                    if (end == null || hypot(end!!.x - segment.start.x, end!!.y - segment.start.y) > 0.1)
                        curve.moveTo(segment.start.x.toFloat(), segment.start.y.toFloat())
                    curve.lineTo(segment.end.x.toFloat(), segment.end.y.toFloat())
                    end = segment.end
                }
                val dash = if (underground) PathEffect.dashPathEffect(floatArrayOf(6.dp.toPx(), 4.dp.toPx())) else null
                drawPath(curve, Color.Black.copy(alpha = 0.34f), style = Stroke(3.5.dp.toPx(), pathEffect = dash))
                drawPath(curve, spaceCompassCelestialPathLineTint(above, underground),
                    style = Stroke(1.5.dp.toPx(), pathEffect = dash))
            }
            val occupiedLabels = mutableListOf<android.graphics.RectF>()
            // Decorative only: neither add hit targets nor cover hourly/event/live markers.
            arrows.forEach { arrow ->
                if (points.any { (_, point) -> hypot(point.x - arrow.center.x, point.y - arrow.center.y) < 16.dp.toPx() } ||
                    projectedCurrent?.let { hypot(it.x - arrow.center.x, it.y - arrow.center.y) < 26.dp.toPx() } == true)
                    return@forEach
                val chevron = Path().apply {
                    moveTo(arrow.left.x.toFloat(), arrow.left.y.toFloat())
                    lineTo(arrow.tip.x.toFloat(), arrow.tip.y.toFloat())
                    lineTo(arrow.right.x.toFloat(), arrow.right.y.toFloat())
                }
                drawPath(chevron, Color.Black.copy(alpha = 0.45f),
                    style = Stroke(3.5.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round))
                drawPath(chevron, (if (arrow.belowHorizon) below else above).copy(alpha = 0.95f),
                    style = Stroke(1.5.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round))
            }
            arrows.forEach { arrow ->
                drawSpaceCompassOrbitName(drawContext.canvas.nativeCanvas, objectName, segments, arrow, orbitText,
                    (if (arrow.belowHorizon) below else above).toArgb(), 9.dp.toPx(), occupiedLabels, size.width, size.height)
            }
            points.forEach { (marker, projected) ->
                val point = Offset(projected.x.toFloat(), projected.y.toFloat())
                val tint = if (marker.position.elevationDegrees < 0) below else above
                val r = (if (marker.event == SpaceCompassSunPathEvent.HOUR) 4.dp else 6.dp).toPx()
                drawCircle(Color.Black.copy(alpha = 0.45f), r + 1.5.dp.toPx(), point)
                drawCircle(tint, r, point)
                if (marker.event != SpaceCompassSunPathEvent.HOUR) {
                    val d = 2.5.dp.toPx()
                    if (marker.event == SpaceCompassSunPathEvent.MINIMUM) {
                        drawLine(Color.Black.copy(alpha = 0.8f), point - Offset(d, 0f), point + Offset(d, 0f),
                            1.8.dp.toPx(), StrokeCap.Round)
                    } else {
                        val symbol = Path().apply {
                            when (marker.event) {
                                SpaceCompassSunPathEvent.SUNRISE -> { moveTo(point.x, point.y - d)
                                    lineTo(point.x + d, point.y + d); lineTo(point.x - d, point.y + d) }
                                SpaceCompassSunPathEvent.SUNSET -> { moveTo(point.x, point.y + d)
                                    lineTo(point.x + d, point.y - d); lineTo(point.x - d, point.y - d) }
                                else -> { moveTo(point.x, point.y - d); lineTo(point.x + d, point.y)
                                    lineTo(point.x, point.y + d); lineTo(point.x - d, point.y) }
                            }
                            close()
                        }
                        drawPath(symbol, Color.Black.copy(alpha = 0.65f))
                    }
                }
                if (marker == selected || marker == highlightedPoint || (focused?.isCurrent == false && marker == focused.point))
                    drawCircle(Color.White, r + 5.dp.toPx(), point, style = Stroke(1.8.dp.toPx()))
            }
        }
        liveMarker()
        if (focused != null && timeMs != null) {
            val padding = with(density) { 4.dp.toPx().toDouble() }
            val excluded = buildList {
                addAll(timeBadgeExclusions)
                if (menuSize.width > 0) add(SpaceCompassSunSceneFrame(
                    if (layoutDirection == LayoutDirection.Rtl) 0.0 else viewport.width - menuSize.width - padding, 0.0,
                    menuSize.width + padding, menuSize.height + padding))
                if (showSelectedPanel && selected != null && selectedPanelSize.height > 0) add(SpaceCompassSunSceneFrame(
                    0.0, viewport.height - selectedPanelSize.height - padding,
                    viewport.width.toDouble(), selectedPanelSize.height + padding))
            }
            SpaceCompassCelestialTimeBadge(focused.point.timeMs, timeMs, path.zone, focused.point.position, orientation,
                primaryText, secondaryText, backgroundColor,
                if (focused.isCurrent) stringResource(R.string.celestial_point_current) else labels.name(focused.point),
                path.body, excluded, perspective)
        }
        if (showActions) SpaceCompassCelestialSkyActions(path.body, onVisualize, { state.openMenu(path) }, title, true,
            Modifier.align(Alignment.TopEnd).padding(4.dp).onSizeChanged { menuSize = it })
        if (showSelectedPanel) SpaceCompassSunPathSelectedPanel(path, state, primaryText, secondaryText, backgroundColor,
            Modifier.align(Alignment.BottomCenter).onSizeChanged { selectedPanelSize = it }, nowMs = timeMs)
    }
}

@Composable
internal fun SpaceCompassCelestialSkyActions(body: SpaceCompassCelestialBody, onVisualize: (() -> Unit)?,
    onPath: (() -> Unit)? = null, pathTitle: String = "", pathSelected: Boolean = false,
    modifier: Modifier = Modifier) {
    val viewTitle = stringResource(R.string.celestial_view_open)
    Row(modifier, verticalAlignment = Alignment.CenterVertically) {
        if (onVisualize != null) SpaceCompassCelestialIconControl(false, viewTitle, onVisualize,
            Modifier.testTag("celestial-visualize"), Role.Button) {
            SpaceCompassCelestialTelescopeIcon(Modifier.size(22.dp))
        }
        if (onPath != null) SpaceCompassCelestialIconControl(pathSelected, pathTitle, onPath,
            Modifier.testTag("sun-path-open"), Role.Button) {
            Canvas(Modifier.size(18.dp)) {
                val curve = Path().apply { moveTo(0f, size.height * 0.8f)
                    cubicTo(size.width * 0.25f, 0f, size.width * 0.75f, 0f, size.width, size.height * 0.8f) }
                drawPath(curve, Color.White, style = Stroke(1.5.dp.toPx(), cap = StrokeCap.Round))
            }
        }
    }
}
