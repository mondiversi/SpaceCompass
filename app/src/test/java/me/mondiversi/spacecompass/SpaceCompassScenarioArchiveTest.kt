package me.mondiversi.spacecompass

import android.content.SharedPreferences
import java.lang.reflect.Proxy
import java.time.Instant
import java.time.ZoneId
import org.junit.Assert.*
import org.junit.Test

class SpaceCompassScenarioArchiveTest {
    private val moment = Instant.parse("2027-04-06T20:00:00Z").toEpochMilli()
    private val plan = spaceCompassObserverPlan(28.35, -14.1, 102.5, moment, "Atlantic/Canary")!!
    private val profile = SpaceCompassScenarioArchive("Canarie", plan,
        SpaceCompassScenarioTimeSettings(true, -60, SpaceCompassScenarioResume.ACTIVE))
    private fun replace(key: String, value: String, text: String = profile.encode()) =
        text.replace(Regex("(?m)^${Regex.escape(key)}=.*$")) { "$key=$value" }
    private fun rejects(text: String) = assertThrows(IllegalArgumentException::class.java) {
        SpaceCompassScenarioArchive.decode(text)
    }

    @Test fun allParametersAndTheResumePolicyRoundTrip() {
        val restored = SpaceCompassScenarioArchive.decode(profile.encode())
        assertEquals(profile, restored)
        assertEquals(-60, restored.timeSettings.multiplier)
        assertNull(restored.configuration.scenarioClock)
    }

    @Test fun everyIndependentFlagCombinationIncludingAllOffIsPreserved() {
        for (mask in 0..31) {
            val config = plan.copy(simulatePosition = mask and 1 != 0, simulateAltitude = mask and 2 != 0,
                simulateZone = mask and 4 != 0, simulateDate = mask and 8 != 0, simulateTime = mask and 16 != 0)
            val restored = SpaceCompassScenarioArchive.decode(profile.copy(configuration = config).encode())
            assertEquals(config, restored.configuration)
            assertEquals(mask != 0, restored.custom)
        }
    }

    @Test fun disabledStoredValuesAreCapturedWithoutReenablingThem() {
        val state = SpaceCompassObserverState(null, elapsedClock = { 0L })
        state.apply(plan); state.apply(null)
        val archive = SpaceCompassScenarioArchive.capture(state, "Disabled")
        assertFalse(archive.custom)
        assertEquals(plan.latitude, archive.configuration.latitude, 0.0)
        assertEquals(moment, archive.configuration.timeMs)
        assertFalse(SpaceCompassScenarioArchive.decode(archive.encode()).custom)
    }

    @Test fun aFreshLiveScenarioCanBeSavedWithoutInventingAnActiveOverride() {
        val archive = SpaceCompassScenarioArchive.capture(SpaceCompassObserverState(null, elapsedClock = { 0L }),
            "Live", moment, ZoneId.of("UTC"))
        assertFalse(archive.custom)
        assertFalse(archive.timeSettings.enabled)
        assertEquals(1, archive.timeSettings.multiplier)
        assertEquals(archive, SpaceCompassScenarioArchive.decode(archive.encode()))
    }

    @Test fun elapsedRuntimeNeverChangesTheArchivedConfiguration() {
        var elapsed = 0L
        val state = SpaceCompassObserverState(null, elapsedClock = { elapsed })
        state.apply(plan); state.configureTime(SpaceCompassScenarioTimeSettings(true))
        state.configureTime(state.timeSettings.copy(rate = 3600)); state.active(true)
        val first = SpaceCompassScenarioArchive.capture(state, "Repeatable").encode()
        elapsed += 60_000
        val later = SpaceCompassScenarioArchive.capture(state, "Repeatable").encode()
        assertEquals(first, later)
        assertFalse(first.contains("checkpoint", ignoreCase = true))
        assertFalse(first.contains("clockOffset"))
        assertFalse(first.lines().any { it.startsWith('#') })
    }

    @Test fun archiveAgeDoesNotAdvanceAnyResumePolicy() {
        val later = Instant.parse("2028-05-01T12:00:00Z").toEpochMilli()
        for (policy in SpaceCompassScenarioResume.entries) {
            val archive = profile.copy(timeSettings = profile.timeSettings.copy(resume = policy))
            assertEquals(moment, archive.configurationAtImport(later, ZoneId.of("UTC")).timeMs)
        }
    }

    @Test fun advanceWithCurrentTimeGetsAFreshOffsetAtImport() {
        val later = Instant.parse("2028-05-01T12:00:00Z").toEpochMilli()
        val archive = profile.copy(configuration = plan.copy(advanceTime = true, clockOffsetMs = 123456L))
        val restored = SpaceCompassScenarioArchive.decode(archive.encode()).configurationAtImport(later, ZoneId.of("UTC"))
        assertTrue(restored.advanceTime)
        assertEquals(moment - later, restored.clockOffsetMs)
        assertEquals(moment, restored.observationTimeMs(later, ZoneId.of("UTC")))
    }

    @Test fun malformedOrMissingFieldsAndNonBooleanFlagsAreRejected() {
        rejects("not a scenario")
        rejects(replace("positionEnabled", "1"))
        rejects(replace("timeEnabled", "TRUE"))
        rejects(profile.encode().lineSequence().filterNot { it.startsWith("zoneId=") }.joinToString("\n"))
    }

