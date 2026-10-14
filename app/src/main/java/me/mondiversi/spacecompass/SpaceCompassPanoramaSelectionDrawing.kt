package me.mondiversi.spacecompass

import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF

internal fun spaceCompassPanoramaSelectionLabels(snapshot: SpaceCompassPanoramaSnapshot,
    selected: List<SpaceCompassPanoramaSelectedPoint>, scale: Float, currentRadius: Float,
    projection: (SpaceCompassSunPosition) -> SpaceCompassSunScenePoint?, occupied: MutableList<RectF>
): List<SpaceCompassPanoramaTimeLabel> = selected.mapNotNull { pin ->
    val point = projection(pin.point.position) ?: return@mapNotNull null
    val radius = if (pin.isCurrent) currentRadius else if (pin.point.event == SpaceCompassSunPathEvent.HOUR) 10f else 20f
    occupied += RectF(point.x.toFloat() - radius * scale, point.y.toFloat() - radius * scale,
        point.x.toFloat() + radius * scale, point.y.toFloat() + radius * scale)
    val caption = formatSpaceCompassPanoramaSelection(snapshot, pin)
    SpaceCompassPanoramaTimeLabel(caption, point, snapshot.selectionTint, principal = true,
        pointRadius = radius, current = pin.isCurrent, selected = true)
}

/** Rings remain visible with labels hidden; camera projection naturally excludes points outside the JPEG. */
internal fun drawSpaceCompassPanoramaSelections(canvas: Canvas, snapshot: SpaceCompassPanoramaSnapshot,
    selected: List<SpaceCompassPanoramaSelectedPoint>, scale: Float, currentRadius: Float,
    projection: (SpaceCompassSunPosition) -> SpaceCompassSunScenePoint?, wrapWidth: Float? = null) {
    val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = snapshot.selectionTint; strokeWidth = 2.5f * scale }
    selected.forEach { pin ->
        val point = projection(pin.point.position) ?: return@forEach
        val radius = if (pin.isCurrent) currentRadius else if (pin.point.event == SpaceCompassSunPathEvent.HOUR) 10f else 20f
        val centers = buildList {
            add(point)
            if (wrapWidth != null) {
                if (point.x < (radius + 2) * scale) add(point.copy(x = point.x + wrapWidth))
                if (point.x > wrapWidth - (radius + 2) * scale) add(point.copy(x = point.x - wrapWidth))
            }
        }
        centers.forEach {
            if (!pin.isCurrent) drawSpaceCompassPanoramaEventMarker(canvas, pin.point.event, it, scale, snapshot.selectionTint)
            paint.style = Paint.Style.STROKE
            canvas.drawCircle(it.x.toFloat(), it.y.toFloat(), radius * scale, paint)
        }
    }
}
