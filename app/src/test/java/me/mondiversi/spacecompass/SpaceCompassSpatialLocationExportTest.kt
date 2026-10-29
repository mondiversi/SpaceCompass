package me.mondiversi.spacecompass

import java.time.Instant
import java.time.ZoneId
import org.junit.Assert.*
import org.junit.Test

class SpaceCompassSpatialLocationExportTest {
    private val now = Instant.parse("2027-07-04T12:00:00Z").toEpochMilli()
    private val old = spaceCompassObserverPlan(45.0, 9.0, 110.0,
        Instant.parse("2026-01-01T08:00:00Z").toEpochMilli(), "Europe/Rome")!!
    private val inactive = old.copy(simulatePosition=false,simulateAltitude=false,simulateZone=false,
        simulateDate=false,simulateTime=false)
    private val base = SpaceCompassScenarioArchive("Location",inactive,
        SpaceCompassScenarioTimeSettings(false,-3600,SpaceCompassScenarioResume.ALWAYS))
    private val current = SpaceCompassScenarioCurrentValues(SpaceCompassDevicePlace(28.35,-14.1,102.5),102.5,
        ZoneId.of("Atlantic/Canary"),now,1)

    @Test fun anAutomaticExportContainsOnlyTheCurrentSpatialValues() {
        val snapshot=base.withAutomaticLocation(current)!!
        val c=snapshot.configuration
        assertEquals(28.35,c.latitude,0.0);assertEquals(-14.1,c.longitude,0.0);assertEquals(102.5,c.altitudeMeters,0.0)
        assertEquals("Atlantic/Canary",c.zoneId)
        assertTrue(c.simulatePosition && c.simulateAltitude && c.simulateZone)
        assertFalse(c.simulateDate || c.simulateTime || c.advanceTime)
        assertEquals(0L,c.timeMs);assertEquals("UTC",c.momentZoneId);assertNull(c.clockOffsetMs)
        assertEquals(SpaceCompassScenarioTimeSettings(),snapshot.timeSettings)
        assertFalse(snapshot.temporalSettingsIncluded)
    }

    @Test fun dormantSavedPlacesAndDatesAreNotMistakenForTheRealLocation() {
        val owner=SpaceCompassObserverState(null,elapsedClock={0L})
        owner.apply(old);owner.apply(null);owner.devicePlace=current.place
        val configured=SpaceCompassScenarioArchive.capture(owner,"Live",now,ZoneId.of("UTC"))
        val snapshot=configured.withAutomaticLocation(current)!!
        assertEquals(current.place!!.latitude,snapshot.configuration.latitude,0.0)
        assertNotEquals(old.latitude,snapshot.configuration.latitude,0.0)
        assertEquals(old,owner.savedPlan);assertNull(owner.plan)
        assertEquals(0L,snapshot.configuration.timeMs)
    }

    @Test fun theSpatialFlagRoundTripsWhileLegacyReadersStillHaveTheirRequiredFields() {
        val snapshot=base.withAutomaticLocation(current)!!
        val text=snapshot.encode()
        assertEquals(snapshot,SpaceCompassScenarioArchive.decode(text))
        for(key in listOf("momentMs=0","momentZoneId=UTC","dateEnabled=false","timeEnabled=false",
            "speedEnabled=false","speedRate=1","resume=RESTART","temporalSettingsIncluded=false"))
            assertTrue(key,text.lineSequence().any { it==key })
        val legacy=SpaceCompassScenarioArchive.decode(text.lineSequence()
            .filterNot { it.startsWith("temporalSettingsIncluded=") }.joinToString("\n"))
        assertTrue(legacy.temporalSettingsIncluded)
        assertFalse(legacy.configuration.hasCustomMoment)
    }

