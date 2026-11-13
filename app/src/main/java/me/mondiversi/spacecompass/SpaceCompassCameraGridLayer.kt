package me.mondiversi.spacecompass

import android.graphics.Paint
import android.graphics.RectF
import androidx.compose.foundation.Canvas
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.sp
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.conflate
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext

private data class SpaceCompassLiveCameraGrid(val size: IntSize,val path: Path,val ticks: List<SpaceCompassCameraGridTick>)

/** Decorative only: one worker owns reusable projection buffers; drawing keeps an immutable frame. */
@Composable
internal fun SpaceCompassCameraGridLayer(orientation: SpaceCompassSunOrientation?,
    lens: SpaceCompassPerspective?,opticalFrame: SpaceCompassSunSceneFrame?,modifier: Modifier,tag: String="camera-angular-grid",showNumbers: Boolean = false) {
    if(orientation==null||lens==null||opticalFrame==null) return
    var viewport by remember { mutableStateOf(IntSize.Zero) }
    val currentOrientation by rememberUpdatedState(orientation)
    val numeric=LocalSpaceCompassNumericFormat.current
    val deviceLocale=LocalSpaceCompassDeviceLocale.current
    // Each immutable grid angle is formatted once, rather than rebuilding number formatters per frame.
    val numberLabels=remember(numeric,deviceLocale) { mutableMapOf<Double,String>() }
    val textSize=with(LocalDensity.current) { 9.sp.toPx() }
    val textPaint=remember(textSize) { Paint(Paint.ANTI_ALIAS_FLAG).apply {
        this.textSize=textSize;color=0xddffffff.toInt();textAlign=Paint.Align.CENTER
        setShadowLayer(textSize*.18f,0f,textSize*.06f,0xff101418.toInt())
    } }
    val drawing = produceState<SpaceCompassLiveCameraGrid?>(null,lens,opticalFrame,viewport,showNumbers) {
        val requestedSize=viewport
        if(requestedSize.width<=0||requestedSize.height<=0) { value=null;return@produceState }
        val width=requestedSize.width.toDouble();val height=requestedSize.height.toDouble()
        val perspective=requireNotNull(spaceCompassSceneFramePerspective(width,height,opticalFrame,lens))
        val projector=withContext(Dispatchers.Default) { SpaceCompassLiveGridProjector(width,height,perspective) }
        // Conflate pending sensor samples, without cancelling a calculation during a steady pan.
        snapshotFlow { currentOrientation }.conflate().collect { pointing ->
            value=withContext(Dispatchers.Default) {
                val path=Path()
                val numbers=if(showNumbers) SpaceCompassLiveGridNumbers(opticalFrame) else null
                projector.project(pointing,object : SpaceCompassLiveGridSink {
                    override fun beginCurve(axis: SpaceCompassCameraGridAxis,degrees: Double) { numbers?.beginCurve(axis,degrees) }
                    override fun moveTo(x: Double,y: Double) { path.moveTo(x.toFloat(),y.toFloat());numbers?.moveTo(x,y) }
                    override fun lineTo(x: Double,y: Double) { path.lineTo(x.toFloat(),y.toFloat());numbers?.lineTo(x,y) }
                })
                ensureActive()
                SpaceCompassLiveCameraGrid(requestedSize,path,numbers?.ticks.orEmpty())
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
            if(showNumbers) drawIntoCanvas { canvas ->
                val occupied=mutableListOf<RectF>()
                for(tick in grid.ticks) {
                    val label=numberLabels.getOrPut(tick.degrees) {
                        formatSpaceCompassNumber(tick.degrees,0,numeric,grouping=false,systemLocale=deviceLocale)+"°"
                    }
                    val half=textPaint.measureText(label)/2
                    if(half*2+4>=size.width||textSize*2>=size.height) continue
                    val horizontal=tick.edge==SpaceCompassCameraGridEdge.TOP||tick.edge==SpaceCompassCameraGridEdge.BOTTOM
                    val x=(if(horizontal) tick.point.x.toFloat() else if(tick.edge==SpaceCompassCameraGridEdge.LEFT)
                        tick.point.x.toFloat()+half+textSize*.4f else tick.point.x.toFloat()-half-textSize*.4f)
                        .coerceIn(half+2,size.width-half-2)
                    val centerY=(if(!horizontal) tick.point.y.toFloat() else tick.point.y.toFloat()+
                        if(tick.edge==SpaceCompassCameraGridEdge.TOP) textSize else -textSize)
                        .coerceIn(textSize,size.height-textSize)
                    val baseline=centerY-(textPaint.fontMetrics.ascent+textPaint.fontMetrics.descent)/2
                    val bounds=RectF(x-half-3,centerY-textSize*.65f,x+half+3,centerY+textSize*.65f)
                    if(occupied.any { RectF.intersects(it,bounds) }) continue
                    canvas.nativeCanvas.drawText(label,x,baseline,textPaint);occupied+=bounds
                }
            }
        }
    }
}
