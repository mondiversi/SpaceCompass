package me.mondiversi.spacecompass

import kotlin.math.*

internal data class SpaceCompassGlobeVector(val x: Double, val y: Double, val z: Double) {
    val length: Double get() = sqrt(x*x+y*y+z*z)
    fun times(scale: Double) = SpaceCompassGlobeVector(x*scale,y*scale,z*scale)
    fun direction(radius: Double = 1.55): SpaceCompassGlobeVector? =
        length.takeIf { it.isFinite() && it > 1e-12 }?.let { times(radius/it) }
}

internal data class SpaceCompassGlobePose(val longitude: Double = 0.0, val latitude: Double = 0.0,
    val zoom: Double = 1.0, val roll: Double = 0.0) {
    fun normalized(maximumZoom: Double = 3.0) = SpaceCompassGlobePose((longitude.takeIf(Double::isFinite) ?: 0.0).let { atan2(sin(it),cos(it)) },
        (latitude.takeIf(Double::isFinite) ?: 0.0).coerceIn(-PI/2,PI/2),(zoom.takeIf(Double::isFinite) ?: 1.0).coerceIn(.5,maximumZoom),roll.takeIf(Double::isFinite) ?: 0.0)
}

/** Same Earth-fixed axes and inverse texture rotation as the offline screensaver. */
internal fun spaceCompassGlobeView(v: SpaceCompassGlobeVector, pose: SpaceCompassGlobePose): SpaceCompassGlobeVector {
    val x=v.x*cos(pose.longitude)-v.z*sin(pose.longitude)
    val z=v.x*sin(pose.longitude)+v.z*cos(pose.longitude)
    val y=v.y*cos(pose.latitude)-z*sin(pose.latitude)
    return SpaceCompassGlobeVector(x*cos(pose.roll)+y*sin(pose.roll),-x*sin(pose.roll)+y*cos(pose.roll),
        v.y*sin(pose.latitude)+z*cos(pose.latitude))
}

/** WGS84 ENU -> ECEF, then the renderer's +X=90E,+Y=north,+Z=Greenwich convention.
 * https://gssc.esa.int/navipedia/index.php/Transformations_between_ECEF_and_ENU_coordinates
 * Add the observer only for a known range; direction-only catalogs never acquire an invented distance.
 */
internal fun spaceCompassGlobeEarthFixed(position: SpaceCompassSunPosition, latitude: Double,
    longitude: Double, altitude: Double, distanceKm: Double?): SpaceCompassGlobeVector? {
    if (!listOf(position.azimuthDegrees,position.elevationDegrees,latitude,longitude,altitude).all(Double::isFinite) ||
        latitude !in -90.0..90.0 || longitude !in -180.0..180.0) return null
    val az=Math.toRadians(position.azimuthDegrees); val el=Math.toRadians(position.elevationDegrees)
    val lat=Math.toRadians(latitude);val lon=Math.toRadians(longitude)
    val e=cos(el)*sin(az);val n=cos(el)*cos(az);val u=sin(el)
    val range=distanceKm?.takeIf { it.isFinite() && it > 0 }
    val earthN=6378.137/sqrt(1-.00669437999014*sin(lat).pow(2))
    val h=altitude/1000
    val horizontal=if(range!=null) (earthN+h)*cos(lat) else 0.0
    val vertical=if(range!=null) (earthN*(1-.00669437999014)+h)*sin(lat) else 0.0
    val scale=range ?: 1.0
    val x=horizontal*cos(lon)+scale*(-sin(lon)*e-sin(lat)*cos(lon)*n+cos(lat)*cos(lon)*u)
    val y=horizontal*sin(lon)+scale*(cos(lon)*e-sin(lat)*sin(lon)*n+cos(lat)*sin(lon)*u)
    val z=vertical+scale*(cos(lat)*n+sin(lat)*u)
    return SpaceCompassGlobeVector(y,z,x)
}

internal data class SpaceCompassGlobeDot(val vector: SpaceCompassGlobeVector,
    val point: SpaceCompassSunPathPoint, val current: Boolean = false, val selected: Boolean = false,
    val antipode: Boolean = false)
internal data class SpaceCompassGlobeTrack(val item: SpaceCompassPanoramaObject,
    val curve: List<Pair<SpaceCompassGlobeVector,Boolean>>, val dots: List<SpaceCompassGlobeDot>)
internal data class SpaceCompassGlobeModel(val tracks: List<SpaceCompassGlobeTrack>, val extent: Double)

/** Reuse verified topocentric ephemerides and undo the observer translation, off the UI thread.
 * Satellites keep physical Earth radii. Distant objects use a labelled direction shell, not false Earth orbits.
 */
