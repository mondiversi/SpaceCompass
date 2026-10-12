package me.mondiversi.spacecompass

import android.graphics.BitmapFactory
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.layout
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.semantics.Role
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import kotlin.math.min

internal data class SpaceCompassPanoramaPreviewData(val file: File, val timeMs: Long,
    val position: SpaceCompassPanoramaPosition = SpaceCompassPanoramaPosition.COMPLETE,
    val snapshot: SpaceCompassPanoramaSnapshot? = null, val captionData: SpaceCompassPanoramaCaptionData? = null,
    val credits: String? = null, val cameraPhoto: SpaceCompassCameraPhotoSnapshot? = null,
    val selectedPresentation: SpaceCompassPanoramaPresentation? = null,
    val fileExportMode: SpaceCompassPanoramaExportMode = SpaceCompassPanoramaExportMode.INTERNATIONAL,
    val orbitTable: SpaceCompassCapturedOrbitTable? = null)

// Match the main sky's 10 dp content inset plus 4 dp action inset, including pixel rounding.
private fun Modifier.spaceCompassPanoramaControlSideInsets(): Modifier =
    padding(start = 10.dp, end = 10.dp).padding(start = 4.dp, end = 4.dp)

// Reserve one floating-control height above each column without increasing the gaps inside it.
private fun Modifier.spaceCompassPanoramaTopControlInsets(): Modifier =
    spaceCompassPanoramaControlSideInsets().padding(top = spaceCompassFloatingControlSize + 8.dp, bottom = 8.dp)

/** Center side clusters, moving down only when the top corner action would overlap them. */
private fun Modifier.spaceCompassPanoramaCenteredControlInsets(height: Dp, count: Int): Modifier {
    val groupHeight = spaceCompassFloatingControlSize * count + 8.dp * (count - 1).coerceAtLeast(0)
    val naturalTop = (height - groupHeight) / 2f
    val minimumTop = spaceCompassFloatingControlSize * 2 + 16.dp
    val shift = (minimumTop - naturalTop).coerceAtLeast(0.dp)
        .coerceAtMost((height - groupHeight - 8.dp - naturalTop).coerceAtLeast(0.dp))
    return spaceCompassPanoramaControlSideInsets().offset(y = shift)
}

