package me.mondiversi.spacecompass

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.hardware.*
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Bundle
import android.os.Build
import android.os.Looper
import android.os.SystemClock
import android.view.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import kotlin.math.*
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

internal enum class SpaceCompassSunLocationStatus { PERMISSION, SEARCHING, DISABLED, READY, ERROR }
internal data class SpaceCompassSunFinderReadings(
    val location: Location? = null,
    val locationStatus: SpaceCompassSunLocationStatus = SpaceCompassSunLocationStatus.PERMISSION,
    val orientation: SpaceCompassSunOrientation? = null,
    val compassAvailable: Boolean = true,
    val compassReliable: Boolean = false,
    val compassUsable: Boolean = compassReliable,
    val cameraAttitude: SpaceCompassCameraAttitude? = null,
    val compassIssue: SpaceCompassSunCompassIssue = when {
        compassReliable -> SpaceCompassSunCompassIssue.NONE
        compassUsable -> SpaceCompassSunCompassIssue.REDUCED_ACCURACY
        else -> SpaceCompassSunCompassIssue.WAITING
    }
)

internal const val SPACE_COMPASS_SUN_LOCATION_MAX_AGE_MS = 15 * 60_000L

internal fun spaceCompassSunLocationUsable(location: Location, elapsedRealtimeNanos: Long): Boolean =
    location.latitude.isFinite() && location.longitude.isFinite() &&
        location.latitude in -90.0..90.0 && location.longitude in -180.0..180.0 &&
        location.hasAccuracy() && location.accuracy.isFinite() && location.accuracy >= 0 &&
        location.elapsedRealtimeNanos > 0 &&
        (elapsedRealtimeNanos - location.elapsedRealtimeNanos) in 0..(SPACE_COMPASS_SUN_LOCATION_MAX_AGE_MS * 1_000_000)

internal fun hasSpaceCompassSunLocationPermission(context: Context): Boolean =
    listOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION).any {
        ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED
    }

