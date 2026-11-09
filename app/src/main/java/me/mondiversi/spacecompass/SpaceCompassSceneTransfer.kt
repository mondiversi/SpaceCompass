package me.mondiversi.spacecompass

import android.content.SharedPreferences
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalResources
import java.util.TimeZone

internal fun readSpaceCompassSceneDisplay(p: SharedPreferences) = SpaceCompassSceneDisplay(
    p.getBoolean(SPACE_COMPASS_SKY_REFERENCES_KEY,SPACE_COMPASS_SKY_REFERENCES_DEFAULT),
    p.getBoolean(SPACE_COMPASS_FORWARD_PATH_KEY,false),
    p.getBoolean(SPACE_COMPASS_WEATHER_VISIBLE_KEY,SPACE_COMPASS_WEATHER_VISIBLE_DEFAULT),
    p.getBoolean(SPACE_COMPASS_CAMERA_OBJECT_TRANSLUCENCY_KEY,SPACE_COMPASS_CAMERA_OBJECT_TRANSLUCENCY_DEFAULT),
    p.getBoolean(SPACE_COMPASS_GLOBE_MODE_KEY,false),p.getBoolean(SPACE_COMPASS_GLOBE_LABELS_KEY,SPACE_COMPASS_GLOBE_LABELS_DEFAULT),
    p.getBoolean(SPACE_COMPASS_GLOBE_AXIS_KEY,true),p.getString(SPACE_COMPASS_GLOBE_LABEL_MODE_KEY,null))

internal fun SharedPreferences.Editor.putSpaceCompassSceneDisplay(display: SpaceCompassSceneDisplay) = apply {
    putBoolean(SPACE_COMPASS_SKY_REFERENCES_KEY,display.guides)
    putBoolean(SPACE_COMPASS_FORWARD_PATH_KEY,display.forward24Hours)
    putBoolean(SPACE_COMPASS_WEATHER_VISIBLE_KEY,display.weather)
    putBoolean(SPACE_COMPASS_CAMERA_OBJECT_TRANSLUCENCY_KEY,display.translucentObjects)
    putBoolean(SPACE_COMPASS_GLOBE_MODE_KEY,display.globe)
    putBoolean(SPACE_COMPASS_GLOBE_LABELS_KEY,display.globeLabels)
    putBoolean(SPACE_COMPASS_GLOBE_AXIS_KEY,display.globeAxisLocked)
    if(display.globeLabelMode==null) remove(SPACE_COMPASS_GLOBE_LABEL_MODE_KEY)
    else putString(SPACE_COMPASS_GLOBE_LABEL_MODE_KEY,display.globeLabelMode)
}

/** Validate the merged local library before changing any observer or visible scene state. */
internal fun importSpaceCompassScene(archive: SpaceCompassSceneArchive, observer: SpaceCompassObserverState,
    comparisons: SpaceCompassTemporalComparisonState, points: SpaceCompassSunDailyPathUiState,
    preferences: SharedPreferences) {
    val checked=SpaceCompassSceneArchive.decode(archive.encode())
    val scene=checked.contents
    scene.personalMoments?.let { spaceCompassScenePersonalMoments(comparisons.personalMoments,it) }
    if (checked.locationIncluded) observer.importArchive(scene.locationOnly())
    // Expire the previous scene before restoring the incoming pins, which already belong to this day.
    val realNow = System.currentTimeMillis()
    val deviceZone = java.time.ZoneId.systemDefault()
    val sceneZone = observer.plan?.observationZone(deviceZone) ?: deviceZone
    val sceneTime = observer.plan?.observationTimeMs(realNow, deviceZone) ?: realNow
    points.refreshObservationDate(java.time.Instant.ofEpochMilli(sceneTime).atZone(sceneZone).toLocalDate())
    val editor=preferences.edit().putSpaceCompassCelestialSelection(requireNotNull(scene.celestialSelection))
    checked.display?.let(editor::putSpaceCompassSceneDisplay)
    editor.apply()
    comparisons.restore(scene.comparisons,scene.activeComparisonId,scene.personalMoments,replaceScene=true)
    points.restoreImportedPoints(scene.selectedOrbitPoints)
    scene.antipodesVisible?.let(points::restoreAntipodes)
}

