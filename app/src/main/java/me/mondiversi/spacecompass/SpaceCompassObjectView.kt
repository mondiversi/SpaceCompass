package me.mondiversi.spacecompass

import kotlin.math.*

internal const val SPACE_COMPASS_OBJECT_VIEW_KEY="object_view"
// Retained only to discard the obsolete observer-aim preference from older scene files.
internal const val SPACE_COMPASS_OBJECT_AIM_KEY="object_view_observer"
internal const val SPACE_COMPASS_OBJECT_EARTH_YAW_KEY="object_earth_yaw_v1"
internal const val SPACE_COMPASS_OBJECT_EARTH_PITCH_KEY="object_earth_pitch_v1"
internal const val SPACE_COMPASS_OBJECT_ZOOM_KEY="object_view_zoom"
internal const val SPACE_COMPASS_OBJECT_LABEL_MODE_KEY="object_label_mode"

/** Camera in the same display coordinates as the Globe tracks. Non-satellite ranges
 * are an apparent display shell, never a physical spacecraft position or measured range. */
internal data class SpaceCompassObjectCamera(val pose: SpaceCompassGlobePose,
    val eye: SpaceCompassGlobeVector?,val target: SpaceCompassGlobeVector) {
    val focal: Double=eye?.let { sqrt((it.length*it.length-1).coerceAtLeast(.000001)) } ?: 1.0
    // Keep field of view constant while looking around, including across the horizon.
    val framing=SpaceCompassGlobeVector(0.0,0.0,1.0)
    fun inFront(world: SpaceCompassGlobeVector)=eye?.let { it.z-spaceCompassGlobeView(world,pose).z>1e-6 } ?: true
    fun project(world: SpaceCompassGlobeVector): SpaceCompassGlobeVector {
        val p=spaceCompassGlobeView(world,pose)
        val normal=p.direction(1.0) ?: return SpaceCompassGlobeVector(0.0,0.0,-1.0)
        return eye?.let { e ->
            val z=(e.z-p.z).coerceAtLeast(.000001)
            SpaceCompassGlobeVector(((p.x-e.x)*focal/z-framing.x)*framing.z,((p.y-e.y)*focal/z-framing.y)*framing.z,
                min(normal.x*e.x+normal.y*e.y+normal.z*e.z-1,e.z-p.z))
        } ?: SpaceCompassGlobeVector(p.x-target.x,p.y-target.y,p.z)
    }
    /** Front intersection of a pixel ray and the unit sphere, in camera-oriented axes. */
    fun normal(x: Double,y: Double): SpaceCompassGlobeVector? {
        val e=eye ?: return (1-(x+target.x).pow(2)-(y+target.y).pow(2)).takeIf { it>=0 }?.let {
            SpaceCompassGlobeVector(x+target.x,y+target.y,sqrt(it)) }
        val qx=x/framing.z+framing.x;val qy=y/framing.z+framing.y
        val ray=SpaceCompassGlobeVector(qx/focal,qy/focal,-1.0).direction(1.0)!!
        val t=-(e.x*ray.x+e.y*ray.y+e.z*ray.z)
        if(t<=0)return null
        val q=SpaceCompassGlobeVector(e.x+t*ray.x,e.y+t*ray.y,e.z+t*ray.z)
        val h=1-q.length*q.length
        if(h< -1e-10)return null
        val s=sqrt(h.coerceAtLeast(0.0))
        return SpaceCompassGlobeVector(q.x-ray.x*s,q.y-ray.y*s,q.z-ray.z*s)
    }
}


internal data class SpaceCompassObjectEarthRotation(val yaw: Double=0.0,val pitch: Double=0.0) {
    fun normalized()=SpaceCompassObjectEarthRotation(yaw.takeIf(Double::isFinite)?.let { atan2(sin(it),cos(it)) } ?: 0.0,
        (pitch.takeIf(Double::isFinite) ?: 0.0).coerceIn(-PI/2,PI/2))
}

internal fun spaceCompassObjectEarthDrag(look: SpaceCompassObjectEarthRotation,dx: Double,dy: Double,
    width: Double,height: Double,zoom: Double): SpaceCompassObjectEarthRotation {
    if(width<=0||height<=0||!listOf(dx,dy,width,height,zoom).all(Double::isFinite))return look
    val sensitivity=zoom.coerceAtLeast(1.0)
    return SpaceCompassObjectEarthRotation(look.yaw+dx/width*PI/sensitivity,look.pitch-dy/height*PI/sensitivity).normalized()
}

/** Rotate the complete Earth reference frame beneath a fixed forward-looking orbital view. */
internal fun SpaceCompassObjectCamera.rotatingEarth(look: SpaceCompassObjectEarthRotation): SpaceCompassObjectCamera {
    val relative=look.normalized()
    if(relative.yaw==0.0&&relative.pitch==0.0)return this
    val turn=SpaceCompassGlobePose(relative.yaw,relative.pitch)
    fun transformed(v: SpaceCompassGlobeVector)=spaceCompassGlobeView(spaceCompassGlobeView(v,pose),turn)
    val x=transformed(SpaceCompassGlobeVector(1.0,0.0,0.0))
    val y=transformed(SpaceCompassGlobeVector(0.0,1.0,0.0))
    val z=transformed(SpaceCompassGlobeVector(0.0,0.0,1.0))
    val latitude=asin(y.z.coerceIn(-1.0,1.0))
    val pole=abs(cos(latitude))<1e-8
    val aimed=SpaceCompassGlobePose(if(pole) 0.0 else atan2(x.z,z.z),latitude,pose.zoom,
        if(pole) atan2(-x.y,x.x) else atan2(y.x,y.y))
    // Geometry, grids, location and texture use the rotated pose together. The eye and
    // optical target stay in their original view coordinates, keeping the horizon fixed.
    return SpaceCompassObjectCamera(aimed,eye,target)
}

