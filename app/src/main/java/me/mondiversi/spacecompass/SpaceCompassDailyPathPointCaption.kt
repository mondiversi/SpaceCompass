package me.mondiversi.spacecompass

import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/** A stable two-line left column; clock and elapsed/remaining minutes share its second line. */
@Composable
internal fun SpaceCompassDailyPathPointCaption(name: String, moment: String, countdown: String,
    primary: Color, secondary: Color, emphasized: Boolean, modifier: Modifier = Modifier,
    point: SpaceCompassSunPathPoint? = null, isCurrent: Boolean = false,
    trend: SpaceCompassElevationTrend? = null) {
    val weight = if (emphasized) FontWeight.Bold else FontWeight.Normal
    Column(modifier) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            point?.let { SpaceCompassDailyPathEventIcons(it, isCurrent, primary, trend) }
            Text(name, color = primary, fontSize = 12.sp, lineHeight = 15.sp,
                fontWeight = weight,
                modifier = Modifier.weight(1f).testTag("daily-path-point-name"))
        }
        Text("$moment · $countdown", color = secondary, fontSize = 11.sp, lineHeight = 14.sp,
            fontWeight = weight,
            style = TextStyle(textDirection = TextDirection.Ltr, fontFeatureSettings = "tnum"),
            modifier = Modifier.testTag("daily-path-point-time"))
    }
}
