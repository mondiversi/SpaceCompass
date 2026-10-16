package me.mondiversi.spacecompass

import androidx.compose.foundation.lazy.staggeredgrid.LazyStaggeredGridState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/** One full-width summary precedes the five editable sections. */
internal enum class SpaceCompassObserverSection { POSITION, ALTITUDE, ZONE, DATE, TIME;
    val gridIndex: Int get() = ordinal + 1
}

internal class SpaceCompassObserverIslandReveal(private val scope: CoroutineScope,
    private val scroll: LazyStaggeredGridState) {
    private var job: Job? = null
    private var target: Int? = null
    fun cancel() { job?.cancel(); job = null; target = null }
    fun changed(section: SpaceCompassObserverSection, expanded: Boolean) {
        changed(section.gridIndex, expanded)
    }
    fun changed(index: Int, expanded: Boolean) {
        if (!expanded) { if (target == index) cancel(); return }
        cancel()
        target = index
        job = scope.launch {
            // Align against the final expanded height; preserve the existing zero bottom padding.
            delay(SPACE_COMPASS_ISLAND_REVEAL_DURATION_MS.toLong())
            if (!scroll.isScrollInProgress) scroll.animateScrollToItem(index)
        }
    }
}

@Composable
internal fun rememberSpaceCompassObserverIslandReveal(scroll: LazyStaggeredGridState): SpaceCompassObserverIslandReveal {
    val scope = rememberCoroutineScope()
    return remember(scope, scroll) { SpaceCompassObserverIslandReveal(scope, scroll) }
}
