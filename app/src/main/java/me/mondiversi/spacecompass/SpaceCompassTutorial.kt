package me.mondiversi.spacecompass

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.geometry.*
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.*
import androidx.compose.ui.unit.*
import kotlin.math.roundToInt

private const val welcomeSeenKey = "tutorial_welcome_seen"

internal enum class SpaceCompassTutorialFeature(val title: Int, val description: Int) {
    INFO(R.string.pc_info, R.string.tutorial_info),
    MENU(R.string.pc_settings, R.string.tutorial_menu),
    OBJECTS(R.string.celestial_select, R.string.tutorial_objects),
    LOCATION(R.string.observer_management_title, R.string.tutorial_location),
    GUIDES(R.string.sky_references_show, R.string.tutorial_guides),
    FORWARD(R.string.path_interval_title, R.string.tutorial_forward),
    CAMERA(R.string.camera_enable, R.string.tutorial_camera),
    WEATHER(R.string.sky_weather_enable, R.string.tutorial_weather),
    AXIS(R.string.pointing_axis_camera, R.string.tutorial_axis),
    TRANSPARENCY(R.string.camera_object_transparency, R.string.tutorial_transparency),
    ZOOM(R.string.tutorial_zoom_title, R.string.tutorial_zoom),
    CAPTURE(R.string.panorama_capture, R.string.tutorial_capture),
    PANELS(R.string.main_details_hide, R.string.tutorial_panels),
    ANTIPODES(R.string.scene_selected_points, R.string.tutorial_antipodes),
    NAVIGATION(R.string.tutorial_navigation_title, R.string.tutorial_navigation),
    OBSERVATORY(R.string.celestial_details_title, R.string.tutorial_observatory),
    REMOVE(R.string.celestial_remove_title, R.string.tutorial_remove),
    DAILY(R.string.celestial_daily_path, R.string.tutorial_daily),
    GPS(R.string.celestial_details_compact, R.string.tutorial_gps),
    POINT(R.string.tutorial_points_title, R.string.tutorial_points),
    RETICLE(R.string.tutorial_reticle_title, R.string.tutorial_reticle),
    REMINDER(R.string.point_reminder_alarm, R.string.tutorial_reminder),
    STATUS(R.string.tutorial_status_title, R.string.tutorial_status)
}

@Stable
internal class SpaceCompassTutorialState(activeState: MutableState<Boolean>, welcomeState: MutableState<Boolean>,
    cameraPreviewState: MutableState<Boolean>) {
    var active by activeState
    var welcome by welcomeState
    var cameraPreview by cameraPreviewState
        private set
    var skyViewport by mutableStateOf<Rect?>(null)
    val targets = mutableStateMapOf<Any, SpaceCompassTutorialTarget>()
    fun start() { targets.clear(); skyViewport = null; cameraPreview = false; active = true }
    fun close() { active = false; cameraPreview = false; targets.clear(); skyViewport = null }
    fun explore(feature: SpaceCompassTutorialFeature) {
        if (!active) return
        when (feature) {
            SpaceCompassTutorialFeature.MENU -> close()
            SpaceCompassTutorialFeature.CAMERA -> cameraPreview = !cameraPreview
            else -> Unit
        }
    }
}
internal val LocalSpaceCompassTutorial = staticCompositionLocalOf<SpaceCompassTutorialState?> { null }

/** No layout work, registration or scene overhead outside an active tutorial. */
internal fun Modifier.spaceCompassTutorialTarget(feature: SpaceCompassTutorialFeature, enabled: Boolean = true,
    circular: Boolean = true, title: Int = feature.title): Modifier = composed {
    val tutorial = LocalSpaceCompassTutorial.current
    if (tutorial?.active != true || !enabled) return@composed this
    val id = remember { Any() }
    DisposableEffect(tutorial, id) { onDispose { tutorial.targets.remove(id) } }
    onGloballyPositioned { coordinates ->
        val bounds = coordinates.boundsInRoot()
        if (bounds.width > 0 && bounds.height > 0)
            tutorial.targets[id] = SpaceCompassTutorialTarget(feature, bounds, circular, title = title)
    }
}

@Composable
internal fun rememberSpaceCompassTutorialState(): SpaceCompassTutorialState {
    val preferences = LocalSpaceCompassPreferences.current
    val activeState = rememberSaveable { mutableStateOf(false) }
    // Read the durable first-display marker on a new composition, never restore an old popup.
    val welcomeState = remember(preferences) { mutableStateOf(preferences?.getBoolean(welcomeSeenKey, false) != true) }
    val cameraPreviewState = rememberSaveable { mutableStateOf(false) }
    return remember { SpaceCompassTutorialState(activeState, welcomeState, cameraPreviewState) }
}

