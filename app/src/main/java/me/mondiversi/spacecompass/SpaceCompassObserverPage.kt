package me.mondiversi.spacecompass

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridItemSpan
import androidx.compose.foundation.lazy.staggeredgrid.rememberLazyStaggeredGridState
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.*
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.*
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.contentDescription
import kotlinx.coroutines.*
import java.time.*
import java.time.format.DateTimeFormatter
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
internal fun SpaceCompassObserverPage(modifier: Modifier = Modifier, onBack: () -> Unit = {}) {
    val observer = LocalSpaceCompassObserver.current ?: return
    key(observer.editorGeneration) { SpaceCompassObserverEditor(observer, modifier, onBack) }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
private fun SpaceCompassObserverEditor(observer: SpaceCompassObserverState, modifier: Modifier, onBack: () -> Unit) {
    val context = LocalContext.current
    val resources = LocalResources.current
    val rejectSettingsExit = LocalSpaceCompassSettingsExitFailure.current
    val feet = LocalSpaceCompassUnits.current.feet
    val numeric = LocalSpaceCompassNumericFormat.current
    fun displayHeight(meters: Double) = formatSpaceCompassNumber(if (feet) meters / .3048 else meters, 2, numeric, grouping = false)
    val scope = rememberCoroutineScope()
    val islandScroll = rememberLazyStaggeredGridState()
    val islandReveal = rememberSpaceCompassObserverIslandReveal(islandScroll)
    val initial = remember { observer.savedPlan }
    val device = observer.devicePlace
    val initialMoment = remember { (observer.plan ?: initial)?.editorMoment(System.currentTimeMillis(), ZoneId.systemDefault())
        ?: Instant.ofEpochMilli(System.currentTimeMillis()).atZone(ZoneId.systemDefault()) }
    var advanceTime by rememberSaveable { mutableStateOf(initial?.advanceTime == true) }
    var clockOffset by rememberSaveable { mutableStateOf(initial?.clockOffsetMs) }
    var simulatePosition by rememberSaveable { mutableStateOf(observer.plan?.simulatePosition == true) }
    var simulateDate by rememberSaveable { mutableStateOf(observer.plan?.simulateDate == true) }
    var simulateTime by rememberSaveable { mutableStateOf(observer.plan?.simulateTime == true) }
    var simulateAltitude by rememberSaveable { mutableStateOf(observer.plan?.simulateAltitude == true) }
    var simulateZone by rememberSaveable { mutableStateOf(observer.plan?.simulateZone == true) }
    var latitude by rememberSaveable { mutableStateOf((initial?.latitude ?: device?.latitude ?: 0.0).toString()) }
    var longitude by rememberSaveable { mutableStateOf((initial?.longitude ?: device?.longitude ?: 0.0).toString()) }
    var altitude by rememberSaveable { mutableStateOf(displayHeight(initial?.altitudeMeters ?: device?.altitude ?: 0.0)) }
    var zone by rememberSaveable { mutableStateOf(initial?.zoneId ?: ZoneId.systemDefault().id) }
    var automaticAltitude by rememberSaveable { mutableStateOf(if (initial != null) initial.automaticAltitudeMeters else device?.altitude) }
    var automaticZone by rememberSaveable { mutableStateOf(if (initial != null) initial.automaticZoneId else device?.let { ZoneId.systemDefault().id }) }
    var altitudeEdited by rememberSaveable { mutableStateOf(initial != null) }
    var zoneEdited by rememberSaveable { mutableStateOf(initial != null) }
    var pendingMoment by rememberSaveable { mutableStateOf<Long?>(null) }
    var preservedMoment by rememberSaveable { mutableStateOf(if (observer.plan?.scenarioClock != null || initial?.advancesClock == true)
        initialMoment.toInstant().toEpochMilli() else initial?.timeOverrideMs) }
    var date by rememberSaveable { mutableStateOf(initialMoment.toLocalDate().toString()) }
    var time by rememberSaveable { mutableStateOf(initialMoment.toLocalTime().format(DateTimeFormatter.ofPattern("HH:mm"))) }
    var search by rememberSaveable { mutableStateOf("") }
    var busy by remember { mutableStateOf(false) }
    var notice by remember { mutableIntStateOf(0) }
    var lookup by remember { mutableStateOf<Job?>(null) }
    var lookupRevision by remember { mutableIntStateOf(0) }
    var metadataField by remember { mutableStateOf<SpaceCompassObserverMetadataField?>(null) }
    var showMap by rememberSaveable { mutableStateOf(false) }
    var showDate by remember { mutableStateOf(false) }
    var showTime by remember { mutableStateOf(false) }
    var editRevision by rememberSaveable { mutableIntStateOf(0) }
    var validationError by remember { mutableStateOf(false) }
    var needsMetadata by rememberSaveable { mutableStateOf(false) }
    fun edited() { editRevision++; validationError = false }
    fun number(value: String) = spaceCompassObserverInputNumber(value)
    // Warm and persist the administrative label while choosing/editing a scenario position.
    rememberSpaceCompassEstimatedPlace(number(latitude), number(longitude),
        enabled = simulatePosition && !LocalSpaceCompassScreensaverActive.current)
    fun effectiveZone() = when {
        simulateZone -> zone.trim()
        simulatePosition -> automaticZone.orEmpty()
        else -> ZoneId.systemDefault().id
    }
    fun preserveMoment(moment: Long?) {
        val liveClock = observer.plan?.scenarioClock
        val advancing = liveClock != null || simulateTime && advanceTime && clockOffset != null
        if ((!simulateDate || !simulateTime) && !advancing) { pendingMoment = null; preservedMoment = null; return }
        val selected = liveClock?.at(System.currentTimeMillis()) ?:
            if (advancing) System.currentTimeMillis() + requireNotNull(clockOffset) else moment
        selected?.let { ms -> runCatching { Instant.ofEpochMilli(ms).atZone(ZoneId.of(effectiveZone())) }
            .getOrNull()?.let {
                if (simulateDate) date = it.toLocalDate().toString()
                time = it.toLocalTime().format(DateTimeFormatter.ofPattern("HH:mm"))
                preservedMoment = ms
                pendingMoment = null
            } }
    }
    fun invalidateMetadata() {
        if (pendingMoment == null) pendingMoment = preservedMoment ?: spaceCompassObserverMoment(date.trim(), time.trim(), effectiveZone())
        automaticAltitude = null; automaticZone = null; needsMetadata = true
    }
    fun currentDraft() = SpaceCompassObserverDraft(simulatePosition, simulateTime,
        latitude, longitude, altitude, zone, date, time, feet, simulateAltitude, simulateZone,
        automaticAltitude, automaticZone, preservedMoment, simulateDate, advanceTime, clockOffset)
    val savedDraft = remember { mutableStateOf(currentDraft()) }
    fun saveEditedDraft(leaving: Boolean = false) {
        if (editRevision == 0) return
        if (busy || needsMetadata) {
            if (leaving) {
                rejectSettingsExit()
                showSpaceCompassBottomMessage(context, resources.getString(R.string.observer_not_saved))
            }
            return
        }
        val editing = currentDraft()
        val resolved = editing.resolve(observer.savedPlan, System.currentTimeMillis(), ZoneId.systemDefault().id)
        if (resolved.isFailure) {
            validationError = true
            if (leaving) {
                rejectSettingsExit()
                showSpaceCompassBottomMessage(context, resources.getString(R.string.observer_not_saved))
            }
        } else {
            val previousDraft = savedDraft.value
            val unchangedMoment = editing.date == previousDraft.date && editing.time == previousDraft.time &&
                editing.simulateDate == previousDraft.simulateDate && editing.simulateTime == previousDraft.simulateTime &&
                editing.advanceTime == previousDraft.advanceTime && editing.zone == previousDraft.zone &&
                editing.simulateZone == previousDraft.simulateZone
            val nextPlan = resolved.getOrNull()?.let { value -> observer.savedPlan?.takeIf { unchangedMoment }?.let { old ->
                value.copy(timeMs = old.timeMs, momentZoneId = old.momentZoneId,
                    advanceTime = old.advanceTime, clockOffsetMs = old.clockOffsetMs)
            } ?: value }
            clockOffset = nextPlan?.clockOffsetMs ?: clockOffset
            observer.apply(nextPlan)
            savedDraft.value = editing
            editRevision = 0
            validationError = false
        }
    }
    fun applyClockEdit(value: SpaceCompassObserverDraft) {
        date = value.date; time = value.time; advanceTime = value.advanceTime
        preservedMoment = value.preservedMomentMs; clockOffset = value.clockOffsetMs; pendingMoment = null
        edited(); saveEditedDraft()
    }
    // A short pause batches typing; opening the editor alone never rewrites a stored plan.
    val draft = currentDraft()
    LaunchedEffect(draft, editRevision, busy) {
        if (editRevision > 0 && !busy) {
            delay(SPACE_COMPASS_OBSERVER_AUTOSAVE_DELAY_MS)
            saveEditedDraft()
        }
    }
    // Flush the latest complete values even when Back follows an edit before the debounce expires.
    val saveOnExit by rememberUpdatedState({ saveEditedDraft(leaving = true) })
    DisposableEffect(observer) { onDispose { saveOnExit() } }
    suspend fun metadata(a: Double, b: Double, overwriteManual: Boolean = false) {
        try {
            val result = lookupSpaceCompassObserverMetadata(a, b)
            currentCoroutineContext().ensureActive()
            automaticAltitude = result.altitude; automaticZone = result.zoneId
            if (overwriteManual || !altitudeEdited) altitude = displayHeight(result.altitude)
            if (overwriteManual || !zoneEdited) zone = result.zoneId
            preserveMoment(pendingMoment)
            // Completion must reschedule autosave even if a previous pending draft was valid.
            edited()
            notice = R.string.observer_terrain_note
        } catch (cancelled: CancellationException) { throw cancelled
        } catch (_: Exception) { notice = R.string.observer_lookup_failed }
    }
    fun choosePlace(a: Double, b: Double, overwriteManual: Boolean = false) {
        lookup?.cancel()
        val request = ++lookupRevision
        metadataField = null
        invalidateMetadata()
        needsMetadata = false
        latitude = a.toString(); longitude = b.toString(); edited()
        lookup = scope.launch { busy = true; notice = 0
            try { metadata(a, b, overwriteManual) } finally { if (request == lookupRevision) busy = false }
        }
    }
    fun detectMetadata(field: SpaceCompassObserverMetadataField, currentPosition: Boolean) {
        val point = if (currentPosition) observer.devicePlace ?: return else {
            if (!simulatePosition) return
            SpaceCompassDevicePlace(number(latitude) ?: return, number(longitude) ?: return, null)
        }
        if (!point.latitude.isFinite() || !point.longitude.isFinite() ||
            point.latitude !in -90.0..90.0 || point.longitude !in -180.0..180.0) return
        lookup?.cancel()
        val request = ++lookupRevision
        metadataField = field; busy = true; notice = 0
        lookup = scope.launch {
            try {
                val result = lookupSpaceCompassObserverMetadata(point.latitude, point.longitude)
                currentCoroutineContext().ensureActive()
                if (field == SpaceCompassObserverMetadataField.ALTITUDE && !simulateAltitude ||
                    field == SpaceCompassObserverMetadataField.ZONE && !simulateZone) return@launch
                val changed = currentDraft().withDetectedMetadata(field, result, numeric, ZoneId.systemDefault().id)
                when (field) {
                    SpaceCompassObserverMetadataField.ALTITUDE -> {
                        altitude = changed.altitude; altitudeEdited = true
                    }
                    SpaceCompassObserverMetadataField.ZONE -> {
                        zone = changed.zone; zoneEdited = true
                        date = changed.date; time = changed.time
                        preservedMoment = changed.preservedMomentMs; pendingMoment = null
                    }
                }
                edited()
            } catch (cancelled: CancellationException) { throw cancelled
            } catch (_: Exception) { notice = R.string.observer_metadata_failed
            } finally { if (request == lookupRevision) busy = false }
        }
    }
    val selectedCoordinatesValid = simulatePosition && number(latitude)?.let { it in -90.0..90.0 } == true &&
        number(longitude)?.let { it in -180.0..180.0 } == true
    val deviceCoordinatesValid = device?.let { it.latitude.isFinite() && it.longitude.isFinite() &&
        it.latitude in -90.0..90.0 && it.longitude in -180.0..180.0 } == true
    val zonePreviewNow = System.currentTimeMillis()
    val zonePreviewMoment = currentDraft().resolve(observer.savedPlan, zonePreviewNow, ZoneId.systemDefault().id)
        .getOrNull()?.observationTimeMs(zonePreviewNow) ?: zonePreviewNow
    // A completed coordinate edit invalidates terrain/zone until the matching lookup succeeds.
    LaunchedEffect(latitude, longitude, simulatePosition, needsMetadata) {
        if (simulatePosition && needsMetadata) {
            delay(SPACE_COMPASS_OBSERVER_METADATA_DELAY_MS)
            val a = number(latitude); val b = number(longitude)
            if (a != null && b != null && a.isFinite() && b.isFinite() &&
                a in -90.0..90.0 && b in -180.0..180.0) {
                choosePlace(a, b)
            } else needsMetadata = false
        }
    }
    Column(modifier.fillMaxSize().testTag("observer-page")) {
        SpaceCompassPageToolbar(stringResource(R.string.observer_title), onBack) {
            SpaceCompassScenarioResumeButton(observer)
            SpaceCompassScenarioTransferActions(capture = {
                saveEditedDraft()
                if (editRevision != 0 || busy || needsMetadata) {
                    showSpaceCompassBottomMessage(context, resources.getString(R.string.observer_not_saved))
                    null
                } else {
                    val archive = SpaceCompassScenarioArchive.capture(observer, resources.getString(R.string.observer_title))
                    val namedImport = observer.archiveName?.takeUnless {
                        it == resources.getString(R.string.observer_title) || it == "Scenario"
                    }
                    archive.copy(name = namedImport ?: resources.getString(R.string.observer_title))
                }
            }, onImport = { archive ->
                // Discard old pending work before replacing the editor; disposal must not overwrite the imported file.
                lookup?.cancel(); lookupRevision++; editRevision = 0
                observer.importArchive(archive)
            }, enabled = !busy)
        }
        SpaceCompassIslandGrid(Modifier.weight(1f), scroll = islandScroll) {
            item(key = "position") {
                SpaceCompassSettingsToggleIsland(stringResource(R.string.observer_place), iconKey = "coordinates",
                    checked = simulatePosition, toggleDescription = stringResource(R.string.observer_enable_position),
                    onCheckedChange = { checked ->
                        val selected = preservedMoment ?: spaceCompassObserverMoment(date.trim(), time.trim(),
                            effectiveZone())
                        simulatePosition = checked
                        islandReveal.changed(SpaceCompassObserverSection.POSITION, checked)
                        // Switching the location source changes the displayed zone, not a chosen instant.
                        preserveMoment(selected)
                        if (checked && (initial == null || automaticAltitude == null || automaticZone == null)) {
                            pendingMoment = selected; needsMetadata = true
                        }
                        if (!checked) {
                            lookup?.cancel(); lookupRevision++; busy = false; metadataField = null; notice = 0; showMap = false; needsMetadata = false; pendingMoment = null
                        }
                        edited(); saveEditedDraft()
                    }, modifier = Modifier.testTag("observer-enabled-position")) {
                    SpaceCompassSettingsTextField(search, { search = it.take(200) }, Modifier.fillMaxWidth().testTag("observer-search"),
                        label = stringResource(R.string.observer_search), enabled = !busy)
                    FlowRow(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(SpaceCompassSettingsControlGap)) {
                        TextButton(onClick = {
                            lookup?.cancel(); val request = ++lookupRevision; metadataField = null
                            lookup = scope.launch {
                                busy = true; notice = 0
                                try {
                                    val found = searchSpaceCompassObserverPlace(context.applicationContext, search, resources.configuration.locales[0])
                                    if (found == null) notice = R.string.observer_lookup_failed else {
                                        needsMetadata = false
                                        latitude = found.latitude.toString(); longitude = found.longitude.toString(); edited()
                                        metadata(found.latitude, found.longitude)
                                    }
                                } catch (cancelled: CancellationException) { throw cancelled
                                } catch (_: Exception) { notice = R.string.observer_lookup_failed
                                } finally { if (request == lookupRevision) busy = false }
                            }
                        }, enabled = !busy && search.isNotBlank()) {
                            Text(stringResource(R.string.observer_search_action))
                        }
                        TextButton(onClick = { showMap = true }, enabled = !busy,
                            modifier = Modifier.testTag("observer-open-map")) {
                            Text(stringResource(R.string.observer_map))
                        }
                    }
                    SpaceCompassSettingsTextField(latitude, { if (latitude != it) { invalidateMetadata(); latitude = it; edited() } }, Modifier.fillMaxWidth().testTag("observer-latitude"),
                        label = stringResource(R.string.observer_latitude) + " (°)", enabled = !busy)
                    SpaceCompassSettingsTextField(longitude, { if (longitude != it) { invalidateMetadata(); longitude = it; edited() } }, Modifier.fillMaxWidth().testTag("observer-longitude"),
                        label = stringResource(R.string.observer_longitude) + " (°)", enabled = !busy)
                    if (busy && metadataField == null) LinearProgressIndicator(Modifier.fillMaxWidth())
                    if (metadataField == null && notice != 0 && notice != R.string.observer_terrain_note)
                        Text(stringResource(notice), style = MaterialTheme.typography.bodySmall)
                    SpaceCompassSettingsDescription(stringResource(R.string.observer_privacy))
                }
            }
            item(key = "altitude") {
                SpaceCompassSettingsToggleIsland(stringResource(R.string.observer_altitude), iconKey = "altitude",
                    checked = simulateAltitude, toggleDescription = stringResource(R.string.observer_enable_altitude),
                    onCheckedChange = { checked ->
                        simulateAltitude = checked
                        islandReveal.changed(SpaceCompassObserverSection.ALTITUDE, checked)
                        if (!checked && metadataField == SpaceCompassObserverMetadataField.ALTITUDE) {
                            lookup?.cancel(); lookupRevision++; busy = false; notice = 0
                        }
                        if (checked) altitudeEdited = true
                        if (!checked && simulatePosition && automaticAltitude == null) needsMetadata = true
                        edited(); saveEditedDraft()
                    }, modifier = Modifier.testTag("observer-enabled-altitude")) {
                    SpaceCompassSettingsTextField(altitude, { if (altitude != it) { altitude = it; altitudeEdited = true; edited() } },
                        Modifier.fillMaxWidth().testTag("observer-altitude"),
                        label = stringResource(R.string.observer_altitude) + if (feet) " (ft)" else " (m)",
                        enabled = !busy)
                    SpaceCompassObserverMetadataActions("altitude", selectedCoordinatesValid && !busy,
                        deviceCoordinatesValid && !busy, busy && metadataField == SpaceCompassObserverMetadataField.ALTITUDE,
                        if (metadataField == SpaceCompassObserverMetadataField.ALTITUDE) notice else 0,
                        onSelected = { detectMetadata(SpaceCompassObserverMetadataField.ALTITUDE, false) },
                        onCurrent = { detectMetadata(SpaceCompassObserverMetadataField.ALTITUDE, true) })
                    SpaceCompassSettingsDescription(stringResource(R.string.observer_terrain_note))
                }
            }
            item(key = "zone") {
                SpaceCompassSettingsToggleIsland(stringResource(R.string.observer_zone), iconKey = "time_zone",
                    checked = simulateZone, toggleDescription = stringResource(R.string.observer_enable_zone),
                    onCheckedChange = { checked ->
                        val selected = preservedMoment ?: spaceCompassObserverMoment(date.trim(), time.trim(), effectiveZone())
                        simulateZone = checked
                        islandReveal.changed(SpaceCompassObserverSection.ZONE, checked)
                        if (!checked && metadataField == SpaceCompassObserverMetadataField.ZONE) {
                            lookup?.cancel(); lookupRevision++; busy = false; notice = 0
                        }
                        if (checked) zoneEdited = true
                        if (!checked && simulatePosition && automaticZone == null) {
                            pendingMoment = selected; needsMetadata = true
                        } else preserveMoment(selected)
                        edited(); saveEditedDraft()
                    }, modifier = Modifier.testTag("observer-enabled-zone")) {
                    SpaceCompassObserverZonePicker(zone, zonePreviewMoment, enabled = !busy) { selectedZone ->
                        if (zone != selectedZone) {
                            val instant = pendingMoment ?: preservedMoment ?:
                                spaceCompassObserverMoment(date.trim(), time.trim(), effectiveZone())
                            zone = selectedZone; zoneEdited = true; preserveMoment(instant); edited()
                        }
                    }
                    SpaceCompassObserverMetadataActions("zone", selectedCoordinatesValid && !busy,
                        deviceCoordinatesValid && !busy, busy && metadataField == SpaceCompassObserverMetadataField.ZONE,
                        if (metadataField == SpaceCompassObserverMetadataField.ZONE) notice else 0,
                        onSelected = { detectMetadata(SpaceCompassObserverMetadataField.ZONE, false) },
                        onCurrent = { detectMetadata(SpaceCompassObserverMetadataField.ZONE, true) })
                }
            }
            item(key = "date") {
                SpaceCompassSettingsToggleIsland(stringResource(R.string.observer_date), iconKey = "date_format",
                    checked = simulateDate, toggleDescription = stringResource(R.string.observer_enable_date),
                    onCheckedChange = { checked ->
                        simulateDate = checked
                        islandReveal.changed(SpaceCompassObserverSection.DATE, checked)
                        if (!checked) showDate = false
                        pendingMoment = null; preservedMoment = null; clockOffset = null
                        edited(); saveEditedDraft()
                    }, modifier = Modifier.testTag("observer-enabled-date")) {
                    SpaceCompassSettingsTextField(date, { if (date != it) { pendingMoment = null; preservedMoment = null; clockOffset = null; date = it; edited() } }, Modifier.fillMaxWidth().testTag("observer-date"),
                        label = stringResource(R.string.observer_date), placeholder = "yyyy-MM-dd",
                        trailingIcon = { val label = stringResource(R.string.observer_date)
                            IconButton(onClick = { showDate = true }, modifier = Modifier.semantics { contentDescription = label }) {
                                SpaceCompassSettingsGroupIcon("date_format", MaterialTheme.colorScheme.primary)
                            } })
                    TextButton(onClick = {
                        currentDraft().withCurrentDate(System.currentTimeMillis(), ZoneId.systemDefault().id)
                            .onSuccess(::applyClockEdit).onFailure { validationError = true }
                    }, enabled = !busy, modifier = Modifier.testTag("observer-current-date")) {
                        Text(stringResource(R.string.observer_current_date))
                    }
                    SpaceCompassSettingsDescription(stringResource(R.string.observer_limits))
                    SpaceCompassScenarioMidnightNote()
                }
            }
            item(key = "time") {
                SpaceCompassSettingsToggleIsland(stringResource(R.string.observer_time), iconKey = "time_format",
                    checked = simulateTime, toggleDescription = stringResource(R.string.observer_enable_time),
                    onCheckedChange = { checked ->
                        simulateTime = checked
                        islandReveal.changed(SpaceCompassObserverSection.TIME, checked)
                        if (!checked) showTime = false
                        pendingMoment = null; preservedMoment = null
                        edited(); saveEditedDraft()
                    }, modifier = Modifier.testTag("observer-enabled-time")) {
                    Text(stringResource(R.string.observer_zone) + ": " + effectiveZone().ifBlank { "—" },
                        style = MaterialTheme.typography.bodySmall)
                    SpaceCompassSettingsTextField(time, { if (time != it) { pendingMoment = null; preservedMoment = null; clockOffset = null; time = it; edited() } }, Modifier.fillMaxWidth().testTag("observer-time"),
                        label = stringResource(R.string.observer_time), placeholder = "HH:mm",
                        trailingIcon = { val label = stringResource(R.string.observer_time)
                            IconButton(onClick = { showTime = true }, modifier = Modifier.semantics { contentDescription = label }) {
                                SpaceCompassSettingsGroupIcon("time_format", MaterialTheme.colorScheme.primary)
                            } })
                    TextButton(onClick = {
                        currentDraft().withCurrentTime(System.currentTimeMillis(), ZoneId.systemDefault().id)
                            .onSuccess(::applyClockEdit).onFailure { validationError = true }
                    }, enabled = !busy, modifier = Modifier.testTag("observer-current-time")) {
                        Text(stringResource(R.string.observer_current_time))
                    }
                    SpaceCompassScenarioMidnightNote()
                }
            }
            item(key = "scenario-speed") { SpaceCompassScenarioSpeedIsland(observer) { islandReveal.changed(5, it) } }
            if (validationError) item(key = "observer-validation", span = StaggeredGridItemSpan.FullLine) {
                Text(stringResource(R.string.observer_invalid),
                    Modifier.padding(horizontal = 8.dp).testTag("observer-validation-error"),
                    color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
            }
        }
    }
    if (showDate) {
        val selected = runCatching { LocalDate.parse(date).atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli() }.getOrNull()
        val state = rememberDatePickerState(selected, yearRange = 1900..2100)
        SpaceCompassAlertDialog(onDismissRequest = { showDate = false }, title = { Text(stringResource(R.string.observer_date)) },
            text = { SpaceCompassObserverPickerScale(360f) {
                DatePicker(state, Modifier.fillMaxWidth(), showModeToggle = false,
                    colors = DatePickerDefaults.colors(containerColor = spaceCompassSettingsCardColor()))
            } }, confirmButton = {
                SpaceCompassObserverAction(onClick = { state.selectedDateMillis?.let { pendingMoment = null; preservedMoment = null; clockOffset = null; date = Instant.ofEpochMilli(it).atZone(ZoneOffset.UTC).toLocalDate().toString(); edited() }; showDate = false; saveEditedDraft() },
                    label = stringResource(R.string.observer_apply))
            })
    }
    if (showTime) {
        val parsed = runCatching { LocalTime.parse(time) }.getOrDefault(LocalTime.NOON)
        val state = rememberTimePickerState(parsed.hour, parsed.minute, is24Hour = true)
        SpaceCompassAlertDialog(onDismissRequest = { showTime = false }, title = { Text(stringResource(R.string.observer_time)) },
            text = { SpaceCompassObserverPickerScale(320f) { TimePicker(state) } }, confirmButton = {
                SpaceCompassObserverAction(onClick = { pendingMoment = null; preservedMoment = null; clockOffset = null; time = "%02d:%02d".format(Locale.ROOT, state.hour, state.minute); edited(); showTime = false; saveEditedDraft() },
                    label = stringResource(R.string.observer_apply))
            })
    }
    if (showMap) SpaceCompassObserverMapPicker(number(latitude) ?: 0.0, number(longitude) ?: 0.0,
        onPick = { a, b -> showMap = false; choosePlace(a, b) }, onBack = { showMap = false })
}

@Composable
private fun SpaceCompassObserverMapPicker(latitude: Double, longitude: Double, onPick: (Double, Double) -> Unit, onBack: () -> Unit) {
    val context = LocalContext.current
    val density = LocalDensity.current
    val rtl = LocalLayoutDirection.current == LayoutDirection.Rtl
    val background = spaceCompassPageBackground()
    val foreground = MaterialTheme.colorScheme.onSurface
    val accent = MaterialTheme.colorScheme.primary
    val title = stringResource(R.string.observer_map)
    val back = stringResource(R.string.navigate_back)
    val unavailable = stringResource(R.string.position_map_unavailable)
    val retry = stringResource(R.string.sun_finder_retry)
    val apply = stringResource(R.string.observer_use_place).uppercase(LocalResources.current.configuration.locales[0])
    val onAccent = MaterialTheme.colorScheme.onPrimary.toArgb()
    val zoomIn = stringResource(R.string.celestial_view_zoom_in)
    val zoomOut = stringResource(R.string.celestial_view_zoom_out)
    val close by rememberUpdatedState(onBack)
    val select by rememberUpdatedState(onPick)
    val url = spaceCompassPositionMapUrl(latitude, longitude)
    DisposableEffect(context, background, foreground, accent, onAccent, title, apply, rtl, density.density, density.fontScale) {
        val window = SpaceCompassNativePositionMap(context, url, title, back, unavailable, retry,
            density.density, density.fontScale, rtl, background.toArgb(), foreground.toArgb(), accent.toArgb(),
            foreground.copy(alpha = .46f).toArgb(), background.luminance() < .4f, { close() },
            onPick = { a, b -> select(a, b) }, pickLabel = apply, zoomIn = zoomIn, zoomOut = zoomOut,
            onAccentArgb = onAccent)
        window.show()
        onDispose { window.releaseMap() }
    }
}


/** Keep the complete Material calendar/dial inside the same bounded app dialog on small phones. */
@Composable
private fun SpaceCompassObserverPickerScale(preferredWidthDp: Float, content: @Composable () -> Unit) {
    BoxWithConstraints(Modifier.fillMaxWidth()) {
        val density = LocalDensity.current
        val factor = (maxWidth.value / preferredWidthDp).coerceIn(.5f, 1f)
        CompositionLocalProvider(LocalDensity provides Density(density.density * factor, density.fontScale)) {
            content()
        }
    }
}

/** Confirmations share repository press feedback; auxiliary actions remain text links. */
@Composable
private fun SpaceCompassObserverAction(label: String, onClick: () -> Unit, enabled: Boolean = true,
    modifier: Modifier = Modifier, icon: Int = R.drawable.ic_check) {
    SpaceCompassSettingsActionButton(label, onClick, modifier, enabled) {
        SpaceCompassActionIcon(icon)
    }
}