    @Test fun importingAPlaceReturnsToLiveTimeWithoutReplacingTheRecipientsResumeChoice() {
        val recipient=SpaceCompassObserverState(null,elapsedClock={0L})
        recipient.apply(old)
        recipient.configureTime(SpaceCompassScenarioTimeSettings(true))
        recipient.configureTime(recipient.timeSettings.copy(rate=-60,resume=SpaceCompassScenarioResume.ACTIVE))
        recipient.importArchive(base.withAutomaticLocation(current)!!)
        assertFalse(recipient.timeSettings.enabled)
        assertEquals(-60,recipient.timeSettings.rate)
        assertEquals(SpaceCompassScenarioResume.ACTIVE,recipient.timeSettings.resume)
        assertNull(recipient.plan!!.scenarioClock)
        assertEquals(now,recipient.plan!!.observationTimeMs(now,ZoneId.of("Europe/Rome")))
        assertEquals(now+86_400_000L,recipient.plan!!.observationTimeMs(now+86_400_000L,ZoneId.of("Europe/Rome")))
        assertEquals(ZoneId.of("Atlantic/Canary"),recipient.plan!!.observationZone(ZoneId.of("Europe/Rome")))
    }

    @Test fun malformedOrContradictorySpatialArchivesAreRejectedBeforeImport() {
        val text=base.withAutomaticLocation(current)!!.encode()
        for(changed in listOf(text+"temporalSettingsIncluded=1\n",
            text.replace("timeEnabled=false","timeEnabled=true"),
            text.replace("resume=RESTART","resume=ALWAYS"),text.replace("momentMs=0","momentMs=$now")))
            assertThrows(IllegalArgumentException::class.java) { SpaceCompassScenarioArchive.decode(changed) }
    }

    @Test fun missingOrInvalidPositionAndAltitudeNeverProduceInventedCoordinates() {
        for(values in listOf(current.copy(place=null),current.copy(altitudeMeters=null),
            current.copy(altitudeMeters=Double.NaN),current.copy(altitudeMeters=30_000.0),
            current.copy(place=SpaceCompassDevicePlace(91.0,0.0,100.0))))
            assertNull(base.withAutomaticLocation(values))
    }

    @Test fun explicitlyConfiguredLocationOrTimeSpeedKeepsItsOriginalExportSemantics() {
        val configured=base.copy(configuration=old)
        assertSame(configured,configured.withAutomaticLocation(current))
        val timed=base.copy(timeSettings=SpaceCompassScenarioTimeSettings(true,3600,SpaceCompassScenarioResume.ALWAYS))
        assertSame(timed,timed.withAutomaticLocation(current))
        assertTrue(SpaceCompassScenarioArchive.decode(configured.encode()).temporalSettingsIncluded)
        assertFalse(configured.encode().contains("temporalSettingsIncluded="))
    }

    @Test fun celestialObjectsStayOptionalAndAreIndependentFromTheSpatialSnapshot() {
        val selection=SpaceCompassCelestialSelection(setOf(SpaceCompassCelestialBody.MOON),SpaceCompassCelestialBody.MOON)
        val snapshot=base.copy(celestialSelection=selection).withAutomaticLocation(current)!!
        assertEquals(selection,SpaceCompassScenarioArchive.decode(snapshot.encode()).celestialSelection)
        assertNull(base.withAutomaticLocation(current)!!.celestialSelection)
    }

    @Test fun readableCommentsAndSuggestedNamesExcludeAllTemporalFields() {
        val snapshot=base.withAutomaticLocation(current)!!
        val labels=mapOf(R.string.observer_place to "Position",R.string.observer_altitude to "Altitude",
            R.string.observer_zone to "Time zone",R.string.observer_date to "Date",R.string.observer_time to "Time",
            R.string.scenario_speed to "Speed",R.string.scenario_resume to "Resume")
        val profile=SpaceCompassDataExportProfile(spaceCompassPanoramaInternationalFormatting,SpaceCompassUnits(),
            SpaceCompassTextResources { id,_ -> labels[id] ?: "Other" })
        assertEquals(listOf("Position","Altitude","Time zone"),spaceCompassScenarioExportFields(snapshot,profile).map { it.first })
        val text=spaceCompassScenarioExportText(snapshot,SpaceCompassPanoramaExportMode.INTERNATIONAL,profile)
        for(label in listOf("Date","Time","Speed","Resume")) assertFalse(text.contains("# $label:"))
        assertEquals(snapshot,SpaceCompassScenarioArchive.decode(text))
        val name=spaceCompassScenarioExportSuggestedName(snapshot,profile,"Canary Islands")
        assertFalse(name.contains("Date"));assertFalse(name.contains("Speed"));assertFalse(name.contains("Resume"))
    }
}
