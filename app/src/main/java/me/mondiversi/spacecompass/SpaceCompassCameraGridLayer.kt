package me.mondiversi.spacecompass

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.isSupported
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.conflate
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import kotlin.math.hypot

private data class SpaceCompassLiveCameraGrid(val size: IntSize,val paths: List<Path>)

/** Decorative only: reuse camera rays, retain the last frame while Default prepares the next one. */
@Composable
internal fun SpaceCompassCameraGridLayer(orientation: SpaceCompassSunOrientation?,
    lens: SpaceCompassPerspective?,opticalFrame: SpaceCompassSunSceneFrame?,modifier: Modifier,tag: String="camera-angular-grid") {
    if(orientation==null||lens==null||opticalFrame==null) return
    val adaptiveContrast=remember { BlendMode.Difference.isSupported() && BlendMode.Saturation.isSupported() }
    var viewport by remember { mutableStateOf(IntSize.Zero) }
    val currentOrientation by rememberUpdatedState(orientation)
    val drawing by produceState<SpaceCompassLiveCameraGrid?>(null,lens,opticalFrame,viewport) {
        val requestedSize=viewport
        if(requestedSize.width<=0||requestedSize.height<=0) { value=null;return@produceState }
        // One worker consumes the latest pose, without cancelling every frame during a steady pan.
        snapshotFlow { currentOrientation }.conflate().collect { pointing ->
            value=withContext(Dispatchers.Default) {
                val width=requestedSize.width.toDouble();val height=requestedSize.height.toDouble()
                val perspective=requireNotNull(spaceCompassSceneFramePerspective(width,height,opticalFrame,lens))
                val grid=spaceCompassCameraAngularGrid(pointing,perspective,width,height,
                    SpaceCompassSunSceneFrame(0.0,0.0,width,height),includeBorderTicks=false)
                ensureActive()
                val paths=grid.lines.map { line ->
                    Path().apply {
                        var end: SpaceCompassSunScenePoint?=null
                        line.segments.forEach { segment ->
                            if(end==null||hypot(end!!.x-segment.start.x,end!!.y-segment.start.y)>.2)
                                moveTo(segment.start.x.toFloat(),segment.start.y.toFloat())
                            lineTo(segment.end.x.toFloat(),segment.end.y.toFloat());end=segment.end
                        }
                    }
                }
                SpaceCompassLiveCameraGrid(requestedSize,paths)
            }
        }
    }
    Canvas(modifier.clipToBounds().onSizeChanged { viewport=it }.testTag(tag)) {
        drawing?.takeIf { it.size==viewport }?.paths?.forEach { path ->
            // Blend against the already drawn sky/TextureView, without isolating a transparent layer.
            // A dark hairline also protects middle-grey regions where simple inversion loses contrast.
            drawPath(path,Color.Black.copy(alpha=.32f),style=Stroke(.9.dp.toPx()))
            if(adaptiveContrast) {
                drawPath(path,Color.White,style=Stroke(SpaceCompassAngularGridAppearance.liveLineDp.dp.toPx()),blendMode=BlendMode.Difference)
                // Remove complementary hues: only the local light/dark contrast should change.
                drawPath(path,Color.Black,style=Stroke(SpaceCompassAngularGridAppearance.liveLineDp.dp.toPx()),blendMode=BlendMode.Saturation)
            } else {
                // Android 8/9 do not implement these blend modes; preserve a contrasting two-tone grid.
                drawPath(path,Color.White.copy(alpha=.72f),style=Stroke(SpaceCompassAngularGridAppearance.liveLineDp.dp.toPx()))
            }
        }
    }
}
