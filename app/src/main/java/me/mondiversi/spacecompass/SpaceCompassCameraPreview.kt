package me.mondiversi.spacecompass

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Matrix
import android.graphics.Rect
import android.graphics.SurfaceTexture
import android.hardware.camera2.*
import android.media.ImageReader
import android.graphics.ImageFormat
import android.os.SystemClock
import kotlinx.coroutines.CancellableContinuation
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import android.hardware.display.DisplayManager
import android.os.Build
import android.os.Handler
import android.os.HandlerThread
import android.os.Looper
import android.view.Surface
import android.view.TextureView
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.math.abs
import kotlin.math.atan

internal fun hasSpaceCompassCameraPermission(context: Context) =
    ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED

@Composable
internal fun SpaceCompassCameraPreview(frame: SpaceCompassSunSceneFrame, modifier: Modifier,
    onPerspective: (SpaceCompassPerspective?) -> Unit, onCaptureReady: (SpaceCompassCameraCapture?) -> Unit,
    attitudeAt: (Long, Int) -> SpaceCompassCameraAttitude?, onError: () -> Unit) {
    val perspective by rememberUpdatedState(onPerspective)
    val error by rememberUpdatedState(onError)
    val captureReady by rememberUpdatedState(onCaptureReady)
    val attitude by rememberUpdatedState(attitudeAt)
    var controller by remember { mutableStateOf<SpaceCompassCameraPreviewController?>(null) }
    AndroidView(modifier = modifier.testTag("camera-preview"), factory = { context ->
        TextureView(context).apply {
            isOpaque = true
            importantForAccessibility = android.view.View.IMPORTANT_FOR_ACCESSIBILITY_NO
            controller = SpaceCompassCameraPreviewController(this, { perspective(it) }, { captureReady(it) }, { time, rotation -> attitude(time, rotation) }, { error() })
        }
    }, update = { controller?.updateFrame(frame) }, onRelease = { controller?.close(); controller = null })
}

