package me.mondiversi.spacecompass

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private const val PERSONAL_MOMENTS_MIME = "application/octet-stream"

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun SpaceCompassPersonalMomentTransferActions(body: SpaceCompassCelestialBody,
    moments: List<SpaceCompassPersonalMoment>, state: SpaceCompassTemporalComparisonState?) {
    val context = LocalContext.current
    val resources = LocalResources.current
    val scope = rememberCoroutineScope()
    var exportText by rememberSaveable(body) { mutableStateOf<String?>(null) }
    var pendingSave by rememberSaveable(body) { mutableStateOf<String?>(null) }
    var working by remember { mutableStateOf(false) }
    fun notice(id: Int, vararg args: Any) = showSpaceCompassBottomMessage(context, resources.getString(id, *args))

    val save = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument(PERSONAL_MOMENTS_MIME)) { uri ->
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
                    if (file.body != body) {
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

    SpaceCompassTitleActionButton(stringResource(R.string.personal_moments_export), {
        runCatching { SpaceCompassPersonalMomentsFile(body, moments).encode() }
            .onSuccess { exportText = it }.onFailure { notice(R.string.personal_moments_file_error) }
    }, Modifier.testTag("personal-moments-export"), enabled = moments.isNotEmpty() && !working) {
        SpaceCompassPanoramaExportIcon(SpaceCompassTitleActionIconStrokeWidth)
    }
    SpaceCompassTitleActionButton(stringResource(R.string.personal_moments_import), {
        runCatching { load.launch(arrayOf("*/*")) }.onFailure { notice(R.string.personal_moments_file_import_error) }
    }, Modifier.testTag("personal-moments-import"), enabled = state != null && !working) {
        SpaceCompassScenarioImportIcon()
    }

    exportText?.let { text ->
        val title = stringResource(R.string.personal_moments_export)
        val bodyName = stringResource(body.nameResource)
        val fileName = "SpaceCompass_${spaceCompassExportFilePart(stringResource(R.string.personal_moments_title))}_" +
            spaceCompassExportFilePart(bodyName) + SPACE_COMPASS_PERSONAL_MOMENTS_EXTENSION
        SpaceCompassAlertDialog(onDismissRequest = { exportText = null }, title = { Text(title) },
            text = { SpaceCompassSettingsDescription(bodyName) }, confirmButton = {
                FlowRow(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End),
                    verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    val padding = PaddingValues(horizontal = 8.dp, vertical = 8.dp)
                    SpaceCompassDialogActionButton(stringResource(R.string.celestial_remove_cancel), { exportText = null },
                        destructive = true, contentPadding = padding)
                    SpaceCompassDialogActionButton(stringResource(R.string.panorama_save_phone), {
                        runCatching {
                            pendingSave = text
                            save.launch(fileName)
                            exportText = null
                        }.onFailure { pendingSave = null; notice(R.string.personal_moments_file_error) }
                    }, Modifier.testTag("personal-moments-export-save"), contentPadding = padding)
                    SpaceCompassDialogActionButton(stringResource(R.string.panorama_share), {
                        runCatching {
                            shareSpaceCompassTextDocument(context, "personal_moments", fileName, title, text, PERSONAL_MOMENTS_MIME)
                            exportText = null
                        }.onFailure { notice(R.string.personal_moments_file_error) }
                    }, Modifier.testTag("personal-moments-export-share"), contentPadding = padding)
                }
            })
    }
}
