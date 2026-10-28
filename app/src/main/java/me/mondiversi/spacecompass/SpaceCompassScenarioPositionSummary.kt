package me.mondiversi.spacecompass

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.unit.sp

/** Keep complete coordinates on one accessible line; use the existing cached real-place lookup. */
@Composable
internal fun SpaceCompassScenarioPositionSummary(coordinates: String, place: SpaceCompassEstimatedPlaceReading) {
    Column(Modifier.fillMaxWidth(),verticalArrangement=Arrangement.spacedBy(SpaceCompassSettingsChoiceSpacing)) {
        SpaceCompassScrollingText(coordinates.replace("\n"," · "),
            Modifier.fillMaxWidth().testTag("scenario-position-coordinates"),
            color=MaterialTheme.colorScheme.onSurface.copy(alpha=.72f),
            style=TextStyle(fontSize=12.sp,lineHeight=16.sp,textDirection=TextDirection.ContentOrLtr))
        SpaceCompassSettingsDescription(place.text ?: stringResource(if(place.loading)
            R.string.environment_place_loading else R.string.environment_place_unavailable),
            Modifier.fillMaxWidth().testTag("scenario-position-locality"))
    }
}
