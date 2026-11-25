package me.mondiversi.spacecompass

import java.time.ZoneId
import java.util.TimeZone

internal data class SpaceCompassPanoramaSelectedPoint(val body: SpaceCompassCelestialBody,
    val point: SpaceCompassSunPathPoint, val path: SpaceCompassSunDailyPath?, val isCurrent: Boolean,
    val isAntipode: Boolean = false, val comparison: SpaceCompassTemporalComparison? = path?.comparison)

/** Resolve only the shutter snapshot: later UI toggles, refreshes and exports cannot change the pins. */
internal fun resolveSpaceCompassPanoramaSelections(snapshot: SpaceCompassPanoramaSnapshot,
    current: Map<SpaceCompassCelestialBody, SpaceCompassSunPosition>): List<SpaceCompassPanoramaSelectedPoint> {
    val objects = snapshot.objects.associateBy { it.body to it.comparison?.id }
    val originals = snapshot.selectedPoints.mapNotNull { saved ->
        val item = objects[saved.key.body to saved.key.comparisonId] ?: return@mapNotNull null
        val position=if(item.comparison==null) current[item.body] else
            if(snapshot.observerPositionKnown) item.currentPosition(snapshot) else item.currentPoint?.position
        val currentPoint=spaceCompassCurrentPathPoint(item.body,item.referenceTime(snapshot.timeMs),position)
        val resolved=spaceCompassResolveOrbitSelection(saved,item.path,currentPoint,
            item.comparison?.zone ?: ZoneId.of(snapshot.zoneId)) ?: return@mapNotNull null
        SpaceCompassPanoramaSelectedPoint(item.body,resolved.point,resolved.path,saved.key.isCurrent,comparison=item.comparison)

    }.distinctBy { listOf(it.body, it.comparison?.id, it.isCurrent, it.point) }
    return originals + if (snapshot.showAntipodes) originals.map {
        it.copy(point = it.point.antipodalDirection(), isAntipode = true)
    } else emptyList()
}

internal fun spaceCompassPanoramaPointSelected(selected: List<SpaceCompassPanoramaSelectedPoint>,
    body: SpaceCompassCelestialBody, point: SpaceCompassSunPathPoint, comparisonId: String? = null): Boolean =
    selected.any { !it.isAntipode && !it.isCurrent && it.body == body && it.comparison?.id == comparisonId && it.point == point }

/** Share source point names between adaptive globe captions and captured/exported labels. */
internal fun spaceCompassPanoramaSelectionName(snapshot: SpaceCompassPanoramaSnapshot,
    selected: SpaceCompassPanoramaSelectedPoint,
    pointNames: SpaceCompassSunPathPointNames? = selected.path?.let { spaceCompassPanoramaPathPointNames(snapshot,it) },
    visualAntipode: Boolean = false): String {
    val anchor = selected.isCurrent || selected.comparison?.let { c ->
        c.source == SpaceCompassComparisonSource.HEIGHT && SpaceCompassSunPathEvent.CULMINATION in selected.point.events &&
            kotlin.math.abs(selected.point.timeMs - c.timeMs) <= 120_000L
    } == true
    val name = if (anchor) selected.comparison?.let { snapshot.comparisonPointName(it) } ?: snapshot.currentPointName
        else spaceCompassPanoramaPathPointName(selected.point,snapshot.formatting,snapshot.pointEventNames,snapshot.bodyNames,pointNames)
    return if (!selected.isAntipode) name else if (visualAntipode) spaceCompassAntipodeCaptionName(name)
        else spaceCompassAntipodePointName(name,snapshot.orbitExportVocabulary.antipode)
}

/** Antipodes identify both the original object and point, with a localized parenthetical suffix. */
internal fun formatSpaceCompassPanoramaSelection(snapshot: SpaceCompassPanoramaSnapshot,
    selected: SpaceCompassPanoramaSelectedPoint,
    pointNames: SpaceCompassSunPathPointNames? = selected.path?.let { spaceCompassPanoramaPathPointNames(snapshot,it) },
    visualAntipode: Boolean = false): String {
    val referenceTime = selected.comparison?.timeMs ?: snapshot.timeMs
    val displayZone = selected.comparison?.let { TimeZone.getTimeZone(snapshot.comparisonDisplayZoneId(it)) } ?: snapshot.displayTimeZone
    val moment = if (selected.comparison != null) null else if (selected.isCurrent) snapshot.currentTime else snapshot.markerTimes[selected.point.timeMs]
    val time = moment ?: formatSpaceCompassPanoramaMarkerTime(selected.point.timeMs,referenceTime,
        displayZone, snapshot.formatting)
    val name = spaceCompassPanoramaSelectionName(snapshot,selected,pointNames,visualAntipode)
    val title = if (selected.isAntipode) {
        val objectName = snapshot.objects.firstOrNull { it.body == selected.body && it.comparison?.id == selected.comparison?.id }?.name
            ?: snapshot.bodyNames[selected.body] ?: selected.body.name
        listOf(objectName,name).filter(String::isNotBlank).joinToString(" · ")
    } else name
    return formatSpaceCompassPanoramaPointLabel(time,selected.point.position,snapshot.formatting,title,
        formatSpaceCompassPanoramaPointCountdown(selected.point.timeMs,referenceTime,snapshot.formatting))
}
