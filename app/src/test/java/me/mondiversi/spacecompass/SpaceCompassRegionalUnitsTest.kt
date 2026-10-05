package me.mondiversi.spacecompass

import org.junit.Assert.*
import org.junit.Test

class SpaceCompassRegionalUnitsTest {
    @Test fun phoneRegionControlsDefaultsAndUnknownRegionUsesMetric() {
        assertEquals(SpaceCompassUnits(true, "mmi", true, true, false), spaceCompassResolveUnits("US") { null })
        for (region in listOf("IT", "GB", ""))
            assertEquals(SpaceCompassUnits(false, "mkm", false, false, false), spaceCompassResolveUnits(region) { "system" })
    }
    @Test fun explicitUnitsOverridePhoneDefaultsAndLegacyDistancesStayValid() {
        val choices = mapOf("speed" to "km", "distance" to "km", "altitude" to "m", "temperature" to "c", "coordinates" to "dms")
        assertEquals(SpaceCompassUnits(false, "mkm", false, false, true), spaceCompassResolveUnits("US", choices::get))
        assertEquals("mmi", spaceCompassResolveUnits("IT") { if (it == "distance") "mi" else null }.distance)
    }
    @Test fun selectedMillionUnitsKeepAstronomicalUnitsAlongside() {
        for (unit in listOf("mkm", "mmi")) {
            val text = formatSpaceCompassSelectedDistance(SpaceCompassCelestialBody.SUN, SPACE_COMPASS_AU_KM, SpaceCompassNumericFormat.AMERICAN, unit)!!
            assertTrue(text.endsWith(" · 1 AU"))
        }
    }
}
