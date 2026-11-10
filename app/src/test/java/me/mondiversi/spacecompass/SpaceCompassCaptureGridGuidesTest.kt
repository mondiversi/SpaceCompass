package me.mondiversi.spacecompass

import org.junit.Assert.*
import org.junit.Test
import kotlin.math.*

class SpaceCompassCaptureGridGuidesTest {
    @Test fun capturedGuideTextRetainsItsLiveSizeAcrossFrameAndMapResolutions() {
        val liveHeight=1200.0;val liveText=32.4
        val framing=SpaceCompassGlobeFraming(1080.0,liveHeight,.5,.3,liveText/liveHeight)
        for(outputHeight in listOf(1024,2048,4096)) {
            val exported=spaceCompassGlobeGuideTextSize(outputHeight*4,outputHeight,framing)
            assertEquals(liveText,exported*liveHeight/outputHeight,1e-5)
        }
    }
    @Test fun guideTextRemainsBalancedWithPointLabelsInTallAndWideCaptures() {
        val framing=SpaceCompassGlobeFraming(1080.0,1200.0,.5,.3,32.4/1200.0)
        for((w,h) in listOf(2048 to 4096,4096 to 2048,4096 to 2304)) {
            val guide=spaceCompassGlobeGuideTextSize(w,h,framing)
            assertTrue(guide<=w/100f*1.1f+.001f)
            assertTrue(guide>0)
        }
        assertEquals(22.528f,spaceCompassGlobeGuideTextSize(2048,4096,null),.001f)
    }
    @Test fun longGeographicNamesCanCrossTheClosedSamplingOrigin() {
        val pose=SpaceCompassGlobePose(0.0,.25,1.0)
        val latitude=23.44;val phi=Math.toRadians(latitude)
        val p=spaceCompassGlobeView(SpaceCompassGlobeVector(0.0,sin(phi),cos(phi)),pose)
        val geometry=spaceCompassGlobeGuideSegments(latitude,pose,false,400.0,400.0,100.0,200.0)
        val center=SpaceCompassSunScenePoint(200+p.x*100,200-p.y*100)
        assertNotNull(spaceCompassOrbitLabelBaseline(geometry.front,center,150.0))
    }
    @Test fun gridsKeepTheSameApparentWeightWhenDifferentAspectsFillTheHeight() {
        for((w,h) in listOf(4096 to 2048,2048 to 4096,4096 to 2304,1024 to 512)) {
            val width=SpaceCompassAngularGridAppearance.lineWidth(w,h)
            assertEquals(1000.0/4096,width*1000.0/h,1e-7)
            assertTrue(width<=2f*w/4096f)
        }
    }
    @Test fun aVeryNarrowFrameNeverGetsAThickerGridThanBefore() {
        assertEquals(2f*512/4096,SpaceCompassAngularGridAppearance.lineWidth(512,4096),0f)
    }
    @Test fun geographicMapParallelsStayAtTheirLatitudeAndSplitTheDateline() {
        val w=1440.0;val h=720.0
        for((latitude,_) in spaceCompassGlobeGeographicParallels) {
            val geometry=spaceCompassGlobeGuideSegments(latitude,SpaceCompassGlobePose(1.2,.7,3.0),true,w,h,100.0,200.0)
            assertTrue(geometry.back.isEmpty());assertTrue(geometry.front.isNotEmpty())
            val y=(90-latitude)/180*h
            geometry.front.forEach { segment ->
                assertEquals(y,segment.start.y,1e-7);assertEquals(y,segment.end.y,1e-7)
                assertTrue(abs(segment.end.x-segment.start.x)<=w/2)
            }
            assertEquals(w,geometry.front.sumOf { abs(it.end.x-it.start.x) },1e-6)
        }
    }
    @Test fun highZoomNamesReceiveOnlyClippedVisibleHemisphereFragments() {
        for(tilt in listOf(-1.2,-.4,0.0,.4,1.2)) for((latitude,_) in spaceCompassGlobeGeographicParallels) {
            val geometry=spaceCompassGlobeGuideSegments(latitude,SpaceCompassGlobePose(.25,tilt,3.0),false,360.0,640.0,800.0,320.0)
            (geometry.front+geometry.back).forEach { s ->
                for(p in listOf(s.start,s.end)) { assertTrue(p.x>=-1e-7&&p.x<=360+1e-7);assertTrue(p.y>=-1e-7&&p.y<=640+1e-7) }
            }
        }
        val front=spaceCompassGlobeGuideSegments(0.0,SpaceCompassGlobePose(),false,400.0,400.0,100.0,200.0)
        assertTrue(front.front.all { it.start.y==200.0&&it.end.y==200.0 })
        assertEquals(200.0,front.front.sumOf { hypot(it.end.x-it.start.x,it.end.y-it.start.y) },1e-6)
        assertTrue(front.back.isNotEmpty())
    }
}
