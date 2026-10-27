package me.mondiversi.spacecompass

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
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

/** Android document pickers and shares are independent of astronomical exports and their preferences. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun SpaceCompassScenarioTransferActions(capture: () -> SpaceCompassScenarioArchive?,
    onImport: (SpaceCompassScenarioArchive) -> Unit, enabled: Boolean = true) {
    val context = LocalContext.current
    val resources = LocalResources.current
    val scope = rememberCoroutineScope()
    val latestCapture by rememberUpdatedState(capture)
    val latestImport by rememberUpdatedState(onImport)
    var exportText by rememberSaveable { mutableStateOf<String?>(null) }
    var pendingSave by rememberSaveable { mutableStateOf<String?>(null) }
    var name by rememberSaveable { mutableStateOf("") }
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
            latestCapture()?.let { archive -> exportText = archive.encode(); name = archive.name }
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
        val validName = name.isNotBlank() && name.none(Char::isISOControl) &&
            runCatching { spaceCompassScenarioFileName(name) }.isSuccess
        SpaceCompassAlertDialog(onDismissRequest = { exportText = null },
            modifier = Modifier.testTag("scenario-export-dialog"),
            title = { Text(stringResource(R.string.scenario_export)) },
            text = {
                SpaceCompassSettingsTextField(name, { name = it.take(80) },
                    Modifier.fillMaxWidth().testTag("scenario-export-name"), label = stringResource(R.string.scenario_file_name))
            }, confirmButton = {
                // Keep the three actions in reading order; compact padding fits the normal phone width.
                FlowRow(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End),
                    verticalArrangement = Arrangement.spacedBy(8.dp)) {
                val padding = PaddingValues(horizontal = 8.dp, vertical = 8.dp)
                SpaceCompassDialogActionButton(stringResource(R.string.celestial_remove_cancel), { exportText = null },
                    Modifier.testTag("scenario-export-cancel"), destructive = true, contentPadding = padding)
                SpaceCompassDialogActionButton(stringResource(R.string.panorama_save_phone), {
                    runCatching {
                        val archive = SpaceCompassScenarioArchive.decode(content).copy(name = name.trim())
                        pendingSave = archive.encode()
                        save.launch(spaceCompassScenarioFileName(archive.name))
                        exportText = null
                    }.onFailure { pendingSave = null; notice(R.string.scenario_file_error) }
                }, Modifier.testTag("scenario-export-save"), enabled = validName, contentPadding = padding)
                SpaceCompassDialogActionButton(stringResource(R.string.panorama_share), {
                    runCatching {
                        val archive = SpaceCompassScenarioArchive.decode(content).copy(name = name.trim())
                        shareSpaceCompassScenario(context, archive, resources.getString(R.string.scenario_export))
                        exportText = null
                    }.onFailure { notice(R.string.scenario_file_error) }
                }, Modifier.testTag("scenario-export-share"), enabled = validName, contentPadding = padding)
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
