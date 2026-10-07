package me.mondiversi.spacecompass

import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import kotlinx.coroutines.withTimeoutOrNull

/** No visible affordance or ordinary long-click action; releasing/cancelling early is a no-op. */
@Composable
internal fun spaceCompassHiddenObjectHold(onReveal: () -> Unit): Modifier {
    val reveal by rememberUpdatedState(onReveal)
    return Modifier.pointerInput(Unit) {
        detectTapGestures(onPress = {
            val released = withTimeoutOrNull(SPACE_COMPASS_HIDDEN_OBJECT_HOLD_MS) { tryAwaitRelease() }
            if (released == null) reveal()
        })
    }
}