@Composable
internal fun SpaceCompassTutorialHost(tutorial: SpaceCompassTutorialState, content: @Composable (Boolean) -> Unit) {
    val preferences = LocalSpaceCompassPreferences.current
    LaunchedEffect(tutorial.welcome, preferences) {
        // Record the first display even if the app is closed before either action is pressed.
        if (tutorial.welcome) preferences?.edit()?.putBoolean(welcomeSeenKey, true)?.apply()
    }
    fun dismissWelcome() {
        tutorial.welcome = false
        preferences?.edit()?.putBoolean(welcomeSeenKey, true)?.apply()
    }
    CompositionLocalProvider(LocalSpaceCompassTutorial provides tutorial) {
        Box(Modifier.fillMaxSize()) {
            Box(if (tutorial.active) Modifier.fillMaxSize().clearAndSetSemantics {} else Modifier.fillMaxSize()) {
                content(tutorial.welcome || tutorial.active)
            }
            if (tutorial.active) SpaceCompassTutorialOverlay(tutorial)
        }
        if (tutorial.welcome) SpaceCompassAlertDialog(
            onDismissRequest = { dismissWelcome() },
            title = { Text(stringResource(R.string.tutorial_welcome_title)) },
            text = { Text(stringResource(R.string.tutorial_welcome_body)) },
            dismissButton = { TextButton(onClick = { dismissWelcome() }, Modifier.testTag("welcome-close")) {
                Text(stringResource(R.string.close)) } },
            confirmButton = { TextButton(onClick = { dismissWelcome(); tutorial.start() },
                Modifier.testTag("welcome-tutorial")) { Text(stringResource(R.string.tutorial_title).uppercase(LocalConfiguration.current.locales[0])) } },
            modifier = Modifier.testTag("tutorial-welcome"))
    }
}

