package me.mondiversi.spacecompass

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContract
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalResources
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

internal data class SpaceCompassCaptureDocumentRequest(val name: String, val mimeType: String)

internal class SpaceCompassCaptureDocumentContract : ActivityResultContract<SpaceCompassCaptureDocumentRequest,Uri?>() {
    override fun createIntent(context: Context, input: SpaceCompassCaptureDocumentRequest): Intent =
        Intent(Intent.ACTION_CREATE_DOCUMENT).addCategory(Intent.CATEGORY_OPENABLE).setType(input.mimeType)
            .putExtra(Intent.EXTRA_TITLE,input.name)
    override fun parseResult(resultCode: Int, intent: Intent?): Uri? =
        intent?.data.takeIf { resultCode == Activity.RESULT_OK }
}

internal data class SpaceCompassCaptureExportState(
    val visible: Boolean, val options: List<SpaceCompassCaptureExportType>, val types: Set<SpaceCompassCaptureExportType>, val mode: SpaceCompassPanoramaExportMode,
    val counts: Map<SpaceCompassCaptureExportType,Int>, val enabled: Boolean, val preparing: Boolean,
    val failed: Boolean, val saving: Boolean, val open: () -> Unit, val close: () -> Unit,
    val changeType: (SpaceCompassCaptureExportType,Boolean) -> Unit,
    val changeMode: (SpaceCompassPanoramaExportMode) -> Unit,
    val retry: () -> Unit, val save: () -> Unit, val share: () -> Unit,
    val position: SpaceCompassPanoramaPosition?, val changePosition: (SpaceCompassPanoramaPosition) -> Unit
)

private enum class SpaceCompassDataExportAction { SAVE, SHARE }
private class SpaceCompassDataExportRequest(val source: SpaceCompassDataExportSource,
    val types: Set<SpaceCompassCaptureExportType>, val mode: SpaceCompassPanoramaExportMode,
    val action: SpaceCompassDataExportAction, val position: SpaceCompassPanoramaPosition)

