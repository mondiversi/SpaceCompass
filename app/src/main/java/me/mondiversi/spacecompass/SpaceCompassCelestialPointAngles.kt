package me.mondiversi.spacecompass

import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.ui.Alignment
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.*
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.dp
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.sp

/** Compact point measurements share one line and retain their full translated accessibility names. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun SpaceCompassCelestialPointAngles(position: SpaceCompassSunPosition, primaryText: Color, secondaryText: Color,
    modifier: Modifier = Modifier, fontSize: TextUnit = 11.sp, lineHeight: TextUnit = 14.sp,
    tagPrefix: String = "celestial-point") {
    val numeric = LocalSpaceCompassNumericFormat.current
    val rows = listOf(
        spaceCompassSunDataRow(stringResource(R.string.celestial_point_azimuth, SPACE_COMPASS_SUN_DATA_MARKER),
            "${formatSpaceCompassNumber(position.azimuthDegrees, 1, numeric)}°", "$tagPrefix-azimuth"),
        spaceCompassSunDataRow(stringResource(R.string.celestial_point_elevation, SPACE_COMPASS_SUN_DATA_MARKER),
            "${formatSpaceCompassNumber(position.elevationDegrees, 1, numeric)}°", "$tagPrefix-elevation"))
    FlowRow(modifier.fillMaxWidth()) {
        rows.forEachIndexed { index, row ->
            Row(Modifier.testTag(row.tag).clearAndSetSemantics {
                contentDescription = row.announcement; text = AnnotatedString(row.value)
            }, verticalAlignment = Alignment.CenterVertically) {
                if (index > 0) Text(" · ", color = secondaryText, fontSize = fontSize, lineHeight = lineHeight)
                SpaceCompassObservationAngleIcon(azimuth = index == 0, color = secondaryText)
                Spacer(Modifier.width(3.dp))
                Text(row.value, color = primaryText, fontSize = fontSize, lineHeight = lineHeight,
                    softWrap = false, maxLines = 1, style = TextStyle(textDirection = TextDirection.Ltr))
            }
        }
    }
}
