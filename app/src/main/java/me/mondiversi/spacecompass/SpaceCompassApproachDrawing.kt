package me.mondiversi.spacecompass

import android.graphics.Canvas
import android.graphics.Paint
import androidx.compose.foundation.Canvas as ComposeCanvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp

internal fun DrawScope.drawSpaceCompassApproachGlyph(center: Offset, radius: Float, color: Color, width: Float) {
    drawCircle(color, radius * .68f, center - Offset(radius*.42f,0f), style = Stroke(width))
    drawCircle(color, radius * .68f, center + Offset(radius*.42f,0f), style = Stroke(width))
}

internal fun drawSpaceCompassApproachGlyph(canvas: Canvas, x: Float, y: Float, radius: Float, color: Int, width: Float) {
    val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply { this.color = color; style = Paint.Style.STROKE; strokeWidth = width }
    canvas.drawCircle(x-radius*.42f,y,radius*.68f,paint)
    canvas.drawCircle(x+radius*.42f,y,radius*.68f,paint)
}

internal fun DrawScope.drawSpaceCompassApproachMarker(point: Offset, tint: Color) {
    drawCircle(Color.Black.copy(alpha = .8f), 9.dp.toPx(), point)
    drawSpaceCompassApproachGlyph(point, 6.dp.toPx(), tint, 1.6.dp.toPx())
}

/** Dedicated event instants, not rounded hourly dots. Paint above every orbit and live thumbnail. */
@Composable
internal fun SpaceCompassApproachPathLayer(paths: Map<SpaceCompassCelestialBody, SpaceCompassSunDailyPath>,
    orientation: SpaceCompassSunOrientation?, perspective: SpaceCompassPerspective?,
    state: SpaceCompassSunDailyPathUiState, focused: SpaceCompassCelestialSceneTarget?) {
    if (orientation == null) return
    val accent = androidx.compose.material3.MaterialTheme.colorScheme.primary
    val markers = remember(paths) { paths.flatMap { (body, path) ->
        path.markers.filter { SpaceCompassSunPathEvent.APPROACH in it.events }.map { Triple(body, path, it) }
    } }.sortedBy { (body, path, marker) ->
        if (focused?.body == body && focused.point == marker) 0 else if (state.isSelected(path, marker)) 1 else 2
    }
    ComposeCanvas(Modifier.fillMaxSize().testTag("close-approach-path-markers")) {
        val occupied = mutableListOf<Pair<Long, SpaceCompassSunScenePoint>>()
        markers.forEach { (body, path, marker) ->
                val projected = projectSpaceCompassSun(marker.position, orientation,
                    size.width.toDouble(), size.height.toDouble(), perspective)
                if (!projected.visible) return@forEach
                val anchor = SpaceCompassSunScenePoint(projected.x, projected.y)
                if (spaceCompassApproachMarkerIsDuplicate(marker.timeMs, anchor, occupied, 18.dp.toPx().toDouble())) return@forEach
                occupied += marker.timeMs to anchor
                val point = Offset(projected.x.toFloat(), projected.y.toFloat())
                val selected = state.isSelected(path, marker)
                val tint = if (selected) accent else spaceCompassCelestialPathVisibilityTint(
                    spaceCompassCelestialPathTint(body), marker.position.elevationDegrees < 0)
                drawSpaceCompassApproachMarker(point, tint)
                if (selected) drawCircle(accent, 11.dp.toPx(), point, style = Stroke(1.8.dp.toPx()))
                else if (focused?.body == body && focused.point == marker)
                    drawCircle(Color.White, 13.dp.toPx(), point, style = Stroke(1.8.dp.toPx()))
        }
    }
}

/** A small double-circle indicator by the live pair, without changing its object selection. */
@Composable
internal fun SpaceCompassCurrentApproachLayer(overlays: Map<SpaceCompassCelestialBody,SpaceCompassCelestialOverlay>,
    orientation: SpaceCompassSunOrientation?, perspective: SpaceCompassPerspective?, width: Double, height: Double) {
    val pairs = overlays.values.firstOrNull()?.currentApproaches.orEmpty()
    val label = stringResource(R.string.celestial_approach)
    val density = LocalDensity.current.density
    val color = androidx.compose.material3.MaterialTheme.colorScheme.primary
    val caption = remember(density) { Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textSize = 10*density; textAlign = Paint.Align.CENTER; setShadowLayer(2*density,0f,density,android.graphics.Color.BLACK)
    } }
    if (orientation == null || pairs.isEmpty()) return
    ComposeCanvas(Modifier.fillMaxSize()) {
        val occupied = mutableListOf<Offset>()
        pairs.forEach { pair ->
            val projected = projectSpaceCompassSun(pair.firstPosition,orientation,width,height,perspective)
            if (projected.visible) {
                val point = Offset(projected.x.toFloat(), projected.y.toFloat() + 43.dp.toPx())
                if (occupied.any { (it-point).getDistance() < 40.dp.toPx() }) return@forEach
                occupied += point
                drawCircle(Color.Black.copy(alpha = .75f), 11.dp.toPx(), point)
                drawSpaceCompassApproachGlyph(point, 7.dp.toPx(), color, 1.6.dp.toPx())
                caption.color = android.graphics.Color.WHITE
                drawContext.canvas.nativeCanvas.drawText(label, point.x, point.y + 23.dp.toPx(), caption)
            }
        }
    }
}

internal fun drawSpaceCompassCurrentApproaches(canvas: Canvas, snapshot: SpaceCompassPanoramaSnapshot,
    positions: Map<SpaceCompassCelestialBody,SpaceCompassSunPosition>, scale: Float,
    projection: (SpaceCompassSunPosition) -> SpaceCompassSunScenePoint?) {
    val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textSize = 24*scale; textAlign = Paint.Align.CENTER; color = android.graphics.Color.WHITE
        setShadowLayer(3*scale,0f,scale,android.graphics.Color.BLACK)
    }
    val occupied = mutableListOf<SpaceCompassSunScenePoint>()
    spaceCompassCurrentApproaches(snapshot.timeMs,positions).forEach { pair ->
        projection(pair.firstPosition)?.let { point ->
            if (occupied.any { kotlin.math.hypot(it.x-point.x,it.y-point.y) < 80*scale }) return@let
            occupied += point
            val x=point.x.toFloat(); val y=point.y.toFloat()+55*scale
            val background = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xcc07101b.toInt() }
            canvas.drawCircle(x,y,18*scale,background)
            drawSpaceCompassApproachGlyph(canvas,x,y,12*scale, snapshot.selectionTint, 2.5f*scale)
            val active = snapshot.activeBody == pair.first || snapshot.activeBody == pair.second
            val selected = snapshot.selectedPoints.any { it.key.isCurrent && (it.key.body == pair.first || it.key.body == pair.second) }
            if (snapshot.labelMode.showsPoint(selected,active,keyPoint=true))
                canvas.drawText(snapshot.pointEventNames[SpaceCompassSunPathEvent.APPROACH].orEmpty(), x,y+40*scale,paint)
        }
    }
}
