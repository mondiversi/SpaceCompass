package me.mondiversi.spacecompass

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** Scenarios share the export profile preference; their canonical import schema stays independent. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun SpaceCompassScenarioTransferActions(capture: (Boolean) -> SpaceCompassScenarioArchive?,
    onImport: (SpaceCompassScenarioArchive) -> Unit, enabled: Boolean = true) {
    val context = LocalContext.current
    val resources = LocalResources.current
    val observer = LocalSpaceCompassObserver.current
    val profiles = rememberSpaceCompassDataExportProfiles()
    val exportMode = rememberSpaceCompassExportMode()
    val suggestName = spaceCompassScenarioNameSuggestion(profiles)
    val scope = rememberCoroutineScope()
    val latestCapture by rememberUpdatedState(capture)
    val latestImport by rememberUpdatedState(onImport)
    var exportText by rememberSaveable { mutableStateOf<String?>(null) }
    var objectsText by rememberSaveable { mutableStateOf<String?>(null) }
    var includeObjects by rememberSaveable { mutableStateOf(false) }
    var pendingSave by rememberSaveable { mutableStateOf<String?>(null) }
    var name by rememberSaveable { mutableStateOf("") }
    var nameEdited by rememberSaveable { mutableStateOf(false) }
    var working by remember { mutableStateOf(false) }
    fun notice(id: Int) = showSpaceCompassBottomMessage(context, resources.getString(id))
    val save = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument(SPACE_COMPASS_SCENARIO_MIME)) { uri ->
        val content = pendingSave
        pendingSave = null
        if (uri != null && content != null) {
            working = true
            scope.launch {
                try {
                    withContext(Dispatchers.IO) {
                        context.contentResolver.openOutputStream(uri, "w")?.use { it.write(content.toByteArray(Charsets.UTF_8)) }
                            ?: error("Scenario destination unavailable")
                    }
                    notice(R.string.scenario_file_saved)
                } catch (cancelled: CancellationException) { throw cancelled
                } catch (_: Exception) { notice(R.string.scenario_file_error)
                } finally { working = false }
            }
        }
    }
    val load = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            working = true
            scope.launch {
                try {
                    val archive = withContext(Dispatchers.IO) { readSpaceCompassScenario(context, uri) }
                    latestImport(archive)
                    notice(R.string.scenario_file_imported)
                } catch (cancelled: CancellationException) { throw cancelled
                } catch (_: Exception) { notice(R.string.scenario_file_invalid)
                } finally { working = false }
            }
        }
    }
    SpaceCompassTitleActionButton(stringResource(R.string.scenario_export), {
        runCatching {
            latestCapture(false)?.let { archive ->
                exportText = archive.encode()
                objectsText = null
                includeObjects = false
                nameEdited = archive.name !in listOf(resources.getString(R.string.observer_title), "Scenario")
                name = if (nameEdited) archive.name else suggestName(archive,exportMode.mode)
            }
        }.onFailure { notice(R.string.observer_not_saved) }
    }, Modifier.testTag("scenario-export-open"), enabled = enabled && !working) {
        SpaceCompassPanoramaExportIcon(SpaceCompassTitleActionIconStrokeWidth)
    }
    SpaceCompassTitleActionButton(stringResource(R.string.scenario_import), {
        runCatching { load.launch(arrayOf("*/*")) }.onFailure { notice(R.string.scenario_file_invalid) }
    }, Modifier.testTag("scenario-import-open"), enabled = enabled && !working) {
        SpaceCompassScenarioImportIcon()
    }
    exportText?.let { content ->
        val baseArchive = remember(content) { SpaceCompassScenarioArchive.decode(content) }
        // A location fix arriving while the dialog is open can complete the otherwise empty export.
        LaunchedEffect(content, observer?.devicePlace) {
            if (!baseArchive.custom && !baseArchive.timeSettings.enabled) {
                runCatching { latestCapture(false) }.getOrNull()?.takeIf { it.custom }?.let { ready ->
                    objectsText = objectsText?.let { included -> ready.copy(
                        celestialSelection = SpaceCompassScenarioArchive.decode(included).celestialSelection).encode() }
                    exportText = ready.encode()
                }
            }
        }
        val selectedContent = if (includeObjects) objectsText ?: content else content
        val archive = remember(selectedContent) { SpaceCompassScenarioArchive.decode(selectedContent) }
        LaunchedEffect(exportMode.mode,selectedContent) {
            if (!nameEdited) name = suggestName(archive,exportMode.mode)
        }
        val validName = name.isNotBlank() && name.none(Char::isISOControl) &&
            runCatching { spaceCompassScenarioFileName(name) }.isSuccess
        val hasValues = archive.custom || archive.timeSettings.enabled || archive.celestialSelection != null
        fun exported(): Pair<SpaceCompassScenarioArchive, String> {
            check(hasValues)
            val named = archive.copy(name = name.trim())
            return named to spaceCompassScenarioExportText(named, exportMode.mode, profiles.forMode(exportMode.mode))
        }
        SpaceCompassAlertDialog(onDismissRequest = { exportText = null },
            modifier = Modifier.testTag("scenario-export-dialog"),
            title = { Text(stringResource(R.string.scenario_export)) },
            text = {
                Column(Modifier.fillMaxWidth(),
                    verticalArrangement=Arrangement.spacedBy(8.dp)) {
                    SpaceCompassSettingsDescription(stringResource(R.string.panorama_export_description),pageDescription=true)
                    if (!hasValues) SpaceCompassSettingsDescription(
                        stringResource(R.string.location_export_wait_position))
                    SpaceCompassSettingsTextField(name, { name = it.take(80); nameEdited = true },
                        Modifier.fillMaxWidth().testTag("scenario-export-name"), label = stringResource(R.string.scenario_file_name))
                    SpaceCompassSettingsCheckbox(stringResource(R.string.location_export_include_objects), includeObjects, { checked ->
                        if (!checked) includeObjects = false else {
                            val snapshot = runCatching { latestCapture(true)?.encode() }.getOrNull()
                            if (snapshot == null) notice(R.string.observer_not_saved) else {
                                objectsText = snapshot
                                includeObjects = true
                            }
                        }
                    }, Modifier.testTag("location-export-objects"), enabled = !working)
                    SpaceCompassSettingsDescription(stringResource(R.string.location_export_objects_description))
                    HorizontalDivider(Modifier.fillMaxWidth().padding(vertical=4.dp).testTag("scenario-export-profile-divider"),
                        color=spaceCompassDialogContentColor().copy(alpha=.16f))
                    SpaceCompassExportProfileChoices(exportMode.mode,exportMode.change,enabled=!working)
                }
            }, confirmButton = {
                FlowRow(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End),
                    verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    val padding = PaddingValues(horizontal = 8.dp, vertical = 8.dp)
                    SpaceCompassDialogActionButton(stringResource(R.string.celestial_remove_cancel), { exportText = null },
                        Modifier.testTag("scenario-export-cancel"), destructive = true, contentPadding = padding)
                    SpaceCompassDialogActionButton(stringResource(R.string.panorama_save_phone), {
                        runCatching {
                            val (named,text) = exported()
                            pendingSave = text
                            save.launch(spaceCompassScenarioFileName(named.name))
                            exportText = null
                        }.onFailure { pendingSave = null; notice(R.string.scenario_file_error) }
                    }, Modifier.testTag("scenario-export-save"), enabled = hasValues && validName && !working, contentPadding = padding)
                    SpaceCompassDialogActionButton(stringResource(R.string.panorama_share), {
                        runCatching {
                            val (named,text) = exported()
                            shareSpaceCompassScenario(context,named,resources.getString(R.string.scenario_export),text)
                            exportText = null
                        }.onFailure { notice(R.string.scenario_file_error) }
                    }, Modifier.testTag("scenario-export-share"), enabled = hasValues && validName && !working, contentPadding = padding)
                }
            })
    }
}

/** Matches the export tray and its 24 dp viewport; the downward arrow denotes restoration. */
@Composable
private fun SpaceCompassScenarioImportIcon() {
    val tint = LocalContentColor.current
    SpaceCompassIconCanvas(tint, Modifier.size(SpaceCompassTitleActionIconSize).offset(y = (-1.5).dp)) { color ->
        fun line(ax: Float, ay: Float, bx: Float, by: Float) = drawLine(color,
            Offset(size.width * ax, size.height * ay), Offset(size.width * bx, size.height * by),
            SpaceCompassTitleActionIconStrokeWidth.toPx(), StrokeCap.Round)
        line(.20f, .72f, .20f, .88f); line(.20f, .88f, .80f, .88f); line(.80f, .88f, .80f, .72f)
        line(.50f, .25f, .50f, .63f); line(.35f, .49f, .50f, .63f); line(.65f, .49f, .50f, .63f)
    }
}
