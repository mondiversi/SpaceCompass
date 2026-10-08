package me.mondiversi.spacecompass

import android.app.ActivityManager
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import kotlin.math.min

/** A power-of-two decode keeps both image axes within the GPU and memory limits. */
internal fun spaceCompassStarAtlasSampleSize(width: Int, height: Int, maximumDimension: Int,
    highDetail: Boolean): Int {
    require(width > 0 && height > 0 && maximumDimension > 0)
    val limit = min(maximumDimension, if (highDetail) 4096 else 2048)
    var sample = 1L
    while ((width.toLong() + sample - 1) / sample > limit ||
        (height.toLong() + sample - 1) / sample > limit) sample *= 2
    require(sample <= Int.MAX_VALUE)
    return sample.toInt()
}

/** The same lossless atlas serves the live sky and exports, with a bounded decode. */
internal fun loadSpaceCompassStarAtlas(context: Context, maximumDimension: Int = 4096): Bitmap {
    val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    context.assets.open(SPACE_COMPASS_STAR_MAP_ASSET).use { BitmapFactory.decodeStream(it, null, bounds) }
    val memory = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
    val options = BitmapFactory.Options().apply {
        inScaled = false
        inSampleSize = spaceCompassStarAtlasSampleSize(bounds.outWidth, bounds.outHeight, maximumDimension,
            highDetail = memory.memoryClass >= 192 && !memory.isLowRamDevice)
    }
    return context.assets.open(SPACE_COMPASS_STAR_MAP_ASSET).use {
        requireNotNull(BitmapFactory.decodeStream(it, null, options))
    }
}
