package me.mondiversi.spacecompass

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Matrix
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.IntSize
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.concurrent.ConcurrentHashMap
import kotlin.math.*

private val celestialThumbnailDispatcher = Dispatchers.Default.limitedParallelism(2)
private val celestialThumbnailCache = ConcurrentHashMap<SpaceCompassCelestialBody, ImageBitmap>()

/** Same maps/structural models as the viewer; cached reference miniatures, not live photographs. */
@Composable
internal fun SpaceCompassCelestialThumbnail(body: SpaceCompassCelestialBody, modifier: Modifier, muted: Boolean = false,
    moonPhase: SpaceCompassMoonPhase? = null) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val colorFilter = remember(muted) {
        if (muted) ColorFilter.colorMatrix(ColorMatrix().apply { setToSaturation(0f) }) else null
    }
    val opacity = if (muted) 0.70f else 1f
    val phase = moonPhase.takeIf { body == SpaceCompassCelestialBody.MOON }
    val phaseDescription = phase?.let {
        stringResource(R.string.moon_phase_description, stringResource(it.kind.nameResource),
            formatSpaceCompassNumber(it.illuminatedFraction * 100, 1, LocalSpaceCompassNumericFormat.current))
    }
    if (body.isComet) {
        SpaceCompassCometSymbol(body, modifier, opacity)
    } else if (body == SpaceCompassCelestialBody.EARTH_CENTER) {
        SpaceCompassEarthCenterSymbol(modifier, opacity)
    } else if (body.usesDeepSkySymbol) {
        SpaceCompassDeepSkySymbol(body, modifier, opacity)
    } else if (body.isSpacecraft) {
        val geometry = remember { SpaceCompassCelestialRotation(yaw = 25.0, pitch = 50.0).geometry() }
        SpaceCompassCelestialCraftCanvas(body, geometry, SpaceCompassCelestialViewportState(), modifier.clip(CircleShape), colorFilter, opacity)
    } else {
        val textureReady = rememberSpaceCompassCelestialTexture(body)
        val thumbnail by produceState(celestialThumbnailCache[body].takeIf { textureReady }, body, textureReady) {
            value = withContext(celestialThumbnailDispatcher) {
                celestialThumbnailCache[body].takeIf { textureReady } ?: runCatching { createSpaceCompassCelestialThumbnail(context.applicationContext, body).asImageBitmap() }
                    .onFailure { SpaceCompassErrorLog.record(context, "celestial:thumbnail", it) }.getOrNull()
                    ?.also { if (textureReady) celestialThumbnailCache[body] = it }
            }
        }
        Canvas(modifier.then(if (phaseDescription == null) Modifier else Modifier.semantics {
            contentDescription = phaseDescription
        })) { drawSpaceCompassCelestialThumbnail(thumbnail, phase, colorFilter, opacity) }
    }
}

internal fun androidx.compose.ui.graphics.drawscope.DrawScope.drawSpaceCompassCelestialThumbnail(
    image: ImageBitmap?, phase: SpaceCompassMoonPhase?, colorFilter: ColorFilter? = null, opacity: Float = 1f
) {
    val silhouette = phase?.let(::spaceCompassMoonPhaseSilhouette)
    val targetSize = IntSize(size.width.roundToInt(), size.height.roundToInt())
    if (silhouette == null) {
        if (image != null) drawImage(image, dstSize = targetSize, colorFilter = colorFilter, alpha = opacity)
        else drawCircle(Color(0xffb9bcc4), size.minDimension * .40f, alpha = opacity)
    } else {
        val radius = size.minDimension * (28f / 64f)
        val matrix = Matrix().apply { translate(center.x, center.y); scale(radius, radius) }
        val litArea = Path().apply { addPath(silhouette); transform(matrix) }
        drawCircle(Color(0xff11141a), radius, alpha = opacity)
        if (image != null) drawImage(image, dstSize = targetSize, colorFilter = colorFilter, alpha = opacity * 0.10f)
        clipPath(litArea) {
            if (image != null) drawImage(image, dstSize = targetSize, colorFilter = colorFilter, alpha = opacity)
            else drawCircle(Color(0xffb9bcc4), radius, alpha = opacity)
        }
    }
}

internal fun createSpaceCompassCelestialThumbnail(context: Context?, body: SpaceCompassCelestialBody, extent: Int = 64): Bitmap {
    val asset = body.viewerTexture
    val map = if (asset == null || context == null) null else {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        SpaceCompassCelestialTextures.open(context, body)?.use { BitmapFactory.decodeStream(it, null, bounds) }
        val options = BitmapFactory.Options().apply { inSampleSize = spaceCompassCelestialTextureSampleSize(bounds.outWidth, 512) }
        SpaceCompassCelestialTextures.open(context, body)?.use { requireNotNull(BitmapFactory.decodeStream(it, null, options)) }
    }
    try {
        if (body.hasCatalogPhotograph && map != null) return createSpaceCompassPhotographicThumbnail(map, extent)
        val pixels = IntArray(extent*extent)
        val radius = if (body == SpaceCompassCelestialBody.SATURN) extent * 17.0 / 64 else extent * 28.0 / 64
        for (y in 0 until extent) for (x in 0 until extent) {
            val nx = (x+0.5-extent/2)/radius; val ny = (extent/2-y-0.5)/radius
            val squared = nx*nx+ny*ny
            if (squared > 1) {
                val ring = nx*nx+(ny/0.28).pow(2)
                if (body == SpaceCompassCelestialBody.SATURN && ring in 1.20..3.10) pixels[y*extent+x] = 0xffb8a78b.toInt()
                continue
            }
            val nz = sqrt(1-squared)
            val u = ((atan2(nz, nx)/(2*PI) + body.textureLongitudeOffset) % 1+1)%1
            val v = acos(ny.coerceIn(-1.0,1.0))/PI
            val sampled = map?.getPixel((u*map.width).toInt().coerceIn(0,map.width-1),
                (v*map.height).toInt().coerceIn(0,map.height-1))
                ?: spaceCompassCelestialPlaceholderColor(body)
            val rgb = if (body.textureHasUnmappedAreas && sampled and 0x00ffffff == 0) 0xff38332e.toInt() else sampled
            val brightness = if (body == SpaceCompassCelestialBody.SUN || body == SpaceCompassCelestialBody.POLARIS) 0.7+0.3*nz
                else 0.25+0.75*((-0.4*nx+0.3*ny+nz)/sqrt(1.25)).coerceAtLeast(0.0)
            pixels[y*extent+x] = (0xff shl 24) or (((rgb shr 16 and 255)*brightness).toInt() shl 16) or
                (((rgb shr 8 and 255)*brightness).toInt() shl 8) or ((rgb and 255)*brightness).toInt()
        }
        return Bitmap.createBitmap(pixels, extent, extent, Bitmap.Config.ARGB_8888)
    } finally { map?.recycle() }
}