internal fun buildSpaceCompassGlobeModel(snapshot: SpaceCompassPanoramaSnapshot,
    curveCache: MutableMap<SpaceCompassSunDailyPath,List<Pair<SpaceCompassGlobeVector,Boolean>>>? = null,
    sky: Boolean = false, checkCancelled: () -> Unit = {}): SpaceCompassGlobeModel {
    if(!snapshot.observerPositionKnown) return SpaceCompassGlobeModel(emptyList(),1.55)
    fun vector(body: SpaceCompassCelestialBody, p: SpaceCompassSunPathPoint): SpaceCompassGlobeVector? {
        checkCancelled()
        if(sky) return spaceCompassSkyPanoramaVector(p.position)
        val observation=runCatching { calculateSpaceCompassCelestialObservation(body,p.timeMs,
            snapshot.latitude,snapshot.longitude,snapshot.altitude,snapshot.remote) }.getOrNull()
        // Missing orbit/ephemeris data must never create a fictitious current or trajectory.
        if ((body.isEarthSatellite || body.usesHorizons) && observation == null) return null
        val fixed=spaceCompassGlobeEarthFixed(p.position,snapshot.latitude,snapshot.longitude,snapshot.altitude,
            observation?.distanceKm) ?: return null
        return if (body.isEarthSatellite) fixed.times(1/6378.137) else fixed.direction()
    }
    val tracks=snapshot.objects.map { item ->
        val path=item.path
        val curve=path?.let { curveCache?.get(it) } ?: path?.samples.orEmpty().let { samples ->
            val step=maxOf(1,(samples.size+479)/480)
            samples.filterIndexed { i,_ -> i%step==0 || i==samples.lastIndex }
        }.mapNotNull { point -> vector(item.body,point)?.let { it to (point.position.elevationDegrees<0) } }
        if(path!=null && curveCache!=null) { if(curveCache.size>64)curveCache.clear();curveCache[path]=curve }
        val markers=path?.markers.orEmpty().mapNotNull { p -> vector(item.body,p)?.let { v ->
            val selected=snapshot.selectedPoints.any { saved -> saved.key.comparisonId==item.comparison?.id &&
                saved.key.body==item.body && !saved.key.isCurrent && spaceCompassOrbitPointKeysMatch(saved.key,
                    spaceCompassOrbitPointKey(item.body,p,path!!.zone,spaceCompassPathSelectionDate(path,p),comparisonId=item.comparison?.id)) }
            SpaceCompassGlobeDot(v,p,selected=selected)
        } }
        val now=if(item.comparison?.hasReference==false) null else runCatching {
            calculateSpaceCompassCelestialObservation(item.body,item.referenceTime(snapshot.timeMs),snapshot.latitude,
                snapshot.longitude,snapshot.altitude,snapshot.remote)
        }.getOrNull()?.let { spaceCompassCurrentPathPoint(item.body,item.referenceTime(snapshot.timeMs),it.position) }
        val current=now?.let { p -> vector(item.body,p)?.let { v -> SpaceCompassGlobeDot(v,p,current=true,
            selected=snapshot.selectedPoints.any { it.key.body==item.body && it.key.comparisonId==item.comparison?.id && it.key.isCurrent }) } }
        // Arbitrary selected instants can be absent from the hourly/event marker list.
        val extra=snapshot.selectedPoints.filter { it.key.body==item.body && it.key.comparisonId==item.comparison?.id && !it.key.isCurrent }
            .filter { saved -> markers.none { abs(it.point.timeMs-saved.point.timeMs)<120_000 } }
            .mapNotNull { saved -> vector(item.body,saved.point)?.let { SpaceCompassGlobeDot(it,saved.point,selected=true) } }
        val dots=listOfNotNull(current)+markers+extra
        val antipodes=if(snapshot.showAntipodes) dots.filter { it.selected }.map {
            it.copy(vector=it.vector.times(-1.0),point=it.point.antipodalDirection(),antipode=true)
        } else emptyList()
        SpaceCompassGlobeTrack(item,curve,dots+antipodes)
    }
    val extent=maxOf(1.55,tracks.flatMap { t -> t.curve.map { it.first.length }+t.dots.map { it.vector.length } }.maxOrNull() ?: 1.55)
    return SpaceCompassGlobeModel(tracks,extent)
}

/** Exact inverse of the shared texture/point transform, including the seasonal apparent roll. */
internal fun spaceCompassGlobeInverseView(v: SpaceCompassGlobeVector,pose: SpaceCompassGlobePose): SpaceCompassGlobeVector {
    val x=v.x*cos(pose.roll)-v.y*sin(pose.roll)
    val y=v.x*sin(pose.roll)+v.y*cos(pose.roll)
    val ay=y*cos(pose.latitude)+v.z*sin(pose.latitude)
    val az=-y*sin(pose.latitude)+v.z*cos(pose.latitude)
    return SpaceCompassGlobeVector(x*cos(pose.longitude)+az*sin(pose.longitude),ay,
        -x*sin(pose.longitude)+az*cos(pose.longitude))
}
internal fun spaceCompassGlobeEffectivePose(base: SpaceCompassGlobePose,season: SpaceCompassScreensaverSeason,
    axisLocked: Boolean)=base.copy(latitude=(season.pitchRadians+if(axisLocked) 0.0 else base.latitude).coerceIn(-PI/2,PI/2),
        roll=season.rollRadians).normalized()
