package me.mondiversi.spacecompass

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription

private val SpaceCompassScenarioResume.labelResource: Int
    get() = when (this) {
        SpaceCompassScenarioResume.RESTART -> R.string.scenario_restart
        SpaceCompassScenarioResume.ACTIVE -> R.string.scenario_active
        SpaceCompassScenarioResume.ALWAYS -> R.string.scenario_always
    }

/** Configuration action: matches the other secondary-page toolbar buttons in both themes. */
@Composable
internal fun SpaceCompassScenarioResumeButton(observer: SpaceCompassObserverState) {
    var expanded by rememberSaveable { mutableStateOf(false) }
    val selection = stringResource(observer.timeSettings.resume.labelResource)
    SpaceCompassTitleActionButton(stringResource(R.string.scenario_resume), onClick = { expanded = true },
        modifier = Modifier.testTag("scenario-resume-open").semantics { stateDescription = selection }) {
        Icon(painterResource(R.drawable.ic_scenario_resume), contentDescription = null,
            modifier = Modifier.size(SpaceCompassTitleActionIconSize))
    }
    if (expanded) SpaceCompassScenarioResumeDialog(observer.timeSettings.resume, onSelect = { mode ->
        if (observer.timeSettings.resume != mode) {
            observer.configureTime(observer.timeSettings.copy(resume = mode))

        }
        expanded = false
    }, onDismiss = { expanded = false })
}

@Composable
private fun SpaceCompassScenarioResumeDialog(selected: SpaceCompassScenarioResume,
    onSelect: (SpaceCompassScenarioResume) -> Unit, onDismiss: () -> Unit) {
    SpaceCompassAlertDialog(onDismissRequest = onDismiss, confirmButton = null,
        title = { Text(stringResource(R.string.scenario_resume)) }, text = {
            Column(Modifier.fillMaxWidth().selectableGroup().testTag("scenario-resume-choices"),
                verticalArrangement = Arrangement.spacedBy(SpaceCompassSettingsChoiceSpacing)) {
                SpaceCompassScenarioResume.entries.forEach { mode ->
                    SpaceCompassSettingsRadioRow(mode == selected, { onSelect(mode) },
                        Modifier.fillMaxWidth().testTag("scenario-resume-${mode.name}")) {
                        SpaceCompassSettingsChoiceLabel(stringResource(mode.labelResource), Modifier.weight(1f))
                    }
                }
            }
        })
}
