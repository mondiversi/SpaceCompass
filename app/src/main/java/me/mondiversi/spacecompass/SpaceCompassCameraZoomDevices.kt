package me.mondiversi.spacecompass

import android.content.Context
import android.graphics.ImageFormat
import android.graphics.SurfaceTexture
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.os.Build
import kotlin.math.abs
import kotlin.math.atan

internal data class SpaceCompassCameraZoomDevice(val option: SpaceCompassCameraZoomOption,
    val characteristics: CameraCharacteristics, val nativeRatio: Boolean)

/** Only public rear cameras that can supply both preview and JPEG are offered. */
internal fun spaceCompassCameraZoomDevices(context: Context): List<SpaceCompassCameraZoomDevice> {
    val manager = context.getSystemService(CameraManager::class.java)
    val candidates = manager.cameraIdList.mapNotNull { id ->
        val c = manager.getCameraCharacteristics(id)
        val streams = c[CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP]
        val size = c[CameraCharacteristics.SENSOR_INFO_PHYSICAL_SIZE]
        val pixels = c[CameraCharacteristics.SENSOR_INFO_PIXEL_ARRAY_SIZE]
        val active = c[CameraCharacteristics.SENSOR_INFO_ACTIVE_ARRAY_SIZE]
        val focal = c[CameraCharacteristics.LENS_INFO_AVAILABLE_FOCAL_LENGTHS]?.firstOrNull()
        if (c[CameraCharacteristics.LENS_FACING] != CameraCharacteristics.LENS_FACING_BACK ||
            streams?.getOutputSizes(SurfaceTexture::class.java).isNullOrEmpty() ||
            streams?.getOutputSizes(ImageFormat.JPEG).isNullOrEmpty() || size == null ||
            pixels == null || active == null || focal == null || !focal.isFinite() || focal <= 0 ||
            size.width <= 0 || active.width() <= 0) null
        else Triple(id, c, focal.toDouble() * pixels.width / size.width / active.width())
    }
    val normal = candidates.minByOrNull { abs(Math.toDegrees(2 * atan(.5 / it.third)) - 65) }
        ?: error("No rear camera with calibrated preview and JPEG")
    // Keep the normal camera first so duplicate optical ranges do not select a vendor auxiliary ID.
    return (listOf(normal) + candidates.filter { it.first != normal.first }).map { (id, c, focal) ->
        val native = if (Build.VERSION.SDK_INT >= 30) c[CameraCharacteristics.CONTROL_ZOOM_RATIO_RANGE] else null
        val usable = native?.takeIf { it.lower.isFinite() && it.upper.isFinite() && it.lower > 0 && it.lower <= 1 && it.upper >= 1 }
        val maximum = c[CameraCharacteristics.SCALER_AVAILABLE_MAX_DIGITAL_ZOOM]?.takeIf { it.isFinite() && it >= 1 } ?: 1f
        SpaceCompassCameraZoomDevice(SpaceCompassCameraZoomOption(id, (focal / normal.third).toFloat(),
            usable?.lower ?: 1f, usable?.upper ?: maximum), c, usable != null)
    }
}