internal fun spaceCompassGlobeDrag(pose: SpaceCompassGlobePose,dx: Double,dy: Double,width: Double,height: Double,
    axisLocked: Boolean): SpaceCompassGlobePose {
    if(width<=0||height<=0)return pose
    return pose.copy(longitude=pose.longitude+dx/width*PI*2,
        latitude=if(axisLocked) pose.latitude else pose.latitude-dy/height*PI).normalized()
}
/** Decode the legacy stored multiplier; the renderer defines 1x as the complete trajectory envelope. */
internal fun spaceCompassGlobeZoom(stored: Double,oldScale: Boolean)=
    ((if(stored.isFinite()&&stored>0) stored else if(oldScale) 1.4 else 1.0)/(if(oldScale) 1.4 else 1.0)).coerceIn(.5,3.0)
internal fun spaceCompassGlobeViewport(width: Double,height: Double,top: Double,bottom: Double): SpaceCompassSunSceneFrame {
    val first=top.coerceIn(0.0,height*.45);val last=(height-bottom).coerceIn(first+height*.1,height)
    return SpaceCompassSunSceneFrame(0.0,first,width,last-first)
}

/** Rotation-invariant trajectory envelope, centred in the free frame, with space for marker ink.
 * Frame dimensions and margin must use the same units (CSS dp live, output px in captures).
 */
internal fun spaceCompassGlobeFitRadius(frame: SpaceCompassSunSceneFrame,extent: Double,zoom: Double,
    margin: Double=24.0): Double =
    (min(frame.width,frame.height)/2-margin).coerceAtLeast(1.0)/extent.coerceAtLeast(1.0)*zoom

internal fun spaceCompassGlobeKeyEvent(event: SpaceCompassSunPathEvent): Boolean = when(event) {
    SpaceCompassSunPathEvent.SUNRISE,SpaceCompassSunPathEvent.CULMINATION,
    SpaceCompassSunPathEvent.SUNSET,SpaceCompassSunPathEvent.MINIMUM -> true
    else -> false
}

/** Tight rectangular envelope, stable through longitude rotation. The Earth's own disc always fits.
 * A flattened apparent daily path need not reserve a full 3-D sphere's height in landscape. */
internal data class SpaceCompassGlobeFitBounds(val horizontal: Double,val vertical: Double)
internal fun spaceCompassGlobeFitBounds(model: SpaceCompassGlobeModel?,pose: SpaceCompassGlobePose): SpaceCompassGlobeFitBounds {
    if(model==null) return SpaceCompassGlobeFitBounds(1.55,1.55)
    val horizontalAxis=cos(pose.latitude)*sin(pose.roll)
    val verticalAxis=cos(pose.latitude)*cos(pose.roll)
    val horizontalRing=sqrt((1-horizontalAxis*horizontalAxis).coerceAtLeast(0.0))
    val verticalRing=sqrt((1-verticalAxis*verticalAxis).coerceAtLeast(0.0))
    var horizontal=1.0;var vertical=1.0
    fun include(v: SpaceCompassGlobeVector) {
        val ring=hypot(v.x,v.z)
        horizontal=max(horizontal,abs(v.y*horizontalAxis)+ring*horizontalRing)
        vertical=max(vertical,abs(v.y*verticalAxis)+ring*verticalRing)
    }
    model.tracks.forEach { track -> track.curve.forEach { include(it.first) };track.dots.forEach { include(it.vector) } }
    return SpaceCompassGlobeFitBounds(horizontal,vertical)
}
internal fun spaceCompassGlobeFitRadius(frame: SpaceCompassSunSceneFrame,bounds: SpaceCompassGlobeFitBounds,
    zoom: Double,margin: Double=24.0): Double = min(
    (frame.width/2-margin).coerceAtLeast(1.0)/bounds.horizontal.coerceAtLeast(1.0),
    (frame.height/2-margin).coerceAtLeast(1.0)/bounds.vertical.coerceAtLeast(1.0))*zoom

/** A compact object strip is a real lower boundary, even when floating controls are hidden. */
internal fun spaceCompassGlobeViewportAboveFooter(frame: SpaceCompassSunSceneFrame,footerTop: Double,gap: Double): SpaceCompassSunSceneFrame =
    frame.copy(height=(min(frame.top+frame.height,footerTop-gap.coerceAtLeast(0.0))-frame.top).coerceAtLeast(1.0))

/** ENU angular shell only: no geographic coordinates or artificial distance. */
internal fun spaceCompassSkyPanoramaVector(position: SpaceCompassSunPosition): SpaceCompassGlobeVector? {
    if(!position.azimuthDegrees.isFinite() || !position.elevationDegrees.isFinite() || position.elevationDegrees !in -90.0..90.0) return null
    val az=Math.toRadians(position.azimuthDegrees);val el=Math.toRadians(position.elevationDegrees)
    return SpaceCompassGlobeVector(cos(el)*sin(az),sin(el),cos(el)*cos(az))
}
