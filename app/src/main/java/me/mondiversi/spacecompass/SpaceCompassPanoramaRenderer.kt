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
    val azimuthNames: List<String> = emptyList(), val markerTimes: Map<Long, String> = emptyMap(),
    val showPointLabels: Boolean = true, val observerPositionKnown: Boolean = true, val showSkyReferences: Boolean = SPACE_COMPASS_SKY_REFERENCES_DEFAULT,
    val formatting: SpaceCompassPanoramaFormatting = spaceCompassPanoramaInternationalFormatting,
    val referenceNames: Map<SpaceCompassSkyReference, String> = emptyMap(),
    val poleNames: Pair<String, String> = "North celestial pole" to "South celestial pole",
    val center: SpaceCompassPanoramaCenter = SpaceCompassPanoramaCenter.SOUTH,
    val observerPointNames: Pair<String, String> = "Earth centre" to "Zenith",
    val weatherEffectsEnabled: Boolean = true,
    val currentPointName: String = "Current", val currentTime: String? = null,
    val pointEventNames: Map<SpaceCompassSunPathEvent, String> = spaceCompassPanoramaDefaultEventNames)

internal fun renderSpaceCompassPanorama(snapshot: SpaceCompassPanoramaSnapshot, width: Int = 4096, context: android.content.Context? = null): Bitmap {
    require(width in 512..4096 && snapshot.objects.isNotEmpty())
    val scale = width / 4096f
    val sceneWidth = width
    val centerAzimuth = snapshot.center.azimuth
    val observations = snapshot.objects.associateWith { item ->
        runCatching { calculateSpaceCompassCelestialObservation(item.body, snapshot.timeMs,
            snapshot.latitude, snapshot.longitude, snapshot.altitude, snapshot.remote) }.getOrNull()
    }
    val sceneHeight = sceneWidth / 2
    // A separate slim heading keeps even the zenith marker clear of the capture details.
    // Angular projection remains 360° × 180° at its original 2:1 scale.
    val sceneTop = SPACE_COMPASS_PANORAMA_HEADER_HEIGHT * scale
    val bitmap = Bitmap.createBitmap(width, sceneHeight + sceneTop.toInt(), Bitmap.Config.ARGB_8888)
    try {
        val canvas = Canvas(bitmap)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        val sky = spaceCompassSunSkyColors(snapshot.phase, snapshot.weather)
        canvas.drawColor(sky.first.toArgb())
        val captionBounds = drawSpaceCompassPanoramaCaption(canvas, snapshot.caption, sceneWidth, scale)
        // All scene geometry uses the same local angular coordinates. The extra 44 pixels
        // above +90° keep a current object image whole without reaching the caption.
        canvas.save(); canvas.translate(0f, sceneTop)
        canvas.clipRect(0f, -44 * scale, sceneWidth.toFloat(), sceneHeight.toFloat())
        captionBounds.offset(0f, -sceneTop)
        paint.shader = LinearGradient(0f, 0f, 0f, sceneHeight / 2f,
            sky.first.toArgb(), sky.second.toArgb(), Shader.TileMode.CLAMP)
        canvas.drawRect(0f, 0f, sceneWidth.toFloat(), sceneHeight / 2f, paint)
        val ground = spaceCompassSunGroundPalette(snapshot.phase)
        paint.shader = LinearGradient(0f, sceneHeight / 2f, 0f, sceneHeight.toFloat(),
            ground.farArgb, ground.nearArgb, Shader.TileMode.CLAMP)
        canvas.drawRect(0f, sceneHeight / 2f, sceneWidth.toFloat(), sceneHeight.toFloat(), paint)
        paint.shader = null
        drawSpaceCompassPanoramaStars(canvas, snapshot, sceneWidth, sceneHeight, context)
        SpaceCompassWeatherRenderer(sceneWidth.toFloat(), sceneHeight / 2f, snapshot.phase, snapshot.weather).draw(canvas)
        val text = spaceCompassOrbitTextPaint(34 * scale)
        fun textAt(value: String, x: Float, y: Float, size: Float = 34 * scale, tint: Int = Color.WHITE) {
            text.textSize = size
            text.style = Paint.Style.FILL; text.color = tint
            text.setShadowLayer(3 * scale, 0f, scale, Color.BLACK)
            val half = text.measureText(value) / 2
            canvas.drawText(value, x.coerceIn(half + 12 * scale, sceneWidth - half - 12 * scale), y, text)
            text.clearShadowLayer()
        }
        // A complete 360° × 180° angular reference, including zenith/nadir and intercardinals.
        paint.color = 0x30FFFFFF; paint.strokeWidth = scale
        for (i in 0..12) canvas.drawLine(sceneWidth * i / 12f, 0f, sceneWidth * i / 12f, sceneHeight.toFloat(), paint)
        for (i in 0..6) canvas.drawLine(0f, sceneHeight * i / 6f, sceneWidth.toFloat(), sceneHeight * i / 6f, paint)
        paint.color = 0x99FFFFFF.toInt(); paint.strokeWidth = 2 * scale
        canvas.drawLine(0f, sceneHeight / 2f, sceneWidth.toFloat(), sceneHeight / 2f, paint)
        val cardinalCount = snapshot.cardinalNames.size
        val cardinalSymbols = when (cardinalCount) {
            8 -> listOf("N", "NE", "E", "SE", "S", "SW", "W", "NW")
            4 -> listOf("N", "E", "S", "W")
            else -> snapshot.cardinalNames
        }
        for (i in 0..cardinalCount) {
            val x = sceneWidth * i / cardinalCount.toFloat()
            paint.style = Paint.Style.FILL; paint.color = 0xdd101923.toInt()
            canvas.drawCircle(x, sceneHeight / 2f, 9 * scale, paint)
            paint.color = Color.WHITE
            canvas.drawCircle(x, sceneHeight / 2f, 5 * scale, paint)
            textAt(snapshot.cardinalNames[spaceCompassPanoramaGridIndex(i, cardinalCount, snapshot.center)], x,
                sceneHeight / 2f - 22 * scale, 32 * scale)
            textAt(cardinalSymbols[spaceCompassPanoramaGridIndex(i, cardinalCount, snapshot.center)], x,
                sceneHeight / 2f + 42 * scale, 28 * scale)
        }
        val elevations = if (snapshot.elevationNames.size == 7) snapshot.elevationNames else
            listOf("+90°") + snapshot.elevationNames + "−90°"
        elevations.forEachIndexed { i, value ->
            val y = (sceneHeight * i / 6f + 28 * scale).coerceIn(28 * scale, sceneHeight - 60 * scale)
            textAt(value, 72 * scale, y, 25 * scale)
            textAt(value, sceneWidth - 72 * scale, y, 25 * scale)
        }
        for (i in 0..12) {
            val index = if (snapshot.center == SpaceCompassPanoramaCenter.SOUTH && i == 12) 12
                else spaceCompassPanoramaGridIndex(i, 12, snapshot.center)
            val value = snapshot.azimuthNames.getOrNull(index) ?: "${index * 30}°"
            textAt(value, sceneWidth * i / 12f, sceneHeight - 16 * scale, 25 * scale)
        }
        val occupied = mutableListOf(captionBounds)
        text.textSize = 32 * scale
        for (i in 0..cardinalCount) {
            val half = text.measureText(snapshot.cardinalNames[spaceCompassPanoramaGridIndex(i, cardinalCount, snapshot.center)]) / 2
            val x = (sceneWidth * i / cardinalCount.toFloat()).coerceIn(half + 12 * scale, sceneWidth - half - 12 * scale)
            occupied += RectF(x - half - 8 * scale, sceneHeight / 2f - 58 * scale,
                x + half + 8 * scale, sceneHeight / 2f + 52 * scale)
        }
        // Reserve current images and names before laying out orbital labels and times.
        // They are drawn last, so unreserved pills would otherwise disappear underneath them.
        text.textSize = 32 * scale
        observations.forEach { (item, observation) ->
            val p = observation?.position?.let { spaceCompassPanoramaPoint(it, sceneWidth.toDouble(), sceneHeight.toDouble(), centerAzimuth) }
                ?: return@forEach
            val x = p.x.toFloat(); val y = p.y.toFloat()
            occupied += RectF(x - 44 * scale, y - 44 * scale, x + 44 * scale, y + 44 * scale)
            if (snapshot.showPointLabels) {
                val half = text.measureText(item.name) / 2
                val labelX = x.coerceIn(half + 12 * scale, sceneWidth - half - 12 * scale)
                val labelY = (y - 48 * scale).coerceIn(105 * scale, sceneHeight - 20 * scale)
                occupied += RectF(labelX - half - 6 * scale, labelY - 36 * scale,
                    labelX + half + 6 * scale, labelY + 8 * scale)
            }
        }
        val referenceDrawing = if (snapshot.showSkyReferences && snapshot.observerPositionKnown)
            drawSpaceCompassSkyReferences(canvas, snapshot.latitude, snapshot.timeMs, sceneWidth, sceneHeight, scale, occupied,
                showLabels = snapshot.showPointLabels, referenceNames = snapshot.referenceNames, poleNames = snapshot.poleNames,
                numericFormat = snapshot.formatting.numeric, systemLocale = snapshot.formatting.deviceLocale,
                centerAzimuthDegrees = centerAzimuth, observerPointNames = snapshot.observerPointNames,
                observerAltitude = snapshot.altitude)
        else SpaceCompassSkyReferenceDrawing(emptyList(), emptyList())
        // Defer point labels until every orbit is drawn, so other paths cannot cross their text.
        val pointLabels = mutableListOf<SpaceCompassPanoramaTimeLabel>()
        val pointMarkers = mutableListOf<Triple<SpaceCompassSunPathPoint, SpaceCompassSunScenePoint, Int>>()
        // Draw every curve before live markers, keeping the selected set frozen throughout export.
        snapshot.objects.forEach { item ->
            val path = item.path ?: return@forEach
            val segments = spaceCompassPanoramaSegments(path, sceneWidth.toDouble(), sceneHeight.toDouble(), centerAzimuth)
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
                val center = spaceCompassPanoramaVectorPoint(direction.center, sceneWidth.toDouble(), sceneHeight.toDouble(), centerAzimuth) ?: return@forEach
                val a = spaceCompassPanoramaVectorPoint(direction.start, sceneWidth.toDouble(), sceneHeight.toDouble(), centerAzimuth) ?: return@forEach
                val b = spaceCompassPanoramaVectorPoint(direction.end, sceneWidth.toDouble(), sceneHeight.toDouble(), centerAzimuth) ?: return@forEach
                var dx = b.x - a.x
                if (dx > sceneWidth / 2) dx -= sceneWidth else if (dx < -sceneWidth / 2) dx += sceneWidth
                val dy = b.y - a.y; val length = hypot(dx, dy)
                if (length < 1e-6 || center.x < 18 * scale || center.x > sceneWidth - 18 * scale) return@forEach
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
                if (snapshot.showPointLabels) drawSpaceCompassOrbitName(canvas, item.name, segments, arrow, text, paint.color, 18 * scale, occupied)
            }
            paint.style = Paint.Style.FILL
            path.markers.forEach { marker ->
                val p = spaceCompassPanoramaPoint(marker.position, sceneWidth.toDouble(), sceneHeight.toDouble(), centerAzimuth) ?: return@forEach
                val markerTint = spaceCompassCelestialPathVisibilityTint(tint, marker.position.elevationDegrees < 0).toArgb()
                pointMarkers += Triple(marker, p, markerTint)
                if (marker.event != SpaceCompassSunPathEvent.HOUR) {
                    // Event glyphs must stay clear of neighboring time pills and late orbit strokes.
                    val radius = 17 * scale
                    for (x in listOf(p.x.toFloat(), p.x.toFloat() - sceneWidth, p.x.toFloat() + sceneWidth))
                        occupied += RectF(x - radius, p.y.toFloat() - radius, x + radius, p.y.toFloat() + radius)
                }
                if (!snapshot.showPointLabels) return@forEach
                val time = snapshot.markerTimes[marker.timeMs] ?: java.time.Instant.ofEpochMilli(marker.timeMs)
                    .atZone(path.zone).toLocalTime().let { "%02d:%02d".format(java.util.Locale.ROOT, it.hour, it.minute) }
                // Read elevation from this object's marker, not the timestamp shared by other paths.
                val label = formatSpaceCompassPanoramaPathPointLabel(time, marker, snapshot.formatting, snapshot.pointEventNames)
                pointLabels += SpaceCompassPanoramaTimeLabel(label, p, markerTint,
                    marker.events.any { it != SpaceCompassSunPathEvent.HOUR })
            }
        }
        pointMarkers.forEach { (marker, point, tint) ->
            // Repeat the edge fragment at the 0/360 degree seam so event symbols remain continuous.
            val radius = 17 * scale
            val centers = buildList {
                add(point)
                if (point.x < radius) add(point.copy(x = point.x + sceneWidth))
                if (point.x > sceneWidth - radius) add(point.copy(x = point.x - sceneWidth))
            }
            centers.forEach { drawSpaceCompassPanoramaEventMarker(canvas, marker.event, it, scale, tint) }
        }
        if (snapshot.showPointLabels) observations.forEach { (item, observation) ->
            val position = observation?.position ?: return@forEach
            val point = spaceCompassPanoramaPoint(position, sceneWidth.toDouble(), sceneHeight.toDouble(), centerAzimuth)
                ?: return@forEach
            pointLabels += spaceCompassPanoramaCurrentLabel(snapshot, position, point,
                spaceCompassCelestialPathTint(item.body).toArgb(), 38f)
        }
        drawSpaceCompassPanoramaTimeLabels(canvas, pointLabels, sceneWidth, sceneHeight, scale, occupied)
        drawSpaceCompassSkyReferenceLabels(canvas, referenceDrawing, scale)
        observations.forEach { (item, observation) ->
            val p = observation?.position?.let { spaceCompassPanoramaPoint(it, sceneWidth.toDouble(), sceneHeight.toDouble(), centerAzimuth) } ?: return@forEach
            val tint = spaceCompassCelestialPathTint(item.body).toArgb()
            val x = p.x.toFloat(); val y = p.y.toFloat()
            paint.style = Paint.Style.FILL; paint.color = 0xcc07101b.toInt()
            canvas.drawCircle(x, y, 38 * scale, paint)
            paint.style = Paint.Style.STROKE; paint.color = tint; paint.strokeWidth = 2 * scale
            canvas.drawCircle(x, y, 38 * scale, paint)
            paint.style = Paint.Style.FILL
            val image = renderSpaceCompassPanoramaMarker(context, item.body, snapshot.timeMs)
            try { canvas.drawBitmap(image, null, RectF(x - 36 * scale, y - 36 * scale, x + 36 * scale, y + 36 * scale),
                Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)) } finally { image.recycle() }
            if (snapshot.showPointLabels) {
                text.textSize = 32 * scale
                val half = text.measureText(item.name) / 2
                val labelX = x.coerceIn(half + 12 * scale, sceneWidth - half - 12 * scale)
                val labelY = (y - 48 * scale).coerceIn(105 * scale, sceneHeight - 20 * scale)
                textAt(item.name, labelX, labelY, 32 * scale, tint)
            }
        }
        canvas.restore()
        return bitmap
    } catch (error: Throwable) {
        bitmap.recycle()
        throw error
    }
}
