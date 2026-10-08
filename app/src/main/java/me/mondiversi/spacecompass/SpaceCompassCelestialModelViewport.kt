package me.mondiversi.spacecompass

import android.opengl.GLSurfaceView
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculateCentroid
import androidx.compose.foundation.gestures.calculateCentroidSize
import androidx.compose.foundation.gestures.calculatePan
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.material3.Text
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import kotlin.math.abs
import kotlin.math.roundToInt

@Composable
internal fun SpaceCompassCelestialModelViewport(body: SpaceCompassCelestialBody, geometry: SpaceCompassCelestialViewGeometry,
    rotating: Boolean, rotation: SpaceCompassCelestialRotation, onRotation: (SpaceCompassCelestialRotation) -> Unit,
    viewport: SpaceCompassCelestialViewportState, onViewport: (SpaceCompassCelestialViewportState) -> Unit,
    description: String, resumeDescription: String, modifier: Modifier) {
    val context = LocalContext.current
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    val currentRotation by rememberUpdatedState(rotation)
    val currentCallback by rememberUpdatedState(onRotation)
    val currentViewport by rememberUpdatedState(viewport)
    val viewportCallback by rememberUpdatedState(onViewport)
    val zoomIn = stringResource(R.string.celestial_view_zoom_in)
    val zoomOut = stringResource(R.string.celestial_view_zoom_out)
    val reset = stringResource(R.string.celestial_view_reset_zoom)
    val zoomDescription = stringResource(R.string.celestial_view_zoom_level,
        formatSpaceCompassNumber((viewport.zoom * 100).roundToInt().toDouble(), 0,
            LocalSpaceCompassNumericFormat.current, grouping = false))
    val gestures = Modifier.pointerInput(body, rotating) {
        awaitEachGesture {
            awaitFirstDown(requireUnconsumed = false)
            var pastSlop = false
            var multiple = false
            var totalPan = Offset.Zero
            var totalZoom = 1f
            // Several touch events can arrive before the next Compose frame. Accumulate locally
            // so none of their zoom/drag increments are lost while the UI catches up.
            var framing = currentViewport
            var orientation = currentRotation
            do {
                val event = awaitPointerEvent()
                val active = event.changes.count { it.pressed }
                val pan = event.calculatePan()
                if (active >= 2) {
                    multiple = true
                    val factor = event.calculateZoom()
                    totalPan += pan
                    totalZoom *= factor
                    if (!pastSlop) pastSlop = totalPan.getDistance() > viewConfiguration.touchSlop ||
                        abs(1 - totalZoom) * event.calculateCentroidSize(useCurrent = false) > viewConfiguration.touchSlop
                    if (pastSlop) {
                        val center = event.calculateCentroid(useCurrent = false)
                        framing = framing.transform(factor.toDouble(),
                            2.0 * center.x / size.width - 1, 1 - 2.0 * center.y / size.height,
                            2.0 * pan.x / size.width, -2.0 * pan.y / size.height)
                        viewportCallback(framing)
                    }
                } else if (active == 1 && !multiple && (rotating || framing.zoom > 1)) {
                    totalPan += pan
                    if (!pastSlop && totalPan.getDistance() > viewConfiguration.touchSlop) {
                        pastSlop = true
                        if (rotating) {
                            orientation = currentRotation.drag(totalPan.x.toDouble(), totalPan.y.toDouble(), size.width.toDouble())
                            currentCallback(orientation)
                        } else {
                            framing = framing.transform(1.0, deltaX = 2.0 * totalPan.x / size.width, deltaY = -2.0 * totalPan.y / size.height)
                            viewportCallback(framing)
                        }
                    } else if (pastSlop) {
                        if (rotating) {
                            orientation = orientation.drag(pan.x.toDouble(), pan.y.toDouble(), size.width.toDouble())
                            currentCallback(orientation)
                        } else {
                            framing = framing.transform(1.0, deltaX = 2.0 * pan.x / size.width, deltaY = -2.0 * pan.y / size.height)
                            viewportCallback(framing)
                        }
                    }
                }
                if (pastSlop) event.changes.forEach { if (it.position != it.previousPosition) it.consume() }
            } while (event.changes.any { it.pressed })
        }
    }.pointerInput(body, rotating) { detectTapGestures(onTap = {
        if (rotating && !currentRotation.automatic) currentCallback(currentRotation.beginReturn())
    }, onDoubleTap = {
        if (rotating) currentCallback(currentRotation.beginReturn()) else viewportCallback(SpaceCompassCelestialViewportState())
    }) }
    Box(modifier.testTag("celestial-model-viewport").then(gestures).semantics {
        contentDescription = description
        stateDescription = zoomDescription
        customActions = buildList {
            add(CustomAccessibilityAction(zoomIn) { viewportCallback(currentViewport.transform(1.25)); true })
            add(CustomAccessibilityAction(zoomOut) { viewportCallback(currentViewport.transform(0.8)); true })
            add(CustomAccessibilityAction(reset) { viewportCallback(SpaceCompassCelestialViewportState()); true })
            if (rotating) add(CustomAccessibilityAction(resumeDescription) { currentCallback(currentRotation.beginReturn()); true })
        }
    }, contentAlignment = Alignment.Center) {
        if (body.hasCatalogPhotograph) SpaceCompassCatalogPhotograph(requireNotNull(body.viewerTexture),
            Modifier.fillMaxSize().graphicsLayer {
                scaleX = viewport.zoom.toFloat(); scaleY = viewport.zoom.toFloat()
                translationX = (viewport.panX * size.width / 2).toFloat()
                translationY = (-viewport.panY * size.height / 2).toFloat()
            })
        else if (body.isComet) SpaceCompassCometSymbol(body, Modifier.fillMaxSize().graphicsLayer {
            scaleX = viewport.zoom.toFloat(); scaleY = viewport.zoom.toFloat()
            translationX = (viewport.panX * size.width / 2).toFloat()
            translationY = (-viewport.panY * size.height / 2).toFloat()
        })
        else if (body.isSpacecraft) SpaceCompassCelestialCraftCanvas(body, geometry, viewport, Modifier.fillMaxSize())
        else {
            val textureReady = rememberSpaceCompassCelestialTexture(body)
            var failed by remember(body, textureReady) { mutableStateOf(false) }
            key(body, lifecycle, textureReady) {
                AndroidView(factory = { viewContext ->
                    SpaceCompassCelestialGlSurfaceView(viewContext, lifecycle, body) {
                        SpaceCompassErrorLog.record(context, "celestial_viewer:renderer", it)
                        android.os.Handler(android.os.Looper.getMainLooper()).post { failed = true }
                    }
                }, modifier = Modifier.fillMaxSize(), onRelease = { it.release() }, update = {
                    it.renderer.geometry = geometry
                    it.renderer.viewport = viewport
                    it.renderer.inspectShadows = !rotating
                    it.requestRender()
                })
            }
            if (failed) Text(stringResource(R.string.celestial_view_render_failed), color = Color.White)
        }
    }
}

