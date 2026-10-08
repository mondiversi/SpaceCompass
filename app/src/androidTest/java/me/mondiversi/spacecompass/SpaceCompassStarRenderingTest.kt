package me.mondiversi.spacecompass

import android.graphics.Bitmap
import android.graphics.Canvas
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.foundation.layout.size
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.platform.app.InstrumentationRegistry
import java.io.File
import java.time.Instant
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import kotlin.math.*

/** Disposable emulator only. Synthetic scenes: no sensors, GPS, network or preference writes. */
class SpaceCompassStarRenderingTest {
    @get:Rule val compose = createComposeRule()
    private val context = InstrumentationRegistry.getInstrumentation().targetContext
    private val time = Instant.parse("2026-10-07T22:00:00Z").toEpochMilli()
    private fun facing(azimuth: Double, elevation: Double): SpaceCompassSunOrientation {
        val a = Math.toRadians(azimuth); val e = Math.toRadians(elevation)
        return SpaceCompassSunOrientation(SpaceCompassSunVector(cos(a), -sin(a), 0.0),
            SpaceCompassSunVector(-sin(e) * sin(a), -sin(e) * cos(a), cos(e)),
            SpaceCompassSunVector(cos(e) * sin(a), cos(e) * cos(a), sin(e)))
    }
    private fun pixels(bitmap: Bitmap): IntArray = IntArray(bitmap.width * bitmap.height).also {
        bitmap.getPixels(it, 0, bitmap.width, 0, 0, bitmap.width, bitmap.height)
    }
    private fun save(bitmap: Bitmap, name: String) = File(context.externalCacheDir, name).outputStream().use {
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, it)
    }

    @Test fun textureViewCompilesTheRealShaderTracksDirectionAndRecreatesItsSurface() {
        val orientation = mutableStateOf(facing(0.0, 25.0))
        val visible = mutableStateOf(true)
        val extent = mutableStateOf(IntSize.Zero)
        var view: SpaceCompassStarTextureView? = null
        val basis = spaceCompassStarBasis(time, 45.0, 9.0, 100.0)
        compose.setContent {
            if (visible.value) AndroidView(factory = { SpaceCompassStarTextureView(it).also { next -> view = next } },
                modifier = Modifier.size(320.dp, 240.dp).onSizeChanged { extent.value = it },
                onRelease = { it.release() }, update = { next ->
                    val size = extent.value
                    if (size.width > 0 && size.height > 0) next.update(spaceCompassStarDrawing(basis, orientation.value,
                        SpaceCompassSunSceneFrame(0.0, 0.0, size.width.toDouble(), size.height.toDouble()), 1f))
                })
        }
        fun readUntil(predicate: (IntArray) -> Boolean): Bitmap {
            var result: Bitmap? = null
            compose.waitUntil(20_000) {
                var next: Bitmap? = null
                compose.runOnIdle { next = view?.takeIf { it.isAvailable }?.bitmap }
                next?.let {
                    if (predicate(pixels(it))) { result = it; true }
                    else { it.recycle(); false }
                } ?: false
            }
            return requireNotNull(result)
        }
        val north = readUntil { p -> p.count { it ushr 24 > 8 } > 100 }
        val northPixels = pixels(north)
        assertTrue("Ground rays must stay transparent", northPixels.takeLast(north.width * 8).all { it ushr 24 == 0 })
        save(north, "stars-gpu-north.png"); north.recycle()
        compose.runOnIdle { orientation.value = facing(180.0, 25.0) }
        val south = readUntil { p -> !p.contentEquals(northPixels) && p.count { it ushr 24 > 8 } > 100 }
        save(south, "stars-gpu-south.png"); south.recycle()
        compose.runOnIdle { orientation.value = facing(0.0, -90.0) }
        readUntil { p -> p.all { it ushr 24 == 0 } }.recycle()
        compose.runOnIdle { visible.value = false }
        compose.runOnIdle { view = null; orientation.value = facing(90.0, 35.0); visible.value = true }
        val restored = readUntil { p -> p.count { it ushr 24 > 8 } > 100 }
        save(restored, "stars-gpu-restored.png"); restored.recycle()
    }

    @Test fun panoramicPixelsIncludeStarsOnlyInNightSkyAndFollowTheObserver() {
        val snapshot = SpaceCompassPanoramaSnapshot(time, 45.0, 9.0, 100.0,
            SpaceCompassSunSkyPhase.NIGHT, null, emptyList(), SpaceCompassCelestialRemoteData(), "fixture", emptyList(), emptyList())
        fun render(item: SpaceCompassPanoramaSnapshot): Bitmap = Bitmap.createBitmap(1024, 512, Bitmap.Config.ARGB_8888).also {
            drawSpaceCompassPanoramaStars(Canvas(it), item, it.width, it.height, context)
        }
        val night = render(snapshot)
        val nightPixels = pixels(night)
        assertTrue(nightPixels.take(night.width * night.height / 2).count { it ushr 24 > 8 } > 100)
        assertTrue(nightPixels.drop(night.width * night.height / 2).all { it ushr 24 == 0 })
        save(night, "stars-panorama-clear.png"); night.recycle()
        val otherPlace = render(snapshot.copy(longitude = 90.0))
        assertFalse(nightPixels.contentEquals(pixels(otherPlace))); otherPlace.recycle()
        val midday = render(snapshot.copy(timeMs = Instant.parse("2026-10-07T11:00:00Z").toEpochMilli()))
        assertTrue(pixels(midday).all { it ushr 24 == 0 }); midday.recycle()
        val cloudy = render(snapshot.copy(weather = SpaceCompassSunWeatherSnapshot(SpaceCompassSunWeatherKind.CLOUDY, 1f, time)))
        assertTrue(pixels(cloudy).all { it ushr 24 == 0 }); cloudy.recycle()
    }
}
