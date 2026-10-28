package me.mondiversi.spacecompass

import org.junit.Assert.*
import org.junit.Test

class SpaceCompassScenarioNamingTest {
    private val base = SpaceCompassObserverPlan(28.05, -14.35, 21.0, 1800000000000L, "Atlantic/Canary",
        simulatePosition = false, simulateTime = false, simulateAltitude = false, simulateZone = false, simulateDate = false)
    private val parts = SpaceCompassScenarioNameParts("Morro Jable", "Quota-21m", "Fuso-UTC+00", "Data-15-12-26", "Ora-18.30", "Tempo-−60x")
    private fun archive(plan: SpaceCompassObserverPlan = base, speed: Boolean = false) =
        SpaceCompassScenarioArchive("Scenario", plan, SpaceCompassScenarioTimeSettings(speed, -60))
    private fun name(archive: SpaceCompassScenarioArchive, values: SpaceCompassScenarioNameParts = parts) =
        spaceCompassScenarioSuggestedName(archive, values, "Scenario attuale")

    @Test fun aPositionOnlyNameDoesNotIncludeDormantOrInferredParameters() {
        assertEquals("Morro-Jable", name(archive(base.copy(simulatePosition = true))))
    }
    @Test fun allConfiguredSectionsAndTheEnabledTimeSpeedFormACompactName() {
        val c = base.copy(simulatePosition = true, simulateAltitude = true, simulateZone = true, simulateDate = true, simulateTime = true)
        assertEquals("Morro-Jable_Quota-21m_Fuso-UTC+00_Data-15-12-26_Ora-18.30_Tempo-−60x", name(archive(c, true)))
    }
    @Test fun timeSpeedAndDateWorkWithoutAnyPosition() {
        assertEquals("Data-15-12-26_Tempo-−60x", name(archive(base.copy(simulateDate = true), true)))
        assertEquals("Tempo-−60x", name(archive(speed = true)))
    }
    @Test fun longPlaceNamesStillLeaveRoomForAllShortParametersAndTheArchiveLimit() {
        val c = base.copy(simulatePosition = true, simulateAltitude = true, simulateZone = true, simulateDate = true, simulateTime = true)
        val result = name(archive(c, true), parts.copy(position = "Very long municipal place name ".repeat(10)))
        assertTrue(result.length <= 80)
        assertTrue(result.endsWith("_Quota-21m_Fuso-UTC+00_Data-15-12-26_Ora-18.30_Tempo-−60x"))
        assertEquals(result + SPACE_COMPASS_SCENARIO_EXTENSION, spaceCompassScenarioFileName(result))
    }
    @Test fun unsafeCharactersAreRemovedWithoutLosingUnicodePlaceNames() {
        val result = name(archive(base.copy(simulatePosition = true)), parts.copy(position = "  São\nTomé / Príncipe:*?  "))
        assertTrue(result.startsWith("São-Tomé"))
        assertTrue(result.none { it in "\\/:*?\"<>|" || it.isISOControl() || it.isWhitespace() })
        assertTrue(result.isNotBlank())
    }
    @Test fun fallbackCoordinatesKeepDecimalCommasAndBothHemispheres() {
        assertEquals("28,05°N-14,35°W", name(archive(base.copy(simulatePosition = true)), parts.copy(position = "28,05°N 14,35°W")))
    }
    @Test fun anUnmodifiedScenarioUsesTheLocalizedCurrentScenarioFallback() {
        assertEquals("Scenario-attuale", name(archive()))
    }
}
