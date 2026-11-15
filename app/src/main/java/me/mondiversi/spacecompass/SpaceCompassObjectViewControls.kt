package me.mondiversi.spacecompass

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp

@Composable
internal fun SpaceCompassObjectModeIcon(tint: Color,size: Dp,stroke: Dp) {
    Canvas(Modifier.size(size)) {
        val s=this.size.minDimension;val edge=stroke.toPx()
        drawCircle(tint,s*.23f,Offset(s*.36f,s*.50f),style=Stroke(edge))
        drawOval(tint,Offset(s*.25f,s*.27f),Size(s*.22f,s*.46f),style=Stroke(edge))
        drawLine(tint,Offset(s*.13f,s*.50f),Offset(s*.59f,s*.50f),edge)
        val p=Path().apply { moveTo(s*.66f,s*.50f);quadraticTo(s*.80f,s*.27f,s*.94f,s*.50f);quadraticTo(s*.80f,s*.73f,s*.66f,s*.50f) }
        drawPath(p,tint,style=Stroke(edge));drawCircle(tint,s*.04f,Offset(s*.80f,s*.50f))
    }
}
