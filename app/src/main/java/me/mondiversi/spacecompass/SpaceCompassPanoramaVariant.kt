package me.mondiversi.spacecompass

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import java.io.File
import java.util.UUID

/** Cache by profile, disclosure and labels so Save/Share cannot reuse a different label or disclosure variant. */
internal fun prepareSpaceCompassPanoramaVariant(context: Context, data: SpaceCompassPanoramaPreviewData,
    position: SpaceCompassPanoramaPosition, showPointLabels: Boolean = data.snapshot?.showPointLabels ?: true,
    exportMode: SpaceCompassPanoramaExportMode = SpaceCompassPanoramaExportMode.INTERNATIONAL,
    center: SpaceCompassPanoramaCenter = data.snapshot?.center ?: SpaceCompassPanoramaCenter.SOUTH): File {
    if (exportMode == SpaceCompassPanoramaExportMode.INTERNATIONAL && position == data.position &&
        showPointLabels == (data.snapshot?.showPointLabels ?: true) && center == (data.snapshot?.center ?: SpaceCompassPanoramaCenter.SOUTH)) return data.file
    val selected = if (exportMode == SpaceCompassPanoramaExportMode.SELECTED) requireNotNull(data.selectedPresentation) else null
    val snapshot = selected?.snapshot ?: requireNotNull(data.snapshot)
    val caption = selected?.caption ?: requireNotNull(data.captionData)
    val directory = File(data.file.parentFile, spaceCompassPanoramaVariantKey(position, showPointLabels, exportMode, center))
    check(directory.isDirectory || directory.mkdirs())
    val target = File(directory, data.file.name)
    if (target.isFile && target.length() > 0L) return target
    val temporary = File(directory, UUID.randomUUID().toString() + ".jpg")
    val size = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    BitmapFactory.decodeFile(data.file.absolutePath, size)
    require(size.outWidth in 128..4096)
    try {
        val variant = snapshot.copy(caption = spaceCompassPanoramaCaptionForPosition(caption, position), showPointLabels = showPointLabels, center = center)
        val bitmap = data.cameraPhoto?.let { renderSpaceCompassCameraPhoto(context, if (selected != null) it.copy(warning = selected.cameraWarning) else it, variant) }
            ?: renderSpaceCompassPanorama(variant, size.outWidth, context)
        try { temporary.outputStream().use { check(bitmap.compress(Bitmap.CompressFormat.JPEG, 95, it)) } }
        finally { bitmap.recycle() }
        // Fresh JPEG encoding carries no GPS EXIF from another variant; only explicit capture/credit tags are added.
        stampSpaceCompassPanoramaFile(temporary, data.timeMs, data.credits)
        check(temporary.renameTo(target))
        return target
    } finally { temporary.delete() }
}
