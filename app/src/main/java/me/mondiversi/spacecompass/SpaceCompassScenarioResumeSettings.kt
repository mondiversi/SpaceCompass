package me.mondiversi.spacecompass

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp

internal val SpaceCompassScenarioResume.labelResource: Int
    get() = when (this) {
        SpaceCompassScenarioResume.RESTART -> R.string.scenario_restart
        SpaceCompassScenarioResume.ACTIVE -> R.string.scenario_active
        SpaceCompassScenarioResume.ALWAYS -> R.string.scenario_always
    }

/** Persistent scenario behavior belongs with the editor's other settings, not in its toolbar. */
@Composable
internal fun SpaceCompassScenarioResumeIsland(observer: SpaceCompassObserverState) {
    val selected = observer.timeSettings.resume
    SpaceCompassSettingsIsland() {
        Column(Modifier.fillMaxWidth().selectableGroup().testTag("scenario-resume-choices"),
            verticalArrangement = Arrangement.spacedBy(SpaceCompassSettingsChoiceSpacing)) {
            SpaceCompassSettingsDescription(stringResource(R.string.scenario_resume_description))
            Spacer(Modifier.height(4.dp))
            SpaceCompassScenarioResume.entries.forEach { mode ->
                SpaceCompassSettingsRadioRow(mode == selected, onClick = {
                    if (observer.timeSettings.resume != mode)
                        observer.configureTime(observer.timeSettings.copy(resume = mode))
                }, modifier = Modifier.fillMaxWidth().testTag("scenario-resume-${mode.name}")) {
                    SpaceCompassSettingsChoiceLabel(stringResource(mode.labelResource), Modifier.weight(1f))
                }
            }
        }
    }
}
