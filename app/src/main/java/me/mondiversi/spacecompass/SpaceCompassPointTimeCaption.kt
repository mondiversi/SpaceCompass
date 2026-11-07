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
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.unit.TextUnit
import java.util.Locale

/** Clock and countdown remain visible together in every live point bubble. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun SpaceCompassPointTimeCaption(moment: String, timeMs: Long, nowMs: Long,
    color: Color, fontSize: TextUnit, lineHeight: TextUnit, modifier: Modifier = Modifier) {
    val locale = if (LocalSpaceCompassNumericFormat.current == SpaceCompassNumericFormat.SYSTEM)
        LocalSpaceCompassDeviceLocale.current else Locale.ROOT
    val countdown = formatSpaceCompassPointCountdown(timeMs, nowMs, locale)
    FlowRow(modifier.semantics(mergeDescendants = true) {}) {
        Text(moment, Modifier.testTag("celestial-point-clock"),
            color = color, fontSize = fontSize, lineHeight = lineHeight,
            style = TextStyle(textDirection = TextDirection.Ltr))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(" · ", color = color, fontSize = fontSize, lineHeight = lineHeight)
            Text(countdown, Modifier.testTag("celestial-point-countdown"),
                color = color, fontSize = fontSize, lineHeight = lineHeight,
                style = TextStyle(textDirection = TextDirection.Ltr, fontFeatureSettings = "tnum"))
        }
    }
}
