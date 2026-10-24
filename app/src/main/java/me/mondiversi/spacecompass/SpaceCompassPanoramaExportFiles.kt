package me.mondiversi.spacecompass

import android.content.ClipData
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.DocumentsContract
import androidx.core.content.FileProvider
import java.io.File
import java.util.UUID

internal enum class SpaceCompassCaptureExportType(val key: String, val extension: String, val mimeType: String,
    val labelResource: Int) {
    IMAGE("image","jpg","image/jpeg",R.string.capture_export_image),
    TXT("txt","txt","text/plain",R.string.capture_export_txt),
    CSV("csv","csv","text/csv",R.string.capture_export_csv),
    CURRENT_IMAGE("current-image","jpg","image/jpeg",R.string.data_export_current_image),
    ROTATION_IMAGE("rotation-image","jpg","image/jpeg",R.string.data_export_rotation_image);

    companion object {
        fun fromStored(values: Set<String>?): Set<SpaceCompassCaptureExportType> =
            if (values == null) emptySet() else entries.filter { it.key in values }.toSet()
    }
}

internal data class SpaceCompassPreparedCaptureExport(val files: List<File>) {
    init { require(files.isNotEmpty() && files.all { it.isFile && it.length() > 0 }) }
    val mimeType: String get() = files.map { file -> SpaceCompassCaptureExportType.entries.first {
        it.extension == file.extension }.mimeType }.distinct().singleOrNull() ?: "*/*"
}

/** Format only frozen data. Neither delivery nor profile selection fetches GPS, weather or orbital updates. */
internal fun prepareSpaceCompassCaptureExport(context: Context, data: SpaceCompassPanoramaPreviewData,
    types: Set<SpaceCompassCaptureExportType>, position: SpaceCompassPanoramaPosition,
    labels: SpaceCompassPanoramaLabelMode, mode: SpaceCompassPanoramaExportMode,
    center: SpaceCompassPanoramaCenter): SpaceCompassPreparedCaptureExport {
    require(types.isNotEmpty())
    val files = SpaceCompassCaptureExportType.entries.filter { it in types }.map { type ->
        if (type == SpaceCompassCaptureExportType.IMAGE) {
            prepareSpaceCompassPanoramaVariant(context,data,position,labels,mode,center)
        } else {
            val presentation = if (mode == SpaceCompassPanoramaExportMode.SELECTED) requireNotNull(data.selectedPresentation) else null
            val snapshot = spaceCompassSnapshotForDisclosure(presentation?.snapshot ?: requireNotNull(data.snapshot),position)
            val captionData = presentation?.caption ?: requireNotNull(data.captionData)
            val table = requireNotNull(data.orbitTable)
            require(table.bodies.isNotEmpty())
            val directory = File(data.file.parentFile,"tables-privacy-v2-${mode.key}-${position.key}")
            check(directory.isDirectory || directory.mkdirs())
            val target = File(directory,data.file.nameWithoutExtension + ".${type.extension}")
            if (!target.isFile || target.length() == 0L) {
                val caption = spaceCompassPanoramaCaptionForPosition(captionData,position)
                val text = if (type == SpaceCompassCaptureExportType.CSV) renderSpaceCompassOrbitCsv(table,snapshot,data.timeMs,caption)
                    else renderSpaceCompassOrbitTxt(table,snapshot,data.timeMs,caption)
                val temporary = File(directory,"${target.name}.${UUID.randomUUID()}.partial")
                try {
                    temporary.writeText(text,Charsets.UTF_8)
                    check(temporary.renameTo(target))
                } finally { temporary.delete() }
            }
            target
        }
    }
    return SpaceCompassPreparedCaptureExport(files)
}

internal fun shareSpaceCompassCaptureExport(context: Context, batch: SpaceCompassPreparedCaptureExport, title: String) {
    val uris = ArrayList(batch.files.map { FileProvider.getUriForFile(context,"${context.packageName}.fileprovider",it) })
    val intent = Intent(if (uris.size == 1) Intent.ACTION_SEND else Intent.ACTION_SEND_MULTIPLE).apply {
        type = batch.mimeType
        if (uris.size == 1) putExtra(Intent.EXTRA_STREAM,uris.first())
        else putParcelableArrayListExtra(Intent.EXTRA_STREAM,uris)
        clipData = ClipData.newUri(context.contentResolver,batch.files.first().name,uris.first()).apply {
            uris.drop(1).forEach { addItem(ClipData.Item(it)) }
        }
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    context.startActivity(Intent.createChooser(intent,title))
}

internal fun spaceCompassCaptureUniqueName(name: String, existing: Set<String>): String {
    fun exists(candidate: String) = existing.any { it.equals(candidate,ignoreCase=true) }
    if (!exists(name)) return name
    val extension = name.substringAfterLast('.',"").let { if (it.isBlank()) "" else ".$it" }
    val base = if (extension.isEmpty()) name else name.dropLast(extension.length)
    var index = 1
    while (exists("$base ($index)$extension")) index++
    return "$base ($index)$extension"
}

/** Create new documents only; a failed batch rolls back only the documents this request created. */
internal fun saveSpaceCompassCaptureExportToTree(context: Context, tree: Uri, files: List<File>): Int {
    require(files.isNotEmpty())
    val resolver = context.contentResolver
    val parent = DocumentsContract.buildDocumentUriUsingTree(tree,DocumentsContract.getTreeDocumentId(tree))
    val children = DocumentsContract.buildChildDocumentsUriUsingTree(tree,DocumentsContract.getTreeDocumentId(tree))
    val existing = mutableSetOf<String>()
    resolver.query(children,arrayOf(DocumentsContract.Document.COLUMN_DISPLAY_NAME),null,null,null)?.use { cursor ->
        while (cursor.moveToNext()) cursor.getString(0)?.let(existing::add)
    } ?: error("Destination listing unavailable")
    val created = mutableListOf<Uri>()
    try {
        files.forEach { file ->
            val format = SpaceCompassCaptureExportType.entries.first { it.extension == file.extension }
            val name = spaceCompassCaptureUniqueName(file.name,existing)
            val uri = requireNotNull(DocumentsContract.createDocument(resolver,parent,format.mimeType,name))
            created += uri
            resolver.openOutputStream(uri,"w")?.use { output -> file.inputStream().use { it.copyTo(output) } }
                ?: error("Destination output unavailable")
            existing += name
        }
        return created.size
    } catch (error: Exception) {
        created.forEach { uri -> runCatching { DocumentsContract.deleteDocument(resolver,uri) } }
        throw error
    }
}
