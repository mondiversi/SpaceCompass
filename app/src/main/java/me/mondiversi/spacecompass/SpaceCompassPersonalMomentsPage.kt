package me.mondiversi.spacecompass

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneOffset
import java.util.TimeZone

@Composable
internal fun SpaceCompassPersonalMomentsPage(body: SpaceCompassCelestialBody, timeMs: Long, onBack: () -> Unit) {
    val state = LocalSpaceCompassComparisons.current
    val saved = state?.personalMoments.orEmpty()
    val moments = remember(saved, body) { saved.filter { it.body == body }.sortedWith(spaceCompassPersonalMomentOrder) }
    val secondary = MaterialTheme.colorScheme.onSurfaceVariant
    val formatting = rememberSpaceCompassDataExportProfiles().selected.formatting
    val utc = remember { TimeZone.getTimeZone("UTC") }
    val stamps = remember(moments, formatting) { mutableMapOf<String, String>() }
    var add by rememberSaveable(body) { mutableStateOf(false) }
    var deleteId by rememberSaveable(body) { mutableStateOf<String?>(null) }
    val transfer = rememberSpaceCompassPersonalMomentTransfer(body, state)
    val listState = rememberLazyListState()
    transfer.exportPage?.let { it(); return }
    BackHandler(onBack = onBack)
    Surface(Modifier.fillMaxSize().testTag("page-personal-moments"), color = spaceCompassPageBackground()) {
        Column {
            SpaceCompassPageToolbar(spaceCompassObservationCountLabel(stringResource(R.string.personal_moments_title), moments.size, countFirst = true),
                onBack, titleModifier = Modifier.testTag("personal-moments-toggle-all-title")
                    .spaceCompassTitleClick(enabled = moments.isNotEmpty()) { state?.toggleAllPersonalMoments(body) }) {
                SpaceCompassTitleActionButton(stringResource(R.string.personal_moment_add), { add = true },
                    Modifier.testTag("personal-moment-add"), enabled = state != null && saved.size < SPACE_COMPASS_PERSONAL_MOMENT_LIMIT) {
                    val ink = LocalContentColor.current
                    Canvas(Modifier.size(SpaceCompassTitleActionIconSize)) {
                        val r = size.minDimension * .28f
                        val stroke = SpaceCompassTitleActionIconStrokeWidth.toPx()
                        drawLine(ink, center - Offset(r, 0f), center + Offset(r, 0f), stroke, StrokeCap.Round)
                        drawLine(ink, center - Offset(0f, r), center + Offset(0f, r), stroke, StrokeCap.Round)
                    }
                }
                SpaceCompassTitleActionButton(stringResource(R.string.personal_moments_export), transfer.openExport,
                    Modifier.testTag("personal-moments-export"), enabled = transfer.canExport) {
                    SpaceCompassPanoramaExportIcon(SpaceCompassTitleActionIconStrokeWidth)
                }
                SpaceCompassTitleActionButton(stringResource(R.string.personal_moments_import), transfer.openImport,
                    Modifier.testTag("personal-moments-import"), enabled = transfer.canImport) {
                    SpaceCompassScenarioImportIcon()
                }
            }
            if (moments.isEmpty()) Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(stringResource(R.string.personal_moments_empty), Modifier.padding(16.dp), color = secondary)
            } else LazyColumn(Modifier.fillMaxSize(), state = listState, contentPadding = PaddingValues(horizontal = 10.dp, vertical = 8.dp)) {
                itemsIndexed(moments, key = { _, moment -> moment.id }) { index, moment ->
                    if (index > 0) HorizontalDivider(color = secondary.copy(alpha = .18f))
                    val stamp = stamps.getOrPut(moment.id) {
                        val time = moment.date.atTime(moment.time ?: LocalTime.NOON).toInstant(ZoneOffset.UTC).toEpochMilli()
                        val dateText = formatSpaceCompassDateOnly(time, formatting.date, formatting.deviceLocale,
                            utc, formatting.numeric, formatting.deviceLocale, allowHistorical = true)
                        val timeText = moment.time?.let {
                            formatSpaceCompassTimeOnly(time, formatting.time, formatting.locale, utc,
                                formatting.numeric, formatting.deviceLocale, includeSeconds = false, allowHistorical = true)
                        }
                        listOfNotNull(dateText, timeText).joinToString(" · ")
                    }
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Row(Modifier.weight(1f).heightIn(min = 52.dp).testTag("personal-moment-check-${moment.id}")
                            .toggleable(moment.enabled, role = Role.Checkbox, onValueChange = { state?.togglePersonalMoment(moment.id) })
                            .padding(horizontal = 8.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            Checkbox(moment.enabled, onCheckedChange = null, colors = spaceCompassCheckboxColors())
                            Column {
                                Text(moment.name, style = MaterialTheme.typography.bodyMedium)
                                Text(stamp, style = MaterialTheme.typography.bodySmall, color = secondary)
                            }
                        }
                        SpaceCompassTitleActionButton(stringResource(R.string.personal_moment_delete), { deleteId = moment.id },
                            Modifier.testTag("personal-moment-delete-${moment.id}"), iconColor = SpaceCompassDestructiveActionColor) {
                            SpaceCompassPersonalMomentTrashIcon()
                        }
                    }
                }
            }
        }
    }
    if (add && state != null) SpaceCompassPersonalMomentEditor(body, timeMs, state, onDismiss = { add = false })
    moments.firstOrNull { it.id == deleteId }?.let { moment ->
        SpaceCompassAlertDialog(onDismissRequest = { deleteId = null },
            title = { Text(stringResource(R.string.personal_moment_delete)) }, text = { Text(moment.name) },
            dismissButton = { SpaceCompassDialogActionButton(stringResource(R.string.celestial_remove_cancel), { deleteId = null }) },
            confirmButton = {
                SpaceCompassDialogActionButton(stringResource(R.string.celestial_remove_action), {
                    state?.deletePersonalMoment(moment.id)
                    deleteId = null
                }, Modifier.testTag("personal-moment-delete-confirm"), destructive = true)
            })
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SpaceCompassPersonalMomentEditor(body: SpaceCompassCelestialBody, timeMs: Long,
    state: SpaceCompassTemporalComparisonState, onDismiss: () -> Unit) {
    val zone = spaceCompassObservationZone()
    val startingDate = Instant.ofEpochMilli(timeMs).atZone(zone).toLocalDate()
    var name by rememberSaveable { mutableStateOf("") }
    var date by rememberSaveable { mutableStateOf(startingDate.toString()) }
    var time by rememberSaveable { mutableStateOf("") }
    var showDate by rememberSaveable { mutableStateOf(false) }
    var showTime by rememberSaveable { mutableStateOf(false) }
    val selectedDate = remember(date) { runCatching { LocalDate.parse(date.trim()).takeIf { it.year in 1900..2100 } }.getOrNull() }
    val selectedTime = remember(time) { runCatching { LocalTime.parse(time.trim()).takeIf { it.second == 0 && it.nano == 0 } }.getOrNull() }
    val moment = remember(body, name, selectedDate, selectedTime, time) {
        if (selectedDate == null || time.isNotBlank() && selectedTime == null) null
        else runCatching { SpaceCompassPersonalMoment(body, name.trim(), selectedDate, selectedTime) }.getOrNull()
    }
    val duplicate = moment != null && state.personalMoments.any { it.id == moment.id }
    SpaceCompassAlertDialog(onDismissRequest = onDismiss, title = { Text(stringResource(R.string.personal_moment_add)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                SpaceCompassSettingsTextField(name, { name = it.take(SPACE_COMPASS_PERSONAL_MOMENT_NAME_LIMIT) },
                    Modifier.fillMaxWidth().testTag("personal-moment-name"), label = stringResource(R.string.celestial_view_name),
                    isError = duplicate)
                if (duplicate) SpaceCompassSettingsDescription(stringResource(R.string.personal_moment_duplicate), compact = true)
                SpaceCompassSettingsTextField(date, { date = it.take(10) },
                    Modifier.fillMaxWidth().testTag("personal-moment-date"), label = stringResource(R.string.observer_date),
                    placeholder = "yyyy-MM-dd", isError = date.isNotBlank() && selectedDate == null,
                    trailingIcon = {
                        SpaceCompassTitleActionButton(stringResource(R.string.observer_date), { showDate = true }) {
                            SpaceCompassSettingsGroupIcon("date_format", LocalContentColor.current)
                        }
                    })
                SpaceCompassSettingsTextField(time, { time = it.take(5) },
                    Modifier.fillMaxWidth().testTag("personal-moment-time"), label = stringResource(R.string.personal_moment_time_optional),
                    placeholder = "HH:mm", isError = time.isNotBlank() && selectedTime == null,
                    trailingIcon = {
                        SpaceCompassTitleActionButton(stringResource(R.string.observer_time), { showTime = true }) {
                            SpaceCompassSettingsGroupIcon("time_format", LocalContentColor.current)
                        }
                    })
            }
        }, dismissButton = {
            SpaceCompassDialogActionButton(stringResource(R.string.celestial_remove_cancel), onDismiss, destructive = true)
        }, confirmButton = {
            SpaceCompassDialogActionButton(stringResource(R.string.panorama_save_phone), {
                moment?.let(state::addPersonalMoment)
                onDismiss()
            }, Modifier.testTag("personal-moment-save"), enabled = moment != null && state.canAddPersonalMoment(moment))
        })
    if (showDate) {
        val picker = rememberDatePickerState(selectedDate?.atStartOfDay()?.toInstant(ZoneOffset.UTC)?.toEpochMilli(), yearRange = 1900..2100)
        SpaceCompassAlertDialog(onDismissRequest = { showDate = false }, title = { Text(stringResource(R.string.observer_date)) },
            text = { SpaceCompassObserverPickerScale(360f) { DatePicker(picker, showModeToggle = false) } },
            confirmButton = { SpaceCompassDialogActionButton(stringResource(R.string.observer_apply), {
                picker.selectedDateMillis?.let { date = Instant.ofEpochMilli(it).atZone(ZoneOffset.UTC).toLocalDate().toString() }
                showDate = false
            }) })
    }
    if (showTime) {
        val picker = rememberTimePickerState(selectedTime?.hour ?: 12, selectedTime?.minute ?: 0, is24Hour = true)
        SpaceCompassAlertDialog(onDismissRequest = { showTime = false }, title = { Text(stringResource(R.string.observer_time)) },
            text = { SpaceCompassObserverPickerScale(320f) { TimePicker(picker) } },
            confirmButton = { SpaceCompassDialogActionButton(stringResource(R.string.observer_apply), {
                time = LocalTime.of(picker.hour, picker.minute).toString()
                showTime = false
            }) })
    }
}

@Composable
private fun SpaceCompassPersonalMomentTrashIcon() {
    val ink = LocalContentColor.current
    Canvas(Modifier.size(SpaceCompassTitleActionIconSize)) {
        val stroke = SpaceCompassTitleActionIconStrokeWidth.toPx()
        drawRect(ink, topLeft = Offset(size.width * .28f, size.height * .34f),
            size = Size(size.width * .44f, size.height * .48f), style = Stroke(stroke))
        drawLine(ink, Offset(size.width * .18f, size.height * .31f), Offset(size.width * .82f, size.height * .31f), stroke, StrokeCap.Round)
        drawLine(ink, Offset(size.width * .40f, size.height * .18f), Offset(size.width * .60f, size.height * .18f), stroke, StrokeCap.Round)
    }
}
