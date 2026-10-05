package me.mondiversi.spacecompass

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
internal fun SpaceCompassCatalogSortButton(sort: SpaceCompassCatalogSort, color: Color,
    onSort: (SpaceCompassCatalogSort) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    Box {
        SpaceCompassTitleActionButton(stringResource(R.string.catalog_sort),
            onClick = { expanded = true }, modifier = Modifier.width(48.dp).testTag("catalog-sort-toggle"),
            iconColor = if (sort == SpaceCompassCatalogSort.CATALOG) color else MaterialTheme.colorScheme.primary) {
            Icon(painterResource(R.drawable.ic_sort), contentDescription = null)
        }
        SpaceCompassAdaptiveDropdownMenu(expanded, onDismissRequest = { expanded = false }, containerColor = spaceCompassSettingsCardColor()) {
            CatalogSortOption(SpaceCompassCatalogSort.CATALOG, sort,
                stringResource(R.string.catalog_sort_original)) { onSort(it); expanded = false }
            HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = .12f))
            val alphabetical = stringResource(R.string.catalog_sort_alphabetical)
            val distance = stringResource(R.string.catalog_distance_from_sun)
            listOf(alphabetical to listOf(SpaceCompassCatalogSort.NAME_ASC, SpaceCompassCatalogSort.NAME_DESC),
                distance to listOf(SpaceCompassCatalogSort.DISTANCE_ASC, SpaceCompassCatalogSort.DISTANCE_DESC))
                .forEachIndexed { index, (title, options) ->
                    if (index > 0) HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = .12f))
                    Text(title, Modifier.padding(horizontal = 12.dp, vertical = 6.dp).semantics { heading() },
                        fontSize = 12.sp, fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = .65f))
                    options.forEach { option ->
                        val label = stringResource(if (option == SpaceCompassCatalogSort.NAME_ASC || option == SpaceCompassCatalogSort.DISTANCE_ASC)
                            R.string.catalog_sort_ascending else R.string.catalog_sort_descending)
                        CatalogSortOption(option, sort, label, "$title: $label") { onSort(it); expanded = false }
                    }
                }
        }
    }
}

@Composable
private fun CatalogSortOption(option: SpaceCompassCatalogSort, selected: SpaceCompassCatalogSort,
    label: String, description: String = label, onSort: (SpaceCompassCatalogSort) -> Unit) {
    DropdownMenuItem(contentPadding = PaddingValues(horizontal = 12.dp), text = { Text(label) }, trailingIcon = {
        RadioButton(selected == option, onClick = null, modifier = Modifier.clearAndSetSemantics {})
    }, onClick = { onSort(option) }, modifier = Modifier.testTag("catalog-sort-${option.name}").semantics {
        contentDescription = description
        role = Role.RadioButton
        this.selected = selected == option
    })
}
