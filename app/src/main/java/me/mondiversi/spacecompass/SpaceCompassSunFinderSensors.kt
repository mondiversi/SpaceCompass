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

internal enum class SpaceCompassSunLocationStatus { PERMISSION, SEARCHING, DISABLED, READY, ERROR }
internal data class SpaceCompassSunFinderReadings(
    val location: Location? = null,
    val locationStatus: SpaceCompassSunLocationStatus = SpaceCompassSunLocationStatus.PERMISSION,
    val orientation: SpaceCompassSunOrientation? = null,
    val compassAvailable: Boolean = true,
    val compassReliable: Boolean = false,
    val compassUsable: Boolean = compassReliable,
    val cameraAttitude: SpaceCompassCameraAttitude? = null
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
    var readings by remember { mutableStateOf(SpaceCompassSunFinderReadings()) }
    DisposableEffect(context, view, lifecycleOwner, permissionGranted) {
        val sensors = context.getSystemService(SensorManager::class.java)
        val locations = context.getSystemService(LocationManager::class.java)
        val gravitySensor = sensors?.getDefaultSensor(Sensor.TYPE_GRAVITY)
            ?: sensors?.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        val magnetometer = sensors?.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD)
        val available = gravitySensor != null && magnetometer != null
        var started = false
        var declination = 0.0
        var expectedFieldMicrotesla: Double? = null
        var gravity: FloatArray? = null
        var magnetic: FloatArray? = null
        var gravityTime = 0L
        var magneticTime = 0L
        var smoothed: SpaceCompassSunOrientation? = null
        var lastEventTime = 0L
        var magneticAccuracy = SensorManager.SENSOR_STATUS_UNRELIABLE
        val recovery = SpaceCompassSunCompassRecovery()
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
            declination = field.declination.toDouble()
            expectedFieldMicrotesla = field.fieldStrength / 1_000.0 // Android reports nanotesla.
            smoothed = null
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
        val sensorListener = object : SensorEventListener {
            override fun onAccuracyChanged(sensor: Sensor, accuracy: Int) {
                if (sensor.type == Sensor.TYPE_MAGNETIC_FIELD) {
                    magneticAccuracy = accuracy
                    // A callback alone cannot establish fresh, plausible magnetic readings.
                    if (accuracy < SensorManager.SENSOR_STATUS_ACCURACY_LOW) {
                        recovery.reset()
                        readings = readings.copy(compassReliable = false, compassUsable = false)
                    }
                }
            }
            override fun onSensorChanged(event: SensorEvent) {
                if (!started || sensorPaused) return
                when (event.sensor.type) {
                    Sensor.TYPE_MAGNETIC_FIELD -> {
                        magnetic = event.values.copyOf()
                        magneticTime = event.timestamp
                        magneticAccuracy = event.accuracy
                    }
                    Sensor.TYPE_GRAVITY, Sensor.TYPE_ACCELEROMETER -> {
                        val previous = gravity
                        gravity = if (event.sensor.type == Sensor.TYPE_ACCELEROMETER && previous != null)
                            FloatArray(3) { previous[it] * 0.8f + event.values[it] * 0.2f }
                        else event.values.copyOf()
                        gravityTime = event.timestamp
                    }
                }
                if (event.timestamp - lastEventTime < 40_000_000L) return
                lastEventTime = event.timestamp
                val g = gravity
                val m = magnetic
                val matrix = FloatArray(9)
                // The calibrated magnetic field establishes north; gravity compensates tilt.
                // Do not let the handset's independent rotation-vector yaw replace that reference.
                if (g == null || m == null || g.size < 3 || m.size < 3 ||
                    !g.all { it.isFinite() } || !m.all { it.isFinite() } ||
                    !SensorManager.getRotationMatrix(matrix, null, g, m)) {
                    recovery.reset()
                    smoothed = null
                    readings = readings.copy(orientation = null, compassReliable = false, compassUsable = false, cameraAttitude = null)
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
                if (!SensorManager.remapCoordinateSystem(matrix, axes.first, axes.second, screen)) return
                val current = spaceCompassSunOrientationFromScreenMatrix(screen, declination) ?: return
                val now = SystemClock.elapsedRealtimeNanos()
                fun fresh(timestamp: Long) = timestamp > 0 && now - timestamp in 0..500_000_000L
                val recent = fresh(gravityTime) && fresh(magneticTime) &&
                    abs(gravityTime - magneticTime) <= 200_000_000L
                val fieldMicrotesla = sqrt(m.sumOf { value -> value.toDouble().pow(2) })
                val usable = recovery.update(recent && spaceCompassSunMagneticReferenceUsable(
                    magneticAccuracy, fieldMicrotesla, expectedFieldMicrotesla), event.timestamp)
                // Recalibration must not blend a previously invalid bearing into a valid one.
                smoothed = if (usable) smoothSpaceCompassSunOrientation(smoothed, current) else null
                val reliable = usable && spaceCompassSunMagneticReferenceReliable(magneticAccuracy, fieldMicrotesla, expectedFieldMicrotesla)
                readings = readings.copy(orientation = smoothed ?: current, compassReliable = reliable, compassUsable = usable,
                    cameraAttitude = spaceCompassSunOrientationFromScreenMatrix(matrix, declination)?.let {
                        SpaceCompassCameraAttitude(event.timestamp, it, usable) })
            }
        }
        fun stop() {
            if (!started) return
            started = false
            sensors?.unregisterListener(sensorListener)
            runCatching { locations?.removeUpdates(locationListener) }
            smoothed = null
            gravity = null
            magnetic = null
            gravityTime = 0L
            magneticTime = 0L
            magneticAccuracy = SensorManager.SENSOR_STATUS_UNRELIABLE
            recovery.reset()
            lastEventTime = 0L
            readings = readings.copy(orientation = null, compassReliable = false, compassUsable = false, cameraAttitude = null)
        }
        fun start() {
            if (started) return
            started = true
            readings = readings.copy(compassAvailable = available, compassReliable = false, compassUsable = false)
            try {
                val registered = if (available) {
                    val g = sensors?.registerListener(sensorListener, gravitySensor, SensorManager.SENSOR_DELAY_UI) == true
                    val m = sensors?.registerListener(sensorListener, magnetometer, SensorManager.SENSOR_DELAY_UI) == true
                    g && m
                } else false
                if (!registered) sensors?.unregisterListener(sensorListener)
                readings = readings.copy(compassAvailable = registered)
            } catch (error: Exception) {
                sensors?.unregisterListener(sensorListener)
                failure("orientation", error)
                readings = readings.copy(compassAvailable = false)
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

/** Smooth vectors across 0/360 degrees, then restore orthonormal axes for correct projection. */
internal fun smoothSpaceCompassSunOrientation(previous: SpaceCompassSunOrientation?, current: SpaceCompassSunOrientation): SpaceCompassSunOrientation {
    if (previous == null || previous.forward.dot(current.forward) < -0.8) return current
    fun mix(a: SpaceCompassSunVector, b: SpaceCompassSunVector) = SpaceCompassSunVector(
        a.east * 0.65 + b.east * 0.35, a.north * 0.65 + b.north * 0.35, a.up * 0.65 + b.up * 0.35
    ).normalized()
    val forward = mix(previous.forward, current.forward)
    val rawRight = mix(previous.right, current.right)
    val parallel = rawRight.dot(forward)
    val right = SpaceCompassSunVector(rawRight.east - parallel * forward.east,
        rawRight.north - parallel * forward.north, rawRight.up - parallel * forward.up).normalized()
    val up = SpaceCompassSunVector(right.north * forward.up - right.up * forward.north,
        right.up * forward.east - right.east * forward.up, right.east * forward.north - right.north * forward.east)
    return SpaceCompassSunOrientation(right, up, forward)
}
