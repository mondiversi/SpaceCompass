package me.mondiversi.spacecompass

import android.icu.text.TimeZoneNames
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.*
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.time.Instant
import java.time.ZoneId
import java.util.TimeZone
import kotlin.math.abs

internal enum class SpaceCompassScenarioEditorSection(val key: String, val titleResource: Int, val iconKey: String) {
    POSITION("position", R.string.observer_place, "coordinates"),
    ALTITUDE("altitude", R.string.observer_altitude, "altitude"),
    ZONE("zone", R.string.observer_zone, "time_zone"),
    DATE("date", R.string.observer_date, "date_format"),
    TIME("time", R.string.observer_time, "time_format"),
    SPEED("speed", R.string.scenario_speed, "scenario_speed"),
    RESUME("resume", R.string.scenario_resume, "scenario_resume")
}

internal data class SpaceCompassScenarioMenuEntry(val section: SpaceCompassScenarioEditorSection,
    val summary: String, val customized: Boolean = false, val automatic: Boolean = false)

/** Show the values actually in use; automatic is a secondary status rather than a replacement value. */
@Composable
internal fun spaceCompassScenarioMenuEntries(observer: SpaceCompassObserverState,
    place: SpaceCompassEstimatedPlaceReading, now: Long): List<SpaceCompassScenarioMenuEntry> {
    val plan = observer.plan
    val current = spaceCompassScenarioCurrentValues(plan, observer.devicePlace, observer.timeSettings, now, ZoneId.systemDefault())
    val numeric = LocalSpaceCompassNumericFormat.current
    val units = LocalSpaceCompassUnits.current
    val locale = LocalConfiguration.current.locales[0]
    val deviceLocale = LocalSpaceCompassDeviceLocale.current
    val dates = LocalSpaceCompassDateFormat.current
    val dateLocale = if (dates == SpaceCompassDateFormat.SYSTEM) deviceLocale else locale
    val times = resolveSpaceCompassTimeFormat(LocalContext.current, LocalSpaceCompassTimeFormat.current)
    val names = remember(locale) { TimeZoneNames.getInstance(locale) }
    val height = current.altitudeMeters?.let { formatSpaceCompassNumber(if (units.feet) it / .3048 else it, 2,
        numeric, grouping = false, systemLocale = deviceLocale) + if (units.feet) " ft" else " m" } ?: "—"
    val zoneLabel = (names.getExemplarLocationName(current.zone.id) ?: current.zone.id.substringAfterLast('/').replace('_', ' ')) +
        " · " + formatSpaceCompassObserverUtcOffset(current.zone.rules.getOffset(Instant.ofEpochMilli(current.instant)))
    val timezone = TimeZone.getTimeZone(current.zone)
    val date = formatSpaceCompassDateOnly(current.instant, dates, dateLocale, timezone, numeric, deviceLocale, allowHistorical = true)
    val time = formatSpaceCompassTimeOnly(current.instant, times, locale, timezone, numeric, deviceLocale,
        includeSeconds = false, allowHistorical = true)
    val rate = (if (current.rate < 0) "−" else "") + formatSpaceCompassNumber(abs(current.rate).toDouble(), 0,
        numeric, grouping = false, systemLocale = deviceLocale) + "×"
    fun entry(section: SpaceCompassScenarioEditorSection, custom: Boolean, value: String) =
        SpaceCompassScenarioMenuEntry(section, value, custom, automatic = !custom)
    val locality = place.text ?: stringResource(if (place.loading) R.string.environment_place_loading else R.string.environment_place_unavailable)
    return listOf(
        entry(SpaceCompassScenarioEditorSection.POSITION, plan?.simulatePosition == true, locality),
        entry(SpaceCompassScenarioEditorSection.ALTITUDE, plan?.simulateAltitude == true, height),
        entry(SpaceCompassScenarioEditorSection.ZONE, plan?.simulateZone == true, zoneLabel),
        entry(SpaceCompassScenarioEditorSection.DATE, plan?.simulateDate == true, date),
        entry(SpaceCompassScenarioEditorSection.TIME, plan?.simulateTime == true, time),
        entry(SpaceCompassScenarioEditorSection.SPEED, observer.timeSettings.enabled, rate),
        SpaceCompassScenarioMenuEntry(SpaceCompassScenarioEditorSection.RESUME,
            stringResource(observer.timeSettings.resume.labelResource)))
}

