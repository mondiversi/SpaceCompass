package me.mondiversi.spacecompass

import androidx.compose.foundation.layout.Box
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.Text
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import java.time.Instant
import java.time.ZoneId
import java.util.TimeZone

/** Compare civil days in the same observation zone, using the running instant rather than the saved origin. */
internal fun spaceCompassShowScenarioMoment(plan: SpaceCompassObserverPlan?, scenarioMs: Long,
    realMs: Long, zone: ZoneId): Boolean = plan.activeSectionCount > 0 &&
    Instant.ofEpochMilli(scenarioMs).atZone(zone).toLocalDate()
        .isAfter(Instant.ofEpochMilli(realMs).atZone(zone).toLocalDate())

internal fun formatSpaceCompassScenarioMoment(timeMs: Long, zone: ZoneId,
    formatting: SpaceCompassPanoramaFormatting): String {
    val timezone = TimeZone.getTimeZone(zone)
    val dateLocale = if (formatting.date == SpaceCompassDateFormat.SYSTEM) formatting.deviceLocale else formatting.locale
    val date = formatSpaceCompassDateOnly(timeMs, formatting.date, dateLocale, timezone,
        formatting.numeric, formatting.deviceLocale, allowHistorical = true)
    val time = formatSpaceCompassTimeOnly(timeMs, formatting.time, formatting.locale, timezone,
        formatting.numeric, formatting.deviceLocale, includeSeconds = true, allowHistorical = true)
    return "$date · $time · ${formatSpaceCompassObserverUtcOffset(zone.rules.getOffset(Instant.ofEpochMilli(timeMs)))}"
}

/** No independent timer: the label shares the sky's effective clock and stops with that clock. */
@Composable
internal fun spaceCompassScenarioMomentLabel(plan: SpaceCompassObserverPlan?, timeMs: Long,
    realMs: Long, zone: ZoneId): String? {
    if (!spaceCompassShowScenarioMoment(plan, timeMs, realMs, zone)) return null
    val locale = LocalConfiguration.current.locales[0]
    val deviceLocale = LocalSpaceCompassDeviceLocale.current
    val numeric = LocalSpaceCompassNumericFormat.current
    val dates = LocalSpaceCompassDateFormat.current
    val times = resolveSpaceCompassTimeFormat(LocalContext.current, LocalSpaceCompassTimeFormat.current)
    val secondMs = Math.floorDiv(timeMs, 1000L) * 1000L
    return remember(secondMs, zone, locale, deviceLocale, numeric, dates, times) {
        formatSpaceCompassScenarioMoment(secondMs, zone, SpaceCompassPanoramaFormatting(
            locale = locale, numeric = numeric, date = dates, time = times, deviceLocale = deviceLocale))
    }
}

/** Paint the slim gray stroke first so the orange fill keeps its original letter weight. */
@Composable
internal fun SpaceCompassScenarioMomentText(text: String, foreground: Color, outline: Color,
    modifier: Modifier = Modifier) {
    val style = LocalTextStyle.current.copy(fontSize = 12.sp, lineHeight = 16.sp,
        fontWeight = FontWeight.Medium, textAlign = TextAlign.Center)
    val width = with(LocalDensity.current) { .8.dp.toPx() }
    Box(modifier) {
        Text(text, Modifier.clearAndSetSemantics {}, color = outline,
            style = style.copy(drawStyle = Stroke(width = width)), maxLines = 2)
        Text(text, color = foreground, style = style, maxLines = 2)
    }
}
