package me.mondiversi.spacecompass

import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private const val PERSONAL_MOMENTS_MIME = "application/octet-stream"

internal enum class SpaceCompassPersonalMomentsExportScope(@StringRes val label: Int) {
    ALL(R.string.personal_moments_scope_all),
    OBJECT(R.string.personal_moments_scope_object);

    fun momentsFor(body: SpaceCompassCelestialBody, moments: List<SpaceCompassPersonalMoment>,
        onlySelected: Boolean = false) = moments.filter {
        (this == ALL || it.body == body) && (!onlySelected || it.enabled)
    }
}

private enum class SpaceCompassPersonalMomentExportFile(@StringRes val label: Int, val extension: String, val mimeType: String) {
    ARCHIVE(R.string.personal_moments_archive_file, SPACE_COMPASS_PERSONAL_MOMENTS_EXTENSION, PERSONAL_MOMENTS_MIME),
    CSV(R.string.capture_export_csv, ".csv", "text/csv")
}

@Composable
internal fun rememberSpaceCompassPersonalMomentTransfer(body: SpaceCompassCelestialBody,
    state: SpaceCompassTemporalComparisonState?): SpaceCompassDocumentTransfer {
    val context = LocalContext.current
    val resources = LocalResources.current
    val scope = rememberCoroutineScope()
    val moments = state?.personalMoments.orEmpty()
    val profiles = rememberSpaceCompassDataExportProfiles()
    val exportMode = rememberSpaceCompassExportMode()
    var exporting by rememberSaveable(body) { mutableStateOf(false) }
    var exportScope by rememberSaveable(body) { mutableStateOf(SpaceCompassPersonalMomentsExportScope.OBJECT) }
    var onlySelected by rememberSaveable(body) { mutableStateOf(false) }
    var fileType by rememberSaveable(body) { mutableStateOf(SpaceCompassPersonalMomentExportFile.ARCHIVE) }
    var exportTimeMs by rememberSaveable(body) { mutableLongStateOf(0L) }
    var pendingSave by rememberSaveable(body) { mutableStateOf<String?>(null) }
    var working by remember { mutableStateOf(false) }
    fun notice(id: Int, vararg args: Any) = showSpaceCompassBottomMessage(context, resources.getString(id, *args))

    val save = rememberLauncherForActivityResult(SpaceCompassCaptureDocumentContract()) { uri ->
        val content = pendingSave
        pendingSave = null
        if (uri != null && content != null) {
            working = true
            scope.launch {
                try {
                    withContext(Dispatchers.IO) {
                        context.contentResolver.openOutputStream(uri, "w")?.use { it.write(content.toByteArray(Charsets.UTF_8)) }
                            ?: error("Personal moments destination unavailable")
                    }
                    notice(R.string.personal_moments_file_saved)
                } catch (cancelled: CancellationException) { throw cancelled }
                catch (_: Exception) { notice(R.string.personal_moments_file_error) }
                finally { working = false }
            }
        }
    }
    val load = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null && state != null) {
            working = true
            scope.launch {
                try {
                    val file = withContext(Dispatchers.IO) {
                        SpaceCompassPersonalMomentsFile.decode(readSpaceCompassDocumentText(context, uri,
                            SPACE_COMPASS_PERSONAL_MOMENTS_FILE_LIMIT))
                    }
                    if (file.body != null && file.body != body) {
                        notice(R.string.personal_moments_file_wrong_body, resources.getString(file.body.nameResource))
                    } else {
                        val added = state.importPersonalMoments(file.moments)
                        notice(if (added > 0) R.string.personal_moments_file_imported else R.string.personal_moments_file_existing)
                    }
                } catch (cancelled: CancellationException) { throw cancelled }
                catch (_: Exception) { notice(R.string.personal_moments_file_import_error) }
                finally { working = false }
            }
        }
    }

    val exportPage: (@Composable () -> Unit)? = if (!exporting) null else {
        {
            val profile = profiles.forMode(exportMode.mode)
            val exportMoments = remember(exportScope, body, moments, onlySelected) {
                exportScope.momentsFor(body, moments, onlySelected)
            }
            val exportBody = body.takeIf { exportScope == SpaceCompassPersonalMomentsExportScope.OBJECT }
            val close = { if (!working) exporting = false }
            fun fields(strings: SpaceCompassTextResources) = listOfNotNull(
                strings.getString(R.string.personal_moments_title),
                exportBody?.let { strings.getString(it.nameResource) },
                strings.getString(R.string.personal_moments_scope_selected).takeIf { onlySelected }
            )
            fun fileName() = spaceCompassExportFileStem(
                exportTimeMs, profile.formatting, "UTC", fields(profile.strings)) + fileType.extension
            fun encoded() = when (fileType) {
                SpaceCompassPersonalMomentExportFile.ARCHIVE -> SpaceCompassPersonalMomentsFile(exportBody, exportMoments).encode()
                SpaceCompassPersonalMomentExportFile.CSV -> renderSpaceCompassPersonalMomentsCsv(exportMoments, profile)
            }
            BackHandler(onBack = close)
            SpaceCompassDataExportPage(close, Modifier.testTag("personal-moments-export-page")) {
                Text(stringResource(R.string.capture_export_files_heading), fontSize = 15.sp, fontWeight = FontWeight.Bold)
                Column(Modifier.fillMaxWidth().selectableGroup(),
                    verticalArrangement = Arrangement.spacedBy(SpaceCompassSettingsChoiceSpacing)) {
                    SpaceCompassPersonalMomentExportFile.entries.forEach { option ->
                        SpaceCompassSettingsRadioRow(fileType == option, { fileType = option },
                            Modifier.fillMaxWidth().testTag("personal-moments-file-${option.name.lowercase()}")) {
                            Text(stringResource(option.label), Modifier.weight(1f))
                        }
                    }
                }
                HorizontalDivider(Modifier.padding(vertical = 4.dp))
                Column(Modifier.fillMaxWidth().selectableGroup(),
                    verticalArrangement = Arrangement.spacedBy(SpaceCompassSettingsChoiceSpacing)) {
                    SpaceCompassPersonalMomentsExportScope.entries.forEach { option ->
                        SpaceCompassSettingsRadioRow(exportScope == option, { exportScope = option },
                            Modifier.fillMaxWidth().testTag("personal-moments-scope-${option.name.lowercase()}")) {
                            Text(stringResource(option.label), Modifier.weight(1f))
                        }
                    }
                }
                SpaceCompassSettingsCheckbox(stringResource(R.string.personal_moments_scope_selected), onlySelected,
                    { onlySelected = it }, Modifier.testTag("personal-moments-scope-selected"))
                HorizontalDivider(Modifier.padding(vertical = 4.dp))
                SpaceCompassExportProfileChoices(exportMode.mode, exportMode.change, enabled = !working)
                SpaceCompassDataExportActions(save = {
                    runCatching {
                        pendingSave = encoded()
                        save.launch(SpaceCompassCaptureDocumentRequest(fileName(), fileType.mimeType))
                        close()
                    }.onFailure { pendingSave = null; notice(R.string.personal_moments_file_error) }
                }, share = {
                    runCatching {
                        shareSpaceCompassTextDocument(context, "personal_moments", fileName(),
                            resources.getString(R.string.personal_moments_export), encoded(), fileType.mimeType)
                        close()
                    }.onFailure { notice(R.string.personal_moments_file_error) }
                }, enabled = exportMoments.isNotEmpty() && !working,
                    saveModifier = Modifier.testTag("personal-moments-export-save"),
                    shareModifier = Modifier.testTag("personal-moments-export-share"))
            }
        }
    }
    return SpaceCompassDocumentTransfer(
        openExport = {
            exportScope = SpaceCompassPersonalMomentsExportScope.OBJECT
            onlySelected = false
            fileType = SpaceCompassPersonalMomentExportFile.ARCHIVE
            exportTimeMs = System.currentTimeMillis()
            exporting = true
        },
        openImport = {
            runCatching { load.launch(arrayOf("*/*")) }.onFailure { notice(R.string.personal_moments_file_import_error) }
        },
        canExport = moments.isNotEmpty() && !working && pendingSave == null,
        canImport = state != null && !working && pendingSave == null,
        exportPage = exportPage
    )
}
