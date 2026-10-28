package me.mondiversi.spacecompass

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.*
import androidx.compose.ui.state.ToggleableState
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private enum class CatalogFilterField(val key: String) { TYPE("type"), VISIBILITY("visibility"), ORIGIN("origin"), POINTING("pointing") }

/** Two paired rows share the same outlined fields and trailing reset action. */
@Composable
internal fun SpaceCompassCatalogFilterBar(types: Set<SpaceCompassCatalogType>, visibility: SpaceCompassCatalogVisibility,
    origin: SpaceCompassCatalogOrigin, pointing: SpaceCompassCatalogPointing, visibleCount: Int,
    onType: (SpaceCompassCatalogType) -> Unit, onAllTypes: () -> Unit,
    onVisibility: (SpaceCompassCatalogVisibility) -> Unit, onOrigin: (SpaceCompassCatalogOrigin) -> Unit,
    onPointing: (SpaceCompassCatalogPointing) -> Unit, onReset: () -> Unit, searchActive: Boolean = false,
    availableBodies: List<SpaceCompassCelestialBody> = spaceCompassCelestialCatalogOrder) {
    val all = stringResource(R.string.catalog_all)
    val numeric = LocalSpaceCompassNumericFormat.current
    val resultCount = formatSpaceCompassNumber(visibleCount.toDouble(), 0,
        numeric, grouping = false)
    val locale = LocalConfiguration.current.locales[0]
    val typeLabels = SpaceCompassCatalogType.entries.associateWith { stringResource(it.label) }
    val typeCounts = remember(availableBodies, typeLabels, locale) {
        spaceCompassCatalogTypeCounts(availableBodies, typeLabels, locale)
    }
    fun countLabel(count: Int) = formatSpaceCompassNumber(count.toDouble(), 0, numeric, grouping = false)
    val typeChoices = listOf("" to "$all (${countLabel(availableBodies.size)})") + typeCounts.map {
        it.type.name to "${typeLabels.getValue(it.type)} (${countLabel(it.count)})"
    }
    val selectedTypes = typeCounts.filter { it.type in types }
    val selectedTypeLabels = selectedTypes.map { typeLabels.getValue(it.type) }
    val selectedTypeCount = countLabel(spaceCompassCatalogTypeMatchCount(availableBodies, types))
    // AnimatedVisibility overlays sibling layouts; keep the separator below the whole panel.
    Column(Modifier.fillMaxWidth()) {
        Column(Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 8.dp).testTag("catalog-filter-bar"),
            verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                CatalogFilterChoice(stringResource(R.string.catalog_filter_type),
                    if (types.isEmpty()) all else selectedTypeLabels.joinToString(", "), typeChoices,
                    Modifier.weight(1f).testTag("catalog-type-menu"), types.map { it.name }.toSet(), CatalogFilterField.TYPE, selectedTypeCount) { key ->
                    if (key.isEmpty()) onAllTypes() else onType(SpaceCompassCatalogType.valueOf(key))
                }
                CatalogFilterChoice(stringResource(R.string.catalog_visibility), stringResource(visibility.label),
                    SpaceCompassCatalogVisibility.entries.map { it.name to stringResource(it.label) },
                    Modifier.weight(1f).testTag("catalog-visibility-menu"), null, CatalogFilterField.VISIBILITY, resultCount) { key ->
                    onVisibility(SpaceCompassCatalogVisibility.valueOf(key))
                }
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                CatalogFilterChoice(stringResource(R.string.catalog_origin), stringResource(origin.label),
                    SpaceCompassCatalogOrigin.entries.map { it.name to stringResource(it.label) },
                    Modifier.weight(1f).testTag("catalog-origin-menu"), null, CatalogFilterField.ORIGIN, resultCount) {
                    onOrigin(SpaceCompassCatalogOrigin.valueOf(it))
                }
                CatalogFilterChoice(stringResource(R.string.catalog_pointing), stringResource(pointing.label),
                    SpaceCompassCatalogPointing.entries.map { it.name to stringResource(it.label) },
                    Modifier.weight(1f).testTag("catalog-pointing-menu"), null, CatalogFilterField.POINTING, resultCount) {
                    onPointing(SpaceCompassCatalogPointing.valueOf(it))
                }
            }
            TextButton(onClick = onReset, enabled = types.isNotEmpty() || visibility != SpaceCompassCatalogVisibility.ALL ||
                origin != SpaceCompassCatalogOrigin.ALL || pointing != SpaceCompassCatalogPointing.ALL || searchActive,
                modifier = Modifier.align(Alignment.End).testTag("catalog-filter-reset")) {
                Text(stringResource(R.string.catalog_reset_filters))
            }
        }
        HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = .12f))
    }
}

