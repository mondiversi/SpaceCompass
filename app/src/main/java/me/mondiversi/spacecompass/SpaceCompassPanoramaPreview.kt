package me.mondiversi.spacecompass

import android.Manifest
import android.content.ClipData
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.BitmapFactory
import android.os.Build
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.semantics.Role
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import kotlin.math.min

internal data class SpaceCompassPanoramaPreviewData(val file: File, val timeMs: Long,
    val position: SpaceCompassPanoramaPosition = SpaceCompassPanoramaPosition.COMPLETE,
    val snapshot: SpaceCompassPanoramaSnapshot? = null, val captionData: SpaceCompassPanoramaCaptionData? = null,
    val credits: String? = null, val cameraPhoto: SpaceCompassCameraPhotoSnapshot? = null,
    val selectedPresentation: SpaceCompassPanoramaPresentation? = null,
    val fileExportMode: SpaceCompassPanoramaExportMode = SpaceCompassPanoramaExportMode.INTERNATIONAL)

// Match the main sky's 10 dp content inset plus 4 dp action inset, including pixel rounding.
private fun Modifier.spaceCompassPanoramaControlSideInsets(includeBottom: Boolean = false): Modifier =
    padding(start = 10.dp, end = 10.dp, bottom = if (includeBottom) 10.dp else 0.dp)
        .padding(start = 4.dp, end = 4.dp, bottom = if (includeBottom) 4.dp else 0.dp)

// Reserve one floating-control height above each column without increasing the gaps inside it.
private fun Modifier.spaceCompassPanoramaTopControlInsets(): Modifier =
    spaceCompassPanoramaControlSideInsets().padding(top = spaceCompassFloatingControlSize + 8.dp, bottom = 8.dp)

