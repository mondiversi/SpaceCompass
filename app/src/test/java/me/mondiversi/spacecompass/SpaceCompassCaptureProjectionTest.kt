package me.mondiversi.spacecompass

import org.junit.Assert.*
import org.junit.Test
import kotlin.math.*

class SpaceCompassCaptureProjectionTest {
    private val pose=SpaceCompassSunOrientation(SpaceCompassSunVector(1.0,0.0,0.0),
        SpaceCompassSunVector(0.0,0.0,1.0),SpaceCompassSunVector(0.0,1.0,0.0))

    @Test fun reframingPreservesEveryVisibleRayWithAnOffCentreReticle() {
        val visible=SpaceCompassSunSceneFrame(20.0,40.0,400.0,600.0)
        val optical=SpaceCompassSunSceneFrame(20.0,-100.0,400.0,800.0)
        val lens=SpaceCompassPerspective(.7,.9)
        val frozen=spaceCompassSkyFraming(visible,optical,pose,lens)
        for(az in listOf(0.0,10.0,350.0)) for(el in listOf(-15.0,0.0,15.0)) {
            val p=SpaceCompassSunPosition(az,el)
            val a=projectSpaceCompassSun(p,pose,optical.width,optical.height,lens)
            val b=projectSpaceCompassSun(p,frozen.orientation,frozen.width,frozen.height,frozen.perspective)
            assertEquals(a.x+optical.left-visible.left,b.x,1e-8)
            assertEquals(a.y+optical.top-visible.top,b.y,1e-8)
            val ray=spaceCompassCameraPixelPosition(b.x,b.y,frozen.width,frozen.height,pose,frozen.perspective)
            assertEquals(p.elevationDegrees,ray.elevationDegrees,1e-8)
            assertEquals(0.0,wrapSpaceCompassSunDegrees(ray.azimuthDegrees-p.azimuthDegrees+180)-180,1e-8)
        }
    }
    @Test fun exportsKeepPortraitAndLandscapeAspectAndBoundBothDimensions() {
        assertEquals(1024 to 512,spaceCompassCaptureSize(800.0,400.0,1024))
        assertEquals(512 to 1024,spaceCompassCaptureSize(400.0,800.0,1024))
        assertEquals(4096 to 4096,spaceCompassCaptureSize(1.0,1.0,4096))
    }
    @Test fun flatMapUsesEarthFixedCoordinatesNotTheCurrentViewRotation() {
        assertEquals(SpaceCompassSunScenePoint(180.0,90.0),spaceCompassGlobeMapPoint(SpaceCompassGlobeVector(0.0,0.0,2.0),360.0,180.0))
        assertEquals(270.0,spaceCompassGlobeMapPoint(SpaceCompassGlobeVector(3.0,0.0,0.0),360.0,180.0).x,1e-8)
        assertEquals(0.0,spaceCompassGlobeMapPoint(SpaceCompassGlobeVector(0.0,5.0,0.0),360.0,180.0).y,1e-8)
    }
    @Test fun datelineCrossingsSplitAtOppositeEdgesInBothDirections() {
        for((a,b) in listOf(SpaceCompassSunScenePoint(359.0,30.0) to SpaceCompassSunScenePoint(1.0,50.0),
            SpaceCompassSunScenePoint(1.0,30.0) to SpaceCompassSunScenePoint(359.0,50.0))) {
            val segments=spaceCompassGlobeMapSegments(a,b,360.0)
            assertEquals(2,segments.size)
            assertEquals(a,segments.first().first);assertEquals(b,segments.last().second)
            assertEquals(40.0,segments.first().second.y,1e-8)
            assertEquals(360.0,abs(segments.first().second.x-segments.last().first.x),1e-8)
            assertTrue(segments.all { abs(it.first.x-it.second.x)<=1.0 })
        }
        val short=SpaceCompassSunScenePoint(90.0,20.0) to SpaceCompassSunScenePoint(110.0,30.0)
        assertEquals(listOf(short),spaceCompassGlobeMapSegments(short.first,short.second,360.0))
    }
    @Test fun photoHasOneImageWhileSkyAndGlobeCanExportBothRepresentations() {
        assertEquals(listOf(SpaceCompassCaptureExportType.IMAGE),spaceCompassCaptureImageTypes(true))
        assertEquals(listOf(SpaceCompassCaptureExportType.FRAMED_IMAGE,SpaceCompassCaptureExportType.PANORAMA_IMAGE),spaceCompassCaptureImageTypes(false))
        assertNotEquals(spaceCompassCaptureProjectionKey(true),spaceCompassCaptureProjectionKey(false))
        assertEquals(SpaceCompassCaptureProjection.PANORAMA,spaceCompassCaptureProjection(true))
        assertEquals(SpaceCompassCaptureProjection.FRAME,spaceCompassCaptureProjection(false))
    }
    @Test fun globeIsCentredInTheFullImageDespiteDifferentCaptionAndLegendHeights() {
        for((height,top,bottom) in listOf(Triple(4096,240,100),Triple(2048,60,90),Triple(720,0,0))) {
            val centre=top+spaceCompassGlobeCaptureCenterY(height,top,bottom)
            assertEquals((top+height+bottom)/2.0,centre,1e-9)
        }
    }
    @Test fun liveGlobeLabelsAlwaysIncludeElapsedOrRemainingTime() {
        val now=java.time.Instant.parse("2026-06-21T08:00:00Z").toEpochMilli()
        val zone=java.util.TimeZone.getTimeZone("UTC")
        val format=spaceCompassPanoramaInternationalFormatting
        assertEquals("09:00 · T−1h0m",formatSpaceCompassGlobeMainPointTime(now+3_600_000,now,zone,format))
        assertEquals("07:00 · T+1h0m",formatSpaceCompassGlobeMainPointTime(now-3_600_000,now,zone,format))
        assertEquals("08:00 · T+0h0m",formatSpaceCompassGlobeMainPointTime(now,now,zone,format))
    }

}
