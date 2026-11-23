package me.mondiversi.spacecompass

import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.input.pointer.*
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp

@Stable
internal class SpaceCompassMainGestureRegions {
    val controls = mutableStateMapOf<Any, Rect>()
}

internal val LocalSpaceCompassMainGestureRegions = staticCompositionLocalOf<SpaceCompassMainGestureRegions?> { null }

/** Register actual action bounds only while the main view provides its gesture scope. */
internal fun Modifier.spaceCompassMainGestureControl(): Modifier = composed {
    val regions = LocalSpaceCompassMainGestureRegions.current ?: return@composed this
    val id = remember { Any() }
    DisposableEffect(regions, id) { onDispose { regions.controls.remove(id) } }
    onGloballyPositioned { coordinates ->
        val bounds = coordinates.boundsInRoot()
        if (bounds.width > 0 && bounds.height > 0) regions.controls[id] = bounds
    }
}

/** Scene points retain their detector; chrome gestures use the same safety gap around real actions. */
internal fun spaceCompassMainFreeAreaGestureAllowed(point: Offset, origin: Offset,
    scene: SpaceCompassSunSceneFrame?, controls: List<Rect>, margin: Float): Boolean {
    if (scene == null || !point.x.isFinite() || !point.y.isFinite()) return false
    if (spaceCompassScenePointIsCovered(SpaceCompassSunScenePoint(point.x.toDouble(), point.y.toDouble()), listOf(scene))) return false
    val rootPoint = point + origin
    return controls.none { it.inflate(margin.coerceAtLeast(0f)).contains(rootPoint) }
}

/** Observe before child shields, but consume only a stationary release so scrolling keeps priority. */
internal fun Modifier.spaceCompassMainFreeAreaGestures(regions: SpaceCompassMainGestureRegions,
    scene: () -> SpaceCompassSunSceneFrame?, enabled: Boolean, onTap: () -> Unit,
    onDoubleTap: () -> Unit): Modifier = composed {
    if (!enabled) return@composed this
    var origin by remember { mutableStateOf(Offset.Zero) }
    val currentScene by rememberUpdatedState(scene)
    val tap by rememberUpdatedState(onTap)
    val doubleTap by rememberUpdatedState(onDoubleTap)
    val margin = with(LocalDensity.current) { SPACE_COMPASS_OVERLAY_GESTURE_MARGIN_DP.dp.toPx() }
    onGloballyPositioned { origin = it.boundsInRoot().topLeft }.pointerInput(regions, margin) {
        fun allowed(point: Offset) = spaceCompassMainFreeAreaGestureAllowed(point, origin,
            currentScene(), regions.controls.values.toList(), margin)
        awaitEachGesture {
            suspend fun AwaitPointerEventScope.release(down: PointerInputChange): PointerInputChange? {
                var stationary = true
                while (true) {
                    val event = awaitPointerEvent(PointerEventPass.Initial)
                    if (event.changes.size > 1) stationary = false
                    val change = event.changes.firstOrNull { it.id == down.id } ?: return null
                    if ((change.position - down.position).getDistance() > viewConfiguration.touchSlop) stationary = false
                    if (!change.pressed) return change.takeIf { stationary && allowed(it.position) }
                }
            }
            val first = awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Initial)
            if (!allowed(first.position)) return@awaitEachGesture
            val up = release(first) ?: return@awaitEachGesture
            up.consume()
            val second = withTimeoutOrNull(minOf(viewConfiguration.doubleTapTimeoutMillis, 200L)) {
                val earliest = up.uptimeMillis + viewConfiguration.doubleTapMinTimeMillis
                var next = awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Initial)
                while (next.uptimeMillis < earliest) next = awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Initial)
                next
            }
            if (second == null || !allowed(second.position)) {
                tap()
            } else {
                val secondUp = release(second)
                if (secondUp != null) { secondUp.consume(); doubleTap() } else tap()
            }
        }
    }
}
