package me.mondiversi.spacecompass

import android.content.SharedPreferences
import java.lang.reflect.Proxy
import java.time.Instant
import java.time.ZoneId
import java.util.Locale
import org.junit.Assert.*
import org.junit.Test

/** Exercise real archive, profile and persisted observer paths without personal device data. */
class SpaceCompassLocationTransferMatrixTest {
    private val now = Instant.parse("2026-10-29T08:00:00Z").toEpochMilli()
    private val moment = Instant.parse("2027-06-21T12:30:00Z").toEpochMilli()
    private val place = SpaceCompassDevicePlace(28.35, -14.1, 102.5)
    private val zone = ZoneId.of("Atlantic/Canary")
    private val source = SpaceCompassObserverPlan(place.latitude, place.longitude, 30.48, moment, zone.id,
        automaticAltitudeMeters=102.5, automaticZoneId=zone.id, momentZoneId=zone.id)
    private val scene = SpaceCompassCelestialSelection(setOf(SpaceCompassCelestialBody.SUN,
        SpaceCompassCelestialBody.MOON, SpaceCompassCelestialBody.MARS), SpaceCompassCelestialBody.MARS)
    private val empty = SpaceCompassCelestialSelection(emptySet(), null)
    private val recipientScene = SpaceCompassCelestialSelection(setOf(SpaceCompassCelestialBody.JUPITER),
        SpaceCompassCelestialBody.JUPITER)

    private fun profile(international: Boolean) = SpaceCompassDataExportProfile(
        if (international) spaceCompassPanoramaInternationalFormatting else SpaceCompassPanoramaFormatting(
            Locale.ITALIAN, SpaceCompassNumericFormat.EUROPEAN, SpaceCompassDateFormat.EUROPEAN,
            SpaceCompassTimeFormat.H12, feet=true, dms=true, deviceLocale=Locale.ITALY),
        SpaceCompassUnits(feet=!international), SpaceCompassTextResources { id,_ -> "Field$id" })

    @Test fun independentSwitchesResumeSpeedsAndScenesSurviveBothProfilesAndAProcessRecreation() {
        var cases=0
        for (mask in 0..31) for (choice in SpaceCompassScenarioResume.entries)
        for (speedEnabled in listOf(false,true)) for (rate in listOf(-3600,0,1,3600)) {
            val selection=listOf(null,empty,scene)[cases%3]
            val archive=SpaceCompassScenarioArchive("Canarie_日本",source.copy(
                simulatePosition=mask and 1!=0, simulateAltitude=mask and 2!=0, simulateZone=mask and 4!=0,
                simulateDate=mask and 8!=0, simulateTime=mask and 16!=0),
                SpaceCompassScenarioTimeSettings(speedEnabled,rate,choice), selection)
            for (mode in SpaceCompassPanoramaExportMode.entries) {
                val rendered=spaceCompassScenarioExportText(archive,mode,profile(mode==SpaceCompassPanoramaExportMode.INTERNATIONAL))
                val decoded=SpaceCompassScenarioArchive.decode(rendered)
                assertEquals("canonical archive in $mode",archive,decoded)
                val store=Store(mapOf("celestial_selected" to setOf("JUPITER"),"celestial_active" to "JUPITER",
                    SPACE_COMPASS_CELESTIAL_FAVORITES_KEY to setOf("VENUS"),"catalog_sort" to "NAME_DESC",
                    "catalog_favorites_only" to true,"scenario_clock_checkpoint" to "foreign-runtime"))
                val receiver=SpaceCompassObserverState(store.preferences,elapsedClock={0L})
                receiver.importArchive(decoded)
                val reopened=SpaceCompassObserverState(store.preferences,elapsedClock={0L})
                assertEquals(archive.configuration,reopened.savedPlan)
                assertEquals(archive.timeSettings,reopened.timeSettings)
                assertEquals(selection ?: recipientScene,readSpaceCompassCelestialSelection(store.preferences))
                assertEquals(setOf("VENUS"),store.values[SPACE_COMPASS_CELESTIAL_FAVORITES_KEY])
                assertEquals("NAME_DESC",store.values["catalog_sort"])
                assertEquals(true,store.values["catalog_favorites_only"])
                assertNotEquals("foreign-runtime",store.values["scenario_clock_checkpoint"])
                val recaptured=SpaceCompassScenarioArchive.capture(reopened,archive.name,now,ZoneId.of("Europe/Rome"))
                assertEquals(archive.copy(celestialSelection=null),recaptured)
                cases++
            }
        }
        assertEquals(1536,cases)
    }

