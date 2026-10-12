package me.mondiversi.spacecompass

import android.content.Context
import java.io.File
import java.io.OutputStream
import java.util.UUID

internal fun spaceCompassPageExportDirectory(context: Context, key: String, mode: SpaceCompassPanoramaExportMode,
    position: SpaceCompassPanoramaPosition? = null): File {
    require(key.matches(Regex("[a-zA-Z0-9_-]+")))
    val root=File(context.cacheDir,"panoramas")
    check(root.isDirectory || root.mkdirs())
    root.listFiles()?.filter { it.isDirectory && System.currentTimeMillis()-it.lastModified()>86_400_000L }
        ?.forEach { if (it.canonicalFile.parentFile==root.canonicalFile) it.deleteRecursively() }
    val directory=File(root,"$key/${mode.key}"+position?.let { "/${it.key}" }.orEmpty())
    check(directory.isDirectory || directory.mkdirs())
    return directory
}

internal fun writeSpaceCompassPageExportFile(directory: File, name: String,
    writer: (OutputStream) -> Unit): File {
    require(File(name).name==name)
    val target=File(directory,name)
    if (target.isFile && target.length()>0) return target
    val temporary=File(directory,"$name.${UUID.randomUUID()}.partial")
    try {
        temporary.outputStream().use(writer)
        check(temporary.length()>0 && temporary.renameTo(target))
    } finally { temporary.delete() }
    return target
}
