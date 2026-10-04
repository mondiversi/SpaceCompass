package me.mondiversi.planetcompass

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.time.Instant
import java.time.ZoneId

/** Same selection and controls, placed over the sky in portrait and above the data in landscape. */
@Composable
internal fun PlanetCompassSunPathSelectedPanel(
    path: PlanetCompassSunDailyPath?, state: PlanetCompassSunDailyPathUiState,
    primaryText: Color, secondaryText: Color, backgroundColor: Color, modifier: Modifier = Modifier,
    compact: Boolean = false, body: PlanetCompassCelestialBody = path?.body ?: PlanetCompassCelestialBody.SUN
) {
    val selected = path?.let(state::selectedIn) ?: state.selectedCurrentFor(body) ?: return
    val zone = path?.zone ?: ZoneId.systemDefault()
    val labelPath = path ?: PlanetCompassSunDailyPath(Instant.ofEpochMilli(selected.timeMs).atZone(zone).toLocalDate(),
        zone, listOf(selected), emptyList(), body)
    val labels = rememberPlanetCompassSunPathLabels(labelPath)
    val pointCaption = if (state.selectedCurrentFor(body) != null) labels.current(selected) else labels.point(selected)
    val caption = stringResource(R.string.celestial_point_time, stringResource(body.nameResource), pointCaption)
    val closeLabel = stringResource(R.string.close)
    val canStep = path != null && path.markers.isNotEmpty()
    val lineHeight = if (compact) 14.sp else TextUnit.Unspecified
    Row(modifier.fillMaxWidth().padding(4.dp).clip(RoundedCornerShape(16.dp))
        .background(backgroundColor.copy(alpha = 0.88f)).testTag("sun-path-selected"),
        verticalAlignment = Alignment.CenterVertically) {
        if (canStep) PlanetCompassSunPathStepButton(false, stringResource(R.string.sun_path_previous), primaryText) {
            path?.let { p -> adjacentPlanetCompassSunPathPoint(p.markers, selected, false)?.let { state.select(p, it) } }
        }
        Column(Modifier.weight(1f).padding(vertical = 5.dp)) {
            Text(caption, Modifier.testTag("celestial-selected-caption"),
                color = primaryText, fontSize = 12.sp, lineHeight = lineHeight,
                fontWeight = FontWeight.Normal)
            PlanetCompassCelestialPointAngles(selected.position, primaryText, secondaryText,
                lineHeight = lineHeight, tagPrefix = "celestial-selected")
        }
        if (canStep) PlanetCompassSunPathStepButton(true, stringResource(R.string.sun_path_next), primaryText) {
            path?.let { p -> adjacentPlanetCompassSunPathPoint(p.markers, selected, true)?.let { state.select(p, it) } }
        }
        IconButton(onClick = state::clearSelection, Modifier.size(48.dp).semantics {
            contentDescription = closeLabel
        }) { Text("×", color = primaryText, fontSize = 22.sp) }
    }
}

@Composable
private fun PlanetCompassSunPathStepButton(right: Boolean, label: String, color: Color, action: () -> Unit) {
    IconButton(onClick = action, Modifier.size(48.dp).semantics { contentDescription = label }) {
        Canvas(Modifier.size(12.dp, 18.dp)) {
            val x = if (right) size.width else 0f
            val from = if (right) 0f else size.width
            val curve = Path().apply { moveTo(from, 0f); lineTo(x, center.y); lineTo(from, size.height) }
            drawPath(curve, color, style = Stroke(1.5.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round))
        }
    }
}