/** Choices are cheap: prepare the frozen selection only after an explicit delivery action. */
@Composable
internal fun rememberSpaceCompassDataExport(ownerKey: String, options: List<SpaceCompassCaptureExportType>,
    previewReady: Boolean = true,
    selectedAvailable: Boolean = true, capture: () -> SpaceCompassDataExportSource): SpaceCompassCaptureExportState {
    val context = LocalContext.current
    val resources = LocalResources.current
    val preferences = LocalSpaceCompassPreferences.current
    val scope = rememberCoroutineScope()
    val latestCapture by rememberUpdatedState(capture)
    var source by remember(ownerKey) { mutableStateOf<SpaceCompassDataExportSource?>(null) }
    var visible by remember(ownerKey) { mutableStateOf(false) }
    var typeKeys by rememberSaveable(ownerKey) { mutableStateOf(emptyList<String>()) }
    var modeKey by rememberSaveable(ownerKey) { mutableStateOf(
        if (!selectedAvailable) SpaceCompassPanoramaExportMode.INTERNATIONAL.key else
            SpaceCompassPanoramaExportMode.fromStored(preferences?.getString(SPACE_COMPASS_PANORAMA_EXPORT_MODE_KEY,null)).key) }
    var positionKey by rememberSaveable(ownerKey) { mutableStateOf<String?>(null) }
    val position = positionKey?.let(SpaceCompassPanoramaPosition::fromStored)
    val mode = SpaceCompassPanoramaExportMode.fromStored(modeKey)
    val counts = SpaceCompassCaptureExportType.entries.associateWith { type ->
        if (type in options) source?.counts?.get(type) ?: 1 else 0 }
    val types = SpaceCompassCaptureExportType.fromStored(typeKeys.toSet())
    val activeTypes = types.filter { counts.getValue(it) > 0 && it in options }.toSet()
    var request by remember(ownerKey) { mutableStateOf<SpaceCompassDataExportRequest?>(null) }
    var lastAction by remember(ownerKey) { mutableStateOf(SpaceCompassDataExportAction.SAVE) }
    var failed by remember(ownerKey) { mutableStateOf(false) }
    var saving by remember { mutableStateOf(false) }
    var pendingPaths by rememberSaveable(ownerKey) { mutableStateOf(emptyList<String>()) }
    fun notifySaved(count: Int) = showSpaceCompassBottomMessage(context,resources.getString(R.string.capture_export_saved,count))
    fun notifyFailed(error: Exception) {
        SpaceCompassErrorLog.record(context,"capture:export-delivery",error)
        showSpaceCompassBottomMessage(context,resources.getString(R.string.capture_export_error))
    }
    fun deliver(uri: Uri?, tree: Boolean) {
        val files = pendingPaths.map(::File)
        pendingPaths = emptyList()
        if (uri == null || files.isEmpty()) return
        saving = true
        scope.launch {
            try {
                val count = withContext(Dispatchers.IO) {
                    if (tree) saveSpaceCompassCaptureExportToTree(context,uri,files) else {
                        require(files.size == 1)
                        context.contentResolver.openOutputStream(uri,"w")?.use { output ->
                            files.single().inputStream().use { it.copyTo(output) }
                        } ?: error("Destination output unavailable")
                        1
                    }
                }
                notifySaved(count)
            } catch (cancelled: CancellationException) { throw cancelled
            } catch (error: Exception) { notifyFailed(error)
            } finally { saving = false }
        }
    }
    val document = rememberLauncherForActivityResult(SpaceCompassCaptureDocumentContract()) { deliver(it,false) }
    val directory = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocumentTree()) { deliver(it,true) }
    LaunchedEffect(request,visible,previewReady) {
        val capturedRequest = request ?: return@LaunchedEffect
        if (!visible || !previewReady) { request = null; return@LaunchedEffect }
        try {
            val batch = withContext(Dispatchers.IO) {
                capturedRequest.source.prepare(context.applicationContext,capturedRequest.types,capturedRequest.mode,capturedRequest.position)
            }
            visible = false // Native pickers and sharing return to the retained source view.
            when (capturedRequest.action) {
                SpaceCompassDataExportAction.SAVE -> {
                    pendingPaths = batch.files.map { it.absolutePath }
                    runCatching {
                        if (batch.files.size == 1) document.launch(SpaceCompassCaptureDocumentRequest(batch.files.single().name,batch.mimeType))
                        else directory.launch(null)
                    }.onFailure { pendingPaths = emptyList(); notifyFailed(it as? Exception ?: RuntimeException(it)) }
                }
                SpaceCompassDataExportAction.SHARE -> runCatching {
                    shareSpaceCompassCaptureExport(context,batch,resources.getString(R.string.panorama_share))
                }.onFailure { notifyFailed(it as? Exception ?: RuntimeException(it)) }
            }
        } catch (cancelled: CancellationException) { throw cancelled
        } catch (error: Exception) {
            failed = true
            SpaceCompassErrorLog.record(context,"capture:export-prepare",error)
        } catch (_: OutOfMemoryError) { failed = true
        } finally { if (request === capturedRequest) request = null }
    }
    val enabled = previewReady && !saving && request == null && activeTypes.isNotEmpty() && source != null
    fun requestDelivery(action: SpaceCompassDataExportAction) {
        if (!visible || !previewReady || saving || request != null || activeTypes.isEmpty()) return
        val capturedSource = source ?: return
        lastAction = action
        failed = false
        request = SpaceCompassDataExportRequest(capturedSource,activeTypes.toSet(),mode,action,position ?: SpaceCompassPanoramaPosition.AREA)
    }
    return SpaceCompassCaptureExportState(visible,options,types,mode,counts,enabled,
        request != null,failed,saving,
        open = {
            if (!saving && request == null && previewReady) try {
                source = latestCapture()
                positionKey = source?.initialPosition?.key
                // File choices belong to this export opening; only the formatting profile persists.
                typeKeys = emptyList()
                failed = false; visible = true
            } catch (error: Exception) { notifyFailed(error) }
            catch (error: OutOfMemoryError) { notifyFailed(RuntimeException("Export memory unavailable",error)) }
        }, close = { if (!saving) { request = null; visible = false } },
        changeType = { type,checked ->
            if (!saving && request == null && type in options) {
                failed = false
                typeKeys = if (checked) (typeKeys + type.key).distinct() else typeKeys - type.key
            }
        }, changeMode = { option ->
            if (!saving && request == null) {
                failed = false
                modeKey = option.key
                preferences?.edit()?.putString(SPACE_COMPASS_PANORAMA_EXPORT_MODE_KEY,option.key)?.apply()
            }
        }, retry = { requestDelivery(lastAction) },
        save = { requestDelivery(SpaceCompassDataExportAction.SAVE) },
        share = { requestDelivery(SpaceCompassDataExportAction.SHARE) },
        position = position, changePosition = { option ->
            if (!saving && request == null && position != null) {
                failed = false; positionKey = option.key
            }
        })
}