/** A topmost input layer explains controls without executing any underlying action. */
@Composable
private fun SpaceCompassTutorialOverlay(state: SpaceCompassTutorialState) {
    BackHandler { state.close() }
    val density = LocalDensity.current
    var origin by remember { mutableStateOf(Offset.Zero) }
    var selected by remember { mutableStateOf<SpaceCompassTutorialFeature?>(null) }
    var cardBounds by remember { mutableStateOf(Rect.Zero) }
    val cardScroll = rememberScrollState()
    LaunchedEffect(selected, state.cameraPreview) { cardScroll.scrollTo(0) }
    val targets = state.targets.values.map { it.translated(-origin) }
    val currentTargets by rememberUpdatedState(targets)
    val pointRadius = with(density) { 24.dp.toPx() }
    // A visible antipode marker shares the feature, but the button owns its current title.
    fun titleResource(feature: SpaceCompassTutorialFeature): Int =
        targets.firstOrNull { it.feature == feature && !it.point }?.title ?: feature.title
    val title = stringResource(selected?.let(::titleResource) ?: R.string.tutorial_title)
    val description = stringResource(selected?.description ?: R.string.tutorial_instruction)
    val close = stringResource(R.string.close)
    fun explain(feature: SpaceCompassTutorialFeature) {
        state.explore(feature)
        selected = feature
    }
    val accessibleFeatures = targets.map { it.feature }.distinct()
        .filterNot { it == SpaceCompassTutorialFeature.MENU }.map { feature ->
        val label = stringResource(titleResource(feature))
        CustomAccessibilityAction(label) {
            explain(feature); true
        }
    }
    BoxWithConstraints(Modifier.fillMaxSize().testTag("main-tutorial")
        .onGloballyPositioned { origin = it.boundsInRoot().topLeft }
        .semantics {
            paneTitle = title
            customActions = accessibleFeatures + CustomAccessibilityAction(close) { state.close(); true }
            dismiss { state.close(); true }
        }
        .pointerInput(state) {
            // Observe taps before children consume them, including the draggable scrollbar.
            // The topmost layer shields real controls; card drags retain normal scrolling.
            awaitEachGesture {
                val down = awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Initial)
                val insideCard = cardBounds.contains(down.position)
                if (!insideCard) down.consume()
                var position = down.position
                var isTap = true
                var released = false
                while (!released) {
                    val event = awaitPointerEvent(PointerEventPass.Initial)
                    if (event.changes.size > 1) isTap = false
                    val change = event.changes.firstOrNull { it.id == down.id } ?: break
                    position = change.position
                    if ((position - down.position).getDistance() > viewConfiguration.touchSlop) isTap = false
                    released = !change.pressed
                    if (!insideCard) event.changes.forEach { it.consume() }
                }
                if (released && isTap && !cardBounds.contains(position)) {
                    val hit = spaceCompassTutorialHit(currentTargets, position, pointRadius)
                    explain(hit?.feature ?: SpaceCompassTutorialFeature.RETICLE)
                }
            }
        }) {
        Canvas(Modifier.fillMaxSize().graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }) {
            drawRect(Color.Black.copy(alpha = .84f))
            targets.forEach { target ->
                val r = target.highlightBounds.inflate(2.dp.toPx())
                if (target.circular) drawOval(Color.Transparent, r.topLeft, r.size, blendMode = BlendMode.Clear)
                else drawRoundRect(Color.Transparent, r.topLeft, r.size, CornerRadius(8.dp.toPx()), blendMode = BlendMode.Clear)
            }
        }
        val accent = MaterialTheme.colorScheme.primary
        Canvas(Modifier.fillMaxSize()) {
            targets.filter { it.feature == selected && !it.point }.forEach { target ->
                val r = target.highlightBounds.inflate(3.dp.toPx())
                if (target.circular) drawOval(accent, r.topLeft, r.size, style = Stroke(2.dp.toPx()))
                else drawRoundRect(accent, r.topLeft, r.size, CornerRadius(8.dp.toPx()), style = Stroke(2.dp.toPx()))
            }
        }
        val viewport = with(density) { Size(maxWidth.toPx(), maxHeight.toPx()) }
        var fixedArea by remember(viewport, density.density, density.fontScale) { mutableStateOf<Rect?>(null) }
        var fixedHeight by remember(viewport, density.density, density.fontScale) { mutableFloatStateOf(0f) }
        LaunchedEffect(viewport, density.density, density.fontScale) {
            cardBounds = Rect.Zero
            // Freeze the gap between measured control groups. Explanations never move the card.
            withFrameNanos { }
            val measured = awaitSpaceCompassTutorialMeasurements(snapshotFlow {
                spaceCompassTutorialMeasurements(state.skyViewport, state.targets.values.toList())
            })
            val skyBounds = measured?.sky ?: state.skyViewport
            val registeredTargets = measured?.targets ?: state.targets.values.toList()
            val root = Rect(Offset.Zero, viewport)
            val sky = skyBounds?.translate(-origin)?.intersect(root)
                ?.takeIf { it.width > 0 && it.height > 0 } ?: root
            val controls = registeredTargets.map { it.translated(-origin) }
            val area = spaceCompassTutorialCardBand(sky, controls)
            val width = minOf(with(density) { 360.dp.toPx() },
                (area.width - with(density) { 16.dp.toPx() }).coerceAtLeast(1f))
            val maximumHeight = with(density) {
                spaceCompassTutorialCardHeightLimit(sky.height.toDp().value).dp.toPx()
            }
            val height = spaceCompassTutorialCardClearHeight(area, width,
                controls.filter { it.circular && !it.point && it.feature !in setOf(
                    SpaceCompassTutorialFeature.REMINDER, SpaceCompassTutorialFeature.OBSERVATORY,
                    SpaceCompassTutorialFeature.DAILY) }.map { it.bounds },
                with(density) { 8.dp.toPx() }, maximumHeight)
            // Discard incomplete geometry rather than freezing an invisible, zero-height card.
            fixedHeight = if (height > 0) height else minOf(maximumHeight,
                (sky.height - with(density) { 16.dp.toPx() }).coerceAtLeast(0f))
            fixedArea = if (height > 0) area else sky
        }
        val area = fixedArea ?: return@BoxWithConstraints
        val width = minOf(360.dp, (with(density) { area.width.toDp() } - 16.dp).coerceAtLeast(1.dp))
        val maxCardHeight = with(density) { fixedHeight.toDp() }
        val card = with(density) { Size(width.toPx(), maxCardHeight.toPx()) }
        val position = spaceCompassTutorialCardPosition(area, card, with(density) { 8.dp.toPx() })
        Surface(Modifier.absoluteOffset { IntOffset(position.x.roundToInt(), position.y.roundToInt()) }
            .width(width).height(maxCardHeight)
            .onGloballyPositioned { cardBounds = it.boundsInRoot().translate(-origin) }.testTag("tutorial-explanation"),
            color = spaceCompassSettingsCardColor(), contentColor = MaterialTheme.colorScheme.onSurface,
            shape = RoundedCornerShape(16.dp), shadowElevation = 6.dp) {
            SpaceCompassTutorialExplanation(title, description, cardScroll)
        }
    }
}
