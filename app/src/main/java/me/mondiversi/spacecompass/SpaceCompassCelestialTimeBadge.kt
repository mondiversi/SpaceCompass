package me.mondiversi.spacecompass

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.time.ZoneId
import kotlin.math.roundToInt

/** Non-interactive caption: taps still reach the trajectory below it. */
@Composable
internal fun SpaceCompassCelestialTimeBadge(
    timeMs: Long, nowMs: Long, zone: ZoneId,
    position: SpaceCompassSunPosition, orientation: SpaceCompassSunOrientation?,
    primaryText: Color, secondaryText: Color, backgroundColor: Color,
    pointName: String, body: SpaceCompassCelestialBody,
    excluded: List<SpaceCompassSunSceneFrame> = emptyList()
) {
    val locale = LocalConfiguration.current.locales[0]
    val timeFormat = resolveSpaceCompassTimeFormat(LocalContext.current, LocalSpaceCompassTimeFormat.current)
    val dateFormat = LocalSpaceCompassDateFormat.current
    val deviceLocale = LocalSpaceCompassDeviceLocale.current
    val numeric = LocalSpaceCompassNumericFormat.current
    val moment = remember(timeMs, nowMs, zone, timeFormat, dateFormat, locale, deviceLocale, numeric) {
        formatSpaceCompassCelestialMoment(timeMs, nowMs, zone, timeFormat, dateFormat, locale, deviceLocale, numeric)
    }
    val shape = RoundedCornerShape(10.dp)
    Layout(content = {
        Column(Modifier.width(IntrinsicSize.Max).testTag("celestial-time-badge").clip(shape)
                .background(backgroundColor.copy(alpha = 0.86f))
                .border(0.5.dp, secondaryText.copy(alpha = 0.3f), shape)
                .padding(horizontal = 8.dp, vertical = 4.dp)) {
            Text(stringResource(body.nameResource), Modifier.testTag("celestial-badge-body"),
                color = primaryText, fontSize = 11.sp, lineHeight = 14.sp)
            SpaceCompassPointTimeCaption(pointName, moment, timeMs, nowMs, primaryText, 11.sp, 14.sp,
                Modifier.fillMaxWidth().testTag("celestial-badge-caption"))
            SpaceCompassCelestialPointAngles(position, primaryText, secondaryText,
                fontSize = 10.sp, lineHeight = 12.sp, tagPrefix = "celestial-badge")
        }
    }) { measurables, constraints ->
        val margin = 4.dp.toPx().toDouble()
        val gap = 18.dp.toPx().toDouble()
        val label = measurables.single().measure(constraints.copy(minWidth = 0, minHeight = 0,
            maxWidth = minOf(220.dp.roundToPx(), (constraints.maxWidth - 2 * margin).toInt().coerceAtLeast(0))))
        val anchor = if (orientation == null || constraints.maxWidth <= 0 || constraints.maxHeight <= 0) null else
            projectSpaceCompassSun(position, orientation, constraints.maxWidth.toDouble(), constraints.maxHeight.toDouble())
                .let { SpaceCompassSunScenePoint(it.x, it.y) }
        val point = placeSpaceCompassCelestialTimeBadge(constraints.maxWidth.toDouble(), constraints.maxHeight.toDouble(),
            label.width.toDouble(), label.height.toDouble(), anchor, margin, gap, excluded)
        layout(constraints.maxWidth, constraints.maxHeight) {
            point?.let { label.place(it.x.roundToInt(), it.y.roundToInt()) }
        }
    }
}
