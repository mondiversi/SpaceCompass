package me.mondiversi.spacecompass

import kotlin.math.*
import org.junit.Assert.*
import org.junit.Test

class SpaceCompassLiveGridProjectorTest {
    private data class Command(val move: Boolean,val x: Double,val y: Double)
    private class RecordingSink : SpaceCompassLiveGridSink {
        val commands=ArrayList<Command>()
        override fun moveTo(x: Double,y: Double) { commands+=Command(true,x,y) }
        override fun lineTo(x: Double,y: Double) { commands+=Command(false,x,y) }
    }
    private fun orientation(azimuth: Double,elevation: Double,roll: Double): SpaceCompassSunOrientation {
        val a=Math.toRadians(azimuth);val e=Math.toRadians(elevation);val r=Math.toRadians(roll)
        val right=SpaceCompassSunVector(cos(a),-sin(a),0.0)
        val up=SpaceCompassSunVector(-sin(a)*sin(e),-cos(a)*sin(e),cos(e))
        fun mix(x: SpaceCompassSunVector,y: SpaceCompassSunVector,c: Double,s: Double)=
            SpaceCompassSunVector(x.east*c+y.east*s,x.north*c+y.north*s,x.up*c+y.up*s)
        return SpaceCompassSunOrientation(mix(right,up,cos(r),sin(r)),mix(up,right,cos(r),-sin(r)),
            SpaceCompassSunVector(sin(a)*cos(e),cos(a)*cos(e),sin(e)))
    }
    @Test fun liveCommandsMatchCaptureGeometryAcrossPolesNorthWrapRollZoomAndShiftedOptics() {
        for((w,h) in listOf(1080.0 to 2280.0,2280.0 to 1080.0)) {
            for(lens in listOf(SpaceCompassPerspective(.15,.23,.48,.61),SpaceCompassPerspective(.8,.9),
                SpaceCompassPerspective(5.0,8.0,.72,.25))) {
                val projector=SpaceCompassLiveGridProjector(w,h,lens)
                for(el in listOf(-90.0,-89.99,-25.0,0.0,45.0,89.99,90.0)) {
                    for(roll in listOf(0.0,32.0,90.0,180.0,270.0)) {
                        val pose=orientation(if(roll==0.0) 355.0 else 3.0,el,roll)
                        val expected=RecordingSink()
                        val reference=spaceCompassCameraAngularGrid(pose,lens,w,h,
                            SpaceCompassSunSceneFrame(0.0,0.0,w,h),includeBorderTicks=false)
                        var segmentCount=0
                        for(line in reference.lines) {
                            var last: SpaceCompassSunScenePoint?=null
                            for(segment in line.segments) {
                                if(last==null||hypot(last.x-segment.start.x,last.y-segment.start.y)>.2)
                                    expected.moveTo(segment.start.x,segment.start.y)
                                expected.lineTo(segment.end.x,segment.end.y);last=segment.end;segmentCount++
                            }
                        }
                        val actual=RecordingSink()
                        assertEquals(segmentCount,projector.project(pose,actual))
                        val description="${w}x$h, $lens, elevation=$el, roll=$roll"
                        assertEquals(description,expected.commands.size,actual.commands.size)
                        expected.commands.zip(actual.commands).forEach { (a,b) ->
                            assertEquals(description,a.move,b.move)
                            assertEquals(description,a.x,b.x,1e-5);assertEquals(description,a.y,b.y,1e-5)
                            assertTrue(b.x.isFinite()&&b.y.isFinite())
                            assertTrue(b.x in 0.0..w&&b.y in 0.0..h)
                        }
                    }
                }
            }
        }
    }
    @Test fun liveProjectionRetainsTheChosenGridInterval() {
        for(step in listOf(5,10,15,30)) {
            val pose=orientation(155.0,24.0,13.0);val lens=SpaceCompassPerspective(.5,.7)
            val grid=spaceCompassCameraAngularGrid(pose,lens,800.0,600.0,
                SpaceCompassSunSceneFrame(0.0,0.0,800.0,600.0),step,includeBorderTicks=false)
            assertEquals(grid.lines.sumOf { it.segments.size },
                SpaceCompassLiveGridProjector(800.0,600.0,lens,step).project(pose,RecordingSink()))
        }
    }
}