/** Preview follows the device orientation; portrait starts filled and pannable. */
@Composable
internal fun SpaceCompassPanoramaPreview(data: SpaceCompassPanoramaPreviewData, onBack: () -> Unit) {
    val context = LocalContext.current
    val resources = LocalResources.current
    val scope = rememberCoroutineScope()
    val preferences = LocalSpaceCompassPreferences.current
    val previewMode = if (data.selectedPresentation != null) SpaceCompassPanoramaExportMode.SELECTED else data.fileExportMode
    var currentFile by remember(data.file) { mutableStateOf(data.file) }
    var currentPosition by remember(data.file) { mutableStateOf(data.position) }
    var requestedPosition by remember(data.file) { mutableStateOf(data.position) }
    var currentPointLabels by remember(data.file) { mutableStateOf(data.snapshot?.showPointLabels ?: true) }
    var requestedPointLabels by remember(data.file) { mutableStateOf(data.snapshot?.showPointLabels ?: true) }
    var requestedExportMode by remember(data.file) { mutableStateOf(
        if (data.selectedPresentation == null) SpaceCompassPanoramaExportMode.INTERNATIONAL else
            SpaceCompassPanoramaExportMode.fromStored(preferences?.getString(SPACE_COMPASS_PANORAMA_EXPORT_MODE_KEY, null))) }
    var revision by remember(data.file) { mutableIntStateOf(0) }
    var preparationFailed by remember(data.file) { mutableStateOf(false) }
    var choosePosition by remember { mutableStateOf(false) }
    var chooseCenter by remember { mutableStateOf(false) }
    var currentCenter by remember(data.file) { mutableStateOf(data.snapshot?.center ?: SpaceCompassPanoramaCenter.SOUTH) }
    var requestedCenter by remember(data.file) { mutableStateOf(data.snapshot?.center ?: SpaceCompassPanoramaCenter.SOUTH) }
    LaunchedEffect(data, requestedPosition, requestedPointLabels, requestedCenter, revision) {
        preparationFailed = false
        if (requestedPosition != currentPosition || requestedPointLabels != currentPointLabels || requestedCenter != currentCenter) {
            val targetPosition = requestedPosition
            val targetLabels = requestedPointLabels
            val targetCenter = requestedCenter
            try {
                val file = withContext(Dispatchers.IO) {
                    prepareSpaceCompassPanoramaVariant(context.applicationContext, data, targetPosition, targetLabels, previewMode, targetCenter)
                }
                currentFile = file
                currentPosition = targetPosition
                currentPointLabels = targetLabels
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
    var savedGalleryUris by rememberSaveable(data.file.absolutePath) { mutableStateOf<Map<String, String>>(emptyMap()) }
    var documentSource by rememberSaveable(data.file.absolutePath) { mutableStateOf<String?>(null) }
    var saving by remember { mutableStateOf(false) }
    var chooseExport by remember { mutableStateOf(false) }
    var preparedExportSource by remember(data.file) { mutableStateOf<File?>(null) }
    var preparedExportKey by remember(data.file) { mutableStateOf<String?>(null) }
    var gallerySource by rememberSaveable(data.file.absolutePath) { mutableStateOf<String?>(null) }
    fun notify(id: Int) = showSpaceCompassBottomMessage(context, resources.getString(id))
    fun saveGallery(file: File) {
        if (saving) return
        saving = true
        scope.launch {
            val status = try {
                val uri = withContext(Dispatchers.IO) { ensureSpaceCompassPanoramaInGallery(context, file,
                    data.timeMs, savedGalleryUris[file.absolutePath]?.let(Uri::parse)) }
                savedGalleryUris = savedGalleryUris + (file.absolutePath to uri.toString())
                R.string.panorama_saved
            } catch (cancelled: kotlinx.coroutines.CancellationException) { throw cancelled
            } catch (error: Exception) {
                SpaceCompassErrorLog.record(context, "panorama:gallery", error)
                R.string.panorama_error
            } finally { saving = false }
            notify(status)
        }
    }
    val permission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {
        val source = gallerySource?.let(::File)
        gallerySource = null
        if (it && source != null) saveGallery(source) else if (!it) notify(R.string.panorama_permission)
    }
    val saveDocument = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("image/jpeg")) { uri ->
        val source = documentSource?.let(::File)
        documentSource = null
        if (uri != null && source != null) {
            saving = true
            scope.launch {
                try {
                    withContext(Dispatchers.IO) {
                        context.contentResolver.openOutputStream(uri, "w")?.use { output ->
                            source.inputStream().use { it.copyTo(output) }
                        } ?: error("Image destination unavailable")
                    }
                    notify(R.string.panorama_exported)
                } catch (cancelled: kotlinx.coroutines.CancellationException) { throw cancelled
                } catch (error: Exception) {
                    SpaceCompassErrorLog.record(context, "panorama:document", error); notify(R.string.panorama_error)
                } finally { saving = false }
            }
        }
    }
    BackHandler(onBack = onBack)
    val background = spaceCompassPageBackground()
    val bitmap = decoded
    val positionReady = requestedPosition == currentPosition && requestedPointLabels == currentPointLabels &&
        requestedCenter == currentCenter && !preparationFailed
    val ready = !saving && positionReady && bitmap != null && !failed
    val exportKey = spaceCompassPanoramaVariantKey(requestedPosition, requestedPointLabels, requestedExportMode, requestedCenter)
    // Export profile changes prepare a separate file; the displayed preview always keeps user formatting.
    LaunchedEffect(data, chooseExport, exportKey, revision) {
        if (!chooseExport) return@LaunchedEffect
        preparedExportSource = null
        preparedExportKey = null
        val position = requestedPosition
        val labels = requestedPointLabels
        val mode = requestedExportMode
        val center = requestedCenter
        try {
            preparedExportSource = withContext(Dispatchers.IO) {
                prepareSpaceCompassPanoramaVariant(context.applicationContext, data, position, labels, mode, center)
            }
            preparedExportKey = exportKey
        } catch (cancelled: kotlinx.coroutines.CancellationException) { throw cancelled
        } catch (error: Exception) {
            SpaceCompassErrorLog.record(context, "panorama:export-profile", error)
            notify(R.string.panorama_error)
        } catch (error: OutOfMemoryError) { notify(R.string.panorama_error) }
    }
    val exportReady = ready && preparedExportKey == exportKey && preparedExportSource != null
    val positionState = stringResource(requestedPosition.labelResource)
    val backButton: @Composable () -> Unit = {
        SpaceCompassPanoramaCircleButton(stringResource(R.string.navigate_back), onBack,
            modifier = Modifier.testTag("panorama-back")) {
            SpaceCompassPanoramaControlIcon(SpaceCompassPanoramaControl.BACK)
        }
    }
    BoxWithConstraints(Modifier.fillMaxSize().background(background).testTag("panorama-preview")) {
        Box(Modifier.fillMaxSize().testTag("panorama-full-image-viewport")) {
            if (bitmap == null || !positionReady) Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                if (failed || preparationFailed) Text(stringResource(R.string.panorama_error)) else CircularProgressIndicator()
                Column(Modifier.align(Alignment.TopStart).spaceCompassPanoramaTopControlInsets().testTag("panorama-left-controls")) {
                    backButton()
                }
            } else SpaceCompassPanoramaZoomImage(bitmap, Modifier.fillMaxSize(), backButton)
            Column(Modifier.align(Alignment.TopEnd).spaceCompassPanoramaTopControlInsets().testTag("panorama-right-controls"),
                verticalArrangement = Arrangement.spacedBy(8.dp)) {
                if (data.snapshot != null && data.captionData != null) SpaceCompassPanoramaCircleButton(
                    stringResource(R.string.panorama_point_labels), {
                        requestedPointLabels = !requestedPointLabels
                        preferences?.edit()?.putBoolean(SPACE_COMPASS_PANORAMA_POINT_LABELS_KEY, requestedPointLabels)?.apply()
                        revision++
                    }, enabled = !saving, checked = requestedPointLabels,
                    modifier = Modifier.testTag("panorama-point-labels")) {
                    SpaceCompassPanoramaLabelsIcon(requestedPointLabels)
                }
                if (data.snapshot != null && data.captionData != null) SpaceCompassPanoramaCircleButton(
                    stringResource(R.string.panorama_position), { choosePosition = true }, enabled = !saving,
                    modifier = Modifier.testTag("panorama-position"), stateText = positionState) {
                    SpaceCompassPanoramaPositionIcon(requestedPosition == SpaceCompassPanoramaPosition.HIDDEN)
                }
                if (data.cameraPhoto == null && data.snapshot != null) SpaceCompassPanoramaCircleButton(
                    stringResource(R.string.panorama_center), { chooseCenter = true }, enabled = !saving,
                    modifier = Modifier.testTag("panorama-center"), stateText = stringResource(requestedCenter.labelResource)) {
                    SpaceCompassPanoramaCompassIcon()
                }
            }
            SpaceCompassPanoramaCircleButton(stringResource(R.string.panorama_export), { chooseExport = true },
                enabled = ready, modifier = Modifier.align(Alignment.BottomEnd).spaceCompassPanoramaControlSideInsets(includeBottom = true).testTag("panorama-export")) {
                SpaceCompassPanoramaExportIcon()
            }
        }
    if (chooseCenter && data.cameraPhoto == null) SpaceCompassPanoramaCenterDialog(requestedCenter, onSelect = { center ->
        preferences?.edit()?.putString(SPACE_COMPASS_PANORAMA_CENTER_KEY, center.key)?.apply()
        requestedCenter = center
        revision++
        chooseCenter = false
    }, onDismiss = { chooseCenter = false })
    if (choosePosition) SpaceCompassPanoramaPositionDialog(requestedPosition, false, onSelect = { position ->
        preferences?.edit()?.putString(SPACE_COMPASS_PANORAMA_POSITION_KEY, position.key)?.apply()
        requestedPosition = position
        revision++
        choosePosition = false
    }, onDismiss = { choosePosition = false })
    if (chooseExport) SpaceCompassPanoramaExportDialog(requestedExportMode, onModeChange = { mode ->
        requestedExportMode = mode
        preferences?.edit()?.putString(SPACE_COMPASS_PANORAMA_EXPORT_MODE_KEY, mode.key)?.apply()
    }, enabled = exportReady, onGallery = {
        if (exportReady) {
            chooseExport = false
            val file = requireNotNull(preparedExportSource)
            if (Build.VERSION.SDK_INT < 29 && ContextCompat.checkSelfPermission(context,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                gallerySource = file.absolutePath
                permission.launch(Manifest.permission.WRITE_EXTERNAL_STORAGE)
            } else saveGallery(file)
        }
    }, onDocument = {
        if (exportReady) {
            chooseExport = false
            documentSource = requireNotNull(preparedExportSource).absolutePath
            saveDocument.launch(spaceCompassPanoramaFileName(data.timeMs))
        }
    }, onShare = {
        if (exportReady) {
            chooseExport = false
            runCatching {
                val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", requireNotNull(preparedExportSource))
                val intent = Intent(Intent.ACTION_SEND).apply {
                    type = "image/jpeg"; putExtra(Intent.EXTRA_STREAM, uri)
                    clipData = ClipData.newUri(context.contentResolver, "Space Compass", uri)
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
                context.startActivity(Intent.createChooser(intent, resources.getString(R.string.panorama_share)))
            }.onFailure { SpaceCompassErrorLog.record(context, "panorama:share", it); notify(R.string.panorama_error) }
        }
    }, onDismiss = { chooseExport = false })
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
    Box(modifier.clipToBounds().background(Color.Black).onSizeChanged { size = it; offset = bound(offset, zoom) }
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
        // Back, decrease and increase form the leading top column in either orientation.
        Column(Modifier.align(Alignment.TopStart).spaceCompassPanoramaTopControlInsets().testTag("panorama-left-controls"),
            verticalArrangement = Arrangement.spacedBy(8.dp)) {
            backButton()
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
            shape = CircleShape, color = spaceCompassPageBackground().copy(alpha = .94f),
            contentColor = foreground.copy(alpha = if (enabled) 1f else .38f),
            border = BorderStroke(1.dp, foreground.copy(alpha = if (enabled) .35f else .16f)), shadowElevation = 3.dp) {
            Box(contentAlignment = Alignment.Center) { content() }
        }
    }
}
