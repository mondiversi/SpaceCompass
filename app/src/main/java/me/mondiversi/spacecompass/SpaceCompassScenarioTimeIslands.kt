package me.mondiversi.spacecompass

import androidx.compose.foundation.layout.*
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.LayoutDirection
import kotlin.math.abs
import kotlin.math.roundToInt

@Composable
@OptIn(ExperimentalMaterial3Api::class)
internal fun SpaceCompassScenarioSpeedControls(observer: SpaceCompassObserverState, enabled: Boolean, inactiveRate: Int) {
    val settings = observer.timeSettings
    val title = stringResource(R.string.scenario_speed)
    var index by remember(enabled, settings.rate, inactiveRate) {
        val shown = if (enabled) settings.normalized().rate else inactiveRate
        mutableFloatStateOf(spaceCompassScenarioRates.indexOf(shown).coerceAtLeast(0).toFloat())
    }
    SpaceCompassSettingsDescription(stringResource(R.string.scenario_speed_description))
    val rate = spaceCompassScenarioRates[index.roundToInt().coerceIn(spaceCompassScenarioRates.indices)]
    val numeric = LocalSpaceCompassNumericFormat.current
    val locale = LocalSpaceCompassDeviceLocale.current
    fun number(value: Int) = formatSpaceCompassNumber(value.toDouble(), 0, numeric,
        grouping = false, systemLocale = locale)
    val shown = (if (rate < 0) "−" else if (rate > 0) "+" else "") + number(abs(rate))
    val speed = stringResource(R.string.scenario_speed_value, shown)
    val magnitude = abs(rate)
    val divisor = if (magnitude >= 3600) 3600 else if (magnitude >= 60) 60 else 1
    val unit = if (divisor == 3600) "h" else if (divisor == 60) "min" else "s"
    val duration = formatSpaceCompassNumber(magnitude.toDouble()/divisor, if (magnitude % divisor == 0) 0 else 1,
        numeric, grouping = false, systemLocale = locale) + " $unit"
    val direction = stringResource(when {
        rate < 0 -> R.string.scenario_time_backward
        rate > 0 -> R.string.scenario_time_forward
        else -> R.string.scenario_time_paused
    })
    val perSecond = stringResource(R.string.scenario_real_second)
    val caption = "$direction · $speed · $perSecond: $duration"
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(3.dp)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(direction, Modifier.weight(1f), fontSize = 13.sp, lineHeight = 17.sp,
                fontWeight = FontWeight.Medium)
            Text(speed, Modifier.testTag("scenario-speed-value"), fontSize = 14.sp, lineHeight = 18.sp,
                fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary,
                style = TextStyle(textDirection = TextDirection.Ltr))
        }
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            SpaceCompassSettingsDescription(perSecond, Modifier.weight(1f))
            Text(duration, Modifier.testTag("scenario-speed-equivalence"), fontSize = 14.sp, lineHeight = 18.sp,
                fontWeight = FontWeight.Medium, style = TextStyle(textDirection = TextDirection.Ltr))
        }
    }
    // Keep negative/zero/positive in the same physical order in RTL interfaces too.
    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
        Slider(index, { if (enabled) index = it }, enabled = enabled, valueRange = 0f..spaceCompassScenarioRates.lastIndex.toFloat(),
            steps = spaceCompassScenarioRates.size - 2, colors = spaceCompassSliderColors(),
            track = { SpaceCompassScenarioSpeedTrack(index) },
            modifier = Modifier.fillMaxWidth().testTag("scenario-speed-slider").semantics {
                contentDescription = title; stateDescription = caption
            }, onValueChangeFinished = {
                val committedRate = spaceCompassScenarioRates[index.roundToInt().coerceIn(spaceCompassScenarioRates.indices)]
                if (enabled && committedRate != settings.rate) {
                    observer.configureTime(settings.copy(rate = committedRate))
                }
            })
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            val edge = number(spaceCompassScenarioRates.last())
            listOf("−$edge×", "${number(0)}×", "+$edge×").forEach {
                Text(it, fontSize = 11.sp, lineHeight = 14.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = .72f),
                    style = TextStyle(textDirection = TextDirection.Ltr))
            }
        }
    }
    SpaceCompassScenarioMidnightNote()
}

/** A shared footer keeps the same rollover explanation separate from each island's controls. */
@Composable
internal fun SpaceCompassScenarioMidnightNote() {
    HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = .12f))
    SpaceCompassSettingsDescription(stringResource(R.string.scenario_midnight_note),
        Modifier.fillMaxWidth().testTag("scenario-midnight-note"))
}