/** A rear preview plus an on-demand still JPEG; no recorder, automatic gallery write or frame upload. */
private class SpaceCompassCameraPreviewController(
    private val view: TextureView, private val onPerspective: (SpaceCompassPerspective?) -> Unit,
    private val onCaptureReady: (SpaceCompassCameraCapture?) -> Unit,
    private val attitudeAt: (Long, Int) -> SpaceCompassCameraAttitude?, private val onError: () -> Unit
) : TextureView.SurfaceTextureListener, DisplayManager.DisplayListener, SpaceCompassCameraCapture {
    private val main = Handler(Looper.getMainLooper())
    private val worker = HandlerThread("SpaceCompass camera").apply { start() }
    private val handler = Handler(worker.looper)
    private val closed = AtomicBoolean(false)
    private val openInFlight = AtomicBoolean(false)
    private val displays = view.context.getSystemService(DisplayManager::class.java)
    private var frame: SpaceCompassSunSceneFrame? = null
    private var lens: SpaceCompassCameraLens? = null
    private var stream: android.util.Size? = null
    private var ready = false
    private var lastGeometry: SpaceCompassCameraGeometry? = null
    // Camera/session/surface are confined to the camera handler, including disposal.
    private var camera: CameraDevice? = null
    private var session: CameraCaptureSession? = null
    private var surface: Surface? = null
    private var photoReader: ImageReader? = null
    private var previewRequest: CaptureRequest? = null
    private var captureLens: ((Rect, Float) -> SpaceCompassCameraLens)? = null
    private var captureRealtime = false
    private var pendingPhoto: PendingPhoto? = null
    private class PendingPhoto(val continuation: CancellableContinuation<SpaceCompassCameraPhotoFrame>, val rotation: Int) {
        var jpeg: ByteArray? = null
        var imageTimestamp: Long = 0
        var resultTimestamp: Long = 0
        var exposureNanos: Long = 0
        var capturedTimeMs: Long = 0
        var lens: SpaceCompassCameraLens? = null
    }
    private val photoTimeout = Runnable { finishPhotoError(IllegalStateException("Camera capture timed out")) }
    private var opening = false
    private val startupTimeout = Runnable { if (!ready && !closed.get()) fail() }
    init {
        view.surfaceTextureListener = this
        displays.registerDisplayListener(this, main)
        main.postDelayed(startupTimeout, 10_000L)
        view.addOnLayoutChangeListener { _, _, _, _, _, _, _, _, _ -> transform() }
        if (view.isAvailable) start(view.surfaceTexture!!)
    }
    fun updateFrame(next: SpaceCompassSunSceneFrame) {
        if (frame != next) { frame = next; transform() }
    }
    private fun transform() {
        if (closed.get() || !ready || view.width <= 0 || view.height <= 0) return
        val currentLens = lens ?: return
        val size = stream ?: return
        val currentFrame = frame ?: return
        val geometry = spaceCompassCameraGeometry(currentLens, size.width.toDouble(), size.height.toDouble(),
            (view.display?.rotation ?: Surface.ROTATION_0) * 90, view.width.toDouble(), view.height.toDouble(), currentFrame)
        if (geometry == null) { fail(); return }
        if (geometry == lastGeometry) return
        fun points(values: List<SpaceCompassSunScenePoint>) = values.flatMap { listOf(it.x.toFloat(), it.y.toFloat()) }.toFloatArray()
        val matrix = Matrix()
        if (!matrix.setPolyToPoly(points(geometry.source), 0, points(geometry.destination), 0, 4)) { fail(); return }
        view.setTransform(matrix)
        lastGeometry = geometry
        onPerspective(geometry.perspective)
        onCaptureReady(this)
    }
    @SuppressLint("MissingPermission")
    private fun start(texture: SurfaceTexture) {
        if (opening || closed.get()) return
        opening = true
        handler.post {
            if (closed.get()) return@post
            try {
                check(hasSpaceCompassCameraPermission(view.context))
                val manager = view.context.getSystemService(CameraManager::class.java)
                // Prefer the ordinary wide rear lens, not a telephoto/ultrawide/front camera.
                val candidates = manager.cameraIdList.map { it to manager.getCameraCharacteristics(it) }
                    .filter { it.second[CameraCharacteristics.LENS_FACING] == CameraCharacteristics.LENS_FACING_BACK &&
                        it.second[CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP]?.getOutputSizes(SurfaceTexture::class.java)?.isNotEmpty() == true }
                val (id, characteristics) = candidates.minByOrNull { (_, c) ->
                    val physical = c[CameraCharacteristics.SENSOR_INFO_PHYSICAL_SIZE]
                    val focal = c[CameraCharacteristics.LENS_INFO_AVAILABLE_FOCAL_LENGTHS]?.firstOrNull()
                    if (physical == null || focal == null) Double.MAX_VALUE else
                        abs(Math.toDegrees(2 * atan(physical.width / (2.0 * focal))) - 65.0)
                } ?: error("No rear camera")
                val active = characteristics[CameraCharacteristics.SENSOR_INFO_ACTIVE_ARRAY_SIZE] ?: error("No sensor rectangle")
                val sizes = characteristics[CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP]!!.getOutputSizes(SurfaceTexture::class.java)
                val bounded = sizes.filter { it.width <= 1920 && it.height <= 1080 }
                val size = (bounded.ifEmpty { sizes.toList() }).minWith(compareBy<android.util.Size> {
                    abs(it.width.toDouble() / it.height - active.width().toDouble() / active.height())
                }.thenBy { abs(it.width.toLong() * it.height - 1280L * 960) })
                val physical = characteristics[CameraCharacteristics.SENSOR_INFO_PHYSICAL_SIZE] ?: error("No lens size")
                val pixels = characteristics[CameraCharacteristics.SENSOR_INFO_PIXEL_ARRAY_SIZE] ?: error("No pixel array")
                val nominalFocal = characteristics[CameraCharacteristics.LENS_INFO_AVAILABLE_FOCAL_LENGTHS]?.firstOrNull() ?: error("No focal length")
                val rotation = characteristics[CameraCharacteristics.SENSOR_ORIENTATION] ?: error("No orientation")
                val preCorrection = characteristics[CameraCharacteristics.SENSOR_INFO_PRE_CORRECTION_ACTIVE_ARRAY_SIZE] ?: active
                val intrinsic = characteristics[CameraCharacteristics.LENS_INTRINSIC_CALIBRATION]?.takeIf {
                    it.size == 5 && it.all { value -> value.isFinite() } && it[0] > 0 && it[1] > 0 && abs(it[4]) < .001f
                }
                // Use hardware calibration where provided, otherwise physical sensor/focal metadata.
                fun optics(crop: Rect, focal: Float) = SpaceCompassCameraLens(crop.left.toDouble(), crop.top.toDouble(),
                    crop.width().toDouble(), crop.height().toDouble(),
                    intrinsic?.let { it[0].toDouble() * focal / nominalFocal } ?: (focal * pixels.width / physical.width.toDouble()),
                    intrinsic?.let { it[1].toDouble() * focal / nominalFocal } ?: (focal * pixels.height / physical.height.toDouble()),
                    intrinsic?.let { preCorrection.left + it[2].toDouble() } ?: active.exactCenterX().toDouble(),
                    intrinsic?.let { preCorrection.top + it[3].toDouble() } ?: active.exactCenterY().toDouble(), rotation)
                captureLens = ::optics
                captureRealtime = characteristics[CameraCharacteristics.SENSOR_INFO_TIMESTAMP_SOURCE] ==
                    CameraCharacteristics.SENSOR_INFO_TIMESTAMP_SOURCE_REALTIME
                val jpegSizes = characteristics[CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP]!!.getOutputSizes(ImageFormat.JPEG)
                val photoSizes = jpegSizes.filter { it.width.toLong() * it.height <= 2048L * 1536 && maxOf(it.width,it.height) <= 2048 }
                val photoSize = (photoSizes.ifEmpty { listOf(jpegSizes.minBy { it.width.toLong()*it.height }) }).minWith(compareBy<android.util.Size> {
                    abs(it.width.toDouble()/it.height-active.width().toDouble()/active.height())
                }.thenByDescending { it.width.toLong()*it.height })
                photoReader = ImageReader.newInstance(photoSize.width, photoSize.height, ImageFormat.JPEG, 2).apply {
                    setOnImageAvailableListener({ reader ->
                        val image = reader.acquireNextImage() ?: return@setOnImageAvailableListener
                        try {
                            pendingPhoto?.let { pending ->
                                val buffer = image.planes[0].buffer
                                pending.jpeg = ByteArray(buffer.remaining()).also { buffer.get(it) }
                                pending.imageTimestamp = image.timestamp
                                finishPhotoIfReady()
                            }
                        } catch (failure: Exception) { finishPhotoError(failure) }
                        catch (_: OutOfMemoryError) { finishPhotoError(IllegalStateException("Insufficient memory for photo")) } finally { image.close() }
                    }, handler)
                }
                texture.setDefaultBufferSize(size.width, size.height)
                surface = Surface(texture)
                main.post { if (!closed.get()) { stream = size; lens = optics(active, nominalFocal) } }
                openInFlight.set(true)
                manager.openCamera(id, object : CameraDevice.StateCallback() {
                    override fun onOpened(device: CameraDevice) {
                        openInFlight.set(false)
                        if (closed.get()) { device.close(); worker.quitSafely(); return }
                        camera = device
                        try {
                            val request = device.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW).apply {
                                addTarget(surface!!)
                                set(CaptureRequest.CONTROL_MODE, CaptureRequest.CONTROL_MODE_AUTO)
                                val autofocus = characteristics[CameraCharacteristics.CONTROL_AF_AVAILABLE_MODES] ?: intArrayOf()
                                set(CaptureRequest.CONTROL_AF_MODE, if (CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE in autofocus)
                                    CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE else CaptureRequest.CONTROL_AF_MODE_OFF)
                                set(CaptureRequest.SCALER_CROP_REGION, active)
                                set(CaptureRequest.LENS_FOCAL_LENGTH, nominalFocal)
                                set(CaptureRequest.CONTROL_VIDEO_STABILIZATION_MODE, CaptureRequest.CONTROL_VIDEO_STABILIZATION_MODE_OFF)
                                set(CaptureRequest.LENS_OPTICAL_STABILIZATION_MODE, CaptureRequest.LENS_OPTICAL_STABILIZATION_MODE_OFF)
                                if (Build.VERSION.SDK_INT >= 31) set(CaptureRequest.SCALER_ROTATE_AND_CROP, CaptureRequest.SCALER_ROTATE_AND_CROP_NONE)
                                if (Build.VERSION.SDK_INT >= 28 && CameraMetadata.DISTORTION_CORRECTION_MODE_FAST in
                                    (characteristics[CameraCharacteristics.DISTORTION_CORRECTION_AVAILABLE_MODES] ?: intArrayOf())) {
                                    set(CaptureRequest.DISTORTION_CORRECTION_MODE, CameraMetadata.DISTORTION_CORRECTION_MODE_FAST)
                                }
                            }.build()
                            @Suppress("DEPRECATION")
                            device.createCaptureSession(listOf(surface!!, photoReader!!.surface), object : CameraCaptureSession.StateCallback() {
                                override fun onConfigured(configured: CameraCaptureSession) {
                                    if (closed.get()) { configured.close(); return }
                                    session = configured
                                    try {
                                        previewRequest = request
                                        var previous: SpaceCompassCameraLens? = null
                                        configured.setRepeatingRequest(request, object : CameraCaptureSession.CaptureCallback() {
                                            override fun onCaptureCompleted(s: CameraCaptureSession, r: CaptureRequest, result: TotalCaptureResult) {
                                                val next = optics(result[CaptureResult.SCALER_CROP_REGION] ?: active,
                                                    result[CaptureResult.LENS_FOCAL_LENGTH] ?: nominalFocal)
                                                if (next != previous) { previous = next; main.post { if (!closed.get()) {
                                                    lens = next; ready = true; transform()
                                                } } }
                                            }
                                        }, handler)
                                    } catch (failure: Exception) { fail(failure) }
                                }
                                override fun onConfigureFailed(configured: CameraCaptureSession) { configured.close(); fail() }
                            }, handler)
                        } catch (failure: Exception) { fail(failure) }
                    }
                    override fun onDisconnected(device: CameraDevice) {
                        openInFlight.set(false); device.close()
                        if (closed.get()) worker.quitSafely() else fail()
                    }
                    override fun onError(device: CameraDevice, error: Int) {
                        openInFlight.set(false); device.close()
                        if (closed.get()) worker.quitSafely() else fail()
                    }
                }, handler)
            } catch (failure: Exception) { openInFlight.set(false); fail(failure) }
        }
    }
    override suspend fun capturePhoto(): SpaceCompassCameraPhotoFrame = suspendCancellableCoroutine { continuation ->
        val rotation = (view.display?.rotation ?: Surface.ROTATION_0) * 90
        if (closed.get() || !ready) {
            continuation.resumeWithException(IllegalStateException("Camera not ready"))
        } else {
            continuation.invokeOnCancellation { handler.post {
                if (pendingPhoto?.continuation === continuation) { pendingPhoto = null; handler.removeCallbacks(photoTimeout) }
            } }
            handler.post {
                if (!continuation.isActive) return@post
                if (closed.get() || pendingPhoto != null || session == null || photoReader == null) {
                    continuation.resumeWithException(IllegalStateException("Camera unavailable")); return@post
                }
                val pending = PendingPhoto(continuation, rotation)
                pendingPhoto = pending
                handler.postDelayed(photoTimeout,10_000L)
                try {
                    // Drain only older JPEGs; still images are never requested by the repeating preview.
                    while (true) (photoReader!!.acquireNextImage() ?: break).close()
                    val device = requireNotNull(camera)
                    val previous = requireNotNull(previewRequest)
                    val request = device.createCaptureRequest(CameraDevice.TEMPLATE_STILL_CAPTURE).apply {
                        addTarget(photoReader!!.surface)
                        for (key in listOf(CaptureRequest.CONTROL_MODE, CaptureRequest.CONTROL_AF_MODE,
                            CaptureRequest.CONTROL_VIDEO_STABILIZATION_MODE, CaptureRequest.LENS_OPTICAL_STABILIZATION_MODE))
                            previous[key]?.let { set(key,it) }
                        previous[CaptureRequest.SCALER_CROP_REGION]?.let { set(CaptureRequest.SCALER_CROP_REGION,it) }
                        previous[CaptureRequest.LENS_FOCAL_LENGTH]?.let { set(CaptureRequest.LENS_FOCAL_LENGTH,it) }
                        if (Build.VERSION.SDK_INT>=28) previous[CaptureRequest.DISTORTION_CORRECTION_MODE]?.let {
                            set(CaptureRequest.DISTORTION_CORRECTION_MODE,it) }
                        if (Build.VERSION.SDK_INT>=31) set(CaptureRequest.SCALER_ROTATE_AND_CROP,CaptureRequest.SCALER_ROTATE_AND_CROP_NONE)
                        set(CaptureRequest.JPEG_ORIENTATION,0)
                        set(CaptureRequest.JPEG_QUALITY,95.toByte())
                        set(CaptureRequest.FLASH_MODE,CaptureRequest.FLASH_MODE_OFF)
                    }.build()
                    session!!.capture(request, object : CameraCaptureSession.CaptureCallback() {
                        override fun onCaptureStarted(s: CameraCaptureSession,r: CaptureRequest,timestamp: Long,frameNumber: Long) {
                            if (pendingPhoto !== pending) return
                            val now = SystemClock.elapsedRealtimeNanos()
                            pending.exposureNanos = if (captureRealtime) timestamp else now
                            pending.capturedTimeMs = System.currentTimeMillis() +
                                (if (captureRealtime) (timestamp-now)/1_000_000 else 0)
                        }
                        override fun onCaptureCompleted(s: CameraCaptureSession,r: CaptureRequest,result: TotalCaptureResult) {
                            if (pendingPhoto !== pending) return
                            val fallback = requireNotNull(lens)
                            val crop = result[CaptureResult.SCALER_CROP_REGION] ?: Rect(fallback.cropLeft.toInt(),fallback.cropTop.toInt(),
                                (fallback.cropLeft+fallback.cropWidth).toInt(),(fallback.cropTop+fallback.cropHeight).toInt())
                            pending.lens = requireNotNull(captureLens)(crop,result[CaptureResult.LENS_FOCAL_LENGTH] ?: previous[CaptureRequest.LENS_FOCAL_LENGTH]!!)
                            pending.resultTimestamp = result[CaptureResult.SENSOR_TIMESTAMP] ?: pending.imageTimestamp
                            finishPhotoIfReady()
                        }
                        override fun onCaptureFailed(s: CameraCaptureSession,r: CaptureRequest,failure: CaptureFailure) {
                            if (pendingPhoto === pending) finishPhotoError(IllegalStateException("Camera capture failed"))
                        }
                    },handler)
                } catch (failure: Exception) { finishPhotoError(failure) }
            }
        }
    }
    private fun finishPhotoIfReady() {
        val pending = pendingPhoto ?: return
        val jpeg = pending.jpeg ?: return
        val optics = pending.lens ?: return
        if (pending.resultTimestamp==0L || pending.imageTimestamp!=pending.resultTimestamp) return
        pendingPhoto=null; handler.removeCallbacks(photoTimeout)
        main.post {
            if (pending.continuation.isActive) pending.continuation.resume(SpaceCompassCameraPhotoFrame(jpeg,optics,
                pending.rotation,pending.capturedTimeMs,attitudeAt(pending.exposureNanos,pending.rotation)))
        }
    }
    private fun finishPhotoError(failure: Exception) {
        val pending = pendingPhoto ?: return
        pendingPhoto=null;handler.removeCallbacks(photoTimeout)
        if (pending.continuation.isActive) pending.continuation.resumeWithException(failure)
    }
    private fun fail(failure: Exception? = null) {
        main.post { if (!closed.get()) {
            failure?.let { SpaceCompassErrorLog.record(view.context, "camera:preview", it) }
            close(); onError()
        } }
    }
    fun close() {
        if (!closed.compareAndSet(false, true)) return
        displays.unregisterDisplayListener(this)
        main.removeCallbacks(startupTimeout)
        view.surfaceTextureListener = null
        onPerspective(null)
        onCaptureReady(null)
        handler.post {
            finishPhotoError(IllegalStateException("Camera preview closed"))
            session?.close(); session = null
            camera?.close(); camera = null
            surface?.release(); surface = null
            photoReader?.close(); photoReader = null
            // Keep the callback handler alive until a pending open can be closed immediately.
            if (!openInFlight.get()) worker.quitSafely()
        }
    }
    override fun onSurfaceTextureAvailable(texture: SurfaceTexture, width: Int, height: Int) = start(texture)
    override fun onSurfaceTextureSizeChanged(texture: SurfaceTexture, width: Int, height: Int) = transform()
    override fun onSurfaceTextureDestroyed(texture: SurfaceTexture): Boolean { fail(); return true }
    override fun onSurfaceTextureUpdated(texture: SurfaceTexture) = Unit
    override fun onDisplayAdded(displayId: Int) = Unit
    override fun onDisplayRemoved(displayId: Int) = Unit
    override fun onDisplayChanged(displayId: Int) { if (view.display?.displayId == displayId) transform() }
}
