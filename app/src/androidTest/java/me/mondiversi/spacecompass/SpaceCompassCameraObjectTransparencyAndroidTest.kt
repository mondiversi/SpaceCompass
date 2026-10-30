package me.mondiversi.spacecompass

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import androidx.test.platform.app.InstrumentationRegistry
import java.io.File
import java.time.Instant
import org.junit.Assert.*
import org.junit.Test
import kotlin.math.*

/** A synthetic private-cache photograph only; no camera, GPS, downloads, preferences or gallery writes. */
class SpaceCompassCameraObjectTransparencyAndroidTest {
    private fun cross(a: SpaceCompassSunVector,b: SpaceCompassSunVector)=SpaceCompassSunVector(
        a.north*b.up-a.up*b.north,a.up*b.east-a.east*b.up,a.east*b.north-a.north*b.east)

    @Test fun theWholeObjectPlateCompositesOverThePhotoAndBothExportProfilesRetainIt() {
        val context=InstrumentationRegistry.getInstrumentation().targetContext
        val source=File(context.cacheDir,"translucent-photo-${System.nanoTime()}.jpg")
        val raw=Bitmap.createBitmap(1024,768,Bitmap.Config.ARGB_8888)
        Canvas(raw).drawColor(Color.rgb(24,72,120))
        try { source.outputStream().use { assertTrue(raw.compress(Bitmap.CompressFormat.JPEG,95,it)) } }
        finally { raw.recycle() }
        val time=Instant.parse("2026-10-28T12:00:00Z").toEpochMilli()
        val remote=SpaceCompassCelestialRemoteData()
        val position=calculateSpaceCompassCelestialObservation(SpaceCompassCelestialBody.SUN,time,45.0,9.0,0.0,remote)!!.position
        val az=Math.toRadians(position.azimuthDegrees);val el=Math.toRadians(position.elevationDegrees)
        val forward=SpaceCompassSunVector(cos(el)*sin(az),cos(el)*cos(az),sin(el))
        val right=cross(forward,SpaceCompassSunVector(0.0,0.0,1.0)).normalized()
        val orientation=SpaceCompassSunOrientation(right,cross(right,forward).normalized(),forward)
        val lens=SpaceCompassCameraLens(0.0,0.0,1024.0,768.0,768.0,768.0,512.0,384.0,0)
        val photo=SpaceCompassCameraPhotoSnapshot(source,lens,0,SpaceCompassCameraAttitude(0,orientation,true))
        val snapshot=SpaceCompassPanoramaSnapshot(time,45.0,9.0,0.0,SpaceCompassSunSkyPhase.NIGHT,null,
            listOf(SpaceCompassPanoramaObject(SpaceCompassCelestialBody.SUN,"Sun",null)),remote,"Capture fixture",
            listOf("N","NE","E","SE","S","SW","W","NW"),listOf("90°","60°","30°","0°","−30°","−60°","−90°"),
            labelMode=SpaceCompassPanoramaLabelMode.NONE,showSkyReferences=false)
        val outputs=mutableListOf<Bitmap>()
        try {
            val baseline=renderSpaceCompassCameraPhoto(context,photo,snapshot.copy(objects=emptyList())).also(outputs::add)
            val opaque=renderSpaceCompassCameraPhoto(context,photo,snapshot).also(outputs::add)
            val frozen=photo.copy(objectOpacity=.5f)
            val translucent=renderSpaceCompassCameraPhoto(context,frozen,snapshot).also(outputs::add)
            val caption=prepareSpaceCompassCameraCaptureCaption(snapshot.caption,1024,768,context)
            val x=512;val y=caption.height+384
            val base=baseline.getPixel(x,y);val solid=opaque.getPixel(x,y);val half=translucent.getPixel(x,y)
            val channels=listOf<(Int)->Int>(Color::red,Color::green,Color::blue)
            assertTrue(channels.sumOf { abs(it(solid)-it(base)) }>40)
            for(channel in channels) assertEquals((channel(solid)+channel(base))/2.0,channel(half).toDouble(),4.0)
            val selected=snapshot.copy(formatting=SpaceCompassPanoramaFormatting(locale=java.util.Locale.ITALIAN),
                objects=listOf(SpaceCompassPanoramaObject(SpaceCompassCelestialBody.SUN,"Sole",null)))
            val other=renderSpaceCompassCameraPhoto(context,frozen.copy(warning="Translated warning"),selected).also(outputs::add)
            assertEquals(half,other.getPixel(x,y))
        } finally { outputs.forEach(Bitmap::recycle);source.delete() }
    }
}