/** Preview follows the device orientation; portrait starts filled and pannable. */
@Composable
internal fun SpaceCompassPanoramaPreview(data: SpaceCompassPanoramaPreviewData, onBack: () -> Unit) {
    val context = LocalContext.current
    val resources = LocalResources.current
    val preferences = LocalSpaceCompassPreferences.current
    val previewMode = if (data.selectedPresentation != null) SpaceCompassPanoramaExportMode.SELECTED else data.fileExportMode
    var currentFile by remember(data.file) { mutableStateOf(data.file) }
    var currentPosition by remember(data.file) { mutableStateOf(data.position) }
    var requestedPosition by remember(data.file) { mutableStateOf(data.position) }
    var currentLabelMode by remember(data.file) { mutableStateOf(data.snapshot?.labelMode ?: SpaceCompassPanoramaLabelMode.ALL) }
    var requestedLabelMode by remember(data.file) { mutableStateOf(data.snapshot?.labelMode ?: SpaceCompassPanoramaLabelMode.ALL) }
    var revision by remember(data.file) { mutableIntStateOf(0) }
    var preparationFailed by remember(data.file) { mutableStateOf(false) }
    var choosePosition by remember { mutableStateOf(false) }
    var chooseLabels by remember { mutableStateOf(false) }
    var chooseCenter by remember { mutableStateOf(false) }
    var currentCenter by remember(data.file) { mutableStateOf(data.snapshot?.center ?: SpaceCompassPanoramaCenter.SOUTH) }
    var requestedCenter by remember(data.file) { mutableStateOf(data.snapshot?.center ?: SpaceCompassPanoramaCenter.SOUTH) }
    LaunchedEffect(data, requestedPosition, requestedLabelMode, requestedCenter, revision) {
        preparationFailed = false
        if (requestedPosition != currentPosition || requestedLabelMode != currentLabelMode || requestedCenter != currentCenter) {
            val targetPosition = requestedPosition
            val targetLabels = requestedLabelMode
            val targetCenter = requestedCenter
            try {
                val file = withContext(Dispatchers.IO) {
                    prepareSpaceCompassPanoramaVariant(context.applicationContext, data, targetPosition, targetLabels, previewMode, targetCenter)
                }
                currentFile = file
                currentPosition = targetPosition
                currentLabelMode = targetLabels
                currentCenter = targetCenter
            } catch (cancelled: kotlinx.coroutines.CancellationException) { throw cancelled
            } catch (error: Exception) {
                preparationFailed = true
                SpaceCompassErrorLog.record(context, "panorama:variant", error)
                showSpaceCompassBottomMessage(context, resources.getString(R.string.panorama_error))
            } catch (error: OutOfMemoryError) {
                preparationFailed = true
                showSpaceCompassBottomMessage(context, resources.getString(R.string.panorama_error))
            }
        }
    }
    var decoded by remember(currentFile) { mutableStateOf<android.graphics.Bitmap?>(null) }
    var failed by remember(currentFile) { mutableStateOf(false) }
    LaunchedEffect(currentFile) {
        // Retain ownership across the IO cancellation boundary until the bitmap reaches the UI.
        var pending: android.graphics.Bitmap? = null
        try {
            withContext(Dispatchers.IO) { pending = BitmapFactory.decodeFile(currentFile.absolutePath) }
            failed = pending == null
            decoded = pending
            pending = null
        } catch (cancelled: kotlinx.coroutines.CancellationException) { throw cancelled
        } catch (error: Exception) {
            failed = true
            SpaceCompassErrorLog.record(context, "panorama:decode", error)
        } catch (_: OutOfMemoryError) { failed = true
        } finally { pending?.recycle() }
    }
    DisposableEffect(currentFile) { onDispose { decoded?.recycle(); decoded = null } }
    fun notify(id: Int, vararg arguments: Any) = showSpaceCompassBottomMessage(context, resources.getString(id, *arguments))
    val background = spaceCompassPageBackground()
    val bitmap = decoded
    val positionReady = requestedPosition == currentPosition && requestedLabelMode == currentLabelMode &&
        requestedCenter == currentCenter && !preparationFailed
    val ready = positionReady && bitmap != null && !failed
    val export = rememberSpaceCompassCaptureExport(data, requestedPosition, requestedLabelMode, requestedCenter, ready)
    BackHandler { if (export.visible) export.close() else if (!export.saving) onBack() }
    val positionState = stringResource(requestedPosition.labelResource)
    val backButton: @Composable () -> Unit = {
        SpaceCompassPanoramaCircleButton(stringResource(R.string.navigate_back), onBack, enabled = !export.saving,
            modifier = Modifier.testTag("panorama-back")) {
            SpaceCompassPanoramaControlIcon(SpaceCompassPanoramaControl.BACK)
        }
    }
    BoxWithConstraints(Modifier.fillMaxSize().background(background).testTag("panorama-preview")) {
        val viewportHeight = maxHeight
        // Retain image zoom and pan while the export page is open, without background touch/accessibility targets.
        Box(Modifier.fillMaxSize().layout { measurable, constraints ->
            val placeable = measurable.measure(constraints)
            layout(placeable.width, placeable.height) {
                if (!export.visible) placeable.placeRelative(0, 0)
            }
        }.testTag("panorama-full-image-viewport")) {
            if (bitmap == null || !positionReady) Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                if (failed || preparationFailed) Text(stringResource(R.string.panorama_error)) else CircularProgressIndicator()
                Column(Modifier.align(Alignment.TopStart).spaceCompassPanoramaTopControlInsets().testTag("panorama-left-controls")) {
                    backButton()
                }
            } else SpaceCompassPanoramaZoomImage(bitmap, Modifier.fillMaxSize(), backButton)
            Column(Modifier.align(Alignment.TopEnd).spaceCompassPanoramaTopControlInsets().testTag("panorama-export-corner")) {
                SpaceCompassPanoramaCircleButton(stringResource(R.string.panorama_export), export.open,
                    enabled = ready && !export.saving, modifier = Modifier.testTag("panorama-export")) {
                    SpaceCompassPanoramaExportIcon()
                }
            }
            val optionCount = (if (data.snapshot != null && data.captionData != null) 2 else 0) +
                (if (data.cameraPhoto == null && data.snapshot != null) 1 else 0)
            Column(Modifier.align(Alignment.CenterEnd).spaceCompassPanoramaCenteredControlInsets(viewportHeight, optionCount)
                .testTag("panorama-right-controls"), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                if (data.snapshot != null && data.captionData != null) SpaceCompassPanoramaCircleButton(
                    stringResource(R.string.panorama_point_labels), { chooseLabels = true }, enabled = !export.saving,
                    stateText = stringResource(requestedLabelMode.labelResource),
                    modifier = Modifier.testTag("panorama-point-labels")) { SpaceCompassPanoramaLabelsIcon(requestedLabelMode == SpaceCompassPanoramaLabelMode.NONE) }
                if (data.snapshot != null && data.captionData != null) SpaceCompassPanoramaCircleButton(
                    stringResource(R.string.panorama_position), { choosePosition = true }, enabled = !export.saving,
                    modifier = Modifier.testTag("panorama-position"), stateText = positionState) {
                    SpaceCompassPanoramaPositionIcon(requestedPosition == SpaceCompassPanoramaPosition.HIDDEN)
                }
                if (data.cameraPhoto == null && data.snapshot != null) SpaceCompassPanoramaCircleButton(
                    stringResource(R.string.panorama_center), { chooseCenter = true }, enabled = !export.saving,
                    modifier = Modifier.testTag("panorama-center"), stateText = stringResource(requestedCenter.labelResource)) {
                    SpaceCompassPanoramaCompassIcon()
                }
            }
        }
    if (chooseLabels) SpaceCompassPanoramaLabelsDialog(requestedLabelMode, onSelect = { mode ->
        if (mode != requestedLabelMode) {
            preferences?.edit()?.putString(SPACE_COMPASS_PANORAMA_LABEL_MODE_KEY, mode.key)?.apply()
            requestedLabelMode = mode
            notify(R.string.control_choice_message, resources.getString(R.string.panorama_point_labels),
                resources.getString(mode.labelResource))
            revision++
        }
        chooseLabels = false
    }, onDismiss = { chooseLabels = false })
    if (chooseCenter && data.cameraPhoto == null) SpaceCompassPanoramaCenterDialog(requestedCenter, onSelect = { center ->
        if (center != requestedCenter) {
            preferences?.edit()?.putString(SPACE_COMPASS_PANORAMA_CENTER_KEY, center.key)?.apply()
            requestedCenter = center
            notify(R.string.control_choice_message, resources.getString(R.string.panorama_center),
                resources.getString(center.labelResource))
            revision++
        }
        chooseCenter = false
    }, onDismiss = { chooseCenter = false })
    if (choosePosition) SpaceCompassPanoramaPositionDialog(requestedPosition, false, onSelect = { position ->
        if (position != requestedPosition) {
            preferences?.edit()?.putString(SPACE_COMPASS_PANORAMA_POSITION_KEY, position.key)?.apply()
            requestedPosition = position
            notify(R.string.control_choice_message, resources.getString(R.string.panorama_position),
                resources.getString(position.labelResource))
            revision++
        }
        choosePosition = false
    }, onDismiss = { choosePosition = false })
    if (export.visible) SpaceCompassPanoramaExportPage(export)
    }
}

