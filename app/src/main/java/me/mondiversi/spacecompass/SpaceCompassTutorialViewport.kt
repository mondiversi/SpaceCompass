package me.mondiversi.spacecompass

import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned

/** Measure the actual sky column, independently of which control the guide explains. */
internal fun Modifier.spaceCompassTutorialViewport(): Modifier = composed {
    val tutorial = LocalSpaceCompassTutorial.current
    if (tutorial?.active != true) return@composed this
    onGloballyPositioned { coordinates ->
        val bounds = coordinates.boundsInRoot()
        if (tutorial.active && bounds.width > 0 && bounds.height > 0) tutorial.skyViewport = bounds
    }
}
