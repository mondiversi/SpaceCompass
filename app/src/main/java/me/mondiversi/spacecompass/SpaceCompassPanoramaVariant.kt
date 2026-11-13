package me.mondiversi.spacecompass

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import java.io.File
import java.util.UUID

/** Cache by profile, disclosure and labels so Save/Share cannot reuse a different label or disclosure variant. */
internal fun prepareSpaceCompassPanoramaVariant(context: Context, data: SpaceCompassPanoramaPreviewData,
    position: SpaceCompassPanoramaPosition, labelMode: SpaceCompassPanoramaLabelMode = data.snapshot?.labelMode ?: SpaceCompassPanoramaLabelMode.DEFAULT,
    exportMode: SpaceCompassPanoramaExportMode = SpaceCompassPanoramaExportMode.INTERNATIONAL,
    center: SpaceCompassPanoramaCenter = data.snapshot?.center ?: SpaceCompassPanoramaCenter.SOUTH,
    projection: SpaceCompassCaptureProjection = data.projection,
    showGrid: Boolean = data.snapshot?.showGrid ?: SPACE_COMPASS_MAIN_GRID_DEFAULT,
    showReferences: Boolean = data.snapshot?.showSkyReferences ?: SPACE_COMPASS_SKY_REFERENCES_DEFAULT,
    showGridNumbers: Boolean = data.snapshot?.showGridNumbers ?: SPACE_COMPASS_GRID_NUMBERS_DEFAULT): File {
    val overlays = SpaceCompassCaptureOverlays(showGrid, showReferences, showGridNumbers)
    if (overlays == SpaceCompassCaptureOverlays.fromSnapshot(data.snapshot) && projection == data.projection && exportMode == data.fileExportMode && position == data.position &&
        labelMode == (data.snapshot?.labelMode ?: SpaceCompassPanoramaLabelMode.DEFAULT) && center == (data.snapshot?.center ?: SpaceCompassPanoramaCenter.SOUTH)) return data.file
    val selected = if (exportMode == SpaceCompassPanoramaExportMode.SELECTED) requireNotNull(data.selectedPresentation) else null
    val snapshot = selected?.snapshot ?: requireNotNull(data.snapshot)
    val caption = selected?.caption ?: requireNotNull(data.captionData)
    val directory = File(data.file.parentFile, spaceCompassPanoramaVariantKey(position, labelMode, exportMode, center)+"-${projection.key}-${overlays.cacheKey}")
    check(directory.isDirectory || directory.mkdirs())
    val target = File(directory, data.file.name)
    if (target.isFile && target.length() > 0L) return target
    val temporary = File(directory, UUID.randomUUID().toString() + ".jpg")
    val size = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    BitmapFactory.decodeFile(data.file.absolutePath, size)
    require(size.outWidth in 128..4096)
    try {
        val variant = overlays.applyTo(spaceCompassSnapshotForDisclosure(snapshot,position)).copy(caption = spaceCompassPanoramaCaptionForPosition(caption, position), labelMode = labelMode, center = center)
        val bitmap = data.cameraPhoto?.let { renderSpaceCompassCameraPhoto(context, if (selected != null) it.copy(warning = selected.cameraWarning) else it, variant) }
            ?: renderSpaceCompassCaptureView(context, variant, data.renderSize, projection)
        try { temporary.outputStream().use { check(bitmap.compress(Bitmap.CompressFormat.JPEG, 95, it)) } }
        finally { bitmap.recycle() }
        // Fresh JPEG encoding carries no GPS EXIF from another variant; only explicit capture/credit tags are added.
        stampSpaceCompassPanoramaFile(temporary, data.timeMs, data.credits)
        check(temporary.renameTo(target))
        return target
    } finally { temporary.delete() }
}
