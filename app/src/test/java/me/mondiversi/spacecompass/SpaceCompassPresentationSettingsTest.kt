package me.mondiversi.spacecompass

import org.junit.Assert.*
import org.junit.Test

class SpaceCompassPresentationSettingsTest {
    @Test fun selectionAndCacheWritesDoNotChangePresentationSettings() {
        val preferences = mutableMapOf("language" to "system", "theme" to "dark", "distance" to "mmi")
        val before = spaceCompassReadPresentationSettings(preferences::get)
        preferences["celestial_active"] = "RIGEL"
        preferences["celestial_selected"] = "RIGEL,PROXIMA_CENTAURI"
        preferences["starlink_source"] = "satcat"
        assertEquals(before, spaceCompassReadPresentationSettings(preferences::get))
        preferences["language"] = "it"
        val after = spaceCompassReadPresentationSettings(preferences::get)
        assertEquals("it", after["language"])
        assertEquals("dark", after["theme"])
        assertEquals("mmi", after["distance"])
        assertNotEquals(before, after)
    }

    @Test fun missingAndClearedPreferencesKeepSystemDefaultsAndLegacyUnitChoices() {
        val empty = spaceCompassReadPresentationSettings { null }
        assertEquals(spaceCompassPresentationKeys, empty.keys)
        assertTrue(empty.values.all { it == null })
        assertEquals(SpaceCompassNumericFormat.SYSTEM, SpaceCompassNumericFormat.fromStoredValue(empty[SPACE_COMPASS_NUMERIC_FORMAT_KEY]))
        assertEquals(SpaceCompassDateFormat.SYSTEM, SpaceCompassDateFormat.fromStoredValue(empty[SPACE_COMPASS_DATE_FORMAT_KEY]))
        assertEquals(SpaceCompassTimeFormat.SYSTEM, SpaceCompassTimeFormat.fromStoredValue(empty[SPACE_COMPASS_TIME_FORMAT_KEY]))
        assertEquals(SpaceCompassUnits(true, "mmi", true, true, false), spaceCompassResolveUnits("US", empty::get))
        val legacy = spaceCompassReadPresentationSettings { if (it == "distance") "mi" else null }
        assertEquals("mmi", spaceCompassResolveUnits("IT", legacy::get).distance)
        assertEquals(empty, spaceCompassReadPresentationSettings { null })
    }
}
