package me.mondiversi.spacecompass

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.text.InlineTextContent
import androidx.compose.foundation.text.appendInlineContent
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.text.Placeholder
import androidx.compose.ui.text.PlaceholderVerticalAlign
import androidx.compose.ui.text.buildAnnotatedString
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

/** Paused time is always explicit; moving time is labelled whenever its civil day differs from today. */
internal val SpaceCompassMainCaptionControlGap = 6.dp

internal fun spaceCompassShowScenarioMoment(plan: SpaceCompassObserverPlan?, scenarioMs: Long,
    realMs: Long, zone: ZoneId, paused: Boolean = false): Boolean = paused ||
    plan?.scenarioClock?.settings?.multiplier == 0 ||
    (plan.activeSectionCount > 0 || plan?.scenarioClock != null) &&
    Instant.ofEpochMilli(scenarioMs).atZone(zone).toLocalDate() !=
        Instant.ofEpochMilli(realMs).atZone(zone).toLocalDate()

internal fun formatSpaceCompassScenarioMoment(timeMs: Long, zone: ZoneId,
    formatting: SpaceCompassPanoramaFormatting, paused: Boolean = false): String {
    val timezone = TimeZone.getTimeZone(zone)
    val dateLocale = if (formatting.date == SpaceCompassDateFormat.SYSTEM) formatting.deviceLocale else formatting.locale
    val date = formatSpaceCompassDateOnly(timeMs, formatting.date, dateLocale, timezone,
        formatting.numeric, formatting.deviceLocale, allowHistorical = true)
    val time = formatSpaceCompassTimeOnly(timeMs, formatting.time, formatting.locale, timezone,
        formatting.numeric, formatting.deviceLocale, includeSeconds = true, allowHistorical = true)
    // Keep the marker in the semantic text; the UI replaces it with a theme-coloured vector.
    val pause = if (paused) "⏸ " else ""
    return "$pause$date · $time · ${formatSpaceCompassObserverUtcOffset(zone.rules.getOffset(Instant.ofEpochMilli(timeMs)))}"
}

/** No independent timer: the label shares the sky's effective clock and stops with that clock. */
@Composable
internal fun spaceCompassScenarioMomentLabel(plan: SpaceCompassObserverPlan?, timeMs: Long,
    realMs: Long, zone: ZoneId, paused: Boolean = false): String? {
    if (!spaceCompassShowScenarioMoment(plan, timeMs, realMs, zone, paused)) return null
    val locale = LocalConfiguration.current.locales[0]
    val deviceLocale = LocalSpaceCompassDeviceLocale.current
    val numeric = LocalSpaceCompassNumericFormat.current
    val dates = LocalSpaceCompassDateFormat.current
    val times = resolveSpaceCompassTimeFormat(LocalContext.current, LocalSpaceCompassTimeFormat.current)
    val secondMs = Math.floorDiv(timeMs, 1000L) * 1000L
    val timePaused = paused || plan?.scenarioClock?.settings?.multiplier == 0
    return remember(secondMs, zone, locale, deviceLocale, numeric, dates, times, timePaused) {
        formatSpaceCompassScenarioMoment(secondMs, zone, SpaceCompassPanoramaFormatting(
            locale = locale, numeric = numeric, date = dates, time = times, deviceLocale = deviceLocale), paused = timePaused)
    }
}

/** Paint the slim gray stroke first so the orange fill keeps its original letter weight. */
@Composable
internal fun SpaceCompassScenarioMomentText(text: String, foreground: Color, outline: Color,
    modifier: Modifier = Modifier) {
    val style = LocalTextStyle.current.copy(fontSize = 12.sp, lineHeight = 16.sp,
        fontWeight = FontWeight.Medium, textAlign = TextAlign.Center)
    val width = with(LocalDensity.current) { .8.dp.toPx() }
    val caption = remember(text) {
        buildAnnotatedString {
            if (text.startsWith("⏸ ")) {
                appendInlineContent("scenario-pause", "⏸")
                append(" "); append(text.removePrefix("⏸ "))
            } else append(text)
        }
    }
    fun pauseContent(color: Color, stroke: Boolean) = mapOf("scenario-pause" to InlineTextContent(
        Placeholder(10.sp, 12.sp, PlaceholderVerticalAlign.TextCenter)) {
        Canvas(Modifier.fillMaxSize()) {
            val paintStyle = if (stroke) Stroke(width) else Fill
            for (x in listOf(.15f, .61f)) drawRoundRect(color,
                topLeft = Offset(size.width * x, size.height * .12f),
                size = Size(size.width * .24f, size.height * .76f),
                cornerRadius = CornerRadius(size.width * .035f), style = paintStyle)
        }
    })
    Box(modifier) {
        Text(caption, Modifier.clearAndSetSemantics {}, color = outline,
            style = style.copy(drawStyle = Stroke(width = width)), maxLines = 2,
            inlineContent = pauseContent(outline, stroke = true))
        Text(caption, color = foreground, style = style, maxLines = 2,
            inlineContent = pauseContent(foreground, stroke = false))
    }
}
