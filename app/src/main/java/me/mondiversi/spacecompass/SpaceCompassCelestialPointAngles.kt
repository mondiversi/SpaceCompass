package me.mondiversi.spacecompass

import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.ui.Alignment
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.*
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.sp

/** Shared two-column point readout for the reticle caption and the tapped-point panel. */
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
    Column(modifier) { rows.forEach { row ->
        Row(Modifier.fillMaxWidth().testTag(row.tag).clearAndSetSemantics {
            contentDescription = row.announcement; text = AnnotatedString(row.value)
        }, verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(row.label, Modifier.weight(1f), color = secondaryText, fontSize = fontSize, lineHeight = lineHeight)
            Text(row.value, color = primaryText, fontSize = fontSize, lineHeight = lineHeight,
                style = TextStyle(textDirection = TextDirection.Ltr), textAlign = TextAlign.End)
        }
    } }
}
