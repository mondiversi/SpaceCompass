package me.mondiversi.spacecompass

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.unit.sp

/** International compass letters stay constant; the radio row supplies translated accessible text. */
@Composable
internal fun SpaceCompassPanoramaCenterIcon(direction: SpaceCompassPanoramaCenter) {
    val tint=LocalContentColor.current
    val symbol=when(direction) {
        SpaceCompassPanoramaCenter.NORTH -> "N"
        SpaceCompassPanoramaCenter.EAST -> "E"
        SpaceCompassPanoramaCenter.SOUTH -> "S"
        SpaceCompassPanoramaCenter.WEST -> "W"
    }
    Box(Modifier.size(SpaceCompassMenuIconSize).clearAndSetSemantics {},contentAlignment=Alignment.Center) {
        Canvas(Modifier.matchParentSize()) {
            val stroke=SpaceCompassMenuIconStrokeWidth.toPx()
            drawCircle(tint,size.minDimension*.43f,style=Stroke(stroke))
            rotate(direction.azimuth.toFloat()) {
                fun point(x: Float,y: Float)=Offset(size.width*x,size.height*y)
                drawLine(tint,point(.5f,.08f),point(.5f,.22f),stroke,StrokeCap.Round)
                drawLine(tint,point(.44f,.15f),point(.5f,.08f),stroke,StrokeCap.Round)
                drawLine(tint,point(.56f,.15f),point(.5f,.08f),stroke,StrokeCap.Round)
            }
        }
        Text(symbol,color=tint,fontSize=10.sp,lineHeight=10.sp,fontWeight=FontWeight.Medium,
            style=TextStyle(platformStyle=PlatformTextStyle(includeFontPadding=false),textDirection=TextDirection.ContentOrLtr))
    }
}