@Composable
private fun CatalogFilterChoice(label: String, selected: String, choices: List<Pair<String, String>>,
    modifier: Modifier, selectedKeys: Set<String>?, field: CatalogFilterField, resultCount: String, onSelect: (String) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    val foreground = MaterialTheme.colorScheme.onSurface
    Box(modifier.semantics(mergeDescendants = true) { contentDescription = "$label: $selected ($resultCount)" }) {
        Surface(onClick = { expanded = true }, shape = RoundedCornerShape(10.dp), color = Color.Transparent,
            border = BorderStroke(1.dp, foreground.copy(alpha = .45f)),
            modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) {
            Row(Modifier.padding(horizontal = 10.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        CatalogFilterLabelIcon(field, foreground.copy(alpha = .65f))
                        Text(label, Modifier.weight(1f).testTag("catalog-filter-label-${field.key}"),
                            fontSize = 11.sp, lineHeight = 14.sp, color = foreground.copy(alpha = .65f),
                            maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                    // Reserve the count so long or multiple selections cannot ellipsize it away.
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Text(selected, Modifier.weight(1f, fill = false).testTag("catalog-filter-value-${field.key}"),
                            fontSize = 13.sp, lineHeight = 16.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text("($resultCount)", Modifier.padding(start = 4.dp), fontSize = 13.sp, lineHeight = 16.sp, maxLines = 1)
                    }
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
        val menuBackground = spaceCompassSettingsCardColor()
        SpaceCompassAdaptiveDropdownMenu(expanded, onDismissRequest = { expanded = false }, containerColor = menuBackground) {
            choices.forEach { (key, title) ->
                val checked = selectedKeys?.let { if (key.isEmpty()) it.isEmpty() else key in it }
                val rowBackground = if (field == CatalogFilterField.TYPE && key.isNotEmpty())
                    spaceCompassCatalogTypeBackground(SpaceCompassCatalogType.valueOf(key), checked == true, menuBackground)
                    else Color.Transparent
                DropdownMenuItem(text = { Text(title, maxLines = 3, overflow = TextOverflow.Ellipsis) },
                    trailingIcon = checked?.let { value -> { Checkbox(value, onCheckedChange = null,
                        modifier = Modifier.clearAndSetSemantics {}) } },
                    onClick = { if (selectedKeys == null) expanded = false; onSelect(key) },
                    modifier = Modifier.background(rowBackground).testTag("catalog-${field.key}-${key.ifEmpty { "all" }}")
                        .semantics { if (checked != null) {
                            role = Role.Checkbox
                            toggleableState = if (checked) ToggleableState.On else ToggleableState.Off
                        } })
            }
        }
    }
}

@Composable
private fun CatalogFilterLabelIcon(field: CatalogFilterField, tint: Color) {
    Canvas(Modifier.size(14.dp)) {
        val stroke = androidx.compose.ui.graphics.drawscope.Stroke(1.3.dp.toPx())
        when (field) {
            CatalogFilterField.TYPE -> {
                drawCircle(tint, size.minDimension * .30f, style = stroke)
                drawOval(tint, topLeft = Offset(size.width * .06f, size.height * .34f),
                    size = androidx.compose.ui.geometry.Size(size.width * .88f, size.height * .32f), style = stroke)
            }
            CatalogFilterField.VISIBILITY -> {
                val path = androidx.compose.ui.graphics.Path().apply {
                    moveTo(size.width * .08f, size.height * .50f)
                    quadraticTo(size.width * .50f, 0f, size.width * .92f, size.height * .50f)
                    quadraticTo(size.width * .50f, size.height, size.width * .08f, size.height * .50f)
                }
                drawPath(path, tint, style = stroke)
                drawCircle(tint, size.minDimension * .12f)
            }
            CatalogFilterField.ORIGIN -> {
                drawCircle(tint, size.minDimension * .38f, style = stroke)
                drawOval(tint, topLeft = Offset(size.width * .32f, size.height * .12f),
                    size = androidx.compose.ui.geometry.Size(size.width * .36f, size.height * .76f), style = stroke)
                drawLine(tint, Offset(size.width * .12f, size.height * .50f),
                    Offset(size.width * .88f, size.height * .50f), stroke.width)
            }
            CatalogFilterField.POINTING -> {
                drawCircle(tint, size.minDimension * .27f, style = stroke)
                drawCircle(tint, size.minDimension * .06f)
                for (i in 0..3) {
                    val angle = i * Math.PI / 2
                    fun point(radius: Float) = center + Offset(kotlin.math.cos(angle).toFloat(), kotlin.math.sin(angle).toFloat()) * size.minDimension * radius
                    drawLine(tint, point(.32f), point(.48f), stroke.width, StrokeCap.Round)
                }
            }
        }
    }
}
