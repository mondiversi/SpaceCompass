package me.mondiversi.spacecompass

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.view.TextureView
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

internal fun hasSpaceCompassCameraPermission(context: Context) =
    ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED

@Composable
internal fun SpaceCompassCameraPreview(frame: SpaceCompassSunSceneFrame, modifier: Modifier,
    onPerspective: (SpaceCompassPerspective?) -> Unit, onCaptureReady: (SpaceCompassCameraCapture?) -> Unit,
    attitudeAt: (Long, Int) -> SpaceCompassCameraAttitude?, onError: () -> Unit,
    zoom: Float, onZoomRange: (SpaceCompassCameraZoomRange?) -> Unit, onZoomActual: (Float) -> Unit) {
    val perspective by rememberUpdatedState(onPerspective)
    val error by rememberUpdatedState(onError)
    val captureReady by rememberUpdatedState(onCaptureReady)
    val attitude by rememberUpdatedState(attitudeAt)
    val zoomRange by rememberUpdatedState(onZoomRange)
    val zoomActual by rememberUpdatedState(onZoomActual)
    val context = androidx.compose.ui.platform.LocalContext.current
    var devices by remember { mutableStateOf<List<SpaceCompassCameraZoomDevice>?>(null) }
    LaunchedEffect(context) {
        try {
            val available = withContext(Dispatchers.IO) { spaceCompassCameraZoomDevices(context) }
            devices = available
            zoomRange(SpaceCompassCameraZoomRange(available.minOf { it.option.lower }, available.maxOf { it.option.upper }))
        } catch (cancelled: kotlinx.coroutines.CancellationException) { throw cancelled
        } catch (_: Exception) { error() }
    }
    DisposableEffect(Unit) { onDispose { zoomRange(null) } }
    devices?.let { available ->
        val options = remember(available) { available.map { it.option } }
        val selected = remember(options, zoom) { spaceCompassCameraZoomOption(options, zoom) }
        val device = remember(available, selected.id) { available.first { it.option.id == selected.id } }
        key(selected.id) {
            var controller by remember { mutableStateOf<SpaceCompassCameraPreviewController?>(null) }
            AndroidView(modifier = modifier.testTag("camera-preview"), factory = { owner ->
                TextureView(owner).apply {
                    isOpaque = true
                    importantForAccessibility = android.view.View.IMPORTANT_FOR_ACCESSIBILITY_NO
                    controller = SpaceCompassCameraPreviewController(this, device, zoom,
                        { perspective(it) }, { captureReady(it) }, { zoomActual(it) },
                        { time, rotation -> attitude(time, rotation) }, { error() })
                }
            }, update = { controller?.updateFrame(frame); controller?.updateZoom(zoom) },
                onRelease = { controller?.close(); controller = null })
        }
    }
}
