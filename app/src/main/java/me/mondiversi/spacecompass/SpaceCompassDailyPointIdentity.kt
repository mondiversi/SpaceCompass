package me.mondiversi.spacecompass

import java.time.Instant
import java.time.ZoneId

/** A displayed number or event occurrence, independent of the observer's current civil date. */
internal data class SpaceCompassDailyPointIdentity(val event: SpaceCompassSunPathEvent,
    val number: Int? = null, val occurrence: Int = 0)

/** Built once per immutable path; selection checks never rescan/sort markers on sensor frames. */
internal class SpaceCompassDailyPointIndex(private val path: SpaceCompassSunDailyPath) {
    private val identities = mutableMapOf<SpaceCompassSunPathPoint,SpaceCompassDailyPointIdentity>()
    private val points = mutableMapOf<Pair<SpaceCompassDailyPointIdentity,SpaceCompassCelestialBody?>,SpaceCompassSunPathPoint>()
    init {
        val markers=path.markers.sortedBy { it.timeMs }.distinctBy { it.timeMs to it.event }
        val occurrences=mutableMapOf<Int,Int>()
        markers.filter { it.event==SpaceCompassSunPathEvent.HOUR }.forEachIndexed { index,p ->
            val number=if(path.body.isEarthSatellite && !path.forward24Hours) index else
                Instant.ofEpochMilli(p.timeMs).atZone(path.zone).hour
            val occurrence=occurrences[number] ?: 0;occurrences[number]=occurrence+1
            val identity=SpaceCompassDailyPointIdentity(SpaceCompassSunPathEvent.HOUR,number,occurrence)
            identities[p]=identity;points[identity to null]=p
        }
        for(event in SpaceCompassSunPathEvent.entries.filter { it!=SpaceCompassSunPathEvent.HOUR }) {
            markers.filter { event in it.events }.groupBy { if(event==SpaceCompassSunPathEvent.APPROACH) it.approach?.other else null }
                .forEach { (other,group) -> group.forEachIndexed { index,p ->
                    val identity=SpaceCompassDailyPointIdentity(event,occurrence=index)
                    if(p.event==event) identities[p]=identity
                    points[identity to other]=p
                } }
        }
    }
    fun identity(point: SpaceCompassSunPathPoint) = identities[point]
    fun point(identity: SpaceCompassDailyPointIdentity,other: SpaceCompassCelestialBody?) =
        points[identity to other.takeIf { identity.event==SpaceCompassSunPathEvent.APPROACH }]
}

internal fun spaceCompassOrbitPointKey(path: SpaceCompassSunDailyPath,point: SpaceCompassSunPathPoint,
    current: Boolean = false): SpaceCompassOrbitPointKey = spaceCompassOrbitPointKey(path.body,point,path.zone,
        spaceCompassPathSelectionDate(path,point).takeUnless { current },current,path.comparison?.id).let { key ->
        if(current || path.comparison!=null) key else key.copy(daily=path.pointIdentities.identity(point))
    }

/** Resolve real markers only. Missing events stay selected but cannot leave yesterday's geometry on screen. */
internal fun spaceCompassResolveOrbitSelection(saved: SpaceCompassSelectedOrbitPoint,
    path: SpaceCompassSunDailyPath?,current: SpaceCompassSunPathPoint? = null,
    zone: ZoneId = path?.zone ?: ZoneId.of(saved.key.zoneId)): SpaceCompassSelectedOrbitPoint? {
    val key=saved.key
    if(path!=null && (path.body!=key.body || path.comparison?.id!=key.comparisonId)) return null
    fun resolved(point: SpaceCompassSunPathPoint,source: SpaceCompassSunDailyPath?,daily: SpaceCompassDailyPointIdentity?): SpaceCompassSelectedOrbitPoint? {
        if(!point.position.azimuthDegrees.isFinite() || !point.position.elevationDegrees.isFinite() ||
            point.position.elevationDegrees !in -90.0..90.0) return null
        val fresh=spaceCompassOrbitPointKey(key.body,point,source?.zone ?: zone,
            source?.let { spaceCompassPathSelectionDate(it,point) }.takeUnless { key.isCurrent },key.isCurrent,key.comparisonId)
            .copy(daily=daily)
        return if(fresh==key && point==saved.point && source==saved.path) saved else SpaceCompassSelectedOrbitPoint(fresh,point,source)
    }
    if(key.isCurrent) {
        val point=current ?: return null
        if(key.comparisonId!=null && spaceCompassOrbitPointKey(key.body,point,zone,current=true,comparisonId=key.comparisonId)!=key) return null
        return resolved(point,path,null)
    }
    path ?: return null
    if(key.comparisonId!=null) {
        if(!spaceCompassPathAcceptsSelection(path,saved)) return null
        val marker=path.markers.firstOrNull { spaceCompassOrbitPointKeysMatch(key,spaceCompassOrbitPointKey(path,it)) }
        return resolved(marker ?: saved.point,if(marker==null) saved.path ?: path else path,null)
    }
    var identity=key.daily ?: saved.path?.pointIdentities?.identity(saved.point)
    // Old v1 files have no identity extension. First bind their exact original marker when available.
    if(identity==null) {
        val marker=path.markers.firstOrNull { spaceCompassOrbitPointKeysMatch(key,spaceCompassOrbitPointKey(path,it)) }
        if(marker!=null) identity=path.pointIdentities.identity(marker)
        if(identity==null && !key.body.isEarthSatellite) {
            if(key.event==SpaceCompassSunPathEvent.HOUR) {
                val local=Instant.ofEpochMilli(saved.point.timeMs).atZone(ZoneId.of(key.zoneId))
                if(local.minute==0 && local.second==0) identity=SpaceCompassDailyPointIdentity(SpaceCompassSunPathEvent.HOUR,
                    local.hour,local.zone.rules.getValidOffsets(local.toLocalDateTime()).indexOf(local.offset).coerceAtLeast(0))
            } else if(key.event!=SpaceCompassSunPathEvent.APPROACH) key.event?.let { identity=SpaceCompassDailyPointIdentity(it) }
        }
    }
    if(identity!=null) {
        val marker=path.pointIdentities.point(identity!!,key.approachBody)
        if(marker==null) {
            // Pre-extension arbitrary hourly instants have no known ordinal; retain only their bounded original-day fallback.
            if(key.daily==null && saved.path?.pointIdentities?.identity(saved.point)==null && key.event==SpaceCompassSunPathEvent.HOUR &&
                !path.forward24Hours && path.date==key.date && path.zone.id==key.zoneId)
                return resolved(saved.point,saved.path ?: path,null)
            return null
        }
        if(path.forward24Hours && (path.samples.isEmpty() || marker.timeMs !in path.samples.first().timeMs..path.samples.last().timeMs)) return null
        if(!path.forward24Hours && !path.body.isEarthSatellite && path.date!=key.date &&
            Instant.ofEpochMilli(marker.timeMs).atZone(path.zone).toLocalDate()!=path.date) return null
        return resolved(marker,path,identity)
    }
    // Legacy arbitrary instants without a numbered marker keep their old, bounded same-day behavior.
    if(path.zone.id!=key.zoneId || if(path.forward24Hours) path.samples.isEmpty() ||
        saved.point.timeMs !in path.samples.first().timeMs..path.samples.last().timeMs else path.date!=key.date) return null
    val marker=path.markers.firstOrNull { spaceCompassOrbitPointKeysMatch(key,spaceCompassOrbitPointKey(path,it)) }
    return resolved(marker ?: saved.point,if(marker==null) saved.path ?: path else path,null)
}
