package me.mondiversi.spacecompass

import kotlin.math.*

internal data class SpaceCompassGlobeCoordinateAnchor(val degrees: Int,val latitude: Boolean,
    val point: SpaceCompassGlobeVector,val rim: SpaceCompassGlobeVector,val side: Int = 0)

/** Parallel labels belong to both visible limbs; meridian labels belong to the true equator. */
internal fun spaceCompassGlobeCoordinateAnchors(pose: SpaceCompassGlobePose): List<SpaceCompassGlobeCoordinateAnchor> = buildList {
    val north=spaceCompassGlobeView(SpaceCompassGlobeVector(0.0,1.0,0.0),pose)
    val east=spaceCompassGlobeView(SpaceCompassGlobeVector(1.0,0.0,0.0),pose)
    val greenwich=spaceCompassGlobeView(SpaceCompassGlobeVector(0.0,0.0,1.0),pose)
    for(curve in spaceCompassGeographicGrid) {
        val angle=Math.toRadians(curve.degrees.toDouble())
        if(!curve.latitude) {
            val p=spaceCompassGlobeView(SpaceCompassGlobeVector(sin(angle),0.0,cos(angle)),pose)
            if(p.z>=-1e-9) add(SpaceCompassGlobeCoordinateAnchor(curve.degrees,false,p,p))
            continue
        }
        val a=sin(angle)*north.z;val b=cos(angle)*east.z;val c=cos(angle)*greenwich.z
        val amplitude=hypot(b,c)
        val roots=if(amplitude>1e-12 && abs(a)<=amplitude+1e-10) {
            val middle=atan2(b,c);val turn=acos((-a/amplitude).coerceIn(-1.0,1.0))
            listOf(middle-turn,middle+turn).map { longitude ->
                spaceCompassGlobeView(SpaceCompassGlobeVector(cos(angle)*sin(longitude),sin(angle),cos(angle)*cos(longitude)),pose)
            }.sortedBy { it.x }
        } else emptyList()
        val distinct=roots.size==2 && hypot(roots[0].x-roots[1].x,roots[0].y-roots[1].y)>1e-7
        if(distinct) roots.forEachIndexed { index,p ->
            add(SpaceCompassGlobeCoordinateAnchor(curve.degrees,true,p,p,if(index==0) -1 else 1))
        } else {
            // A fully visible polar parallel does not meet the limb. Keep its true curve anchor
            // and use a leader to its perimeter label instead of inventing a limb intersection.
            val visible=curve.vertices.map { spaceCompassGlobeView(it,pose) }.filter { it.z>=-1e-9 }
            if(visible.isNotEmpty()) for(side in listOf(-1,1)) {
                val p=if(side<0) visible.minBy { it.x } else visible.maxBy { it.x }
                val rim=SpaceCompassGlobeVector(side*sqrt((1-p.y*p.y).coerceAtLeast(0.0)),p.y,0.0)
                add(SpaceCompassGlobeCoordinateAnchor(curve.degrees,true,p,rim,side))
            }
        }
    }
    // A geographic pole is a point, not a degenerate parallel or a perimeter value.
    for(degrees in listOf(-90,90)) {
        val p=spaceCompassGlobeView(SpaceCompassGlobeVector(0.0,degrees/90.0,0.0),pose)
        if(p.z>=-1e-9) add(SpaceCompassGlobeCoordinateAnchor(degrees,true,p,p))
    }
}
