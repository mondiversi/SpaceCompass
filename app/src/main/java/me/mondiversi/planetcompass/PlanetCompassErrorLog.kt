package me.mondiversi.planetcompass

import android.content.Context
import android.os.Build
import java.io.File
import java.io.PrintWriter
import java.io.StringWriter
import java.time.Instant

/** App-private, bounded diagnostics. Never shared with the UVIR sensor archive. */
object PlanetCompassErrorLog {
    private val lock = Any()
    @Volatile private var installed = false
    private const val MAX_BYTES = 256 * 1024
    fun install(context: Context) {
        synchronized(lock) {
            if (installed) return
            val application = context.applicationContext
            val previous = Thread.getDefaultUncaughtExceptionHandler()
            Thread.setDefaultUncaughtExceptionHandler { thread, error ->
                record(application, "uncaught:${thread.name}", error)
                previous?.uncaughtException(thread, error)
            }
            installed = true
        }
    }
    fun record(context: Context, source: String, error: Throwable) {
        val trace = StringWriter().also { error.printStackTrace(PrintWriter(it)) }.toString()
        record(context, source, trace)
    }
    fun record(context: Context, source: String, message: String) {
        runCatching {
            synchronized(lock) {
                val file = File(context.applicationContext.filesDir, "planet_compass_error_log.txt")
                val entry = "${Instant.now()} | $source | Android ${Build.VERSION.SDK_INT} | ${Build.MANUFACTURER} ${Build.MODEL}\n${message.take(32_768)}\n\n"
                val existing = if (file.isFile && file.length() <= MAX_BYTES) file.readText() else ""
                // UTF-8 can occupy up to four bytes per character: cap characters conservatively.
                file.writeText((existing.takeLast(MAX_BYTES / 8) + entry).takeLast(MAX_BYTES / 4))
            }
        }
    }
}
