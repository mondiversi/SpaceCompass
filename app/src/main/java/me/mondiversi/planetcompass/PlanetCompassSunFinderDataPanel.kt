package me.mondiversi.planetcompass

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.text
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.Constraints

/** Two compact translucent islands: body data, then compass/GPS context. */
@Composable
internal fun PlanetCompassSunFinderDataPanel(
    rows: List<PlanetCompassSunDataRow>, locationRows: List<PlanetCompassSunDataRow>,
    orientation: PlanetCompassSunOrientation?, compassReliable: Boolean,
    message: String?, weatherText: String,
    hasWeather: Boolean, primaryText: Color, secondaryText: Color, backgroundColor: Color,
    modifier: Modifier, compact: Boolean, locationActionLabel: String?, onLocationAction: () -> Unit,
    bodyName: String? = null, bodyNavigation: PlanetCompassCelestialBodyNavigation? = null,
    orientationRows: List<PlanetCompassSunDataRow> = emptyList(),
    locationInfoRows: List<PlanetCompassSunDataRow> = emptyList(),
    bodyActions: @Composable () -> Unit = {}
) {
    var info by rememberSaveable { mutableStateOf(false) }
    val scroll = rememberScrollState()
    val style = TextStyle(fontSize = if (compact) 11.sp else 12.sp,
        lineHeight = if (compact) 14.sp else 16.sp, fontFeatureSettings = "tnum")
    val detailsLabel = stringResource(R.string.celestial_details_compact)
    Column(modifier.testTag("sun-finder-details")
        // Normally fits without scrolling. Keep a fallback for large accessibility fonts/windows.
        .scrollbarOverlay(scroll, secondaryText.copy(alpha = 0.46f))
        .verticalScroll(scroll), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        if (rows.isNotEmpty()) PlanetCompassCelestialBodyDataIsland(bodyName, primaryText, backgroundColor, compact, bodyNavigation, bodyActions) {
            val angles = rows.filter { it.tag == "sun-data-azimuth" || it.tag == "sun-data-elevation" }
            Column(Modifier.fillMaxWidth().testTag("sun-finder-pointing-data")) {
                PlanetCompassSunFinderDataTable(rows - angles.toSet(),
                    style, primaryText, secondaryText, compact, Modifier.fillMaxWidth(), angleRows = angles)
            }
        }
        PlanetCompassSunFinderDataIsland(primaryText, backgroundColor, compact, "sun-finder-environment-data") {
            if (message != null) Text(message, color = primaryText, style = style, fontWeight = FontWeight.Bold)
            if (locationActionLabel != null) TextButton(onClick = onLocationAction,
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)) { Text(locationActionLabel) }
            Row(Modifier.fillMaxWidth().testTag("sun-finder-environment-content"), verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                val compassSize = if (compact) 68.dp else 80.dp
                Column(Modifier.testTag("sun-finder-compass-column").padding(end = 8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally) {
                    PlanetCompassSunFinderCompass(orientation, compassReliable, primaryText, Modifier.size(compassSize))
                }
                Box(Modifier.weight(1f).align(Alignment.CenterVertically).testTag("sun-finder-environment-values")) {
                    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        PlanetCompassSunFinderDataTable(orientationRows + locationRows, style, primaryText, secondaryText, compact,
                            Modifier.fillMaxWidth().testTag("sun-finder-location-data"))
                        // Reserve only the visible text height, not another empty 48 dp row.
                        Spacer(Modifier.height(with(LocalDensity.current) { style.lineHeight.toDp() } + 4.dp))
                    }
                    // The target extends upwards into the non-interactive data area. It remains
                    // fully inside the island, without making the visible column taller.
                    Box(Modifier.align(Alignment.BottomEnd).testTag("sun-finder-info")
                        .defaultMinSize(minWidth = 48.dp, minHeight = 48.dp)
                        .clip(RoundedCornerShape(8.dp)).clickable { info = true }
                        .planetCompassAccessibleAction(detailsLabel, onClick = { info = true }),
                        contentAlignment = Alignment.BottomEnd) {
                        Text(detailsLabel, Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                            color = primaryText, style = style,
                            fontWeight = FontWeight.Medium, textDecoration = TextDecoration.Underline)
                    }
                }
            }
        }
    }
    if (info) PlanetCompassSunFinderModelInfo(hasWeather, weatherText, locationInfoRows, primaryText, secondaryText, backgroundColor) { info = false }
}

