package me.mondiversi.spacecompass

import android.content.res.Configuration
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.staggeredgrid.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp

/** At most two readable cards; portrait and narrow/accessibility-sized windows remain one column. */
internal fun spaceCompassIslandColumnCount(landscape: Boolean, contentWidthDp: Float, fontScale: Float): Int {
    if (!landscape || !contentWidthDp.isFinite()) return 1
    val readableFontScale = fontScale.takeIf { it.isFinite() && it > 0f } ?: 1f
    val minimumCardWidth = 280f * (readableFontScale / 1.3f).coerceAtLeast(1f)
    return if (contentWidthDp >= minimumCardWidth * 2 + SpaceCompassSettingsIslandGap.value) 2 else 1
}

/** Pack natural-height islands into the shortest column; keyed content survives reflow. */
@Composable
internal fun SpaceCompassIslandGrid(modifier: Modifier = Modifier, content: LazyStaggeredGridScope.() -> Unit) {
    val configuration = LocalConfiguration.current
    val density = LocalDensity.current
    val direction = LocalLayoutDirection.current
    BoxWithConstraints(modifier.fillMaxSize()) {
        val contentWidth = maxWidth - spaceCompassPageContentPadding.calculateLeftPadding(direction) -
            spaceCompassPageContentPadding.calculateRightPadding(direction)
        val columns = spaceCompassIslandColumnCount(
            configuration.orientation == Configuration.ORIENTATION_LANDSCAPE,
            contentWidth.value, density.fontScale)
        LazyVerticalStaggeredGrid(columns = StaggeredGridCells.Fixed(columns),
            modifier = Modifier.fillMaxSize().testTag("island-grid"),
            contentPadding = spaceCompassPageContentPadding,
            horizontalArrangement = Arrangement.spacedBy(SpaceCompassSettingsIslandGap),
            verticalItemSpacing = SpaceCompassSettingsIslandGap, content = content)
    }
}