/** A single-column navigation list keeps every parameter reachable without expanding unrelated cards. */
@Composable
internal fun SpaceCompassScenarioMenu(entries: List<SpaceCompassScenarioMenuEntry>,
    onOpen: (SpaceCompassScenarioEditorSection) -> Unit, scroll: ScrollState, modifier: Modifier = Modifier,
    validationMessage: String? = null) {
    val foreground = MaterialTheme.colorScheme.onSurface
    val attention = spaceCompassAttentionColor(isSystemInDarkTheme())
    val rtl = LocalLayoutDirection.current == LayoutDirection.Rtl
    val direction = LocalLayoutDirection.current
    Column(modifier.fillMaxWidth().scrollbarOverlay(scroll,foreground.copy(alpha=.46f))
        .verticalScroll(scroll).padding(spaceCompassPageContentPadding).testTag("scenario-management-menu"),
        verticalArrangement = Arrangement.spacedBy(SpaceCompassSettingsIslandGap)) {
        entries.forEach { entry ->
            Surface(onClick = { onOpen(entry.section) }, shape = RoundedCornerShape(16.dp),
                color = spaceCompassSettingsCardColor(), contentColor = foreground,
                modifier = Modifier.fillMaxWidth().testTag("scenario-section-${entry.section.key}")) {
                Row(Modifier.fillMaxWidth().heightIn(min=56.dp).padding(
                    start=spaceCompassIslandContentPadding.calculateStartPadding(direction),
                    end=spaceCompassIslandContentPadding.calculateEndPadding(direction),top=8.dp,bottom=8.dp),
                    verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(10.dp)) {
                    Box(Modifier.size(28.dp),contentAlignment=Alignment.Center) {
                        SpaceCompassSettingsGroupIcon(entry.section.iconKey,foreground)
                    }
                    SpaceCompassSettingsChoiceLabel(stringResource(entry.section.titleResource),Modifier.weight(.48f),lineHeight=18.sp)
                    Column(Modifier.weight(.52f), horizontalAlignment = Alignment.End,
                        verticalArrangement = Arrangement.spacedBy(SpaceCompassSettingsChoiceSpacing)) {
                        Text(entry.summary, Modifier.fillMaxWidth().testTag("scenario-summary-${entry.section.key}"),
                            fontSize = 12.sp, lineHeight = 16.sp,
                            color = if (entry.customized) attention else foreground.copy(alpha = .72f),
                            textAlign = TextAlign.End, style = TextStyle(textDirection = TextDirection.ContentOrLtr))
                        if (entry.automatic) Text(stringResource(R.string.scenario_summary_automatic),
                            Modifier.fillMaxWidth(), fontSize = 11.sp, lineHeight = 14.sp,
                            color = foreground.copy(alpha = .56f), textAlign = TextAlign.End)
                    }
                    SpaceCompassIconCanvas(foreground.copy(alpha=.72f),Modifier.size(18.dp)) { ink ->
                        fun point(x: Float,y: Float)=Offset(size.width*(if(rtl) 1f-x else x),size.height*y)
                        val path=Path().apply {
                            moveTo(point(.38f,.25f).x,point(.38f,.25f).y)
                            lineTo(point(.63f,.50f).x,point(.63f,.50f).y)
                            lineTo(point(.38f,.75f).x,point(.38f,.75f).y)
                        }
                        drawPath(path,ink,style=Stroke(2.dp.toPx(),cap=StrokeCap.Round,join=StrokeJoin.Round))
                    }
                }
            }
        }
        if(validationMessage != null) Text(validationMessage,Modifier.padding(8.dp).testTag("observer-validation-error"),
            color=MaterialTheme.colorScheme.error,style=MaterialTheme.typography.bodySmall)
    }
}
