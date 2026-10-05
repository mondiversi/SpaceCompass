package me.mondiversi.spacecompass

import android.content.Context
import android.util.AtomicFile
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.*
import java.io.ByteArrayOutputStream
import java.io.File
import java.net.HttpURLConnection
import java.net.URI
import kotlin.coroutines.coroutineContext

internal data class SpaceCompassCelestialRemoteData(val iss: SpaceCompassIssOrbit? = null,
    val ephemerides: Map<SpaceCompassCelestialBody, SpaceCompassHorizonsEphemeris> = emptyMap(),
    val motions: Map<SpaceCompassCelestialBody, SpaceCompassHorizonsMotion> = emptyMap(),
    val loading: Boolean = false,
    val loadingMotionFor: SpaceCompassCelestialBody? = null,
    val starlink: SpaceCompassStarlinkOrbit? = null,
    val timedOutBodies: Set<SpaceCompassCelestialBody> = emptySet())

internal suspend fun fetchSpaceCompassCelestialText(url: String, maximumBytes: Int = 65_536): String = withContext(Dispatchers.IO) {
    require(maximumBytes in 1..SPACE_COMPASS_SATCAT_MAX_BYTES)
    val connection = URI(url).toURL().openConnection() as HttpURLConnection
    try {
        connection.connectTimeout = 10_000; connection.readTimeout = 10_000
        connection.instanceFollowRedirects = false
        connection.setRequestProperty("User-Agent", "SpaceCompass/${BuildConfig.VERSION_NAME}")
        val status = connection.responseCode
        if (status != 200) throw SpaceCompassCelestialHttpException(status)
        require(connection.contentLengthLong <= maximumBytes)
        connection.inputStream.use { input ->
            val output = ByteArrayOutputStream(); val buffer = ByteArray(4096)
            while (true) {
                coroutineContext.ensureActive()
                val n = input.read(buffer); if (n < 0) break
                require(output.size() + n <= maximumBytes)
                output.write(buffer, 0, n)
            }
            output.toString("UTF-8")
        }
    } finally { connection.disconnect() }
}
private fun orbitalCache(context: Context, body: SpaceCompassCelestialBody, motion: Boolean = false): AtomicFile {
    require(!motion || body.usesHorizons)
    val name = if (motion) "celestial-${body.name.lowercase(java.util.Locale.ROOT)}-heliocentric-motion-v2.txt" else when (body) {
        SpaceCompassCelestialBody.ISS -> "celestial-iss-v1.tle"
        SpaceCompassCelestialBody.STARLINK_V3 -> "celestial-starlink-40083-v1.csv"
        SpaceCompassCelestialBody.SEDNA -> "celestial-sedna-v1.txt"
        SpaceCompassCelestialBody.VOYAGER_1 -> "celestial-voyager-1-v1.txt"
        SpaceCompassCelestialBody.VOYAGER_2 -> "celestial-voyager-2-v1.txt"
        else -> throw IllegalArgumentException("No public ephemeris cache for $body")
    }
    return AtomicFile(File(context.cacheDir, name))
}
private fun readOrbitalCache(context: Context, body: SpaceCompassCelestialBody, motion: Boolean = false) =
    runCatching { orbitalCache(context, body, motion).readFully().also { require(it.size <= 65_536) }.toString(Charsets.UTF_8) }.getOrNull()
private fun storeOrbitalCache(context: Context, body: SpaceCompassCelestialBody, text: String, motion: Boolean = false) {
    val cache = orbitalCache(context, body, motion); val stream = cache.startWrite()
    try { stream.write(text.toByteArray(Charsets.UTF_8)); cache.finishWrite(stream) }
    catch (error: Exception) { cache.failWrite(stream); throw error }
}

/** Foreground-only, independent two-hour caches; five-minute retry only after an actual failure. */
@Composable
internal fun rememberSpaceCompassCelestialRemote(body: SpaceCompassCelestialBody, resumed: Boolean): SpaceCompassCelestialRemoteData =
    rememberSpaceCompassCelestialRemote(setOf(body), resumed)

