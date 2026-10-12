package me.mondiversi.spacecompass

import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.drag
import androidx.compose.foundation.MutatePriority
import androidx.compose.foundation.lazy.staggeredgrid.LazyStaggeredGridState
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Job
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch

/** Use the grid's pixel estimates so unequal-height islands retain their masonry layout. */
internal fun Modifier.staggeredScrollbarOverlay(state: LazyStaggeredGridState, color: Color): Modifier =
    drawWithContent {
        drawContent()
        if (size.height <= 0f || size.width <= 0f || (!state.canScrollBackward && !state.canScrollForward)) {
            return@drawWithContent
        }
        val indicator = state.scrollIndicatorState ?: return@drawWithContent
        val viewport = indicator.viewportSize
        val content = indicator.contentSize
        val offset = indicator.scrollOffset
        if (viewport <= 0 || content <= 0 || viewport == Int.MAX_VALUE || content == Int.MAX_VALUE ||
            offset == Int.MAX_VALUE) return@drawWithContent
        val thumbHeight = spaceCompassScrollbarThumbHeight(size.height,
            viewport.toFloat() / content, 28.dp.toPx())
        val fraction = when {
            !state.canScrollBackward -> 0f
            !state.canScrollForward -> 1f
            else -> (offset.toFloat() / (content - viewport).coerceAtLeast(1)).coerceIn(0f, 1f)
        }
        val width = 3.dp.toPx()
        drawRoundRect(color, Offset(size.width - width - 2.dp.toPx(), (size.height - thumbHeight) * fraction),
            Size(width, thumbHeight), CornerRadius(width, width))
    }.pointerInput(state) {
        val touchWidth = 16.dp.toPx()
        coroutineScope {
            var scrollJob: Job? = null
            awaitEachGesture {
                val down = awaitFirstDown(requireUnconsumed = false)
                if ((!state.canScrollBackward && !state.canScrollForward) ||
                    down.position.x < size.width - touchWidth) return@awaitEachGesture
                down.consume()
                fun requestScroll(pointerY: Float) {
                    val indicator = state.scrollIndicatorState ?: return
                    val content = indicator.contentSize
                    val viewport = indicator.viewportSize
                    val offset = indicator.scrollOffset
                    if (size.height <= 0 || content == Int.MAX_VALUE || viewport == Int.MAX_VALUE ||
                        offset == Int.MAX_VALUE || content <= viewport) return
                    val fraction = (pointerY / size.height).coerceIn(0f, 1f)
                    val delta = fraction * (content - viewport) - offset
                    scrollJob?.cancel()
                    scrollJob = launch {
                        // The grid's content drag must not cancel a gesture on its scrollbar.
                        state.scroll(MutatePriority.PreventUserInput) { scrollBy(delta) }
                    }
                }
                requestScroll(down.position.y)
                drag(down.id) { change ->
                    requestScroll(change.position.y)
                    change.consume()
                }
            }
        }
    }
