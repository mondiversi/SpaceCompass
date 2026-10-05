package me.mondiversi.spacecompass

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
internal enum class SpaceCompassHapticCue { CHOICE, TOGGLE_ON, TOGGLE_OFF, COMMAND, HOLD_COMPLETE }

internal data class SpaceCompassHapticPulse(val durationMs: Long, val amplitude: Int)

internal fun spaceCompassHapticPulse(cue: SpaceCompassHapticCue): SpaceCompassHapticPulse = when (cue) {
    SpaceCompassHapticCue.CHOICE,
    SpaceCompassHapticCue.TOGGLE_ON,
    SpaceCompassHapticCue.TOGGLE_OFF,
    SpaceCompassHapticCue.COMMAND,
    SpaceCompassHapticCue.HOLD_COMPLETE -> SpaceCompassHapticPulse(26L, 130)
}

internal class SpaceCompassHapticController(context: Context) {
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
    fun spaceCompassHaptic(cue: SpaceCompassHapticCue) {
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
        val pulse = spaceCompassHapticPulse(cue)
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
private object SpaceCompassHapticRegistry {
    @Volatile private var controller: SpaceCompassHapticController? = null

    fun get(context: Context): SpaceCompassHapticController =
        controller ?: synchronized(this) {
            controller ?: SpaceCompassHapticController(context).also { controller = it }
        }
}

@Composable
internal fun rememberSpaceCompassHapticController(): SpaceCompassHapticController {
    val applicationContext = LocalContext.current.applicationContext
    return remember(applicationContext) { SpaceCompassHapticRegistry.get(applicationContext) }
}

/** Immediate, non-consuming touch feedback; the control still decides on release whether to act. */
@Composable
internal fun Modifier.spaceCompassHapticOnPress(enabled: Boolean = true): Modifier {
    val haptics = rememberSpaceCompassHapticController()
    return if (!enabled) this else this.pointerInput(haptics) {
        awaitEachGesture {
            awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Initial)
            haptics.spaceCompassHaptic(SpaceCompassHapticCue.CHOICE)
            do {
                val event = awaitPointerEvent(PointerEventPass.Initial)
            } while (event.changes.any { it.pressed })
        }
    }
}
