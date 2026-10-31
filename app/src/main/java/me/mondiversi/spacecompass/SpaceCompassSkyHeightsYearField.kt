package me.mondiversi.spacecompass

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/** Editable combo uses the ordinary settings field, palette, dropdown and accessible targets. */
@Composable
internal fun SpaceCompassSkyHeightsYearField(year: Int, onYear: (Int) -> Unit,
    onValid: (Boolean) -> Unit, onInteracting: (Boolean) -> Unit, modifier: Modifier = Modifier) {
    val format = rememberSpaceCompassDataExportProfiles().selected.formatting
    fun label(value: Int) = formatSpaceCompassNumber(value.toDouble(), 0, format.numeric,
        grouping = false, systemLocale = format.deviceLocale)
    val selectedLabel = label(year)
    val title = stringResource(R.string.observation_heights_year)
    var text by rememberSaveable { mutableStateOf(selectedLabel) }
    var focused by remember { mutableStateOf(false) }
    var expanded by remember { mutableStateOf(false) }
    val parsed = parseSpaceCompassObservationYear(text)
    val focus = LocalFocusManager.current
    val keyboard = LocalSoftwareKeyboardController.current
    val menuScroll = rememberScrollState()
    val itemHeight = with(LocalDensity.current) { 48.dp.roundToPx() }
    val foreground = MaterialTheme.colorScheme.onSurface
    SideEffect { onValid(parsed == year); onInteracting(focused || expanded) }
    LaunchedEffect(selectedLabel, focused) { if (!focused) text = selectedLabel }
    LaunchedEffect(expanded) {
        if (expanded) menuScroll.scrollTo((year - spaceCompassObservationYears.first - 2).coerceAtLeast(0) * itemHeight)
    }
    Box(modifier) {
        Column {
            SpaceCompassSettingsTextField(text, { next ->
                if (next.length <= 4 && next.all(Char::isDigit)) {
                    text = next
                    parseSpaceCompassObservationYear(next)?.let(onYear)
                }
            }, Modifier.fillMaxWidth().onFocusChanged { focused = it.isFocused }.testTag("sky-heights-year"),
                label = title, trailingIcon = {
                    IconButton(onClick = { focus.clearFocus(); keyboard?.hide(); expanded = true },
                        modifier = Modifier.size(SpaceCompassCompactFieldIconSize)) {
                        Canvas(Modifier.size(16.dp).semantics { contentDescription = title }) {
                            drawLine(foreground, Offset(size.width * .2f, size.height * .35f),
                                Offset(size.width * .5f, size.height * .65f), 1.7.dp.toPx(), StrokeCap.Round)
                            drawLine(foreground, Offset(size.width * .5f, size.height * .65f),
                                Offset(size.width * .8f, size.height * .35f), 1.7.dp.toPx(), StrokeCap.Round)
                        }
                    }
                }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = { focus.clearFocus(); keyboard?.hide() }),
                isError = text.length == 4 && parsed == null)
            if (text.length == 4 && parsed == null) Text(
                label(spaceCompassObservationYears.first) + "–" + label(spaceCompassObservationYears.last),
                color = MaterialTheme.colorScheme.error, fontSize = 11.sp, lineHeight = 14.sp)
        }
        SpaceCompassAdaptiveDropdownMenu(expanded, { expanded = false }, Modifier.width(128.dp),
            scrollState = menuScroll, containerColor = spaceCompassSettingsCardColor()) {
            spaceCompassObservationYears.forEach { option ->
                DropdownMenuItem(text = { Text(label(option), fontSize = 14.sp,
                    fontWeight = if (year == option) FontWeight.SemiBold else FontWeight.Normal,
                    color = if (year == option) MaterialTheme.colorScheme.primary else foreground) },
                    modifier = Modifier.height(48.dp).testTag("sky-heights-year-$option").semantics { selected = year == option },
                    onClick = {
                        onYear(option); text = label(option); expanded = false; focus.clearFocus(); keyboard?.hide()
                    })
            }
        }
    }
}
