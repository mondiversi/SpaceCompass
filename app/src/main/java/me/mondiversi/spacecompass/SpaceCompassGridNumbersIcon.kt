package me.mondiversi.spacecompass

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.material3.LocalContentColor
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke

/** A quiet 123 glyph uses the same size and line weight as the other popup icons. */
@Composable
internal fun SpaceCompassGridNumbersIcon() {
    val color=LocalContentColor.current
    Canvas(Modifier.size(SpaceCompassMenuIconSize)) {
        val u=size.minDimension/24f
        val stroke=Stroke(SpaceCompassMenuIconStrokeWidth.toPx(),cap=StrokeCap.Round,join=StrokeJoin.Round)
        fun digit(vararg p: Pair<Float,Float>) {
            drawPath(Path().apply { moveTo(p[0].first*u,p[0].second*u)
                p.drop(1).forEach { lineTo(it.first*u,it.second*u) } },color,style=stroke)
        }
        digit(2f to 8f,4f to 6f,4f to 18f)
        digit(8f to 7f,12f to 7f,13f to 10f,8f to 18f,13f to 18f)
        digit(17f to 7f,22f to 7f,22f to 18f,17f to 18f)
        digit(18f to 12.5f,22f to 12.5f)
    }
}
