package me.mondiversi.spacecompass

import java.io.File
import java.nio.file.AtomicMoveNotSupportedException
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import java.util.concurrent.ConcurrentHashMap

/** Persistent app-private maps; publish verified complete files and leave old maps intact on failure. */
internal class SpaceCompassCelestialTextureFiles(private val directory: File) {
    private data class Stamp(val bytes: Long, val modified: Long)
    private val verified = ConcurrentHashMap<String, Stamp>()

    fun cached(texture: SpaceCompassCelestialTexture): File? {
        val file = File(directory, texture.name)
        if (!file.isFile || file.length() != texture.bytes.toLong()) return null
        val stamp = Stamp(file.length(), file.lastModified())
        if (verified[texture.name] != stamp) {
            if (!runCatching { SpaceCompassCelestialTexturePolicy.matches(texture, file.readBytes()) }.getOrDefault(false)) return null
            verified[texture.name] = stamp
        }
        return file
    }

    fun store(texture: SpaceCompassCelestialTexture, bytes: ByteArray): File {
        require(SpaceCompassCelestialTexturePolicy.matches(texture, bytes))
        check(directory.isDirectory || directory.mkdirs())
        val target = File(directory, texture.name)
        val temporary = File.createTempFile("texture-", ".download", directory)
        try {
            temporary.outputStream().use { it.write(bytes) }
            try { Files.move(temporary.toPath(), target.toPath(), StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING) }
            catch (_: AtomicMoveNotSupportedException) { Files.move(temporary.toPath(), target.toPath(), StandardCopyOption.REPLACE_EXISTING) }
            verified[texture.name] = Stamp(target.length(), target.lastModified())
            return target
        } finally { temporary.delete() }
    }
}