/** The native surface belongs to AndroidView; its GL lifecycle begins only after attachment. */
private class SpaceCompassCelestialGlSurfaceView(context: android.content.Context,
    private val lifecycle: Lifecycle, body: SpaceCompassCelestialBody,
    onFailure: (Throwable) -> Unit) : GLSurfaceView(context) {
    val renderer = SpaceCompassCelestialGlRenderer(context.applicationContext, body, onFailure)
    private val observer = LifecycleEventObserver { _, event ->
        when (event) {
            Lifecycle.Event.ON_RESUME -> { onResume(); requestRender() }
            Lifecycle.Event.ON_PAUSE -> onPause()
            else -> Unit
        }
    }
    init {
        setEGLContextClientVersion(2)
        preserveEGLContextOnPause = true
        setRenderer(renderer)
        renderMode = RENDERMODE_WHEN_DIRTY
    }
    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        lifecycle.addObserver(observer)
        if (lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)) {
            onResume(); requestRender()
        } else onPause()
    }
    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        // Compose can size a newly inserted AndroidView during drawing, after the
        // native pre-draw pass saw a zero-sized SurfaceView. A new root traversal
        // lets SurfaceView create its buffer before a static model's first frame.
        if (w > 0 && h > 0) post {
            if (isAttachedToWindow) rootView.requestLayout()
        }
    }
    override fun onDetachedFromWindow() {
        release()
        super.onDetachedFromWindow()
    }
    fun release() {
        lifecycle.removeObserver(observer)
        onPause()
    }
}
