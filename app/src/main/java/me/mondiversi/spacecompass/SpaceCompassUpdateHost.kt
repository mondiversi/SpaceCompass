package me.mondiversi.spacecompass

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties

@Composable
internal fun SpaceCompassUpdateHost() {
    val context = LocalContext.current
    val state by SpaceCompassUpdates.state.collectAsState()
    val notice = state.notice?.let { stringResource(it) }
    LaunchedEffect(notice) {
        // Compose resources carry the app language while the Activity context stays intact.
        notice?.let { showSpaceCompassBottomMessage(context, it) }
    }
    val installer = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        SpaceCompassUpdates.installerReturned(it.resultCode)
    }
    val permission = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        if (context.packageManager.canRequestPackageInstalls()) {
            SpaceCompassUpdates.continueInstall(context) { file ->
                try { installer.launch(SpaceCompassUpdates.installIntent(context, file)) }
                catch (_: Exception) { SpaceCompassUpdates.issue(R.string.update_installer_unavailable) }
            }
        } else SpaceCompassUpdates.issue(R.string.update_permission)
    }
    SpaceCompassUpdateDialog(state, onDismiss = SpaceCompassUpdates::dismiss, onUpdate = {
        SpaceCompassUpdates.download(context) { file ->
            try {
                if (context.packageManager.canRequestPackageInstalls()) installer.launch(SpaceCompassUpdates.installIntent(context, file))
                else permission.launch(SpaceCompassUpdates.permissionIntent(context))
            } catch (_: Exception) { SpaceCompassUpdates.issue(R.string.update_installer_unavailable) }
        }
    })
}

@Composable
internal fun SpaceCompassUpdateDialog(state: SpaceCompassUpdateState, onDismiss: () -> Unit, onUpdate: () -> Unit) {
    val release = state.release?.app ?: return
    if (!state.dialog) return
    val text = spaceCompassDialogContentColor()
    SpaceCompassAlertDialog(
        onDismissRequest = { if (!state.busy) onDismiss() },
        modifier = Modifier.testTag("app-update-dialog"),
        containerColor = spaceCompassSettingsCardColor(),
        titleContentColor = text,
        textContentColor = text,
        title = { Text(stringResource(R.string.update_available)) },
        properties = DialogProperties(dismissOnBackPress = !state.busy, dismissOnClickOutside = !state.busy),
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(stringResource(R.string.update_version, release.version))
                Text("${BuildConfig.DISPLAY_VERSION} → ${release.version}", style = MaterialTheme.typography.bodyMedium)
                Text(stringResource(R.string.update_install_note), style = MaterialTheme.typography.bodySmall)
                if (state.busy) {
                    Text(stringResource(R.string.update_download))
                    state.progress?.let { progress ->
                        LinearProgressIndicator(progress = { progress }, modifier = Modifier.fillMaxWidth())
                    } ?: LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                }
                state.message?.let { Text(stringResource(it), color = MaterialTheme.colorScheme.error) }
            }
        },
        confirmButton = {
            TextButton(onClick = onUpdate, enabled = !state.busy && !state.checking, modifier = Modifier.testTag("install-app-update")) {
                Text(stringResource(R.string.update_app))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, enabled = !state.busy) { Text(stringResource(R.string.update_later)) }
        }
    )
}
