package me.mondiversi.planetcompass

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

internal data class PlanetCompassCelestialRemoteData(val iss: PlanetCompassIssOrbit? = null,
    val ephemerides: Map<PlanetCompassCelestialBody, PlanetCompassHorizonsEphemeris> = emptyMap(),
    val motions: Map<PlanetCompassCelestialBody, PlanetCompassHorizonsMotion> = emptyMap(),
    val loading: Boolean = false,
    val loadingMotionFor: PlanetCompassCelestialBody? = null,
    val starlink: PlanetCompassStarlinkOrbit? = null)

internal suspend fun fetchPlanetCompassCelestialText(url: String): String = withContext(Dispatchers.IO) {
    val connection = URI(url).toURL().openConnection() as HttpURLConnection
    try {
        connection.connectTimeout = 10_000; connection.readTimeout = 10_000
        connection.instanceFollowRedirects = false
        connection.setRequestProperty("User-Agent", "PlanetCompass/${BuildConfig.VERSION_NAME}")
        val status = connection.responseCode
        if (status != 200) throw PlanetCompassCelestialHttpException(status)
        require(connection.contentLengthLong <= 65_536)
        connection.inputStream.use { input ->
            val output = ByteArrayOutputStream(); val buffer = ByteArray(4096)
            while (true) {
                coroutineContext.ensureActive()
                val n = input.read(buffer); if (n < 0) break
                require(output.size() + n <= 65_536)
                output.write(buffer, 0, n)
            }
            output.toString("UTF-8")
        }
    } finally { connection.disconnect() }
}
private fun orbitalCache(context: Context, body: PlanetCompassCelestialBody, motion: Boolean = false): AtomicFile {
    require(!motion || body.usesHorizons)
    val name = if (motion) "celestial-${body.name.lowercase(java.util.Locale.ROOT)}-heliocentric-motion-v2.txt" else when (body) {
        PlanetCompassCelestialBody.ISS -> "celestial-iss-v1.tle"
        PlanetCompassCelestialBody.STARLINK_V3 -> "celestial-starlink-40083-v1.csv"
        PlanetCompassCelestialBody.SEDNA -> "celestial-sedna-v1.txt"
        PlanetCompassCelestialBody.VOYAGER_1 -> "celestial-voyager-1-v1.txt"
        PlanetCompassCelestialBody.VOYAGER_2 -> "celestial-voyager-2-v1.txt"
        else -> throw IllegalArgumentException("No public ephemeris cache for $body")
    }
    return AtomicFile(File(context.cacheDir, name))
}
private fun readOrbitalCache(context: Context, body: PlanetCompassCelestialBody, motion: Boolean = false) =
    runCatching { orbitalCache(context, body, motion).readFully().also { require(it.size <= 65_536) }.toString(Charsets.UTF_8) }.getOrNull()
private fun storeOrbitalCache(context: Context, body: PlanetCompassCelestialBody, text: String, motion: Boolean = false) {
    val cache = orbitalCache(context, body, motion); val stream = cache.startWrite()
    try { stream.write(text.toByteArray(Charsets.UTF_8)); cache.finishWrite(stream) }
    catch (error: Exception) { cache.failWrite(stream); throw error }
}

/** Foreground-only, independent two-hour caches; five-minute retry only after an actual failure. */
@Composable
internal fun rememberPlanetCompassCelestialRemote(body: PlanetCompassCelestialBody, resumed: Boolean): PlanetCompassCelestialRemoteData =
    rememberPlanetCompassCelestialRemote(setOf(body), resumed)

