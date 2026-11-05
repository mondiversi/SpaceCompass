package me.mondiversi.spacecompass

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

internal const val SpaceCompassObservationLabelWeight = .40f
internal const val SpaceCompassObservationAnglesWeight = .26f
internal const val SpaceCompassObservationDateWeight = .34f

/** Both annual tables use the same columns, symbols and reading order. */
@Composable
internal fun SpaceCompassObservationTableHeader(color: Color) {
    Row(Modifier.fillMaxWidth().padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
        Spacer(Modifier.width(32.dp))
        Text(stringResource(R.string.capture_table_events), Modifier.weight(SpaceCompassObservationLabelWeight),
            color = color, fontSize = 10.sp, lineHeight = 13.sp, fontWeight = FontWeight.SemiBold)
        Text(stringResource(R.string.capture_table_azimuth) + "\n" + stringResource(R.string.capture_table_elevation),
            Modifier.weight(SpaceCompassObservationAnglesWeight).testTag("observation-angles-heading"),
            color = color, fontSize = 10.sp, lineHeight = 13.sp, fontWeight = FontWeight.SemiBold,
            textAlign = TextAlign.Center)
        Text(stringResource(R.string.format_date_time_heading) + "\n" + stringResource(R.string.format_time_heading),
            Modifier.weight(SpaceCompassObservationDateWeight).padding(end = SpaceCompassObservationInkInset),
            color = color, fontSize = 10.sp, lineHeight = 13.sp, fontWeight = FontWeight.SemiBold,
            textAlign = TextAlign.End)
    }
    HorizontalDivider(thickness = .5.dp, color = color.copy(alpha = .18f))
}

@Composable
internal fun SpaceCompassObservationAngles(azimuth: Double?, elevation: Double?,
    formatting: SpaceCompassPanoramaFormatting, color: Color, tag: String, modifier: Modifier = Modifier) {
    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        SpaceCompassObservationAngle(azimuth, true, formatting, color, "$tag-azimuth")
        SpaceCompassObservationAngle(elevation, false, formatting, color, "$tag-elevation")
    }
}

@Composable
private fun SpaceCompassObservationAngle(value: Double?, azimuth: Boolean,
    formatting: SpaceCompassPanoramaFormatting, color: Color, tag: String) {
    val text = remember(value, formatting) { value?.let { formatSpaceCompassPanoramaElevation(it, formatting) + "°" } ?: "—" }
    val description = stringResource(if (azimuth) R.string.celestial_point_azimuth else R.string.celestial_point_elevation, text)
    Row(Modifier.testTag(tag).clearAndSetSemantics { contentDescription = description },
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(3.dp)) {
        // Draw the horizontal/vertical double arrows so their ink is centered on the value.
        Canvas(Modifier.size(10.dp)) {
            fun point(x: Float, y: Float) = if (azimuth) Offset(size.width*x, size.height*y)
                else Offset(size.width*y, size.height*x)
            fun line(x1: Float, y1: Float, x2: Float, y2: Float) =
                drawLine(color, point(x1,y1), point(x2,y2), 1.dp.toPx(), StrokeCap.Round)
            line(.1f,.5f,.9f,.5f)
            line(.1f,.5f,.34f,.26f); line(.1f,.5f,.34f,.74f)
            line(.9f,.5f,.66f,.26f); line(.9f,.5f,.66f,.74f)
        }
        Text(text, color = color, fontSize = 12.sp, lineHeight = 16.sp, softWrap = false,
            style = TextStyle(textDirection = TextDirection.ContentOrLtr))
    }
}
