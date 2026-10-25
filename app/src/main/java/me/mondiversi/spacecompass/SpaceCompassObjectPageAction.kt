package me.mondiversi.spacecompass

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource

/** One ordinary title action opens the other object page without reserving a switch track. */
@Composable
internal fun SpaceCompassObjectPageAction(pathSelected: Boolean, pathAvailable: Boolean,
    enabled: Boolean, onPageChange: (Boolean) -> Unit) {
    val glyph = if (pathSelected) SpaceCompassObjectHeaderGlyph.OBSERVATORY else SpaceCompassObjectHeaderGlyph.DAILY_PATH
    val label = stringResource(if (pathSelected) R.string.celestial_details_title else R.string.celestial_daily_path)
    val ink = spaceCompassExportIconInkSize(SpaceCompassTitleActionIconStrokeWidth)
    SpaceCompassTitleActionButton(label, { onPageChange(!pathSelected) },
        Modifier.testTag(if (pathSelected) "object-page-observatory" else "sun-path-open"),
        enabled = enabled && (pathSelected || pathAvailable)) {
        // Match Export's painted bounds, not the full canvas used by the island glyphs.
        Box(Modifier.size(SpaceCompassTitleActionIconSize), contentAlignment = Alignment.Center) {
            SpaceCompassObjectHeaderIcon(glyph, Modifier.size(ink.width, ink.height),
                strokeWidth = SpaceCompassTitleActionIconStrokeWidth)
        }
    }
}
