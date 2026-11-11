package me.mondiversi.spacecompass

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
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

private data class SpaceCompassLiveCameraGrid(val size: IntSize,val path: Path)

/** Decorative only: one worker owns reusable projection buffers; drawing keeps an immutable frame. */
@Composable
internal fun SpaceCompassCameraGridLayer(orientation: SpaceCompassSunOrientation?,
    lens: SpaceCompassPerspective?,opticalFrame: SpaceCompassSunSceneFrame?,modifier: Modifier,tag: String="camera-angular-grid") {
    if(orientation==null||lens==null||opticalFrame==null) return
    var viewport by remember { mutableStateOf(IntSize.Zero) }
    val currentOrientation by rememberUpdatedState(orientation)
    val drawing = produceState<SpaceCompassLiveCameraGrid?>(null,lens,opticalFrame,viewport) {
        val requestedSize=viewport
        if(requestedSize.width<=0||requestedSize.height<=0) { value=null;return@produceState }
        val width=requestedSize.width.toDouble();val height=requestedSize.height.toDouble()
        val perspective=requireNotNull(spaceCompassSceneFramePerspective(width,height,opticalFrame,lens))
        val projector=withContext(Dispatchers.Default) { SpaceCompassLiveGridProjector(width,height,perspective) }
        // Conflate pending sensor samples, without cancelling a calculation during a steady pan.
        snapshotFlow { currentOrientation }.conflate().collect { pointing ->
            value=withContext(Dispatchers.Default) {
                val path=Path()
                projector.project(pointing,object : SpaceCompassLiveGridSink {
                    override fun moveTo(x: Double,y: Double) { path.moveTo(x.toFloat(),y.toFloat()) }
                    override fun lineTo(x: Double,y: Double) { path.lineTo(x.toFloat(),y.toFloat()) }
                })
                ensureActive()
                SpaceCompassLiveCameraGrid(requestedSize,path)
            }
        }
    }
    Canvas(modifier.clipToBounds().onSizeChanged { viewport=it }.testTag(tag)) {
        // Read the frame only in drawing, so publishing it never invalidates composition/layout.
        drawing.value?.takeIf { it.size==viewport }?.let { grid ->
            drawPath(grid.path,Color(SpaceCompassAngularGridAppearance.liveShadowArgb),
                style=Stroke(SpaceCompassAngularGridAppearance.liveShadowDp.dp.toPx()))
            drawPath(grid.path,Color(SpaceCompassAngularGridAppearance.liveLineArgb),
                style=Stroke(SpaceCompassAngularGridAppearance.liveLineDp.dp.toPx()))
        }
    }
}
