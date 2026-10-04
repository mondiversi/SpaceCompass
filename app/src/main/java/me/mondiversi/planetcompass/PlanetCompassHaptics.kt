package me.mondiversi.planetcompass

import android.content.Context
import android.media.AudioAttributes
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.os.VibrationAttributes
import android.os.SystemClock
import android.provider.Settings
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext

/** Short feedback for the fixed capture, record and stop controls only. */
internal enum class PlanetCompassHapticCue { CHOICE, TOGGLE_ON, TOGGLE_OFF, COMMAND, HOLD_COMPLETE }

internal data class PlanetCompassHapticPulse(val durationMs: Long, val amplitude: Int)

internal fun planetCompassHapticPulse(cue: PlanetCompassHapticCue): PlanetCompassHapticPulse = when (cue) {
    PlanetCompassHapticCue.CHOICE,
    PlanetCompassHapticCue.TOGGLE_ON,
    PlanetCompassHapticCue.TOGGLE_OFF,
    PlanetCompassHapticCue.COMMAND,
    PlanetCompassHapticCue.HOLD_COMPLETE -> PlanetCompassHapticPulse(26L, 130)
}

internal class PlanetCompassHapticController(context: Context) {
    private val contentResolver = context.applicationContext.contentResolver
    private var lastPulseAtMs = 0L
    private val vibrator: Vibrator? =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            context.applicationContext.getSystemService(VibratorManager::class.java)?.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            context.applicationContext.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
        }

    private val touchAttributes = AudioAttributes.Builder()
        .setUsage(AudioAttributes.USAGE_ASSISTANCE_SONIFICATION)
        .build()

    @Suppress("DEPRECATION")
    fun planetCompassHaptic(cue: PlanetCompassHapticCue) {
        if (Settings.System.getInt(
                contentResolver,
                Settings.System.HAPTIC_FEEDBACK_ENABLED,
                1
            ) == 0
        ) return
        val motor = vibrator?.takeIf { it.hasVibrator() } ?: return
        val now = SystemClock.elapsedRealtime()
        // A long press followed by a menu selection must not feel like two strong clicks.
        if (now - lastPulseAtMs < 80L) return
        lastPulseAtMs = now
        val pulse = planetCompassHapticPulse(cue)
        val effect = VibrationEffect.createOneShot(pulse.durationMs, pulse.amplitude)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            motor.vibrate(
                effect,
                VibrationAttributes.Builder()
                    .setUsage(VibrationAttributes.USAGE_TOUCH)
                    .build()
            )
        } else {
            motor.vibrate(effect, touchAttributes)
        }
    }
}

/** One controller per process also coalesces a touch seen by nested clickable components. */
private object PlanetCompassHapticRegistry {
    @Volatile private var controller: PlanetCompassHapticController? = null

    fun get(context: Context): PlanetCompassHapticController =
        controller ?: synchronized(this) {
            controller ?: PlanetCompassHapticController(context).also { controller = it }
        }
}

@Composable
internal fun rememberPlanetCompassHapticController(): PlanetCompassHapticController {
    val applicationContext = LocalContext.current.applicationContext
    return remember(applicationContext) { PlanetCompassHapticRegistry.get(applicationContext) }
}

/** Immediate, non-consuming touch feedback; the control still decides on release whether to act. */
@Composable
internal fun Modifier.planetCompassHapticOnPress(enabled: Boolean = true): Modifier {
    val haptics = rememberPlanetCompassHapticController()
    return if (!enabled) this else this.pointerInput(haptics) {
        awaitEachGesture {
            awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Initial)
            haptics.planetCompassHaptic(PlanetCompassHapticCue.CHOICE)
            do {
                val event = awaitPointerEvent(PointerEventPass.Initial)
            } while (event.changes.any { it.pressed })
        }
    }
}
