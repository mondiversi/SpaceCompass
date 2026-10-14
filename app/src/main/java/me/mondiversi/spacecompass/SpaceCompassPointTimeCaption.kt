package me.mondiversi.spacecompass

import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import java.util.Locale

/** Clock and countdown remain visible together in every live point bubble. */
@Composable
internal fun SpaceCompassPointTimeCaption(moment: String, timeMs: Long, nowMs: Long,
    color: Color, fontSize: TextUnit, lineHeight: TextUnit, modifier: Modifier = Modifier) {
    val locale = if (LocalSpaceCompassNumericFormat.current == SpaceCompassNumericFormat.SYSTEM)
        LocalSpaceCompassDeviceLocale.current else Locale.ROOT
    val countdown = formatSpaceCompassPointCountdown(timeMs, nowMs, locale)
    Row(modifier.semantics(mergeDescendants = true) {}, verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(moment, Modifier.weight(1f).testTag("celestial-point-clock"),
            color = color, fontSize = fontSize, lineHeight = lineHeight, maxLines = 1,
            textAlign = TextAlign.Start, style = TextStyle(textDirection = TextDirection.Ltr),
            overflow = TextOverflow.Ellipsis)
        Text(countdown, Modifier.testTag("celestial-point-countdown"),
            color = color, fontSize = fontSize, lineHeight = lineHeight, maxLines = 1,
            textAlign = TextAlign.End, style = TextStyle(textDirection = TextDirection.Ltr, fontFeatureSettings = "tnum"))
    }
}
