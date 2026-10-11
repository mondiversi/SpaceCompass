package me.mondiversi.spacecompass

import androidx.lifecycle.*
import org.junit.Assert.*
import org.junit.Test

class SpaceCompassScreensaverContentLifecycleTest {
    private class Parent(state: Lifecycle.State = Lifecycle.State.RESUMED) : LifecycleOwner {
        val registry = LifecycleRegistry.createUnsafe(this).apply { currentState = state }
        override val lifecycle: Lifecycle get() = registry
    }
    private fun child(parent: Parent, paused: Boolean = false) =
        SpaceCompassScreensaverContentLifecycle(parent.lifecycle, paused, LifecycleRegistry::createUnsafe).also { it.attach() }

    @Test fun saverPausesAndStopsPageListenersButDoesNotPauseTheParent() {
        val parent = Parent()
        val child = child(parent)
        val events = mutableListOf<Lifecycle.Event>()
        child.lifecycle.addObserver(LifecycleEventObserver { _, event -> events += event })
        events.clear()
        child.setPaused(true)
        assertEquals(listOf(Lifecycle.Event.ON_PAUSE, Lifecycle.Event.ON_STOP), events)
        assertEquals(Lifecycle.State.CREATED, child.lifecycle.currentState)
        assertEquals(Lifecycle.State.RESUMED, parent.lifecycle.currentState)
        child.setPaused(false)
        assertEquals(listOf(Lifecycle.Event.ON_PAUSE, Lifecycle.Event.ON_STOP,
            Lifecycle.Event.ON_START, Lifecycle.Event.ON_RESUME), events)
        child.release()
    }

    @Test fun rotationWithAnAlreadyVisibleSaverNeverStartsPageSensors() {
        val parent = Parent()
        val child = child(parent, paused = true)
        val events = mutableListOf<Lifecycle.Event>()
        child.lifecycle.addObserver(LifecycleEventObserver { _, event -> events += event })
        assertEquals(listOf(Lifecycle.Event.ON_CREATE), events)
        child.release()
    }

    @Test fun leavingSaverWhileTheAppIsBackgroundedCannotRestartPageWork() {
        val parent = Parent()
        val child = child(parent)
        child.setPaused(true)
        parent.registry.currentState = Lifecycle.State.CREATED
        child.setPaused(false)
        assertEquals(Lifecycle.State.CREATED, child.lifecycle.currentState)
        parent.registry.currentState = Lifecycle.State.RESUMED
        assertEquals(Lifecycle.State.RESUMED, child.lifecycle.currentState)
        child.release()
    }

    @Test fun repeatedIdleChangesDoNotDuplicateLifecycleTransitions() {
        val parent = Parent()
        val child = child(parent)
        val events = mutableListOf<Lifecycle.Event>()
        child.lifecycle.addObserver(LifecycleEventObserver { _, event -> events += event })
        events.clear()
        repeat(3) { child.setPaused(true) }
        assertEquals(listOf(Lifecycle.Event.ON_PAUSE, Lifecycle.Event.ON_STOP), events)
        events.clear()
        repeat(3) { child.setPaused(false) }
        assertEquals(listOf(Lifecycle.Event.ON_START, Lifecycle.Event.ON_RESUME), events)
        child.release()
    }

    @Test fun releasingTheRetainedPageRemovesItsParentObserver() {
        val parent = Parent()
        val child = child(parent)
        assertEquals(1, parent.registry.observerCount)
        child.release(); child.release()
        assertEquals(0, parent.registry.observerCount)
        assertEquals(Lifecycle.State.DESTROYED, child.lifecycle.currentState)
    }

    @Test fun parentOwnedMusicContinuesAcrossPagePauseAndResume() {
        val parent = Parent()
        val music = SpaceCompassAmbientMusicPolicy()
        parent.lifecycle.addObserver(LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> music.setForeground(true)
                Lifecycle.Event.ON_PAUSE, Lifecycle.Event.ON_STOP -> music.setForeground(false)
                else -> Unit
            }
        })
        music.onFocus(SpaceCompassMusicFocus.GRANTED)
        val child = child(parent)
        child.setPaused(true)
        assertTrue(music.canPlay)
        child.setPaused(false)
        assertTrue(music.canPlay)
        parent.registry.currentState = Lifecycle.State.CREATED
        assertFalse(music.canPlay)
        child.release()
    }
}
