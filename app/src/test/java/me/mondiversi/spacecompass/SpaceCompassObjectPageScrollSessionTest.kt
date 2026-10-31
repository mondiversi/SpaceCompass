package me.mondiversi.spacecompass

import org.junit.Assert.*
import org.junit.Test

class SpaceCompassObjectPageScrollSessionTest {
    @Test fun objectsAndPagesKeepIndependentOffsetsThroughNavigation() {
        val session = SpaceCompassObjectPageScrollSession()
        val sun = session.open(SpaceCompassCelestialBody.SUN, SpaceCompassObjectPage.OBSERVATORY)
        assertFalse(sun.returning)
        sun.entry.position.request(780)
        val path = session.open(SpaceCompassCelestialBody.SUN, SpaceCompassObjectPage.DAILY_PATH)
        path.entry.position.request(320)
        val moon = session.open(SpaceCompassCelestialBody.MOON, SpaceCompassObjectPage.OBSERVATORY)
        moon.entry.position.request(1200)
        val again = session.open(SpaceCompassCelestialBody.SUN, SpaceCompassObjectPage.OBSERVATORY)
        assertTrue(again.returning)
        assertSame(sun.entry.scroll, again.entry.scroll)
        assertEquals(780, again.entry.position.offset)
        assertEquals(320, session.open(SpaceCompassCelestialBody.SUN, SpaceCompassObjectPage.DAILY_PATH).entry.position.offset)
        assertEquals(1200, session.open(SpaceCompassCelestialBody.MOON, SpaceCompassObjectPage.OBSERVATORY).entry.position.offset)
    }

    @Test fun aNewSessionHasNoScrollOrQuickHeaderHistory() {
        val old = SpaceCompassObjectPageScrollSession().open(SpaceCompassCelestialBody.SUN, SpaceCompassObjectPage.DAILY_PATH)
        old.entry.position.request(1000); old.entry.headerVisible = false
        val fresh = SpaceCompassObjectPageScrollSession().open(SpaceCompassCelestialBody.SUN, SpaceCompassObjectPage.DAILY_PATH)
        assertFalse(fresh.returning)
        assertEquals(0, fresh.entry.position.offset)
        assertEquals(0, fresh.entry.scroll.value)
        assertTrue(fresh.entry.headerVisible)
    }

    @Test fun temporaryShortContentAndRotationDoNotEraseTheRequestedPosition() {
        val position = SpaceCompassObjectPageScrollPosition()
        assertNull(position.observe(0, 1500, false))
        assertNull(position.observe(1100, 1500, true))
        assertNull(position.observe(1100, 1500, false))
        assertNull(position.observe(300, 300, false)) // Loading view clamps to its shorter extent.
        assertEquals(1100, position.offset)
        assertEquals(1100, position.observe(300, 1500, false))
        assertNull(position.observe(1100, 1500, false))
        assertNull(position.observe(900, 900, false)) // Taller landscape viewport.
        assertEquals(1100, position.offset)
        assertEquals(1100, position.observe(900, 1500, false))
    }

    @Test fun directScrollbarAndProgrammaticChangesReplaceOldHistory() {
        val position = SpaceCompassObjectPageScrollPosition()
        position.observe(0, 2000, false)
        position.observe(800, 2000, false) // A direct scrollbar transaction can finish between frames.
        assertEquals(800, position.offset)
        position.observe(200, 2000, true)
        position.observe(180, 2000, false)
        assertEquals(180, position.offset)
        position.request(600)
        assertNull(position.observe(600, 2000, false))
        assertEquals(600, position.offset)
    }

    @Test fun scrollingAfterAClampCancelsTheOlderDeferredRestore() {
        val position = SpaceCompassObjectPageScrollPosition()
        position.observe(0, 2000, false)
        position.observe(1500, 2000, true)
        position.observe(300, 300, false)
        position.observe(120, 300, true)
        assertEquals(120, position.offset)
        assertNull(position.observe(120, 2000, false))
    }
}