@Composable
private fun PlanetCompassSunFinderDataIsland(primaryText: Color, backgroundColor: Color, compact: Boolean,
    tag: String, content: @Composable ColumnScope.() -> Unit) {
    val shape = RoundedCornerShape(20.dp)
    Column(Modifier.fillMaxWidth().testTag(tag).clip(shape)
        .background(backgroundColor.copy(alpha = 0.84f))
        .border(1.dp, primaryText.copy(alpha = 0.12f), shape)
        .padding(horizontal = 12.dp, vertical = if (compact) 8.dp else 10.dp),
        verticalArrangement = Arrangement.spacedBy(if (compact) 3.dp else 5.dp), content = content)
}

@Composable
private fun PlanetCompassSunFinderDataTable(
    rows: List<PlanetCompassSunDataRow>, style: TextStyle, primaryText: Color, secondaryText: Color,
    compact: Boolean, modifier: Modifier, angleRows: List<PlanetCompassSunDataRow> = emptyList()
) {
    val density = LocalDensity.current
    val measurer = rememberTextMeasurer()
    BoxWithConstraints(modifier) {
        val maximumValueWidth = maxWidth * 0.76f
        Column(Modifier.fillMaxWidth()) {
            rows.forEach { row ->
                // Right edges align, but a short speed must not reserve the distance's whole width.
                val valueWidth = with(density) { measurer.measure(row.value, style).size.width.toDp() + 2.dp }
                    .coerceAtMost(maximumValueWidth)
                val valueStyle = if (density.fontScale > 1.4f) style else style.copy(fontSize =
                    fitPlanetCompassButtonFontSize(style.fontSize.value, 10f) { candidate ->
                        !measurer.measure(row.value, style.copy(fontSize = candidate.sp),
                            maxLines = row.value.count { it == '\n' } + 1,
                            softWrap = false, constraints = Constraints(maxWidth = with(density) { valueWidth.roundToPx() }))
                            .hasVisualOverflow
                    }.sp)
                Row(Modifier.fillMaxWidth().padding(vertical = if (compact) 0.dp else 1.dp)
                    .testTag(row.tag).clearAndSetSemantics {
                        contentDescription = row.announcement
                        text = AnnotatedString(row.value)
                    },
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(row.label, Modifier.weight(1f), color = secondaryText, style = style)
                    Text(row.value, Modifier.width(valueWidth), color = primaryText,
                        style = valueStyle,
                        textAlign = TextAlign.End,
                        maxLines = if (row.tag == "celestial-distance" && density.fontScale <= 1.4f) 1 else Int.MAX_VALUE)
                }
            }
            if (angleRows.isNotEmpty()) PlanetCompassCelestialInlineAngles(angleRows, style, primaryText, secondaryText,
                Modifier.padding(vertical = if (compact) 0.dp else 1.dp))
        }
    }
}

@Composable
private fun PlanetCompassSunFinderModelInfo(
    hasWeather: Boolean, weatherText: String, locationInfoRows: List<PlanetCompassSunDataRow>, primaryText: Color,
    secondaryText: Color, backgroundColor: Color, onDismiss: () -> Unit
) {
    val context = LocalContext.current
    PlanetCompassAlertDialog(onDismissRequest = onDismiss, confirmButton = null,
        modifier = Modifier.testTag("sun-finder-model-info"), containerColor = backgroundColor,
        titleContentColor = primaryText, textContentColor = primaryText,
        title = { Text(stringResource(R.string.celestial_environment_info_title)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                if (locationInfoRows.isNotEmpty()) PlanetCompassSunFinderDataTable(locationInfoRows,
                    TextStyle(fontSize = 13.sp, lineHeight = 18.sp, fontFeatureSettings = "tnum"),
                    primaryText, secondaryText, false, Modifier.fillMaxWidth())
                Text(weatherText, Modifier.testTag("celestial-environment-weather"))
                Text(stringResource(R.string.celestial_environment_orientation_note))
                Text(stringResource(R.string.sun_finder_terrain_note))
                Text(stringResource(R.string.sun_weather_privacy))
                if (hasWeather) Text("Open-Meteo · CC BY 4.0", Modifier.clickable {
                    runCatching { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://open-meteo.com/en/licence"))) }
                        .onFailure { PlanetCompassErrorLog.record(context, "sun_finder:weather_attribution", it) }
                }, color = secondaryText, textDecoration = TextDecoration.Underline)
            }
        })
}
