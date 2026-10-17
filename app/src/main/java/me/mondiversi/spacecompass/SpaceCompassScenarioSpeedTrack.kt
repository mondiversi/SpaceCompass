package me.mondiversi.spacecompass

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.unit.dp
import kotlin.math.max
import kotlin.math.min

/** Slider owns input/semantics; only the track is drawn from the central pause origin. */
@Composable
internal fun SpaceCompassScenarioSpeedTrack(index: Float) {
    val scheme = MaterialTheme.colorScheme
    Canvas(Modifier.fillMaxWidth().height(20.dp)) {
        val last = spaceCompassScenarioRates.lastIndex.toFloat()
        val fraction = (index / last).coerceIn(0f, 1f)
        val middle = size.width / 2f
        val end = size.width * fraction
        val y = size.height / 2f
        drawLine(scheme.primary.copy(alpha = .24f), Offset(0f, y), Offset(size.width, y),
            4.dp.toPx(), StrokeCap.Round)
        drawLine(scheme.primary, Offset(middle, y), Offset(end, y), 4.dp.toPx(), StrokeCap.Round)
        spaceCompassScenarioRates.indices.forEach { tick ->
            val x = size.width * tick / last
            val active = x >= min(middle, end) && x <= max(middle, end)
            drawCircle(if (active) scheme.onPrimary else scheme.primary.copy(alpha = .65f),
                1.5.dp.toPx(), Offset(x, y))
        }
        drawLine(scheme.onSurface.copy(alpha = .55f), Offset(middle, y - 6.dp.toPx()),
            Offset(middle, y + 6.dp.toPx()), 2.dp.toPx(), StrokeCap.Round)
    }
}
