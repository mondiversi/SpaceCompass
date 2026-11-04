package me.mondiversi.spacecompass

import android.content.ClipData
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider
import java.io.ByteArrayOutputStream
import java.io.File
import java.util.UUID

internal fun readSpaceCompassScenario(context: Context, uri: Uri): SpaceCompassScenarioArchive =
    SpaceCompassScenarioArchive.decode(readSpaceCompassDocumentText(context, uri, SPACE_COMPASS_SCENARIO_FILE_LIMIT))

internal fun readSpaceCompassDocumentText(context: Context, uri: Uri, sizeLimit: Int): String {
    require(uri.scheme == "content")
    val bytes = context.contentResolver.openInputStream(uri)?.use { input ->
        val out = ByteArrayOutputStream()
        val buffer = ByteArray(4096)
        while (true) {
            val count = input.read(buffer)
            if (count < 0) break
            require(out.size() + count <= sizeLimit)
            out.write(buffer, 0, count)
        }
        out.toByteArray()
    } ?: error("Document unavailable")
    return bytes.toString(Charsets.UTF_8)
}

internal fun shareSpaceCompassScenario(context: Context, archive: SpaceCompassScenarioArchive, title: String,
    text: String = archive.encode()) = shareSpaceCompassTextDocument(context, "scenarios",
    spaceCompassScenarioFileName(archive.name), title, text, SPACE_COMPASS_SCENARIO_MIME)

internal fun shareSpaceCompassTextDocument(context: Context, cacheFolder: String, fileName: String, title: String,
    text: String, mimeType: String) {
    val directory = File(context.cacheDir, "$cacheFolder/${UUID.randomUUID()}")
    check(directory.mkdirs())
    val file = File(directory, fileName)
    file.writeText(text, Charsets.UTF_8)
    val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = mimeType
        putExtra(Intent.EXTRA_STREAM, uri)
        clipData = ClipData.newUri(context.contentResolver, file.name, uri)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    context.startActivity(Intent.createChooser(intent, title))
}
