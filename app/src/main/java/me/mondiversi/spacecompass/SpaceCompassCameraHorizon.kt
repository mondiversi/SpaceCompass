package me.mondiversi.spacecompass

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp

/** Only the gravity horizon is added to the real scene: no virtual terrain/weather/grid. */
@Composable
internal fun SpaceCompassCameraHorizon(orientation: SpaceCompassSunOrientation?, frame: SpaceCompassSunSceneFrame?,
    perspective: SpaceCompassPerspective?, modifier: Modifier) {
    Canvas(modifier.testTag("camera-horizon")) {
        if (orientation == null || frame == null || perspective == null) return@Canvas
        val horizon = projectSpaceCompassSunGround(orientation, frame, size.width.toDouble(), size.height.toDouble(), perspective)?.horizon
        if (horizon?.size != 2) return@Canvas
        val start = Offset(horizon[0].x.toFloat(), horizon[0].y.toFloat())
        val end = Offset(horizon[1].x.toFloat(), horizon[1].y.toFloat())
        drawLine(Color.Black.copy(alpha = .55f), start, end, 3.dp.toPx(), StrokeCap.Round)
        drawLine(Color.White.copy(alpha = .85f), start, end, 1.dp.toPx(), StrokeCap.Round)
    }
}
