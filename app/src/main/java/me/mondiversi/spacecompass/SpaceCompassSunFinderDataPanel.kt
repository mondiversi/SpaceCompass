package me.mondiversi.spacecompass

import androidx.activity.compose.BackHandler
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.text.style.TextDirection
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
internal fun SpaceCompassSunFinderDataPanel(
    rows: List<SpaceCompassSunDataRow>, locationRows: List<SpaceCompassSunDataRow>,
    orientation: SpaceCompassSunOrientation?, compassReliable: Boolean,
    weatherText: String,
    hasWeather: Boolean, primaryText: Color, secondaryText: Color, backgroundColor: Color,
    modifier: Modifier, compact: Boolean,
    bodyName: String? = null, bodyNavigation: SpaceCompassCelestialBodyNavigation? = null,
    orientationRows: List<SpaceCompassSunDataRow> = emptyList(),
    locationInfoRows: List<SpaceCompassSunDataRow> = emptyList(),
    onInfo: () -> Unit = {},
    bodyActions: @Composable () -> Unit = {}
) {
    val scroll = rememberScrollState()
    val style = TextStyle(fontSize = if (compact) 11.sp else 12.sp,
        lineHeight = if (compact) 14.sp else 16.sp, fontFeatureSettings = "tnum")
    val detailsLabel = stringResource(R.string.celestial_details_compact)
    Column(modifier.testTag("sun-finder-details")
        // Normally fits without scrolling. Keep a fallback for large accessibility fonts/windows.
        .scrollbarOverlay(scroll, secondaryText.copy(alpha = 0.46f))
        .verticalScroll(scroll), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        if (rows.isNotEmpty()) SpaceCompassCelestialBodyDataIsland(bodyName, primaryText, backgroundColor, compact, bodyNavigation, bodyActions) {
            val angles = rows.filter { it.tag == "sun-data-azimuth" || it.tag == "sun-data-elevation" }
            Column(Modifier.fillMaxWidth().testTag("sun-finder-pointing-data")) {
                SpaceCompassSunFinderDataTable(rows - angles.toSet(),
                    style, primaryText, secondaryText, compact, Modifier.fillMaxWidth(), angleRows = angles)
            }
        }
        SpaceCompassSunFinderDataIsland(primaryText, backgroundColor, compact, "sun-finder-environment-data") {
            Row(Modifier.fillMaxWidth().testTag("sun-finder-environment-content"), verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                val compassSize = if (compact) 68.dp else 80.dp
                Column(Modifier.testTag("sun-finder-compass-column").padding(end = 8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally) {
                    SpaceCompassSunFinderCompass(orientation, compassReliable, primaryText, Modifier.size(compassSize))
                }
                Box(Modifier.weight(1f).align(Alignment.CenterVertically).testTag("sun-finder-environment-values")) {
                    Column(Modifier.fillMaxWidth().heightIn(min = 48.dp)
                        .clip(RoundedCornerShape(8.dp)).clickable { onInfo() }
                        .spaceCompassAccessibleAction(detailsLabel, onClick = { onInfo() }),
                        verticalArrangement = Arrangement.spacedBy(2.dp, Alignment.CenterVertically)) {
                        SpaceCompassSunFinderDataTable(orientationRows + locationRows, style, primaryText, secondaryText, compact,
                            Modifier.fillMaxWidth().testTag("sun-finder-location-data"))

                        Box(Modifier.fillMaxWidth().testTag("sun-finder-info"),
                            contentAlignment = Alignment.TopStart) {
                            Text(detailsLabel, Modifier.padding(vertical = 2.dp), color = primaryText,
                                maxLines = 1, softWrap = false, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                                style = style, fontWeight = FontWeight.Medium,
                                textDecoration = TextDecoration.Underline)
                        }
                    }

                }
            }

        }
    }
}

@Composable
private fun SpaceCompassSunFinderDataIsland(primaryText: Color, backgroundColor: Color, compact: Boolean,
    tag: String, content: @Composable ColumnScope.() -> Unit) {
    val shape = RoundedCornerShape(20.dp)
    Column(Modifier.fillMaxWidth().testTag(tag).clip(shape)
        .background(backgroundColor.copy(alpha = 0.84f))
        .border(1.dp, primaryText.copy(alpha = 0.12f), shape)
        .padding(horizontal = 12.dp, vertical = if (compact) 8.dp else 10.dp),
        verticalArrangement = Arrangement.spacedBy(if (compact) 3.dp else 5.dp), content = content)
}

@Composable
private fun SpaceCompassSunFinderDataTable(
    rows: List<SpaceCompassSunDataRow>, style: TextStyle, primaryText: Color, secondaryText: Color,
    compact: Boolean, modifier: Modifier, angleRows: List<SpaceCompassSunDataRow> = emptyList()
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
                val valueStyle = if (density.fontScale > 1.4f || row.tag == "sun-info-estimated-place") style else style.copy(fontSize =
                    fitSpaceCompassButtonFontSize(style.fontSize.value, 10f) { candidate ->
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
                        style = valueStyle.copy(textDirection = TextDirection.ContentOrLtr),
                        textAlign = TextAlign.End,
                        maxLines = if (row.tag == "celestial-distance" && density.fontScale <= 1.4f) 1 else Int.MAX_VALUE)
                }
            }
            if (angleRows.isNotEmpty()) SpaceCompassCelestialInlineAngles(angleRows, style, primaryText, secondaryText,
                Modifier.padding(vertical = if (compact) 0.dp else 1.dp))
        }
    }
}

@Composable
internal fun SpaceCompassSunFinderModelInfo(
    hasWeather: Boolean, weatherRow: SpaceCompassSunDataRow, locationInfoRows: List<SpaceCompassSunDataRow>, primaryText: Color,
    secondaryText: Color, backgroundColor: Color, latitude: Double? = null, longitude: Double? = null,
    onDismiss: () -> Unit
) {
    SpaceCompassPositionDetails(hasWeather, weatherRow, locationInfoRows, latitude, longitude, secondaryText.copy(alpha = 0.46f), onDismiss)
}
