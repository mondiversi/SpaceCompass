package me.mondiversi.spacecompass

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.unit.sp
import kotlin.math.roundToInt

@Composable
internal fun SpaceCompassScenarioSpeedIsland(observer: SpaceCompassObserverState, onExpanded: (Boolean) -> Unit = {}) {
    val settings = observer.timeSettings
    val context = LocalContext.current
    val saved = stringResource(R.string.settings_saved)
    val title = stringResource(R.string.scenario_speed)
    var index by remember(settings.rate) { mutableFloatStateOf(spaceCompassScenarioRates.indexOf(settings.rate).toFloat()) }
    SpaceCompassSettingsToggleIsland(title, "scenario_speed", settings.enabled, title, {
        observer.configureTime(settings.copy(enabled = it)); onExpanded(it); showSpaceCompassBottomMessage(context, saved)
    }, Modifier.testTag("scenario-speed-enabled")) {
        SpaceCompassSettingsDescription(stringResource(R.string.scenario_speed_description))
        val rate = spaceCompassScenarioRates[index.roundToInt().coerceIn(spaceCompassScenarioRates.indices)]
        val shown = formatSpaceCompassNumber(rate.toDouble(), 0, LocalSpaceCompassNumericFormat.current,
            grouping = false, systemLocale = LocalSpaceCompassDeviceLocale.current)
        val divisor = if (rate >= 3600) 3600 else if (rate >= 60) 60 else 1
        val unit = if (divisor == 3600) "h" else if (divisor == 60) "min" else "s"
        val duration = formatSpaceCompassNumber(rate.toDouble()/divisor, if (rate % divisor == 0) 0 else 1,
            LocalSpaceCompassNumericFormat.current, grouping = false, systemLocale = LocalSpaceCompassDeviceLocale.current)
        val caption = stringResource(R.string.scenario_speed_value, shown) + " · 1 s = $duration $unit"
        Text(caption, Modifier.testTag("scenario-speed-value"), fontSize = 14.sp, lineHeight = 18.sp,
            style = TextStyle(textDirection = TextDirection.Ltr))
        Slider(index, { index = it }, valueRange = 0f..spaceCompassScenarioRates.lastIndex.toFloat(),
            steps = spaceCompassScenarioRates.size - 2, colors = spaceCompassSliderColors(),
            modifier = Modifier.fillMaxWidth().testTag("scenario-speed-slider").semantics {
                contentDescription = title; stateDescription = caption
            }, onValueChangeFinished = {
                val committedRate = spaceCompassScenarioRates[index.roundToInt().coerceIn(spaceCompassScenarioRates.indices)]
                if (committedRate != settings.rate) {
                    observer.configureTime(settings.copy(rate = committedRate)); showSpaceCompassBottomMessage(context, saved)
                }
            })
    }
}

@Composable
internal fun SpaceCompassScenarioResumeChoices(observer: SpaceCompassObserverState) {
    val settings = observer.timeSettings
    val context = LocalContext.current
    val saved = stringResource(R.string.settings_saved)
    val title = stringResource(R.string.scenario_resume)
    val options = listOf(SpaceCompassScenarioResume.RESTART to R.string.scenario_restart,
        SpaceCompassScenarioResume.ACTIVE to R.string.scenario_active,
        SpaceCompassScenarioResume.ALWAYS to R.string.scenario_always)
    Text(title, fontSize = 13.sp, fontWeight = FontWeight.Medium)
    Column(Modifier.fillMaxWidth().selectableGroup().testTag("scenario-resume-choices"),
        verticalArrangement = Arrangement.spacedBy(SpaceCompassSettingsChoiceSpacing)) {
        options.forEach { (mode, label) ->
            SpaceCompassSettingsRadioRow(settings.resume == mode, {
                if (settings.resume != mode) {
                    observer.configureTime(settings.copy(resume = mode))
                    showSpaceCompassBottomMessage(context, saved)
                }
            }, Modifier.fillMaxWidth().testTag("scenario-resume-${mode.name}")) {
                SpaceCompassSettingsChoiceLabel(stringResource(label), Modifier.weight(1f))
            }
        }
    }
}
