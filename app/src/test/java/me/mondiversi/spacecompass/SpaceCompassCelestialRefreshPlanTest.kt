package me.mondiversi.spacecompass

import org.junit.Assert.*
import org.junit.Test

class SpaceCompassCelestialRefreshPlanTest {
    private val allRemote = setOf(SpaceCompassCelestialBody.ISS, SpaceCompassCelestialBody.STARLINK_V3,
        SpaceCompassCelestialBody.SEDNA, SpaceCompassCelestialBody.HALLEY, SpaceCompassCelestialBody.COMET_67P,
        SpaceCompassCelestialBody.VOYAGER_1, SpaceCompassCelestialBody.VOYAGER_2)

    @Test fun uncheckedCometsProbesAndSatellitesAreWarmedEvenWithEmptySelection() {
        val plan = spaceCompassCelestialRefreshBodies(emptySet(), emptySet(), true)
        assertEquals(allRemote, plan.toSet())
        assertEquals(allRemote.size, plan.size)
        assertEquals(listOf(SpaceCompassCelestialBody.ISS, SpaceCompassCelestialBody.STARLINK_V3), plan.take(2))
        assertFalse(plan.contains(SpaceCompassCelestialBody.SUN))
        assertFalse(plan.contains(SpaceCompassCelestialBody.POLARIS))
    }
    @Test fun selectedObjectsStayFirstAndSelectionDoesNotExpand() {
        val selected = setOf(SpaceCompassCelestialBody.SUN, SpaceCompassCelestialBody.STARLINK_V3, SpaceCompassCelestialBody.HALLEY)
        val plan = spaceCompassCelestialRefreshBodies(selected, setOf(SpaceCompassCelestialBody.HALLEY), true)
        assertEquals(listOf(SpaceCompassCelestialBody.HALLEY, SpaceCompassCelestialBody.STARLINK_V3), plan.take(2))
        assertEquals(allRemote, plan.toSet())
        assertEquals(3, selected.size)
    }
    @Test fun optOutPreservesSingleObjectDownloadScope() {
        assertEquals(listOf(SpaceCompassCelestialBody.HALLEY),
            spaceCompassCelestialRefreshBodies(setOf(SpaceCompassCelestialBody.HALLEY, SpaceCompassCelestialBody.SUN), emptySet(), false))
        assertTrue(spaceCompassCelestialRefreshBodies(emptySet(), emptySet(), false).isEmpty())
    }
    @Test fun uncheckedHorizonsObjectsGetSolarDistanceAndSpeedBeforePointing() {
        for (body in allRemote.filter { it.usesHorizons }) {
            val requests = spaceCompassCelestialRefreshRequests(body, emptySet(), emptySet(), true, false, false)
            assertEquals(listOf(SpaceCompassCelestialRequest(body, true), SpaceCompassCelestialRequest(body)), requests)
        }
    }
    @Test fun selectedMissingPointingAndSatelliteElementsRemainImmediatePriorities() {
        val body = SpaceCompassCelestialBody.HALLEY
        assertFalse(spaceCompassCelestialRefreshRequests(body, setOf(body), setOf(body), true, false, false).first().motion)
        assertTrue(spaceCompassCelestialRefreshRequests(body, setOf(body), setOf(body), true, true, false).first().motion)
        for (satellite in allRemote.filter { it.isEarthSatellite })
            assertEquals(listOf(SpaceCompassCelestialRequest(satellite)),
                spaceCompassCelestialRefreshRequests(satellite, emptySet(), emptySet(), true, false, false))
    }
}
