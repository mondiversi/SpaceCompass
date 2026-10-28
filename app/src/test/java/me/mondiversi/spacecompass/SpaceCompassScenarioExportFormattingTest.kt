package me.mondiversi.spacecompass

import org.junit.Assert.*
import org.junit.Test
import java.time.Instant
import java.time.ZoneId
import java.util.Locale

class SpaceCompassScenarioExportFormattingTest {
    private val moment=Instant.parse("2026-10-28T18:32:00Z").toEpochMilli()
    private val archive=SpaceCompassScenarioArchive("Scenario",SpaceCompassObserverPlan(28.05,-14.35,30.48,moment,
        "Atlantic/Canary",simulatePosition=true,simulateAltitude=true,simulateZone=true,simulateDate=true,
        simulateTime=true,momentZoneId="Atlantic/Canary"),
        SpaceCompassScenarioTimeSettings(true,-60,SpaceCompassScenarioResume.ALWAYS))
    private fun profile(italian: Boolean, feet: Boolean=false): SpaceCompassDataExportProfile {
        val labels=mapOf(
            R.string.observer_place to (if(italian) "Posizione" else "Position"),
            R.string.observer_altitude to (if(italian) "Quota" else "Altitude"),
            R.string.observer_zone to (if(italian) "Fuso" else "Time zone"),
            R.string.observer_date to (if(italian) "Data" else "Date"),
            R.string.observer_time to (if(italian) "Ora" else "Time"),
            R.string.scenario_speed to (if(italian) "Tempo" else "Time"),
            R.string.observer_current_scenario to (if(italian) "Scenario attuale" else "Current scenario"))
        val f=if(italian) SpaceCompassPanoramaFormatting(Locale.ITALIAN,SpaceCompassNumericFormat.EUROPEAN,
            SpaceCompassDateFormat.EUROPEAN,SpaceCompassTimeFormat.H12,feet=feet,dms=true,deviceLocale=Locale.ITALY)
            else spaceCompassPanoramaInternationalFormatting
        return SpaceCompassDataExportProfile(f,SpaceCompassUnits(feet=feet),SpaceCompassTextResources { id,_ -> labels[id] ?: "Label" })
    }

    @Test fun automaticNamesUseTheSelectedUnitsDateAndLanguageWithoutChangingPhysicalValues() {
        val enabled=archive.copy(configuration=archive.configuration.copy(simulateZone=false,simulateTime=false),
            timeSettings=archive.timeSettings.copy(enabled=false))
        assertEquals("Morro-Jable_Quota-100ft_Data-28-10-2026",spaceCompassScenarioExportSuggestedName(enabled,profile(true,true),"Morro Jable"))
        assertEquals("Morro-Jable_Altitude-30.5m_Date-2026-10-28",spaceCompassScenarioExportSuggestedName(enabled,profile(false),"Morro Jable"))
        assertEquals(30.48,enabled.configuration.altitudeMeters,0.0)
    }
    @Test fun bothHumanReadableProfilesImportIdenticallyWithCanonicalUnitsAndResumePolicy() {
        for(mode in SpaceCompassPanoramaExportMode.entries) {
            val profile=profile(mode==SpaceCompassPanoramaExportMode.SELECTED,mode==SpaceCompassPanoramaExportMode.SELECTED)
            val text=spaceCompassScenarioExportText(archive,mode,profile)
            assertEquals(archive,SpaceCompassScenarioArchive.decode(text))
            assertEquals(archive.configurationAtImport(moment,ZoneId.of("UTC")),
                SpaceCompassScenarioArchive.decode(text).configurationAtImport(moment,ZoneId.of("UTC")))
            assertTrue(text.contains("altitudeMeters=30.48"))
            assertTrue(text.contains(if(mode==SpaceCompassPanoramaExportMode.SELECTED) "# Quota: 100ft" else "# Altitude: 30.5m"))
            assertTrue(text.toByteArray(Charsets.UTF_8).size<=SPACE_COMPASS_SCENARIO_FILE_LIMIT)
        }
    }
    @Test fun automaticInferredValuesDoNotBecomeExplicitOverridesOrNameComponents() {
        val positional=archive.copy(configuration=archive.configuration.copy(simulateAltitude=false,simulateZone=false,
            simulateDate=false,simulateTime=false,automaticAltitudeMeters=120.0,automaticZoneId="Atlantic/Canary"),
            timeSettings=archive.timeSettings.copy(enabled=false))
        assertEquals("Morro-Jable",spaceCompassScenarioExportSuggestedName(positional,profile(false),"Morro Jable"))
        assertEquals(positional,SpaceCompassScenarioArchive.decode(spaceCompassScenarioExportText(positional,
            SpaceCompassPanoramaExportMode.INTERNATIONAL,profile(false))))
    }
    @Test fun backwardTimeAndNegativeAltitudesCannotLoseTheirMinusSignInNames() {
        val backward=archive.copy(configuration=archive.configuration.copy(simulatePosition=false,simulateAltitude=false,
            simulateZone=false,simulateDate=false,simulateTime=false))
        assertEquals("Time-−60x",spaceCompassScenarioExportSuggestedName(backward,profile(false)))
        assertEquals("Time-60x",spaceCompassScenarioExportSuggestedName(backward.copy(
            timeSettings=backward.timeSettings.copy(rate=60)),profile(false)))
        val underground=backward.copy(configuration=backward.configuration.copy(simulateAltitude=true,altitudeMeters=-10.0),
            timeSettings=backward.timeSettings.copy(enabled=false))
        assertEquals("Altitude-−10m",spaceCompassScenarioExportSuggestedName(underground,profile(false)))
    }

    @Test fun legacyArchivesAndUserAssignedNamesRemainCompatible() {
        val named=archive.copy(name="La mia prova 日本")
        assertEquals(named,SpaceCompassScenarioArchive.decode(named.encode()))
        assertEquals("My-test_.spacecompass",spaceCompassScenarioFileName("My-test_"))
        assertEquals("La mia prova 日本.spacecompass",spaceCompassScenarioFileName(named.name))
        assertEquals(named.name,SpaceCompassScenarioArchive.decode(spaceCompassScenarioExportText(named,
            SpaceCompassPanoramaExportMode.INTERNATIONAL,profile(false))).name)
    }
}
