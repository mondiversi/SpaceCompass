package me.mondiversi.spacecompass

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.unit.dp

/** React only to deliberate list gestures, never to programmatic centering or animation remeasurement. */
@Stable
internal class SpaceCompassQuickReturnHeaderState(
    private val thresholdPx: Float, private val pinned: () -> Boolean
) {
    var visible by mutableStateOf(true)
        private set
    private var travel = 0f

    fun show() { visible = true; travel = 0f }
    fun hide() { visible = false; travel = 0f }

    val connection = object : NestedScrollConnection {
        override fun onPostScroll(consumed: Offset, available: Offset, source: NestedScrollSource): Offset {
            if (source != NestedScrollSource.UserInput) return Offset.Zero
            if (pinned()) { show(); return Offset.Zero }
            // An upward drag must actually move content; a downward pull also works at the top.
            val delta = if (consumed.y < 0f) consumed.y else (consumed.y + available.y).coerceAtLeast(0f)
            if (!delta.isFinite() || delta == 0f) return Offset.Zero
            travel = (if (travel * delta < 0f) delta else travel + delta).coerceIn(-thresholdPx, thresholdPx)
            if (travel <= -thresholdPx) visible = false
            else if (travel >= thresholdPx) visible = true
            return Offset.Zero
        }
    }
}

@Composable
internal fun rememberSpaceCompassQuickReturnHeader(pinned: Boolean = false, initialVisible: Boolean = true): SpaceCompassQuickReturnHeaderState {
    val currentPinned by rememberUpdatedState(pinned)
    val threshold = with(LocalDensity.current) { 8.dp.toPx() }
    val state = remember(threshold) { SpaceCompassQuickReturnHeaderState(threshold) { currentPinned }.apply { if (!initialVisible) hide() } }
    LaunchedEffect(pinned) { if (pinned) state.show() }
    return state
}

/** Outgoing controls become inert immediately, while their height and opacity animate together. */
@Composable
internal fun SpaceCompassQuickReturnHeader(state: SpaceCompassQuickReturnHeaderState,
    modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    val interaction = if (state.visible) Modifier else Modifier.clearAndSetSemantics { }
        .pointerInput(Unit) {
            awaitPointerEventScope {
                while (true) awaitPointerEvent(PointerEventPass.Initial).changes.forEach { it.consume() }
            }
        }
    AnimatedVisibility(state.visible, modifier.fillMaxWidth().clipToBounds().then(interaction),
        enter = expandVertically(tween(SPACE_COMPASS_ISLAND_REVEAL_DURATION_MS, easing = FastOutSlowInEasing),
            expandFrom = Alignment.Top) + fadeIn(tween(SPACE_COMPASS_ISLAND_REVEAL_DURATION_MS)),
        exit = shrinkVertically(tween(SPACE_COMPASS_ISLAND_REVEAL_DURATION_MS, easing = FastOutSlowInEasing),
            shrinkTowards = Alignment.Top) + fadeOut(tween(SPACE_COMPASS_ISLAND_REVEAL_DURATION_MS))) {
        Column(Modifier.fillMaxWidth(), content = content)
    }
}
