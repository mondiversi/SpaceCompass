package me.mondiversi.spacecompass

import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Bitmap
import android.provider.MediaStore
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.filters.SdkSuppress
import org.junit.Assert.*
import org.junit.Test
import java.time.Instant
import java.time.ZoneId

/** Synthetic fixtures only. The gallery round trip removes only the image created by this test. */
class SpaceCompassPanoramaAndroidTest {
    private val time = Instant.parse("2026-10-05T12:00:00Z").toEpochMilli()
    private val zone = ZoneId.of("Europe/Rome")
    private fun snapshot() = SpaceCompassPanoramaSnapshot(time,41.9028,12.4964,50.0,
        SpaceCompassSunSkyPhase.AFTERNOON,null,listOf(
            SpaceCompassPanoramaObject(SpaceCompassCelestialBody.SUN,"Sun",calculateSpaceCompassSunDailyPath(Instant.ofEpochMilli(time).atZone(zone).toLocalDate(),zone,41.9028,12.4964,50.0)),
            SpaceCompassPanoramaObject(SpaceCompassCelestialBody.EARTH_CENTER,"Earth centre",null),
            SpaceCompassPanoramaObject(SpaceCompassCelestialBody.STARLINK_V3,"Starlink","".let { null })),
        SpaceCompassCelestialRemoteData(),"Space Compass · fixture",listOf("N","E","S","W"),listOf("60°","30°","0°","−30°","−60°"),"Unavailable","Orbit unavailable")

    @Test fun fullPanoramaContainsTheExactLiveSolarMarkerAndPreservesTheFrozenInput() {
        val input = snapshot()
        val before = input.objects.first().path!!.samples.toList()
        val bitmap = renderSpaceCompassPanorama(input,1024)
        try {
            assertEquals(1024,bitmap.width)
            assertTrue(bitmap.height > 512)
            val position = calculateSpaceCompassSunPosition(time,input.latitude,input.longitude,input.altitude)
            val point = spaceCompassPanoramaPoint(position,1024.0,512.0)!!
            assertEquals(Color.WHITE,bitmap.getPixel(point.x.toInt(),point.y.toInt()))
            assertEquals(before,input.objects.first().path!!.samples)
            assertNotEquals(bitmap.getPixel(400,100),bitmap.getPixel(400,450))
        } finally { bitmap.recycle() }
    }
    @Test fun complexScriptNamesAreRenderedAsWholeStringsOnCurvedBaselines() {
        for (name in listOf("الشمس","خورشید","שמש","Alpha Centauri")) {
            val bitmap = Bitmap.createBitmap(600,300,Bitmap.Config.ARGB_8888)
            try {
                val points = (0..60).map { SpaceCompassSunScenePoint(it*10.0,180 + (it-30)*(it-30)*.03) }
                val segments = points.zipWithNext().map { (a,b) -> SpaceCompassSunPathSegment(a,b,false) }
                val p = points[30]
                val arrow = SpaceCompassSunPathArrow(p,p,p,p,false)
                assertTrue(drawSpaceCompassOrbitName(Canvas(bitmap),name,segments,arrow,spaceCompassOrbitTextPaint(24f),Color.YELLOW,14f))
                assertTrue((120 until 220).sumOf { y -> (0 until 600).count { x -> Color.alpha(bitmap.getPixel(x,y)) > 0 } } > 100)
                val inkX = (0 until 600).filter { x -> (120 until 220).any { y -> Color.alpha(bitmap.getPixel(x,y)) > 0 } }
                assertTrue("The complete shaped name must fit, rather than only its first letters",
                    inkX.last() - inkX.first() >= spaceCompassOrbitTextPaint(24f).measureText(name) * .7f)
            } finally { bitmap.recycle() }
        }
    }
    @Test @SdkSuppress(minSdkVersion=29)
    fun galleryRoundTripPublishesACompleteJpegWithCaptureTimeAndAlbum() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val bitmap = renderSpaceCompassPanorama(snapshot(),1024)
        val captured = System.currentTimeMillis()
        val uri = try { saveSpaceCompassPanorama(context,bitmap,captured) } finally { bitmap.recycle() }
        val resolver = context.contentResolver
        try {
            resolver.query(uri,arrayOf(MediaStore.Images.Media.IS_PENDING,MediaStore.Images.Media.MIME_TYPE,
                MediaStore.Images.Media.DATE_TAKEN,MediaStore.Images.Media.RELATIVE_PATH),null,null,null)!!.use {
                assertTrue(it.moveToFirst()); assertEquals(0,it.getInt(0)); assertEquals("image/jpeg",it.getString(1))
                assertEquals(captured,it.getLong(2)); assertEquals("DCIM/SpaceCompass/",it.getString(3))
            }
            resolver.openInputStream(uri)!!.use {
                val decoded = BitmapFactory.decodeStream(it)!!
                try { assertEquals(1024,decoded.width); assertTrue(decoded.height > 512) }
                finally { decoded.recycle() }
            }
        } finally { assertEquals(1,resolver.delete(uri,null,null)) }
    }
}
