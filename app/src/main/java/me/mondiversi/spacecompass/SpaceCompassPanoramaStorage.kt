package me.mondiversi.spacecompass

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.media.MediaScannerConnection
import android.media.ExifInterface
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import java.io.File
import java.io.IOException
import java.time.Instant
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

internal fun spaceCompassPanoramaFileName(timeMs: Long): String = "SpaceCompass_" +
    DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss_SSS").withZone(ZoneOffset.UTC).format(Instant.ofEpochMilli(timeMs)) + ".jpg"

private fun stampSpaceCompassPanorama(exif: ExifInterface, timeMs: Long) {
    val instant = Instant.ofEpochMilli(timeMs)
    val zone = if (Build.VERSION.SDK_INT >= 29) ZoneOffset.UTC else java.time.ZoneId.systemDefault()
    val date = DateTimeFormatter.ofPattern("yyyy:MM:dd HH:mm:ss").withZone(zone).format(instant)
    val fraction = DateTimeFormatter.ofPattern("SSS").withZone(zone).format(instant)
    exif.setAttribute(ExifInterface.TAG_DATETIME, date)
    exif.setAttribute(ExifInterface.TAG_DATETIME_ORIGINAL, date)
    exif.setAttribute(ExifInterface.TAG_DATETIME_DIGITIZED, date)
    exif.setAttribute(ExifInterface.TAG_SUBSEC_TIME, fraction)
    exif.setAttribute(ExifInterface.TAG_SUBSEC_TIME_ORIGINAL, fraction)
    if (Build.VERSION.SDK_INT >= 29) {
        exif.setAttribute(ExifInterface.TAG_OFFSET_TIME, "+00:00")
        exif.setAttribute(ExifInterface.TAG_OFFSET_TIME_ORIGINAL, "+00:00")
        exif.setAttribute(ExifInterface.TAG_OFFSET_TIME_DIGITIZED, "+00:00")
    }
    exif.setAttribute(ExifInterface.TAG_SOFTWARE, "Space Compass ${BuildConfig.VERSION_NAME}")
    exif.saveAttributes()
}

/** Own gallery images need no media-read permission. Partial writes are rolled back before reporting failure. */
internal fun saveSpaceCompassPanorama(context: Context, bitmap: Bitmap, timeMs: Long): Uri {
    val name = spaceCompassPanoramaFileName(timeMs)
    if (Build.VERSION.SDK_INT >= 29) {
        val resolver = context.contentResolver
        val values = ContentValues().apply {
            put(MediaStore.Images.Media.DISPLAY_NAME, name)
            put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
            put(MediaStore.Images.Media.DATE_TAKEN, timeMs)
            put(MediaStore.Images.Media.WIDTH, bitmap.width)
            put(MediaStore.Images.Media.HEIGHT, bitmap.height)
            put(MediaStore.Images.Media.RELATIVE_PATH, "${Environment.DIRECTORY_DCIM}/SpaceCompass")
            put(MediaStore.Images.Media.IS_PENDING, 1)
        }
        val uri = resolver.insert(MediaStore.Images.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY), values)
            ?: throw IOException("Image insertion failed")
        try {
            resolver.openOutputStream(uri, "w")?.use {
                if (!bitmap.compress(Bitmap.CompressFormat.JPEG, 95, it)) throw IOException("JPEG encoding failed")
            } ?: throw IOException("Image stream unavailable")
            resolver.openFileDescriptor(uri, "rw")?.use { stampSpaceCompassPanorama(ExifInterface(it.fileDescriptor), timeMs) }
                ?: throw IOException("Image metadata stream unavailable")
            if (resolver.update(uri, ContentValues().apply { put(MediaStore.Images.Media.IS_PENDING, 0) }, null, null) != 1)
                throw IOException("Image publication failed")
            return uri
        } catch (error: Throwable) {
            runCatching { resolver.delete(uri, null, null) }
            throw error
        }
    }
    @Suppress("DEPRECATION")
    val directory = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM), "SpaceCompass")
    if (!directory.isDirectory && !directory.mkdirs()) throw IOException("Image folder unavailable")
    val file = File(directory, name)
    // Never replace an existing photograph, including another capture in the same millisecond.
    if (!file.createNewFile()) throw IOException("Image already exists")
    try {
        file.outputStream().use {
            if (!bitmap.compress(Bitmap.CompressFormat.JPEG, 95, it)) throw IOException("JPEG encoding failed")
        }
        stampSpaceCompassPanorama(ExifInterface(file.absolutePath), timeMs)
        MediaScannerConnection.scanFile(context, arrayOf(file.absolutePath), arrayOf("image/jpeg"), null)
        return Uri.fromFile(file)
    } catch (error: Throwable) {
        file.delete()
        throw error
    }
}