/** Screen-owned listeners only: no background tracking, storage, uploads or sensor-device commands. */
@Composable
internal fun rememberSpaceCompassSunFinderReadings(permissionGranted: Boolean, paused: Boolean = false): SpaceCompassSunFinderReadings {
    val context = LocalContext.current
    val view = LocalView.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val sensorPaused by rememberUpdatedState(paused)
    val fluidity by rememberUpdatedState(SpaceCompassSkyFluidity.fromStored(
        LocalSpaceCompassPresentationSettings.current[SPACE_COMPASS_SKY_FLUIDITY_KEY]))
    val monitorScope = rememberCoroutineScope()
    var readings by remember { mutableStateOf(SpaceCompassSunFinderReadings()) }
    DisposableEffect(context, view, lifecycleOwner, permissionGranted) {
        val sensors = context.getSystemService(SensorManager::class.java)
        val locations = context.getSystemService(LocationManager::class.java)
        val gravitySensor = sensors?.getDefaultSensor(Sensor.TYPE_GRAVITY)
            ?: sensors?.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        val magnetometer = sensors?.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD)
        val gyroscope = sensors?.getDefaultSensor(Sensor.TYPE_GYROSCOPE)
        val available = gravitySensor != null && magnetometer != null
        var started = false
        var declination = 0.0
        var expectedFieldMicrotesla: Double? = null
        var gravity: FloatArray? = null
        var magnetic: FloatArray? = null
        var gravityTime = 0L
        var magneticTime = 0L
        var smoothed: SpaceCompassSunOrientation? = null
        var smoothedRotation: Int? = null
        var lastEventTime = 0L
        var magneticAccuracy = SensorManager.SENSOR_STATUS_UNRELIABLE
        val motionStabilizer = SpaceCompassSunMotionStabilizer()
        val recovery = SpaceCompassSunCompassRecovery()
        val issueStabilizer = SpaceCompassSunCompassIssueStabilizer()
        val watchdog = SpaceCompassSunSensorWatchdog()
        var watchdogJob: Job? = null
        var fluidityJob: Job? = null
        var registeredFluidity = fluidity
        val loggedErrors = mutableSetOf<String>()
        fun failure(source: String, error: Exception) {
            if (loggedErrors.add(source)) SpaceCompassErrorLog.record(context, "sun_finder:$source", error)
        }
        fun acceptLocation(location: Location) {
            if (!started) return
            if (!spaceCompassSunLocationUsable(location, SystemClock.elapsedRealtimeNanos())) return
            val previous = readings.location
            // Do not replace a recent precise GPS fix with an older network result.
            if (previous != null && location.elapsedRealtimeNanos < previous.elapsedRealtimeNanos) return
            val copy = Location(location)
            val height = copy.takeIf { it.hasAltitude() && it.altitude.isFinite() && it.altitude in -500.0..20_000.0 }
                ?.altitude?.toFloat() ?: 0f
            val field = GeomagneticField(copy.latitude.toFloat(), copy.longitude.toFloat(),
                height, System.currentTimeMillis())
            val nextDeclination = field.declination.toDouble()
            // A normal GPS refresh must not snap the display back to an unfiltered pose.
            smoothed = smoothed?.let { rebaseSpaceCompassSunOrientation(it, nextDeclination - declination) }
            declination = nextDeclination
            expectedFieldMicrotesla = field.fieldStrength / 1_000.0 // Android reports nanotesla.
            readings = readings.copy(location = copy, locationStatus = SpaceCompassSunLocationStatus.READY)
        }
        val locationListener = object : LocationListener {
            override fun onLocationChanged(location: Location) = acceptLocation(location)
            override fun onProviderDisabled(provider: String) {
                if (!started) return
                if (locations != null && !isSpaceCompassSunLocationEnabled(locations))
                    readings = readings.copy(location = null, locationStatus = SpaceCompassSunLocationStatus.DISABLED)
            }
            override fun onProviderEnabled(provider: String) {
                if (!started) return
                readings = readings.copy(locationStatus = SpaceCompassSunLocationStatus.SEARCHING)
            }
            @Deprecated("Required by older Android versions")
            override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) = Unit
        }
        fun sensorReadingsFresh(now: Long) = spaceCompassSunSensorSamplesFresh(now, gravityTime, magneticTime)
        fun invalidateOrientation() {
            motionStabilizer.reset()
            recovery.reset()
            issueStabilizer.reset()
            smoothed = null
            readings = readings.copy(orientation = null, compassReliable = false, compassUsable = false,
                cameraAttitude = null, compassIssue = SpaceCompassSunCompassIssue.WAITING)
        }
        fun resetSensorSamples() {
            gravity = null
            magnetic = null
            gravityTime = 0L
            magneticTime = 0L
            magneticAccuracy = SensorManager.SENSOR_STATUS_UNRELIABLE
            lastEventTime = 0L
            invalidateOrientation()
        }
        val sensorListener = object : SensorEventListener {
            override fun onAccuracyChanged(sensor: Sensor, accuracy: Int) {
                if (!started || sensorPaused) return
                if (sensor.type == Sensor.TYPE_GYROSCOPE && accuracy <= SensorManager.SENSOR_STATUS_UNRELIABLE)
                    motionStabilizer.reset()
                if (sensor.type == Sensor.TYPE_MAGNETIC_FIELD) {
                    magneticAccuracy = accuracy
                    // A callback alone cannot establish fresh, plausible magnetic readings.
                    if (accuracy < SensorManager.SENSOR_STATUS_ACCURACY_MEDIUM) {
                        val field = magnetic?.takeIf { it.size >= 3 }?.let { values ->
                            sqrt(values.take(3).sumOf { it.toDouble().pow(2) })
                        } ?: Double.NaN
                        val fresh = sensorReadingsFresh(SystemClock.elapsedRealtimeNanos())
                        val usable = readings.compassUsable && fresh &&
                            spaceCompassSunMagneticReferenceUsable(accuracy, field, expectedFieldMicrotesla)
                        if (!usable) { recovery.reset(); motionStabilizer.reset() }
                        readings = readings.copy(compassReliable = false, compassUsable = usable,
                            cameraAttitude = readings.cameraAttitude?.takeIf { usable },
                            compassIssue = issueStabilizer.update(spaceCompassSunCompassIssue(accuracy, field, expectedFieldMicrotesla,
                                fresh, recovered = usable), magneticTime))
                    }
                }
            }
            override fun onSensorChanged(event: SensorEvent) {
                if (!started || sensorPaused) return
                when (event.sensor.type) {
                    Sensor.TYPE_GYROSCOPE -> {
                        if (event.values.size >= 3) motionStabilizer.recordGyroscope(event.timestamp,
                            event.values[0].toDouble(), event.values[1].toDouble(), event.values[2].toDouble(),
                            event.accuracy > SensorManager.SENSOR_STATUS_UNRELIABLE)
                        else motionStabilizer.reset()
                        // A gyro event must never refresh the absolute pose or magnetic sample age.
                        return
                    }
                    Sensor.TYPE_MAGNETIC_FIELD -> {
                        magnetic = event.values.copyOf()
                        magneticTime = event.timestamp
                        magneticAccuracy = event.accuracy
                    }
                    Sensor.TYPE_GRAVITY, Sensor.TYPE_ACCELEROMETER -> {
                        val previous = gravity
                        gravity = if (event.sensor.type == Sensor.TYPE_ACCELEROMETER && previous != null) {
                            // Preserve the fallback filter response when sampling faster or irregularly.
                            val blend = spaceCompassSunSmoothingWeight(event.timestamp - gravityTime,
                                SPACE_COMPASS_SUN_GRAVITY_SMOOTHING_NS).toFloat()
                            FloatArray(3) { previous[it] + (event.values[it] - previous[it]) * blend }
                        } else event.values.copyOf()
                        gravityTime = event.timestamp
                    }
                    else -> return
                }
                val elapsed = event.timestamp - lastEventTime
                if (elapsed < registeredFluidity.minimumUpdateIntervalNs) return
                lastEventTime = event.timestamp
                val g = gravity
                val m = magnetic
                val matrix = FloatArray(9)
                // The calibrated magnetic field establishes north; gravity compensates tilt.
                // Do not let the handset's independent rotation-vector yaw replace that reference.
                if (g == null || m == null || g.size < 3 || m.size < 3 ||
                    !g.all { it.isFinite() } || !m.all { it.isFinite() } ||
                    !SensorManager.getRotationMatrix(matrix, null, g, m)) {
                    invalidateOrientation()
                    return
                }
                val displayRotation = view.display?.rotation ?: Surface.ROTATION_0
                val axes = when (displayRotation) {
                    Surface.ROTATION_90 -> SensorManager.AXIS_Y to SensorManager.AXIS_MINUS_X
                    Surface.ROTATION_180 -> SensorManager.AXIS_MINUS_X to SensorManager.AXIS_MINUS_Y
                    Surface.ROTATION_270 -> SensorManager.AXIS_MINUS_Y to SensorManager.AXIS_X
                    else -> SensorManager.AXIS_X to SensorManager.AXIS_Y
                }
                val screen = FloatArray(9)
                if (!SensorManager.remapCoordinateSystem(matrix, axes.first, axes.second, screen)) {
                    invalidateOrientation(); return
                }
                val current = spaceCompassSunOrientationFromScreenMatrix(screen, declination)
                    ?: run { invalidateOrientation(); return }
                val now = SystemClock.elapsedRealtimeNanos()
                val recent = sensorReadingsFresh(now)
                val fieldMicrotesla = sqrt(m.sumOf { value -> value.toDouble().pow(2) })
                val usable = recovery.update(recent && spaceCompassSunMagneticReferenceUsable(
                    magneticAccuracy, fieldMicrotesla, expectedFieldMicrotesla), event.timestamp)
                // Recalibration must not blend a previously invalid bearing into a valid one.
                if (smoothedRotation != displayRotation || !usable) {
                    smoothed = null
                    motionStabilizer.reset()
                }
                smoothedRotation = displayRotation
                smoothed = if (usable) motionStabilizer.smooth(smoothed, current,
                    event.timestamp - elapsed, event.timestamp, now, displayRotation) else null
                val measuredPose = smoothed ?: current
                val reliable = usable && spaceCompassSunMagneticReferenceReliable(magneticAccuracy, fieldMicrotesla, expectedFieldMicrotesla)
                readings = readings.copy(orientation = measuredPose, compassReliable = reliable, compassUsable = usable,
                    compassIssue = issueStabilizer.update(
                        spaceCompassSunCompassIssue(magneticAccuracy, fieldMicrotesla, expectedFieldMicrotesla, recent, usable),
                        magneticTime),
                    cameraAttitude = SpaceCompassCameraAttitude(event.timestamp,
                        spaceCompassSceneRemapOrientation(measuredPose, displayRotation, Surface.ROTATION_0), usable))
            }
        }
        fun registerSensorListeners() {
            registeredFluidity = fluidity
            sensors?.unregisterListener(sensorListener)
            resetSensorSamples()
            try {
                val registered = if (available) {
                    val g = sensors?.registerListener(sensorListener, gravitySensor, registeredFluidity.sensorPeriodUs) == true
                    val m = sensors?.registerListener(sensorListener, magnetometer, registeredFluidity.sensorPeriodUs) == true
                    g && m
                } else false
                if (!registered) sensors?.unregisterListener(sensorListener)
                else if (gyroscope != null) {
                    // Optional: a missing/rejected gyro leaves the previous fast filter intact.
                    try { sensors?.registerListener(sensorListener, gyroscope, registeredFluidity.sensorPeriodUs) }
                    catch (error: Exception) { failure("motion_stabilization", error) }
                }
                readings = readings.copy(compassAvailable = registered)
            } catch (error: Exception) {
                sensors?.unregisterListener(sensorListener)
                failure("orientation", error)
                readings = readings.copy(compassAvailable = false)
            }
        }
        fun stop() {
            if (!started) return
            started = false
            watchdogJob?.cancel()
            watchdogJob = null
            fluidityJob?.cancel()
            fluidityJob = null
            watchdog.reset()
            sensors?.unregisterListener(sensorListener)
            runCatching { locations?.removeUpdates(locationListener) }
            resetSensorSamples()
        }
        fun start() {
            if (started) return
            started = true
            watchdog.reset()
            registerSensorListeners()
            if (available) fluidityJob = monitorScope.launch {
                snapshotFlow { fluidity }.collect { next ->
                    // Reconfigure only sensors: GPS, ephemerides and selections stay alive.
                    if (started && next != registeredFluidity) registerSensorListeners()
                }
            }
            if (available) watchdogJob = monitorScope.launch {
                while (isActive && started) {
                    delay(SPACE_COMPASS_SUN_SENSOR_POLL_MS)
                    val health = watchdog.check(SystemClock.elapsedRealtimeNanos(), gravityTime, magneticTime,
                        monitoring = !sensorPaused)
                    if (health.stale) invalidateOrientation()
                    // Retry only the sensors: preserve the GPS fix, selected objects and online data.
                    if (health.restart) registerSensorListeners()
                }
            }
            if (!permissionGranted || !hasSpaceCompassSunLocationPermission(context)) {
                readings = readings.copy(location = null, locationStatus = SpaceCompassSunLocationStatus.PERMISSION)
                return
            }
            try {
                if (locations == null || !isSpaceCompassSunLocationEnabled(locations)) {
                    readings = readings.copy(location = null, locationStatus = SpaceCompassSunLocationStatus.DISABLED)
                    return
                }
                readings = readings.copy(location = null, locationStatus = SpaceCompassSunLocationStatus.SEARCHING)
                val providers = listOf(LocationManager.GPS_PROVIDER, LocationManager.NETWORK_PROVIDER)
                    .filter { locations.isProviderEnabled(it) }
                if (providers.isEmpty()) {
                    readings = readings.copy(locationStatus = SpaceCompassSunLocationStatus.DISABLED)
                    return
                }
                var locationUpdatesRegistered = false
                for (provider in providers) {
                    try {
                        locations.getLastKnownLocation(provider)?.let(::acceptLocation)
                        locations.requestLocationUpdates(provider, 5_000L, 5f, locationListener, Looper.getMainLooper())
                        locationUpdatesRegistered = true
                    } catch (error: SecurityException) {
                        // Approximate-only permission can deny GPS while still allowing network location.
                        if (provider == LocationManager.NETWORK_PROVIDER) throw error
                    }
                }
                if (!locationUpdatesRegistered && readings.location == null)
                    readings = readings.copy(locationStatus = SpaceCompassSunLocationStatus.ERROR)
            } catch (error: Exception) {
                failure("location", error)
                readings = readings.copy(location = null, locationStatus =
                    if (error is SecurityException) SpaceCompassSunLocationStatus.PERMISSION else SpaceCompassSunLocationStatus.ERROR)
            }
        }
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> start()
                Lifecycle.Event.ON_PAUSE, Lifecycle.Event.ON_STOP -> stop()
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        if (lifecycleOwner.lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)) start()
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer); stop() }
    }
    return readings
}

private fun isSpaceCompassSunLocationEnabled(manager: LocationManager): Boolean =
    if (Build.VERSION.SDK_INT >= 28) manager.isLocationEnabled
    else manager.isProviderEnabled(LocationManager.GPS_PROVIDER) || manager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)