@Composable
internal fun rememberSpaceCompassCelestialRemote(bodies: Set<SpaceCompassCelestialBody>, resumed: Boolean,
    motionBodies: Set<SpaceCompassCelestialBody> = bodies): SpaceCompassCelestialRemoteData {
    val context = LocalContext.current
    var data by remember { mutableStateOf(SpaceCompassCelestialRemoteData()) }
    val policy = remember { SpaceCompassCelestialRefreshPolicy() }
    val starlinkSources = remember { SpaceCompassStarlinkSources() }
    val networkRevision = rememberSpaceCompassNetworkRevision(resumed) {
        policy.networkChanged()
    }
    // The catalogue can show already downloaded Sun distances before those objects are selected.
    // Read local caches once; never trigger a batch of extra network requests by opening the list.
    LaunchedEffect(context) {
        val cached = withContext(Dispatchers.IO) {
            val bodies = SpaceCompassCelestialBody.entries.filter { it.usesHorizons }
            SpaceCompassCelestialRemoteData(
                iss = readOrbitalCache(context, SpaceCompassCelestialBody.ISS)?.let { runCatching { SpaceCompassIssOrbit.parse(it) }.getOrNull() },
                starlink = readOrbitalCache(context, SpaceCompassCelestialBody.STARLINK_V3)?.let { runCatching { SpaceCompassStarlinkOrbit.parse(it) }.getOrNull() },
                ephemerides = bodies.mapNotNull { candidate -> readOrbitalCache(context,candidate)?.let { text ->
                    runCatching { candidate to parseSpaceCompassHorizonsEphemeris(candidate,text) }.getOrNull()
                } }.toMap(),
                motions = bodies.mapNotNull { candidate -> readOrbitalCache(context,candidate,motion=true)?.let { text ->
                    runCatching { candidate to parseSpaceCompassHorizonsMotion(candidate,text) }.getOrNull()
                } }.toMap())
        }
        // A selected body's foreground refresh may already have supplied newer data.
        data = data.copy(iss = data.iss ?: cached.iss, starlink = data.starlink ?: cached.starlink,
            ephemerides = cached.ephemerides + data.ephemerides, motions = cached.motions + data.motions)
    }
    LaunchedEffect(bodies, motionBodies, resumed, networkRevision) {
        if (!resumed) return@LaunchedEffect
        // One serial queue for all visible objects: no concurrent JPL requests or cache races.
        val remoteBodies = spaceCompassCelestialCatalogOrder.filter { it in bodies && (it.usesHorizons || it.isEarthSatellite) }
            .sortedBy { if (it in motionBodies) 0 else 1 }
        while (true) {
            val upcoming = mutableListOf<Long>()
            for (body in remoteBodies) {
                val iss = body == SpaceCompassCelestialBody.ISS
                val satellite = body.isEarthSatellite
                val now = System.currentTimeMillis()
                val pointingUsable = if (satellite) data.satelliteOrbit(body)?.usable(now) == true else data.ephemerides[body]?.at(now) != null
                val motionUsable = data.motions[body]?.speedAt(now) != null
                val position = SpaceCompassCelestialRequest(body)
                val motion = SpaceCompassCelestialRequest(body, motion = true)
                // Fetch missing velocity first when pointing is already present. Never parallelize JPL requests.
                val requests = if (satellite || body !in motionBodies) listOf(position) else if (pointingUsable && !motionUsable)
                    listOf(motion, position) else listOf(position, motion)
                val next = withContext(Dispatchers.IO) { requests.associateWith { request ->
                    policy.nextAttempt(request, if (request.motion) motionUsable else pointingUsable,
                        orbitalCache(context, body, request.motion).baseFile.lastModified())
                } }
                val pending = requests.filter { next.getValue(it) <= now }
                if (pending.isEmpty()) {
                    upcoming += next.values.min()
                    continue
                }
                try {
                    for (request in pending) {
                        data = data.copy(loading = true, loadingMotionFor =
                            if (request.motion || satellite || (!motionUsable && pending.any { it.motion })) body else null)
                        val result = policy.attempt(request, System::currentTimeMillis) {
                            val text = if (body == SpaceCompassCelestialBody.STARLINK_V3)
                                starlinkSources.load(System.currentTimeMillis(), ::fetchSpaceCompassCelestialText)
                            else fetchSpaceCompassCelestialText(when {
                                iss -> "https://celestrak.org/NORAD/elements/gp.php?CATNR=25544&FORMAT=TLE"
                                body == SpaceCompassCelestialBody.STARLINK_V3 -> SPACE_COMPASS_STARLINK_OMM_URL
                                else -> spaceCompassHorizonsUrl(body, System.currentTimeMillis(), geometricMotion = request.motion)
                            })
                            val updated = when {
                                iss -> {
                                    val orbit = SpaceCompassIssOrbit.parse(text)
                                    require(orbit.usable(System.currentTimeMillis())) { "ISS elements are too old" }
                                    data.copy(iss = orbit)
                                }
                                body == SpaceCompassCelestialBody.STARLINK_V3 -> {
                                    val orbit = SpaceCompassStarlinkOrbit.parse(text)
                                    require(orbit.usable(System.currentTimeMillis())) { "Starlink elements are too old" }
                                    data.copy(starlink = orbit)
                                }
                                request.motion -> {
                                    val velocity = parseSpaceCompassHorizonsMotion(body, text)
                                    require(velocity.speedAt(System.currentTimeMillis()) != null) { "Velocity data is out of date" }
                                    data.copy(motions = data.motions + (body to velocity))
                                }
                                else -> {
                                    val ephemeris = parseSpaceCompassHorizonsEphemeris(body, text)
                                    require(ephemeris.at(System.currentTimeMillis()) != null) { "Position data is out of date" }
                                    data.copy(ephemerides = data.ephemerides + (body to ephemeris))
                                }
                            }
                            withContext(Dispatchers.IO) { storeOrbitalCache(context, body, text, request.motion) }
                            data = updated.copy(timedOutBodies = updated.timedOutBodies - body)
                        }
                        result.exceptionOrNull()?.let { error ->
                            data = data.copy(timedOutBodies = if (error is java.net.SocketTimeoutException)
                                data.timedOutBodies + body else data.timedOutBodies - body)
                            // Public orbital requests contain no GPS, sensor credentials or device identifiers.
                            android.util.Log.w("SpaceCompassCelestial", "${body.name}: ${if (request.motion) "velocity" else "position"} refresh failed", error)
                            SpaceCompassErrorLog.record(context, if (request.motion) "celestial:velocity" else "celestial:ephemeris",
                                IllegalStateException("${body.name}: ${if (request.motion) "velocity" else "position"} refresh failed", error))
                        }
                        // Failure of one component must not block the other or discard valid cached data.
                    }
                } finally {
                    data = data.copy(loading = false, loadingMotionFor = null)
                }
                upcoming += System.currentTimeMillis() + 1_000L
            }
            val now = System.currentTimeMillis()
            delay(((upcoming.minOrNull() ?: (now + SPACE_COMPASS_CELESTIAL_REFRESH_MS)) - now).coerceIn(1L, SPACE_COMPASS_CELESTIAL_REFRESH_MS))
        }
    }
    return data
}
