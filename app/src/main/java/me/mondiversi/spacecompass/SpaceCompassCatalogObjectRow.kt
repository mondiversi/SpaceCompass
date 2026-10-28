package me.mondiversi.spacecompass

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.indication
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/** Selection, observation and the passive value occupy separate touch regions. */
@Composable
internal fun SpaceCompassCatalogObjectRow(
    body: SpaceCompassCelestialBody, name: String, checked: Boolean, value: String, color: Color,
    onToggle: () -> Unit, onObserve: () -> Unit, rowBackground: Color = MaterialTheme.colorScheme.surface
) {
    val observationInteraction = remember { MutableInteractionSource() }
    // Share observation feedback across the row without expanding its clickable area.
    Row(Modifier.fillMaxWidth().heightIn(min = 48.dp)
        .background(spaceCompassCatalogRowBackground(body, checked, rowBackground))
        .indication(observationInteraction, ripple(color = color))
        .padding(start = 12.dp, end = 16.dp)
        .testTag("celestial-body-${body.name}"),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        // Match the fixed header's checkbox centre, preserving a full touch target.
        // The following gap keeps that target outside the thumbnail/name action.
        Box(Modifier.width(24.dp).height(48.dp), contentAlignment = Alignment.Center) {
            Checkbox(checked, onCheckedChange = { onToggle() }, colors = spaceCompassCheckboxColors(),
                modifier = Modifier.requiredSize(48.dp).testTag("catalog-check-${body.name}")
                    .semantics { contentDescription = name })
        }
        Row(Modifier.weight(1f).heightIn(min = 48.dp)
            .clickable(interactionSource = observationInteraction, indication = null,
                role = Role.Button, onClickLabel = stringResource(R.string.celestial_details_title),
                onClick = onObserve)
            .testTag("catalog-observe-${body.name}").padding(vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            SpaceCompassCelestialThumbnail(body, Modifier.size(24.dp)
                .testTag("catalog-thumbnail-${body.name}").then(
                    if (checked) Modifier.border(2.dp, spaceCompassCelestialPathTint(body), CircleShape)
                    else Modifier))
            Text(name, Modifier.weight(1f).testTag("catalog-name-${body.name}"), color = color,
                fontSize = 14.sp, lineHeight = 16.sp, softWrap = true,
                fontWeight = if (checked) FontWeight.Bold else FontWeight.Normal)
        }
        Text(value, Modifier.widthIn(max = 144.dp).testTag("catalog-value-${body.name}"),
            textAlign = TextAlign.End, color = color.copy(alpha = .72f), fontSize = 11.sp, lineHeight = 14.sp)
    }
}
