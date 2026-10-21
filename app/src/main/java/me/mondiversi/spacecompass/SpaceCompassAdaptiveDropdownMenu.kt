package me.mondiversi.spacecompass

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MenuDefaults
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupPositionProvider
import androidx.compose.ui.window.PopupProperties

/** Dropdown windows must use the same adaptive scale as their anchor, including their chrome. */
@Composable
internal fun SpaceCompassAdaptiveDropdownMenu(
    expanded: Boolean,
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
    scrollState: ScrollState = rememberScrollState(),
    containerColor: Color = MenuDefaults.containerColor,
    shape: Shape = MenuDefaults.shape,
    tonalElevation: Dp = MenuDefaults.TonalElevation,
    scrollbarColor: Color = spaceCompassNeutralContentColor().copy(alpha = 0.45f),
    content: @Composable ColumnScope.() -> Unit
) {
    if (!expanded) return
    val dimming = LocalSpaceCompassDropdownDimming.current
    DisposableEffect(dimming) {
        dimming?.acquire()
        onDispose { dimming?.release() }
    }
    val density = LocalDensity.current
    val configuration = LocalConfiguration.current
    val direction = LocalLayoutDirection.current
    val margin = with(density) { 8.dp.roundToPx() }
    val position = remember(margin) { SpaceCompassDropdownPositionProvider(margin) }
    Popup(onDismissRequest = onDismissRequest, popupPositionProvider = position,
        properties = PopupProperties(focusable = true)) {
        CompositionLocalProvider(LocalDensity provides density, LocalConfiguration provides configuration,
            LocalLayoutDirection provides direction) {
            Surface(
                modifier = Modifier
                    .widthIn(max = (configuration.screenWidthDp.dp - 16.dp).coerceAtLeast(1.dp))
                    .heightIn(max = (configuration.screenHeightDp.dp - 48.dp).coerceAtLeast(1.dp)),
                shape = shape, color = containerColor,
                tonalElevation = tonalElevation, shadowElevation = MenuDefaults.ShadowElevation
            ) {
                Column(modifier.scrollbarOverlay(scrollState, scrollbarColor)
                    .padding(vertical = 8.dp).width(IntrinsicSize.Max)
                    .verticalScroll(scrollState), content = content)
            }
        }
    }
}

/** Prefer below/above the anchor, clamp to the visible window, and mirror the start edge in RTL. */
internal class SpaceCompassDropdownPositionProvider(private val margin: Int) : PopupPositionProvider {
    override fun calculatePosition(anchorBounds: IntRect, windowSize: IntSize,
        layoutDirection: LayoutDirection, popupContentSize: IntSize): IntOffset {
        val maxX = (windowSize.width - margin - popupContentSize.width).coerceAtLeast(margin)
        val maxY = (windowSize.height - margin - popupContentSize.height).coerceAtLeast(margin)
        val start = if (layoutDirection == LayoutDirection.Ltr) anchorBounds.left
            else anchorBounds.right - popupContentSize.width
        val end = if (layoutDirection == LayoutDirection.Ltr) anchorBounds.right - popupContentSize.width
            else anchorBounds.left
        val x = listOf(start, end).firstOrNull { it in margin..maxX } ?: start.coerceIn(margin, maxX)
        val y = listOf(anchorBounds.bottom, anchorBounds.top - popupContentSize.height)
            .firstOrNull { it in margin..maxY } ?: anchorBounds.bottom.coerceIn(margin, maxY)
        return IntOffset(x, y)
    }
}

/** Count open dropdowns so a dismissed/nested window cannot leave the app dimmed. */
private class SpaceCompassDropdownDimmingState {
    var windows by mutableIntStateOf(0)
        private set
    fun acquire() { windows++ }
    fun release() { windows = (windows - 1).coerceAtLeast(0) }
}

private val LocalSpaceCompassDropdownDimming =
    staticCompositionLocalOf<SpaceCompassDropdownDimmingState?> { null }

/** Popup windows stay above this draw-only scrim; outside taps and Back retain native dismissal. */
@Composable
internal fun SpaceCompassDropdownDimmingHost(content: @Composable BoxScope.() -> Unit) {
    val dimming = remember { SpaceCompassDropdownDimmingState() }
    val alpha = animateFloatAsState(if (dimming.windows > 0) .18f else 0f,
        animationSpec = tween(150), label = "dropdownBackgroundDim")
    CompositionLocalProvider(LocalSpaceCompassDropdownDimming provides dimming) {
        Box(Modifier.fillMaxSize()) {
            content()
            // Read animation state while drawing, without recomposing the live sky each frame.
            Box(Modifier.matchParentSize().drawBehind {
                if (alpha.value > 0f) drawRect(Color.Black.copy(alpha = alpha.value))
            })
        }
    }
}
