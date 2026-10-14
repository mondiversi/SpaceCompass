package me.mondiversi.spacecompass

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.time.ZoneId

/** Pinned and focused points share a clock/countdown row and an optional event heading. */
@Composable
internal fun SpaceCompassCelestialBadgeContent(timeMs: Long, nowMs: Long, zone: ZoneId,
    position: SpaceCompassSunPosition, primaryText: Color, secondaryText: Color, backgroundColor: Color,
    pointName: String, pinned: Boolean = false, modifier: Modifier = Modifier) {
    val locale = LocalConfiguration.current.locales[0]
    val numeric = LocalSpaceCompassNumericFormat.current
    val deviceLocale = LocalSpaceCompassDeviceLocale.current
    val moment = formatSpaceCompassCelestialMoment(timeMs, nowMs, zone,
        resolveSpaceCompassTimeFormat(LocalContext.current, LocalSpaceCompassTimeFormat.current),
        LocalSpaceCompassDateFormat.current, locale, deviceLocale, numeric)
    val shape = RoundedCornerShape(10.dp)
    Column(modifier.width(IntrinsicSize.Max).testTag(if (pinned) "celestial-pinned-badge" else "celestial-time-badge")
        .clip(shape).background(backgroundColor.copy(alpha = 0.86f))
        .border(if (pinned) 1.dp else 0.5.dp,
            if (pinned) MaterialTheme.colorScheme.primary else secondaryText.copy(alpha = 0.3f), shape)
        .padding(horizontal = 8.dp, vertical = 4.dp)) {
        if (pointName.isNotBlank()) {
            Text(pointName, Modifier.testTag("celestial-badge-event"),
                color = primaryText, fontSize = 11.sp, lineHeight = 14.sp)
        }
        SpaceCompassPointTimeCaption(moment, timeMs, nowMs, primaryText,
            if (pinned) 10.sp else 11.sp, if (pinned) 12.sp else 14.sp,
            Modifier.fillMaxWidth().testTag(if (pinned) "celestial-pinned-caption" else "celestial-badge-caption"))
        SpaceCompassCelestialPointAngles(position, primaryText, secondaryText,
            fontSize = 10.sp, lineHeight = 12.sp, tagPrefix = "celestial-badge")
    }
}
