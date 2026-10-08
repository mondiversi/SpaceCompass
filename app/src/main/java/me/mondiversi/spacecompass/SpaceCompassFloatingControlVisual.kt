package me.mondiversi.spacecompass

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalViewConfiguration
import androidx.compose.ui.platform.ViewConfiguration
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import kotlin.math.roundToInt

// Reduce the preceding 88% controls by ten percent, including their touch bounds.
private const val SPACE_COMPASS_FLOATING_CONTROL_SCALE = .792f
internal val spaceCompassFloatingControlSize = 48.dp * SPACE_COMPASS_FLOATING_CONTROL_SCALE

/** Scale the complete control, including measured bounds, semantics and pointer coordinates. */
internal fun Modifier.spaceCompassFloatingControlVisual(): Modifier = layout { measurable, constraints ->
    val placeable = measurable.measure(constraints)
    layout((placeable.width * SPACE_COMPASS_FLOATING_CONTROL_SCALE).roundToInt(),
        (placeable.height * SPACE_COMPASS_FLOATING_CONTROL_SCALE).roundToInt()) {
        // Absolute placement keeps a single control centered in its own bounds in both LTR and RTL.
        placeable.placeWithLayer(0, 0) {
            scaleX = SPACE_COMPASS_FLOATING_CONTROL_SCALE
            scaleY = SPACE_COMPASS_FLOATING_CONTROL_SCALE
            transformOrigin = TransformOrigin(0f, 0f)
        }
    }
}

/** Disable automatic hit expansion only for the floating actions whose targets the user reduced. */
@Composable
internal fun SpaceCompassFloatingControlHitRegion(content: @Composable () -> Unit) {
    val configuration = LocalViewConfiguration.current
    val exactBounds = remember(configuration) {
        object : ViewConfiguration by configuration {
            override val minimumTouchTargetSize = DpSize.Zero
        }
    }
    CompositionLocalProvider(LocalViewConfiguration provides exactBounds, content = content)
}
