package me.mondiversi.spacecompass

import java.io.File
import java.io.IOException

/** Legacy galleries need unique names when different profiles of the same capture are saved. */
internal fun createUniqueSpaceCompassPanoramaFile(directory: File, name: String): File {
    require(name == File(name).name && name.endsWith(".jpg"))
    val stem = name.removeSuffix(".jpg")
    for (index in 0..999) {
        val target = File(directory, if (index == 0) name else "$stem ($index).jpg")
        if (target.createNewFile()) return target
    }
    throw IOException("No unused image filename")
}
