package me.mondiversi.spacecompass

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.*
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
internal fun SpaceCompassCatalogSortButton(sort: SpaceCompassCatalogSort, color: Color,
    onSort: (SpaceCompassCatalogSort) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    val choose: (SpaceCompassCatalogSort) -> Unit = { onSort(it); expanded = false }
    Box {
        SpaceCompassTitleActionButton(stringResource(R.string.catalog_sort),
            onClick = { expanded = true }, modifier = Modifier.width(48.dp).testTag("catalog-sort-toggle"),
            iconColor = if (sort == SpaceCompassCatalogSort.CATALOG) color else MaterialTheme.colorScheme.primary) {
            Icon(painterResource(R.drawable.ic_sort), contentDescription = null)
        }
        SpaceCompassAdaptiveDropdownMenu(expanded, onDismissRequest = { expanded = false },
            modifier = Modifier.width(312.dp).selectableGroup().testTag("catalog-sort-menu"),
            containerColor = spaceCompassSettingsCardColor()) {
            DropdownMenuItem(contentPadding = PaddingValues(horizontal = 12.dp),
                text = { Text(stringResource(R.string.catalog_sort_original), fontSize = 13.sp) },
                trailingIcon = { RadioButton(sort == SpaceCompassCatalogSort.CATALOG, onClick = null,
                    modifier = Modifier.clearAndSetSemantics {}) }, onClick = { choose(SpaceCompassCatalogSort.CATALOG) },
                modifier = Modifier.testTag("catalog-sort-CATALOG").semantics {
                    role = Role.RadioButton; selected = sort == SpaceCompassCatalogSort.CATALOG
                })
            SpaceCompassCatalogSortField.entries.forEach { field ->
                HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = .12f))
                val title = stringResource(field.label)
                Row(Modifier.fillMaxWidth().heightIn(min = 48.dp).padding(start = 12.dp, end = 4.dp),
                    verticalAlignment = Alignment.CenterVertically) {
                    Text(title, Modifier.weight(1f).padding(end = 4.dp), fontSize = 12.sp, lineHeight = 15.sp)
                    listOf(false, true).forEach { descending ->
                        val option = SpaceCompassCatalogSort.entries.single { it.field == field && it.descending == descending }
                        CatalogSortDirectionOption(option, sort, title, choose)
                    }
                }
            }
        }
    }
}

@Composable
private fun CatalogSortDirectionOption(option: SpaceCompassCatalogSort, selected: SpaceCompassCatalogSort,
    title: String, onSort: (SpaceCompassCatalogSort) -> Unit) {
    val direction = stringResource(if (option.descending) R.string.catalog_sort_descending else R.string.catalog_sort_ascending)
    val checked = selected == option
    val tint = if (checked) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = .65f)
    Row(Modifier.width(56.dp).heightIn(min = 48.dp)
        .selectable(checked, role = Role.RadioButton, onClick = { onSort(option) })
        .testTag("catalog-sort-${option.name}").semantics { contentDescription = "$title: $direction" },
        horizontalArrangement = Arrangement.spacedBy(4.dp, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically) {
        Canvas(Modifier.size(16.dp)) {
            fun point(x: Float, y: Float) = Offset(size.width * x, size.height * if (option.descending) 1 - y else y)
            val stroke = 1.7.dp.toPx()
            drawLine(tint, point(.5f, .86f), point(.5f, .14f), stroke, StrokeCap.Round)
            drawLine(tint, point(.20f, .44f), point(.5f, .14f), stroke, StrokeCap.Round)
            drawLine(tint, point(.80f, .44f), point(.5f, .14f), stroke, StrokeCap.Round)
        }
        CompositionLocalProvider(LocalMinimumInteractiveComponentSize provides 0.dp) {
            RadioButton(checked, onClick = null, modifier = Modifier.size(24.dp).clearAndSetSemantics {})
        }
    }
}
