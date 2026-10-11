package me.mondiversi.spacecompass

import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry

/** Suspend retained page owners while the independent saver and parent-owned audio remain active. */
internal class SpaceCompassScreensaverContentLifecycle(
    private val parent: Lifecycle,
    initiallyPaused: Boolean,
    registryFactory: (LifecycleOwner) -> LifecycleRegistry = { LifecycleRegistry(it) }
) : LifecycleOwner {
    private val registry = registryFactory(this)
    override val lifecycle: Lifecycle get() = registry
    private var paused = initiallyPaused
    private var attached = false
    private var released = false
    private val observer = LifecycleEventObserver { _, _ -> sync() }

    init { sync() }

    fun attach() {
        if (attached || released) return
        attached = true
        parent.addObserver(observer)
        sync()
    }

    fun setPaused(value: Boolean) {
        if (released || paused == value) return
        paused = value
        sync()
    }

    private fun sync() {
        val state = parent.currentState
        registry.currentState = if (paused && state.isAtLeast(Lifecycle.State.STARTED)) Lifecycle.State.CREATED else state
    }

    fun release() {
        if (released) return
        released = true
        parent.removeObserver(observer)
        // A never-created owner cannot jump directly from INITIALIZED to DESTROYED.
        if (registry.currentState == Lifecycle.State.INITIALIZED) registry.currentState = Lifecycle.State.CREATED
        registry.currentState = Lifecycle.State.DESTROYED
    }
}
