package me.mondiversi.spacecompass

import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalResources
import java.time.ZoneId
import java.util.TimeZone

/** Formatting and metadata are keyed by astronomy/locale changes, never raw orientation frames. */
@Composable
internal fun rememberSpaceCompassGlobeScene(timeMs: Long,latitude: Double?,longitude: Double?,altitude: Double,
    phase: SpaceCompassSunSkyPhase,bodies: Set<SpaceCompassCelestialBody>,
    overlays: Map<SpaceCompassCelestialBody,SpaceCompassCelestialOverlay>,comparisons: List<SpaceCompassComparisonOverlay>,
    remote: SpaceCompassCelestialRemoteData,state: SpaceCompassSunDailyPathUiState,showReferences: Boolean,
    activeBody: SpaceCompassCelestialBody?,activeComparisonId: String?,zone: ZoneId,forward: Boolean,
    simulated: Boolean): SpaceCompassPanoramaSnapshot {
    val resources=LocalResources.current
    val units=LocalSpaceCompassUnits.current
    val formatting=SpaceCompassPanoramaFormatting(resources.configuration.locales[0],LocalSpaceCompassNumericFormat.current,
        LocalSpaceCompassDateFormat.current,LocalSpaceCompassTimeFormat.current,units.feet,units.dms,LocalSpaceCompassDeviceLocale.current)
    val selected=state.selectedPoints
    val antipodes=state.antipodesVisible
    return remember(timeMs/1000,latitude,longitude,altitude,phase,bodies,overlays,comparisons,remote,selected,antipodes,
        showReferences,activeBody,activeComparisonId,zone,forward,simulated,formatting,resources) {
        val objects=bodies.map { SpaceCompassPanoramaObject(it,resources.getString(it.nameResource),overlays[it]?.path) } +
            comparisons.map { SpaceCompassPanoramaObject(it.comparison.body,resources.getString(it.comparison.body.nameResource),it.overlay.path,it.comparison) }
        val snapshot=SpaceCompassPanoramaSnapshot(timeMs,latitude ?: 0.0,longitude ?: 0.0,altitude,phase,null,
            objects,remote,"",emptyList(),emptyList(),observerPositionKnown=latitude!=null&&longitude!=null,
            showSkyReferences=showReferences,selectedPoints=selected,showAntipodes=antipodes,
            activeBody=activeBody,activeComparisonId=activeComparisonId,zoneId=zone.id,forward24Hours=forward)
        spaceCompassPanoramaPresentation(snapshot,resources,formatting,TimeZone.getTimeZone(zone),null,
            altitude,null,simulated,false).snapshot
    }
}
