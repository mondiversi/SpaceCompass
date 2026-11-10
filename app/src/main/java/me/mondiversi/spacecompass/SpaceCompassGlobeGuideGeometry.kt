package me.mondiversi.spacecompass

import kotlin.math.*

internal val spaceCompassGlobeGeographicParallels = listOf(
    -66.56 to SpaceCompassSkyReference.ANTARCTIC, -23.44 to SpaceCompassSkyReference.CAPRICORN,
    0.0 to SpaceCompassSkyReference.EQUATOR, 23.44 to SpaceCompassSkyReference.CANCER,
    66.56 to SpaceCompassSkyReference.ARCTIC)

internal data class SpaceCompassGlobeGuideSegments(val front: List<SpaceCompassSunPathSegment>,
    val back: List<SpaceCompassSunPathSegment>)

/** Geographic, clipped arc fragments: captions cannot bridge the dateline or the hidden hemisphere. */
internal fun spaceCompassGlobeGuideSegments(latitude: Double, pose: SpaceCompassGlobePose, flat: Boolean,
    width: Double, height: Double, radius: Double, centerY: Double): SpaceCompassGlobeGuideSegments {
    val front=mutableListOf<SpaceCompassSunPathSegment>();val back=mutableListOf<SpaceCompassSunPathSegment>()
    val frame=SpaceCompassSunSceneFrame(0.0,0.0,width,height)
    val phi=Math.toRadians(latitude)
    fun vector(i: Int)= (i*PI/120).let { SpaceCompassGlobeVector(cos(phi)*sin(it),sin(phi),cos(phi)*cos(it)) }
    fun point(v: SpaceCompassGlobeVector)=SpaceCompassSunScenePoint(width/2+radius*v.x,centerY-radius*v.y)
    fun add(a: SpaceCompassGlobeVector,b: SpaceCompassGlobeVector,hidden: Boolean) {
        spaceCompassCameraGridClip(SpaceCompassSunPathSegment(point(a),point(b),false),frame)?.let {
            (if(hidden) back else front)+=it
        }
    }
    for(i in 1..240) {
        if(flat) {
            spaceCompassGlobeMapSegments(spaceCompassGlobeMapPoint(vector(i-1),width,height),
                spaceCompassGlobeMapPoint(vector(i),width,height),width).forEach { (a,b) ->
                front+=SpaceCompassSunPathSegment(a,b,false)
            }
        } else {
            val a=spaceCompassGlobeView(vector(i-1),pose);val b=spaceCompassGlobeView(vector(i),pose)
            if((a.z<0)==(b.z<0)) add(a,b,a.z<0)
            else {
                val t=a.z/(a.z-b.z);val limb=SpaceCompassGlobeVector(a.x+(b.x-a.x)*t,a.y+(b.y-a.y)*t,0.0)
                add(a,limb,a.z<0);add(limb,b,b.z<0)
            }
        }
    }
    // The visible arc can cross the 0/360 sampling origin. Join its matching ends before
    // measuring a long name; otherwise each half is incorrectly treated as too short.
    if(!flat && front.size>1 && hypot(front.last().end.x-front.first().start.x,
        front.last().end.y-front.first().start.y)<.5) {
        val gap=(1 until front.size).firstOrNull { i -> hypot(front[i-1].end.x-front[i].start.x,
            front[i-1].end.y-front[i].start.y)>.5 }
        if(gap!=null) return SpaceCompassGlobeGuideSegments(front.drop(gap)+front.take(gap),back)
    }
    return SpaceCompassGlobeGuideSegments(front,back)
}

/** Keep the live 12 dp guide lettering proportional to the height-filling capture viewport.
 * Cap the live scale against point-pill text so tall framed captures remain balanced. */
internal fun spaceCompassGlobeGuideTextSize(sceneWidth: Int,sceneHeight: Int,framing: SpaceCompassGlobeFraming?): Float {
    val ratio=framing?.guideTextSizeByHeight?.takeIf { it.isFinite()&&it>0 } ?: (12.0/600.0)
    // Point pills use sceneWidth / 100. Keep guides close to that typography even in tall frames.
    return minOf(sceneHeight*ratio,sceneWidth/100.0*1.1).toFloat()
}
