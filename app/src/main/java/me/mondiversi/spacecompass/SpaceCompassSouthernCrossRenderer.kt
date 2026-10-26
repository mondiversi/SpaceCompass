package me.mondiversi.spacecompass

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import androidx.compose.foundation.Canvas as ComposeCanvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import kotlin.math.abs
import kotlin.math.hypot

/** A measured angular pattern, rendered with the same projection as every orbital ray. */
internal fun drawSpaceCompassSouthernCross(canvas: Canvas, positions: List<SpaceCompassSunPosition>,
    width: Int, height: Int, scale: Float, orientation: SpaceCompassSunOrientation? = null,
    perspective: SpaceCompassPerspective? = null, centreAzimuth: Double = 180.0, showNames: Boolean = true,
    body: SpaceCompassCelestialBody = SpaceCompassCelestialBody.SOUTHERN_CROSS) {
    val pattern = requireNotNull(body.constellationPattern)
    require(positions.size == pattern.stars.size)
    val tint = spaceCompassCelestialPathTint(body).toArgb()
    val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = tint; strokeWidth = 2f * scale; strokeCap = Paint.Cap.ROUND }
    val vectors = positions.map { spaceCompassSunCompassDirection(it.azimuthDegrees, it.elevationDegrees) }
    canvas.save(); canvas.clipRect(0, 0, width, height)
    pattern.edges.forEach { (a, b) ->
        val from = vectors[a]; val to = vectors[b]
        for (step in 0 until 16) {
            fun ray(f: Double) = SpaceCompassSunVector(from.east + (to.east - from.east) * f,
                from.north + (to.north - from.north) * f, from.up + (to.up - from.up) * f)
            val start = ray(step / 16.0); val end = ray((step + 1) / 16.0)
            if (orientation != null) {
                projectSpaceCompassSunPathSegment(start, end, orientation, width.toDouble(), height.toDouble(),
                    (start.up + end.up) < 0, perspective)?.let {
                    canvas.drawLine(it.start.x.toFloat(), it.start.y.toFloat(), it.end.x.toFloat(), it.end.y.toFloat(), paint)
                }
            } else {
                val x = spaceCompassPanoramaVectorPoint(start, width.toDouble(), height.toDouble(), centreAzimuth) ?: return@forEach
                val y = spaceCompassPanoramaVectorPoint(end, width.toDouble(), height.toDouble(), centreAzimuth) ?: return@forEach
                var endX = y.x
                if (abs(x.x - endX) > width / 2.0) endX += if (endX < x.x) width else -width
                for (offset in listOf(-width, 0, width)) canvas.drawLine((x.x + offset).toFloat(), x.y.toFloat(),
                    (endX + offset).toFloat(), y.y.toFloat(), paint)
            }
        }
    }
    val text = spaceCompassOrbitTextPaint(13f * scale).apply { textAlign = Paint.Align.LEFT }
    val points = positions.map { position ->
        if (orientation != null) projectSpaceCompassSun(position, orientation, width.toDouble(), height.toDouble(), perspective)
            .takeIf { it.visible }?.let { SpaceCompassSunScenePoint(it.x, it.y) }
            else spaceCompassPanoramaPoint(position, width.toDouble(), height.toDouble(), centreAzimuth)
    }
    val separated = points.filterNotNull().let { available -> available.indices.all { a ->
        (a + 1 until available.size).all { b -> hypot(available[a].x - available[b].x,
            available[a].y - available[b].y) >= 40 * scale }
    } }
    points.forEachIndexed { index, point ->
        point?.let {
            paint.color = Color.BLACK; canvas.drawCircle(it.x.toFloat(), it.y.toFloat(), 5f * scale, paint)
            paint.color = Color.WHITE; canvas.drawCircle(it.x.toFloat(), it.y.toFloat(), 3f * scale, paint)
            paint.color = tint
            if (showNames && separated) {
                text.color = Color.WHITE; text.setShadowLayer(2f * scale, 0f, 0f, Color.BLACK)
                canvas.drawText(pattern.stars[index].name, it.x.toFloat() + 7f * scale,
                    it.y.toFloat() - 5f * scale, text)
            }
        }
    }
    canvas.restore()
}

@Composable
internal fun SpaceCompassSouthernCrossLayer(enabled: Boolean, timeMs: Long, latitude: Double?, longitude: Double?,
    altitude: Double, orientation: SpaceCompassSunOrientation, perspective: SpaceCompassPerspective?) {
    if (!enabled || latitude == null || longitude == null) return
    val positions = remember(timeMs / 1000, latitude, longitude, altitude) {
        calculateSpaceCompassSouthernCross(timeMs, latitude, longitude, altitude)
    }
    ComposeCanvas(Modifier.fillMaxSize()) {
        drawIntoCanvas { drawSpaceCompassSouthernCross(it.nativeCanvas, positions, size.width.toInt(), size.height.toInt(),
            density, orientation, perspective) }
    }
}

/** Only checked constellations are projected; unused catalogue patterns do no frame work. */
@Composable
internal fun SpaceCompassConstellationsLayer(bodies: Collection<SpaceCompassCelestialBody>, timeMs: Long,
    latitude: Double?, longitude: Double?, altitude: Double, orientation: SpaceCompassSunOrientation,
    perspective: SpaceCompassPerspective?) {
    if (latitude == null || longitude == null) return
    val constellations = remember(bodies) { bodies.filter { it.constellationPattern != null } }
    val positions = remember(constellations, timeMs / 1000, latitude, longitude, altitude) {
        constellations.associateWith { calculateSpaceCompassConstellation(it, timeMs, latitude, longitude, altitude) }
    }
    ComposeCanvas(Modifier.fillMaxSize()) {
        drawIntoCanvas { canvas -> positions.forEach { (body, stars) ->
            drawSpaceCompassSouthernCross(canvas.nativeCanvas, stars, size.width.toInt(), size.height.toInt(),
                density, orientation, perspective, body = body)
        } }
    }
}
