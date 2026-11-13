package me.mondiversi.spacecompass

import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

/** Collect only crossings emitted by the existing projector; no second ray/trigonometry pass. */
internal class SpaceCompassLiveGridNumbers(private val frame: SpaceCompassSunSceneFrame) : SpaceCompassLiveGridSink {
    val ticks = mutableListOf<SpaceCompassCameraGridTick>()
    private var axis=SpaceCompassCameraGridAxis.AZIMUTH
    private var degrees=0.0
    private var x=Double.NaN
    private var y=Double.NaN
    override fun beginCurve(axis: SpaceCompassCameraGridAxis, degrees: Double) {
        this.axis=axis;this.degrees=degrees;x=Double.NaN;y=Double.NaN
    }
    override fun moveTo(x: Double,y: Double) { this.x=x;this.y=y }
    override fun lineTo(x: Double,y: Double) {
        if(this.x.isFinite()) {
            crossing(this.x,x,this.y,y,frame.left,false,SpaceCompassCameraGridEdge.LEFT)
            crossing(this.x,x,this.y,y,frame.left+frame.width,false,SpaceCompassCameraGridEdge.RIGHT)
            crossing(this.y,y,this.x,x,frame.top,true,SpaceCompassCameraGridEdge.TOP)
            crossing(this.y,y,this.x,x,frame.top+frame.height,true,SpaceCompassCameraGridEdge.BOTTOM)
        }
        this.x=x;this.y=y
    }
    private fun crossing(a: Double,b: Double,c: Double,d: Double,border: Double,horizontal: Boolean,
        edge: SpaceCompassCameraGridEdge) {
        if(abs(b-a)<1e-9||border<min(a,b)-1e-6||border>max(a,b)+1e-6) return
        val other=c+(d-c)*(border-a)/(b-a)
        val low=if(horizontal) frame.left else frame.top
        val high=low+if(horizontal) frame.width else frame.height
        if(other<low-1e-6||other>high+1e-6) return
        val point=if(horizontal) SpaceCompassSunScenePoint(other,border) else SpaceCompassSunScenePoint(border,other)
        if(ticks.any { it.axis==axis&&it.degrees==degrees&&it.edge==edge&&
            abs(it.point.x-point.x)+abs(it.point.y-point.y)<1 }) return
        ticks+=SpaceCompassCameraGridTick(axis,degrees,edge,point)
    }
}
