package me.mondiversi.spacecompass

import android.app.Activity
import android.content.ClipData
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.core.content.FileProvider
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

internal data class SpaceCompassUpdateState(
    val dialog: Boolean = false,
    val checking: Boolean = false,
    val busy: Boolean = false,
    val progress: Float? = null,
    val release: SpaceCompassVerifiedRelease? = null,
    val message: Int? = null,
    val notice: Int? = null
)

/** Process-owned work survives page navigation; installation starts only after confirmation. */
internal object SpaceCompassUpdates {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val mutableState = MutableStateFlow(SpaceCompassUpdateState())
    val state = mutableState.asStateFlow()

    fun check(context: Context, manual: Boolean) {
        if (state.value.checking || state.value.busy) return
        val app = context.applicationContext
        mutableState.value = state.value.copy(checking = true, notice = null, message = null)
        scope.launch {
            try {
                val catalog = withContext(Dispatchers.IO) { SpaceCompassUpdateClient(app).catalog() }
                val notice = when {
                    catalog.release != null || !manual -> null
                    catalog.incompatible -> R.string.update_incompatible
                    else -> R.string.update_none
                }
                mutableState.value = SpaceCompassUpdateState(dialog = catalog.release != null, release = catalog.release, notice = notice)
            } catch (cancelled: CancellationException) { throw cancelled }
            catch (error: Exception) {
                withContext(Dispatchers.IO) { SpaceCompassErrorLog.record(app, "update_check", error) }
                mutableState.value = state.value.copy(checking = false, notice = if (manual) R.string.update_check_failed else null)
            }
        }
    }

    fun dismiss() {
        if (!state.value.busy) mutableState.value = state.value.copy(dialog = false, message = null)
    }

    fun download(context: Context, onReady: (File) -> Unit) {
        val release = state.value.release ?: return
        if (state.value.busy || state.value.checking) return
        val app = context.applicationContext
        mutableState.value = state.value.copy(busy = true, progress = null, message = null)
        scope.launch {
            try {
                val file = withContext(Dispatchers.IO) {
                    SpaceCompassUpdateClient(app).download(release) { progress ->
                        scope.launch { mutableState.value = state.value.copy(progress = progress) }
                    }
                }
                mutableState.value = state.value.copy(busy = false, dialog = false, progress = null)
                onReady(file)
            } catch (cancelled: CancellationException) { throw cancelled }
            catch (error: Exception) {
                withContext(Dispatchers.IO) { SpaceCompassErrorLog.record(app, "update_download", error) }
                mutableState.value = state.value.copy(busy = false, progress = null, dialog = true, message = R.string.update_failed)
            }
        }
    }

    /** Revalidate after Android settings, including recovery if that activity killed our process. */
    fun continueInstall(context: Context, onReady: (File) -> Unit) {
        if (state.value.busy) return
        val app = context.applicationContext
        mutableState.value = state.value.copy(busy = true)
        scope.launch {
            try {
                val (release, file) = withContext(Dispatchers.IO) { SpaceCompassUpdateClient(app).pendingInstaller() }
                mutableState.value = state.value.copy(busy = false, release = release, dialog = false, message = null)
                onReady(file)
            } catch (cancelled: CancellationException) { throw cancelled }
            catch (error: Exception) {
                withContext(Dispatchers.IO) { SpaceCompassErrorLog.record(app, "update_install", error) }
                issue(R.string.update_failed)
            }
        }
    }

    fun issue(message: Int) {
        mutableState.value = state.value.copy(busy = false, dialog = false, notice = message, progress = null)
    }

    fun installerReturned(result: Int) {
        if (result != Activity.RESULT_OK) issue(R.string.update_install_cancelled)
    }

    @Suppress("DEPRECATION")
    fun installIntent(context: Context, file: File): Intent {
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        return Intent(Intent.ACTION_INSTALL_PACKAGE).apply {
            setDataAndType(uri, "application/vnd.android.package-archive")
            clipData = ClipData.newRawUri("Space Compass update", uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            putExtra(Intent.EXTRA_RETURN_RESULT, true)
        }
    }

    fun permissionIntent(context: Context): Intent =
        Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES, Uri.parse("package:${context.packageName}"))
}
