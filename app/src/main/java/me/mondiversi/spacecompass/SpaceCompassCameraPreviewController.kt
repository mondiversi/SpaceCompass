package me.mondiversi.spacecompass

import android.annotation.SuppressLint
import android.graphics.ImageFormat
import android.graphics.Matrix
import android.graphics.Rect
import android.graphics.SurfaceTexture
import android.hardware.camera2.*
import android.hardware.display.DisplayManager
import android.media.ImageReader
import android.os.Build
import android.os.Handler
import android.os.HandlerThread
import android.os.Looper
import android.os.SystemClock
import android.view.Surface
import android.view.TextureView
import java.util.concurrent.atomic.AtomicBoolean
import kotlinx.coroutines.CancellableContinuation
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.math.abs

/** A rear preview plus an on-demand still JPEG; no recorder, automatic gallery write or frame upload. */
internal class SpaceCompassCameraPreviewController(
    private val view: TextureView, private val selected: SpaceCompassCameraZoomDevice, initialZoom: Float,
    private val onPerspective: (SpaceCompassPerspective?) -> Unit,
    private val onCaptureReady: (SpaceCompassCameraCapture?) -> Unit, private val onZoomActual: (Float) -> Unit,
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
    private var captureLens: ((TotalCaptureResult) -> SpaceCompassCameraLens)? = null
    private var requestedZoom = (initialZoom / selected.option.base).coerceIn(selected.option.minimum, selected.option.maximum)
    private var lastZoomInput = initialZoom
    private var submitPreview: (() -> Unit)? = null
    private var openAttempts = 0
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
    fun updateZoom(next: Float) {
        if (!next.isFinite() || next <= 0 || next == lastZoomInput || closed.get()) return
        lastZoomInput = next
        handler.post {
            if (!closed.get()) {
                requestedZoom = (next / selected.option.base).coerceIn(selected.option.minimum, selected.option.maximum)
                try { submitPreview?.invoke() } catch (failure: Exception) { fail(failure) }
            }
        }
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
                val id = selected.option.id
                val characteristics = selected.characteristics
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
                fun resultZoom(result: TotalCaptureResult): Float = if (selected.nativeRatio && Build.VERSION.SDK_INT >= 30) {
                    result[CaptureResult.CONTROL_ZOOM_RATIO] ?: result.request[CaptureRequest.CONTROL_ZOOM_RATIO] ?: 1f
                } else {
                    val crop = result[CaptureResult.SCALER_CROP_REGION] ?: result.request[CaptureRequest.SCALER_CROP_REGION] ?: active
                    minOf(active.width().toFloat() / crop.width(), active.height().toFloat() / crop.height())
                }
                fun resultOptics(result: TotalCaptureResult): SpaceCompassCameraLens {
                    val crop = result[CaptureResult.SCALER_CROP_REGION] ?: result.request[CaptureRequest.SCALER_CROP_REGION] ?: active
                    // Ratio coordinates describe the virtual post-zoom array, not the active physical lens.
                    val lens = optics(crop, if (selected.nativeRatio) nominalFocal else result[CaptureResult.LENS_FOCAL_LENGTH] ?: nominalFocal)
                    return if (selected.nativeRatio) lens.withZoomRatio(resultZoom(result), active.left.toDouble(),
                        active.top.toDouble(), active.width().toDouble(), active.height().toDouble()) else lens
                }
                captureLens = ::resultOptics
                fun applyZoom(builder: CaptureRequest.Builder) {
                    if (selected.nativeRatio && Build.VERSION.SDK_INT >= 30) {
                        builder.set(CaptureRequest.SCALER_CROP_REGION, active)
                        builder.set(CaptureRequest.CONTROL_ZOOM_RATIO, requestedZoom)
                        if (Build.VERSION.SDK_INT >= 36 && CaptureRequest.CONTROL_ZOOM_METHOD in characteristics.availableCaptureRequestKeys)
                            builder.set(CaptureRequest.CONTROL_ZOOM_METHOD, CameraMetadata.CONTROL_ZOOM_METHOD_ZOOM_RATIO)
                    } else {
                        val crop = spaceCompassCameraZoomCrop(active.left, active.top, active.width(), active.height(), requestedZoom)
                        builder.set(CaptureRequest.SCALER_CROP_REGION, Rect(crop[0], crop[1], crop[2], crop[3]))
                        builder.set(CaptureRequest.LENS_FOCAL_LENGTH, nominalFocal)
                    }
                }
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
                openAttempts++
                manager.openCamera(id, object : CameraDevice.StateCallback() {
                    override fun onOpened(device: CameraDevice) {
                        openInFlight.set(false)
                        if (closed.get()) { device.close(); worker.quitSafely(); return }
                        camera = device
                        try {
                            val builder = device.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW).apply {
                                addTarget(surface!!)
                                set(CaptureRequest.CONTROL_MODE, CaptureRequest.CONTROL_MODE_AUTO)
                                val autofocus = characteristics[CameraCharacteristics.CONTROL_AF_AVAILABLE_MODES] ?: intArrayOf()
                                set(CaptureRequest.CONTROL_AF_MODE, if (CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE in autofocus)
                                    CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE else CaptureRequest.CONTROL_AF_MODE_OFF)
                                applyZoom(this)
                                set(CaptureRequest.CONTROL_VIDEO_STABILIZATION_MODE, CaptureRequest.CONTROL_VIDEO_STABILIZATION_MODE_OFF)
                                set(CaptureRequest.LENS_OPTICAL_STABILIZATION_MODE, CaptureRequest.LENS_OPTICAL_STABILIZATION_MODE_OFF)
                                if (Build.VERSION.SDK_INT >= 31) set(CaptureRequest.SCALER_ROTATE_AND_CROP, CaptureRequest.SCALER_ROTATE_AND_CROP_NONE)
                                if (Build.VERSION.SDK_INT >= 28 && CameraMetadata.DISTORTION_CORRECTION_MODE_FAST in
                                    (characteristics[CameraCharacteristics.DISTORTION_CORRECTION_AVAILABLE_MODES] ?: intArrayOf())) {
                                    set(CaptureRequest.DISTORTION_CORRECTION_MODE, CameraMetadata.DISTORTION_CORRECTION_MODE_FAST)
                                }
                            }
                            @Suppress("DEPRECATION")
                            device.createCaptureSession(listOf(surface!!, photoReader!!.surface), object : CameraCaptureSession.StateCallback() {
                                override fun onConfigured(configured: CameraCaptureSession) {
                                    if (closed.get()) { configured.close(); return }
                                    session = configured
                                    try {
                                        var previous: SpaceCompassCameraLens? = null
                                        var previousZoom = Float.NaN
                                        val callback = object : CameraCaptureSession.CaptureCallback() {
                                            override fun onCaptureCompleted(s: CameraCaptureSession, r: CaptureRequest, result: TotalCaptureResult) {
                                                val next = resultOptics(result)
                                                val actual = resultZoom(result) * selected.option.base
                                                if (next != previous || actual != previousZoom) {
                                                    previous = next; previousZoom = actual
                                                    main.post { if (!closed.get()) {
                                                        lens = next; ready = true; transform(); onZoomActual(actual)
                                                    } }
                                                }
                                            }
                                        }
                                        submitPreview = {
                                            applyZoom(builder)
                                            val request = builder.build()
                                            previewRequest = request
                                            configured.setRepeatingRequest(request, callback, handler)
                                        }
                                        submitPreview!!.invoke()
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
                        if (closed.get()) worker.quitSafely()
                        else if (error !in listOf(CameraDevice.StateCallback.ERROR_CAMERA_IN_USE, CameraDevice.StateCallback.ERROR_MAX_CAMERAS_IN_USE) || !retryBusyCamera(texture)) fail()
                    }
                }, handler)
            } catch (failure: Exception) {
                openInFlight.set(false)
                if (failure !is CameraAccessException || failure.reason !in listOf(CameraAccessException.CAMERA_IN_USE,
                    CameraAccessException.MAX_CAMERAS_IN_USE) || !retryBusyCamera(texture)) fail(failure)
            }
        }
    }
    /** A lens handoff may briefly overlap HAL close/open; retry only that bounded busy state. */
    private fun retryBusyCamera(texture: SurfaceTexture): Boolean {
        if (closed.get() || openAttempts >= 3) return false
        photoReader?.close(); photoReader = null
        surface?.release(); surface = null
        main.postDelayed({ if (!closed.get()) { opening = false; start(texture) } }, 200L)
        return true
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
                        if (Build.VERSION.SDK_INT >= 30) previous[CaptureRequest.CONTROL_ZOOM_RATIO]?.let {
                            set(CaptureRequest.CONTROL_ZOOM_RATIO, it) }
                        if (Build.VERSION.SDK_INT >= 36) previous[CaptureRequest.CONTROL_ZOOM_METHOD]?.let {
                            set(CaptureRequest.CONTROL_ZOOM_METHOD, it) }
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
                            pending.lens = requireNotNull(captureLens)(result)
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
            submitPreview = null
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
