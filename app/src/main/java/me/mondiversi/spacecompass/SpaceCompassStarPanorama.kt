package me.mondiversi.spacecompass

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import kotlin.math.*

/** Same celestial atlas as the live GPU view, sampled into the frozen 360-degree sky. */
internal fun drawSpaceCompassPanoramaStars(canvas: Canvas, snapshot: SpaceCompassPanoramaSnapshot,
    width: Int, sceneHeight: Int, context: Context?) {
    if (context == null || !snapshot.observerPositionKnown) return
    val sun = calculateSpaceCompassSunPosition(snapshot.timeMs, snapshot.latitude, snapshot.longitude, snapshot.altitude)
    val opacity = spaceCompassStarVisibility(sun.elevationDegrees, snapshot.weather)
    if (opacity <= .001f) return
    val basis = spaceCompassStarBasis(snapshot.timeMs, snapshot.latitude, snapshot.longitude, snapshot.altitude)
    val decoded = loadSpaceCompassStarAtlas(context)
    val mapWidth = decoded.width; val mapHeight = decoded.height
    val pixels = IntArray(mapWidth * mapHeight)
    try { decoded.getPixels(pixels, 0, mapWidth, 0, 0, mapWidth, mapHeight) } finally { decoded.recycle() }
    val sine = DoubleArray(width)
    val cosine = DoubleArray(width)
    for (x in 0 until width) {
        val azimuth = Math.toRadians(snapshot.center.azimuth - 180 + (x + .5) * 360 / width)
        sine[x] = sin(azimuth); cosine[x] = cos(azimuth)
    }
    val row = IntArray(width)
    val strip = Bitmap.createBitmap(width, 1, Bitmap.Config.ARGB_8888)
    val paint = Paint()
    try {
        for (y in 0 until sceneHeight / 2) {
            val elevation = PI / 2 - (y + .5) * PI / sceneHeight
            val up = sin(elevation); val horizontal = cos(elevation)
            val fade = opacity * spaceCompassStarHorizonOpacity(up)
            for (x in 0 until width) {
                val east = horizontal * sine[x]; val north = horizontal * cosine[x]
                val qx = basis.east.x * east + basis.north.x * north + basis.up.x * up
                val qy = basis.east.y * east + basis.north.y * north + basis.up.y * up
                val qz = basis.east.z * east + basis.north.z * north + basis.up.z * up
                val u = ((.5 - atan2(qy, qx) / (2 * PI)) % 1 + 1) % 1
                val v = .5 - asin(qz.coerceIn(-1.0, 1.0)) / PI
                val sx = u * mapWidth - .5; val sy = v * mapHeight - .5
                val ix = floor(sx).toInt(); val iy = floor(sy).toInt()
                val fx = sx - ix; val fy = sy - iy
                val x0 = ((ix % mapWidth) + mapWidth) % mapWidth; val x1 = (x0 + 1) % mapWidth
                val y0 = iy.coerceIn(0, mapHeight - 1); val y1 = (iy + 1).coerceIn(0, mapHeight - 1)
                val a = pixels[y0 * mapWidth + x0]; val b = pixels[y0 * mapWidth + x1]
                val c = pixels[y1 * mapWidth + x0]; val d = pixels[y1 * mapWidth + x1]
                fun channel(shift: Int): Double = ((a shr shift and 255) * (1 - fx) + (b shr shift and 255) * fx) * (1 - fy) +
                    ((c shr shift and 255) * (1 - fx) + (d shr shift and 255) * fx) * fy
                val red = channel(16); val green = channel(8); val blue = channel(0)
                val peak = max(red, max(green, blue))
                val alpha = (peak * fade).roundToInt().coerceIn(0, 255)
                row[x] = if (alpha == 0 || peak == 0.0) 0 else (alpha shl 24) or
                    ((red * 255 / peak).roundToInt().coerceIn(0, 255) shl 16) or
                    ((green * 255 / peak).roundToInt().coerceIn(0, 255) shl 8) or
                    (blue * 255 / peak).roundToInt().coerceIn(0, 255)
            }
            strip.setPixels(row, 0, width, 0, 0, width, 1)
            canvas.drawBitmap(strip, 0f, y.toFloat(), paint)
        }
    } finally { strip.recycle() }
}
