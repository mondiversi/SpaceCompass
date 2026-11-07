package me.mondiversi.spacecompass

import android.content.SharedPreferences
import java.lang.reflect.Proxy
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.util.Base64
import java.util.Locale
import org.junit.Assert.*
import org.junit.Test

class SpaceCompassSceneArchiveTest {
    private val now=Instant.parse("2027-06-21T12:00:00Z").toEpochMilli()
    private val zone=ZoneId.of("Atlantic/Canary")
    private val sun=SpaceCompassCelestialBody.SUN
    private val moon=SpaceCompassCelestialBody.MOON
    private val mars=SpaceCompassCelestialBody.MARS
    private val plan=SpaceCompassObserverPlan(28.35,-14.1,102.5,now,zone.id)
    private val display=SpaceCompassSceneDisplay(true,true,false,true)
    private val selection=SpaceCompassCelestialSelection(setOf(sun,moon),moon)
    private val comparison=SpaceCompassTemporalComparison.create(sun,now,zone,
        SpaceCompassComparisonSource.HEIGHT,"annual-maximum")
    private val personal=SpaceCompassPersonalMoment(moon,"Moon reference",LocalDate.of(2027,6,21),LocalTime.NOON,true)
    private fun point(body: SpaceCompassCelestialBody, id: String?=null, current: Boolean=false): SpaceCompassSelectedOrbitPoint {
        val p=SpaceCompassSunPathPoint(now,SpaceCompassSunPosition(150.0,60.0),SpaceCompassSunPathEvent.HOUR)
        return SpaceCompassSelectedOrbitPoint(spaceCompassOrbitPointKey(body,p,zone,current=current,comparisonId=id),p,null)
    }
    private fun contents(objects: SpaceCompassCelestialSelection=selection)=SpaceCompassScenarioArchive("Example",plan,
        SpaceCompassScenarioTimeSettings(true,-60,SpaceCompassScenarioResume.ACTIVE))
        .withCelestialObjects(objects,listOf(point(moon,current=true),point(sun,comparison.id),point(moon,personal.id)),
            listOf(comparison),personal.id,true,listOf(personal))
    private fun scene(include: Boolean=true)=SpaceCompassSceneArchive(
        contents().let { if(include) it else it.withoutLocation() },include,display,now)
    private fun state()=SpaceCompassSunDailyPathUiState()

