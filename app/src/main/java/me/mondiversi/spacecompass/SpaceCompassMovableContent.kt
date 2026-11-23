package me.mondiversi.spacecompass

import androidx.compose.runtime.Composable
import androidx.compose.runtime.movableContentOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState

/** Retain controls and open dialogs when responsive layouts move them to another parent. */
@Composable
internal fun rememberSpaceCompassMovableContent(content: @Composable () -> Unit): @Composable () -> Unit {
    val latestContent = rememberUpdatedState(content)
    return remember { movableContentOf { latestContent.value() } }
}
