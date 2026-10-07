package me.mondiversi.spacecompass

import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculateCentroidSize
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import kotlin.math.abs

/** Observe before scene taps, but leave all one-finger interactions untouched. */
@Composable
internal fun Modifier.spaceCompassCameraPinchZoom(range: SpaceCompassCameraZoomRange?, requested: Float,
    enabled: Boolean, ready: Boolean, onZoom: (Float) -> Unit): Modifier {
    val currentRange by rememberUpdatedState(range)
    val currentRequest by rememberUpdatedState(requested)
    val currentEnabled by rememberUpdatedState(enabled)
    val currentReady by rememberUpdatedState(ready)
    val updateZoom by rememberUpdatedState(onZoom)
    // A zoom request can replace the camera device. Keep this gesture on the stable sky parent.
    return pointerInput(Unit) {
        awaitEachGesture {
            awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Initial)
            var twoPointers = false
            var accepted = false
            var pastSlop = false
            var accumulated = 1f
            var gestureRange: SpaceCompassCameraZoomRange? = null
            var gestureZoom = currentRequest
            var emittedZoom = currentRequest
            do {
                val event = awaitPointerEvent(PointerEventPass.Initial)
                if (!twoPointers && event.changes.count { it.pressed } >= 2) {
                    twoPointers = true
                    gestureRange = currentRange
                    accepted = currentEnabled && currentReady && gestureRange != null
                    gestureZoom = currentRequest
                    emittedZoom = currentRequest
                }
                if (accepted) {
                    // Cancel underlying orbit taps, including the final single-finger release.
                    event.changes.forEach { it.consume() }
                    if (currentEnabled && event.changes.count { it.pressed && it.previousPressed } >= 2) {
                        val factor = event.calculateZoom()
                        if (factor.isFinite() && factor > 0f) {
                            val appliedFactor = if (pastSlop) factor else {
                                accumulated *= factor
                                pastSlop = abs(1f - accumulated) * event.calculateCentroidSize(useCurrent = false) > viewConfiguration.touchSlop
                                accumulated
                            }
                            if (pastSlop) {
                                val range = gestureRange!!
                                gestureZoom = range.pinch(gestureZoom, appliedFactor)
                                val next = range.snap(gestureZoom, emittedZoom)
                                if (next != emittedZoom) {
                                    emittedZoom = next
                                    updateZoom(next)
                                }
                            }
                        }
                    }
                }
            } while (event.changes.any { it.pressed })
        }
    }
}
