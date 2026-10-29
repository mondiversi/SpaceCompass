package me.mondiversi.spacecompass

import android.icu.text.TimeZoneNames
import androidx.compose.foundation.lazy.staggeredgrid.LazyStaggeredGridState
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridItemSpan
import androidx.compose.foundation.lazy.staggeredgrid.items
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.clickable
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.semantics.Role
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.layout
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
import androidx.compose.ui.text.font.FontWeight
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

/** Only the compact icon/check/title target toggles; the rest of each card opens its editor. */
@Composable
internal fun SpaceCompassScenarioMenu(entries: List<SpaceCompassScenarioMenuEntry>,
    onOpen: (SpaceCompassScenarioEditorSection) -> Unit,
    onToggle: (SpaceCompassScenarioEditorSection, Boolean) -> Unit,
    scroll: LazyStaggeredGridState, modifier: Modifier = Modifier, validationMessage: String? = null) {
    val foreground = MaterialTheme.colorScheme.onSurface
    val attention = spaceCompassAttentionColor(isSystemInDarkTheme())
    val rtl = LocalLayoutDirection.current == LayoutDirection.Rtl
    SpaceCompassIslandGrid(modifier.testTag("scenario-management-menu"), scroll=scroll) {
        items(entries, key={ it.section.key }) { entry ->
            val title = stringResource(entry.section.titleResource)
            val canToggle = entry.section != SpaceCompassScenarioEditorSection.RESUME
            val open = { onOpen(entry.section) }
            val activation = if (canToggle) Modifier.testTag("scenario-toggle-${entry.section.key}")
                .toggleable(entry.customized, interactionSource=remember { MutableInteractionSource() }, indication=null,
                    role=Role.Checkbox) { onToggle(entry.section, it) }
                .spaceCompassAccessibleAction(title, role = Role.Checkbox, checkedState = entry.customized,
                    onClick = { onToggle(entry.section, !entry.customized) })
            else Modifier
            Surface(onClick = open, shape = RoundedCornerShape(16.dp),
                color = spaceCompassSettingsCardColor(), contentColor = foreground,
                modifier = Modifier.fillMaxWidth().testTag("scenario-section-${entry.section.key}")
                    .spaceCompassNestedAccessibleAction("$title: ${entry.summary}", onClick = open)) {
                Column(Modifier.fillMaxWidth().padding(start=12.dp,end=12.dp,top=0.dp,bottom=8.dp)) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement=Arrangement.SpaceBetween,
                        verticalAlignment=Alignment.CenterVertically) {
                        // Keep the full activation target without an additional top inset around its contents.
                        Box(Modifier.weight(1f,fill=false).heightIn(min=48.dp).then(activation),
                            contentAlignment=Alignment.CenterStart) {
                            Row(verticalAlignment=Alignment.CenterVertically,
                                horizontalArrangement=Arrangement.spacedBy(6.dp)) {
                                Box(Modifier.size(20.dp),contentAlignment=Alignment.Center) {
                                    SpaceCompassSettingsGroupIcon(entry.section.iconKey,foreground)
                                }
                                if (canToggle) CompositionLocalProvider(LocalMinimumInteractiveComponentSize provides 0.dp) {
                                    Checkbox(entry.customized, onCheckedChange = null,
                                        modifier = Modifier.size(SpaceCompassSettingsControlSize),
                                        colors = spaceCompassCheckboxColors(uncheckedColor = foreground.copy(alpha=.72f)))
                                }
                                SpaceCompassSettingsChoiceLabel(title,Modifier.weight(1f,fill=false),lineHeight=18.sp)
                            }
                        }
                        Spacer(Modifier.width(8.dp))
                        SpaceCompassIconCanvas(foreground.copy(alpha=.72f),Modifier.size(18.dp).offset(x=6.dp)) { ink ->
                            fun point(x: Float,y: Float)=Offset(size.width*(if(rtl) 1f-x else x),size.height*y)
                            val path=Path().apply {
                                moveTo(point(.38f,.25f).x,point(.38f,.25f).y)
                                lineTo(point(.63f,.50f).x,point(.63f,.50f).y)
                                lineTo(point(.38f,.75f).x,point(.38f,.75f).y)
                            }
                            drawPath(path,ink,style=Stroke(2.dp.toPx(),cap=StrokeCap.Round,join=StrokeJoin.Round))
                        }
                    }
                    // Consume a little of the header's lower blank area without shrinking its touch target.
                    Row(Modifier.fillMaxWidth().layout { measurable, constraints ->
                        val values = measurable.measure(constraints)
                        val overlap = minOf(2.dp.roundToPx(), (values.height - constraints.minHeight).coerceAtLeast(0))
                        layout(values.width, values.height - overlap) { values.placeRelative(0, -overlap) }
                    },verticalAlignment=Alignment.CenterVertically,
                        horizontalArrangement=Arrangement.spacedBy(10.dp)) {
                        Text(entry.summary, Modifier.weight(1f).testTag("scenario-summary-${entry.section.key}"),
                            fontSize=12.sp,lineHeight=16.sp,
                            fontWeight=if(entry.customized) FontWeight.Bold else FontWeight.Normal,
                            color=if(entry.customized) attention else foreground.copy(alpha=.72f),
                            style=TextStyle(textDirection=TextDirection.ContentOrLtr))
                        if(entry.automatic) Text(stringResource(R.string.scenario_summary_automatic),
                            fontSize=11.sp,lineHeight=14.sp,color=foreground.copy(alpha=.56f))
                    }
                }
            }
        }
        if(validationMessage != null) item(key="validation", span=StaggeredGridItemSpan.FullLine) {
            Text(validationMessage,Modifier.padding(8.dp).testTag("observer-validation-error"),
                color=MaterialTheme.colorScheme.error,style=MaterialTheme.typography.bodySmall)
        }
    }
}
