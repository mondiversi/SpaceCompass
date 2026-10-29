package me.mondiversi.spacecompass

import org.junit.Assert.*
import org.junit.Test

class SpaceCompassCatalogFilterCountsTest {
    private val sun=SpaceCompassCelestialBody.SUN
    private val moon=SpaceCompassCelestialBody.MOON
    private val mars=SpaceCompassCelestialBody.MARS
    private val comet=SpaceCompassCelestialBody.HALLEY
    private val fiction=SpaceCompassCelestialBody.LV_426
    private val roadster=SpaceCompassCelestialBody.TESLA_ROADSTER
    private val bodies=listOf(sun,moon,mars,comet,fiction,roadster)
    private val elevations=mapOf(sun to 20.0,moon to -5.0,mars to -2.0,comet to 30.0,fiction to null,roadster to 0.0)
    private val directions=mapOf(sun to true,moon to true,mars to false,comet to true,fiction to false,roadster to true)
    private fun counts(scope:List<SpaceCompassCelestialBody> = bodies,selected:Set<SpaceCompassCelestialBody> = emptySet(),
        types:Set<SpaceCompassCatalogType> = emptySet(),visibility:SpaceCompassCatalogVisibility=SpaceCompassCatalogVisibility.ALL,
        origin:SpaceCompassCatalogOrigin=SpaceCompassCatalogOrigin.ALL,pointing:SpaceCompassCatalogPointing=SpaceCompassCatalogPointing.ALL,
        search:Set<SpaceCompassCelestialBody> = bodies.toSet(),positions:Map<SpaceCompassCelestialBody,Boolean> = directions) =
        spaceCompassCatalogFilterCounts(scope,selected,types,visibility,origin,pointing,elevations,positions,search)

    @Test fun everyFilterChoiceIncludingAllHasACounter() {
        val c=counts()
        assertEquals(SpaceCompassCatalogType.entries.toSet(),c.types.keys)
        assertEquals(SpaceCompassCatalogVisibility.entries.toSet(),c.visibility.keys)
        assertEquals(SpaceCompassCatalogOrigin.entries.toSet(),c.origin.keys)
        assertEquals(SpaceCompassCatalogPointing.entries.toSet(),c.pointing.keys)
        assertEquals(6,c.allTypes)
    }

    @Test fun horizonOriginAndPointingTotalsHaveKnownIndependentValues() {
        val c=counts()
        assertEquals(3,c.visibility[SpaceCompassCatalogVisibility.ABOVE])
        assertEquals(2,c.visibility[SpaceCompassCatalogVisibility.BELOW])
        assertEquals(5,c.origin[SpaceCompassCatalogOrigin.REAL])
        assertEquals(1,c.origin[SpaceCompassCatalogOrigin.FICTION])
        assertEquals(4,c.pointing[SpaceCompassCatalogPointing.AVAILABLE])
        assertEquals(2,c.pointing[SpaceCompassCatalogPointing.UNAVAILABLE])
    }

    @Test fun choicesRespectAllOtherFiltersAndTheSearchScope() {
        val c=counts(types=setOf(SpaceCompassCatalogType.STAR,SpaceCompassCatalogType.NATURAL_SATELLITE),
            visibility=SpaceCompassCatalogVisibility.ABOVE,origin=SpaceCompassCatalogOrigin.REAL,
            pointing=SpaceCompassCatalogPointing.AVAILABLE,search=setOf(sun,comet,fiction))
        assertEquals(2,c.allTypes)
        assertEquals(1,c.types[SpaceCompassCatalogType.STAR])
        assertEquals(1,c.types[SpaceCompassCatalogType.COMET])
        assertEquals(0,c.types[SpaceCompassCatalogType.PLANET])
        assertEquals(0,c.visibility[SpaceCompassCatalogVisibility.BELOW])
        assertEquals(0,c.origin[SpaceCompassCatalogOrigin.FICTION])
        assertEquals(0,c.pointing[SpaceCompassCatalogPointing.UNAVAILABLE])
    }

    @Test fun favoriteScopesExcludeOtherCheckedObjectsButRetainTheirOwnCheckedExceptions() {
        val c=counts(scope=listOf(sun,moon),selected=setOf(sun,mars),search=emptySet())
        assertEquals(1,c.allTypes)
        assertTrue(c.types.values.all { it==1 })
        assertTrue(c.visibility.values.all { it==1 })
        assertEquals(0,counts(scope=emptyList(),selected=bodies.toSet()).allTypes)
    }

    @Test fun pendingPointingIsNotCountedAsAnUnavailableResult() {
        val c=counts(positions=emptyMap())
        assertEquals(6,c.pointing[SpaceCompassCatalogPointing.ALL])
        assertEquals(0,c.pointing[SpaceCompassCatalogPointing.AVAILABLE])
        assertEquals(0,c.pointing[SpaceCompassCatalogPointing.UNAVAILABLE])
    }

    @Test fun currentChoiceCountsAgreeWithActualVisibleRowsForCombinedFiltersAndOverlappingCategories() {
        val typeSelections=listOf(emptySet(),setOf(SpaceCompassCatalogType.STAR),
            setOf(SpaceCompassCatalogType.NATURAL_SATELLITE),setOf(SpaceCompassCatalogType.SCIENCE_FICTION),
            setOf(SpaceCompassCatalogType.SCIENCE_FICTION,SpaceCompassCatalogType.NATURAL_SATELLITE))
        for(scope in listOf(bodies,listOf(sun,moon,fiction),emptyList()))
        for(selected in listOf(emptySet(),setOf(sun,moon)))
        for(search in listOf(bodies.toSet(),setOf(sun,fiction),emptySet()))
        for(types in typeSelections)
        for(v in SpaceCompassCatalogVisibility.entries)
        for(o in SpaceCompassCatalogOrigin.entries)
        for(p in SpaceCompassCatalogPointing.entries) {
            val c=counts(scope,selected,types,v,o,p,search)
            val matched=spaceCompassFilterCatalog(types,v,elevations,scope,o,p,directions).filter { it in search }.toSet()
            val actual=spaceCompassCatalogDisplayOrder(scope,matched,selected).size
            assertEquals(actual,c.visibility.getValue(v))
            assertEquals(actual,c.origin.getValue(o))
            assertEquals(actual,c.pointing.getValue(p))
            if(types.isEmpty()) assertEquals(actual,c.allTypes)
            if(types.size==1) assertEquals(actual,c.types.getValue(types.single()))
        }
    }
}