@Composable
private fun SpaceCompassPanoramaZoomImage(bitmap: android.graphics.Bitmap, modifier: Modifier,
    backButton: @Composable () -> Unit) {
    val image = remember(bitmap) { bitmap.asImageBitmap() }
    var size by remember { mutableStateOf(IntSize.Zero) }
    var zoom by remember { mutableFloatStateOf(1f) }
    var offset by remember { mutableStateOf(Offset.Zero) }
    fun bound(value: Offset, scale: Float): Offset {
        val fit = min(size.width.toFloat() / bitmap.width, size.height.toFloat() / bitmap.height) *
            spaceCompassCaptureBaseScale(size.width.toFloat(), size.height.toFloat(), bitmap.width, bitmap.height)
        val x = ((bitmap.width * fit * scale - size.width) / 2).coerceAtLeast(0f)
        val y = ((bitmap.height * fit * scale - size.height) / 2).coerceAtLeast(0f)
        return Offset(value.x.coerceIn(-x, x), value.y.coerceIn(-y, y))
    }
    val zoomInLabel = stringResource(R.string.celestial_view_zoom_in)
    val zoomOutLabel = stringResource(R.string.celestial_view_zoom_out)
    BoxWithConstraints(modifier.clipToBounds().background(Color.Black).onSizeChanged { size = it; offset = bound(offset, zoom) }
        .pointerInput(bitmap, size) { detectTransformGestures { centroid, pan, scale, _ ->
            val next = (zoom * scale).coerceIn(1f, 8f)
            val ratio = next / zoom
            val center = Offset(size.width / 2f, size.height / 2f)
            offset = bound(offset * ratio + (centroid - center) * (1 - ratio) + pan, next)
            zoom = next
        } }.pointerInput(bitmap) { detectTapGestures(onDoubleTap = {
            zoom = if (zoom > 1f) 1f else 3f; offset = Offset.Zero
        }) }) {
        Image(image, stringResource(R.string.panorama_preview), Modifier.fillMaxSize()
            .graphicsLayer {
                val base = spaceCompassCaptureBaseScale(size.width.toFloat(), size.height.toFloat(), bitmap.width, bitmap.height)
                scaleX = zoom * base; scaleY = zoom * base; translationX = offset.x; translationY = offset.y },
            contentScale = ContentScale.Fit)
        Column(Modifier.align(Alignment.TopStart).spaceCompassPanoramaTopControlInsets().testTag("panorama-back-corner")) {
            backButton()
        }
        Column(Modifier.align(Alignment.CenterStart).spaceCompassPanoramaCenteredControlInsets(maxHeight, 2)
            .testTag("panorama-left-controls"), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            SpaceCompassPanoramaCircleButton(zoomOutLabel,
                onClick = { zoom = (zoom / 1.5f).coerceAtLeast(1f); offset = bound(offset, zoom) },
                enabled = zoom > 1f, modifier = Modifier.testTag("panorama-zoom-out")) {
                SpaceCompassPanoramaControlIcon(SpaceCompassPanoramaControl.ZOOM_OUT)
            }
            SpaceCompassPanoramaCircleButton(zoomInLabel,
                onClick = { zoom = (zoom * 1.5f).coerceAtMost(8f); offset = bound(offset, zoom) },
                enabled = zoom < 8f, modifier = Modifier.testTag("panorama-zoom-in")) {
                SpaceCompassPanoramaControlIcon(SpaceCompassPanoramaControl.ZOOM_IN)
            }
        }
    }
}

/** Same neutral circular surface, outline, elevation and touch target as the celestial selector. */
@Composable
private fun SpaceCompassPanoramaCircleButton(label: String, onClick: () -> Unit,
    modifier: Modifier = Modifier, enabled: Boolean = true, checked: Boolean? = null,
    stateText: String? = null, content: @Composable () -> Unit) {
    val foreground = spaceCompassFloatingControlTint(MaterialTheme.colorScheme.onSurface,
        active = checked == true, enabled = enabled)
    SpaceCompassFloatingControlHitRegion {
        Surface(onClick = onClick, enabled = enabled,
            modifier = modifier.spaceCompassFloatingControlVisual().size(48.dp).spaceCompassAccessibleAction(label, enabled = enabled,
                role = if (checked == null) Role.Button else Role.Checkbox, checkedState = checked,
                stateText = stateText, onClick = onClick),
            shape = CircleShape, color = spaceCompassPageBackground().copy(alpha = 1f),
            contentColor = foreground.copy(alpha = if (enabled) 1f else .38f),
            border = BorderStroke(1.dp, foreground.copy(alpha = if (enabled) .35f else .16f)), shadowElevation = 3.dp) {
            Box(contentAlignment = Alignment.Center) { content() }
        }
    }
}
