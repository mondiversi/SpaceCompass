package me.mondiversi.spacecompass

import android.Manifest
import android.app.ActivityManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.TimeZone

internal data class SpaceCompassPanoramaAction(val busy: Boolean, val capture: () -> Unit)

@Composable
internal fun rememberSpaceCompassPanoramaAction(timeMs: Long, latitude: Double?, longitude: Double?, altitude: Double,
    phase: SpaceCompassSunSkyPhase, weather: SpaceCompassSunWeatherSnapshot?,
    bodies: Set<SpaceCompassCelestialBody>, overlays: Map<SpaceCompassCelestialBody, SpaceCompassCelestialOverlay>,
    remote: SpaceCompassCelestialRemoteData): SpaceCompassPanoramaAction {
    val context = LocalContext.current
    val resources = LocalResources.current
    val application = context.applicationContext
    val locale = LocalConfiguration.current.locales[0]
    val deviceLocale = LocalSpaceCompassDeviceLocale.current
    val numeric = LocalSpaceCompassNumericFormat.current
    val dateFormat = LocalSpaceCompassDateFormat.current
    val timeFormat = resolveSpaceCompassTimeFormat(context, LocalSpaceCompassTimeFormat.current)
    val scope = rememberCoroutineScope()
    var busy by remember { mutableStateOf(false) }
    var pending by remember { mutableStateOf<SpaceCompassPanoramaSnapshot?>(null) }
    fun notify(resource: Int) = Toast.makeText(application, resources.getString(resource), Toast.LENGTH_LONG).show()
    fun save(snapshot: SpaceCompassPanoramaSnapshot) {
        busy = true
        notify(R.string.panorama_saving)
        scope.launch {
            try {
                withContext(Dispatchers.IO) {
                    val memory = (application.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager).memoryClass
                    val bitmap = renderSpaceCompassPanorama(snapshot, if (memory >= 192) 4096 else 3072)
                    try { saveSpaceCompassPanorama(application, bitmap, snapshot.timeMs) }
                    finally { bitmap.recycle() }
                }
                notify(R.string.panorama_saved)
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (memory: OutOfMemoryError) {
                notify(R.string.panorama_error)
            } catch (error: Exception) {
                SpaceCompassErrorLog.record(application, "panorama:save", error)
                notify(R.string.panorama_error)
            } finally { busy = false }
        }
    }
    val permission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        val snapshot = pending
        pending = null
        if (granted && snapshot != null) save(snapshot) else { busy = false; notify(R.string.panorama_permission) }
    }
    return SpaceCompassPanoramaAction(busy) {
        if (!busy) {
            if (latitude == null || longitude == null || bodies.isEmpty()) notify(R.string.panorama_not_ready)
            else {
                val zone = TimeZone.getDefault()
                val dateLocale = if (dateFormat == SpaceCompassDateFormat.SYSTEM) deviceLocale else locale
                val date = formatSpaceCompassDateOnly(timeMs, dateFormat, dateLocale, zone, numeric, deviceLocale)
                val time = formatSpaceCompassTimeOnly(timeMs, timeFormat, locale, zone, numeric, deviceLocale, includeSeconds = false)
                val snapshot = SpaceCompassPanoramaSnapshot(timeMs, latitude, longitude, altitude, phase, weather,
                    spaceCompassCelestialCatalogOrder.filter { it in bodies }.map {
                        SpaceCompassPanoramaObject(it, resources.getString(it.nameResource), overlays[it]?.path)
                    }, remote, "Space Compass · $date · $time · ${zone.getDisplayName(zone.inDaylightTime(java.util.Date(timeMs)), TimeZone.SHORT, locale)}",
                    listOf(R.string.panorama_north, R.string.panorama_east, R.string.panorama_south, R.string.panorama_west).map(resources::getString),
                    listOf(60.0, 30.0, 0.0, -30.0, -60.0).map { "${formatSpaceCompassNumber(it, 0, numeric, grouping = false)}°" },
                    resources.getString(R.string.panorama_data_unavailable), resources.getString(R.string.panorama_path_unavailable))
                if (Build.VERSION.SDK_INT < 29 && ContextCompat.checkSelfPermission(context,
                        Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                    pending = snapshot; busy = true
                    permission.launch(Manifest.permission.WRITE_EXTERNAL_STORAGE)
                } else save(snapshot)
            }
        }
    }
}
