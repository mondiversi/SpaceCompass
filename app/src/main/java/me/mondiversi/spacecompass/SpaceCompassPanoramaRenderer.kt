package me.mondiversi.spacecompass

import android.graphics.*
import androidx.compose.ui.graphics.toArgb
import kotlin.math.*

internal data class SpaceCompassPanoramaObject(val body: SpaceCompassCelestialBody, val name: String,
    val path: SpaceCompassSunDailyPath?)

/** Frozen at the tap. Projection does not depend on orientation, screen dimensions or later GPS updates. */
internal data class SpaceCompassPanoramaSnapshot(val timeMs: Long, val latitude: Double, val longitude: Double,
    val altitude: Double, val phase: SpaceCompassSunSkyPhase, val weather: SpaceCompassSunWeatherSnapshot?,
    val objects: List<SpaceCompassPanoramaObject>, val remote: SpaceCompassCelestialRemoteData,
    val caption: String, val cardinalNames: List<String>, val elevationNames: List<String>,
    val unavailable: String, val pathUnavailable: String)

internal fun renderSpaceCompassPanorama(snapshot: SpaceCompassPanoramaSnapshot, width: Int = 4096): Bitmap {
    require(width in 512..4096 && snapshot.objects.isNotEmpty())
    val scale = width / 4096f
    val sceneHeight = width / 2
    val columns = min(4, snapshot.objects.size)
    val legendRows = (snapshot.objects.size + columns - 1) / columns
    val footerHeight = ((54 + legendRows * 50) * scale).roundToInt()
    val bitmap = Bitmap.createBitmap(width, sceneHeight + footerHeight, Bitmap.Config.ARGB_8888)
    try {
        val canvas = Canvas(bitmap)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        val sky = spaceCompassSunSkyColors(snapshot.phase, snapshot.weather)
        paint.shader = LinearGradient(0f, 0f, 0f, sceneHeight / 2f,
            sky.first.toArgb(), sky.second.toArgb(), Shader.TileMode.CLAMP)
        canvas.drawRect(0f, 0f, width.toFloat(), sceneHeight / 2f, paint)
        val ground = spaceCompassSunGroundPalette(snapshot.phase)
        paint.shader = LinearGradient(0f, sceneHeight / 2f, 0f, sceneHeight.toFloat(),
            ground.farArgb, ground.nearArgb, Shader.TileMode.CLAMP)
        canvas.drawRect(0f, sceneHeight / 2f, width.toFloat(), sceneHeight.toFloat(), paint)
        paint.shader = null
        val night = snapshot.phase == SpaceCompassSunSkyPhase.NIGHT || snapshot.phase == SpaceCompassSunSkyPhase.EVENING
        val cover = spaceCompassSunDisplayCloudCover(snapshot.weather)
        if (night && cover < .65f) repeat(100) { i ->
            paint.color = Color.argb(((1 - cover) * 130).toInt(), 255, 255, 255)
            canvas.drawCircle(width * ((i * .618034f) % 1), sceneHeight * (.04f + ((i * .414214f) % 1) * .40f),
                (if (i % 4 == 0) 2.5f else 1.5f) * scale, paint)
        }
        repeat(spaceCompassSunDisplayCloudCount(snapshot.weather)) { i ->
            paint.color = Color.argb((35 + cover * 65).toInt(), 215, 228, 238)
            val x = width * ((.13f + i * .38197f) % 1); val y = sceneHeight * (.06f + (i % 4) * .08f)
            repeat(4) { part ->
                val r = (35 + cover * 45) * scale
                canvas.drawOval(x + (part - 2) * r, y - r * .65f, x + part * r, y + r * .65f, paint)
            }
        }
        val text = spaceCompassOrbitTextPaint(34 * scale)
        fun textAt(value: String, x: Float, y: Float, size: Float = 34 * scale, tint: Int = Color.WHITE) {
            text.textSize = size
            text.style = Paint.Style.FILL; text.color = tint
            text.setShadowLayer(3 * scale, 0f, scale, Color.BLACK)
            val half = text.measureText(value) / 2
            canvas.drawText(value, x.coerceIn(half + 12 * scale, width - half - 12 * scale), y, text)
            text.clearShadowLayer()
        }
        // Cardinal grid is an angular reference, not a terrestrial camera photograph.
        paint.color = 0x30FFFFFF; paint.strokeWidth = scale
        for (i in 0..7) canvas.drawLine(width * i / 8f, 65 * scale, width * i / 8f, sceneHeight.toFloat(), paint)
        for (i in 1..5) canvas.drawLine(0f, sceneHeight * i / 6f, width.toFloat(), sceneHeight * i / 6f, paint)
        paint.color = 0x99FFFFFF.toInt(); paint.strokeWidth = 2 * scale
        canvas.drawLine(0f, sceneHeight / 2f, width.toFloat(), sceneHeight / 2f, paint)
        for (i in 0..4) textAt(snapshot.cardinalNames[i % 4],
            (width * i / 4f).coerceIn(30 * scale, width - 30 * scale), sceneHeight / 2f - 18 * scale, 32 * scale)
        snapshot.elevationNames.forEachIndexed { i, value ->
            textAt(value, width - 70 * scale, sceneHeight * (i + 1) / 6f + (if (i == 2) 28 else -12) * scale, 25 * scale)
        }
        textAt(snapshot.caption, width / 2f, 48 * scale, 34 * scale)
        val occupied = mutableListOf<RectF>()
        val observations = snapshot.objects.associateWith { item ->
            runCatching { calculateSpaceCompassCelestialObservation(item.body, snapshot.timeMs,
                snapshot.latitude, snapshot.longitude, snapshot.altitude, snapshot.remote) }.getOrNull()
        }
        // Draw every curve before live markers, keeping the selected set frozen throughout export.
        snapshot.objects.forEach { item ->
            val path = item.path ?: return@forEach
            val segments = spaceCompassPanoramaSegments(path, width.toDouble(), sceneHeight.toDouble())
            val tint = spaceCompassCelestialPathTint(item.body)
            for (below in listOf(false, true)) {
                val curve = Path()
                var end: SpaceCompassSunScenePoint? = null
                segments.filter { it.belowHorizon == below }.forEach { segment ->
                    if (end == null || hypot(end!!.x - segment.start.x, end!!.y - segment.start.y) > .5)
                        curve.moveTo(segment.start.x.toFloat(), segment.start.y.toFloat())
                    curve.lineTo(segment.end.x.toFloat(), segment.end.y.toFloat()); end = segment.end
                }
                paint.style = Paint.Style.STROKE; paint.strokeJoin = Paint.Join.ROUND
                paint.pathEffect = if (below) DashPathEffect(floatArrayOf(12 * scale, 8 * scale), 0f) else null
                paint.color = 0x65000000; paint.strokeWidth = 7 * scale; canvas.drawPath(curve, paint)
                paint.color = spaceCompassCelestialPathLineTint(tint, below).toArgb()
                paint.strokeWidth = 3 * scale; canvas.drawPath(curve, paint)
            }
            paint.pathEffect = null
            spaceCompassSunPathDirections(path).forEach { direction ->
                val center = spaceCompassPanoramaVectorPoint(direction.center, width.toDouble(), sceneHeight.toDouble()) ?: return@forEach
                val a = spaceCompassPanoramaVectorPoint(direction.start, width.toDouble(), sceneHeight.toDouble()) ?: return@forEach
                val b = spaceCompassPanoramaVectorPoint(direction.end, width.toDouble(), sceneHeight.toDouble()) ?: return@forEach
                var dx = b.x - a.x
                if (dx > width / 2) dx -= width else if (dx < -width / 2) dx += width
                val dy = b.y - a.y; val length = hypot(dx, dy)
                if (length < 1e-6 || center.x < 18 * scale || center.x > width - 18 * scale) return@forEach
                val x = dx / length * 10 * scale; val y = dy / length * 10 * scale
                val arrow = SpaceCompassSunPathArrow(center, center.copy(x = center.x + x, y = center.y + y),
                    center.copy(x = center.x - x - y * .7, y = center.y - y + x * .7),
                    center.copy(x = center.x - x + y * .7, y = center.y - y - x * .7), direction.center.up < 0)
                val chevron = Path().apply { moveTo(arrow.left.x.toFloat(), arrow.left.y.toFloat())
                    lineTo(arrow.tip.x.toFloat(), arrow.tip.y.toFloat()); lineTo(arrow.right.x.toFloat(), arrow.right.y.toFloat()) }
                paint.style = Paint.Style.STROKE; paint.strokeWidth = 4 * scale
                paint.color = spaceCompassCelestialPathVisibilityTint(tint, arrow.belowHorizon).toArgb()
                canvas.drawPath(chevron, paint)
                text.textSize = 32 * scale
                drawSpaceCompassOrbitName(canvas, item.name, segments, arrow, text, paint.color, 18 * scale, occupied)
            }
            paint.style = Paint.Style.FILL
            path.markers.forEach { marker ->
                val p = spaceCompassPanoramaPoint(marker.position, width.toDouble(), sceneHeight.toDouble()) ?: return@forEach
                paint.color = spaceCompassCelestialPathVisibilityTint(tint, marker.position.elevationDegrees < 0).toArgb()
                canvas.drawCircle(p.x.toFloat(), p.y.toFloat(), (if (marker.event == SpaceCompassSunPathEvent.HOUR) 6 else 9) * scale, paint)
            }
        }
        observations.forEach { (item, observation) ->
            val p = observation?.position?.let { spaceCompassPanoramaPoint(it, width.toDouble(), sceneHeight.toDouble()) } ?: return@forEach
            val tint = spaceCompassCelestialPathTint(item.body).toArgb()
            val x = p.x.toFloat(); val y = p.y.toFloat()
            paint.style = Paint.Style.FILL; paint.color = Color.BLACK
            canvas.drawCircle(x, y, 22 * scale, paint)
            paint.color = tint; canvas.drawCircle(x, y, 17 * scale, paint)
            paint.color = Color.WHITE; canvas.drawCircle(x, y, 4 * scale, paint)
            text.textSize = 32 * scale
            val half = text.measureText(item.name) / 2
            val labelX = x.coerceIn(half + 12 * scale, width - half - 12 * scale)
            val labelY = (y - 30 * scale).coerceIn(105 * scale, sceneHeight - 20 * scale)
            textAt(item.name, labelX, labelY, 32 * scale, tint)
        }
        paint.style = Paint.Style.FILL; paint.color = 0xFF101923.toInt()
        canvas.drawRect(0f, sceneHeight.toFloat(), width.toFloat(), bitmap.height.toFloat(), paint)
        snapshot.objects.forEachIndexed { index, item ->
            val x = width * ((index % columns) + .5f) / columns
            val y = sceneHeight + (55 + index / columns * 50) * scale
            val status = if (observations[item] == null) snapshot.unavailable else
                if (item.body.supportsDailyPath && item.path == null) snapshot.pathUnavailable else ""
            val label = item.name + if (status.isEmpty()) "" else " · $status"
            text.textSize = 28 * scale
            val size = (28 * scale * min(1f, (width / columns.toFloat() - 32 * scale) / text.measureText(label))).coerceAtLeast(17 * scale)
            textAt(label, x, y, size, spaceCompassCelestialPathTint(item.body).toArgb())
        }
        return bitmap
    } catch (error: Throwable) {
        bitmap.recycle()
        throw error
    }
}
