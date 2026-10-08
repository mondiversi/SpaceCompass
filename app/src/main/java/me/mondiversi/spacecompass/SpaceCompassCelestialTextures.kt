package me.mondiversi.spacecompass

import android.content.Context
import android.graphics.BitmapFactory
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.sync.withPermit
import java.io.File
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.URI
import java.util.concurrent.ConcurrentHashMap
import kotlin.coroutines.coroutineContext

/** The UI observes availability; HTTP always runs away from UI and GL threads. */
internal object SpaceCompassCelestialTextures {
    val ready = MutableStateFlow<Set<String>>(emptySet())
    private val stores = ConcurrentHashMap<String, SpaceCompassCelestialTextureFiles>()
    private val locks = ConcurrentHashMap<String, Mutex>()
    private val retryAfter = ConcurrentHashMap<String, Long>()
    private val downloads = Semaphore(2)

    private fun store(context: Context): SpaceCompassCelestialTextureFiles {
        val directory = File(context.noBackupFilesDir, SpaceCompassCelestialTexturePolicy.PACK)
        return stores.computeIfAbsent(directory.absolutePath) { SpaceCompassCelestialTextureFiles(directory) }
    }

    fun open(context: Context, body: SpaceCompassCelestialBody): InputStream? {
        val name = body.viewerTexture ?: return null
        if (body == SpaceCompassCelestialBody.LV_426) return context.assets.open("celestial/$name")
        return openImage(context, name)
    }

    fun openImage(context: Context, name: String?): InputStream? {
        val texture = SpaceCompassCelestialTexturePolicy.texture(name) ?: return null
        return store(context).cached(texture)?.inputStream()
    }

    suspend fun ensure(context: Context, body: SpaceCompassCelestialBody): Boolean = ensureImage(context, body.viewerTexture)

    suspend fun ensureImage(context: Context, name: String?): Boolean = withContext(Dispatchers.IO) {
        val texture = SpaceCompassCelestialTexturePolicy.texture(name) ?: return@withContext true
        locks.computeIfAbsent(texture.name) { Mutex() }.withLock {
            val files = store(context)
            if (files.cached(texture) != null) {
                ready.update { it + texture.name }
                android.util.Log.d("SpaceCompassTextures", "${texture.name}: reused verified offline map")
                return@withLock true
            }
            val imported = runCatching {
                files.importVerified(texture, SpaceCompassCelestialTexturePolicy.legacyPacks.map {
                    File(context.noBackupFilesDir, it)
                })
            }.getOrNull()
            if (imported != null) {
                ready.update { it + texture.name }
                android.util.Log.i("SpaceCompassTextures", "${texture.name}: reused verified previous pack")
                return@withLock true
            }
            val now = android.os.SystemClock.elapsedRealtime()
            if (now < (retryAfter[texture.name] ?: 0)) return@withLock false
            try {
                downloads.withPermit {
                    val bytes = fetch(texture)
                    coroutineContext.ensureActive()
                    val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                    BitmapFactory.decodeByteArray(bytes, 0, bytes.size, bounds)
                    require(bounds.outWidth == texture.width && bounds.outHeight == texture.height) { "Texture dimensions differ" }
                    files.store(texture, bytes)
                    android.util.Log.i("SpaceCompassTextures", "${texture.name}: stored verified map (${texture.bytes} bytes)")
                }
                retryAfter.remove(texture.name)
                ready.update { it + texture.name }
                true
            } catch (cancelled: CancellationException) { throw cancelled
            } catch (error: Exception) {
                retryAfter[texture.name] = android.os.SystemClock.elapsedRealtime() + 30_000
                SpaceCompassErrorLog.record(context, "celestial:texture:${texture.name}", error)
                false
            }
        }
    }

