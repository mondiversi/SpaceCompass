package me.mondiversi.spacecompass

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp

/** Preserve the original model-mode capsule, using the shared accessible two-icon switch. */
@Composable
internal fun SpaceCompassCelestialViewModeSwitch(rotating: Boolean, currentLabel: String,
    rotationLabel: String, onModeChange: (Boolean) -> Unit, modifier: Modifier = Modifier) {
    SpaceCompassTwoIconSwitch(rotating, currentLabel, rotationLabel, onModeChange, modifier,
        prefix = "celestial-view", firstTag = "celestial-view-current", secondTag = "celestial-view-rotation",
        firstIcon = { color -> SpaceCompassPasswordVisibilityIcon(true, Modifier.size(18.dp), color) },
        secondIcon = { color ->
            Canvas(Modifier.size(18.dp)) {
                val inset = 2.dp.toPx()
                drawArc(color, -45f, 285f, false, Offset(inset, inset),
                    Size(size.width - 2 * inset, size.height - 2 * inset),
                    style = Stroke(1.6.dp.toPx(), cap = StrokeCap.Round))
                val tip = Offset(size.width - inset, size.height * .30f)
                drawLine(color, tip - Offset(4.dp.toPx(), 0f), tip,
                    1.6.dp.toPx(), StrokeCap.Round)
                drawLine(color, tip + Offset(0f, 4.dp.toPx()), tip,
                    1.6.dp.toPx(), StrokeCap.Round)
            }
        })
}
