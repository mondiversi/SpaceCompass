package me.mondiversi.spacecompass

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextOverflow
import java.time.ZoneId

/** The object heading, clock/countdown and two angle rows remain visible together. */
@Composable
internal fun SpaceCompassCelestialBadgeContent(timeMs: Long, nowMs: Long, zone: ZoneId,
    position: SpaceCompassSunPosition, primaryText: Color, secondaryText: Color, backgroundColor: Color,
    pointName: String, bodyName: String, pinned: Boolean = false, modifier: Modifier = Modifier) {
    val locale = LocalConfiguration.current.locales[0]
    val numeric = LocalSpaceCompassNumericFormat.current
    val deviceLocale = LocalSpaceCompassDeviceLocale.current
    val moment = formatSpaceCompassCelestialMoment(timeMs, nowMs, zone,
        resolveSpaceCompassTimeFormat(LocalContext.current, LocalSpaceCompassTimeFormat.current),
        LocalSpaceCompassDateFormat.current, locale, deviceLocale, numeric)
    val shape = RoundedCornerShape(10.dp)
    // Cap the entire caption at the requested reference heading, including its inner padding.
    // Measuring text also respects the device's font size instead of baking it into a dp constant.
    val textMeasurer = rememberTextMeasurer()
    val titleStyle = LocalTextStyle.current.copy(fontSize = 11.sp, lineHeight = 14.sp)
    val maxWidth = with(LocalDensity.current) {
        // Measure the three separate text nodes to preserve their individual pixel rounding.
        val nameWidth = textMeasurer.measure("Tesla Roadster (2018)",
            style = titleStyle.copy(fontWeight = FontWeight.Bold), softWrap = false).size.width
        val separatorWidth = textMeasurer.measure(" · ", style = titleStyle, softWrap = false).size.width
        val eventWidth = textMeasurer.measure("Tramonta", style = titleStyle, softWrap = false).size.width
        (nameWidth + separatorWidth + eventWidth).toDp()
    } + 16.dp
    Column(modifier.widthIn(max = maxWidth).width(IntrinsicSize.Max)
        .testTag(if (pinned) "celestial-pinned-badge" else "celestial-time-badge")
        .clip(shape).background(backgroundColor.copy(alpha = SPACE_COMPASS_POINT_CAPTION_ALPHA))
        .border(if (pinned) 1.dp else 0.5.dp,
            if (pinned) MaterialTheme.colorScheme.primary else secondaryText.copy(alpha = 0.3f), shape)
        .padding(horizontal = 8.dp, vertical = 4.dp)) {
        Row(Modifier.fillMaxWidth().testTag("celestial-badge-title"),
            verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
            Text(bodyName, Modifier.weight(1f, fill = false).testTag("celestial-badge-body"),
                color = primaryText, style = titleStyle, fontWeight = FontWeight.Bold)
            if (pointName.isNotBlank()) {
                Text(" · ", color = primaryText, style = titleStyle)
                Text(pointName, Modifier.testTag("celestial-badge-event"), color = primaryText,
                    style = titleStyle, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
        SpaceCompassPointTimeCaption(moment, timeMs, nowMs, primaryText,
            if (pinned) 10.sp else 11.sp, if (pinned) 12.sp else 14.sp,
            Modifier.fillMaxWidth().testTag(if (pinned) "celestial-pinned-caption" else "celestial-badge-caption"))
        SpaceCompassCelestialPointAngles(position, primaryText, secondaryText,
            fontSize = 10.sp, lineHeight = 12.sp, tagPrefix = "celestial-badge")
    }
}
