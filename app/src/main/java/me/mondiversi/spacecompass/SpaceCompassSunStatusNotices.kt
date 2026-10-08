package me.mondiversi.spacecompass

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.unit.constrainWidth
import androidx.compose.ui.unit.constrainHeight
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign

/** Data/location status takes precedence over secondary notices until it clears. */
@Composable
internal fun SpaceCompassSunStatusNotices(
    compassWarning: String?, message: String?, actionLabel: String?, onAction: () -> Unit,
    primaryText: Color, secondaryText: Color, backgroundColor: Color, modifier: Modifier = Modifier,
    simulationLabel: String? = null, onSimulation: () -> Unit = {},
    compassCalibrationRequired: Boolean = false
) {
    // Keep the retry/loading status exclusive without changing the underlying compass state.
    val hasStatus = message != null || actionLabel != null
    val shape = RoundedCornerShape(14.dp)
    val noticePadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
    val noticeBackground = backgroundColor.copy(alpha = 0.90f)
    // Scenario and calibration warnings share a theme-aware orange; reduced precision stays neutral.
    val warningOrange = if (backgroundColor.luminance() < .4f) Color(0xFFFF8F1F) else Color(0xFFAB3D00)
    SpaceCompassNoticesByWidth(modifier.widthIn(max = 280.dp)) {
        if (!hasStatus && simulationLabel != null) {
            Text(simulationLabel, color = warningOrange, fontSize = 10.sp, lineHeight = 13.sp,
                modifier = Modifier.testTag("observer-simulation-banner")
                    .background(noticeBackground, shape).clickable(role = Role.Button, onClick = onSimulation)
                    .padding(noticePadding))
        }
        if (!hasStatus && compassWarning != null) Text(compassWarning,
            color = if (compassCalibrationRequired) warningOrange else secondaryText, fontSize = 10.sp,
            lineHeight = 13.sp, modifier = Modifier.background(noticeBackground, shape)
                .padding(noticePadding).testTag("celestial-compass-warning")
                .semantics { liveRegion = LiveRegionMode.Polite })
        if (hasStatus) Row(
            Modifier.width(IntrinsicSize.Max).testTag("sun-finder-status-island")
                .semantics { liveRegion = LiveRegionMode.Polite }.background(noticeBackground, shape)
                .padding(noticePadding),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            if (message != null) Text(message, Modifier.weight(1f, fill = false), color = primaryText,
                fontSize = 10.sp, lineHeight = 13.sp)
            // The compact label adds no padding/layout height of its own. Foundation
            // expands its clickable/touch bounds to 48 dp without enlarging the island.
            if (actionLabel != null) Text(actionLabel,
                Modifier.widthIn(min = 48.dp).clickable(role = Role.Button, onClick = onAction)
                    .spaceCompassAccessibleAction(label = actionLabel, onClick = onAction)
                    .testTag("sun-finder-status-action"),
                color = MaterialTheme.colorScheme.primary, fontSize = 10.sp, lineHeight = 13.sp,
                fontWeight = FontWeight.Medium, textAlign = TextAlign.Center)
        }
    }
}

/** Measure once, then place wider islands first in this same pass; ties preserve source order. */
@Composable
private fun SpaceCompassNoticesByWidth(modifier: Modifier, content: @Composable () -> Unit) {
    Layout(content = content, modifier = modifier) { measurables, constraints ->
        val children = measurables.map { it.measure(constraints.copy(minWidth = 0, minHeight = 0)) }
        val ordered = children.sortedByDescending { it.width }
        val gap = 4.dp.roundToPx()
        val width = constraints.constrainWidth(children.maxOfOrNull { it.width } ?: 0)
        val height = constraints.constrainHeight(children.sumOf { it.height } + gap * (children.size - 1).coerceAtLeast(0))
        layout(width, height) {
            var top = 0
            ordered.forEach { child ->
                child.placeRelative(0, top)
                top += child.height + gap
            }
        }
    }
}