    @Test fun automaticSpatialExportsOmitTemporalOverridesAndKeepTheRecipientsResumeChoice() {
        for (mode in SpaceCompassScenarioResume.entries) {
            val sender=SpaceCompassObserverState(null,elapsedClock={0L})
            sender.apply(source);sender.apply(null);sender.devicePlace=place
            sender.configureTime(sender.timeSettings.copy(resume=mode))
            val current=SpaceCompassScenarioCurrentValues(place,102.5,zone,now,1)
            val exported=SpaceCompassScenarioArchive.capture(sender,"Automatic",now,zone).withAutomaticLocation(current)!!
            assertFalse(exported.temporalSettingsIncluded)
            val store=Store()
            val recipient=SpaceCompassObserverState(store.preferences,elapsedClock={0L})
            recipient.configureTime(recipient.timeSettings.copy(resume=mode,enabled=true))
            recipient.configureTime(recipient.timeSettings.copy(rate=-60))
            recipient.importArchive(SpaceCompassScenarioArchive.decode(exported.encode()))
            val reopened=SpaceCompassObserverState(store.preferences,elapsedClock={0L})
            assertEquals(mode,reopened.timeSettings.resume)
            assertFalse(reopened.timeSettings.enabled)
            assertEquals(-60,reopened.timeSettings.rate)
            assertFalse(reopened.plan!!.simulateDate || reopened.plan!!.simulateTime)
            assertEquals(now,reopened.plan!!.observationTimeMs(now,ZoneId.of("Europe/Rome")))
            assertEquals(zone,reopened.plan!!.observationZone(ZoneId.of("Europe/Rome")))
        }
    }

    @Test fun automaticPositionMetadataAndSelectedMomentsRemainPortableAcrossDifferentDeviceZones() {
        for (date in listOf(false,true)) for (time in listOf(false,true)) {
            val archive=SpaceCompassScenarioArchive("Canary",source.copy(simulateAltitude=false,
                simulateZone=false,simulateDate=date,simulateTime=time),SpaceCompassScenarioTimeSettings())
            val receiver=SpaceCompassObserverState(null,elapsedClock={0L})
            receiver.importArchive(SpaceCompassScenarioArchive.decode(archive.encode()))
            for (deviceZone in listOf(ZoneId.of("Europe/Rome"),ZoneId.of("America/New_York"))) {
                assertEquals(zone,receiver.plan!!.observationZone(deviceZone))
                assertEquals(102.5,receiver.plan!!.resolvePlace(SpaceCompassDevicePlace(45.0,9.0,300.0))!!.altitude!!,0.0)
                val effective=receiver.savedPlan!!.observationTimeMs(now,deviceZone)
                val expected=archive.configuration.observationTimeMs(now,deviceZone)
                assertEquals(expected,effective)
            }
        }
    }

    @Test fun rejectedResumeAndObjectFilesLeaveAllRecipientPreferencesUntouched() {
        val archive=SpaceCompassScenarioArchive("Invalid",source,SpaceCompassScenarioTimeSettings(),scene)
        val text=archive.encode()
        for (invalid in listOf(text.replace("resume=RESTART","resume=UNKNOWN"),
            text.replace("speedRate=1","speedRate=59"),
            text.replace("positionEnabled=true","positionEnabled=TRUE"),
            text.replace("objectsIncluded=true","objectsIncluded=TRUE"))) {
            val store=Store(mapOf("unrelated" to "keep", "celestial_selected" to setOf("JUPITER")))
            val before=store.values.toMap()
            assertThrows(IllegalArgumentException::class.java) {
                SpaceCompassObserverState(store.preferences,elapsedClock={0L})
                    .importArchive(SpaceCompassScenarioArchive.decode(invalid))
            }
            assertEquals(before,store.values)
        }
    }

    @Test fun archivesFromTheTemporaryCheckboxDesignKeepTheirEffectivePolicy() {
        for (mode in SpaceCompassScenarioResume.entries) {
            val legacy=SpaceCompassScenarioArchive("Compatible",source,
                SpaceCompassScenarioTimeSettings(resume=mode))
            val text=legacy.encode()+"resumeEnabled=${mode!=SpaceCompassScenarioResume.RESTART}\nresumeChoice=ALWAYS\n"
            val store=Store(mapOf("scenario_resume_enabled" to false,"scenario_resume_choice" to "ACTIVE"))
            SpaceCompassObserverState(store.preferences,elapsedClock={0L})
                .importArchive(SpaceCompassScenarioArchive.decode(text))
            assertEquals(mode,SpaceCompassObserverState(store.preferences,elapsedClock={0L}).timeSettings.resume)
        }
    }

    private class Store(initial: Map<String,Any?> = emptyMap()) {
        val values=initial.toMutableMap()
        private val pending=mutableMapOf<String,Any?>()
        private val editor=Proxy.newProxyInstance(SharedPreferences.Editor::class.java.classLoader,
            arrayOf(SharedPreferences.Editor::class.java)) { proxy,method,args -> when(method.name) {
            "putString","putStringSet","putBoolean","putInt" -> {pending[args!![0] as String]=args[1];proxy}
            "remove" -> {pending[args!![0] as String]=null;proxy}
            "apply","commit" -> {pending.forEach { (k,v) -> if(v==null) values.remove(k) else values[k]=v }
                pending.clear();if(method.name=="commit") true else null}
            else -> error(method.name)
        }} as SharedPreferences.Editor
        val preferences=Proxy.newProxyInstance(SharedPreferences::class.java.classLoader,
            arrayOf(SharedPreferences::class.java)) { _,method,args -> when(method.name) {
            "getString","getStringSet","getBoolean","getInt" -> values[args!![0] as String] ?: args[1]
            "edit" -> editor
            else -> error(method.name)
        }} as SharedPreferences
    }
}
