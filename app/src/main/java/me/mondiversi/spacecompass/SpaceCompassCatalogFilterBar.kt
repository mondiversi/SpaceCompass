package me.mondiversi.spacecompass

import androidx.compose.foundation.background
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.*
import androidx.compose.ui.state.ToggleableState
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/** Matches UVIR's paired outlined fields and trailing reset action. */
@Composable
internal fun SpaceCompassCatalogFilterBar(types: Set<SpaceCompassCatalogType>, visibility: SpaceCompassCatalogVisibility, visibleCount: Int,
    onType: (SpaceCompassCatalogType) -> Unit, onAllTypes: () -> Unit,
    onVisibility: (SpaceCompassCatalogVisibility) -> Unit, onReset: () -> Unit) {
    val all = stringResource(R.string.catalog_all)
    val resultCount = formatSpaceCompassNumber(visibleCount.toDouble(), 0,
        LocalSpaceCompassNumericFormat.current, grouping = false)
    val typeChoices = listOf("" to all) + SpaceCompassCatalogType.entries.map { it.name to stringResource(it.label) }
    val selectedTypeLabels = typeChoices.filter { it.first in types.map { type -> type.name } }.map { it.second }
    // AnimatedVisibility overlays sibling layouts; keep the separator below the whole panel.
    Column(Modifier.fillMaxWidth()) {
        Column(Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 8.dp).testTag("catalog-filter-bar"),
            verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                CatalogFilterChoice(stringResource(R.string.catalog_filter_type),
                    if (types.isEmpty()) all else selectedTypeLabels.joinToString(", "), typeChoices,
                    Modifier.weight(1f).testTag("catalog-type-menu"), types.map { it.name }.toSet(), true, resultCount) { key ->
                    if (key.isEmpty()) onAllTypes() else onType(SpaceCompassCatalogType.valueOf(key))
                }
                CatalogFilterChoice(stringResource(R.string.catalog_visibility), stringResource(visibility.label),
                    SpaceCompassCatalogVisibility.entries.map { it.name to stringResource(it.label) },
                    Modifier.weight(1f).testTag("catalog-visibility-menu"), null, false, resultCount) { key ->
                    onVisibility(SpaceCompassCatalogVisibility.valueOf(key))
                }
            }
            TextButton(onClick = onReset, enabled = types.isNotEmpty() || visibility != SpaceCompassCatalogVisibility.ALL,
                modifier = Modifier.align(Alignment.End).testTag("catalog-filter-reset")) {
                Text(stringResource(R.string.catalog_reset_filters))
            }
        }
        HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = .12f))
    }
}

@Composable
private fun CatalogFilterChoice(label: String, selected: String, choices: List<Pair<String, String>>,
    modifier: Modifier, selectedKeys: Set<String>?, typeField: Boolean, resultCount: String, onSelect: (String) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    val foreground = MaterialTheme.colorScheme.onSurface
    val labelInset = with(LocalDensity.current) { 14.sp.toDp() / 2 }
    Box(modifier.semantics(mergeDescendants = true) { contentDescription = "$label: $selected ($resultCount)" }) {
        Surface(onClick = { expanded = true }, shape = RoundedCornerShape(10.dp), color = Color.Transparent,
            border = BorderStroke(1.dp, foreground.copy(alpha = .45f)),
            modifier = Modifier.fillMaxWidth().padding(top = labelInset).heightIn(min = 48.dp)) {
            Row(Modifier.padding(horizontal = 10.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                // Reserve the count so long or multiple selections cannot ellipsize it away.
                Row(Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
                    Text(selected, Modifier.weight(1f, fill = false), fontSize = 13.sp,
                        maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text("($resultCount)", Modifier.padding(start = 4.dp), fontSize = 13.sp, maxLines = 1)
                }
                Canvas(Modifier.size(16.dp)) {
                    val stroke = 2.21.dp.toPx()
                    drawLine(foreground, Offset(size.width * .22f, size.height * .38f),
                        Offset(size.width * .50f, size.height * .65f), stroke, StrokeCap.Round)
                    drawLine(foreground, Offset(size.width * .50f, size.height * .65f),
                        Offset(size.width * .78f, size.height * .38f), stroke, StrokeCap.Round)
                }
            }
        }
        Row(Modifier.align(Alignment.TopStart).padding(start = 10.dp, end = 10.dp)
            .background(spaceCompassPageBackground()).padding(horizontal = 4.dp),
            verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            CatalogFilterLabelIcon(typeField, foreground.copy(alpha = .65f))
            Text(label, fontSize = 11.sp, lineHeight = 14.sp, color = foreground.copy(alpha = .65f),
                maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        SpaceCompassAdaptiveDropdownMenu(expanded, onDismissRequest = { expanded = false }, containerColor = spaceCompassSettingsCardColor()) {
            choices.forEach { (key, title) ->
                val checked = selectedKeys?.let { if (key.isEmpty()) it.isEmpty() else key in it }
                DropdownMenuItem(text = { Text(title, maxLines = 3, overflow = TextOverflow.Ellipsis) },
                    trailingIcon = checked?.let { value -> { Checkbox(value, onCheckedChange = null,
                        modifier = Modifier.clearAndSetSemantics {}) } },
                    onClick = { if (selectedKeys == null) expanded = false; onSelect(key) },
                    modifier = Modifier.testTag(if (typeField) "catalog-type-${key.ifEmpty { "all" }}" else "catalog-visibility-$key")
                        .semantics { if (checked != null) {
                            role = Role.Checkbox
                            toggleableState = if (checked) ToggleableState.On else ToggleableState.Off
                        } })
            }
        }
    }
}

@Composable
private fun CatalogFilterLabelIcon(typeField: Boolean, tint: Color) {
    Canvas(Modifier.size(14.dp)) {
        val stroke = androidx.compose.ui.graphics.drawscope.Stroke(1.3.dp.toPx())
        if (typeField) {
            drawCircle(tint, size.minDimension * .30f, style = stroke)
            drawOval(tint, topLeft = Offset(size.width * .06f, size.height * .34f),
                size = androidx.compose.ui.geometry.Size(size.width * .88f, size.height * .32f), style = stroke)
        } else {
            val path = androidx.compose.ui.graphics.Path().apply {
                moveTo(size.width * .08f, size.height * .50f)
                quadraticTo(size.width * .50f, 0f, size.width * .92f, size.height * .50f)
                quadraticTo(size.width * .50f, size.height, size.width * .08f, size.height * .50f)
            }
            drawPath(path, tint, style = stroke)
            drawCircle(tint, size.minDimension * .12f)
        }
    }
}
