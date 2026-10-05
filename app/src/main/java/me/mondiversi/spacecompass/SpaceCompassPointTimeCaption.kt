package me.mondiversi.spacecompass

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.TextUnit
import java.util.Locale

/** Both surfaces share the same two-second interval and reserve only one text row. */
@Composable
internal fun SpaceCompassPointTimeCaption(pointName: String, moment: String, timeMs: Long, nowMs: Long,
    color: Color, fontSize: TextUnit, lineHeight: TextUnit, modifier: Modifier = Modifier) {
    val locale = if (LocalSpaceCompassNumericFormat.current == SpaceCompassNumericFormat.SYSTEM)
        LocalSpaceCompassDeviceLocale.current else Locale.ROOT
    val countdown = formatSpaceCompassPointCountdown(timeMs, nowMs, locale)
    val showCountdown = nowMs / 2000L % 2L == 1L
    val progress by animateFloatAsState(if (showCountdown) 1f else 0f,
        tween(if (android.animation.ValueAnimator.areAnimatorsEnabled()) 300 else 0), label = "point-time-scroll")
    Row(modifier.semantics(mergeDescendants = true) {}, verticalAlignment = Alignment.CenterVertically) {
        Text("$pointName · ", color = color, fontSize = fontSize, fontWeight = FontWeight.Normal, lineHeight = lineHeight,
            maxLines = 1, overflow = TextOverflow.Ellipsis)
        // Measuring both alternatives stabilizes width and keeps the angles aligned at the right.
        Box(Modifier.weight(1f).clipToBounds()) {
            Text(moment, Modifier.fillMaxWidth().graphicsLayer {
                alpha = 1f - progress
                translationY = -progress * size.height
            }.then(if (showCountdown) Modifier.clearAndSetSemantics {} else Modifier),
                color = color, fontSize = fontSize, fontWeight = FontWeight.Normal, lineHeight = lineHeight, maxLines = 1,
                textAlign = TextAlign.End, overflow = TextOverflow.Ellipsis)
            Text(countdown, Modifier.fillMaxWidth().graphicsLayer {
                alpha = progress
                translationY = (1f - progress) * size.height
            }.then(if (showCountdown) Modifier else Modifier.clearAndSetSemantics {}),
                color = color, fontSize = fontSize, fontWeight = FontWeight.Normal, lineHeight = lineHeight, maxLines = 1,
                textAlign = TextAlign.End, style = TextStyle(textDirection = TextDirection.Ltr, fontFeatureSettings = "tnum"),
                overflow = TextOverflow.Ellipsis)
        }
    }
}
