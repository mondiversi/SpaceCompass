package me.mondiversi.spacecompass

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalViewConfiguration
import androidx.compose.ui.platform.ViewConfiguration
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlin.math.roundToInt

// Reduce the preceding 88% controls by ten percent, including their touch bounds.
private const val SPACE_COMPASS_FLOATING_CONTROL_SCALE = .792f
internal val spaceCompassFloatingControlBaseSize = 48.dp * SPACE_COMPASS_FLOATING_CONTROL_SCALE
internal const val SPACE_COMPASS_COMPACT_CONTROL_MULTIPLIER = .9f
internal val LocalSpaceCompassFloatingControlMultiplier = staticCompositionLocalOf { 1f }
internal val spaceCompassFloatingControlSize: Dp
    @Composable get() = spaceCompassFloatingControlBaseSize * LocalSpaceCompassFloatingControlMultiplier.current

// Match the zoom glyph before the shared control transform. Vector icons use
// 2.04 viewport units at 26 dp (24-unit viewport) for this same visible width.
internal val SpaceCompassFloatingIconStrokeWidth = 2.21.dp

/** Scope size changes to a host while keeping drawing, bounds and hit coordinates together. */
@Composable
internal fun Modifier.spaceCompassFloatingControlVisual(): Modifier {
    val scale = SPACE_COMPASS_FLOATING_CONTROL_SCALE * LocalSpaceCompassFloatingControlMultiplier.current
    return layout { measurable, constraints ->
        val placeable = measurable.measure(constraints)
        layout((placeable.width * scale).roundToInt(), (placeable.height * scale).roundToInt()) {
            placeable.placeWithLayer(0, 0) {
                scaleX = scale
                scaleY = scale
                transformOrigin = TransformOrigin(0f, 0f)
            }
        }
    }
}

/** Disable automatic hit expansion only for controls whose targets the user explicitly reduced. */
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