@Composable
internal fun rememberSpaceCompassSceneTransfer(observer: SpaceCompassObserverState,
    comparisons: SpaceCompassTemporalComparisonState, points: SpaceCompassSunDailyPathUiState): SpaceCompassDocumentTransfer {
    val context=LocalContext.current
    val resources=LocalResources.current
    val preferences=LocalSpaceCompassPreferences.current ?: remember(context) {
        context.getSharedPreferences(SPACE_COMPASS_PREFERENCES_NAME,android.content.Context.MODE_PRIVATE)
    }
    return rememberSpaceCompassConfigurationTransfer(scene=true,observer=observer,capture={
        val base=spaceCompassCaptureLocationExport(observer,resources.getString(R.string.scene_title))
        fun SpaceCompassScenarioArchive.objects()=withCelestialObjects(readSpaceCompassCelestialSelection(preferences),
            points.selectedPoints,comparisons.entries,comparisons.activeId,points.antipodesVisible,comparisons.personalMoments)
        base.copy(configured=base.configured.objects(),complete=base.complete?.objects(),display=readSpaceCompassSceneDisplay(preferences))
    },decode={ SpaceCompassSceneArchive.decode(it) },onImport={ importSpaceCompassScene(it,observer,comparisons,points,preferences) })
}

internal fun spaceCompassSceneSuggestedName(scene: SpaceCompassSceneArchive, profile: SpaceCompassDataExportProfile): String {
    val current=scene.contents
    val active=current.activeComparisonId?.let { id -> current.comparisons?.find { it.id==id }?.body
        ?: current.personalMoments?.find { it.id==id }?.body } ?: current.celestialSelection?.active
    val zone=TimeZone.getTimeZone(if (scene.locationIncluded) current.configuration.momentZoneId else "UTC")
    val f=profile.formatting
    val date=formatSpaceCompassDateOnly(scene.capturedAtMs,f.date,
        if(f.date==SpaceCompassDateFormat.SYSTEM) f.deviceLocale else f.locale,zone,f.numeric,f.deviceLocale,allowHistorical=true)
    val time=formatSpaceCompassTimeOnly(scene.capturedAtMs,f.time,f.locale,zone,f.numeric,f.deviceLocale,
        includeSeconds=true,allowHistorical=true)
    val offset=formatSpaceCompassObserverUtcOffset(java.time.ZoneId.of(zone.id).rules.getOffset(
        java.time.Instant.ofEpochMilli(scene.capturedAtMs)))
    return spaceCompassLimitFileText(listOfNotNull(profile.strings.getString(R.string.scene_title),
        active?.let { profile.strings.getString(it.nameResource) },date,time,offset)
        .joinToString("_") { spaceCompassExportFilePart(it,40) },80)
}

internal fun spaceCompassSceneExportText(scene: SpaceCompassSceneArchive, mode: SpaceCompassPanoramaExportMode,
    profile: SpaceCompassDataExportProfile): String {
    val rows=listOf(profile.strings.getString(R.string.scene_file_name) to scene.contents.name,
        profile.strings.getString(R.string.panorama_export_profile) to profile.strings.getString(
            if (mode==SpaceCompassPanoramaExportMode.SELECTED) R.string.panorama_export_selected else R.string.panorama_export_international)) +
        if (scene.locationIncluded) spaceCompassScenarioExportFields(scene.contents,profile) else emptyList()
    return rows.joinToString("\n",postfix="\n") { (key,value) ->
        "# "+("$key: $value").replace(Regex("[\\p{Cc}\\p{Cf}]")," ")
    }+scene.encode()
}
