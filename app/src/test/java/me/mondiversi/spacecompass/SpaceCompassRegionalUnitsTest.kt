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

    @Test fun unifiedChoiceOverridesEveryLegacyLengthAndSpeedChoice() {
        val oldImperial = mapOf("speed" to "mi", "distance" to "mmi", "altitude" to "ft")
        val metric = oldImperial + (SPACE_COMPASS_DISTANCE_SPEED_UNIT_KEY to "metric")
        assertEquals(SpaceCompassUnits(false, "mkm", false, true, false), spaceCompassResolveUnits("US", metric::get))
        val oldMetric = mapOf("speed" to "km", "distance" to "mkm", "altitude" to "m")
        val imperial = oldMetric + (SPACE_COMPASS_DISTANCE_SPEED_UNIT_KEY to "imperial")
        assertEquals(SpaceCompassUnits(true, "mmi", true, false, false), spaceCompassResolveUnits("IT", imperial::get))
    }

    @Test fun unifiedSystemChoiceIgnoresLegacyOverridesAndFollowsDeviceRegion() {
        val saved = mapOf(SPACE_COMPASS_DISTANCE_SPEED_UNIT_KEY to "system", "distance" to "mmi", "altitude" to "ft", "speed" to "mi")
        assertEquals(SpaceCompassUnits(false, "mkm", false, false, false), spaceCompassResolveUnits("IT", saved::get))
        assertEquals(SpaceCompassUnits(true, "mmi", true, true, false), spaceCompassResolveUnits("US", saved::get))
    }

    @Test fun legacyMigrationHasDeterministicDistanceThenLengthThenSpeedPriority() {
        val mixed = mapOf("distance" to "mmi", "altitude" to "m", "speed" to "km")
        assertEquals("imperial", spaceCompassDistanceSpeedPreference(mixed::get))
        val metric = mixed + ("distance" to "mkm")
        assertEquals("metric", spaceCompassDistanceSpeedPreference(metric::get))
        val normalOnly = mapOf("distance" to "default", "altitude" to "ft", "speed" to "km")
        assertEquals("imperial", spaceCompassDistanceSpeedPreference(normalOnly::get))
        assertEquals("imperial", spaceCompassDistanceSpeedPreference { if (it == "speed") "mi" else null })
        assertEquals("metric", spaceCompassDistanceSpeedPreference { if (it == "speed") "km" else null })
    }

    @Test fun missingInvalidOrFormerAutomaticChoicesMigrateToSystem() {
        assertEquals("system", spaceCompassDistanceSpeedPreference { null })
        assertEquals("system", spaceCompassDistanceSpeedPreference { "system" })
        assertEquals("system", spaceCompassDistanceSpeedPreference { "default" })
        assertEquals("system", spaceCompassDistanceSpeedPreference { "invalid" })
        assertEquals("metric", spaceCompassDistanceSpeedPreference {
            if (it == "distance") "mkm" else "invalid"
        })
    }

    @Test fun temperatureAndCoordinatesRemainIndependentOfUnifiedLengthUnits() {
        val saved = mapOf(SPACE_COMPASS_DISTANCE_SPEED_UNIT_KEY to "imperial", "temperature" to "c", "coordinates" to "dms")
        assertEquals(SpaceCompassUnits(true, "mmi", true, false, true), spaceCompassResolveUnits("US", saved::get))
    }
}