    @Test fun fullSceneRoundTripsLiveAndDatedObjectsPinsAntipodesAndAllDisplaySwitches() {
        for(include in listOf(false,true)) for(mask in 0..15) {
            val d=SpaceCompassSceneDisplay(mask and 1!=0,mask and 2!=0,mask and 4!=0,mask and 8!=0)
            val value=scene(include).copy(display=d)
            assertEquals(value,SpaceCompassSceneArchive.decode(value.encode()))
        }
    }
    @Test fun locationOnlyStripsAllCelestialExtensionsFromLegacyMixedDocuments() {
        val legacy=SpaceCompassScenarioArchive.decode(contents().encode())
        val location=legacy.locationOnly()
        assertEquals(plan,location.configuration);assertEquals(legacy.timeSettings,location.timeSettings)
        assertNull(location.celestialSelection);assertNull(location.comparisons);assertNull(location.personalMoments)
        assertNull(location.selectedOrbitPoints);assertNull(location.activeComparisonId);assertNull(location.antipodesVisible)
        assertFalse(location.encode().contains("objectsIncluded"))
    }
    @Test fun legacyMixedDocumentsCanStillBeImportedAsScenesWithoutChangingDisplayPreferences() {
        val legacy=contents()
        val scene=SpaceCompassSceneArchive.decode(legacy.encode())
        assertTrue(scene.locationIncluded);assertNull(scene.display);assertEquals(legacy,scene.contents)
        assertEquals(scene,SpaceCompassSceneArchive.decode(scene.encode()))
    }
    @Test fun newSceneFilesAreNotMistakenForLocationFiles() {
        assertThrows(IllegalArgumentException::class.java) { SpaceCompassScenarioArchive.decode(scene().encode()) }
        assertTrue(spaceCompassSceneFileName("Example").endsWith(".spacecompass-scene"))
        assertEquals("Example.spacecompass-scene",spaceCompassSceneFileName("Example.spacecompass-scene"))
        assertEquals("Example.spacecompass",spaceCompassScenarioFileName("Example"))
    }
    @Test fun omittedLocationContainsNoDormantObserverOrClockConfiguration() {
        val original=scene().copy(locationIncluded=false)
        val decoded=SpaceCompassSceneArchive.decode(original.encode()).contents
        assertEquals(0.0,decoded.configuration.latitude,0.0);assertEquals(0.0,decoded.configuration.longitude,0.0)
        assertEquals(0.0,decoded.configuration.altitudeMeters,0.0);assertEquals(0L,decoded.configuration.timeMs)
        assertEquals("UTC",decoded.configuration.zoneId);assertFalse(decoded.custom)
        assertEquals(SpaceCompassScenarioTimeSettings(),decoded.timeSettings)
        assertFalse(decoded.temporalSettingsIncluded);assertEquals(selection,decoded.celestialSelection)
        assertEquals(contents().selectedOrbitPoints,decoded.selectedOrbitPoints)
    }
    @Test fun malformedSceneHeadersOrPayloadsAreRejectedBeforeImport() {
        val text=scene().encode()
        for(broken in listOf(text.replace("version=1","version=99"),text.replace("locationIncluded=true","locationIncluded=maybe"),
            text.replace("format=space-compass-scene","format=other-app"),text.replace("contents=","contents=@@@"),
            text.lineSequence().filterNot { it.startsWith("contents=") }.joinToString("\n"),
            text.replace("weather=false","weather=unknown"),text.replace("capturedAtMs=$now","capturedAtMs=no"))) {
            assertThrows(Exception::class.java) { SpaceCompassSceneArchive.decode(broken) }
        }
        assertThrows(IllegalArgumentException::class.java) { SpaceCompassSceneArchive.decode("x".repeat(SPACE_COMPASS_SCENARIO_FILE_LIMIT+1)) }
        assertThrows(IllegalArgumentException::class.java) { SpaceCompassSceneArchive.decode(text.replace("locationIncluded=true","locationIncluded=false")) }
        assertThrows(IllegalArgumentException::class.java) { SpaceCompassSceneArchive.decode(text.replace("weather=false\n","")) }
    }
    @Test fun allAutomaticLocationCanBeCompletedButNeverBorrowsDormantOverrides() {
        val observer=SpaceCompassObserverState(null,elapsedClock={0L})
        observer.apply(plan);observer.apply(null)
        observer.devicePlace=SpaceCompassDevicePlace(45.0,9.0,120.0)
        val snapshot=spaceCompassCaptureLocationExport(observer,"Here",now,ZoneId.of("Europe/Rome"))
        assertFalse(snapshot.configured.custom)
        val completed=snapshot.complete!!
        assertEquals(45.0,completed.configuration.latitude,0.0);assertEquals(120.0,completed.configuration.altitudeMeters,0.0)
        assertEquals("Europe/Rome",completed.configuration.zoneId);assertEquals(now,completed.configuration.timeMs)
        assertEquals(5,completed.configuration.activeSections.size)
        assertNull(observer.plan);assertNull(snapshot.configured.celestialSelection)
        assertSame(snapshot.configured,snapshot.location(false));assertSame(snapshot.complete,snapshot.location(true))
    }
    @Test fun customizedLocationUsesItsAutomaticAltitudeAndZoneNotTheRecipientsDeviceValues() {
        val observer=SpaceCompassObserverState(null,elapsedClock={0L})
        observer.apply(plan.copy(simulateAltitude=false,simulateZone=false,automaticAltitudeMeters=25.0,
            automaticZoneId=zone.id,simulateDate=false,simulateTime=false))
        observer.devicePlace=SpaceCompassDevicePlace(45.0,9.0,120.0)
        val snapshot=spaceCompassCaptureLocationExport(observer,"Custom",now,ZoneId.of("Europe/Rome"))
        assertEquals(28.35,snapshot.complete!!.configuration.latitude,0.0)
        assertEquals(25.0,snapshot.complete.configuration.altitudeMeters,0.0)
        assertEquals(zone.id,snapshot.complete.configuration.zoneId)
        assertFalse(snapshot.configured.configuration.simulateAltitude)
        assertFalse(snapshot.configured.configuration.simulateZone)
        assertFalse(snapshot.configured.configuration.simulateDate)
    }
    @Test fun missingAutomaticDataDoesNotInventCoordinatesOrAltitudeAndDoesNotBlockSceneOnlyExport() {
        val observer=SpaceCompassObserverState(null,elapsedClock={0L})
        val noGps=spaceCompassCaptureLocationExport(observer,"No fix",now,zone)
        assertNull(noGps.complete)
        observer.devicePlace=SpaceCompassDevicePlace(1.0,2.0,null)
        assertNull(spaceCompassCaptureLocationExport(observer,"No altitude",now,zone).complete)
        val snapshot=noGps.copy(configured=contents(),display=display)
        assertNull(snapshot.scene(true));assertNotNull(snapshot.scene(false))
    }
    @Test fun importingSceneWithoutLocationKeepsTheExistingObserverAndPersonalApplicationPreferences() {
        val store=Store(mapOf("language" to "it",SPACE_COMPASS_CELESTIAL_FAVORITES_KEY to setOf("MARS"),"music_volume" to 20))
        val observer=SpaceCompassObserverState(store.preferences,elapsedClock={0L})
        val own=plan.copy(latitude=44.0,zoneId="Europe/Rome")
        observer.apply(own)
        val originalSettings=observer.timeSettings
        val points=state();val comparisons=SpaceCompassTemporalComparisonState(points)
        importSpaceCompassScene(scene(false),observer,comparisons,points,store.preferences)
        assertEquals(own,observer.savedPlan);assertEquals(originalSettings,observer.timeSettings)
        assertEquals(selection,readSpaceCompassCelestialSelection(store.preferences))
        assertEquals(display,readSpaceCompassSceneDisplay(store.preferences))
        assertEquals(contents().selectedOrbitPoints!!.toSet(),points.selectedPoints.toSet())
        assertTrue(points.antipodesVisible);assertEquals(personal.id,comparisons.activeId)
        assertEquals(setOf("MARS"),store.values[SPACE_COMPASS_CELESTIAL_FAVORITES_KEY])
        assertEquals("it",store.values["language"]);assertEquals(20,store.values["music_volume"])
    }
    @Test fun importingSceneWithLocationReplacesObserverValuesAndClockSettings() {
        val store=Store();val observer=SpaceCompassObserverState(store.preferences,elapsedClock={0L})
        val points=state();val comparisons=SpaceCompassTemporalComparisonState(points)
        importSpaceCompassScene(scene(),observer,comparisons,points,store.preferences)
        assertEquals(plan,observer.savedPlan);assertEquals(contents().timeSettings,observer.timeSettings)
        assertEquals(selection,readSpaceCompassCelestialSelection(store.preferences))
    }
    @Test fun loadingADifferentDayRestoresIncomingLivePinsBeforeTheNextRollover() {
        val store = Store()
        val observer = SpaceCompassObserverState(store.preferences, elapsedClock = { 0L })
        observer.apply(plan.copy(timeMs = now - 86_400_000L))
        val points = SpaceCompassSunDailyPathUiState()
        val oldDate = java.time.Instant.ofEpochMilli(now).atZone(zone).toLocalDate().minusDays(1)
        points.refreshObservationDate(oldDate)
        val comparisons = SpaceCompassTemporalComparisonState(points)
        importSpaceCompassScene(scene(), observer, comparisons, points, store.preferences)
        points.refreshObservationDate(oldDate.plusDays(1)) // First main-view frame after loading.
        assertEquals(contents().selectedOrbitPoints!!.toSet(), points.selectedPoints.toSet())
        assertTrue(points.antipodesVisible)
        points.refreshObservationDate(oldDate.plusDays(2))
        assertEquals(contents().selectedOrbitPoints!!.filter { it.key.comparisonId != null }.toSet(),
            points.selectedPoints.toSet())
    }