    private suspend fun fetch(texture: SpaceCompassCelestialTexture): ByteArray {
        var url = texture.url
        repeat(4) { redirect ->
            coroutineContext.ensureActive()
            require(SpaceCompassCelestialTexturePolicy.allowedUrl(url, redirect > 0))
            val connection = URI(url).toURL().openConnection() as HttpURLConnection
            try {
                connection.instanceFollowRedirects = false
                connection.connectTimeout = 10_000
                connection.readTimeout = 10_000
                connection.setRequestProperty("User-Agent", "SpaceCompass/${BuildConfig.VERSION_NAME}")
                when (connection.responseCode) {
                    in listOf(301, 302, 303, 307, 308) -> {
                        val location = requireNotNull(connection.getHeaderField("Location"))
                        url = URI(url).resolve(location).toString()
                        require(SpaceCompassCelestialTexturePolicy.allowedUrl(url, redirect = true))
                    }
                    200 -> {
                        require(connection.contentLengthLong == -1L || connection.contentLengthLong == texture.bytes.toLong())
                        val job = coroutineContext[Job]
                        return connection.inputStream.use { input ->
                            SpaceCompassCelestialTexturePolicy.read(texture, input) { job?.ensureActive() }
                        }
                    }
                    else -> error("Texture request failed: HTTP ${connection.responseCode}")
                }
            } finally { connection.disconnect() }
        }
        error("Too many texture redirects")
    }
}

@Composable
internal fun rememberSpaceCompassCelestialTexture(body: SpaceCompassCelestialBody): Boolean =
    rememberSpaceCompassCelestialImage(body.viewerTexture)

@Composable
internal fun rememberSpaceCompassCelestialImage(name: String?): Boolean {
    val context = LocalContext.current.applicationContext
    val texture = SpaceCompassCelestialTexturePolicy.texture(name) ?: return true
    val ready by SpaceCompassCelestialTextures.ready.collectAsState()
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    var resumed by remember(lifecycle) { mutableStateOf(lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)) }
    DisposableEffect(lifecycle) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) resumed = true
            if (event == Lifecycle.Event.ON_PAUSE || event == Lifecycle.Event.ON_STOP) resumed = false
        }
        lifecycle.addObserver(observer)
        onDispose { lifecycle.removeObserver(observer) }
    }
    LaunchedEffect(context, texture.name, resumed) {
        if (!resumed) return@LaunchedEffect
        while (!SpaceCompassCelestialTextures.ensureImage(context, name)) delay(30_000)
    }
    return texture.name in ready
}

internal suspend fun ensureSpaceCompassCelestialTextures(context: Context, bodies: Set<SpaceCompassCelestialBody>) = coroutineScope {
    bodies.map { body -> async { SpaceCompassCelestialTextures.ensure(context, body) } }.awaitAll()
}

internal fun spaceCompassCelestialPlaceholderColor(body: SpaceCompassCelestialBody): Int = when (body) {
    SpaceCompassCelestialBody.SUN -> 0xffffc45c.toInt()
    SpaceCompassCelestialBody.MOON, SpaceCompassCelestialBody.MERCURY -> 0xff9b9b9b.toInt()
    SpaceCompassCelestialBody.VENUS, SpaceCompassCelestialBody.SATURN -> 0xffcab082.toInt()
    SpaceCompassCelestialBody.JUPITER -> 0xffb79578.toInt()
    SpaceCompassCelestialBody.URANUS -> 0xff91c6d5.toInt()
    SpaceCompassCelestialBody.NEPTUNE -> 0xff467bc0.toInt()
    SpaceCompassCelestialBody.POLARIS -> 0xfffff3d6.toInt()
    SpaceCompassCelestialBody.SIRIUS -> 0xffd2e9ff.toInt()
    SpaceCompassCelestialBody.BETELGEUSE, SpaceCompassCelestialBody.TITAN -> 0xffd99865.toInt()
    SpaceCompassCelestialBody.ORION_NEBULA -> 0xffb887a2.toInt()
    SpaceCompassCelestialBody.PLEIADES -> 0xff89b9d8.toInt()
    else -> 0xff9c4f3a.toInt()
}
