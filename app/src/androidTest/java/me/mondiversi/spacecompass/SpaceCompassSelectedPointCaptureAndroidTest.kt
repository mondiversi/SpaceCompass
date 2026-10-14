package me.mondiversi.spacecompass

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.RectF
import androidx.test.platform.app.InstrumentationRegistry
import java.io.File
import java.time.LocalDate
import java.time.ZoneId
import org.junit.Assert.*
import org.junit.Test

/** Synthetic photographs and local fixtures only; no camera, network or gallery access. */
class SpaceCompassSelectedPointCaptureAndroidTest {
    private val zone = ZoneId.of("Europe/Rome")
    private val date = LocalDate.parse("2026-10-12")
    private val time = date.atTime(12, 0).atZone(zone).toInstant().toEpochMilli()
    private val accent = Color.rgb(24, 169, 237)
    private fun fixture(azimuth: Double) : SpaceCompassPanoramaSnapshot {
        val point = SpaceCompassSunPathPoint(time - 3_600_000, SpaceCompassSunPosition(azimuth, 12.0))
        val path = SpaceCompassSunDailyPath(date, zone, listOf(point, point.copy(timeMs = time)),
            listOf(point), SpaceCompassCelestialBody.MOON)
        val state = SpaceCompassSunDailyPathUiState(); state.select(path, point)
        return SpaceCompassPanoramaSnapshot(time, 45.0, 9.0, 100.0, SpaceCompassSunSkyPhase.AFTERNOON, null,
            listOf(SpaceCompassPanoramaObject(path.body, "Moon", path)), SpaceCompassCelestialRemoteData(),
            "Space Compass · fixture", listOf("N", "E", "S", "W"), listOf("60°", "30°", "0°", "−30°", "−60°"),
            selectedPoints = state.selectedPoints.toList(), selectionTint = accent, zoneId = zone.id, activeBody = path.body)
    }
    private fun nearAccent(pixel: Int): Boolean = kotlin.math.abs(Color.red(pixel) - Color.red(accent)) < 35 &&
        kotlin.math.abs(Color.green(pixel) - Color.green(accent)) < 35 && kotlin.math.abs(Color.blue(pixel) - Color.blue(accent)) < 35
    private fun assertPin(bitmap: Bitmap, x: Double, y: Double, radius: Double) {
        assertTrue("The selected point keeps its blue core", (-1..1).any { dx ->
            (-1..1).any { dy -> nearAccent(bitmap.getPixel(x.toInt()+dx, y.toInt()+dy)) } })
        assertTrue("The selected point keeps its outer ring", (-2..2).any { dx ->
            (-2..2).any { dy -> nearAccent(bitmap.getPixel(x.toInt()+dx, (y-radius).toInt()+dy)) } })
    }

    @Test fun panoramaRetainsSelectedCoreAndRingInBothProfilesEvenWithLabelsHidden() {
        val captured = fixture(60.0)
        for (formatting in listOf(spaceCompassPanoramaInternationalFormatting,
            SpaceCompassPanoramaFormatting(locale=java.util.Locale.ITALIAN, numeric=SpaceCompassNumericFormat.SYSTEM,
                deviceLocale=java.util.Locale.ITALIAN))) for (labels in SpaceCompassPanoramaLabelMode.entries) {
            val snapshot = captured.copy(formatting = formatting, labelMode = labels)
            val bitmap = renderSpaceCompassPanorama(snapshot, 2048)
            try {
                val p = spaceCompassPanoramaPoint(captured.selectedPoints.single().point.position, 2048.0, 1024.0,
                    snapshot.center.azimuth)!!
                assertPin(bitmap, p.x, p.y + spaceCompassPanoramaSceneTop(prepareSpaceCompassCaptureCaption(snapshot.caption,2048),2048), 5.0)
            } finally { bitmap.recycle() }
        }
    }

