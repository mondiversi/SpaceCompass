package me.mondiversi.spacecompass

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

internal data class SpaceCompassCelestialDetailRow(val label: String, val value: String,
    val subtitle: String? = null, val tag: String? = null)

/** Sections fill the same page-inset slot as the model, preserving table typography and RTL. */
@Composable
internal fun SpaceCompassCelestialDetailTable(title: String, icon: String,
    rows: List<SpaceCompassCelestialDetailRow>, primaryText: Color, secondaryText: Color,
    tag: String, note: String? = null, extraContent: (@Composable () -> Unit)? = null) {
    if (rows.isEmpty()) return
    Column(Modifier.fillMaxWidth().testTag(tag)
        .clip(RoundedCornerShape(18.dp)).background(spaceCompassSettingsCardColor()).padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            SpaceCompassSettingsGroupIcon(icon, primaryText)
            Text(title, color = primaryText, fontWeight = FontWeight.SemiBold, fontSize = 13.sp, lineHeight = 17.sp)
        }
        for (row in rows) Row(Modifier.fillMaxWidth(),
            verticalAlignment = if (row.subtitle != null) Alignment.Top else Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(row.label, Modifier.weight(0.52f), color = secondaryText, fontSize = 12.sp, lineHeight = 16.sp)
            Column(Modifier.weight(0.48f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(row.value, modifier = row.tag?.let { Modifier.testTag(it) } ?: Modifier,
                    color = primaryText, fontSize = 12.sp, lineHeight = 16.sp,
                    style = TextStyle(textDirection = TextDirection.ContentOrLtr))
                row.subtitle?.let { Text(it, Modifier.testTag("celestial-view-designation"),
                    color = secondaryText, fontSize = 10.sp, lineHeight = 13.sp,
                    style = TextStyle(textDirection = TextDirection.ContentOrLtr)) }
            }
        }
        extraContent?.invoke()
        note?.let { Text(it, color = secondaryText, fontSize = 10.sp, lineHeight = 13.sp) }
    }
}
