package me.mondiversi.spacecompass

import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest

/** Scope the browsing throttle to catalogue rows; main-sky markers and Observatory stay live. */
internal val LocalSpaceCompassCatalogLoadsAllowed = staticCompositionLocalOf { true }

/** Cached pixels remain visible. Start new optional work only after gestures/flings settle. */
@Composable
internal fun rememberSpaceCompassCatalogLoadsAllowed(scroll: LazyListState): Boolean {
    var allowed by remember(scroll) { mutableStateOf(!scroll.isScrollInProgress) }
    LaunchedEffect(scroll) {
        snapshotFlow { scroll.isScrollInProgress }.collectLatest { moving ->
            if (moving) allowed = false
            else {
                delay(160)
                allowed = true
            }
        }
    }
    return allowed
}
