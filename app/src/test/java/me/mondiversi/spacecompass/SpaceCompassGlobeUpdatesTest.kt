package me.mondiversi.spacecompass

import org.junit.Assert.*
import org.junit.Test

class SpaceCompassGlobeUpdatesTest {
    @Test fun aBurstOfMotionAndLayoutUpdatesKeepsOnlyTheLatestWithoutResendingTracks() {
        val updates = SpaceCompassGlobeUpdates()
        val content = "{\"tracks\":[{\"id\":1}]}"
        updates.update(content, "[800,400]", "[0,0,1]")
        assertTrue(updates.drain()!!.contains("\"tracks\""))
        for (i in 1..120) updates.update(content, "[${800-i},400]", "[$i,0,1]")
        assertEquals("{\"frame\":[680,400],\"pose\":[120,0,1]}", updates.drain())
        assertNull(updates.drain())
    }

    @Test fun selectionsAndTrackChangesTravelWithTheLatestPoseInOneBatch() {
        val updates = SpaceCompassGlobeUpdates()
        updates.update("{\"selected\":false}", "{}", "[0,0,1]"); updates.drain()
        updates.update("{\"selected\":true}", "{}", "[2,0,1]")
        assertEquals("{\"content\":{\"selected\":true},\"pose\":[2,0,1]}", updates.drain())
    }

    @Test fun reloadingTheRendererResendsTheCurrentCompleteStateOnce() {
        val updates = SpaceCompassGlobeUpdates()
        updates.update("{}", "{\"canvasSize\":[400,800]}", "[1,0,2]")
        val first = updates.drain()
        assertNull(updates.drain())
        updates.invalidate()
        assertEquals(first, updates.drain())
        assertNull(updates.drain())
    }
}
