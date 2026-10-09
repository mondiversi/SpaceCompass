package me.mondiversi.spacecompass

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withLink
import androidx.compose.ui.text.font.FontWeight

/** Data/location status takes precedence over secondary notices until it clears. */
@Composable
internal fun SpaceCompassSunStatusNotices(
    compassWarning: String?, message: String?, actionLabel: String?, onAction: () -> Unit,
    backgroundColor: Color, modifier: Modifier = Modifier,
    simulationLabel: String? = null, onSimulation: () -> Unit = {},
    compassWarningAttention: Boolean = false, messageAttention: Boolean = false,
    primaryText: Color = MaterialTheme.colorScheme.onSurface
) {
    val hasStatus = message != null || actionLabel != null
    val shape = RoundedCornerShape(14.dp)
    val noticePadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
    val noticeBackground = backgroundColor.copy(alpha = 0.90f)
    val warningOrange = if (backgroundColor.luminance() < .4f) Color(0xFFFF8F1F) else Color(0xFFAB3D00)
    val linkColor = MaterialTheme.colorScheme.primary
    SpaceCompassNoticesByWidth(modifier.widthIn(max = 280.dp)) {
        if (!hasStatus && simulationLabel != null) {
            Text(simulationLabel, color = warningOrange, fontSize = 10.sp, lineHeight = 13.sp,
                modifier = Modifier.testTag("observer-simulation-banner")
                    .background(noticeBackground, shape).clickable(role = Role.Button, onClick = onSimulation)
                    .padding(noticePadding))
        }
        if (!hasStatus && compassWarning != null) Text(spaceCompassStatusSentence(compassWarning),
            color = if (compassWarningAttention) warningOrange else primaryText, fontSize = 10.sp,
            lineHeight = 13.sp, modifier = Modifier.background(noticeBackground, shape)
                .padding(noticePadding).testTag("celestial-compass-warning")
                .semantics { liveRegion = LiveRegionMode.Polite })
        if (hasStatus) {
            // One paragraph lets the action follow the sentence, including when text wraps or is RTL.
            // LinkAnnotation gives the action its own native focus/click target without making the
            // surrounding message clickable or allocating an empty right-hand column.
            val text = buildAnnotatedString {
                if (!message.isNullOrBlank()) {
                    append(spaceCompassStatusSentence(message))
                    if (!actionLabel.isNullOrBlank()) append(" ")
                }
                if (!actionLabel.isNullOrBlank()) withLink(LinkAnnotation.Clickable(
                    tag = "status-action",
                    styles = TextLinkStyles(style = SpanStyle(color = linkColor, fontWeight = FontWeight.Medium)),
                    linkInteractionListener = { onAction() }
                )) { append(actionLabel.trimEnd().trimEnd('.', '。', '।', '۔', '…')) }
            }
            Text(text, color = if (messageAttention) warningOrange else primaryText,
                fontSize = 10.sp, lineHeight = 13.sp,
                modifier = Modifier.testTag("sun-finder-status-island")
                    .semantics { liveRegion = LiveRegionMode.Polite }
                    .background(noticeBackground, shape).padding(noticePadding))
        }
    }
}

/** Keep one sentence terminator, respecting localized full stops; actions stay unpunctuated. */
private fun spaceCompassStatusSentence(value: String): String {
    val text = value.trimEnd()
    val stop = text.lastOrNull()?.takeIf { it in "。।۔" } ?: '.'
    val sentence = text.trimEnd('.', '。', '।', '۔', '…').trimEnd()
    return if (sentence.isEmpty()) sentence else "$sentence$stop"
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
