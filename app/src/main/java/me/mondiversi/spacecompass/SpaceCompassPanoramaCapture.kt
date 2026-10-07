package me.mondiversi.spacecompass

import android.app.ActivityManager
import android.os.SystemClock
import android.content.Context
import android.content.res.Configuration
import android.graphics.Bitmap
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.util.TimeZone
import java.util.UUID

internal data class SpaceCompassPanoramaAction(val busy: Boolean, val capture: () -> Unit,
    val preview: SpaceCompassPanoramaPreviewData? = null, val closePreview: () -> Unit = {})

@Composable
internal fun rememberSpaceCompassPanoramaAction(timeMs: Long, latitude: Double?, longitude: Double?, altitude: Double?,
    phase: SpaceCompassSunSkyPhase, weather: SpaceCompassSunWeatherSnapshot?,
    bodies: Set<SpaceCompassCelestialBody>, overlays: Map<SpaceCompassCelestialBody, SpaceCompassCelestialOverlay>,
    remote: SpaceCompassCelestialRemoteData, gpsAccuracyMeters: Double? = null,
    cameraEnabled: Boolean = false, cameraCapture: SpaceCompassCameraCapture? = null,
    showSkyReferences: Boolean = SPACE_COMPASS_SKY_REFERENCES_DEFAULT): SpaceCompassPanoramaAction {
    val context = LocalContext.current
    val resources = LocalResources.current
    val application = context.applicationContext
    val configuration = LocalConfiguration.current
    val exportResources = remember(context, configuration) {
        context.createConfigurationContext(Configuration(configuration).apply {
            setLocale(spaceCompassPanoramaExportLocale)
            setLayoutDirection(spaceCompassPanoramaExportLocale)
        }).resources
    }
    val observationZone = spaceCompassObservationZone()
    val simulated = LocalSpaceCompassObserver.current?.plan != null
    val simulatedTime = LocalSpaceCompassObserver.current?.plan?.simulateTime == true
    val simulatedAltitude = LocalSpaceCompassObserver.current?.plan?.let { it.simulatePosition || it.simulateAltitude } == true
    val preferences = LocalSpaceCompassPreferences.current
    val units = LocalSpaceCompassUnits.current
    val selectedFormatting = SpaceCompassPanoramaFormatting(configuration.locales[0],
        LocalSpaceCompassNumericFormat.current, LocalSpaceCompassDateFormat.current, LocalSpaceCompassTimeFormat.current,
        units.feet, units.dms, LocalSpaceCompassDeviceLocale.current)
    val scope = rememberCoroutineScope()
    val placeCache = remember { SpaceCompassPlaceCache() }
    var busy by remember { mutableStateOf(false) }
    var preview by remember { mutableStateOf<SpaceCompassPanoramaPreviewData?>(null) }
    fun notify(resource: Int) = showSpaceCompassBottomMessage(application, resources.getString(resource), longDuration = true)
    return SpaceCompassPanoramaAction(busy, capture = {
        if (!busy) {
            if (cameraEnabled && cameraCapture == null) notify(R.string.camera_unavailable)
            else if (!cameraEnabled && (latitude == null || longitude == null || bodies.isEmpty())) notify(R.string.panorama_not_ready)
            else {
                val position = SpaceCompassPanoramaPosition.fromStored(preferences?.getString(SPACE_COMPASS_PANORAMA_POSITION_KEY, null))
                val showPointLabels = preferences?.getBoolean(SPACE_COMPASS_PANORAMA_POINT_LABELS_KEY, true) ?: true
                val center = SpaceCompassPanoramaCenter.fromStored(
                    preferences?.getString(SPACE_COMPASS_PANORAMA_CENTER_KEY, null), latitude)
                val zone = if (simulated) TimeZone.getTimeZone(observationZone) else TimeZone.getDefault()
                val capturedTime = if (simulated) System.currentTimeMillis() else timeMs
                val objects = spaceCompassAllCelestialOrder.filter { it in bodies && latitude != null && longitude != null }.map {
                    SpaceCompassPanoramaObject(it, exportResources.getString(it.nameResource), overlays[it]?.path)
                }
                val credits = "NASA/GSFC/ASU; NASA/JPL/USGS; Solar System Scope / INOVE; solarsystemscope.com/textures/; " +
                    "CC BY 4.0 (creativecommons.org/licenses/by/4.0/)" +
                    (if (SpaceCompassCelestialBody.LV_426 in bodies) "; LV-426: fictional Alien moon; original AI illustration" else "") + if (weather != null) "; Open-Meteo (CC BY 4.0)" else ""
                val snapshot = SpaceCompassPanoramaSnapshot(timeMs, latitude ?: 0.0, longitude ?: 0.0, altitude ?: 0.0, phase, weather,
                    objects, remote, "", emptyList(), emptyList(), showPointLabels = showPointLabels,
                    observerPositionKnown = latitude != null && longitude != null, showSkyReferences = showSkyReferences, center = center)
                busy = true
                val preparingNotice = showSpaceCompassBottomMessage(application,
                    resources.getString(R.string.panorama_preparing), longDuration = true)
                scope.launch {
                    var file: File? = null
                    var cameraSource: File? = null
                    try {
                        // Photograph first; geocoding/rendering must never delay the actual shutter.
                        val photoFrame = if (cameraEnabled) requireNotNull(cameraCapture).capturePhoto() else null
                        val captureTime = photoFrame?.capturedTimeMs ?: capturedTime
                        val referenceTime = if (simulatedTime) timeMs else photoFrame?.capturedTimeMs ?: timeMs
                        val referenceSnapshot = snapshot.copy(timeMs = referenceTime)
                        val result = withContext(Dispatchers.IO) {
                            suspend fun placeFor(locale: java.util.Locale): SpaceCompassPlaceParts? {
                                val key = spaceCompassPlaceKey(latitude, longitude, locale.toLanguageTag()) ?: return null
                                return (placeCache.get(key, SystemClock.elapsedRealtime()) ?: run {
                                    val parts = try { lookupSpaceCompassEstimatedPlaceParts(application, key) }
                                    catch (cancelled: CancellationException) { throw cancelled }
                                    catch (_: Exception) { null }
                                    placeCache.put(key, parts?.let(::formatSpaceCompassEstimatedPlace),
                                        SystemClock.elapsedRealtime(), parts)
                                }).parts
                            }
                            val place = placeFor(spaceCompassPanoramaExportLocale)
                            val selectedPlace = if (selectedFormatting.locale.language == spaceCompassPanoramaExportLocale.language)
                                place else placeFor(selectedFormatting.locale)
                            val international = spaceCompassPanoramaPresentation(referenceSnapshot, exportResources,
                                spaceCompassPanoramaInternationalFormatting, zone, place, altitude, gpsAccuracyMeters, simulated, simulatedAltitude)
                            val selected = spaceCompassPanoramaPresentation(referenceSnapshot, resources,
                                selectedFormatting, zone, selectedPlace, altitude, gpsAccuracyMeters, simulated, simulatedAltitude)
                            val resolvedSnapshot = international.snapshot.copy(caption = spaceCompassPanoramaCaptionForPosition(international.caption, position))
                            val cache = File(application.cacheDir, "panoramas")
                            cache.listFiles()?.filter { it.isDirectory && System.currentTimeMillis() - it.lastModified() > 86_400_000L }
                                ?.forEach { it.deleteRecursively() }
                            val directory = File(cache, UUID.randomUUID().toString())
                            check(directory.mkdirs()) { "Preview directory unavailable" }
                            val target = File(directory, spaceCompassPanoramaFileName(captureTime))
                            file = target
                            val memory = (application.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager).memoryClass
                            val cameraPhoto = photoFrame?.let { frame ->
                                val source = File(directory, "camera-source.jpg")
                                cameraSource = source
                                source.writeBytes(frame.jpeg)
                                SpaceCompassCameraPhotoSnapshot(source, frame.lens, frame.displayRotation, frame.attitude,
                                    international.cameraWarning)
                            }
                            val bitmap = cameraPhoto?.let { renderSpaceCompassCameraPhoto(application, it, resolvedSnapshot) }
                                ?: renderSpaceCompassPanorama(resolvedSnapshot, if (memory >= 192) 4096 else 3072, application)
                            try { target.outputStream().use { check(bitmap.compress(Bitmap.CompressFormat.JPEG, 95, it)) } }
                            finally { bitmap.recycle() }
                            stampSpaceCompassPanoramaFile(target, captureTime, credits)
                            SpaceCompassPanoramaPreviewData(target, captureTime, position, resolvedSnapshot, international.caption, credits, cameraPhoto, selected)
                        }
                        preview = result
                    } catch (cancelled: CancellationException) { file?.delete(); cameraSource?.delete(); throw cancelled
                    } catch (error: OutOfMemoryError) { file?.delete(); cameraSource?.delete(); notify(R.string.panorama_error)
                    } catch (error: Exception) {
                        file?.delete(); cameraSource?.delete(); SpaceCompassErrorLog.record(application, "panorama:prepare", error); notify(R.string.panorama_error)
                    } finally { preparingNotice.cancel(); busy = false }
                }
            }
        }
    }, preview = preview, closePreview = { preview?.cameraPhoto?.source?.delete(); preview = null })
}
