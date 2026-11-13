package me.mondiversi.spacecompass

import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.Dp

// Keep the legacy preference name so existing camera-grid choices migrate without a reset.
internal const val SPACE_COMPASS_MAIN_GRID_KEY="camera_grid_visible"
internal const val SPACE_COMPASS_MAIN_GRID_DEFAULT=true
internal const val SPACE_COMPASS_GRID_NUMBERS_KEY="grid_numbers_visible"
internal const val SPACE_COMPASS_GRID_NUMBERS_DEFAULT=false

@Composable
internal fun SpaceCompassAngularGridIcon(tint: Color, requestedSize: Dp = 26.dp, strokeWidth: Dp = SpaceCompassFloatingIconStrokeWidth) {
    val visualStroke=strokeWidth*LocalSpaceCompassFloatingStrokeScale.current
    SpaceCompassIconCanvas(tint,Modifier.size(requestedSize)) { ink ->
        val s=size.minDimension;val stroke=Stroke(visualStroke.toPx(),cap=StrokeCap.Round)
        drawCircle(ink,s*.38f,style=stroke)
        drawOval(ink,Offset(s*.30f,s*.12f),Size(s*.40f,s*.76f),style=stroke)
        drawLine(ink,Offset(s*.12f,s*.50f),Offset(s*.88f,s*.50f),stroke.width,StrokeCap.Round)
    }
}
