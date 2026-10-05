package me.mondiversi.spacecompass

import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.text
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

/** One aligned table row: paired labels on the left, paired angle values on the right. */
@Composable
internal fun SpaceCompassCelestialInlineAngles(rows: List<SpaceCompassSunDataRow>, style: TextStyle,
    primaryText: Color, secondaryText: Color, modifier: Modifier = Modifier) {
    Row(modifier.fillMaxWidth().testTag("celestial-body-angles"),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(rows.joinToString(" / ") { it.label }, Modifier.weight(1f), color = secondaryText, style = style)
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            rows.forEachIndexed { index, row ->
                if (index > 0) Text("/", color = secondaryText, style = style)
                Text(row.value, Modifier.testTag(row.tag).clearAndSetSemantics {
                    contentDescription = row.announcement
                    text = AnnotatedString(row.value)
                }, color = primaryText, style = style, textAlign = TextAlign.End)
            }
        }
    }
}
