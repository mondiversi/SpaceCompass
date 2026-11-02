package me.mondiversi.spacecompass

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/** Use the normal zoom button's appearance without a camera device, range or preferences. */
@Composable
internal fun SpaceCompassTutorialCameraZoom(color: Color, background: Color) {
    val value = formatSpaceCompassNumber(1.0, 0, LocalSpaceCompassNumericFormat.current, grouping = false) + "×"
    SpaceCompassFloatingActionButton(stringResource(R.string.camera_zoom_level, value), {}, color, background,
        Modifier.testTag("tutorial-camera-zoom").spaceCompassTutorialTarget(SpaceCompassTutorialFeature.ZOOM)) {
        Text(value, Modifier.padding(2.dp), fontSize = 14.sp, fontWeight = FontWeight.SemiBold,
            textAlign = TextAlign.Center, maxLines = 1)
    }
}