    @Test fun selectedOnlyAddsThePinnedCaptionAndOmitsTheOtherAnnotations() {
        val captured = fixture(60.0)
        val all = renderSpaceCompassPanorama(captured.copy(labelMode = SpaceCompassPanoramaLabelMode.ALL), 2048)
        val selected = renderSpaceCompassPanorama(captured.copy(labelMode = SpaceCompassPanoramaLabelMode.SELECTED), 2048)
        val none = renderSpaceCompassPanorama(captured.copy(labelMode = SpaceCompassPanoramaLabelMode.NONE), 2048)
        val objects = renderSpaceCompassPanorama(captured.copy(labelMode = SpaceCompassPanoramaLabelMode.OBJECTS_SELECTED), 2048)
        try {
            assertFalse("The explicitly selected point still has its caption", selected.sameAs(none))
            assertFalse("Object names and other annotations are omitted", selected.sameAs(all))
            assertFalse("Object names are added to the selected captions", objects.sameAs(selected))
            assertTrue("The sole active object includes its unselected and current captions", objects.sameAs(all))
            val pin = captured.selectedPoints.single().point.position
            val point = spaceCompassPanoramaPoint(pin, 2048.0, 1024.0, captured.center.azimuth)!!
            for (bitmap in listOf(all, selected, none, objects)) {
                assertPin(bitmap, point.x, point.y + spaceCompassPanoramaSceneTop(prepareSpaceCompassCaptureCaption(captured.caption,2048),2048), 5.0)
            }
        } finally { all.recycle(); selected.recycle(); none.recycle(); objects.recycle() }
    }

    @Test fun activeObjectLabelsRemainVisibleEvenWithNoIndividuallySelectedPoints() {
        val captured = fixture(60.0).copy(selectedPoints = emptyList(),
            labelMode = SpaceCompassPanoramaLabelMode.OBJECTS_SELECTED)
        val active = renderSpaceCompassPanorama(captured, 2048)
        val inactive = renderSpaceCompassPanorama(captured.copy(activeBody = SpaceCompassCelestialBody.SUN), 2048)
        val none = renderSpaceCompassPanorama(captured.copy(labelMode = SpaceCompassPanoramaLabelMode.NONE), 2048)
        try {
            assertFalse("The active orbit has captions without pins", active.sameAs(none))
            assertTrue("A different checked target cannot leak captions into this orbit", inactive.sameAs(none))
        } finally { active.recycle(); inactive.recycle(); none.recycle() }
    }

    @Test fun photographReprojectsPinsIntoTheRealLensFieldAndDoesNotInventOffscreenPins() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val source = File(context.cacheDir, "selected-point-source-${System.nanoTime()}.jpg")
        val original = Bitmap.createBitmap(1024, 768, Bitmap.Config.ARGB_8888)
        Canvas(original).drawColor(Color.DKGRAY)
        try { source.outputStream().use { original.compress(Bitmap.CompressFormat.JPEG, 95, it) } }
        finally { original.recycle() }
        val orientation = SpaceCompassSunOrientation(SpaceCompassSunVector(1.0,0.0,0.0),
            SpaceCompassSunVector(0.0,0.0,1.0), SpaceCompassSunVector(0.0,1.0,0.0))
        val lens = SpaceCompassCameraLens(0.0,0.0,1024.0,768.0,768.0,768.0,512.0,384.0,0)
        val photo = SpaceCompassCameraPhotoSnapshot(source,lens,0,SpaceCompassCameraAttitude(0,orientation,true))
        try {
            val captured = fixture(10.0).copy(labelMode=SpaceCompassPanoramaLabelMode.NONE)
            val bitmap = renderSpaceCompassCameraPhoto(context,photo,captured)
            try {
                val geometry = spaceCompassCameraPhotoGeometry(lens,1024,768,0)
                val p = projectSpaceCompassSun(captured.selectedPoints.single().point.position,orientation,
                    1024.0,768.0,geometry.perspective)
                val caption = prepareSpaceCompassCameraCaptureCaption(captured.caption,1024,768)
                assertEquals(1024,bitmap.width)
                assertTrue("The entire 768 px lens frame fits below the metadata",bitmap.height > caption.height + 768)
                assertTrue(p.visible); assertPin(bitmap,p.x,p.y+caption.height,10.0*(768f/1080f))
            } finally { bitmap.recycle() }
            val outside = renderSpaceCompassCameraPhoto(context,photo,fixture(190.0).copy(labelMode=SpaceCompassPanoramaLabelMode.NONE))
            try {
                val pixels = IntArray(outside.width*outside.height)
                outside.getPixels(pixels,0,outside.width,0,0,outside.width,outside.height)
                assertEquals("No selected ring or core is drawn outside the photographed field",0,pixels.count(::nearAccent))
            } finally { outside.recycle() }
        } finally { source.delete() }
    }
}
