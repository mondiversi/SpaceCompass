package me.mondiversi.spacecompass

import android.icu.text.TimeZoneNames
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.time.Instant
import java.time.ZoneId

/** Searchable, bounded list with the same theme, scale and radio rows as the other settings. */
@Composable
internal fun SpaceCompassObserverZonePicker(value: String, timeMs: Long, enabled: Boolean,
    onSelect: (String) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    var search by remember { mutableStateOf("") }
    val locale = LocalConfiguration.current.locales[0]
    val names = remember(locale) { TimeZoneNames.getInstance(locale) }
    fun label(id: String): String = names.getExemplarLocationName(id)
        ?: id.substringAfterLast('/').replace('_', ' ')
    val selected = remember(value, timeMs) {
        runCatching { ZoneId.of(value).rules.getOffset(Instant.ofEpochMilli(timeMs)) }.getOrNull()
    }
    val foreground = MaterialTheme.colorScheme.onSurface
    val tint = foreground.copy(alpha = if (enabled) 1f else .4f)
    Surface(onClick = { search = ""; expanded = true }, enabled = enabled,
        modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp).testTag("observer-zone"),
        shape = MaterialTheme.shapes.small, color = Color.Transparent,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = if (enabled) 1f else .4f))) {
        Row(Modifier.padding(horizontal = 12.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Column(Modifier.weight(1f)) {
                Text(label(value), color = tint, fontSize = 14.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
                if (label(value) != value) Text(value, color = tint.copy(alpha = .65f), fontSize = 10.sp,
                    maxLines = 1, overflow = TextOverflow.Ellipsis, style = TextStyle(textDirection = TextDirection.Ltr))
            }
            selected?.let { Text(formatSpaceCompassObserverUtcOffset(it), color = tint, fontSize = 12.sp,
                style = TextStyle(textDirection = TextDirection.Ltr), maxLines = 1) }
            Canvas(Modifier.size(16.dp)) {
                val stroke = 1.7.dp.toPx()
                drawLine(tint, Offset(size.width * .2f, size.height * .35f),
                    Offset(size.width * .5f, size.height * .65f), stroke, StrokeCap.Round)
                drawLine(tint, Offset(size.width * .5f, size.height * .65f),
                    Offset(size.width * .8f, size.height * .35f), stroke, StrokeCap.Round)
            }
        }
    }
    if (expanded) {
        val options = remember(timeMs, value) {
            // ICU identifies true aliases; retain distinct geographic zones and their historical rules.
            // An existing selection keeps its exact stored ID instead of silently migrating it.
            spaceCompassObserverTimeZoneChoices(timeMs, value)
                .groupBy { android.icu.util.TimeZone.getCanonicalID(it.id) ?: it.id }
                .values.map { aliases -> aliases.firstOrNull { it.id == value } ?: aliases.first() }
                .sortedWith(compareBy<SpaceCompassObserverTimeZoneChoice> { it.offset.totalSeconds }.thenBy { it.id })
        }
        val visible = remember(options, search, names) { options.filter {
            search.isBlank() || it.id.contains(search.trim(), ignoreCase = true) ||
                label(it.id).contains(search.trim(), ignoreCase = true) ||
                formatSpaceCompassObserverUtcOffset(it.offset).contains(search.trim(), ignoreCase = true)
        } }
        val list = rememberLazyListState(initialFirstVisibleItemIndex =
            options.indexOfFirst { it.id == value }.coerceAtLeast(0))
        LaunchedEffect(search) {
            if (search.isNotBlank()) list.scrollToItem(0)
            else list.scrollToItem(options.indexOfFirst { it.id == value }.coerceAtLeast(0))
        }
        val listHeight = (LocalConfiguration.current.screenHeightDp.dp * .38f).coerceIn(96.dp, 340.dp)
        SpaceCompassAlertDialog(onDismissRequest = { expanded = false }, confirmButton = null,
            modifier = Modifier.testTag("observer-zone-menu"), scrollContent = false,
            title = { Text(stringResource(R.string.observer_zone)) }, text = {
                Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(search, { search = it.take(100) }, Modifier.fillMaxWidth().testTag("observer-zone-search"),
                        label = { Text(stringResource(R.string.observer_zone_search)) }, singleLine = true)
                    if (visible.isEmpty()) Text(stringResource(R.string.observer_zone_no_results),
                        style = MaterialTheme.typography.bodySmall)
                    LazyColumn(Modifier.fillMaxWidth().heightIn(max = listHeight)
                        .selectableGroup().lazyScrollbarOverlay(list, foreground.copy(alpha = .45f))
                        .testTag("observer-zone-options"), state = list, contentPadding = PaddingValues(end = 8.dp)) {
                        items(visible, key = { it.id }) { choice ->
                            val checked = choice.id == value
                            Row(Modifier.fillMaxWidth().heightIn(min = 48.dp).clip(RoundedCornerShape(10.dp))
                                .background(if (checked) foreground.copy(alpha = .07f) else Color.Transparent)
                                .selectable(checked, role = Role.RadioButton, onClick = {
                                    onSelect(choice.id); expanded = false
                                }).padding(horizontal = 8.dp, vertical = 6.dp).testTag("observer-zone-${choice.id}"),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                CompositionLocalProvider(LocalMinimumInteractiveComponentSize provides 0.dp) {
                                    RadioButton(checked, onClick = null, modifier = Modifier.size(24.dp))
                                }
                                Column(Modifier.weight(1f)) {
                                    Text(label(choice.id), fontSize = 14.sp, lineHeight = 17.sp)
                                    Text(choice.id, color = foreground.copy(alpha = .65f), fontSize = 10.sp,
                                        lineHeight = 13.sp, style = TextStyle(textDirection = TextDirection.Ltr))
                                }
                                Text(formatSpaceCompassObserverUtcOffset(choice.offset), fontSize = 11.sp,
                                    color = foreground.copy(alpha = .65f), style = TextStyle(textDirection = TextDirection.Ltr))
                            }
                        }
                    }
                }
            })
    }
}
