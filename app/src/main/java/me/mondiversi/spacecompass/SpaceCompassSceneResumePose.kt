package me.mondiversi.spacecompass

import androidx.compose.runtime.*
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner

internal data class SpaceCompassSceneVisualPose(
    val orientation: SpaceCompassSunOrientation?,
    val pointing: SpaceCompassSunOrientation?
)

/** Display-only memory. It never restores measurement trust or a camera capture attitude. */
internal class SpaceCompassSceneResumePose {
    private var previous: SpaceCompassSunOrientation? = null
    private var previousRotation = 0

    fun accept(orientation: SpaceCompassSunOrientation, rotation: Int) {
        previous = orientation
        previousRotation = rotation
    }

    fun resolve(current: SpaceCompassSunOrientation?, usable: Boolean, holding: Boolean,
        rotation: Int): SpaceCompassSceneVisualPose {
        val held = previous?.takeIf { holding && !usable }?.let {
            spaceCompassSceneRemapOrientation(it, previousRotation, rotation)
        }
        return SpaceCompassSceneVisualPose(held ?: current,
            held ?: spaceCompassSunTrustedPointingOrientation(current, usable))
    }
}

/** Screen rotation changes the two screen axes, never the physical rear-camera axis. */
internal fun spaceCompassSceneRemapOrientation(orientation: SpaceCompassSunOrientation,
    fromRotation: Int, toRotation: Int): SpaceCompassSunOrientation {
    fun opposite(v: SpaceCompassSunVector) = SpaceCompassSunVector(-v.east, -v.north, -v.up)
    val right = when (fromRotation) {
        1 -> orientation.screenUp
        2 -> opposite(orientation.right)
        3 -> opposite(orientation.screenUp)
        else -> orientation.right
    }
    val up = when (fromRotation) {
        1 -> opposite(orientation.right)
        2 -> opposite(orientation.screenUp)
        3 -> orientation.right
        else -> orientation.screenUp
    }
    return SpaceCompassSunOrientation(
        when (toRotation) { 1 -> opposite(up); 2 -> opposite(right); 3 -> up; else -> right },
        when (toRotation) { 1 -> right; 2 -> opposite(up); 3 -> opposite(right); else -> up },
        orientation.forward)
}

@Composable
internal fun rememberSpaceCompassSceneResumePose(orientation: SpaceCompassSunOrientation?, usable: Boolean,
    rotation: Int, active: Boolean): SpaceCompassSceneVisualPose {
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    val memory = remember { SpaceCompassSceneResumePose() }
    var awaitingResume by remember { mutableStateOf(false) }
    DisposableEffect(lifecycle) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_PAUSE || event == Lifecycle.Event.ON_STOP) awaitingResume = true
        }
        lifecycle.addObserver(observer)
        onDispose { lifecycle.removeObserver(observer) }
    }
    val visual = memory.resolve(orientation, usable, awaitingResume || !active, rotation)
    SideEffect {
        if (active && usable && orientation != null) {
            memory.accept(orientation, rotation)
            awaitingResume = false
        }
    }
    return visual
}