    @Test fun legacyLocationImportCannotReplaceTheRecipientsObjectsOrPoints() {
        val store=Store(mapOf("celestial_selected" to setOf("MARS"),"celestial_active" to "MARS"))
        val observer=SpaceCompassObserverState(store.preferences,elapsedClock={0L})
        observer.importArchive(SpaceCompassScenarioArchive.decode(contents().encode()).locationOnly())
        assertEquals(setOf(mars),readSpaceCompassCelestialSelection(store.preferences).selected)
        assertEquals(plan,observer.savedPlan)
    }
    @Test fun sceneRestoresIncomingPersonalCheckboxesAndRetainsOtherDefinitionsUnchecked() {
        val points=state();val state=SpaceCompassTemporalComparisonState(points)
        val other=personal.copy(name="My own moment")
        state.addPersonalMoment(personal.copy(enabled=false));state.addPersonalMoment(other)
        state.togglePersonalMoment(other.id)
        state.restore(listOf(comparison),personal.id,listOf(personal),replaceScene=true)
        assertTrue(state.personalMoments.single { it.id==personal.id }.enabled)
        assertFalse(state.personalMoments.single { it.id==other.id }.enabled)
        assertEquals(2,state.personalMoments.size);assertEquals(personal.id,state.activeId)
    }
    @Test fun explicitEmptySceneClearsVisibleContentWithoutDeletingPersonalDefinitions() {
        val store=Store();val observer=SpaceCompassObserverState(store.preferences,elapsedClock={0L})
        val points=state();val comparisons=SpaceCompassTemporalComparisonState(points)
        comparisons.addPersonalMoment(personal.copy(enabled=false));comparisons.togglePersonalMoment(personal.id)
        comparisons.selectComparison(comparison)
        points.restoreImportedPoints(listOf(point(sun,comparison.id)));points.restoreAntipodes(true)
        val empty=SpaceCompassScenarioArchive("Empty",plan,SpaceCompassScenarioTimeSettings()).withCelestialObjects(
            SpaceCompassCelestialSelection(emptySet(),null),emptyList(),emptyList(),null,false,emptyList()).withoutLocation()
        importSpaceCompassScene(SpaceCompassSceneArchive(empty,false,display,now),observer,comparisons,points,store.preferences)
        assertTrue(readSpaceCompassCelestialSelection(store.preferences).selected.isEmpty())
        assertTrue(comparisons.entries.isEmpty());assertTrue(points.selectedPoints.isEmpty());assertFalse(points.antipodesVisible)
        assertEquals(1,comparisons.personalMoments.size);assertFalse(comparisons.personalMoments.single().enabled)
    }
    @Test fun personalLibraryCapacityFailureLeavesObserverAndSceneUntouched() {
        val store=Store(mapOf("celestial_selected" to setOf("MARS"),"celestial_active" to "MARS"))
        val observer=SpaceCompassObserverState(store.preferences,elapsedClock={0L});observer.apply(plan.copy(latitude=44.0))
        val points=state();val comparisons=SpaceCompassTemporalComparisonState(points)
        repeat(SPACE_COMPASS_PERSONAL_MOMENT_LIMIT) { comparisons.addPersonalMoment(personal.copy(name="Stored $it",enabled=false)) }
        val before=store.values.toMap();val configured=observer.savedPlan
        assertThrows(IllegalArgumentException::class.java) { importSpaceCompassScene(scene(),observer,comparisons,points,store.preferences) }
        assertEquals(before,store.values);assertEquals(configured,observer.savedPlan);assertTrue(points.selectedPoints.isEmpty())
    }
    @Test fun datedObjectsCanBeSharedWhileTheirLiveParentIsUnchecked() {
        val empty=SpaceCompassCelestialSelection(emptySet(),null)
        val archive=SpaceCompassSceneArchive(contents(empty),true,display,now)
        val decoded=SpaceCompassSceneArchive.decode(archive.encode()).contents
        assertTrue(decoded.celestialSelection!!.selected.isEmpty());assertEquals(listOf(comparison),decoded.comparisons)
        assertEquals(listOf(personal),decoded.personalMoments)
        assertEquals(setOf(comparison.id,personal.id),decoded.selectedOrbitPoints!!.map { it.key.comparisonId }.toSet())
    }
    @Test fun exportProfilesChangeReadableFieldsAndSuggestedNamesButNotImportableData() {
        fun profile(international: Boolean)=SpaceCompassDataExportProfile(
            if(international) spaceCompassPanoramaInternationalFormatting else SpaceCompassPanoramaFormatting(Locale.ITALIAN,
                SpaceCompassNumericFormat.EUROPEAN,SpaceCompassDateFormat.EUROPEAN,SpaceCompassTimeFormat.H12,feet=true,
                deviceLocale=Locale.ITALY),SpaceCompassUnits(feet=!international),SpaceCompassTextResources { id,_ -> "Field$id" })
        val value=scene()
        val texts=SpaceCompassPanoramaExportMode.entries.map { mode ->
            val p=profile(mode==SpaceCompassPanoramaExportMode.INTERNATIONAL)
            val text=spaceCompassSceneExportText(value,mode,p)
            assertEquals(value,SpaceCompassSceneArchive.decode(text));text
        }
        assertNotEquals(texts[0],texts[1])
        assertNotEquals(spaceCompassSceneSuggestedName(value,profile(false)),spaceCompassSceneSuggestedName(value,profile(true)))
        assertFalse(spaceCompassSceneSuggestedName(value,profile(true)).contains("Space-Compass"))
        assertEquals(value.contents,SpaceCompassSceneArchive.decode(texts[0]).contents)
    }

