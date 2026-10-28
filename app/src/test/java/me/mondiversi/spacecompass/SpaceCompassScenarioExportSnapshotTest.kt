package me.mondiversi.spacecompass

import java.time.Instant
import java.time.ZoneId
import org.junit.Assert.*
import org.junit.Test

class SpaceCompassScenarioExportSnapshotTest {
    private val moment = Instant.parse("2027-01-06T23:30:00Z").toEpochMilli()
    private val canary = ZoneId.of("Atlantic/Canary")
    private val rome = ZoneId.of("Europe/Rome")
    private val fuerte = SpaceCompassDevicePlace(28.35, -14.1, 102.5)
    private val plan = spaceCompassObserverPlan(28.35, -14.1, 102.5, moment, canary.id)!!
    private val archive = SpaceCompassScenarioArchive("Fuerteventura", plan, SpaceCompassScenarioTimeSettings())
    private val values = SpaceCompassScenarioCurrentValues(fuerte, 102.5, canary, moment, 1)

    @Test fun liveSnapshotKeepsExporterLocationAndInstantWhenImportedElsewhere() {
        val observer = SpaceCompassObserverState(null, elapsedClock = { 0L })
        observer.devicePlace = fuerte
        val configured = SpaceCompassScenarioArchive.capture(observer, "Live", moment, canary)
        assertFalse(configured.custom)
        val effective = spaceCompassScenarioCurrentValues(observer.plan, observer.devicePlace,
            observer.timeSettings, moment, canary)
        val completed = configured.withAutomaticValues(effective)!!
        val restored = SpaceCompassScenarioArchive.decode(completed.encode())
            .configurationAtImport(moment + 86_400_000L, rome)
        val place = restored.resolvePlace(SpaceCompassDevicePlace(45.0, 9.0, 120.0))!!
        assertEquals(fuerte, place)
        assertEquals(canary, restored.observationZone(rome))
        assertEquals(moment, restored.observationTimeMs(moment + 86_400_000L, rome))
        assertEquals(5, restored.activeSections.size)
        assertNull(observer.plan); assertNull(observer.savedPlan)
    }

    @Test fun automaticValuesFollowChosenPositionRatherThanDeviceGps() {
        val selected = plan.copy(altitudeMeters = 999.0, zoneId = "Asia/Tokyo", simulateAltitude = false,
            simulateZone = false, simulateDate = false, simulateTime = false,
            automaticAltitudeMeters = 12.0, automaticZoneId = canary.id)
        val effective = spaceCompassScenarioCurrentValues(selected, SpaceCompassDevicePlace(45.0, 9.0, 120.0),
            SpaceCompassScenarioTimeSettings(), moment, rome)
        val completed = archive.copy(configuration = selected).withAutomaticValues(effective)!!.configuration
        assertEquals(28.35, completed.latitude, 0.0); assertEquals(-14.1, completed.longitude, 0.0)
        assertEquals(12.0, completed.altitudeMeters, 0.0)
        assertEquals(canary, completed.observationZone(rome))
        assertEquals(moment, completed.timeMs)
    }

    @Test fun currentClockMomentAndReverseRateArePreservedAsANewBase() {
        val settings = SpaceCompassScenarioTimeSettings(true, -3600, SpaceCompassScenarioResume.ACTIVE)
        val advanced = moment - 2 * 86_400_000L
        val completed = archive.copy(timeSettings = settings).withAutomaticValues(values.copy(instant = advanced, rate = -3600))!!
        val restored = SpaceCompassScenarioArchive.decode(completed.encode())
        assertEquals(advanced, restored.configurationAtImport(moment + 100_000L, rome).timeMs)
        assertEquals(settings, restored.timeSettings)
        assertEquals(moment, archive.configuration.timeMs)
        assertFalse(completed.encode().contains("checkpoint"))
        assertNull(restored.configuration.scenarioClock)
    }

    @Test fun allResumePoliciesAndTheClockAdvanceChoiceSurviveACompleteSnapshot() {
        for (policy in SpaceCompassScenarioResume.entries) {
            val settings = SpaceCompassScenarioTimeSettings(true, 0, policy)
            val moving = archive.copy(configuration = plan.copy(advanceTime = true, clockOffsetMs = 5_000L), timeSettings = settings)
            val completed = moving.withAutomaticValues(values.copy(rate = 0))!!
            val restored = SpaceCompassScenarioArchive.decode(completed.encode())
            assertEquals(settings, restored.timeSettings)
            val imported = restored.configurationAtImport(moment + 86_400_000L, rome)
            assertTrue(imported.advanceTime)
            assertEquals(moment, imported.observationTimeMs(moment + 86_400_000L, rome))
        }
    }

    @Test fun missingFixOrAltitudeDoesNotProduceZeroCoordinatesOrHeight() {
        assertNull(archive.withAutomaticValues(values.copy(place = null)))
        assertNull(archive.withAutomaticValues(values.copy(altitudeMeters = null)))
        val zero = archive.withAutomaticValues(values.copy(place = SpaceCompassDevicePlace(0.0, 0.0, 0.0), altitudeMeters = 0.0))!!
        assertEquals(0.0, zero.configuration.latitude, 0.0)
        assertEquals(0.0, zero.configuration.altitudeMeters, 0.0)
    }

    @Test fun nonFiniteAndUnsupportedAutomaticValuesAreRejected() {
        assertNull(archive.withAutomaticValues(values.copy(place = fuerte.copy(latitude = Double.NaN))))
        assertNull(archive.withAutomaticValues(values.copy(altitudeMeters = Double.POSITIVE_INFINITY)))
        assertNull(archive.withAutomaticValues(values.copy(altitudeMeters = 20_001.0)))
        assertNull(archive.withAutomaticValues(values.copy(instant = Long.MAX_VALUE)))
    }

    @Test fun completingAnArchiveDoesNotChangeItsSourceOrRuntimeClock() {
        val clock = SpaceCompassScenarioClock(SpaceCompassScenarioTimeSettings(true, 60), "source", moment, moment, 0L,
            elapsed = { 0L })
        plan.scenarioClock = clock
        val source = archive.encode()
        val completed = archive.withAutomaticValues(values.copy(instant = moment + 3_600_000L))!!
        assertEquals(source, archive.encode())
        assertSame(clock, plan.scenarioClock)
        assertEquals(moment, clock.at(moment, 0L))
        assertNull(completed.configuration.scenarioClock)
        assertEquals(moment + 3_600_000L, completed.configuration.timeMs)
    }
}
