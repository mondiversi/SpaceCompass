package me.mondiversi.spacecompass

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import androidx.core.graphics.createBitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** An archived telescope frame is kept flat, never wrapped onto an invented planetary sphere. */
@Composable
internal fun SpaceCompassCatalogPhotograph(name: String, modifier: Modifier) {
    val context = LocalContext.current.applicationContext
    val ready = rememberSpaceCompassCelestialImage(name)
    val image by produceState<androidx.compose.ui.graphics.ImageBitmap?>(null, name, ready) {
        value = if (!ready) null else withContext(Dispatchers.IO) {
            runCatching { SpaceCompassCelestialTextures.openImage(context, name)?.use {
                BitmapFactory.decodeStream(it)?.asImageBitmap()
            } }.onFailure { SpaceCompassErrorLog.record(context, "celestial:photograph:$name", it) }.getOrNull()
        }
    }
    Box(modifier, contentAlignment = Alignment.Center) {
        val current = image
        if (current == null) Text(stringResource(R.string.celestial_image_loading), color = Color.LightGray)
        else Image(current, contentDescription = null, contentScale = ContentScale.Fit)
    }
}

/** A circular, centre-cropped miniature preserves image geometry rather than sampling longitude. */
internal fun createSpaceCompassPhotographicThumbnail(map: Bitmap, extent: Int): Bitmap {
    val bitmap = createBitmap(extent, extent, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)
    val radius = extent * 28f / 64
    canvas.clipPath(Path().apply { addCircle(extent/2f, extent/2f, radius, Path.Direction.CW) })
    val side = minOf(map.width, map.height)
    val left = (map.width - side)/2; val top = (map.height - side)/2
    canvas.drawBitmap(map, android.graphics.Rect(left, top, left+side, top+side),
        RectF(extent/2f-radius, extent/2f-radius, extent/2f+radius, extent/2f+radius),
        Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG))
    return bitmap
}

/** Author names are retained exactly; full sources and licences accompany the external pack. */
internal fun spaceCompassCatalogImageCredit(name: String?): String? = when (name) {
    "titan.webp" -> "NASA/JPL-Caltech/Space Science Institute"
    "sirius.webp" -> "NASA, ESA, H. Bond (STScI), and M. Barstow (University of Leicester)"
    "betelgeuse.webp" -> "ALMA (ESO/NAOJ/NRAO)/E. O’Gorman/P. Kervella"
    "orion_nebula.webp" -> "NASA, ESA, M. Robberto (STScI/ESA), and the Hubble Space Telescope Orion Treasury Project Team"
    "pleiades.webp" -> "NASA/JPL-Caltech/UCLA"
    "andromeda_galaxy.webp" -> "NASA, ESA, Benjamin F. Williams (UWashington), Zhuo Chen (UWashington), L. Clifton Johnson (Northwestern); Image Processing: Joseph DePasquale (STScI)"
    else -> null
}
