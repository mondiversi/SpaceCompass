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
        SpaceCompassCelestialRemoteData(),"Space Compass · fixture",listOf("N","E","S","W"),listOf("60°","30°","0°","−30°","−60°"))

    @Test fun fullPanoramaContainsTheExactLiveSolarMarkerAndPreservesTheFrozenInput() {
        val input = snapshot()
        val before = input.objects.first().path!!.samples.toList()
        val bitmap = renderSpaceCompassPanorama(input,1024, InstrumentationRegistry.getInstrumentation().targetContext)
        try {
            assertEquals(1024,bitmap.width)
            assertTrue(bitmap.height < bitmap.width)
            val position = calculateSpaceCompassSunPosition(time,input.latitude,input.longitude,input.altitude)
            val angularPoint = spaceCompassPanoramaPoint(position,bitmap.width.toDouble(),(bitmap.width / 2).toDouble())!!
            val point = angularPoint.copy(y = angularPoint.y + SPACE_COMPASS_PANORAMA_HEADER_HEIGHT * .25)
            val marker = renderSpaceCompassPanoramaMarker(InstrumentationRegistry.getInstrumentation().targetContext,
                SpaceCompassCelestialBody.SUN, time)
            try {
                val reference = Bitmap.createBitmap(1024, bitmap.height, Bitmap.Config.ARGB_8888)
                try {
                    val x = point.x.toFloat(); val y = point.y.toFloat()
                    Canvas(reference).drawBitmap(marker, null, android.graphics.RectF(x - 9, y - 9, x + 9, y + 9),
                        android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG or android.graphics.Paint.FILTER_BITMAP_FLAG))
                    assertEquals(reference.getPixel(point.x.toInt(), point.y.toInt()),
                        bitmap.getPixel(point.x.toInt(), point.y.toInt()))
                } finally { reference.recycle() }
            } finally { marker.recycle() }
            assertEquals(before,input.objects.first().path!!.samples)
            assertNotEquals(bitmap.getPixel(400,bitmap.height / 8),bitmap.getPixel(400,bitmap.height * 7 / 8))
            assertNotEquals(bitmap.getPixel(400,100),bitmap.getPixel(400,bitmap.height - 12))
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
    @Test fun namesOnInsideTurnsStayCompleteAndTightBendsCannotStackTheirFinalLetters() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val bitmap = Bitmap.createBitmap(800, 500, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        try {
            canvas.drawColor(Color.rgb(16, 20, 24))
            for ((index, name) in listOf("Alpha Centauri", "Stephenson 2-18", "Rigel").withIndex()) {
                val radius = 160.0
                val cy = -20.0 + index * 130
                val points = (0..180).map {
                    val angle = Math.toRadians(135.0 - it / 2.0)
                    SpaceCompassSunScenePoint(400 + radius * kotlin.math.cos(angle), cy + radius * kotlin.math.sin(angle))
                }
                val segments = points.zipWithNext().map { (a, b) -> SpaceCompassSunPathSegment(a, b, false) }
                val center = SpaceCompassSunScenePoint(400.0, cy + radius)
                val paint = spaceCompassOrbitTextPaint(24f)
                val baseline = spaceCompassOrbitTextBaseline(segments, center, paint.measureText(name).toDouble(), 24.0, 16.0)!!
                assertEquals(paint.measureText(name).toDouble() + 19.2,
                    baseline.zipWithNext().sumOf { (a, b) -> kotlin.math.hypot(b.x - a.x, b.y - a.y) }, 1e-4)
                val orbit = android.graphics.Path().apply {
                    moveTo(points.first().x.toFloat(), points.first().y.toFloat())
                    points.drop(1).forEach { lineTo(it.x.toFloat(), it.y.toFloat()) }
                }
                canvas.drawPath(orbit, android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply {
                    color = Color.GRAY; style = android.graphics.Paint.Style.STROKE; strokeWidth = 2f
                })
                assertTrue(drawSpaceCompassOrbitName(canvas, name, segments,
                    SpaceCompassSunPathArrow(center, center, center, center, false), paint, Color.YELLOW, 16f))
                val left = baseline.minOf { it.x }.toInt() - 5
                val right = baseline.maxOf { it.x }.toInt() + 5
                val inkX = (left..right).filter { x ->
                    ((cy + 70).toInt()..(cy + 170).toInt()).any { y ->
                        val pixel = bitmap.getPixel(x, y)
                        Color.red(pixel) > 200 && Color.green(pixel) > 200 && Color.blue(pixel) < 100
                    }
                }
                assertTrue("The end of $name is rendered without being clipped or stacked at the start",
                    inkX.last() - inkX.first() > paint.measureText(name) * .75)
            }
            val tight = (0..180).map {
                val angle = Math.toRadians(135.0 - it / 2.0)
                SpaceCompassSunScenePoint(650 + 40 * kotlin.math.cos(angle), 150 + 40 * kotlin.math.sin(angle))
            }.zipWithNext().map { (a, b) -> SpaceCompassSunPathSegment(a, b, false) }
            val center = SpaceCompassSunScenePoint(650.0, 190.0)
            assertFalse(drawSpaceCompassOrbitName(canvas, "II", tight,
                SpaceCompassSunPathArrow(center, center, center, center, false), spaceCompassOrbitTextPaint(24f), Color.YELLOW, 14f))
            val file = java.io.File(context.getExternalFilesDir(null), "orbit-text-inside-turns.png")
            file.outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
        } finally { bitmap.recycle() }
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
                try { assertEquals(1024,decoded.width); assertTrue(decoded.height < decoded.width) }
                finally { decoded.recycle() }
            }
        } finally { assertEquals(1,resolver.delete(uri,null,null)) }
    }

    @Test fun lunarMiniaturePreservesItsTerminatorAndIssKeepsTheSharedSolarPanelModel() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val phase = calculateSpaceCompassMoonPhase(time)
        assertTrue(phase.illuminatedFraction in .05.. .95)
        val moon = renderSpaceCompassPanoramaMarker(context, SpaceCompassCelestialBody.MOON, time)
        val iss = renderSpaceCompassPanoramaMarker(context, SpaceCompassCelestialBody.ISS, time)
        try {
            val radius = moon.width * 28.0 / 64
            val center = moon.width / 2.0
            val lit = mutableListOf<Int>(); val dark = mutableListOf<Int>()
            for (y in 0 until moon.height) for (x in 0 until moon.width) {
                val nx = (x + .5 - center) / radius; val ny = (center - y - .5) / radius
                if (nx * nx + ny * ny >= .8) continue
                val bounds = phase.litHorizontalBounds(ny)
                val color = moon.getPixel(x, y)
                val light = Color.red(color) + Color.green(color) + Color.blue(color)
                if (nx in bounds.first..bounds.second) lit += light else dark += light
            }
            assertTrue(lit.average() > dark.average() * 2)
            val pixels = IntArray(iss.width * iss.height)
            iss.getPixels(pixels, 0, iss.width, 0, 0, iss.width, iss.height)
            assertTrue(pixels.distinct().size > 20)
            assertTrue(pixels.count { Color.alpha(it) > 0 && Color.blue(it) > Color.red(it) * 1.4 && Color.blue(it) > 40 } > 20)
        } finally { moon.recycle(); iss.recycle() }
    }

    @Test @SdkSuppress(minSdkVersion=29)
    fun savingThePreviewFilePreservesItsPixelsAndFrozenTime() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val file = java.io.File(context.cacheDir, "preview-roundtrip-${System.nanoTime()}.jpg")
        val bitmap = renderSpaceCompassPanorama(snapshot(), 1024, context)
        try { file.outputStream().use { bitmap.compress(Bitmap.CompressFormat.JPEG, 95, it) } }
        finally { bitmap.recycle() }
        stampSpaceCompassPanoramaFile(file, time)
        val uri = saveSpaceCompassPanoramaFile(context, file, time)
        try {
            val source = BitmapFactory.decodeFile(file.absolutePath)!!
            val saved = context.contentResolver.openInputStream(uri)!!.use { BitmapFactory.decodeStream(it)!! }
            try {
                assertEquals(source.width, saved.width); assertEquals(source.height, saved.height)
                for (y in 0 until source.height step 47) for (x in 0 until source.width step 53)
                    assertEquals(source.getPixel(x, y), saved.getPixel(x, y))
            } finally { source.recycle(); saved.recycle() }
            context.contentResolver.query(uri, arrayOf(MediaStore.Images.Media.DATE_TAKEN), null, null, null)!!.use {
                assertTrue(it.moveToFirst()); assertEquals(time, it.getLong(0))
            }
        } finally { context.contentResolver.delete(uri, null, null); file.delete() }
    }

    @Test @SdkSuppress(minSdkVersion=29)
    fun explicitGallerySaveCanBeRepeatedWithoutDuplicatingAndRestoredAfterDeletion() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val captured = System.currentTimeMillis()
        val file = java.io.File(context.cacheDir, "gallery-idempotence-${System.nanoTime()}.jpg")
        val bitmap = renderSpaceCompassPanorama(snapshot(), 1024, context)
        try { file.outputStream().use { bitmap.compress(Bitmap.CompressFormat.JPEG, 95, it) } }
        finally { bitmap.recycle() }
        var uri: android.net.Uri? = null
        try {
            val initial = ensureSpaceCompassPanoramaInGallery(context, file, captured, null)
            uri = initial
            assertEquals(initial, ensureSpaceCompassPanoramaInGallery(context, file, captured, initial))
            assertEquals(1, context.contentResolver.delete(initial, null, null))
            uri = null
            val restored = ensureSpaceCompassPanoramaInGallery(context, file, captured, initial)
            uri = restored
            context.contentResolver.query(restored, arrayOf(MediaStore.Images.Media.IS_PENDING), null, null, null)!!.use {
                assertTrue(it.moveToFirst()); assertEquals(0, it.getInt(0))
            }
        } finally { uri?.let { context.contentResolver.delete(it, null, null) }; file.delete() }
    }

    @Test fun compactCaptionKeepsOneLineAndTheAngularSceneHasNoFooter() {
        val caption = "Space Compass · 05/10/26 · 20:32 CEST · Zinasco, Lombardia, Italia · " +
            "45.106887° N · 8.999557° E (±20 m) · GPS altitude 118 m · Partly cloudy (53%)"
        for (value in listOf("Space Compass", caption, caption.repeat(2), "Space Compass · المدينة، البلد · غائم جزئياً (53%)")) {
            val bitmap = renderSpaceCompassPanorama(snapshot().copy(caption = value), 1024)
            try {
                val headerHeight = SPACE_COMPASS_PANORAMA_HEADER_HEIGHT * .25f
                assertEquals(512 + headerHeight.toInt(), bitmap.height)
                val bounds = drawSpaceCompassPanoramaCaption(Canvas(bitmap), value, 1024, .25f)
                assertTrue(bounds.left >= 0 && bounds.right <= 1024)
                assertTrue(bounds.top > 0)
                assertEquals("The fitted line stays vertically centered above the grid",
                    headerHeight / 2, bounds.centerY(), .001f)
                // Reserve the unchanged 44 px marker extent, scaled with the angular scene.
                assertTrue("Even the +90° marker must stay below the caption",
                    bounds.bottom + 2 * .25f <= headerHeight - 44 * .25f)
            } finally { bitmap.recycle() }
        }
    }


}