/** Forward orbital view: the centre ray grazes Earth, with local nadir below the eye. */
internal fun spaceCompassObjectCamera(orbitalPosition: SpaceCompassGlobeVector,
    forwardHint: SpaceCompassGlobeVector?=null): SpaceCompassObjectCamera? {
    val radius=orbitalPosition.length
    if(!radius.isFinite()||radius<=1.00001)return null
    val radial=orbitalPosition.direction(1.0) ?: return null
    fun dot(a: SpaceCompassGlobeVector,b: SpaceCompassGlobeVector)=a.x*b.x+a.y*b.y+a.z*b.z
    val tangent=forwardHint?.let { hint -> val radialPart=dot(hint,radial)
        SpaceCompassGlobeVector(hint.x-radialPart*radial.x,hint.y-radialPart*radial.y,hint.z-radialPart*radial.z).direction(1.0)
    } ?: SpaceCompassGlobeVector(radial.z,0.0,-radial.x).direction(1.0) ?: SpaceCompassGlobeVector(1.0,0.0,0.0)
    val depression=sqrt(1-1/(radius*radius))
    val forward=SpaceCompassGlobeVector(tangent.x/radius-radial.x*depression,
        tangent.y/radius-radial.y*depression,tangent.z/radius-radial.z*depression)
    val up=SpaceCompassGlobeVector(radial.x/radius+tangent.x*depression,
        radial.y/radius+tangent.y*depression,radial.z/radius+tangent.z*depression)
    val right=SpaceCompassGlobeVector(tangent.y*radial.z-tangent.z*radial.y,
        tangent.z*radial.x-tangent.x*radial.z,tangent.x*radial.y-tangent.y*radial.x)
    val back=forward.times(-1.0)
    val latitude=asin(back.y.coerceIn(-1.0,1.0));val pole=abs(cos(latitude))<1e-8
    val pose=SpaceCompassGlobePose(if(pole) 0.0 else atan2(back.x,back.z),latitude,1.0,
        if(pole) atan2(-up.x,right.x) else atan2(right.y,up.y))
    val horizon=SpaceCompassGlobeVector(orbitalPosition.x+forward.x*sqrt(radius*radius-1),
        orbitalPosition.y+forward.y*sqrt(radius*radius-1),orbitalPosition.z+forward.z*sqrt(radius*radius-1))
    return SpaceCompassObjectCamera(pose,spaceCompassGlobeView(orbitalPosition,pose),spaceCompassGlobeView(horizon,pose))
}

internal fun spaceCompassObjectCamera(body: SpaceCompassCelestialBody,timeMs: Long,latitude: Double,
    longitude: Double,altitude: Double,remote: SpaceCompassCelestialRemoteData): SpaceCompassObjectCamera? {
    fun at(instant: Long): SpaceCompassGlobeVector? {
        val observation=calculateSpaceCompassCelestialObservation(body,instant,latitude,longitude,altitude,remote) ?: return null
        val fixed=spaceCompassGlobeEarthFixed(observation.position,latitude,longitude,altitude,observation.distanceKm) ?: return null
        return spaceCompassGlobeDisplayVector(body,fixed)
    }
    val current=at(timeMs) ?: return null
    val next=at(timeMs+30000);val previous=if(next==null) at(timeMs-30000) else null
    val hint=next?.let { SpaceCompassGlobeVector(it.x-current.x,it.y-current.y,it.z-current.z) }
        ?: previous?.let { SpaceCompassGlobeVector(current.x-it.x,current.y-it.y,current.z-it.z) }
    return spaceCompassObjectCamera(current,hint)
}

internal fun spaceCompassGlobeCapturePoint(v: SpaceCompassGlobeVector,capture: SpaceCompassGlobeCapture)=
    capture.objectCamera?.project(v) ?: spaceCompassGlobeView(v,capture.pose)

internal fun spaceCompassObjectCoordinateAnchors(camera: SpaceCompassObjectCamera): List<SpaceCompassGlobeCoordinateAnchor> = buildList {
    for(curve in spaceCompassGeographicGrid) {
        val visible=curve.vertices.map(camera::project).filter { it.z>=0 }
        if(visible.isEmpty())continue
        if(curve.latitude) for(side in listOf(-1,1)) {
            val p=if(side<0) visible.minBy { it.x } else visible.maxBy { it.x }
            add(SpaceCompassGlobeCoordinateAnchor(curve.degrees,true,p,p,side))
        } else {
            val lon=Math.toRadians(curve.degrees.toDouble());val p=camera.project(SpaceCompassGlobeVector(sin(lon),0.0,cos(lon)))
            if(p.z>=0)add(SpaceCompassGlobeCoordinateAnchor(curve.degrees,false,p,p))
        }
    }
    for(sign in listOf(-1,1)) {
        val p=camera.project(SpaceCompassGlobeVector(0.0,sign.toDouble(),0.0))
        if(p.z>=0)add(SpaceCompassGlobeCoordinateAnchor(sign*90,true,p,p))
    }
}

/** Export samples the complete captured viewport; off-axis/near-Earth discs are not square-clipped. */
internal data class SpaceCompassObjectEarthFrame(val width: Int,val height: Int,val radius: Double,val centerY: Double)
