package me.mondiversi.spacecompass

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/** Keep physical speed and its c-equivalent on one shared baseline, including large fonts. */
@Composable
internal fun SpaceCompassTravelSpeedReadout(name: String, speed: String, lightEquivalent: String?,
    primary: Color, secondary: Color) {
    val inherited = LocalTextStyle.current
    val nameStyle = inherited.merge(TextStyle(fontSize = 13.sp, lineHeight = 17.sp, fontWeight = FontWeight.Medium))
    val speedStyle = inherited.merge(TextStyle(fontSize = 12.sp, lineHeight = 16.sp,
        textDirection = TextDirection.ContentOrLtr))
    val lightStyle = inherited.merge(TextStyle(fontSize = 11.sp, lineHeight = 14.sp,
        textDirection = TextDirection.ContentOrLtr))
    val measurer = rememberTextMeasurer()
    val density = LocalDensity.current
    val gap = 8.dp
    BoxWithConstraints(Modifier.fillMaxWidth()) {
        val nameWidth = measurer.measure(name, nameStyle, softWrap = false).size.width
        val speedWidth = measurer.measure(speed, speedStyle, softWrap = false).size.width
        val lightWidth = lightEquivalent?.let { measurer.measure(it, lightStyle, softWrap = false).size.width } ?: 0
        val available = with(density) { maxWidth.toPx() }
        val gapPx = with(density) { gap.toPx() }
        val valuesWidth = speedWidth + if (lightEquivalent != null) gapPx + lightWidth else 0f
        val values: @Composable (Modifier) -> Unit = { placement ->
            Row(placement, verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(gap, Alignment.End)) {
                Text(speed, Modifier.alignByBaseline().testTag("travel-speed-value"), color = primary,
                    style = speedStyle, maxLines = 1, softWrap = false)
                lightEquivalent?.let { Text(it, Modifier.alignByBaseline().testTag("travel-speed-light-equivalent"),
                    color = secondary, style = lightStyle, maxLines = 1, softWrap = false) }
            }
        }
        if (nameWidth + gapPx + valuesWidth <= available) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(gap)) {
                Text(name, Modifier.weight(1f).testTag("travel-speed-name"), color = primary, style = nameStyle)
                values(Modifier)
            }
        } else {
            // Move the mode name above the values instead of splitting km/s and c onto separate lines.
            Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(name, Modifier.testTag("travel-speed-name"), color = primary, style = nameStyle)
                values(if (valuesWidth > available) Modifier.fillMaxWidth().horizontalScroll(rememberScrollState())
                    else Modifier.fillMaxWidth())
            }
        }
    }
}
