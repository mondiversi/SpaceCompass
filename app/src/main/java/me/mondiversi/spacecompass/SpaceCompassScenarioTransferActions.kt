package me.mondiversi.spacecompass

import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.LocalContentColor
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
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

/** Location-only import deliberately ignores celestial extensions in previous mixed documents. */
@Composable
internal fun rememberSpaceCompassLocationTransfer(capture: () -> SpaceCompassConfigurationExport?,
    onImport: (SpaceCompassScenarioArchive) -> Unit, enabled: Boolean = true): SpaceCompassDocumentTransfer =
    rememberSpaceCompassConfigurationTransfer(false,LocalSpaceCompassObserver.current,capture,
        { SpaceCompassScenarioArchive.decode(it).locationOnly() },onImport,enabled)

/** Shared document delivery and export styling; each document type owns its canonical decoder. */
@Composable
internal fun <T> rememberSpaceCompassConfigurationTransfer(scene: Boolean, observer: SpaceCompassObserverState?,
    capture: () -> SpaceCompassConfigurationExport?, decode: (String) -> T, onImport: (T) -> Unit,
    enabled: Boolean = true): SpaceCompassDocumentTransfer {
    val context=LocalContext.current
    val resources=LocalResources.current
    val profiles=rememberSpaceCompassDataExportProfiles()
    val exportMode=rememberSpaceCompassExportMode()
    val suggestLocationName=spaceCompassScenarioNameSuggestion(profiles)
    val scope=rememberCoroutineScope()
    val latestCapture by rememberUpdatedState(capture)
    val latestImport by rememberUpdatedState(onImport)
    val latestDecode by rememberUpdatedState(decode)
    var configuredText by rememberSaveable { mutableStateOf<String?>(null) }
    var completeText by rememberSaveable { mutableStateOf<String?>(null) }
    var instant by rememberSaveable { mutableLongStateOf(0L) }
    var displayText by rememberSaveable { mutableStateOf<String?>(null) }
    var include by rememberSaveable { mutableStateOf(true) }
    var completeAutomatic by rememberSaveable { mutableStateOf(true) }
    var pendingSave by rememberSaveable { mutableStateOf<String?>(null) }
    var name by rememberSaveable { mutableStateOf("") }
    var nameEdited by rememberSaveable { mutableStateOf(false) }
    var working by remember { mutableStateOf(false) }
    fun notice(id: Int)=showSpaceCompassBottomMessage(context,resources.getString(id))
    val errorId=if (scene) R.string.scene_file_error else R.string.scenario_file_error
    val invalid=if (scene) R.string.scene_file_invalid else R.string.scenario_file_invalid
    fun filename(value: String)=if (scene) spaceCompassSceneFileName(value) else spaceCompassScenarioFileName(value)
    fun stage(snapshot: SpaceCompassConfigurationExport) {
        configuredText=snapshot.configured.encode();completeText=snapshot.complete?.encode();instant=snapshot.capturedAtMs
        displayText=snapshot.display?.let { listOf(it.guides,it.forward24Hours,it.weather,it.translucentObjects).joinToString(",") }
    }
    val save=rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument(SPACE_COMPASS_SCENARIO_MIME)) { uri ->
        val text=pendingSave;pendingSave=null
        if (uri!=null && text!=null) {
            working=true
            scope.launch {
                try {
                    withContext(Dispatchers.IO) {
                        context.contentResolver.openOutputStream(uri,"w")?.use { it.write(text.toByteArray(Charsets.UTF_8)) }
                            ?: error("Document destination unavailable")
                    }
                    notice(if (scene) R.string.scene_file_saved else R.string.scenario_file_saved)
                } catch (cancelled: CancellationException) { throw cancelled
                } catch (_: Exception) { notice(errorId)
                } finally { working=false }
            }
        }
    }
    val load=rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri!=null) {
            working=true
            scope.launch {
                try {
                    val document=withContext(Dispatchers.IO) {
                        latestDecode(readSpaceCompassDocumentText(context,uri,SPACE_COMPASS_SCENARIO_FILE_LIMIT))
                    }
                    latestImport(document)
                    notice(if (scene) R.string.scene_file_imported else R.string.scenario_file_imported)
                } catch (cancelled: CancellationException) { throw cancelled
                } catch (_: Exception) { notice(invalid)
                } finally { working=false }
            }
        }
    }
    val page=configuredText?.let { text ->
        @Composable {
            // A late GPS fix can complete a pending export, but valid snapshots never drift while editing.
            LaunchedEffect(text,observer?.devicePlace) {
                if (completeText==null) runCatching { latestCapture() }.getOrNull()?.takeIf { it.complete!=null }?.let(::stage)
            }
            val base=remember(text) { SpaceCompassScenarioArchive.decode(text) }
            val complete=remember(completeText) { completeText?.let { SpaceCompassScenarioArchive.decode(it) } }
            val display=remember(displayText) { displayText?.split(',')?.map(String::toBooleanStrict)?.let {
                SpaceCompassSceneDisplay(it[0],it[1],it[2],it[3]) } }
            val snapshot=SpaceCompassConfigurationExport(base,complete,instant,display)
            val selected=if (scene) snapshot.scene(include,completeAutomatic)?.contents else snapshot.location(include)
            val valid=selected!=null && (scene || selected.custom || selected.timeSettings.enabled)
            LaunchedEffect(exportMode.mode,selected,include) {
                if (!nameEdited && selected!=null) name=if (scene)
                    spaceCompassSceneSuggestedName(requireNotNull(snapshot.scene(include,completeAutomatic)),profiles.forMode(exportMode.mode))
                    else suggestLocationName(selected,exportMode.mode)
            }
            val validName=name.isNotBlank() && name.none(Char::isISOControl) && runCatching { filename(name) }.isSuccess
            fun exported(): String {
                check(valid)
                val archive=requireNotNull(selected).copy(name=name.trim())
                val profile=profiles.forMode(exportMode.mode)
                return if (scene) spaceCompassSceneExportText(requireNotNull(snapshot.scene(include,completeAutomatic)).copy(contents=archive),exportMode.mode,profile)
                    else spaceCompassScenarioExportText(archive,exportMode.mode,profile)
            }
            val close={ if (!working) configuredText=null }
            BackHandler(onBack=close)
            SpaceCompassDataExportPage(close,Modifier.testTag(if (scene) "scene-export-page" else "scenario-export-page")) {
                if (include && (!scene || completeAutomatic) && complete==null) SpaceCompassSettingsDescription(stringResource(R.string.location_export_wait_position))
                Column(Modifier.fillMaxWidth(),verticalArrangement=Arrangement.spacedBy(SpaceCompassSettingsControlGap)) {
                    SpaceCompassSettingsTextField(name,{ name=it.take(80);nameEdited=true },
                        Modifier.fillMaxWidth().testTag("scenario-export-name"),
                        label=stringResource(if (scene) R.string.scene_file_name else R.string.scenario_file_name),enabled=!working)
                    Column(Modifier.fillMaxWidth(),verticalArrangement=Arrangement.spacedBy(SpaceCompassSettingsChoiceSpacing)) {
                        SpaceCompassSettingsCheckbox(stringResource(if (scene) R.string.scene_export_include_location
                            else R.string.scenario_export_include_automatic),include,{ include=it },
                            Modifier.testTag(if (scene) "scene-export-location" else "location-export-automatic"),
                            enabled=!working,compact=true)
                        if (scene) SpaceCompassSettingsCheckbox(stringResource(R.string.scenario_export_include_automatic),
                            completeAutomatic,{ completeAutomatic=it },
                            Modifier.testTag("scene-export-automatic"),enabled=include && !working,compact=true)
                    }
                }
                HorizontalDivider(Modifier.fillMaxWidth().padding(vertical=4.dp).testTag("scenario-export-profile-divider"),
                    color=spaceCompassDialogContentColor().copy(alpha=.16f))
                SpaceCompassExportProfileChoices(exportMode.mode,exportMode.change,enabled=!working)
                SpaceCompassDataExportActions(save={
                    runCatching { pendingSave=exported();save.launch(filename(name.trim()));close() }
                        .onFailure { pendingSave=null;notice(errorId) }
                },share={
                    runCatching { shareSpaceCompassTextDocument(context,"scenarios",filename(name.trim()),
                        resources.getString(if (scene) R.string.scene_export else R.string.scenario_export),exported(),SPACE_COMPASS_SCENARIO_MIME);close() }
                        .onFailure { notice(errorId) }
                },enabled=valid && validName && !working,
                    saveModifier=Modifier.testTag("scenario-export-save"),shareModifier=Modifier.testTag("scenario-export-share"))
            }
        }
    }
    return SpaceCompassDocumentTransfer(openExport={
        runCatching { latestCapture()?.let {
            stage(it);include=true;completeAutomatic=true
            nameEdited=!scene && it.configured.name !in listOf(resources.getString(R.string.observer_title),"Scenario")
            name=if (nameEdited) it.configured.name else ""
        } }
            .onFailure { notice(errorId) }
    },openImport={ runCatching { load.launch(arrayOf("*/*")) }.onFailure { notice(invalid) } },
        canExport=enabled && !working && pendingSave==null,canImport=enabled && !working && pendingSave==null,exportPage=page)
}

/** Matches the export tray and its 24 dp viewport; the downward arrow denotes restoration. */
@Composable
internal fun SpaceCompassScenarioImportIcon() {
    val tint = LocalContentColor.current
    SpaceCompassIconCanvas(tint, Modifier.size(SpaceCompassTitleActionIconSize).offset(y = (-1.5).dp)) { color ->
        fun line(ax: Float, ay: Float, bx: Float, by: Float) = drawLine(color,
            Offset(size.width * ax, size.height * ay), Offset(size.width * bx, size.height * by),
            SpaceCompassTitleActionIconStrokeWidth.toPx(), StrokeCap.Round)
        line(.20f, .72f, .20f, .88f); line(.20f, .88f, .80f, .88f); line(.80f, .88f, .80f, .72f)
        line(.50f, .25f, .50f, .63f); line(.35f, .49f, .50f, .63f); line(.65f, .49f, .50f, .63f)
    }
}
