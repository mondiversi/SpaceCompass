package me.mondiversi.spacecompass

import java.io.File

internal interface SpaceCompassCameraCapture {
    suspend fun capturePhoto(): SpaceCompassCameraPhotoFrame
}

/** One JPEG plus metadata of that exposure, delivered only after an explicit shutter tap. */
internal data class SpaceCompassCameraPhotoFrame(val jpeg: ByteArray, val lens: SpaceCompassCameraLens,
    val displayRotation: Int, val capturedTimeMs: Long, val attitude: SpaceCompassCameraAttitude?)

internal data class SpaceCompassCameraPhotoSnapshot(val source: File, val lens: SpaceCompassCameraLens,
    val displayRotation: Int, val attitude: SpaceCompassCameraAttitude?, val warning: String? = null)