    @Test fun invalidCoordinatesHeightsAndNonfiniteNumbersAreRejected() {
        for ((key, value) in listOf("latitude" to "91", "longitude" to "-181", "altitudeMeters" to "20001",
            "latitude" to "NaN", "altitudeMeters" to "Infinity")) rejects(replace(key, value))
    }

    @Test fun unsupportedVersionsFormatsZonesAndDatesAreRejected() {
        for ((key, value) in listOf("version" to "2", "format" to "unrelated-data", "zoneId" to "Not/AZone",
            "momentZoneId" to "Not/AZone", "momentMs" to Long.MAX_VALUE.toString())) rejects(replace(key, value))
    }

    @Test fun unsupportedSpeedsAndPoliciesAreRejectedWithoutRoundingThem() {
        rejects(replace("speedRate", "59")); rejects(replace("speedRate", "1.5")); rejects(replace("resume", "UNKNOWN"))
        for (rate in spaceCompassScenarioRates) assertEquals(rate,
            SpaceCompassScenarioArchive.decode(profile.copy(timeSettings = profile.timeSettings.copy(rate = rate)).encode()).timeSettings.rate)
    }

    @Test fun oversizedInputIsRejectedBeforeApplyingAnyConfiguration() {
        rejects("x".repeat(SPACE_COMPASS_SCENARIO_FILE_LIMIT + 1))
    }

    @Test fun reimportingTheSameScenarioResetsItsRuntimeAndRestoresItsSelectedSpeed() {
        var elapsed = 0L
        val state = SpaceCompassObserverState(null, elapsedClock = { elapsed })
        state.active(true); state.importArchive(profile)
        elapsed += 1000
        assertEquals(moment - 60_000, state.plan!!.scenarioClock!!.at(System.currentTimeMillis(), elapsed))
        state.importArchive(profile)
        assertEquals(moment, state.plan!!.scenarioClock!!.at(System.currentTimeMillis(), elapsed))
        assertEquals(profile.timeSettings, state.timeSettings)
        assertEquals(2, state.editorGeneration)
    }

    @Test fun aDisabledConfigurationAndSpeedChoicesSurviveANewObserverInstance() {
        val store = Store()
        store.values["scenario_clock_checkpoint"] = "old runtime"
        val config = plan.copy(simulatePosition = false, simulateAltitude = false, simulateZone = false,
            simulateDate = false, simulateTime = false)
        val archive = profile.copy(configuration = config, timeSettings = SpaceCompassScenarioTimeSettings(false, -120))
        SpaceCompassObserverState(store.preferences, elapsedClock = { 0L }).importArchive(archive)
        val reopened = SpaceCompassObserverState(store.preferences, elapsedClock = { 0L })
        assertNull(reopened.plan); assertEquals(config, reopened.savedPlan)
        assertEquals(archive.timeSettings, reopened.timeSettings)
        assertFalse(store.values.containsKey("scenario_clock_checkpoint"))
    }

    @Test fun invalidImportsCannotChangeTheExistingObserver() {
        val state = SpaceCompassObserverState(null, elapsedClock = { 0L })
        state.apply(plan)
        assertThrows(IllegalArgumentException::class.java) {
            state.importArchive(profile.copy(configuration = plan.copy(latitude = Double.NaN)))
        }
        assertEquals(plan, state.savedPlan); assertEquals(0, state.editorGeneration)
    }

    @Test fun unicodeNamesRoundTripAndFileNamesCannotEscapeTheirDirectory() {
        val named = profile.copy(name = "Luna · 日本 / Canarie")
        assertEquals(named.name, SpaceCompassScenarioArchive.decode(named.encode()).name)
        val fileName = spaceCompassScenarioFileName("../Canarie\\Luna: sera")
        assertFalse(fileName.contains('/')); assertFalse(fileName.contains('\\')); assertFalse(fileName.contains(':'))
        assertTrue(fileName.endsWith(SPACE_COMPASS_SCENARIO_EXTENSION))
        assertThrows(IllegalArgumentException::class.java) { spaceCompassScenarioFileName("...") }
    }

    private class Store {
        val values = mutableMapOf<String, Any?>()
        private val pending = mutableMapOf<String, Any?>()
        private val editor = Proxy.newProxyInstance(SharedPreferences.Editor::class.java.classLoader,
            arrayOf(SharedPreferences.Editor::class.java)) { proxy, method, args -> when (method.name) {
            "putString", "putBoolean", "putInt" -> { pending[args!![0] as String] = args[1]; proxy }
            "remove" -> { pending[args!![0] as String] = null; proxy }
            "apply", "commit" -> { pending.forEach { (key, value) -> if (value == null) values.remove(key) else values[key] = value }
                pending.clear(); if (method.name == "commit") true else null }
            else -> error(method.name)
        } } as SharedPreferences.Editor
        val preferences = Proxy.newProxyInstance(SharedPreferences::class.java.classLoader,
            arrayOf(SharedPreferences::class.java)) { _, method, args -> when (method.name) {
            "getString", "getBoolean", "getInt" -> values[args!![0] as String] ?: args[1]
            "edit" -> editor
            else -> error(method.name)
        } } as SharedPreferences
    }
}
