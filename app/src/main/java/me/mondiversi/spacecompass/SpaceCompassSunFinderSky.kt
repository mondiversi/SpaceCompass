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
    weather: SpaceCompassSunWeatherSnapshot?, solarLighting: SpaceCompassSolarLighting? = null): Pair<Color, Color> {
    val palette = spaceCompassSunSkyPalette(phase, solarLighting)
    val cover = spaceCompassSunDisplayCloudCover(weather)
    val storm = weather?.kind == SpaceCompassSunWeatherKind.STORM
    val cloudy = cover * if (storm) 0.88f else 0.65f
    val atmosphere = spaceCompassWeatherPalette(phase, storm, solarLighting)
    return lerp(Color(palette.topArgb), Color(atmosphere.overcastTopArgb), cloudy) to
        lerp(Color(palette.horizonArgb), Color(atmosphere.overcastBottomArgb), cloudy)
}

/** Full-screen backdrop, separate from the pointing viewport so panels never get an opaque sky margin. */
@Composable
internal fun SpaceCompassSunSkyBackdrop(phase: SpaceCompassSunSkyPhase, weather: SpaceCompassSunWeatherSnapshot?, modifier: Modifier, solarLighting: SpaceCompassSolarLighting? = null, stars: @Composable () -> Unit = {}) {
    val palette = spaceCompassSunSkyColors(phase, weather, solarLighting)
    val currentPhase = rememberUpdatedState(phase)
    val currentLighting = rememberUpdatedState(solarLighting)
    val transition = if (LocalSpaceCompassObserver.current?.timeSettings?.accelerated == true) 100 else 2_000
    val top by animateColorAsState(palette.first, tween(transition), label = "solar sky top")
    val bottom by animateColorAsState(palette.second, tween(transition), label = "solar sky horizon")
    Box(modifier.testTag("sun-finder-backdrop")) {
        Canvas(Modifier.fillMaxSize()) { drawRect(Brush.verticalGradient(listOf(top, bottom))) }
        stars()
        Box(Modifier.fillMaxSize().drawWithCache {
            val weatherDrawing = if (weather != null && size.width > 0 && size.height > 0)
                SpaceCompassWeatherRenderer(size.width, size.height, SpaceCompassSunSkyPhase.NIGHT, weather) else null
            onDrawBehind { drawIntoCanvas {
                weatherDrawing?.updateLighting(currentPhase.value, currentLighting.value)
                weatherDrawing?.draw(it.nativeCanvas)
            } }
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
    onVisualize: (() -> Unit)? = null,
    compassWarning: String? = null, showActions: Boolean = true,
    offscreenBody: SpaceCompassCelestialBody? = body,
    overlays: Map<SpaceCompassCelestialBody, SpaceCompassCelestialOverlay> = emptyMap(),
    compassAccurate: Boolean = true,
    statusMessage: String? = null, statusActionLabel: String? = null, onStatusAction: () -> Unit = {},
    compassWarningAttention: Boolean = false, statusAttention: Boolean = false,
    simulationLabel: String? = null, onSimulation: () -> Unit = {},
    perspective: SpaceCompassPerspective? = null, topActionsWidth: androidx.compose.ui.unit.Dp = 62.dp,
    observerLatitude: Double? = null, showSkyReferences: Boolean = SPACE_COMPASS_SKY_REFERENCES_DEFAULT,
    observerAltitude: Double = 0.0,
    bottomStartActions: @Composable (Modifier) -> Unit = {},
    bottomActions: @Composable (Modifier) -> Unit = {},
    noticesBelowReticle: Boolean = false,
    topStartActions: @Composable (Modifier) -> Unit = {},
    topEndActions: @Composable (Modifier) -> Unit = {},
    topCenterActions: (@Composable (Modifier) -> Unit)? = null,
    topContentInset: androidx.compose.ui.unit.Dp = 0.dp,
    projectionTopInset: androidx.compose.ui.unit.Dp = 0.dp,
    drawScene: Boolean = true, objectOpacity: Float = 1f,
    approachOverlays: Map<SpaceCompassCelestialBody,SpaceCompassCelestialOverlay> = overlays,
    showReticle: Boolean = true,
    topControlSpacing: androidx.compose.ui.unit.Dp = 8.dp,
    opticalFrame: SpaceCompassSunSceneFrame? = null
) {
    BoxWithConstraints(modifier.testTag("sun-finder-sky").semantics { contentDescription = description }) {
        val density = LocalDensity.current
        val rtl = LocalLayoutDirection.current == LayoutDirection.Rtl
        val width = with(density) { maxWidth.toPx().toDouble() }
        val height = with(density) { maxHeight.toPx().toDouble() }
        val scenePerspective = remember(width, height, projectionTopInset, perspective, opticalFrame) {
            if (opticalFrame != null) spaceCompassSceneFramePerspective(width, height, opticalFrame, perspective)
            else spaceCompassSceneDrawingPerspective(width, height,
                with(density) { projectionTopInset.toPx().toDouble() }, perspective)
        }
        val reticleCenter = SpaceCompassSunScenePoint(width * (scenePerspective?.principalX ?: .5),
            height * (scenePerspective?.principalY ?: .5))
        var menuSize by remember { mutableStateOf(IntSize.Zero) }
        var noticeSize by remember { mutableStateOf(IntSize.Zero) }
        var bottomActionsSize by remember { mutableStateOf(IntSize.Zero) }
        var bottomStartActionsSize by remember { mutableStateOf(IntSize.Zero) }
        var topStartActionsSize by remember { mutableStateOf(IntSize.Zero) }
        var topEndActionsSize by remember { mutableStateOf(IntSize.Zero) }
        var topCenterActionsSize by remember { mutableStateOf(IntSize.Zero) }
        val centerActionsSize = if (topCenterActions == null) IntSize.Zero else topCenterActionsSize
        val centerActionsHeight = if (centerActionsSize.height > 0)
            with(density) { centerActionsSize.height.toDp() } else spaceCompassFloatingControlBaseSize
        val centerActionsTop = topContentInset + 4.dp + (spaceCompassFloatingControlSize - centerActionsHeight) / 2
        val centerGap = topControlSpacing - 4.dp
        val centerActionsLeft = (if (rtl) topEndActionsSize.width else topStartActionsSize.width) +
            (width - topStartActionsSize.width - topEndActionsSize.width - centerActionsSize.width) / 2
        val paths = overlays.mapNotNull { (candidate, overlay) -> overlay.path?.let { candidate to it } }.toMap() +
            (dailyPath?.let { mapOf(body to it) } ?: emptyMap())
        val positions = overlays.mapNotNull { (candidate, overlay) -> overlay.observation?.position?.let { candidate to it } }.toMap() +
            (sun?.let { mapOf(body to it) } ?: emptyMap())
        val projections = remember(positions, orientation, maxWidth, maxHeight, scenePerspective) {
            if (orientation == null) emptyMap() else positions.mapValues { (_, position) ->
                val projected = projectSpaceCompassSun(position, orientation, maxWidth.value.toDouble(), maxHeight.value.toDouble(), scenePerspective)
                val radius = SPACE_COMPASS_CELESTIAL_LIVE_PREVIEW_SIZE_DP / 2
                // A partly clipped miniature becomes a full directional locator until it fits inside the sky.
                projected.copy(visible = projected.visible && projected.x >= radius && projected.y >= radius &&
                    projected.x <= maxWidth.value - radius && projected.y <= maxHeight.value - radius)
            }
        }
        val current = timeMs?.let { moment -> positions.mapValues { SpaceCompassSunPathPoint(moment, it.value) } }.orEmpty()
        val moonPhase = rememberSpaceCompassMoonMarkerPhase(timeMs.takeIf { SpaceCompassCelestialBody.MOON in positions })
        val targets = remember(paths, current, orientation, compassReliable, width, height, scenePerspective) {
            projectSpaceCompassCelestialSceneTargets(paths, current, orientation.takeIf { compassReliable }, width, height, scenePerspective)
        }
        val focused = if (showReticle) focusedSpaceCompassCelestialSceneTarget(targets, width, height,
            with(density) { SPACE_COMPASS_CELESTIAL_RETICLE_RADIUS_DP.dp.toPx().toDouble() }, scenePerspective) else null
        val selectedPoints = dailyPathUiState.selectedPoints
        val pinned = remember(selectedPoints, paths, current, orientation, compassReliable, width, height, scenePerspective) {
            projectSpaceCompassSelectedOrbitTargets(selectedPoints, paths, current,
                orientation.takeIf { compassReliable }, width, height, scenePerspective)
        }
        val padding = with(density) { 4.dp.toPx().toDouble() }
        // The reticle's outer arms end at 32 dp; keep an eight-dp gap before notices.
        val noticeTopInset = with(density) { reticleCenter.y.toFloat().toDp() } + 40.dp
        val titleTouchExclusions = buildList {
            val headerHeight = with(density) { projectionTopInset.toPx().toDouble() }.coerceIn(0.0, height)
            if (headerHeight > 0) add(SpaceCompassSunSceneFrame(0.0, 0.0, width, headerHeight))
            if (centerActionsSize.width > 0 && centerActionsSize.height > 0) add(SpaceCompassSunSceneFrame(
                centerActionsLeft, with(density) { centerActionsTop.coerceAtLeast(0.dp).toPx().toDouble() },
                centerActionsSize.width.toDouble(), centerActionsSize.height.toDouble()))
        }
        val exclusions = buildList {
            addAll(timeBadgeExclusions)
            // Paths can pass beneath the header, while captions and edge locators avoid it.
            val topInsetPx = with(density) { topContentInset.toPx().toDouble() }.coerceIn(0.0, height)
            if (topInsetPx > 0) add(SpaceCompassSunSceneFrame(0.0, 0.0, width, topInsetPx))
            if (noticeSize.width > 0) {
                val noticeLeft = if (noticesBelowReticle) (width - noticeSize.width) / 2
                    else if (rtl) width - noticeSize.width - padding else padding
                val noticeTop = with(density) {
                    (if (noticesBelowReticle) noticeTopInset else topContentInset + 4.dp).roundToPx().toDouble()
                }
                add(SpaceCompassSunSceneFrame(noticeLeft, noticeTop,
                    noticeSize.width.toDouble(), noticeSize.height.toDouble()))
            }
            if (topStartActionsSize.width > 0) add(SpaceCompassSunSceneFrame(
                if (rtl) width - topStartActionsSize.width else 0.0, 0.0,
                topStartActionsSize.width.toDouble(), topStartActionsSize.height.toDouble()))
            if (topEndActionsSize.width > 0) add(SpaceCompassSunSceneFrame(
                if (rtl) 0.0 else width - topEndActionsSize.width, 0.0,
                topEndActionsSize.width.toDouble(), topEndActionsSize.height.toDouble()))
            if (centerActionsSize.width > 0 && centerActionsSize.height > 0) add(SpaceCompassSunSceneFrame(
                centerActionsLeft, with(density) { centerActionsTop.coerceAtLeast(0.dp).toPx().toDouble() },
                centerActionsSize.width.toDouble(), centerActionsSize.height.toDouble()))
            if (menuSize.width > 0) add(SpaceCompassSunSceneFrame(
                if (rtl) 0.0 else width - menuSize.width - padding, 0.0,
                menuSize.width + padding, menuSize.height + padding))
            // Keep pinned captions clear of the measured shutter and leading controls.
            if (bottomActionsSize.width > 0 && bottomActionsSize.height > 0)
                add(SpaceCompassSunSceneFrame(if (rtl) 0.0 else width - bottomActionsSize.width,
                    height - bottomActionsSize.height,
                    bottomActionsSize.width.toDouble(), bottomActionsSize.height.toDouble()))
            if (bottomStartActionsSize.width > 0 && bottomStartActionsSize.height > 0)
                add(SpaceCompassSunSceneFrame(if (rtl) width - bottomStartActionsSize.width else 0.0,
                    height - bottomStartActionsSize.height,
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
        // Guide names stay continuous across moving objects, orbit dots and transient captions.
        // Keep the measured fixed controls/header as exclusions.
        val guideExclusions = exclusions.filterNot { it in timeBadgeExclusions }
        SpaceCompassSkyReferenceLayer(observerLatitude, timeMs, orientation.takeIf { compassReliable }, scenePerspective,
            guideExclusions, enabled = showSkyReferences && drawScene, observerAltitude = observerAltitude) {
            if (drawScene) paths.forEach { (candidate, path) -> key(candidate) {
                SpaceCompassSunDailyPathLayer(path, orientation.takeIf { compassReliable }, dailyPathUiState,
                    primaryText, secondaryText, backgroundColor, showActions = false,
                    interactive = false, pathTint = spaceCompassCelestialPathTint(candidate),
                    highlightedPoint = focused?.takeIf { it.body == candidate && !it.isCurrent }?.point, perspective = scenePerspective,
                    drawApproaches = false)
            } }
            if (showReticle) Canvas(Modifier.fillMaxSize().testTag("sun-finder-reticle")) {
                val aim = Offset(reticleCenter.x.toFloat(), reticleCenter.y.toFloat())
                val reticle = Color.White
                drawCircle(Color.Black.copy(alpha = 0.30f), SPACE_COMPASS_CELESTIAL_RETICLE_RADIUS_DP.dp.toPx(), aim, style = Stroke(1.92.dp.toPx()))
                drawCircle(reticle, SPACE_COMPASS_CELESTIAL_RETICLE_RADIUS_DP.dp.toPx(), aim, style = Stroke(0.96.dp.toPx()))
                val inner = 24.dp.toPx(); val outer = 32.dp.toPx()
                listOf(Offset(1f, 0f), Offset(-1f, 0f), Offset(0f, 1f), Offset(0f, -1f)).forEach {
                    drawLine(Color.Black.copy(alpha = 0.30f), aim + it * inner, aim + it * outer, 1.92.dp.toPx(), StrokeCap.Round)
                    drawLine(reticle, aim + it * inner, aim + it * outer, 0.96.dp.toPx(), StrokeCap.Round)
                }
            }
            SpaceCompassCelestialSceneInteraction(targets, current, focused, dailyPathUiState, timeMs, orientation.takeIf { compassReliable },
                // Captions paint above celestial locators; moving them must not flip a caption's side.
                primaryText, secondaryText, backgroundColor, exclusions,
                scenePerspective, paths, pinnedTargets = pinned, hitExclusions = titleTouchExclusions,
                showCaptions = showReticle) {
                // Only thumbnails have small local targets; they never intercept another path's whole viewport.
                spaceCompassAllCelestialOrder.forEach { candidate -> key(candidate) { projections[candidate]?.let { projected ->
                    val below = positions.getValue(candidate).elevationDegrees < 0
                    if (projected.visible) {
                        if (candidate.constellationPattern == null) SpaceCompassCelestialLiveMarker(candidate, projected, below, current[candidate], dailyPathUiState,
                        moonPhase = moonPhase, path = paths[candidate],
                        interactive = compassReliable && !spaceCompassScenePointIsCovered(SpaceCompassSunScenePoint(
                            pixelProjections.getValue(candidate).x, pixelProjections.getValue(candidate).y), titleTouchExclusions),
                        focused = focused?.let { it.isCurrent && it.body == candidate } == true, objectOpacity = objectOpacity)
                    }
                    else offscreen[candidate]?.let { placement ->
                        SpaceCompassCelestialOffscreenMarker(candidate, projected, below,
                            spaceCompassCelestialPathLineTint(spaceCompassCelestialPathTint(candidate), below),
                            menuItems = 0, forcedPlacement = placement, diameter = (markerDiameter / density.density).toFloat().dp,
                            moonPhase = moonPhase, objectOpacity = objectOpacity)
                    }
                } } }
                if (drawScene) SpaceCompassApproachPathLayer(paths, orientation.takeIf { compassReliable },
                    scenePerspective, dailyPathUiState, focused)
                SpaceCompassCurrentApproachLayer(approachOverlays, orientation.takeIf { compassReliable }, scenePerspective, width, height)
            }
            if (showActions) SpaceCompassCelestialSkyActions(body, onVisualize,
                dailyPath?.let { { dailyPathUiState.openMenu(it, current[body]?.let { point -> spaceCompassCurrentPathPoint(body, point.timeMs, point.position) }) } },
                stringResource(spaceCompassCelestialPathTitle(body)),
                pathSelected = dailyPath != null,
                modifier = Modifier.align(Alignment.TopEnd).padding(4.dp).onSizeChanged { menuSize = it })
            topStartActions(Modifier.align(Alignment.TopStart).onSizeChanged { topStartActionsSize = it }
                .padding(top = topContentInset))
            topEndActions(Modifier.align(Alignment.TopEnd).onSizeChanged { topEndActionsSize = it }
                .padding(top = topContentInset))
            if (topCenterActions != null) {
                // Keep floating app navigation between the measured corner clusters, also in RTL.
                Box(Modifier.align(Alignment.TopCenter).fillMaxWidth()
                    .padding(start = with(density) { topStartActionsSize.width.toDp() } + centerGap,
                        end = with(density) { topEndActionsSize.width.toDp() } + centerGap,
                        top = centerActionsTop.coerceAtLeast(0.dp)), contentAlignment = Alignment.TopCenter) {
                    topCenterActions(Modifier.widthIn(max = 360.dp)
                        .onSizeChanged { topCenterActionsSize = it })
                }
            }
            // Keep the optical viewport fixed: controls stack over the scene instead of resizing it.
            Column(Modifier.align(Alignment.BottomCenter).fillMaxWidth(), horizontalAlignment = Alignment.End) {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.align(Alignment.Bottom), contentAlignment = Alignment.CenterStart) {
                        bottomStartActions(Modifier.onSizeChanged { bottomStartActionsSize = it })
                    }
                    Spacer(Modifier.weight(1f))
                    Box(Modifier.align(Alignment.Bottom)) {
                        bottomActions(Modifier.onSizeChanged { bottomActionsSize = it })
                    }
                }
            }
            SpaceCompassSunStatusNotices(compassWarning, statusMessage, statusActionLabel, onStatusAction,
                backgroundColor,
                (if (noticesBelowReticle) Modifier.align(Alignment.TopCenter).offset(y = noticeTopInset)
                    .padding(horizontal = 4.dp)
                else Modifier.align(Alignment.TopStart)
                    .padding(start = 4.dp, end = topActionsWidth, top = topContentInset + 4.dp))
                    .onSizeChanged { noticeSize = it }, simulationLabel, onSimulation,
                compassWarningAttention, statusAttention, primaryText)
        }
    }
}