@Composable
internal fun rememberPlanetCompassCelestialRemote(bodies: Set<PlanetCompassCelestialBody>, resumed: Boolean,
    motionBodies: Set<PlanetCompassCelestialBody> = bodies): PlanetCompassCelestialRemoteData {
    val context = LocalContext.current
    var data by remember { mutableStateOf(PlanetCompassCelestialRemoteData()) }
    val policy = remember { PlanetCompassCelestialRefreshPolicy() }
    // The catalogue can show already downloaded Sun distances before those objects are selected.
    // Read local caches once; never trigger a batch of extra network requests by opening the list.
    LaunchedEffect(context) {
        val cached = withContext(Dispatchers.IO) {
            val bodies = PlanetCompassCelestialBody.entries.filter { it.usesHorizons }
            PlanetCompassCelestialRemoteData(
                iss = readOrbitalCache(context, PlanetCompassCelestialBody.ISS)?.let { runCatching { PlanetCompassIssOrbit.parse(it) }.getOrNull() },
                starlink = readOrbitalCache(context, PlanetCompassCelestialBody.STARLINK_V3)?.let { runCatching { PlanetCompassStarlinkOrbit.parse(it) }.getOrNull() },
                ephemerides = bodies.mapNotNull { candidate -> readOrbitalCache(context,candidate)?.let { text ->
                    runCatching { candidate to parsePlanetCompassHorizonsEphemeris(candidate,text) }.getOrNull()
                } }.toMap(),
                motions = bodies.mapNotNull { candidate -> readOrbitalCache(context,candidate,motion=true)?.let { text ->
                    runCatching { candidate to parsePlanetCompassHorizonsMotion(candidate,text) }.getOrNull()
                } }.toMap())
        }
        // A selected body's foreground refresh may already have supplied newer data.
        data = data.copy(iss = data.iss ?: cached.iss, starlink = data.starlink ?: cached.starlink,
            ephemerides = cached.ephemerides + data.ephemerides, motions = cached.motions + data.motions)
    }
    LaunchedEffect(bodies, motionBodies, resumed) {
        if (!resumed) return@LaunchedEffect
        // One serial queue for all visible objects: no concurrent JPL requests or cache races.
        val remoteBodies = planetCompassCelestialCatalogOrder.filter { it in bodies && (it.usesHorizons || it.isEarthSatellite) }
            .sortedBy { if (it in motionBodies) 0 else 1 }
        while (true) {
            val upcoming = mutableListOf<Long>()
            for (body in remoteBodies) {
                val iss = body == PlanetCompassCelestialBody.ISS
                val satellite = body.isEarthSatellite
                val now = System.currentTimeMillis()
                val pointingUsable = if (satellite) data.satelliteOrbit(body)?.usable(now) == true else data.ephemerides[body]?.at(now) != null
                val motionUsable = data.motions[body]?.speedAt(now) != null
                val position = PlanetCompassCelestialRequest(body)
                val motion = PlanetCompassCelestialRequest(body, motion = true)
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
                            val text = fetchPlanetCompassCelestialText(when {
                                iss -> "https://celestrak.org/NORAD/elements/gp.php?CATNR=25544&FORMAT=TLE"
                                body == PlanetCompassCelestialBody.STARLINK_V3 -> PLANET_COMPASS_STARLINK_OMM_URL
                                else -> planetCompassHorizonsUrl(body, System.currentTimeMillis(), geometricMotion = request.motion)
                            })
                            val updated = when {
                                iss -> {
                                    val orbit = PlanetCompassIssOrbit.parse(text)
                                    require(orbit.usable(System.currentTimeMillis())) { "ISS elements are too old" }
                                    data.copy(iss = orbit)
                                }
                                body == PlanetCompassCelestialBody.STARLINK_V3 -> {
                                    val orbit = PlanetCompassStarlinkOrbit.parse(text)
                                    require(orbit.usable(System.currentTimeMillis())) { "Starlink elements are too old" }
                                    data.copy(starlink = orbit)
                                }
                                request.motion -> {
                                    val velocity = parsePlanetCompassHorizonsMotion(body, text)
                                    require(velocity.speedAt(System.currentTimeMillis()) != null) { "Velocity data is out of date" }
                                    data.copy(motions = data.motions + (body to velocity))
                                }
                                else -> {
                                    val ephemeris = parsePlanetCompassHorizonsEphemeris(body, text)
                                    require(ephemeris.at(System.currentTimeMillis()) != null) { "Position data is out of date" }
                                    data.copy(ephemerides = data.ephemerides + (body to ephemeris))
                                }
                            }
                            withContext(Dispatchers.IO) { storeOrbitalCache(context, body, text, request.motion) }
                            data = updated
                        }
                        result.exceptionOrNull()?.let { error ->
                            // Public orbital requests contain no GPS, sensor credentials or device identifiers.
                            android.util.Log.w("PlanetCompassCelestial", "${body.name}: ${if (request.motion) "velocity" else "position"} refresh failed", error)
                            PlanetCompassErrorLog.record(context, if (request.motion) "celestial:velocity" else "celestial:ephemeris",
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
            delay(((upcoming.minOrNull() ?: (now + PLANET_COMPASS_CELESTIAL_REFRESH_MS)) - now).coerceIn(1L, PLANET_COMPASS_CELESTIAL_REFRESH_MS))
        }
    }
    return data
}