    @Test fun sceneCanIncludePersonalizedLocationWithoutCompletingAutomaticParameters() {
        val configured=contents().copy(configuration=plan.copy(simulateAltitude=false,simulateZone=false,
            simulateDate=false,simulateTime=false),timeSettings=SpaceCompassScenarioTimeSettings())
        val snapshot=SpaceCompassConfigurationExport(configured,contents(),now,display)
        val selected=SpaceCompassSceneArchive.decode(snapshot.scene(true,false)!!.encode())
        assertTrue(selected.locationIncluded)
        assertEquals(configured,selected.contents)
        assertFalse(selected.contents.configuration.simulateDate)
        assertFalse(selected.contents.configuration.simulateTime)
        assertFalse(selected.contents.configuration.simulateAltitude)
        assertEquals(selection,selected.contents.celestialSelection)
        assertEquals(configured.selectedOrbitPoints,selected.contents.selectedOrbitPoints)
        assertEquals(contents(),snapshot.scene(true,true)!!.contents)
        assertEquals(contents(),snapshot.scene(true)!!.contents)
    }
    @Test fun sceneWithAutomaticLocationDoesNotNeedGpsWhenCompletionIsUnchecked() {
        val observer=SpaceCompassObserverState(null,elapsedClock={0L})
        val location=spaceCompassCaptureLocationExport(observer,"Automatic",now,zone)
        val configured=location.configured.withCelestialObjects(selection,emptyList(),emptyList(),null,false,emptyList())
        val snapshot=location.copy(configured=configured,display=display)
        assertNull(snapshot.scene(true,true))
        val exported=SpaceCompassSceneArchive.decode(snapshot.scene(true,false)!!.encode())
        assertTrue(exported.locationIncluded);assertFalse(exported.contents.custom)
        assertEquals(selection,exported.contents.celestialSelection)
        assertNull(observer.plan)
    }
    @Test fun excludingLocationIgnoresTheAutomaticCompletionChoice() {
        val snapshot=SpaceCompassConfigurationExport(contents(),null,now,display)
        assertEquals(snapshot.scene(false,true),snapshot.scene(false,false))
        assertFalse(snapshot.scene(false,false)!!.locationIncluded)
        assertEquals(contents().withoutLocation(),snapshot.scene(false,false)!!.contents)
    }

    private class Store(initial: Map<String,Any?> = emptyMap()) {
        val values=initial.toMutableMap();private val pending=mutableMapOf<String,Any?>()
        private val editor=Proxy.newProxyInstance(SharedPreferences.Editor::class.java.classLoader,
            arrayOf(SharedPreferences.Editor::class.java)) { proxy,method,args -> when(method.name) {
                "putString","putStringSet","putBoolean","putInt" -> { pending[args!![0] as String]=args[1];proxy }
                "remove" -> { pending[args!![0] as String]=null;proxy }
                "apply","commit" -> { pending.forEach { (k,v) -> if(v==null) values.remove(k) else values[k]=v };pending.clear();if(method.name=="commit") true else null }
                else -> error(method.name)
            } } as SharedPreferences.Editor
        val preferences=Proxy.newProxyInstance(SharedPreferences::class.java.classLoader,arrayOf(SharedPreferences::class.java)) { _,method,args -> when(method.name) {
            "getString","getStringSet","getBoolean","getInt" -> values[args!![0] as String] ?: args[1]
            "edit" -> editor
            else -> error(method.name)
        } } as SharedPreferences
    }
}
