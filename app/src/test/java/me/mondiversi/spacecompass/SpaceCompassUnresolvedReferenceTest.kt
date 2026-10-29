package me.mondiversi.spacecompass

import org.junit.Assert.*
import org.junit.Test

class SpaceCompassUnresolvedReferenceTest {
    @Test fun allAddedNucleiHaveOneRealHostAndNeitherSideCreatesAConjunctionWithIt() {
        val nuclei=spaceCompassLargeCatalogEntries.filter { it.body.ordinal>=1010 && it.entry.type==SpaceCompassCatalogType.BLACK_HOLE }
        assertEquals(90,nuclei.size)
        for (nucleus in nuclei) {
            val key=spaceCompassUnresolvedReferenceKey(nucleus.galaxy)
            val hosts=spaceCompassLargeCatalogEntries.filter { it.entry.type==SpaceCompassCatalogType.GALAXY &&
                spaceCompassUnresolvedReferenceKey(it.entry.designation)==key }
            assertEquals(nucleus.body.name,1,hosts.size)
            val host=hosts.single().body
            assertTrue(spaceCompassSharesUnresolvedTarget(host,nucleus.body))
            assertTrue(spaceCompassSharesUnresolvedTarget(nucleus.body,host))
        }
        assertEquals(1,spaceCompassLargeCatalogEntries.count { it.entry.type==SpaceCompassCatalogType.GALAXY &&
            (it.entry.designation=="ESO097-013" || it.entry.designation=="Circinus") })
    }

    @Test fun unresolvedPlanetsAtTheSameExplicitHostDoNotImplyAnEclipseOrConjunction() {
        val planets=spaceCompassLargeCatalogEntries.filter { it.entry.type==SpaceCompassCatalogType.EXOPLANET }
            .groupBy { it.facts.parentName }.values.first { it.size>1 }
        val a=planets[0].body;val b=planets[1].body
        assertTrue(spaceCompassSharesUnresolvedTarget(a,b))
        assertTrue(spaceCompassSharesUnresolvedTarget(SpaceCompassCelestialBody.PROXIMA_B,SpaceCompassCelestialBody.PROXIMA_CENTAURI))
        assertFalse(spaceCompassSharesUnresolvedTarget(SpaceCompassCelestialBody.SUN,SpaceCompassCelestialBody.MOON))
        val positions=mapOf(a to SpaceCompassSunPosition(100.0,30.0),b to SpaceCompassSunPosition(100.0,30.0))
        assertTrue(spaceCompassCurrentApproaches(0L,positions).isEmpty())
    }
}
